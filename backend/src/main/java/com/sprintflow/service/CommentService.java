package com.sprintflow.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.Objects;
import com.sprintflow.entity.Comment;
import com.sprintflow.entity.Issue;
import com.sprintflow.entity.User;
import com.sprintflow.repository.CommentRepository;
import com.sprintflow.repository.IssueRepository;
import com.sprintflow.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository, IssueRepository issueRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Comment createComment(Long issueId, String userEmail, String content) {
        Long resolvedIssueId = Objects.requireNonNull(issueId, "issueId must not be null");
        String resolvedUserEmail = Objects.requireNonNull(userEmail, "userEmail must not be null");
        Optional<Issue> issue = issueRepository.findById(resolvedIssueId);
        User user = userRepository.findByEmail(resolvedUserEmail);
        if(issue.isEmpty() || user == null) {
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
        return savedComment;
    }

    @Transactional
    public void deleteComment(Long commentId, String userEmail) {
        Long resolvedCommentId = Objects.requireNonNull(commentId, "commentId must not be null");
        String resolvedUserEmail = Objects.requireNonNull(userEmail, "userEmail must not be null");
        Optional<Comment> comment = commentRepository.findById(resolvedCommentId);
        User user = userRepository.findByEmail(resolvedUserEmail);
        if(comment.isEmpty() || user == null) {
            throw new IllegalArgumentException("Invalid commentId or user");
        }
        Comment commentEntity = comment.get();

        if(commentEntity.getCreatedBy().getUserId().equals(user.getUserId())) {
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
