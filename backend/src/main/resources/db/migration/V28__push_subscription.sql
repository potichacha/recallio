-- Les rappels (ADR 0047) : un abonnement Web Push par appareil, accepté par l'utilisateur, et l'heure de son rappel
-- dans son fuseau. Comme les autres modules, pas de clé étrangère vers les tables d'un autre module.
CREATE TABLE push_subscription (
    id               uuid        PRIMARY KEY,
    user_id          uuid        NOT NULL,
    -- L'adresse du service de notification du navigateur : l'envoyer revient à joindre l'appareil.
    endpoint         text        NOT NULL UNIQUE CHECK (length(endpoint) <= 2048),
    p256dh           text        NOT NULL,
    auth             text        NOT NULL,
    reminder_hour    smallint    NOT NULL CHECK (reminder_hour BETWEEN 0 AND 23),
    last_reminded_on date,
    created_at       timestamptz NOT NULL
);

CREATE INDEX push_subscription_user_idx ON push_subscription (user_id);
