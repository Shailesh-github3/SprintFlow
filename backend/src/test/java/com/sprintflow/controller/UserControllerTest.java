package com.sprintflow.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprintflow.config.JwtService;
import com.sprintflow.config.JwtTokenValidator;
import com.sprintflow.dto.DashboardStatsDTO;
import com.sprintflow.dto.UserDTO;
import com.sprintflow.entity.User;
import com.sprintflow.mapper.UserMapper;
import com.sprintflow.service.UserService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtTokenValidator jwtTokenValidator;

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    void getCurrentUser_shouldReturnProfile_whenAuthenticated() throws Exception {
        User user = new User();
        user.setUserId(1L);
        user.setFullName("Alice");
        user.setEmail("alice@example.com");

        UserDTO dto = new UserDTO();
        dto.setUserId(1L);
        dto.setFullName("Alice");
        dto.setEmail("alice@example.com");
        dto.setRoles(java.util.List.of("USER"));

        when(userService.getUserByEmail("alice@example.com")).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(dto);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.fullName").value("Alice"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    @WithMockUser(username = "ghost@example.com", roles = "USER")
    void getCurrentUser_shouldReturn404_whenUserRowMissing() throws Exception {
        when(userService.getUserByEmail("ghost@example.com")).thenReturn(null);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = "USER")
    @DisplayName("getDashboardStats returns aggregated metrics")
    void getDashboardStats_shouldReturnAggregatedStats_whenAuthenticated() throws Exception {
        User user = new User();
        user.setUserId(1L);
        user.setEmail("alice@example.com");

        DashboardStatsDTO stats = new DashboardStatsDTO(4L, 15L, 5L, 3L, 7L, 2L, 46.7);

        when(userService.getUserByEmail("alice@example.com")).thenReturn(user);
        when(userService.getDashboardStats(user)).thenReturn(stats);

        mockMvc.perform(get("/api/users/dashboard-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjects").value(4))
                .andExpect(jsonPath("$.totalIssues").value(15))
                .andExpect(jsonPath("$.todoIssues").value(5))
                .andExpect(jsonPath("$.inProgressIssues").value(3))
                .andExpect(jsonPath("$.completedIssues").value(7))
                .andExpect(jsonPath("$.overdueIssues").value(2))
                .andExpect(jsonPath("$.completionPercentage").value(46.7));
    }
}
