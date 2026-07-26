-- ============================================================
-- Flyway Migration: V1__initial_schema.sql
-- Description: Initial database schema for SprintFlow
-- This migration creates all tables matching the JPA entity definitions.
-- ============================================================

-- ----------------------------------------
-- Table: user
-- Entity: User
-- Stores registered users of the application.
-- ----------------------------------------
CREATE TABLE user (
    user_id       BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255),
    password      VARCHAR(255) NOT NULL,
    project_size  INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_user_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------
-- Table: user_roles
-- ElementCollection in User entity
-- Stores role assignments for each user (e.g., ROLE_USER, ROLE_ADMIN).
-- ----------------------------------------
CREATE TABLE user_roles (
    user_id BIGINT       NOT NULL,
    role    VARCHAR(255),
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------
-- Table: project
-- Entity: Project
-- Stores project information created by users.
-- ----------------------------------------
CREATE TABLE project (
    project_id          BIGINT       NOT NULL AUTO_INCREMENT,
    category            VARCHAR(255),
    project_description VARCHAR(255),
    project_name        VARCHAR(255),
    project_owner_user_id BIGINT,
    PRIMARY KEY (project_id),
    CONSTRAINT fk_project_owner FOREIGN KEY (project_owner_user_id) REFERENCES user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------
-- Table: project_tags
-- ElementCollection in Project entity
-- Stores tags associated with each project.
-- ----------------------------------------
CREATE TABLE project_tags (
    project_id BIGINT       NOT NULL,
    tags       VARCHAR(255),
    CONSTRAINT fk_project_tags_project FOREIGN KEY (project_id) REFERENCES project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------
-- Table: project_project_members
-- ManyToMany join table: Project <-> User
-- Maps which users are members of which projects.
-- ----------------------------------------
CREATE TABLE project_project_members (
    project_project_id      BIGINT NOT NULL,
    project_members_user_id BIGINT NOT NULL,
    PRIMARY KEY (project_project_id, project_members_user_id),
    CONSTRAINT fk_pm_project FOREIGN KEY (project_project_id) REFERENCES project (project_id),
    CONSTRAINT fk_pm_user    FOREIGN KEY (project_members_user_id) REFERENCES user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------
-- Table: issue
-- Entity: Issue
-- Stores tasks/issues within a project, assigned to users.
-- ----------------------------------------
CREATE TABLE issue (
    issue_id             BIGINT       NOT NULL AUTO_INCREMENT,
    due_date             DATE,
    issue_description    VARCHAR(255),
    issue_status         VARCHAR(255),
    issue_title          VARCHAR(255),
    priority             VARCHAR(255),
    assigned_user_user_id BIGINT,
    project_project_id   BIGINT,
    PRIMARY KEY (issue_id),
    CONSTRAINT fk_issue_assigned_user FOREIGN KEY (assigned_user_user_id) REFERENCES user (user_id),
    CONSTRAINT fk_issue_project       FOREIGN KEY (project_project_id) REFERENCES project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------
-- Table: issue_tags
-- ElementCollection in Issue entity
-- Stores tags associated with each issue.
-- ----------------------------------------
CREATE TABLE issue_tags (
    issue_id BIGINT       NOT NULL,
    tags     VARCHAR(255),
    CONSTRAINT fk_issue_tags_issue FOREIGN KEY (issue_id) REFERENCES issue (issue_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------
-- Table: comment
-- Entity: Comment
-- Stores comments on issues, created by users.
-- ----------------------------------------
CREATE TABLE comment (
    comment_id        BIGINT       NOT NULL AUTO_INCREMENT,
    comment_text      VARCHAR(255),
    created_at        DATETIME(6),
    created_by_user_id BIGINT,
    issue_issue_id    BIGINT,
    PRIMARY KEY (comment_id),
    CONSTRAINT fk_comment_created_by FOREIGN KEY (created_by_user_id) REFERENCES user (user_id),
    CONSTRAINT fk_comment_issue      FOREIGN KEY (issue_issue_id) REFERENCES issue (issue_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------
-- Table: chat
-- Entity: Chat
-- One-to-one with Project. Stores chat rooms for project collaboration.
-- ----------------------------------------
CREATE TABLE chat (
    chat_id            BIGINT NOT NULL AUTO_INCREMENT,
    chat_name          VARCHAR(255),
    project_project_id BIGINT,
    PRIMARY KEY (chat_id),
    CONSTRAINT uk_chat_project UNIQUE (project_project_id),
    CONSTRAINT fk_chat_project FOREIGN KEY (project_project_id) REFERENCES project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------
-- Table: chat_participants
-- ManyToMany join table: Chat <-> User
-- Maps which users are participants in which chats.
-- ----------------------------------------
CREATE TABLE chat_participants (
    chat_chat_id        BIGINT NOT NULL,
    participants_user_id BIGINT NOT NULL,
    PRIMARY KEY (chat_chat_id, participants_user_id),
    CONSTRAINT fk_cp_chat FOREIGN KEY (chat_chat_id) REFERENCES chat (chat_id),
    CONSTRAINT fk_cp_user FOREIGN KEY (participants_user_id) REFERENCES user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------
-- Table: message
-- Entity: Message
-- Stores messages within a chat, sent by users.
-- ----------------------------------------
CREATE TABLE message (
    message_id    BIGINT       NOT NULL AUTO_INCREMENT,
    created_at    DATETIME(6),
    message_text  VARCHAR(255),
    chat_chat_id  BIGINT,
    sender_user_id BIGINT,
    PRIMARY KEY (message_id),
    CONSTRAINT fk_message_chat   FOREIGN KEY (chat_chat_id) REFERENCES chat (chat_id),
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_user_id) REFERENCES user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------
-- Table: invitation
-- Entity: Invitation
-- Stores project invitation tokens sent via email.
-- ----------------------------------------
CREATE TABLE invitation (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    created_at  DATETIME(6),
    expiry_date DATETIME(6),
    project_id  BIGINT,
    token       VARCHAR(255),
    used        BIT          NOT NULL DEFAULT 0,
    user_email  VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------
-- Table: subscription
-- Entity: Subscription
-- Stores user subscription plans (FREE, MONTHLY, YEARLY).
-- One-to-one with User.
-- ----------------------------------------
CREATE TABLE subscription (
    subscription_id BIGINT       NOT NULL AUTO_INCREMENT,
    end_date        DATE,
    is_valid        BIT          NOT NULL DEFAULT 0,
    plan_type       VARCHAR(255),
    start_date      DATE,
    user_user_id    BIGINT,
    PRIMARY KEY (subscription_id),
    CONSTRAINT uk_subscription_user UNIQUE (user_user_id),
    CONSTRAINT fk_subscription_user FOREIGN KEY (user_user_id) REFERENCES user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;