package com.sprintflow.service;

import java.time.LocalDateTime;
import java.util.UUID;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import com.sprintflow.entity.Invitation;
import com.sprintflow.repository.InvitationRepository;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final EmailService emailService;
    private final String frontendUrl;

    public InvitationService(InvitationRepository invitationRepository, EmailService emailService, @Value("${sprintflow.frontend.url}") String frontendUrl) {
        this.invitationRepository = invitationRepository;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public void sendInvitation(String userEmail, long projectId) {
        Invitation existing = invitationRepository.findByUserEmail(userEmail);
        if (existing != null && existing.getProjectId().equals(projectId)) {
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

        String invitationLink = frontendUrl + "/invitations/accept_invitation?token=" + invitationToken;
        emailService.sendEmailWithToken(userEmail, invitationLink);
    }

    @Transactional
    public Invitation acceptInvitation(String token) throws Exception {
        Invitation invitation = invitationRepository.findByToken(token);
        if (invitation == null) {
            throw new Exception("Invalid invitation token");
        }
        if (invitation.isUsed()) {
            throw new Exception("Invitation has already been used");
        }
        if (invitation.getExpiryDate() != null && invitation.getExpiryDate().isBefore(LocalDateTime.now())) {
            invitationRepository.delete(invitation);
            throw new Exception("Invitation has expired");
        }
        invitation.setUsed(true);
        invitationRepository.save(invitation);
        return invitation;
    }

    @Transactional(readOnly = true)
    public String getTokenByUserEmail(String userEmail){
        Invitation invitation = invitationRepository.findByUserEmail(userEmail);
        if (invitation != null) {
            return invitation.getToken();
        }
        throw new RuntimeException("No invitation found for the provided email: " + userEmail);
    }

    @Transactional
    public void deleteToken(String token) {
        Invitation invitation = invitationRepository.findByToken(token);
        if (invitation != null) {
            invitationRepository.deleteById(invitation.getId());
        }
    }

}