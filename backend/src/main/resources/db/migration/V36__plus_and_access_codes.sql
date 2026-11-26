-- Le droit « Plus » et les codes d'accès (ADR 0079), et la limite quotidienne de la révision intensive.
-- Comme les autres modules, pas de clé étrangère vers les utilisateurs (ADR 0001).

-- Le droit « Plus » d'un compte : une date de fin, rien d'autre. Une ligne dont la fin est passée est un droit échu.
CREATE TABLE plus_entitlement (
    user_id    uuid        PRIMARY KEY,
    ends_at    timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

-- Les comptes qui ont Plus à ce jour, pour la page « Bêta ».
CREATE INDEX plus_entitlement_ends_at_idx ON plus_entitlement (ends_at);

-- Un lot de codes, créé par un admin : ce que chaque code donne (des jours de Plus), combien de comptes peuvent
-- l'utiliser, jusqu'à quand. Révoquer le lot ferme ses codes ; les droits déjà ouverts restent.
CREATE TABLE access_code_batch (
    id                       uuid        PRIMARY KEY,
    label                    text        NOT NULL CHECK (length(label) BETWEEN 1 AND 100),
    plus_days                integer     NOT NULL CHECK (plus_days > 0),
    max_redemptions_per_code integer     NOT NULL CHECK (max_redemptions_per_code > 0),
    -- Absente : les codes n'expirent pas.
    expires_at               timestamptz,
    created_at               timestamptz NOT NULL,
    revoked_at               timestamptz
);

-- Un code : le secret lui-même n'est pas gardé, seulement son empreinte SHA-256 et ses derniers caractères, pour le
-- reconnaître dans une liste. Le compteur d'utilisations reste quand un compte qui l'a utilisé est supprimé : le code
-- ne se rouvre pas.
CREATE TABLE access_code (
    id          uuid    PRIMARY KEY,
    batch_id    uuid    NOT NULL REFERENCES access_code_batch (id) ON DELETE CASCADE,
    code_hash   text    NOT NULL UNIQUE,
    suffix      text    NOT NULL,
    redemptions integer NOT NULL DEFAULT 0 CHECK (redemptions >= 0)
);

CREATE INDEX access_code_batch_id_idx ON access_code (batch_id);

-- Quel compte a utilisé quel code, et quand : un compte n'utilise un même code qu'une fois.
CREATE TABLE access_code_redemption (
    code_id     uuid        NOT NULL REFERENCES access_code (id) ON DELETE CASCADE,
    user_id     uuid        NOT NULL,
    redeemed_at timestamptz NOT NULL,
    plus_days   integer     NOT NULL CHECK (plus_days > 0),
    PRIMARY KEY (code_id, user_id)
);

CREATE INDEX access_code_redemption_user_id_idx ON access_code_redemption (user_id);
CREATE INDEX access_code_redemption_redeemed_at_idx ON access_code_redemption (redeemed_at);

-- Les essais ratés d'un compte, contre l'essai en force : une ligne par compte, une fenêtre et son compteur.
CREATE TABLE access_code_attempt (
    user_id           uuid        PRIMARY KEY,
    window_started_at timestamptz NOT NULL,
    failures          integer     NOT NULL CHECK (failures > 0)
);

-- L'idempotence de la correction en révision intensive : les empreintes des dernières réponses comptées ce jour-là
-- (l'identifiant de requête, la carte et les choix, hachés ensemble). Une requête rejouée n'est pas comptée deux fois.
-- Ni la carte ni la réponse ne s'y lisent (ADR 0078).
ALTER TABLE drill_activity ADD COLUMN recent_checks uuid[] NOT NULL DEFAULT '{}';
