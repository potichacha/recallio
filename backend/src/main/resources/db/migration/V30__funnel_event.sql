-- Le parcours mesuré (ADR 0064) : une visite de la page publique, un essai commencé, un compte créé, une première
-- révision. Aucune donnée personnelle : ni identifiant d'utilisateur, ni adresse, ni cookie. Une ligne ne dit que
-- l'étape franchie et l'instant ; rien à exporter ni à supprimer avec un compte (ADR 0056).
CREATE TABLE funnel_event (
    id          uuid        PRIMARY KEY,
    step        text        NOT NULL CHECK (step IN ('VISIT', 'TRIAL', 'ACCOUNT', 'FIRST_REVIEW')),
    occurred_at timestamptz NOT NULL
);

-- Le rapport compte les événements semaine par semaine.
CREATE INDEX funnel_event_occurred_at_idx ON funnel_event (occurred_at);
