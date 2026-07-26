package com.sprintflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.sprintflow.entity.Comment;
import com.sprintflow.entity.Issue;
import com.sprintflow.entity.User;
import com.sprintflow.repository.CommentRepository;
import com.sprintflow.repository.IssueRepository;
import com.sprintflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    void createComment_shouldPersistCommentForIssueAndUserEmail() {
        Issue issue = new Issue();
        issue.setIssueId(10L);

        User user = new User();
        user.setUserId(20L);
        user.setEmail("alice@example.com");

        Comment savedComment = new Comment();
        savedComment.setCommentId(99L);
        savedComment.setCommentText("Looks good");
        savedComment.setCreatedAt(LocalDateTime.now());
        savedComment.setIssue(issue);
        savedComment.setCreatedBy(user);

        when(issueRepository.findById(10L)).thenReturn(Optional.of(issue));
        when(userRepository.findByEmail("alice@example.com")).thenReturn(user);
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        Comment result = commentService.createComment(10L, "alice@example.com", "Looks good");

        assertEquals(99L, result.getCommentId());
        assertEquals("Looks good", result.getCommentText());
        assertEquals(user, result.getCreatedBy());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void deleteComment_shouldRejectNonOwner() {
        User owner = new User();
        owner.setUserId(1L);
        owner.setEmail("owner@example.com");

        User otherUser = new User();
        otherUser.setUserId(2L);
        otherUser.setEmail("other@example.com");

        Comment comment = new Comment();
        comment.setCommentId(5L);
        comment.setCreatedBy(owner);

        when(commentRepository.findById(5L)).thenReturn(Optional.of(comment));
        when(userRepository.findByEmail("other@example.com")).thenReturn(otherUser);

        assertThrows(SecurityException.class, () -> commentService.deleteComment(5L, "other@example.com"));
    }

    @Test
    void findCommentsByIssueId_shouldReturnRepositoryResults() {
        Comment comment = new Comment();
        comment.setCommentId(7L);
        Page<Comment> page = new PageImpl<>(List.of(comment));
        Pageable pageable = PageRequest.of(0, 10);
        
        when(commentRepository.findByIssue_IssueId(11L, pageable)).thenReturn(page);

        Page<Comment> result = commentService.findCommentsByIssueId(11L, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(7L, result.getContent().get(0).getCommentId());
    }
}
