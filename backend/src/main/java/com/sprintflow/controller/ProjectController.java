package com.sprintflow.controller;

import com.sprintflow.dto.ProjectDTO;
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

import com.sprintflow.entity.Invitation;
import com.sprintflow.entity.Project;
import com.sprintflow.entity.User;
import com.sprintflow.request.ProjectRequest;
import com.sprintflow.request.InviteRequest;
import com.sprintflow.response.MessageResponse;
import com.sprintflow.service.ProjectService;
import com.sprintflow.service.UserService;
import com.sprintflow.service.InvitationService;
import com.sprintflow.mapper.ProjectMapper;
import com.sprintflow.mapper.ChatMapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final UserService userService;
    private final InvitationService invitationService;
    private final ProjectMapper projectMapper;
    private final ChatMapper chatMapper;

    public ProjectController(ProjectService projectService, UserService userService, InvitationService invitationService, ProjectMapper projectMapper, ChatMapper chatMapper) {
        this.projectService = projectService;
        this.userService = userService;
        this.invitationService = invitationService;
        this.projectMapper = projectMapper;
        this.chatMapper = chatMapper;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createProject(@Valid @RequestBody ProjectRequest request) {
        try {
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

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
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
                                         @PageableDefault(size = 20, sort = "projectId", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            User teamMember = userService.findByEmail(currentUserEmail);
            Page<Project> projectPage = projectService.getProjectByTeamMember(teamMember, category, tag, pageable);
            return ResponseEntity.ok(projectPage.map(projectMapper::toDto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }


    @PutMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProject(@PathVariable Long projectId, @Valid @RequestBody ProjectRequest request) {
        try {
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            projectService.ensureProjectOwner(projectId, currentUserEmail);
            Project updatedProject = new Project();
            updatedProject.setProjectName(request.getProjectName());
            updatedProject.setProjectDescription(request.getProjectDescription());
            updatedProject.setCategory(request.getCategory());
            updatedProject.setTags(request.getTags());
            Project project = projectService.updateProject(projectId, updatedProject);
            return ResponseEntity.ok(projectMapper.toDto(project));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    
    
    @DeleteMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteProject(@PathVariable Long projectId) {
        try {
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            projectService.ensureProjectOwner(projectId, currentUserEmail);
            projectService.deleteProject(projectId);
            return ResponseEntity.ok(new MessageResponse("Project deleted successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> searchProjects(@RequestParam(required = false) String name) {
        try {
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            User teamMember = userService.findByEmail(currentUserEmail);
            List<ProjectDTO> dtoList = projectService.searchProjects(name, teamMember).stream()
                                                    .map(projectMapper::toDto)
                                                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtoList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping("/{projectId}/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getChatByProjectId(@PathVariable Long projectId) {
        try {
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            projectService.ensureProjectMemberOrOwner(projectId, currentUserEmail);
            return ResponseEntity.ok(chatMapper.toDto(projectService.getChatByProjectId(projectId)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/invite")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> inviteUserToProject(@Valid @RequestBody InviteRequest inviteRequest) {

        try {
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            projectService.ensureProjectOwner(inviteRequest.getProjectId(), currentUserEmail);
            User user = userService.getUserByEmail(inviteRequest.getUserEmail());
            if(user == null){
                return ResponseEntity.badRequest().body(new MessageResponse("User not found. They must register first."));
            }
            invitationService.sendInvitation(user.getEmail(),inviteRequest.getProjectId());
            return ResponseEntity.ok(new MessageResponse("Invitation sent successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse(e.getMessage()));
        }
    }


    @GetMapping("/invite")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> acceptInvitation(@RequestParam String token) {
        try {
            Invitation invitation = invitationService.acceptInvitation(token);
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            if (!currentUserEmail.equals(invitation.getUserEmail())) {
                throw new SecurityException("This invitation was not sent to you");
            }
            User currentUser = userService.findByEmail(currentUserEmail);
            projectService.addTeamMember(invitation.getProjectId(), currentUser.getUserId());
            return ResponseEntity.ok(new MessageResponse("Invitation accepted successfully."));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse(e.getMessage()));
        }
    }
     

}