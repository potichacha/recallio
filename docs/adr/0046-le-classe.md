# 0046. Le classé : cotes, rangs, duels et saisons

- Statut : accepté
- Date : 2026-11-05

## Contexte

CLAUDE.md (6.3, phase 3 ter, étapes 13 à 17) prévoit un mode **classé** : un rang par sujet (Bronze → Argent → Or →
Platine → Diamant → Maître, en divisions), une cote Glicko-2 cachée, des matchs de placement, des saisons aux
récompenses cosmétiques ; des duels asynchrones d'abord, le classé en direct ensuite ; uniquement des formats corrigés
par le serveur, avec un chrono mesuré par le serveur (2.4), et des questions adaptées au niveau, grâce à une cote de
difficulté des cartes.

Le produit compte peu d'utilisateurs pour l'instant : un appariement qui exige un adversaire de même niveau, en ligne au
même moment, laisserait la plupart des duels sans adversaire.

## Options envisagées

- **Elo** : simple, mais une cote neuve bouge aussi peu qu'une cote ancienne ; il faudrait inventer des règles pour les
  matchs de placement et pour les joueurs qui reviennent après une pause.
- **Glicko-2** (retenu) : la cote a une incertitude (RD) qui grandit avec l'inactivité et fond avec les parties. Les
  matchs de placement en découlent : une RD haute fait bouger la cote vite.
- **TrueSkill** : fait pour les équipes, breveté, plus complexe sans gain pour des duels.
- Pour le classé en direct, **des WebSockets** comme le mode live (ADR 0021), ou **le même duel, joué en même temps**,
  suivi par interrogation régulière du serveur (retenu pour commencer) : aucun nouveau protocole, le serveur reste seul
  maître du temps et de la correction.

## Décision

- **Un module `ranked`**, qui dépend de `content` (les cartes), de `user` (l'utilisateur, les noms affichés) et de
  l'événement public `review.CardReviewed` ; rien ne dépend de lui.
- **Glicko-2** (Glickman, 2012) dans le domaine, en Java pur : cote 1500, RD 350, volatilité 0,06 au départ, τ = 0,5. Une
  période de notation par duel. Les cotes ne quittent jamais le serveur : on montre le rang, pas le nombre.
- **La cote de difficulté d'une carte** (étape 13) : chaque carte corrigée par le serveur a sa cote Glicko-2. Chaque
  réponse est une partie entre la carte et celui qui répond, avec la cote de ce dernier dans le sujet (ou la cote de
  départ) : une bonne réponse fait perdre la carte. Elle apprend des révisions (`CardReviewed`) et des duels ; ni le
  Tour, ni l'entraînement, ni l'examen blanc ne la touchent, puisqu'ils n'enregistrent rien.
- **Les rangs** : Bronze sous 1300, puis une tranche de 200 points par rang (Argent, Or, Platine, Diamant), Maître à
  partir de 2100 ; chaque rang sauf Maître a quatre divisions de 50 points (IV la plus basse). Une cote de départ
  (1500) mène à Or IV, avant le placement.
- **Le placement** : les 5 premiers duels d'une saison dans un sujet. Le rang reste caché pendant le placement.
- **Les duels asynchrones** (étape 14) : 5 questions corrigées par le serveur (QCM, vrai ou faux, code à compléter),
  tirées près de la cote du premier joueur, une par notion si possible. Chacun joue quand il veut ; une question servie
  a 30 secondes, mesurées par le serveur, et ne se resert pas. Le premier joueur ouvre le duel, le suivant qui cherche un
  duel dans ce sujet le rejoint (le plus proche en cote parmi les duels ouverts). Un duel ouvert sans adversaire expire
  après 48 heures, sans changer les cotes ; un adversaire qui n'a pas fini à temps perd les questions qui lui restent.
- **Le résultat** : le plus de bonnes réponses gagne ; à égalité, le temps total le plus court ; à égalité parfaite,
  match nul. Chaque joueur prend une partie contre la cote d'avant duel de l'autre.
- **Les saisons** (étape 16) : un trimestre civil (UTC). Au premier duel d'une nouvelle saison, la cote se rapproche de
  1500 de moitié et la RD remonte à 200 au moins ; le placement recommence. Le meilleur rang atteint dans la saison reste
  comme récompense cosmétique (un insigne « Saison 2026-T4 · Or »), affiché sur la page du classé.
- **Le classé en direct** (étape 17) : une file d'attente par sujet. Deux joueurs présents au même moment (la PWA se
  signale toutes les quelques secondes) sont appariés dans un duel joué en même temps, qui expire après 5 minutes ;
  chacun voit la progression de l'autre. Mêmes questions, même chrono, même résultat qu'en asynchrone.
- **L'API** : `GET /api/v1/me/ranked` (rangs, placement, insignes), `POST /api/v1/ranked/duels` (chercher un duel),
  `POST /api/v1/ranked/live-queue` et `DELETE` (le direct), `GET /api/v1/ranked/duels/{id}`, `POST …/next` (la question
  suivante, servie une fois), `POST …/answers` (la correction), `GET /api/v1/me/ranked/duels` (les derniers duels).
- **La PWA** : la page « Classé » (rang par sujet, placement, insignes, duels), le duel (question, chrono, correction,
  résultat, partage d'une promotion).

## Conséquences

- Les cotes des cartes servent au tirage des duels ; à terme, elles repéreront aussi les cartes mal calibrées ou
  fausses, avec les signalements (ADR 0042).
- Un duel asynchrone peut attendre son adversaire deux jours : le rang ne change qu'au résultat. C'est le prix d'un
  appariement qui marche avec peu de joueurs.
- Les expirations se règlent à la lecture d'un duel ou à la recherche d'un nouveau : aucune tâche planifiée.
- Le classé reste à part de la révision : ses réponses n'entrent ni dans le journal de révision, ni dans FSRS, ni dans
  l'XP. Seules les cotes (joueurs et cartes) en apprennent.
- Le direct par interrogation coûte une requête toutes les deux secondes par joueur pendant 5 minutes au plus : assez
  pour quelques dizaines de duels simultanés. Au-delà, il passera sur les WebSockets du mode live.
