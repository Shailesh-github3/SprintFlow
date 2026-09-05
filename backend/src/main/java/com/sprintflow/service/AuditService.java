package com.sprintflow.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sprintflow.dto.AuditLogDTO;
import com.sprintflow.entity.AuditAction;
import com.sprintflow.entity.AuditEntityType;
import com.sprintflow.entity.AuditLog;
import com.sprintflow.entity.Project;
import com.sprintflow.entity.User;
import com.sprintflow.exception.ResourceNotFoundException;
import com.sprintflow.repository.AuditLogRepository;
import com.sprintflow.repository.ProjectRepository;
import com.sprintflow.repository.UserRepository;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository,
                        ProjectRepository projectRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * Records one audit entry. Runs in the caller's transaction, so a failed
     * business operation never leaves a phantom log line behind.
     */
    @Transactional
    public AuditLog log(String actorEmail, Long projectId, AuditEntityType entityType,
                        Long entityId, AuditAction action, String oldValue, String newValue) {
        User actor = userRepository.findByEmail(actorEmail);
        if (actor == null) {
            throw new IllegalArgumentException("Unknown actor: " + actorEmail);
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
        AuditLog entry = new AuditLog();
        entry.setActor(actor);
        entry.setProject(project);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setOldValue(oldValue);
        entry.setNewValue(newValue);
        entry.setCreatedAt(LocalDateTime.now());
        return auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getActivityForProject(Long projectId, Pageable pageable) {
        return auditLogRepository.findByProject_ProjectId(projectId, pageable);
    }

    public AuditLogDTO toDto(AuditLog entry) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setAuditId(entry.getAuditId());
        dto.setActorName(entry.getActor() != null ? entry.getActor().getFullName() : "Unknown");
        dto.setActorEmail(entry.getActor() != null ? entry.getActor().getEmail() : null);
        dto.setEntityType(entry.getEntityType() != null ? entry.getEntityType().name() : null);
        dto.setEntityId(entry.getEntityId());
        dto.setAction(entry.getAction() != null ? entry.getAction().name() : null);
        dto.setOldValue(entry.getOldValue());
        dto.setNewValue(entry.getNewValue());
        dto.setCreatedAt(entry.getCreatedAt());
        dto.setSummary(buildSummary(dto.getActorName(), entry.getEntityType(), entry.getEntityId(),
                entry.getAction(), entry.getOldValue(), entry.getNewValue()));
        return dto;
    }

    static String buildSummary(String actorName, AuditEntityType entityType, Long entityId,
                               AuditAction action, String oldValue, String newValue) {
        if (entityType == AuditEntityType.ISSUE && action == AuditAction.CREATED) {
            return actorName + " created issue #" + entityId;
        }
        if (entityType == AuditEntityType.ISSUE && action == AuditAction.ASSIGNED) {
            return actorName + " assigned issue #" + entityId + " to " + newValue;
        }
        if (entityType == AuditEntityType.ISSUE && action == AuditAction.STATUS_CHANGED) {
            return actorName + " changed issue #" + entityId + ": " + oldValue + " → " + newValue;
        }
        if (entityType == AuditEntityType.PROJECT_MEMBER && action == AuditAction.INVITED) {
            return actorName + " invited " + newValue;
        }
        if (entityType == AuditEntityType.PROJECT_MEMBER && action == AuditAction.JOINED) {
            return actorName + " joined project";
        }
        return actorName + " performed " + action + " on " + entityType + " #" + entityId;
    }
}
