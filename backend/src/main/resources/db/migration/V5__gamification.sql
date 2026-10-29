-- Module gamification : points et activité quotidienne (CLAUDE.md, sections 6.3 et 7 ; ADR 0012).
-- Comme pour review, pas de clé étrangère vers les tables des autres modules (ADR 0001).

-- Registre append-only : le total d'un utilisateur est la somme de ses lignes.
CREATE TABLE points_ledger (
    id          uuid        PRIMARY KEY,
    user_id     uuid        NOT NULL,
    amount      integer     NOT NULL CHECK (amount > 0),
    reason      text        NOT NULL CHECK (reason IN ('REVIEW', 'DAILY_GOAL')),
    -- Ce qui a rapporté les points (identifiant de la révision) : une même source ne paie qu'une fois par raison.
    source_id   uuid        NOT NULL,
    occurred_at timestamptz NOT NULL,
    UNIQUE (user_id, reason, source_id)
);

CREATE FUNCTION points_ledger_forbid_update() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'points_ledger est append-only : des points attribués ne se modifient pas';
END;
$$;

CREATE TRIGGER points_ledger_append_only
    BEFORE UPDATE ON points_ledger
    FOR EACH ROW EXECUTE FUNCTION points_ledger_forbid_update();

-- Une ligne par utilisateur et par jour local (fuseau de l'utilisateur au moment de la révision) : base du streak.
CREATE TABLE daily_activity (
    user_id        uuid    NOT NULL,
    local_date     date    NOT NULL,
    cards_reviewed integer NOT NULL CHECK (cards_reviewed >= 0),
    -- Figé avec l'objectif en vigueur : changer l'objectif ne réécrit pas le passé.
    goal_reached   boolean NOT NULL,
    PRIMARY KEY (user_id, local_date)
);
