package com.sprintflow.service;

import java.time.LocalDateTime;
import java.util.UUID;


import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import com.sprintflow.entity.AuditAction;
import com.sprintflow.entity.AuditEntityType;
import com.sprintflow.entity.Invitation;
import com.sprintflow.repository.InvitationRepository;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final EmailService emailService;
    private final ProjectService projectService;
    private final AuditService auditService;
    private final String frontendUrl;

    public InvitationService(InvitationRepository invitationRepository, EmailService emailService, @Lazy ProjectService projectService, AuditService auditService, @Value("${sprintflow.frontend.url}") String frontendUrl) {
        this.invitationRepository = invitationRepository;
        this.emailService = emailService;
        this.projectService = projectService;
        this.auditService = auditService;
        this.frontendUrl = frontendUrl;
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
        if (!invitation.getUserEmail().equals(currentUserEmail)) {
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
}