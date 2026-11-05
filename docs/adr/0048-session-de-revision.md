# 0048. La session de révision : un sujet, une durée, des cartes qui se retournent

- Statut : accepté
- Date : 2026-11-05
- Revient en partie sur : ADR 0014 (quota de nouvelles cartes à parts égales) et CLAUDE.md, section 6.1 (les quatre
  notes de l'auto-évaluation)

## Contexte

Le propriétaire du projet a testé l'application en local avant le déploiement. Il en a rapporté trois retours sur la
révision :

1. **« Je n'ai pas pu répondre, et c'est moi qui choisis si la carte revient. »** Une question comme « Java est-il
   compilé ou interprété ? » se révèle, puis on choisit parmi quatre boutons (« À revoir », « Difficile », « Bien »,
   « Facile »), chacun avec le délai que donnerait FSRS. Le joueur ne voit pas ce qu'il note : son souvenir, ou la
   date du prochain passage ? Les formats corrigés par le serveur (QCM, vrai ou faux, code à compléter, exercices)
   font déjà ce qu'il attend : on répond, le serveur corrige, la carte revient vite si c'est faux. Mais ils ne sont
   que 46 % des cartes de Java et de Go ; les 54 % restants s'auto-évaluent.
2. **« Je ne veux pas de questions de Go et de Java mélangées. »** La session par défaut, « Tous mes sujets », alternait
   les nouvelles cartes de chaque sujet suivi (ADR 0014). Le besoin est inverse : choisir un langage, le réviser, puis
   passer à un autre.
3. **« Choisir un temps de révision : 5, 10, 15, 20 minutes. »**

Il a aussi demandé que les cartes à auto-évaluer **se retournent** comme une vraie carte.

## Options envisagées

Pour les cartes à auto-évaluer :

- **Tout transformer en QCM** : réécrire et relire quelque 2 500 questions de cours. C'est un très gros chantier de
  contenu, et la reconnaissance parmi des choix est plus facile que le rappel : en entretien, personne ne propose de
  choix.
- **Ajouter seulement un mode « questions corrigées »** au lancement, sans toucher aux quatre boutons.
- **Un mode « questions corrigées », et deux boutons sans délais** (retenu) : « Je savais » et « Je ne savais pas ».
  On peut écrire sa réponse avant de retourner la carte, puis la comparer à celle attendue. FSRS fonctionne avec ces
  deux seules notes, « Bien » et « À revoir », et beaucoup d'utilisateurs d'Anki révisent déjà ainsi.

Pour le mélange des sujets : garder « Tous mes sujets » à côté du choix d'un sujet, ou **un sujet par session**
(retenu), comme le demande le retour. Le quota de nouvelles cartes doit alors suivre : un quota global, partagé à parts
égales (ADR 0014), serait épuisé par la première session du jour, et Go n'aurait plus de nouvelles cartes après une
session de Java.

Pour la durée : un **minuteur côté client** (retenu), ou une session enregistrée sur le serveur. Une révision
personnelle n'a pas d'enjeu de triche (CLAUDE.md, section 2.4), donc rien ne justifie de la mesurer sur le serveur.

## Décision

1. **Une session porte sur un sujet.** La page « Réviser » ouvre un écran de lancement avec trois choix :
   - le sujet, parmi les sujets suivis, avec ses cartes à revoir et ses nouvelles cartes du jour ;
   - la durée : 5, 10, 15 ou 20 minutes, ou sans limite ;
   - les questions : toutes, ou seulement celles que le serveur corrige.

   Les derniers réglages sont retenus sur l'appareil (stockage local, simple commodité). Un sujet ouvert depuis le
   catalogue est proposé même s'il n'est pas suivi. Les liens directs (un thème, un groupe, un format) ouvrent la
   session sans écran de lancement ni durée, comme avant.
2. **Un quota de nouvelles cartes par sujet** : 20 par jour et par sujet
   (`recallio.review.new-cards-per-subject-per-day`), au lieu de 20 au total, comme un paquet d'Anki. L'API garde la
   file à plusieurs sujets (celle d'un groupe, ou sans filtre) : chaque sujet y garde son quota, et les nouvelles
   cartes alternent entre eux.
3. **Le mode « questions corrigées »** filtre la file sur les formats que corrige le serveur
   (`CardType.GRADED_BY_SERVER`) : le paramètre `type` de `GET /api/v1/review-queue` devient répétable.
4. **La durée borne la session sans la presser.**
   - Le temps restant s'affiche en minutes.
   - La carte en cours n'est jamais coupée : le bilan vient après elle.
   - Rien ne dépend de la vitesse : ni XP, ni score (CLAUDE.md, section 6.3).
   - Les séries de cartes s'enchaînent seules jusqu'à la fin du temps, ou jusqu'à ce qu'il ne reste rien à réviser.
   - On peut aussi terminer la session plus tôt.
   - Le bilan dit combien de cartes ont été révisées, et combien réussies.
5. **Une carte à auto-évaluer se retourne.**
   - Au recto : la question, et de quoi écrire sa réponse (facultatif).
   - Au verso : la question rappelée, la réponse écrite, la réponse attendue, puis « Je savais » (« Bien ») ou
     « Je ne savais pas » (« À revoir »).
   - Le demi-tour dure une demi-seconde, sans animation pour qui a demandé moins de mouvement à son système.
   - La page « S'entraîner » (ADR 0038) retourne ses cartes de la même façon.
6. **Les délais prévus de chaque note disparaissent de la file** (`previews`) : plus rien ne les affiche.

## Conséquences

- ➕ Le joueur choisit ce qu'il révise et pour combien de temps. Finir Java ne prive pas Go de ses nouvelles cartes.
- ➕ Les questions corrigées sont à un clic pour qui veut être corrigé, sans réécrire le contenu.
- ➕ L'auto-évaluation devient une question simple (« Saviez-vous ? ») plutôt qu'un choix d'échéance.
- ➖ Suivre beaucoup de sujets peut introduire beaucoup de nouvelles cartes par jour. La durée de session limite
  l'effort, et les cartes dues passent toujours en premier dans chaque sujet.
- ➖ « Difficile » et « Facile » ne sont plus proposés : FSRS apprend un peu moins finement de chaque réponse. L'API
  les accepte toujours.
- ➖ Les cartes dues d'un sujet attendent qu'on le révise. Le tableau de bord et l'écran de lancement montrent ce qui
  attend dans chacun.
- 🔁 « Qu'affiche ce code » pourra être corrigé par le serveur : la sortie attendue est déjà vérifiée en CI (ADR 0028),
  il suffirait de comparer celle que tape le joueur. Ce sera une étape à part.
