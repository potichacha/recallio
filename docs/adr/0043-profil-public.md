# 0043. Le profil public de compétences

- Statut : accepté
- Date : 2026-11-05

## Contexte

CLAUDE.md (6.3, phase 3 ter, étape 11) prévoit un **profil public de compétences**, **privé par défaut** (RGPD,
protection des données dès la conception), que chacun rend public ou privé à tout moment. Le profil distingue le
**vérifié** (ce que le serveur a corrigé) du **déclaré** (la maîtrise en révision personnelle, auto-évaluée en
partie) : un recruteur doit savoir ce qui a été prouvé.

Les données viennent de trois modules : le nom (`user`), la maîtrise et les réussites (`review`), l'XP, la série et
les succès (`gamification`).

## Options envisagées

- **L'identifiant de l'utilisateur dans l'adresse** : il ne dit rien, et il est définitif. Le publier relie aussi
  toutes ses traces.
- **Le nom affiché dans l'adresse** : il n'est pas unique, et le changer casserait les liens partagés.
- **Une adresse choisie** (retenu) : `/u/<adresse>`, unique, que l'utilisateur choisit et peut changer.

## Décision

- **Les réglages** (module `user`, table `public_profile`) : une adresse de 3 à 30 caractères (minuscules, chiffres,
  tirets) et un drapeau `visible`, faux tant que l'utilisateur ne l'a pas rendu public. Sans ligne, pas de profil.
  `GET` et `PUT /api/v1/me/public-profile` ; une adresse prise répond 409.
- **Le vérifié**, par sujet suivi (module `review`, `SkillProfiles`) : les exercices de code réussis (jugés par leurs
  tests), les autres cartes corrigées par le serveur réussies (QCM, vrai ou faux, code à compléter), les notions du
  Tour validées. Plus tard, le rang en classé s'y ajoutera.
- **Le déclaré**, par sujet suivi : la maîtrise des notions (ADR 0034) et l'indice de préparation (ADR 0041), qui
  reposent en partie sur l'auto-évaluation.
- **Le reste** (module `gamification`) : le niveau de chaque sujet, la série du jour et les succès débloqués.
- **La lecture** : `GET /api/v1/public-profiles/{adresse}`, sans compte, comme le catalogue. Un profil privé, une
  adresse inconnue et une adresse mal formée répondent tous 404 : on ne peut pas savoir qu'un profil privé existe.
- **Ce qui n'est jamais publié** : l'identifiant de l'utilisateur, son fuseau horaire, ses groupes, son historique de
  révision carte par carte.
- **La PWA** : la section « Profil public » du profil (adresse, case « Rendre mon profil public », lien vers le profil),
  et la page `/u/:handle`.

## Conséquences

- Le module `gamification` assemble le profil : il dépend déjà de `review` et de `user`, et de `content` pour les titres
  des sujets (`SubjectDirectory.publishedTitles`). Aucun cycle.
- Rendre le profil privé le retire aussitôt : rien n'est mis en cache côté serveur. Une copie déjà vue par un tiers ne
  se rattrape pas, comme partout sur le web ; la PWA le dit en une phrase.
- Changer d'adresse casse les liens déjà partagés : c'est le prix d'une adresse libre. L'ancienne adresse redevient
  disponible pour un autre utilisateur.
- La suppression de compte (phase 4, RGPD) devra supprimer la ligne `public_profile` avec le profil.
