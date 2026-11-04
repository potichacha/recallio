-- L'XP par sujet (ADR 0035) : l'XP remplace les points, dans le même registre append-only. Chaque entrée garde le sujet
-- de la carte révisée ; le bonus de l'objectif du jour n'en a pas. Une réussite corrigée par le serveur rapporte un
-- bonus, sous sa propre raison.
-- Rétrocompatible : les entrées existantes restent, sans sujet, et comptent toujours dans le total.

ALTER TABLE points_ledger ADD COLUMN subject_slug text;

ALTER TABLE points_ledger DROP CONSTRAINT points_ledger_reason_check;
ALTER TABLE points_ledger ADD CONSTRAINT points_ledger_reason_check
    CHECK (reason IN ('REVIEW', 'VERIFIED_SUCCESS', 'DAILY_GOAL'));
