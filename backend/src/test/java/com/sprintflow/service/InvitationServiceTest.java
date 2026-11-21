package com.sprintflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sprintflow.entity.Invitation;
import com.sprintflow.repository.InvitationRepository;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private ProjectService projectService;

    @Mock
    private AuditService auditService;

    private InvitationService invitationService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        invitationService = new InvitationService(invitationRepository, emailService, projectService, auditService, "http://localhost:5173");
    }

    @Nested
    @DisplayName("sendInvitation")
    class SendInvitationTests {

        @Test
        @DisplayName("generates a UUID token, stores a 7-day expiry, and sends the invitation email")
        void sendInvitation_generatesTokenAndExpiry() {
            // Given
            Invitation existing = new Invitation();
            existing.setId(11L);
            existing.setUserEmail("alice@example.com");
            existing.setProjectId(42L);

            when(invitationRepository.findByUserEmailAndProjectId("alice@example.com", 42L)).thenReturn(existing);
            when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UUID fixedUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
            try (MockedStatic<UUID> uuid = mockStatic(UUID.class)) {
                uuid.when(UUID::randomUUID).thenReturn(fixedUuid);

                // When
                invitationService.sendInvitation("alice@example.com", 42L, "owner@example.com", 7L);
            }

            ArgumentCaptor<Invitation> invitationCaptor = ArgumentCaptor.forClass(Invitation.class);
            verify(invitationRepository).save(invitationCaptor.capture());
            Invitation saved = invitationCaptor.getValue();

            // Then
            assertEquals(fixedUuid.toString(), saved.getToken());
            assertEquals("alice@example.com", saved.getUserEmail());
            assertEquals(42L, saved.getProjectId());
            assertFalse(saved.isUsed());
            assertTrue(saved.getExpiryDate().isAfter(LocalDateTime.now().plusDays(6)));
            assertTrue(saved.getExpiryDate().isBefore(LocalDateTime.now().plusDays(8)));
            verify(invitationRepository).delete(existing);
            verify(emailService).sendEmailWithToken("alice@example.com", "http://localhost:5173/invite/accept?token=" + fixedUuid);
            verify(auditService).log("owner@example.com", 42L,
                    com.sprintflow.entity.AuditEntityType.PROJECT_MEMBER, 7L,
                    com.sprintflow.entity.AuditAction.INVITED, null, "alice@example.com");
        }
    }

    @Nested
    @DisplayName("acceptInvitation")
    class AcceptInvitationTests {

        @Test
        @DisplayName("marks a valid invitation as used")
        void acceptInvitation_success_marksUsed() throws Exception {
            // Given
            Invitation invitation = buildInvitation(false, LocalDateTime.now().plusDays(2));
            when(invitationRepository.findByToken("valid-token")).thenReturn(invitation);
            when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Invitation result = invitationService.acceptInvitation("valid-token");

            // Then
            assertTrue(result.isUsed());
            verify(invitationRepository).save(invitation);
        }

        @Test
        @DisplayName("rejects an expired invitation token")
        void acceptInvitation_expiredToken_throws() {
            // Given
            Invitation invitation = buildInvitation(false, LocalDateTime.now().minusDays(1));
            when(invitationRepository.findByToken("expired-token")).thenReturn(invitation);
            doNothing().when(invitationRepository).delete(invitation);

            // When / Then
            Exception exception = assertThrows(Exception.class, () -> invitationService.acceptInvitation("expired-token"));
            assertEquals("Invitation has expired", exception.getMessage());
            verify(invitationRepository).delete(invitation);
        }

        @Test
        @DisplayName("rejects an already accepted invitation token")
        void acceptInvitation_alreadyUsed_throws() {
            // Given
            Invitation invitation = buildInvitation(true, LocalDateTime.now().plusDays(2));
            when(invitationRepository.findByToken("used-token")).thenReturn(invitation);

            // When / Then
            Exception exception = assertThrows(Exception.class, () -> invitationService.acceptInvitation("used-token"));
            assertEquals("Invitation has already been used", exception.getMessage());
        }

        @Test
        @DisplayName("rejects an invalid token format")
        void acceptInvitation_invalidToken_returnsInvalidTokenError() {
            // Given
            when(invitationRepository.findByToken("not-a-valid-token")).thenReturn(null);

            // When / Then
            Exception exception = assertThrows(Exception.class, () -> invitationService.acceptInvitation("not-a-valid-token"));
            assertEquals("Invalid invitation token", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("acceptInvitationForUser (atomic)")
    class AcceptInvitationForUserTests {

        @Test
        @DisplayName("adds the member and marks the invitation used in one flow")
        void acceptForUser_success_addsMemberAndMarksUsed() throws Exception {
            Invitation invitation = buildInvitation(false, LocalDateTime.now().plusDays(2));
            when(invitationRepository.findByToken("valid-token")).thenReturn(invitation);
            when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Invitation result = invitationService.acceptInvitationForUser("valid-token", "alice@example.com", 7L);

            assertTrue(result.isUsed());
            verify(projectService).addTeamMember(42L, 7L);
            verify(invitationRepository).save(invitation);
            verify(auditService).log("alice@example.com", 42L,
                    com.sprintflow.entity.AuditEntityType.PROJECT_MEMBER, 7L,
                    com.sprintflow.entity.AuditAction.JOINED, null, null);
        }

        @Test
        @DisplayName("rejects an invitation addressed to a different user without consuming it")
        void acceptForUser_wrongRecipient_throwsSecurityException() {
            Invitation invitation = buildInvitation(false, LocalDateTime.now().plusDays(2));
            when(invitationRepository.findByToken("valid-token")).thenReturn(invitation);

            SecurityException exception = assertThrows(SecurityException.class,
                    () -> invitationService.acceptInvitationForUser("valid-token", "bob@example.com", 8L));
            assertEquals("This invitation was not sent to you", exception.getMessage());
            assertFalse(invitation.isUsed());
            verify(invitationRepository, org.mockito.Mockito.never()).save(any(Invitation.class));
        }
    }

    private Invitation buildInvitation(boolean used, LocalDateTime expiryDate) {
        Invitation invitation = new Invitation();
        invitation.setId(1L);
        invitation.setToken("token");
        invitation.setUserEmail("alice@example.com");
        invitation.setProjectId(42L);
        invitation.setCreatedAt(LocalDateTime.now().minusDays(1));
        invitation.setExpiryDate(expiryDate);
        invitation.setUsed(used);
        return invitation;
    }
}