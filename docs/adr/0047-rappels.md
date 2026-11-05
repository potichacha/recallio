# 0047. Les rappels par notification

- Statut : accepté
- Date : 2026-11-05

## Contexte

CLAUDE.md (phase 3 ter, étape 10) prévoit des **rappels** : des notifications de la PWA, sur acceptation, pour garder
l'habitude (« Ta série de 12 jours t'attend »). Une PWA reçoit des notifications par le protocole **Web Push** : le
serveur envoie un message chiffré au service de notification du navigateur (Google, Mozilla, Apple, Microsoft), qui le
remet à l'appareil, même application fermée.

Deux contraintes : rien sans l'accord de l'utilisateur (RGPD, et la règle des navigateurs), et aucun secret dans le
dépôt.

## Options envisagées

- **Une bibliothèque Web Push** (webpush-java) : elle tire Bouncy Castle et une pile HTTP de plus, pour une centaine de
  lignes de cryptographie que le JDK sait faire (ECDH, HKDF, AES-GCM, ECDSA).
- **Un service tiers** (OneSignal, Firebase Cloud Messaging en direct) : il recevrait les identifiants et l'activité
  des utilisateurs.
- **Le protocole implémenté avec le JDK** (retenu) : le chiffrement de la RFC 8291 et l'identification VAPID de la
  RFC 8292, vérifiés par l'exemple de la RFC 8291 dans les tests ; l'envoi par le client HTTP du JDK.
- Pour le passage régulier, **l'extension de planification de Quarkus**, ou **un minuteur Vert.x** (retenu) : le mode
  live en utilise déjà (ADR 0022), sans dépendance de plus.

## Décision

- **Un module `notification`**, qui dépend de `user` (le fuseau d'un utilisateur, par `UserDirectory.user`) et de
  `gamification` (la série et l'objectif du jour, par `DailyGoals`) ; rien ne dépend de lui.
- **L'abonnement** : l'utilisateur active les rappels dans son profil et choisit l'heure (19 h par défaut, dans son
  fuseau). Le navigateur demande la permission, puis crée l'abonnement avec la clé publique du serveur ; le serveur
  garde l'adresse, la clé et le secret de l'appareil (`push_subscription`). Chaque appareil s'abonne à part.
- **La sécurité** : le serveur n'envoie qu'aux services de notification connus, en https (une liste fermée de
  domaines) : une adresse quelconque donnée par un client serait une porte vers le réseau interne (SSRF). Le message
  est chiffré pour l'appareil : le service de notification ne le lit pas.
- **Le rappel** : au plus un par jour et par appareil, à partir de l'heure choisie, seulement si l'objectif du jour
  n'est pas atteint. Le message parle de la série quand il y en a une (« 🔥 Ta série de 12 jours t'attend »), sinon des
  cartes du jour ; il ouvre la révision.
- **Le passage** : toutes les 10 minutes, un minuteur Vert.x réserve les rappels dus en base (le jour est noté sur
  l'abonnement, en une instruction : deux instances n'envoient pas deux fois), puis les envoie hors transaction. Un
  appareil désabonné (404 ou 410 du service) est oublié.
- **Les clés VAPID** : des variables d'environnement en production (`RECALLIO_PUSH_VAPID_PUBLIC_KEY`,
  `RECALLIO_PUSH_VAPID_PRIVATE_KEY`, `RECALLIO_PUSH_SUBJECT`). Absentes, le serveur tire une paire au démarrage, avec un
  avertissement : les abonnements ne survivent pas à un redémarrage, ce qui ne convient qu'au développement.
- **L'API** : `GET /api/v1/push/public-key`, `PUT /api/v1/me/push-subscription`, `DELETE
  /api/v1/me/push-subscription?endpoint=`.
- **La PWA** : la section « Rappels » du profil ; le service worker importe `push-sw.js`, qui affiche la notification
  et ouvre la page du rappel au clic.

## Conséquences

- Sur iPhone, les notifications d'une PWA demandent qu'elle soit ajoutée à l'écran d'accueil (iOS 16.4 et plus) : la
  PWA le dit quand le navigateur ne les accepte pas.
- Un abonnement ne contient aucune donnée personnelle lisible, mais il identifie un appareil : la suppression de compte
  (phase 4) devra supprimer les abonnements de l'utilisateur.
- Les clés VAPID ne doivent pas changer en production : de nouvelles clés invalident tous les abonnements, que les
  utilisateurs devraient refaire. Leur rotation, si un jour elle s'impose, passera par un nouvel ADR.
- Le passage toutes les 10 minutes lit tous les abonnements : assez pour des milliers. Au-delà, il faudra le
  restreindre aux fuseaux dont l'heure vient de passer.
