-- V1: core tables for teams, users, categories and tickets.

CREATE TABLE team (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE app_user (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email              VARCHAR(255) NOT NULL UNIQUE,
    full_name          VARCHAR(150) NOT NULL,
    preferred_language VARCHAR(2)   NOT NULL DEFAULT 'fr'
                       CHECK (preferred_language IN ('fr', 'ar', 'en')),
    team_id            BIGINT REFERENCES team (id)
);

CREATE TABLE category (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name_fr VARCHAR(100) NOT NULL,
    name_ar VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    team_id BIGINT NOT NULL REFERENCES team (id)
);

CREATE TABLE ticket (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reference    VARCHAR(20)  NOT NULL UNIQUE,
    title        VARCHAR(200) NOT NULL,
    description  TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'NEW'
                 CHECK (status IN ('NEW', 'ASSIGNED', 'IN_PROGRESS',
                                   'WAITING_FOR_USER', 'RESOLVED', 'CLOSED')),
    priority     VARCHAR(2)   NOT NULL DEFAULT 'P3'
                 CHECK (priority IN ('P1', 'P2', 'P3', 'P4')),
    category_id  BIGINT       NOT NULL REFERENCES category (id),
    requester_id BIGINT       NOT NULL REFERENCES app_user (id),
    assignee_id  BIGINT       REFERENCES app_user (id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_ticket_status    ON ticket (status);
CREATE INDEX idx_ticket_requester ON ticket (requester_id);
CREATE INDEX idx_ticket_assignee  ON ticket (assignee_id);