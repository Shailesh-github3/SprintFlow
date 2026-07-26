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

import com.sprintflow.dto.IssueDTO;
import com.sprintflow.entity.Issue;
import com.sprintflow.request.IssueRequest;
import com.sprintflow.service.IssueService;
import com.sprintflow.service.ProjectService;
import org.springframework.web.bind.annotation.PutMapping;
import com.sprintflow.mapper.IssueMapper;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueService issueService;
    private final ProjectService projectService;
    private final IssueMapper issueMapper;

    public IssueController(IssueService issueService, ProjectService projectService, IssueMapper issueMapper) {
        this.issueService = issueService;
        this.projectService = projectService;
        this.issueMapper = issueMapper;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createIssue(@Valid @RequestBody IssueRequest issueRequest) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Issue createdIssue = issueService.createIssue(issueRequest, currentUserEmail);
        IssueDTO issueDTO = issueMapper.toDto(createdIssue);
        return ResponseEntity.status(201).body(issueDTO);
    }

    @GetMapping("/{issueId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getIssueById(@PathVariable Long issueId) throws Exception {
        Issue issue = issueService.getIssueById(issueId);
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        projectService.ensureProjectMemberOrOwner(issue.getProject().getProjectId(), currentUserEmail);
        return ResponseEntity.ok(issueMapper.toDto(issue));
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getIssuesByProjectId(@PathVariable Long projectId,
                                                   @PageableDefault(size = 20, sort = "issueId", direction = Sort.Direction.DESC) Pageable pageable) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        projectService.ensureProjectMemberOrOwner(projectId, currentUserEmail);
        Page<Issue> issuePage = issueService.getIssuesByProjectId(projectId, pageable);
        return ResponseEntity.ok(issuePage.map(issueMapper::toDto));
    }

    @DeleteMapping("/{issueId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteIssue(@PathVariable Long issueId) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        issueService.deleteIssue(issueId, currentUserEmail);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{issueId}/assignee/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addUserToIssue(@PathVariable Long issueId, @PathVariable Long userId) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        issueService.addUserToIssue(issueId, userId, currentUserEmail);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{issueId}/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateIssueStatus(@PathVariable String status, @PathVariable Long issueId) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        issueService.updateIssueStatus(issueId, status, currentUserEmail);
        return ResponseEntity.ok().build();
    }

}