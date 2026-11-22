package com.sprintflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sprintflow.entity.Notification;
import com.sprintflow.entity.User;
import com.sprintflow.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    private User recipient;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
        recipient = new User();
        recipient.setUserId(1L);
        recipient.setEmail("user@example.com");
    }

    @Test
    @DisplayName("createNotification builds and persists notification with readStatus false")
    void createNotification_success() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setNotificationId(99L);
            return n;
        });

        Notification created = notificationService.createNotification(recipient, "Test Title", "Test Message", "/project/1");

        assertNotNull(created);
        assertEquals(99L, created.getNotificationId());
        assertEquals("Test Title", created.getTitle());
        assertEquals("Test Message", created.getMessage());
        assertEquals("/project/1", created.getLink());
        assertFalse(created.isReadStatus());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("markAsRead marks unread notification as read")
    void markAsRead_success() {
        Notification notification = new Notification();
        notification.setNotificationId(10L);
        notification.setRecipient(recipient);
        notification.setReadStatus(false);

        when(notificationRepository.findByNotificationIdAndRecipient_UserId(10L, 1L))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.markAsRead(10L, 1L);

        assertTrue(result.isReadStatus());
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("getUnreadCount delegates to repository")
    void getUnreadCount_success() {
        when(notificationRepository.countByRecipient_UserIdAndReadStatusFalse(1L)).thenReturn(5L);

        long count = notificationService.getUnreadCount(1L);

        assertEquals(5L, count);
    }

    @Test
    @DisplayName("markAllAsReadForUser delegates to repository")
    void markAllAsReadForUser_success() {
        notificationService.markAllAsRead(1L);

        verify(notificationRepository).markAllAsReadForUser(1L);
    }
}
