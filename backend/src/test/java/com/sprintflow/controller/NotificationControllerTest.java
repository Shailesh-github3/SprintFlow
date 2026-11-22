package com.sprintflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.sprintflow.config.JwtService;
import com.sprintflow.config.JwtTokenValidator;
import com.sprintflow.dto.NotificationDTO;
import com.sprintflow.entity.Notification;
import com.sprintflow.entity.User;
import com.sprintflow.mapper.NotificationMapper;
import com.sprintflow.service.NotificationService;
import com.sprintflow.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private NotificationMapper notificationMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtTokenValidator jwtTokenValidator;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setEmail("alice@example.com");
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    @DisplayName("getNotifications returns page of notifications for authenticated user")
    void getNotifications_success() throws Exception {
        Notification notification = new Notification();
        notification.setNotificationId(10L);
        notification.setTitle("New Task");
        notification.setMessage("Assigned to you");

        NotificationDTO dto = new NotificationDTO(10L, "New Task", "Assigned to you", false, "/task/10", null);

        when(userService.findByEmail("alice@example.com")).thenReturn(user);
        when(notificationService.getNotificationsForUser(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notification)));
        when(notificationMapper.toDto(notification)).thenReturn(dto);

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].notificationId").value(10))
                .andExpect(jsonPath("$.content[0].title").value("New Task"));
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    @DisplayName("getUnreadCount returns unread count map")
    void getUnreadCount_success() throws Exception {
        when(userService.findByEmail("alice@example.com")).thenReturn(user);
        when(notificationService.getUnreadCount(1L)).thenReturn(3L);

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    @DisplayName("markAsRead updates notification status to read")
    void markAsRead_success() throws Exception {
        Notification notification = new Notification();
        notification.setNotificationId(10L);
        notification.setReadStatus(true);

        NotificationDTO dto = new NotificationDTO(10L, "New Task", "Assigned to you", true, "/task/10", null);

        when(userService.findByEmail("alice@example.com")).thenReturn(user);
        when(notificationService.markAsRead(10L, 1L)).thenReturn(notification);
        when(notificationMapper.toDto(notification)).thenReturn(dto);

        mockMvc.perform(put("/api/notifications/10/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readStatus").value(true));
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    @DisplayName("markAllAsRead marks all user notifications as read")
    void markAllAsRead_success() throws Exception {
        when(userService.findByEmail("alice@example.com")).thenReturn(user);
        when(notificationService.markAllAsRead(1L)).thenReturn(5);

        mockMvc.perform(put("/api/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markedCount").value(5));
    }
}
