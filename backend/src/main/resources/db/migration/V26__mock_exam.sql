-- L'examen blanc (ADR 0044) : une épreuve chronométrée sur des questions corrigées par le serveur. Les réponses ne
-- sont pas gardées, seulement la note. Comme les autres tables du module review, pas de clé étrangère vers les cartes
-- (ADR 0001).
CREATE TABLE mock_exam (
    id           uuid        PRIMARY KEY,
    user_id      uuid        NOT NULL,
    subject_slug text        NOT NULL,
    started_at   timestamptz NOT NULL,
    deadline     timestamptz NOT NULL,
    submitted_at timestamptz,
    correct      integer     CHECK (correct >= 0),
    total        integer     CHECK (total >= 0),
    CHECK ((submitted_at IS NULL) = (correct IS NULL)),
    CHECK ((submitted_at IS NULL) = (total IS NULL))
);

-- L'historique d'un sujet : les dernières copies rendues d'abord.
CREATE INDEX mock_exam_user_subject_idx ON mock_exam (user_id, subject_slug, started_at DESC);

-- Les questions d'un examen, dans l'ordre où elles sont posées.
CREATE TABLE mock_exam_question (
    exam_id  uuid    NOT NULL REFERENCES mock_exam (id) ON DELETE CASCADE,
    position integer NOT NULL CHECK (position >= 0),
    card_id  uuid    NOT NULL,
    PRIMARY KEY (exam_id, position)
);
