package com.sprintflow.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender javaMailSender;

    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void sendEmailWithToken(String userEmail, String link) {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
        String subject = "Join Project Invitation";
        String content = "<p>Hello,</p>"
                + "<p>You have been invited to join a project. Please click the link below to accept the invitation:</p>"
                + "<p><a href=\"" + link + "\">Accept Invitation</a></p>"
                + "<br>"
                + "<p>If you did not expect this invitation, please ignore this email.</p>";
        try {
            helper.setTo(userEmail);
            helper.setSubject(subject);
            helper.setText(content, true);
            javaMailSender.send(message);
        } catch (Exception e) {
            logger.error("Failed to send invitation email to {}: {}", userEmail, e.getMessage(), e);
        }
    }

}