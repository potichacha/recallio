# 0042. Signaler une erreur

- Statut : accepté
- Date : 2026-11-05

## Contexte

Le contenu est rédigé par Claude et relu par le propriétaire du projet (ADR 0025). Avec plus de 12 000 cartes et des
centaines de pages du Tour, des erreurs passeront la relecture. CLAUDE.md (phase 3 ter, étape 9) demande que les
utilisateurs puissent les remonter, sur chaque carte et chaque page du Tour.

Un signalement est une donnée produite par un utilisateur, à propos du contenu. Le module `content` ne peut pas en
porter l'auteur : il ne dépend pas du module `user`, qui dépend de lui (les sujets suivis).

## Options envisagées

- **Un lien vers les issues GitHub** : il faut un compte GitHub, et il sort de l'application. Il convient aux
  contributeurs, pas aux utilisateurs.
- **Le module `content`** : il devrait connaître l'utilisateur, ce qui crée un cycle entre `content` et `user`.
- **Le module `review`** : il dépend déjà de `content` et de `user`, mais un signalement n'est pas une révision.
- **Un nouveau module `feedback`** (retenu) : il dépend de `content` (vérifier le contenu signalé) et de `user`
  (l'auteur), et rien ne dépend de lui.

## Décision

- **Le module `feedback`** porte les signalements, dans la table `content_report`, sans clé étrangère vers les tables
  des autres modules (ADR 0001).
- **Ce qu'on signale** : une carte, ou une page d'une notion du Tour, avec une raison (réponse fausse, pas clair, code
  qui ne marche pas, faute de frappe, autre) et des précisions facultatives (1 000 caractères au plus). Seul le
  contenu publié se signale : c'est celui que voient les utilisateurs. Les brouillons se relisent dans leur pull
  request.
- **Les limites** : un utilisateur ne signale qu'une fois un même contenu tant que son signalement est ouvert (409), et
  il a au plus 20 signalements ouverts à la fois (429). De quoi décourager le spam, sans gêner un usage normal.
- **Le traitement** : un admin voit les signalements ouverts, du plus ancien au plus récent, avec de quoi retrouver le
  contenu (le début de la question d'une carte, le lien vers la page du Tour), puis les marque corrigés ou rejetés.
  La correction elle-même passe par une pull request de contenu, comme toute modification du contenu (CLAUDE.md, 5.4).
  Le traitement se fait en une instruction qui ne touche qu'un signalement ouvert : deux admins ne le traitent pas
  deux fois.
- **La vie privée** : l'auteur n'est montré à personne, l'admin n'en a pas besoin. Les journaux ne gardent que
  l'identifiant du signalement, sa raison et son sujet.
- **L'API** : `POST /api/v1/content-reports` (une carte, ou une page : `subject`, `notion`, `page`), puis, pour les
  admins, `GET /api/v1/admin/content-reports?status=OPEN|RESOLVED` (paginé) et
  `PUT /api/v1/admin/content-reports/{id}/resolution`.
- **La PWA** : « Signaler une erreur » sous chaque carte en révision et à l'entraînement, et sur chaque page du Tour pour
  un utilisateur connecté ; la page « Signalements » des admins, depuis leur profil.

## Conséquences

- Un nouveau module, à ajouter au tableau de CLAUDE.md (4.1) et à `docs/architecture.md`. ArchUnit vérifie qu'il ne
  touche qu'aux API publiques de `content` et de `user`.
- Une carte retirée garde ses signalements ; la liste des admins le dit (« carte retirée depuis »).
- L'utilisateur n'est pas prévenu quand son signalement est traité. Si le besoin apparaît, les rappels (étape 10)
  pourront le faire.
- Le nombre de signalements d'une carte n'est pas encore agrégé : à envisager avec la cote de difficulté des cartes
  (étape 13), qui repérera aussi les cartes mal calibrées.
