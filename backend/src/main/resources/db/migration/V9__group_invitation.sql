-- Lien d'invitation d'un groupe (CLAUDE.md, section 6.4 ; ADR 0016). Un seul lien actif par groupe : le créateur le
-- remplace (l'ancien cesse de fonctionner) ou le désactive. Le jeton est aléatoire (192 bits) et expire.

CREATE TABLE group_invitation (
    group_id   uuid        PRIMARY KEY REFERENCES study_group (id) ON DELETE CASCADE,
    token      text        NOT NULL UNIQUE,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL
);
