-- Module review : mémoire des cartes par utilisateur et journal de révision (CLAUDE.md, sections 6.2 et 7 ; ADR 0011).
-- Pas de clé étrangère vers app_user ni card : chaque module possède ses tables (ADR 0001), les identifiants
-- viennent des façades des modules user et content.

-- État FSRS d'une carte pour un utilisateur. Une carte sans ligne est « nouvelle » pour lui.
CREATE TABLE review_state (
    user_id        uuid             NOT NULL,
    card_id        uuid             NOT NULL,
    state          text             NOT NULL CHECK (state IN ('LEARNING', 'REVIEW', 'RELEARNING')),
    -- Étape d'apprentissage en cours (significative en LEARNING et RELEARNING).
    learning_step  integer          NOT NULL CHECK (learning_step >= 0),
    stability      double precision NOT NULL CHECK (stability > 0),
    difficulty     double precision NOT NULL CHECK (difficulty BETWEEN 1 AND 10),
    due_at         timestamptz      NOT NULL,
    reps           integer          NOT NULL CHECK (reps >= 1),
    lapses         integer          NOT NULL CHECK (lapses >= 0),
    last_review_at timestamptz      NOT NULL,
    -- Première révision : sert au quota quotidien de nouvelles cartes.
    introduced_at  timestamptz      NOT NULL,
    PRIMARY KEY (user_id, card_id)
);

CREATE INDEX review_state_user_introduced_idx ON review_state (user_id, introduced_at);

-- Journal append-only : une ligne par réponse, jamais modifiée (réentraînement FSRS, statistiques).
CREATE TABLE review_log (
    id              uuid             PRIMARY KEY,
    -- Identifiant choisi par le client : rejouer une requête ne crée pas de seconde révision.
    request_id      uuid             NOT NULL UNIQUE,
    user_id         uuid             NOT NULL,
    card_id         uuid             NOT NULL,
    card_version_id uuid             NOT NULL,
    -- Note FSRS : 1 = à revoir, 2 = difficile, 3 = bien, 4 = facile.
    rating          smallint         NOT NULL CHECK (rating BETWEEN 1 AND 4),
    reviewed_at     timestamptz      NOT NULL,
    elapsed_ms      integer          CHECK (elapsed_ms >= 0),
    -- État obtenu après la révision.
    state           text             NOT NULL CHECK (state IN ('LEARNING', 'REVIEW', 'RELEARNING')),
    stability       double precision NOT NULL,
    difficulty      double precision NOT NULL,
    due_at          timestamptz      NOT NULL
);

CREATE INDEX review_log_user_reviewed_idx ON review_log (user_id, reviewed_at);

-- Garantie en base du caractère append-only. La suppression reste possible (droit à l'effacement, RGPD).
CREATE FUNCTION review_log_forbid_update() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'review_log est append-only : une révision enregistrée ne se modifie pas';
END;
$$;

CREATE TRIGGER review_log_append_only
    BEFORE UPDATE ON review_log
    FOR EACH ROW EXECUTE FUNCTION review_log_forbid_update();
