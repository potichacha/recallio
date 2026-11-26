-- La révision intensive (ADR 0078) : des questions sans fin, corrigées par le serveur, qui ne laissent rien dans FSRS, le
-- journal de révision, l'XP ni la série. Une seule chose est gardée : combien de réponses un compte a fait corriger
-- chaque jour, et combien étaient justes. Ni la carte, ni la réponse, ni l'heure. Ce compteur servira la limite
-- quotidienne des comptes gratuits (ADR 0077) et dit à la page « Bêta » qui s'entraîne (ADR 0076). Comme les autres tables
-- du module review, pas de clé étrangère vers les utilisateurs (ADR 0001).

CREATE TABLE drill_activity (
    user_id    uuid    NOT NULL,
    -- Le jour de l'utilisateur, dans son fuseau, comme dans daily_activity.
    local_date date    NOT NULL,
    answers    integer NOT NULL CHECK (answers > 0),
    correct    integer NOT NULL CHECK (correct >= 0 AND correct <= answers),
    PRIMARY KEY (user_id, local_date)
);

-- Les comptes qui se sont entraînés sur une période, pour la page « Bêta ».
CREATE INDEX drill_activity_local_date_idx ON drill_activity (local_date);
