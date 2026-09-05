package com.sprintflow.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendEmailWithToken_shouldPopulateRecipientSubjectAndHtmlLink() throws Exception {
        // Given
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        String invitationLink = "http://localhost:5173/invitations/accept_invitation?token=abc123";

        // When
        emailService.sendEmailWithToken("alice@example.com", invitationLink);

        // Then
        verify(javaMailSender).send(mimeMessage);
        assertTrue(mimeMessage.getSubject().contains("Join Project Invitation"));
        assertTrue(mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString().contains("alice@example.com"));
        assertTrue(String.valueOf(mimeMessage.getContent()).contains(invitationLink));
    }
}