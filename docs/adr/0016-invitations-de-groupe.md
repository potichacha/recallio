# 0016. Invitations de groupe : un lien secret par groupe, expirant et remplaçable

- Statut : accepté
- Date : 2026-11-01

## Contexte

Un créateur doit pouvoir faire entrer des membres dans son groupe (CLAUDE.md, section 6.4 : « génère un lien
d'invitation (expirant) »). L'application ne connaît pas les adresses e-mail de ses utilisateurs (le fournisseur OIDC
les détient) et n'envoie aucun message : le créateur partage lui-même le lien, par la messagerie de son équipe.

## Options envisagées

Pour la forme de l'invitation :

1. **Un lien partageable par groupe** (retenu) : un seul lien actif à la fois, que le créateur remplace ou désactive.
   Simple à expliquer, et suffisant pour une équipe.
2. **Une invitation nominative** par personne (e-mail ou nom d'utilisateur) : plus de contrôle, mais il faudrait
   chercher des utilisateurs (fuite d'information sur qui utilise l'application) et envoyer des e-mails.
3. **Plusieurs liens actifs** avec un nombre d'utilisations : utile pour une ESN, aucun besoin exprimé ; ajoutable en
   relâchant la clé primaire de `group_invitation`.

Pour le stockage du jeton :

1. **En clair** (retenu) : le créateur peut réafficher et recopier le lien à tout moment.
2. **Haché** (SHA-256), comme un mot de passe : une fuite de la base ne permettrait pas de rejoindre des groupes, mais
   le créateur ne verrait le lien qu'une fois et devrait le régénérer, invalidant celui déjà partagé.

Le jeton n'ouvre que l'adhésion à un groupe de révision (aucune donnée personnelle au-delà des noms affichés), il
expire, et le créateur peut le désactiver : la commodité l'emporte. Le choix serait à revoir si une invitation donnait un
jour plus de droits (groupe d'entreprise, phase 4).

## Décision

- Table `group_invitation (group_id PK, token unique, created_at, expires_at)` : **un lien par groupe au plus**, remplacé
  par un upsert. Supprimer le groupe supprime son lien (cascade).
- Le jeton (`InvitationToken`) : **192 bits aléatoires** (`SecureRandom`), encodés en base64url (32 caractères). Un
  jeton mal formé est rejeté avant toute requête en base ; `toString()` le masque, pour qu'il n'apparaisse dans aucun
  log.
- **Validité de 7 jours** (`GroupInvitation.VALIDITY`), le temps qu'une équipe rejoigne le groupe.
- Le créateur seul génère, consulte, remplace (`POST`) ou désactive (`DELETE`) le lien de `/api/v1/groups/{id}/invitation`
  (403 pour un membre, 404 pour les autres).
- L'invité, **connecté**, consulte `GET /api/v1/invitations/{token}` (nom du groupe, nombre de membres) puis le rejoint
  par `POST .../accept`. Un lien inconnu, mal formé, expiré ou remplacé répond le même **404**
  `group.invitation-not-found`, sans rien révéler du groupe.
- Rejoindre est **idempotent** : un membre qui rouvre le lien ne change rien (insertion `on conflict do nothing`).
- La borne de **100 membres** (ADR 0015) est appliquée ici : **409** `group.full`.
- Côté PWA, le lien pointe vers `/join/{token}`, une page qui exige la connexion et y ramène l'invité ensuite. La
  politique `Referrer-Policy: strict-origin-when-cross-origin` évite que le jeton parte vers un autre site.

## Conséquences

- Remplacer le lien invalide aussitôt celui déjà partagé : c'est aussi la façon de « fermer » un lien qui a trop
  circulé.
- Deux adhésions simultanées au 100ᵉ membre pourraient dépasser la borne d'une unité : acceptable, la borne protège la
  taille des classements, pas une règle métier stricte.
- Le parcours de bout en bout (Playwright) couvre l'invitation à deux utilisateurs : alice invite, bob rejoint, le lien
  remplacé cesse de fonctionner.
