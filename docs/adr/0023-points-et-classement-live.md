# 0023. Points et classement des parties live : la justesse, puis la rapidité ; résultats conservés

- Statut : accepté
- Date : 2026-11-02

## Contexte

Une partie live attribue des points « selon la justesse et la rapidité » et affiche un classement en direct
(CLAUDE.md, section 6.5). C'est le seul contexte compétitif du produit (section 2.4) : la vitesse peut y compter, alors
qu'elle ne rapporte rien en révision personnelle (section 6.3). Les parties vivent en mémoire (ADR 0021) ; il faut garder
une trace de leurs résultats.

## Options envisagées

Pour le barème :

1. **Une base pour la justesse, un bonus proportionnel au temps restant** (retenu) : 500 points pour une réponse juste,
   jusqu'à 500 de plus selon la part du chrono qui restait. Une réponse juste, même lente, vaut toujours plus qu'une
   réponse fausse rapide.
2. Des points pour la seule rapidité, réponse juste ou non : pousse à cliquer au hasard.
3. Un ordre d'arrivée (le premier qui trouve gagne plus) : décourage dès la première question ceux qui lisent moins vite.

Pour conserver les résultats :

1. **Le classement final en base, à la fin de la partie** (retenu) : deux tables (`live_game`, `live_game_player`).
2. Chaque réponse en base : utile pour des statistiques détaillées, sans besoin exprimé.

## Décision

- `LiveScoring.points` : **0** pour une réponse fausse ou absente, **500 + 500 × part du chrono restante** pour une
  réponse juste (arrondi), mesurée depuis l'instant où la question a été posée.
- Le **classement** est calculé par `LiveGame` à chaque diffusion, sur les seules questions **déjà corrigées** : pendant
  le chrono, il ne révèle rien des réponses données. Les ex æquo partagent leur rang. Un joueur parti en cours de partie
  **garde ses points** et reste classé.
- La PWA affiche, à chaque correction, les points gagnés et le haut du classement (les cinq premiers, et soi-même) ; à
  la fin, le podium et le classement complet. Le groupe montre ses cinq dernières parties avec leurs trois premiers.
- Les noms sont affichés, quel que soit le réglage de visibilité du classement du groupe (ADR 0018) : une partie live
  réunit des joueurs qui se voient déjà dans la salle d'attente. La visibilité du groupe s'applique au classement de la
  semaine et aux défis, qui comparent aussi les absents.
- Le résultat est enregistré **à la fin de la partie**, sur un thread de travail (`LiveScheduler.runBlocking`) : la fin
  arrive par un réveil du minuteur, sur la boucle d'événements, où rien ne doit bloquer. L'enregistrement est
  idempotent. Les noms y sont figés tels qu'ils étaient pendant la partie.
- `GET /api/v1/groups/{id}/live-games/results` : les cinq dernières parties du groupe, réservées à ses membres.

## Conséquences

- Les tables de résultats ne référencent ni le groupe ni les joueurs par clé étrangère (ADR 0001) : la suppression d'un
  groupe laisse ses résultats, devenus inaccessibles. La suppression de compte (phase 4, RGPD) devra aussi effacer les
  noms conservés ici.
- Un redémarrage du backend en cours de partie perd la partie et son résultat : acceptable pour des parties de quelques
  minutes (ADR 0021).
- Les points live ne s'ajoutent pas aux points de gamification (registre `points_ledger`) : la révision personnelle ne
  récompense pas la vitesse, et le mode live ne doit pas fausser le classement hebdomadaire.
