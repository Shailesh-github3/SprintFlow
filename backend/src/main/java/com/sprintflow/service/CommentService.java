package com.sprintflow.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDateTime;

import com.sprintflow.entity.Comment;
import com.sprintflow.entity.Issue;
import com.sprintflow.entity.User;
import com.sprintflow.repository.CommentRepository;
import com.sprintflow.repository.IssueRepository;
import com.sprintflow.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");

    public CommentService(CommentRepository commentRepository, IssueRepository issueRepository, UserRepository userRepository) {
        this(commentRepository, issueRepository, userRepository, null);
    }

    @Autowired
    public CommentService(CommentRepository commentRepository, IssueRepository issueRepository,
                          UserRepository userRepository,
                          @Autowired(required = false) NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Comment createComment(Long issueId, String userEmail, String content) {
        Long resolvedIssueId = Objects.requireNonNull(issueId, "issueId must not be null");
        String resolvedUserEmail = Objects.requireNonNull(userEmail, "userEmail must not be null");
        Optional<Issue> issue = issueRepository.findById(resolvedIssueId);
        User user = userRepository.findByEmail(resolvedUserEmail);
        if (issue.isEmpty() || user == null) {
            throw new IllegalArgumentException("Invalid issueId or user");
        }
        Issue issueEntity = issue.get();
        Comment comment = new Comment();
        comment.setIssue(issueEntity);
        comment.setCreatedBy(user);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setCommentText(content);

        Comment savedComment = commentRepository.save(comment);
        issueEntity.getComments().add(savedComment);

        // Handle notifications
        if (notificationService != null) {
            Long projectId = issueEntity.getProject() != null ? issueEntity.getProject().getProjectId() : null;
            String issueLink = projectId != null ? "/project/" + projectId + "?issue=" + issueEntity.getIssueId() : "/dashboard";

            // 1. Mentions
            if (content != null && content.contains("@")) {
                Matcher matcher = MENTION_PATTERN.matcher(content);
                Set<String> mentionedEmails = new HashSet<>();
                while (matcher.find()) {
                    mentionedEmails.add(matcher.group(1));
                }
                for (String mentionedEmail : mentionedEmails) {
                    if (!mentionedEmail.equalsIgnoreCase(resolvedUserEmail)) {
                        User mentionedUser = userRepository.findByEmail(mentionedEmail);
                        if (mentionedUser != null) {
                            notificationService.createNotification(
                                    mentionedUser,
                                    "Mentioned in Comment",
                                    user.getFullName() + " mentioned you in a comment on issue #" + issueEntity.getIssueId(),
                                    issueLink
                            );
                        }
                    }
                }
            }

            // 2. Notify assignee if not the commenter
            if (issueEntity.getAssignedUser() != null &&
                    !issueEntity.getAssignedUser().getEmail().equalsIgnoreCase(resolvedUserEmail)) {
                notificationService.createNotification(
                        issueEntity.getAssignedUser(),
                        "New Comment on Your Issue",
                        user.getFullName() + " commented on issue #" + issueEntity.getIssueId(),
                        issueLink
                );
            }
        }

        return savedComment;
    }

    @Transactional
    public void deleteComment(Long commentId, String userEmail) {
        Long resolvedCommentId = Objects.requireNonNull(commentId, "commentId must not be null");
        String resolvedUserEmail = Objects.requireNonNull(userEmail, "userEmail must not be null");
        Optional<Comment> comment = commentRepository.findById(resolvedCommentId);
        User user = userRepository.findByEmail(resolvedUserEmail);
        if (comment.isEmpty() || user == null) {
            throw new IllegalArgumentException("Invalid commentId or user");
        }
        Comment commentEntity = comment.get();

        boolean isAuthor = commentEntity.getCreatedBy() != null && commentEntity.getCreatedBy().getUserId().equals(user.getUserId());
        boolean isProjectOwner = commentEntity.getIssue() != null &&
                                 commentEntity.getIssue().getProject() != null &&
                                 commentEntity.getIssue().getProject().getProjectOwner() != null &&
                                 commentEntity.getIssue().getProject().getProjectOwner().getUserId().equals(user.getUserId());

        if (isAuthor || isProjectOwner) {
            commentRepository.delete(commentEntity);
        } else {
            throw new SecurityException("User is not authorized to delete this comment");
        }
    }

    @Transactional(readOnly = true)
    public Page<Comment> findCommentsByIssueId(Long issueId, Pageable pageable) {
        Long resolvedIssueId = Objects.requireNonNull(issueId, "issueId must not be null");
        return commentRepository.findByIssue_IssueId(resolvedIssueId, pageable);
    }

}
