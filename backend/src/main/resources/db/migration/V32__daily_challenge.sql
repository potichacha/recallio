-- Le défi du jour (ADR 0067) : cinq questions par sujet et par jour, les mêmes pour tous. Un défi est créé à la première
-- demande du jour pour son sujet, puis gardé : une correction du contenu dans la journée ne le change pas. Le jour est
-- celui de chaque joueur, dans son fuseau, comme Wordle. Comme les autres tables du module review, pas de clé étrangère
-- vers les cartes ni vers les utilisateurs (ADR 0001).

CREATE TABLE daily_challenge (
    id           uuid        PRIMARY KEY,
    -- Le sujet, par son slug définitif, comme dans followed_subject.
    subject_slug text        NOT NULL,
    day          date        NOT NULL,
    created_at   timestamptz NOT NULL,
    -- Un défi par sujet et par jour : deux premières demandes simultanées gardent le même (ON CONFLICT DO NOTHING).
    -- L'index sert aussi à retrouver les derniers défis d'un sujet, dont les cartes ne reviennent pas.
    UNIQUE (subject_slug, day)
);

-- Les questions d'un défi, dans l'ordre où elles sont posées, de la plus facile à la plus difficile.
CREATE TABLE daily_challenge_question (
    challenge_id uuid    NOT NULL REFERENCES daily_challenge (id) ON DELETE CASCADE,
    position     integer NOT NULL CHECK (position >= 0),
    card_id      uuid    NOT NULL,
    PRIMARY KEY (challenge_id, position)
);

-- Les réponses d'un joueur : une seule par question, définitive. Seul son verdict est gardé, de quoi compter le score et
-- les chiffres du jour. request_id rend l'envoi idempotent (CLAUDE.md, section 4.4).
CREATE TABLE daily_challenge_answer (
    challenge_id uuid        NOT NULL,
    user_id      uuid        NOT NULL,
    position     integer     NOT NULL,
    correct      boolean     NOT NULL,
    answered_at  timestamptz NOT NULL,
    request_id   uuid        NOT NULL UNIQUE,
    PRIMARY KEY (challenge_id, user_id, position),
    FOREIGN KEY (challenge_id, position) REFERENCES daily_challenge_question (challenge_id, position) ON DELETE CASCADE
);

-- Les défis d'un joueur, pour son tableau de bord, son export et la suppression de son compte (ADR 0056).
CREATE INDEX daily_challenge_answer_user_idx ON daily_challenge_answer (user_id, challenge_id);
