-- La progression du Tour (ADR 0037) : les notions qu'un utilisateur a validées en répondant juste à leurs questions.
-- Elle reste à part du journal de révision : ces réponses ne passent pas par FSRS. Comme les autres tables du module
-- review, pas de clé étrangère vers celles des autres modules (ADR 0001).

CREATE TABLE tour_progress (
    user_id      uuid        NOT NULL,
    notion_id    uuid        NOT NULL,
    -- La première validation : valider de nouveau ne change rien.
    validated_at timestamptz NOT NULL,
    PRIMARY KEY (user_id, notion_id)
);
