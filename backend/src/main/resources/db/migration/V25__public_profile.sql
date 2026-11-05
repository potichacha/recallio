-- Le profil public de compétences (ADR 0043) : privé par défaut. Un utilisateur sans ligne, ou avec visible = false,
-- n'a pas de profil public ; son adresse (/u/<handle>) répond comme une adresse inconnue.
CREATE TABLE public_profile (
    user_id    uuid        PRIMARY KEY REFERENCES app_user (id),
    -- De 3 à 30 caractères : minuscules, chiffres et tirets, sans tiret au début ni à la fin.
    handle     text        NOT NULL UNIQUE CHECK (handle ~ '^[a-z0-9][a-z0-9-]{1,28}[a-z0-9]$'),
    visible    boolean     NOT NULL,
    updated_at timestamptz NOT NULL
);
