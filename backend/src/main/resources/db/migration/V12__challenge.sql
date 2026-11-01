-- Défis de groupe (CLAUDE.md, section 6.4 ; ADR 0020) : une série de QCM commune, un délai par carte mesuré par le
-- serveur. Les cartes et les membres sont désignés par leur identifiant, sans clé étrangère vers les tables des modules
-- content et user (ADR 0001).

CREATE TABLE challenge (
    id               uuid        PRIMARY KEY,
    group_id         uuid        NOT NULL REFERENCES study_group (id) ON DELETE CASCADE,
    title            text        NOT NULL,
    seconds_per_card integer     NOT NULL CHECK (seconds_per_card BETWEEN 10 AND 120),
    created_by       uuid        NOT NULL,
    created_at       timestamptz NOT NULL,
    ends_at          timestamptz NOT NULL CHECK (ends_at > created_at)
);

CREATE INDEX challenge_group_idx ON challenge (group_id, created_at DESC);

-- La série tirée à la création : la même pour tous les membres, dans le même ordre.
CREATE TABLE challenge_card (
    challenge_id uuid    NOT NULL REFERENCES challenge (id) ON DELETE CASCADE,
    position     integer NOT NULL CHECK (position >= 0),
    card_id      uuid    NOT NULL,
    PRIMARY KEY (challenge_id, position)
);

-- Une ligne par carte présentée à un membre : l'heure de présentation fait foi pour le délai.
CREATE TABLE challenge_answer (
    challenge_id     uuid        NOT NULL REFERENCES challenge (id) ON DELETE CASCADE,
    user_id          uuid        NOT NULL,
    position         integer     NOT NULL,
    presented_at     timestamptz NOT NULL,
    answered_at      timestamptz,
    correct          boolean,
    timed_out        boolean     NOT NULL DEFAULT false,
    PRIMARY KEY (challenge_id, user_id, position),
    FOREIGN KEY (challenge_id, position) REFERENCES challenge_card (challenge_id, position) ON DELETE CASCADE,
    CHECK ((answered_at IS NULL) = (correct IS NULL))
);
