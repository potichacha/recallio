-- Module group : groupes de révision et leurs membres (CLAUDE.md, section 6.4 ; ADR 0015).
-- Les utilisateurs sont désignés par leur identifiant applicatif (app_user.id), sans clé étrangère vers la table du
-- module user, que ce module ne lit jamais (ADR 0001).

CREATE TABLE study_group (
    id         uuid        PRIMARY KEY,
    name       text        NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE TABLE group_member (
    group_id  uuid        NOT NULL REFERENCES study_group (id) ON DELETE CASCADE,
    user_id   uuid        NOT NULL,
    role      text        NOT NULL CHECK (role IN ('OWNER', 'MEMBER')),
    joined_at timestamptz NOT NULL,
    PRIMARY KEY (group_id, user_id)
);

-- Exactement un créateur par groupe : la base l'interdit, pas seulement le code.
CREATE UNIQUE INDEX group_member_one_owner_idx ON group_member (group_id) WHERE role = 'OWNER';

-- « Mes groupes » : les adhésions d'un utilisateur.
CREATE INDEX group_member_user_idx ON group_member (user_id);
