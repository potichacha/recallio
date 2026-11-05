-- La dernière page du Tour qu'un utilisateur a ouverte, par sujet (ADR 0037) : le sommaire lui propose d'y reprendre.
-- Le sujet est désigné par son slug, définitif, comme dans followed_subject ; comme les autres tables du module review,
-- pas de clé étrangère vers celles des autres modules (ADR 0001).

CREATE TABLE tour_last_page (
    user_id      uuid        NOT NULL,
    subject_slug text        NOT NULL,
    notion_id    uuid        NOT NULL,
    -- Le rang de la page dans sa notion, à partir de 1.
    page         integer     NOT NULL CHECK (page >= 1),
    opened_at    timestamptz NOT NULL,
    PRIMARY KEY (user_id, subject_slug)
);
