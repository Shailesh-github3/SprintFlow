-- ----------------------------------------
-- V2: issue domain constraints + lookup indexes
-- ----------------------------------------
-- Issue status/priority were free-text strings. The API contract allows
-- statuses TODO/IN_PROGRESS/DONE and priorities LOW/MEDIUM/HIGH
-- (see IssueStatus / IssuePriority). Enforce at the DB level so invalid
-- values cannot bypass the application layer.

ALTER TABLE issue
    ADD CONSTRAINT chk_issue_status
    CHECK (issue_status IN ('TODO', 'IN_PROGRESS', 'DONE'));

ALTER TABLE issue
    ADD CONSTRAINT chk_issue_priority
    CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'));

-- High-frequency lookups that previously had no index.
CREATE INDEX idx_invitation_token ON invitation (token);
CREATE INDEX idx_message_chat ON message (chat_chat_id);
