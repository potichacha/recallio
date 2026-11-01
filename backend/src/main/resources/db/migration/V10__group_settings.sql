-- Réglages d'un groupe (CLAUDE.md, section 6.4 ; ADR 0017). Une ligne n'existe qu'une fois les réglages modifiés : un
-- groupe sans ligne a les réglages par défaut (aucun sujet, tous les formats, objectif de 10 cartes, classement nominatif).
-- Les sujets et les thèmes sont désignés par leur slug, sans clé étrangère vers les tables du module content (ADR 0001).

CREATE TABLE group_settings (
    group_id               uuid        PRIMARY KEY REFERENCES study_group (id) ON DELETE CASCADE,
    subject_slugs          text[]      NOT NULL,
    theme_slugs            text[]      NOT NULL,
    card_types             text[]      NOT NULL,
    daily_goal             integer     NOT NULL CHECK (daily_goal BETWEEN 1 AND 200),
    leaderboard_visibility text        NOT NULL CHECK (leaderboard_visibility IN ('NAMED', 'ANONYMOUS', 'HIDDEN')),
    updated_at             timestamptz NOT NULL
);
