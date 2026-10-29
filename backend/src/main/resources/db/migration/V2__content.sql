-- Module content : thèmes, cartes et versions de cartes (CLAUDE.md, section 7 ; ADR 0008 et 0009).
-- Les identifiants de thèmes et de cartes sont stables (UUID v8 dérivés de la source) ;
-- ceux des versions sont des UUID v7.

CREATE TABLE theme (
    id         uuid        PRIMARY KEY,
    slug       text        NOT NULL UNIQUE,
    title      text        NOT NULL,
    position   integer     NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE TABLE card (
    id              uuid        PRIMARY KEY,
    theme_id        uuid        NOT NULL REFERENCES theme (id),
    type            text        NOT NULL CHECK (type IN ('FLASHCARD', 'CODE_OUTPUT', 'OPEN_SCENARIO', 'DEFINITION')),
    position        integer     NOT NULL,
    current_version integer     NOT NULL CHECK (current_version >= 1),
    created_at      timestamptz NOT NULL,
    -- Une carte n'est jamais supprimée : la progression des utilisateurs y est rattachée.
    retired_at      timestamptz
);

-- Liste des cartes actives d'un thème, dans l'ordre de la source.
CREATE INDEX card_theme_active_idx ON card (theme_id, position) WHERE retired_at IS NULL;

CREATE TABLE card_version (
    id           uuid        PRIMARY KEY,
    card_id      uuid        NOT NULL REFERENCES card (id),
    version      integer     NOT NULL CHECK (version >= 1),
    prompt       text        NOT NULL,
    answer       text        NOT NULL,
    content_hash text        NOT NULL,
    created_at   timestamptz NOT NULL,
    UNIQUE (card_id, version)
);
