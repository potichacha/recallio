-- Les quêtes du jour et de la semaine, et les succès (ADR 0040). Comme les autres tables de la gamification, pas de clé
-- étrangère vers celles des autres modules (ADR 0001).

-- La progression de chaque quête, par utilisateur et par période : le jour, ou le lundi de la semaine, dans le fuseau
-- de l'utilisateur. Une quête qui n'a pas commencé n'a pas de ligne.
CREATE TABLE quest_progress (
    user_id      uuid        NOT NULL,
    quest        text        NOT NULL,
    period_start date        NOT NULL,
    progress     integer     NOT NULL CHECK (progress >= 0),
    completed_at timestamptz,
    PRIMARY KEY (user_id, quest, period_start)
);

-- Ce qu'une quête ne doit compter qu'une fois par période : un format de carte, un jour actif, une page du Tour.
CREATE TABLE quest_seen (
    user_id      uuid NOT NULL,
    period_start date NOT NULL,
    item         text NOT NULL,
    PRIMARY KEY (user_id, period_start, item)
);

-- Les succès, débloqués une seule fois.
CREATE TABLE achievement (
    user_id     uuid        NOT NULL,
    code        text        NOT NULL,
    unlocked_at timestamptz NOT NULL,
    PRIMARY KEY (user_id, code)
);

-- Une quête terminée rapporte de l'XP, sans sujet, comme le bonus de l'objectif du jour.
ALTER TABLE points_ledger DROP CONSTRAINT points_ledger_reason_check;
ALTER TABLE points_ledger ADD CONSTRAINT points_ledger_reason_check
    CHECK (reason IN ('REVIEW', 'VERIFIED_SUCCESS', 'DAILY_GOAL', 'QUEST'));
