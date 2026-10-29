-- Module user : profil applicatif lié à l'identité OIDC (CLAUDE.md, section 7).
-- Le backend ne stocke ni mot de passe ni e-mail : l'identité appartient au fournisseur OIDC.

CREATE TABLE app_user (
    id           uuid        PRIMARY KEY,
    -- Claim « sub » du jeton : identifiant stable de l'utilisateur chez le fournisseur.
    oidc_subject text        NOT NULL UNIQUE,
    display_name text        NOT NULL,
    -- Fuseau IANA (Europe/Paris...) : sert à calculer « le jour » d'un streak.
    timezone     text        NOT NULL,
    created_at   timestamptz NOT NULL,
    updated_at   timestamptz NOT NULL
);
