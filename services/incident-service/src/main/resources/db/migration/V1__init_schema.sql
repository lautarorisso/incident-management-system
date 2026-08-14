-- V1: Schema base
-- Tablas incidents y outbox_events, creadas por Flyway para que
-- ddl-auto=validate funcione sobre bases frescas.
--
-- CREATE TABLE IF NOT EXISTS es deliberado: en ambientes pre-existentes
-- (schema creado por Hibernate antes de adoptar Flyway) la migración es
-- no-op y no rompe el boot después de un flyway repair.

CREATE TABLE IF NOT EXISTS incidents (
    id          UUID         NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(31)  NOT NULL,
    priority    VARCHAR(31)  NOT NULL,
    assignee_id UUID,
    team_id     UUID,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_incidents PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS outbox_events (
    id           UUID        NOT NULL,
    aggregate_id UUID        NOT NULL,
    event_type   VARCHAR(255) NOT NULL,
    payload      TEXT,
    published    BOOLEAN     NOT NULL DEFAULT false,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_outbox_events PRIMARY KEY (id)
);
