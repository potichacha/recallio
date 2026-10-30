-- Catalogue multi-sujets (CLAUDE.md, section 2.5 ; ADR 0013) : chaque thème appartient à un sujet (Java, Go...).
--
-- Les identifiants des thèmes et des cartes incluent désormais le sujet. Le contenu importé avant cette migration
-- (Java seul, identifiants sans sujet) est supprimé : il est réimporté au démarrage avec ses nouveaux identifiants.
-- Aucune donnée de production n'existe encore (rien n'a été déployé) ; en développement, la progression liée à
-- l'ancien contenu devient orpheline et n'est plus proposée (les modules review et gamification ignorent les cartes
-- inconnues du catalogue).

CREATE TABLE subject (
    id         uuid        PRIMARY KEY,
    -- Identifiant stable, dans les URL et l'identité des cartes : ne jamais le modifier.
    slug       text        NOT NULL UNIQUE,
    title      text        NOT NULL,
    category   text        NOT NULL
        CHECK (category IN ('LANGUAGES', 'FRAMEWORKS', 'CLOUD_DEVOPS', 'DATA', 'FUNDAMENTALS', 'AI')),
    position   integer     NOT NULL,
    -- Un brouillon est importé mais invisible des utilisateurs.
    status     text        NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED')),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

DELETE FROM card_version;
DELETE FROM card;
DELETE FROM theme;

ALTER TABLE theme ADD COLUMN subject_id uuid NOT NULL REFERENCES subject (id);

-- Thèmes d'un sujet, dans l'ordre du document.
CREATE INDEX theme_subject_idx ON theme (subject_id, position);
