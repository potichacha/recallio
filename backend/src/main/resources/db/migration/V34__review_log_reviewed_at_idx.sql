-- La page « Bêta » des admins (ADR 0076) lit le journal de révision sur une fenêtre de trente jours, tous utilisateurs
-- confondus : les comptes actifs, les réponses par sujet, les cartes les plus ratées. L'index existant
-- (user_id, reviewed_at) sert les lectures d'un utilisateur, pas celles-ci : sans index sur la date seule, chaque
-- ouverture de la page relirait tout le journal, la seule table qui grossit d'une ligne par réponse.
-- Aucune donnée nouvelle : ni table, ni colonne.
CREATE INDEX review_log_reviewed_at_idx ON review_log (reviewed_at);
