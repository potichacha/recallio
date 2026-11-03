-- Le parcours choisi pour chaque sujet suivi (ADR 0032) : apprendre, préparer un entretien ou entretenir son niveau.
-- Il change l'ordre et le choix des nouvelles cartes de ce sujet. Rétrocompatible : les sujets déjà suivis passent en
-- « préparer un entretien », le parcours par défaut.

ALTER TABLE followed_subject ADD COLUMN track text NOT NULL DEFAULT 'PREPARE';
ALTER TABLE followed_subject ADD CONSTRAINT followed_subject_track_check
    CHECK (track IN ('LEARN', 'PREPARE', 'MAINTAIN'));
