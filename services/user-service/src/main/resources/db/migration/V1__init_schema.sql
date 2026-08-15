-- V1: Schema base para user_db (users, teams y la join table de @ElementCollection).
-- Creada por Flyway para que ddl-auto=validate funcione sobre bases frescas.

CREATE TABLE IF NOT EXISTS users (
    id           UUID         NOT NULL,
    keycloak_id  UUID         NOT NULL,
    username     VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ,
    updated_at   TIMESTAMPTZ,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_keycloak_id UNIQUE (keycloak_id),
    CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS teams (
    id          UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ,
    updated_at  TIMESTAMPTZ,
    CONSTRAINT pk_teams PRIMARY KEY (id),
    CONSTRAINT uk_teams_name UNIQUE (name)
);

-- Join table generada por @ElementCollection List<UUID> teamIds en User.
CREATE TABLE IF NOT EXISTS users_team_ids (
    users_id UUID NOT NULL,
    team_id  UUID NOT NULL,
    CONSTRAINT pk_users_team_ids PRIMARY KEY (users_id, team_id),
    CONSTRAINT fk_users_team_ids_users FOREIGN KEY (users_id) REFERENCES users (id),
    CONSTRAINT fk_users_team_ids_teams FOREIGN KEY (team_id) REFERENCES teams (id)
);
