-- Le test de niveau (ADR 0068) : les notions débutantes qu'un utilisateur a montré connaître, domaine par domaine. Une
-- notion connue n'est pas acquise : elle ouvre le parcours « apprendre » (ni Tour à valider, ni prérequis qui manque) et
-- ses cartes passent après les autres, mais ni la maîtrise, ni FSRS ne la voient. Comme tour_progress, elle reste à part
-- du journal de révision, et comme les autres tables du module review, sans clé étrangère vers celles des autres modules
-- (ADR 0001).

CREATE TABLE known_notion (
    user_id      uuid        NOT NULL,
    notion_id    uuid        NOT NULL,
    -- Le sujet de la notion, par son slug définitif : annuler le test d'un sujet retire toutes ses notions connues.
    subject_slug text        NOT NULL,
    -- Le premier domaine réussi qui l'a rendue connue : refaire le test ne change rien.
    known_at     timestamptz NOT NULL,
    PRIMARY KEY (user_id, notion_id)
);

-- Les notions connues d'un sujet, pour la file de révision, le Tour et l'annulation du test.
CREATE INDEX known_notion_subject_idx ON known_notion (user_id, subject_slug);
