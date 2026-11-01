# 0015. Groupes : un module dédié, un créateur unique, l'appartenance vérifiée côté serveur

- Statut : accepté
- Date : 2026-11-01

## Contexte

La phase 2 introduit les groupes (CLAUDE.md, section 6.4) : un créateur réunit des membres, qui révisent ensemble et se
comparent dans un classement. Cette première étape pose le modèle : qui appartient à quel groupe, qui peut le gérer. Les
invitations, les réglages, les classements et les défis suivront, chacun dans sa propre étape.

## Options envisagées

Pour les rôles :

1. **Un créateur unique** (`OWNER`) et des membres (`MEMBER`) (retenu) : suffisant pour la V1, règles simples.
2. **Plusieurs administrateurs** par groupe : utile pour une ESN, mais aucun besoin exprimé ; ajoutable plus tard
   (nouvelle valeur de rôle) sans changer le modèle.

Pour un groupe que l'appelant ne voit pas :

1. **403** : honnête, mais révèle qu'un groupe existe derrière cet identifiant.
2. **404** (retenu) : un groupe inexistant et un groupe dont on n'est pas membre répondent pareil, comme un brouillon de
   sujet (ADR 0013).

## Décision

- Un module **`group`** (CLAUDE.md, section 4.1), avec son domaine : `StudyGroup` (nom, membres) porte les règles,
  vérifiées par des tests unitaires :
  - exactement **un créateur**, garanti aussi par la base (index unique partiel sur `group_member`) ;
  - seul le créateur retire un membre ou supprime le groupe (**403** `group.owner-only` pour un membre) ;
  - le créateur ne peut ni quitter son groupe ni en être retiré (**409** `group.owner-cannot-leave`) : il le supprime.
- Tables `study_group` et `group_member (group_id, user_id, role, joined_at)`. Les utilisateurs sont désignés par leur
  identifiant applicatif, **sans clé étrangère** vers `app_user` : le module ne lit jamais les tables du module `user`
  (ADR 0001). Il obtient les noms affichés par une nouvelle API publique, `user.UserDirectory`.
- L'**appartenance est vérifiée pour chaque requête**, dans le cas d'usage (`StudyGroupService`) : un non-membre reçoit
  **404** `group.not-found` (CLAUDE.md, section 4.5).
- Bornes : 20 groupes créés par utilisateur (`group.too-many-groups`), 100 membres par groupe (appliquée à l'étape des
  invitations).
- La plateforme gagne une `ForbiddenException`, traduite en 403 par le gestionnaire d'erreurs global.

## Conséquences

- Supprimer un groupe supprime ses adhésions (cascade) ; les données de révision des membres ne dépendent pas du groupe
  et restent intactes.
- Un membre dont le compte disparaîtrait resterait listé sous un nom générique, sans erreur.
- Les étapes suivantes ajoutent les invitations (rejoindre un groupe), puis les réglages, qui s'attachent au groupe sans
  modifier ce modèle.
