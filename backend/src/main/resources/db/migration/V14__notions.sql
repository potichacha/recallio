-- Notions du référentiel et cartes à clé explicite (ADR 0025 et 0026). Rétrocompatible : les colonnes ajoutées sont
-- facultatives, et les sujets encore décrits par un document Markdown n'ont ni notion ni clé.

-- Un thème qui disparaît de sa source est retiré, comme une carte : il n'est plus listé et libère son slug, qu'un thème
-- de la nouvelle source peut reprendre (le domaine « collections » reprend « java-collections »).
ALTER TABLE theme ADD COLUMN retired_at timestamptz;
ALTER TABLE theme DROP CONSTRAINT theme_slug_key;
CREATE UNIQUE INDEX theme_slug_active_idx ON theme (slug) WHERE retired_at IS NULL;

CREATE TABLE notion (
    -- Identifiant stable, dérivé du sujet et de l'identifiant de la notion dans son référentiel.
    id            uuid        PRIMARY KEY,
    subject_id    uuid        NOT NULL REFERENCES subject (id),
    slug          text        NOT NULL,
    domain        text        NOT NULL,
    chapter       text        NOT NULL,
    chapter_title text        NOT NULL,
    title         text        NOT NULL,
    level         text        NOT NULL CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'EXPERT')),
    -- 1 essentiel, 2 courant, 3 avancé, 4 rare.
    priority      smallint    NOT NULL CHECK (priority BETWEEN 1 AND 4),
    -- Identifiants des notions prérequises dans le même sujet, vérifiés à l'import et en CI.
    requires      text[]      NOT NULL,
    position      integer     NOT NULL,
    lesson        text,
    created_at    timestamptz NOT NULL,
    updated_at    timestamptz NOT NULL,
    retired_at    timestamptz,
    UNIQUE (subject_id, slug)
);

ALTER TABLE card ADD COLUMN notion_id uuid REFERENCES notion (id);
CREATE INDEX card_notion_idx ON card (notion_id) WHERE notion_id IS NOT NULL;
