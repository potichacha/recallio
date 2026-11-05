-- Le classé (ADR 0046) : les cotes de difficulté des cartes, la place des joueurs par sujet, les insignes de saison,
-- les duels et la file du direct. Comme les autres modules, pas de clé étrangère vers les tables d'un autre module.

-- La cote Glicko-2 de chaque carte corrigée par le serveur à laquelle quelqu'un a répondu.
CREATE TABLE card_rating (
    card_id      uuid             PRIMARY KEY,
    subject_slug text             NOT NULL,
    rating       double precision NOT NULL,
    deviation    double precision NOT NULL CHECK (deviation > 0),
    volatility   double precision NOT NULL CHECK (volatility > 0),
    answers      integer          NOT NULL CHECK (answers >= 0),
    updated_at   timestamptz      NOT NULL
);

CREATE INDEX card_rating_subject_idx ON card_rating (subject_slug, rating);

-- La place d'un joueur dans un sujet, pour la saison de son dernier duel. La cote ne quitte jamais le serveur.
CREATE TABLE ranked_standing (
    user_id      uuid             NOT NULL,
    subject_slug text             NOT NULL,
    season       text             NOT NULL CHECK (season ~ '^[0-9]{4}-T[1-4]$'),
    rating       double precision NOT NULL,
    deviation    double precision NOT NULL CHECK (deviation > 0),
    volatility   double precision NOT NULL CHECK (volatility > 0),
    season_duels integer          NOT NULL CHECK (season_duels >= 0),
    wins         integer          NOT NULL CHECK (wins >= 0),
    losses       integer          NOT NULL CHECK (losses >= 0),
    draws        integer          NOT NULL CHECK (draws >= 0),
    peak_rating  double precision,
    updated_at   timestamptz      NOT NULL,
    PRIMARY KEY (user_id, subject_slug)
);

-- Le meilleur rang atteint dans une saison finie : une récompense cosmétique.
CREATE TABLE ranked_badge (
    user_id      uuid        NOT NULL,
    subject_slug text        NOT NULL,
    season       text        NOT NULL,
    tier         text        NOT NULL CHECK (tier IN ('BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND', 'MASTER')),
    awarded_at   timestamptz NOT NULL,
    PRIMARY KEY (user_id, subject_slug, season)
);

CREATE TABLE duel (
    id           uuid        PRIMARY KEY,
    subject_slug text        NOT NULL,
    mode         text        NOT NULL CHECK (mode IN ('ASYNC', 'LIVE')),
    status       text        NOT NULL CHECK (status IN ('OPEN', 'FULL', 'FINISHED', 'EXPIRED')),
    season       text        NOT NULL,
    created_at   timestamptz NOT NULL,
    expires_at   timestamptz NOT NULL,
    finished_at  timestamptz
);

-- La recherche d'un adversaire : les duels ouverts d'un sujet.
CREATE INDEX duel_open_idx ON duel (subject_slug, created_at) WHERE status = 'OPEN';

CREATE TABLE duel_question (
    duel_id  uuid    NOT NULL REFERENCES duel (id) ON DELETE CASCADE,
    position integer NOT NULL CHECK (position >= 0),
    card_id  uuid    NOT NULL,
    PRIMARY KEY (duel_id, position)
);

-- La place 0 est celle du joueur qui a ouvert le duel, la place 1 celle de son adversaire.
CREATE TABLE duel_player (
    duel_id          uuid             NOT NULL REFERENCES duel (id) ON DELETE CASCADE,
    user_id          uuid             NOT NULL,
    seat             smallint         NOT NULL CHECK (seat IN (0, 1)),
    rating_before    double precision NOT NULL,
    deviation_before double precision NOT NULL CHECK (deviation_before > 0),
    volatility_before double precision NOT NULL CHECK (volatility_before > 0),
    joined_at        timestamptz      NOT NULL,
    finished_at      timestamptz,
    rating_after     double precision,
    PRIMARY KEY (duel_id, user_id),
    UNIQUE (duel_id, seat)
);

CREATE INDEX duel_player_user_idx ON duel_player (user_id, joined_at DESC);

-- Une question servie à un joueur : servie une seule fois, son temps compte dès cet instant.
CREATE TABLE duel_answer (
    duel_id     uuid        NOT NULL,
    user_id     uuid        NOT NULL,
    position    integer     NOT NULL CHECK (position >= 0),
    served_at   timestamptz NOT NULL,
    answered_at timestamptz,
    correct     boolean     NOT NULL,
    PRIMARY KEY (duel_id, user_id, position),
    FOREIGN KEY (duel_id, user_id) REFERENCES duel_player (duel_id, user_id) ON DELETE CASCADE
);

-- La file du classé en direct : un joueur à la fois, qui se signale régulièrement.
CREATE TABLE ranked_queue (
    user_id         uuid        PRIMARY KEY,
    subject_slug    text        NOT NULL,
    heartbeat_at    timestamptz NOT NULL,
    matched_duel_id uuid
);

CREATE INDEX ranked_queue_waiting_idx ON ranked_queue (subject_slug, heartbeat_at) WHERE matched_duel_id IS NULL;
