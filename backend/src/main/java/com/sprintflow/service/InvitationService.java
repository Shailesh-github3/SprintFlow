package com.sprintflow.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import com.sprintflow.entity.AuditAction;
import com.sprintflow.entity.AuditEntityType;
import com.sprintflow.entity.Invitation;
import com.sprintflow.entity.User;
import com.sprintflow.repository.InvitationRepository;
import com.sprintflow.repository.UserRepository;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final EmailService emailService;
    private final ProjectService projectService;
    private final AuditService auditService;
    private final String frontendUrl;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public InvitationService(InvitationRepository invitationRepository, EmailService emailService,
                             @Lazy ProjectService projectService, AuditService auditService,
                             @Value("${sprintflow.frontend.url}") String frontendUrl) {
        this(invitationRepository, emailService, projectService, auditService, frontendUrl, null, null);
    }

    @Autowired
    public InvitationService(InvitationRepository invitationRepository, EmailService emailService,
                             @Lazy ProjectService projectService, AuditService auditService,
                             @Value("${sprintflow.frontend.url}") String frontendUrl,
                             @Autowired(required = false) UserRepository userRepository,
                             @Autowired(required = false) NotificationService notificationService) {
        this.invitationRepository = invitationRepository;
        this.emailService = emailService;
        this.projectService = projectService;
        this.auditService = auditService;
        this.frontendUrl = frontendUrl;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void sendInvitation(String userEmail, long projectId, String invitedByEmail, Long invitedUserId) {
        Invitation existing = invitationRepository.findByUserEmailAndProjectId(userEmail, projectId);
        if (existing != null) {
            invitationRepository.delete(existing);
        }

        String invitationToken = UUID.randomUUID().toString();
        Invitation invitation = new Invitation();
        invitation.setToken(invitationToken);
        invitation.setUserEmail(userEmail);
        invitation.setProjectId(projectId);
        invitation.setCreatedAt(LocalDateTime.now());
        invitation.setExpiryDate(LocalDateTime.now().plusDays(7));
        invitation.setUsed(false);
        invitationRepository.save(invitation);

        auditService.log(invitedByEmail, projectId, AuditEntityType.PROJECT_MEMBER,
                invitedUserId, AuditAction.INVITED, null, userEmail);

        String invitationLink = frontendUrl + "/invite/accept?token=" + invitationToken;
        emailService.sendEmailWithToken(userEmail, invitationLink);

        if (notificationService != null && userRepository != null) {
            User user = userRepository.findByEmail(userEmail);
            if (user != null) {
                notificationService.createNotification(
                        user,
                        "Project Invitation",
                        "You were invited by " + invitedByEmail + " to join project #" + projectId,
                        "/invite/accept?token=" + invitationToken
                );
            }
        }
    }

    @Transactional
    public Invitation acceptInvitation(String token) throws Exception {
        Invitation invitation = invitationRepository.findByToken(token);
        if (invitation == null) {
            throw new IllegalArgumentException("Invalid invitation token");
        }
        if (invitation.isUsed()) {
            throw new IllegalArgumentException("Invitation has already been used");
        }
        if (invitation.getExpiryDate() != null && invitation.getExpiryDate().isBefore(LocalDateTime.now())) {
            invitationRepository.delete(invitation);
            throw new IllegalArgumentException("Invitation has expired");
        }
        invitation.setUsed(true);
        invitationRepository.save(invitation);
        return invitation;
    }

    /**
     * Validates the invitation, checks the recipient, adds the user to the project
     * and marks the invitation used — all in a single transaction. If adding the
     * team member fails, the invitation stays unconsumed.
     */
    @Transactional
    public Invitation acceptInvitationForUser(String token, String currentUserEmail, Long currentUserId) throws Exception {
        Invitation invitation = invitationRepository.findByToken(token);
        if (invitation == null) {
            throw new IllegalArgumentException("Invalid invitation token");
        }
        if (!invitation.getUserEmail().equalsIgnoreCase(currentUserEmail)) {
            throw new SecurityException("This invitation was not sent to you");
        }
        if (invitation.isUsed()) {
            throw new IllegalArgumentException("Invitation has already been used");
        }
        if (invitation.getExpiryDate() != null && invitation.getExpiryDate().isBefore(LocalDateTime.now())) {
            invitationRepository.delete(invitation);
            throw new IllegalArgumentException("Invitation has expired");
        }
        projectService.addTeamMember(invitation.getProjectId(), currentUserId);
        invitation.setUsed(true);
        invitationRepository.save(invitation);
        auditService.log(currentUserEmail, invitation.getProjectId(), AuditEntityType.PROJECT_MEMBER,
                currentUserId, AuditAction.JOINED, null, null);
        return invitation;
    }

    @Transactional
    public void rejectInvitationForUser(String token, String currentUserEmail) {
        Invitation invitation = invitationRepository.findByToken(token);
        if (invitation == null) {
            throw new IllegalArgumentException("Invalid invitation token");
        }
        if (!invitation.getUserEmail().equalsIgnoreCase(currentUserEmail)) {
            throw new SecurityException("This invitation was not sent to you");
        }
        invitationRepository.delete(invitation);
    }
}
