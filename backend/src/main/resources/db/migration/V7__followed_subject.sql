-- Module user : les sujets que suit chaque utilisateur (CLAUDE.md, sections 2.5 et 6.1 ; ADR 0014).
-- Ils alimentent sa file de révision. Le sujet est désigné par son slug, définitif par construction (ADR 0013) :
-- pas de clé étrangère vers la table subject du module content, que ce module ne lit jamais (ADR 0001).
-- Arrêter de suivre un sujet supprime seulement la ligne : la progression (review_state) est conservée.

CREATE TABLE followed_subject (
    user_id      uuid        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    subject_slug text        NOT NULL,
    followed_at  timestamptz NOT NULL,
    PRIMARY KEY (user_id, subject_slug)
);
