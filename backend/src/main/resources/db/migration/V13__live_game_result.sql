-- Résultats des parties live (CLAUDE.md, section 6.5 ; ADR 0023). Les parties vivent en mémoire (ADR 0021) ; seul leur
-- résultat final est conservé. Groupes, sujets et joueurs sont désignés par leur identifiant ou leur slug, sans clé
-- étrangère vers les tables des autres modules (ADR 0001).

CREATE TABLE live_game (
    id             uuid        PRIMARY KEY,
    group_id       uuid        NOT NULL,
    subject_slug   text        NOT NULL,
    host_id        uuid        NOT NULL,
    started_at     timestamptz NOT NULL,
    finished_at    timestamptz NOT NULL CHECK (finished_at >= started_at),
    question_count integer     NOT NULL CHECK (question_count > 0)
);

-- « Les dernières parties du groupe ».
CREATE INDEX live_game_group_idx ON live_game (group_id, finished_at DESC);

CREATE TABLE live_game_player (
    game_id       uuid    NOT NULL REFERENCES live_game (id) ON DELETE CASCADE,
    user_id       uuid    NOT NULL,
    display_name  text    NOT NULL,
    rank          integer NOT NULL CHECK (rank >= 1),
    score         integer NOT NULL CHECK (score >= 0),
    correct_count integer NOT NULL CHECK (correct_count >= 0),
    PRIMARY KEY (game_id, user_id)
);
