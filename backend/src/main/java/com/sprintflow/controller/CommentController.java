package com.sprintflow.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sprintflow.entity.Comment;
import com.sprintflow.request.CreateCommentRequest;
import com.sprintflow.service.CommentService;
import com.sprintflow.service.ProjectService;

import jakarta.validation.Valid;
import com.sprintflow.mapper.CommentMapper;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;
    private final ProjectService projectService;
    private final CommentMapper commentMapper;

    public CommentController(CommentService commentService, ProjectService projectService, CommentMapper commentMapper) {
        this.commentService = commentService;
        this.projectService = projectService;
        this.commentMapper = commentMapper;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createComment(@Valid @RequestBody CreateCommentRequest commentRequest) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Comment createdComment = commentService.createComment(commentRequest.getIssueId(), currentUserEmail, commentRequest.getContent());
        return ResponseEntity.status(201).body(commentMapper.toDto(createdComment));
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        commentService.deleteComment(commentId, currentUserEmail);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/issue/{issueId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCommentsByIssueId(@PathVariable Long issueId,
                                                   @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) throws Exception {
        Page<Comment> commentPage = commentService.findCommentsByIssueId(issueId, pageable);
        return ResponseEntity.ok(commentPage.map(commentMapper::toDto));
    }

}