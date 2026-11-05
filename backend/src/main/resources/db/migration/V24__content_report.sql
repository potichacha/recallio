-- Les signalements d'erreur dans le contenu (ADR 0042) : une carte, ou une page du Tour. Comme les autres modules, pas
-- de clé étrangère vers les tables d'un autre module (ADR 0001) : une carte retirée garde ses signalements.
CREATE TABLE content_report (
    id           uuid        PRIMARY KEY,
    user_id      uuid        NOT NULL,
    subject_slug text        NOT NULL,
    card_id      uuid,
    notion_slug  text,
    page         integer     CHECK (page >= 1),
    reason       text        NOT NULL
        CHECK (reason IN ('WRONG_ANSWER', 'UNCLEAR', 'BROKEN_CODE', 'TYPO', 'OTHER')),
    comment      text        NOT NULL CHECK (length(comment) <= 1000),
    created_at   timestamptz NOT NULL,
    resolution   text        CHECK (resolution IN ('FIXED', 'REJECTED')),
    resolved_at  timestamptz,
    -- Une carte, ou une page d'une notion : jamais les deux.
    CHECK ((card_id IS NULL) <> (notion_slug IS NULL)),
    CHECK ((notion_slug IS NULL) = (page IS NULL)),
    CHECK ((resolution IS NULL) = (resolved_at IS NULL))
);

-- La file des admins : les signalements ouverts, du plus ancien au plus récent.
CREATE INDEX content_report_open_idx ON content_report (created_at) WHERE resolution IS NULL;

-- Un utilisateur ne signale qu'une fois un même contenu tant que son signalement est ouvert.
CREATE INDEX content_report_user_open_idx ON content_report (user_id) WHERE resolution IS NULL;
