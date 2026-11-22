-- ============================================================
-- Flyway Migration: V4__cascade_audit_project_and_issue_metadata.sql
-- Description: Cascade delete on audit log, issue metadata, and notification schema
-- ============================================================

-- 1. Cascade delete on audit log when project is deleted
ALTER TABLE audit_log DROP FOREIGN KEY fk_audit_project;
ALTER TABLE audit_log ADD CONSTRAINT fk_audit_project 
    FOREIGN KEY (project_project_id) REFERENCES project (project_id) ON DELETE CASCADE;

-- 2. Add metadata columns to issue (creator, createdAt, updatedAt)
ALTER TABLE issue 
    ADD COLUMN created_by_user_id BIGINT,
    ADD COLUMN created_at DATETIME(6),
    ADD COLUMN updated_at DATETIME(6);

ALTER TABLE issue 
    ADD CONSTRAINT fk_issue_created_by FOREIGN KEY (created_by_user_id) REFERENCES user (user_id) ON DELETE SET NULL;

-- 3. Notifications table
CREATE TABLE notification (
    notification_id   BIGINT        NOT NULL AUTO_INCREMENT,
    recipient_user_id BIGINT        NOT NULL,
    title             VARCHAR(255)  NOT NULL,
    message           VARCHAR(1000) NOT NULL,
    read_status       BIT           NOT NULL DEFAULT 0,
    link              VARCHAR(255),
    created_at        DATETIME(6)   NOT NULL,
    PRIMARY KEY (notification_id),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_user_id) REFERENCES user (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notification_recipient_read ON notification (recipient_user_id, read_status, created_at);
