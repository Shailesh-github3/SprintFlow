-- ----------------------------------------
-- V3: minimal audit log
-- ----------------------------------------
-- Append-only history of who did what per project (issue created /
-- assigned / status-changed, member invited / joined). old_value and
-- new_value snapshot the human-readable before/after (e.g. assignee
-- names, statuses) so history stays meaningful if rows are renamed.

CREATE TABLE audit_log (
    audit_id           BIGINT       NOT NULL AUTO_INCREMENT,
    created_at         DATETIME(6),
    entity_type        VARCHAR(255),
    entity_id          BIGINT,
    action             VARCHAR(255),
    old_value          VARCHAR(500),
    new_value          VARCHAR(500),
    actor_user_id      BIGINT,
    project_project_id BIGINT,
    PRIMARY KEY (audit_id),
    CONSTRAINT fk_audit_actor   FOREIGN KEY (actor_user_id) REFERENCES user (user_id),
    CONSTRAINT fk_audit_project FOREIGN KEY (project_project_id) REFERENCES project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_project ON audit_log (project_project_id, created_at);
