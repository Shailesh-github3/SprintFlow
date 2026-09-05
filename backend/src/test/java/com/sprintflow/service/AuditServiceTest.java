package com.sprintflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sprintflow.dto.AuditLogDTO;
import com.sprintflow.entity.AuditAction;
import com.sprintflow.entity.AuditEntityType;
import com.sprintflow.entity.AuditLog;
import com.sprintflow.entity.Project;
import com.sprintflow.entity.User;
import com.sprintflow.repository.AuditLogRepository;
import com.sprintflow.repository.ProjectRepository;
import com.sprintflow.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    @DisplayName("log persists actor, project, entity, action and before/after values")
    void log_persistsFullEntry() {
        User actor = user("John", "john@example.com");
        Project project = project(9L);
        when(userRepository.findByEmail("john@example.com")).thenReturn(actor);
        when(projectRepository.findById(9L)).thenReturn(Optional.of(project));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        AuditLog saved = auditService.log("john@example.com", 9L, AuditEntityType.ISSUE, 42L,
                AuditAction.STATUS_CHANGED, "TODO", "IN_PROGRESS");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog entry = captor.getValue();
        assertEquals(actor, entry.getActor());
        assertEquals(project, entry.getProject());
        assertEquals(AuditEntityType.ISSUE, entry.getEntityType());
        assertEquals(42L, entry.getEntityId());
        assertEquals(AuditAction.STATUS_CHANGED, entry.getAction());
        assertEquals("TODO", entry.getOldValue());
        assertEquals("IN_PROGRESS", entry.getNewValue());
        assertEquals(saved, entry);
    }

    @Test
    @DisplayName("log rejects an unknown actor")
    void log_unknownActor_throws() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> auditService.log("ghost@example.com", 9L,
                AuditEntityType.ISSUE, 1L, AuditAction.CREATED, null, null));
    }

    @Test
    @DisplayName("summaries read like the examples: created / assigned / status / invited / joined")
    void toDto_buildsHumanReadableSummaries() {
        assertSummary("John created issue #42",
                entry("John", AuditEntityType.ISSUE, 42L, AuditAction.CREATED, null, null));
        assertSummary("John assigned issue #42 to Priya",
                entry("John", AuditEntityType.ISSUE, 42L, AuditAction.ASSIGNED, "Unassigned", "Priya"));
        assertSummary("Priya changed issue #42: TODO → IN_PROGRESS",
                entry("Priya", AuditEntityType.ISSUE, 42L, AuditAction.STATUS_CHANGED, "TODO", "IN_PROGRESS"));
        assertSummary("John invited alex@example.com",
                entry("John", AuditEntityType.PROJECT_MEMBER, 7L, AuditAction.INVITED, null, "alex@example.com"));
        assertSummary("Alex joined project",
                entry("Alex", AuditEntityType.PROJECT_MEMBER, 7L, AuditAction.JOINED, null, null));
    }

    private void assertSummary(String expected, AuditLog entry) {
        AuditLogDTO dto = auditService.toDto(entry);
        assertEquals(expected, dto.getSummary());
        assertEquals(entry.getEntityId(), dto.getEntityId());
    }

    private AuditLog entry(String actorName, AuditEntityType type, Long entityId,
                           AuditAction action, String oldValue, String newValue) {
        User actor = user(actorName, actorName.toLowerCase() + "@example.com");
        AuditLog entry = new AuditLog();
        entry.setAuditId(1L);
        entry.setActor(actor);
        entry.setProject(project(9L));
        entry.setEntityType(type);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setOldValue(oldValue);
        entry.setNewValue(newValue);
        entry.setCreatedAt(LocalDateTime.now());
        return entry;
    }

    private User user(String fullName, String email) {
        User user = new User();
        user.setUserId(1L);
        user.setFullName(fullName);
        user.setEmail(email);
        return user;
    }

    private Project project(Long id) {
        Project project = new Project();
        project.setProjectId(id);
        project.setProjectName("P");
        return project;
    }
}
