# 0022. Déroulé des parties live : une machine à états pilotée par le temps, réveillée par le serveur

- Statut : accepté
- Date : 2026-11-02

## Contexte

Après la salle d'attente (ADR 0021), la partie doit se jouer : l'hôte démarre, la même question est posée à tous au même
moment, un chrono court la ferme, la correction s'affiche, puis vient la question suivante (CLAUDE.md, section 6.5). Un
joueur qui se reconnecte doit retrouver la partie où elle en est. Le rythme ne peut dépendre ni d'un navigateur (celui
de l'hôte peut se fermer) ni de l'horloge des téléphones.

## Options envisagées

Pour faire avancer la partie :

1. **Le serveur, avec un minuteur** (retenu) : chaque étape programme son réveil à son échéance ; c'est la seule source
   de vérité sur le temps.
2. Le navigateur de l'hôte, qui enverrait « question suivante » : la partie s'arrêterait si l'hôte perd sa connexion, et
   rien n'empêcherait un hôte d'accélérer.

Pour les commandes des joueurs (démarrer, répondre) :

1. **En REST** (retenu) : les mêmes Problem Details et codes stables que le reste de l'API, la même validation, des tests
   d'intégration simples. Le WebSocket ne fait que diffuser.
2. Par messages sur le WebSocket : un protocole d'erreurs à réinventer pour un gain de latence négligeable à cette
   échelle.

## Décision

- `LiveGame` porte une **machine à états** en Java pur : `LOBBY` → (`QUESTION` → `REVEAL`) × N → `FINISHED`. Chaque
  méthode reçoit l'instant présent, la partie ne lit jamais d'horloge : le déroulé se teste avec une heure fixe.
- `advance(now)` fait passer à l'étape suivante si l'échéance de l'étape en cours est atteinte, et ne fait rien sinon.
  Le minuteur (`LiveScheduler`, des minuteurs Vert.x qui ne bloquent aucun thread) ne fait que réveiller la partie à
  l'échéance.
- Chaque réveil connaît l'échéance qu'il vise. Il est **ignoré s'il est périmé** (la correction a été anticipée), et il
  **se reprogramme s'il arrive un peu en avance** sur l'horloge, sinon la partie resterait bloquée. Un réveil n'est
  programmé que quand l'échéance change : une réponse ou une arrivée ne multiplie pas les minuteurs.
- Quand **tous les joueurs présents ont répondu**, la correction est révélée sans attendre la fin du chrono ; c'est aussi
  le cas si le dernier qui n'avait pas répondu s'en va.
- Seule la **première réponse** d'un joueur compte ; une réponse hors délai, pour une autre question ou d'un utilisateur
  non connecté à la partie est refusée (**409** `live.round-closed`, `live.not-in-game`). Seul l'**hôte** démarre
  (**403** `live.host-only`), une seule fois (`live.already-started`).
- Les questions sont des **QCM** du sujet, dans les thèmes du groupe, tirés au hasard au démarrage (10 au plus, 3 au
  moins : **422** `live.not-enough-questions`). Chrono de **20 secondes**, correction affichée **5 secondes**
  (`recallio.live.*`, raccourcis en test).
- Chaque changement est diffusé (`LiveGameEvents`, implémenté par le WebSocket) avec le **temps restant** calculé par le
  serveur à l'envoi ; les bonnes réponses n'y figurent qu'à la correction. Un joueur qui se reconnecte, ou qui arrive en
  cours de partie, reçoit l'état courant et reprend là.
- Une partie finie ne bloque plus son groupe : on peut en ouvrir une autre.

## Conséquences

- Le chrono affiché par la PWA part du temps restant reçu ; un léger décalage réseau raccourcit d'autant le temps
  affiché, jamais celui accordé par le serveur.
- Les réponses sont gardées avec leur instant : l'étape suivante en tire les points (justesse et rapidité) et le
  classement en direct.
- Le minuteur appelle le service sur la boucle d'événements : rien sur ce chemin ne doit bloquer (pas de base de données,
  pas de transaction).
