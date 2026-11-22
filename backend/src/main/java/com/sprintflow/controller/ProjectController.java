package com.sprintflow.controller;

import com.sprintflow.dto.ProjectDTO;
import com.sprintflow.dto.ProjectStatsDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import com.sprintflow.entity.AuditLog;
import com.sprintflow.entity.Project;
import com.sprintflow.entity.User;
import com.sprintflow.request.ProjectRequest;
import com.sprintflow.request.InviteRequest;
import com.sprintflow.response.MessageResponse;
import com.sprintflow.service.ProjectService;
import com.sprintflow.service.UserService;
import com.sprintflow.service.AuditService;
import com.sprintflow.service.InvitationService;
import com.sprintflow.mapper.ProjectMapper;
import com.sprintflow.mapper.ChatMapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final UserService userService;
    private final InvitationService invitationService;
    private final AuditService auditService;
    private final ProjectMapper projectMapper;
    private final ChatMapper chatMapper;

    public ProjectController(ProjectService projectService, UserService userService, InvitationService invitationService,
                             AuditService auditService, ProjectMapper projectMapper, ChatMapper chatMapper) {
        this.projectService = projectService;
        this.userService = userService;
        this.invitationService = invitationService;
        this.auditService = auditService;
        this.projectMapper = projectMapper;
        this.chatMapper = chatMapper;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createProject(@Valid @RequestBody ProjectRequest request) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User projectOwner = userService.findByEmail(currentUserEmail);

        Project project = new Project();
        project.setProjectName(request.getProjectName());
        project.setProjectDescription(request.getProjectDescription());
        project.setCategory(request.getCategory());
        project.setTags(request.getTags());

        Project createdProject = projectService.createProject(project, projectOwner);
        ProjectDTO dto = projectMapper.toDto(createdProject);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProjectById(@PathVariable Long projectId) throws Exception {
        Project project = projectService.getProjectById(projectId);
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        projectService.ensureProjectMemberOrOwner(projectId, currentUserEmail);
        return ResponseEntity.ok(projectMapper.toDto(project));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProjects(@RequestParam(required = false) String category,
                                         @RequestParam(required = false) String tag,
                                         @PageableDefault(size = 20, sort = "projectId", direction = Sort.Direction.DESC) Pageable pageable) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User teamMember = userService.findByEmail(currentUserEmail);
        Page<Project> projectPage = projectService.getProjectByTeamMember(teamMember, category, tag, pageable);
        return ResponseEntity.ok(projectPage.map(projectMapper::toDto));
    }

    @PutMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProject(@PathVariable Long projectId, @Valid @RequestBody ProjectRequest request) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        projectService.ensureProjectOwner(projectId, currentUserEmail);
        Project updatedProject = new Project();
        updatedProject.setProjectName(request.getProjectName());
        updatedProject.setProjectDescription(request.getProjectDescription());
        updatedProject.setCategory(request.getCategory());
        updatedProject.setTags(request.getTags());
        Project project = projectService.updateProject(projectId, updatedProject);
        return ResponseEntity.ok(projectMapper.toDto(project));
    }

    @DeleteMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteProject(@PathVariable Long projectId) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        projectService.ensureProjectOwner(projectId, currentUserEmail);
        projectService.deleteProject(projectId);
        return ResponseEntity.ok(new MessageResponse("Project deleted successfully."));
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> removeMember(@PathVariable Long projectId, @PathVariable Long userId) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        projectService.removeMember(projectId, userId, currentUserEmail);
        return ResponseEntity.ok(new MessageResponse("Member removed successfully."));
    }

    @PostMapping("/{projectId}/leave")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> leaveProject(@PathVariable Long projectId) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        projectService.leaveProject(projectId, currentUserEmail);
        return ResponseEntity.ok(new MessageResponse("You have left the project."));
    }

    @GetMapping("/{projectId}/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectStatsDTO> getProjectStats(@PathVariable Long projectId) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        ProjectStatsDTO stats = projectService.getProjectStats(projectId, currentUserEmail);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> searchProjects(@RequestParam(required = false) String name,
                                            @PageableDefault(size = 20, sort = "projectId", direction = Sort.Direction.DESC) Pageable pageable) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User teamMember = userService.findByEmail(currentUserEmail);
        Page<Project> projectPage = projectService.searchProjects(name, teamMember, pageable);
        return ResponseEntity.ok(projectPage.map(projectMapper::toDto));
    }

    @GetMapping("/{projectId}/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getChatByProjectId(@PathVariable Long projectId) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        projectService.ensureProjectMemberOrOwner(projectId, currentUserEmail);
        return ResponseEntity.ok(chatMapper.toDto(projectService.getChatByProjectId(projectId)));
    }

    @PostMapping("/invite")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> inviteUserToProject(@Valid @RequestBody InviteRequest inviteRequest) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        projectService.ensureProjectOwner(inviteRequest.getProjectId(), currentUserEmail);
        User user = userService.getUserByEmail(inviteRequest.getUserEmail());
        if (user == null) {
            throw new IllegalArgumentException("User not found. They must register first.");
        }
        invitationService.sendInvitation(user.getEmail(), inviteRequest.getProjectId(), currentUserEmail, user.getUserId());
        return ResponseEntity.ok(new MessageResponse("Invitation sent successfully."));
    }

    @GetMapping("/invite")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> acceptInvitation(@RequestParam String token) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.findByEmail(currentUserEmail);
        invitationService.acceptInvitationForUser(token, currentUserEmail, currentUser.getUserId());
        return ResponseEntity.ok(new MessageResponse("Invitation accepted successfully."));
    }

    @PostMapping("/invite/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> rejectInvitation(@RequestParam String token) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        invitationService.rejectInvitationForUser(token, currentUserEmail);
        return ResponseEntity.ok(new MessageResponse("Invitation declined successfully."));
    }

    @GetMapping("/{projectId}/activity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProjectActivity(@PathVariable Long projectId,
                                                @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) throws Exception {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        projectService.ensureProjectMemberOrOwner(projectId, currentUserEmail);
        Page<AuditLog> activityPage = auditService.getActivityForProject(projectId, pageable);
        return ResponseEntity.ok(activityPage.map(auditService::toDto));
    }

}
