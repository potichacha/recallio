-- La date d'un entretien, pour un sujet suivi (ADR 0066). Le compte à rebours et son plan par semaine se recalculent à
-- chaque lecture, d'après la maîtrise des notions : seuls la date, le jour où elle a été fixée et la ligne de départ
-- sont gardés. Arrêter de suivre le sujet garde la ligne : elle revient si on le suit de nouveau. Le sujet est désigné
-- par son slug, définitif, comme dans followed_subject ; comme les autres tables du module review, pas de clé
-- étrangère vers celles des autres modules (ADR 0001).

CREATE TABLE interview_plan (
    user_id            uuid    NOT NULL,
    subject_slug       text    NOT NULL,
    -- Le jour de l'entretien.
    interview_on       date    NOT NULL,
    -- Le jour, dans le fuseau de l'utilisateur, où la date a été fixée ou changée : la semaine 1 commence ce jour-là.
    planned_on         date    NOT NULL,
    -- La ligne de départ : les notions essentielles du sujet pas encore acquises ce jour-là.
    notions_to_acquire integer NOT NULL CHECK (notions_to_acquire >= 0),
    CHECK (interview_on > planned_on),
    PRIMARY KEY (user_id, subject_slug)
);
