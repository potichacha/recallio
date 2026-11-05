# Rétrospective : gamification v2, préparation, partage et classé (phase 3 ter)

- Période : du 2026-11-04 au 2026-11-05
- Livré : la maîtrise, la carte des compétences, l'XP et le gel de streak dans les PR précédentes (étapes 1 à 4), puis
  les étapes 5 à 17 dans la PR « finir le projet »

## Ce qui a été produit

| Étape | Fonctionnalité | ADR |
|---|---|---|
| 5, 6 | Quêtes du jour et de la semaine, succès | 0040 |
| 7 | Indice de préparation par sujet | 0041 |
| 8 | Examen blanc | 0044 |
| 9 | Signaler une erreur, et le traitement par les admins | 0042 |
| 10 | Rappels par notification (Web Push) | 0047 |
| 11 | Profil public de compétences, privé par défaut | 0043 |
| 12 | Cartes de partage | 0045 |
| 13 à 17 | Classé : cotes des cartes, rangs, duels, saisons, direct | 0046 |

Trois modules sont apparus : `feedback` (les signalements), `ranked` (le classé) et `notification` (les rappels).
Chacun dépend des API publiques des autres, et rien ne dépend de lui ; ArchUnit le vérifie.

## Ce qui a bien marché

- **Rien de stocké quand un calcul suffit.** Les quêtes d'une période se déduisent de l'utilisateur et de la date,
  l'indice de préparation de la maîtrise déjà calculée, le rang de la cote. Aucune tâche planifiée pour les quêtes, les
  saisons ou les duels expirés : tout se règle à la lecture.
- **Les algorithmes publiés, testés sur leurs exemples.** Glicko-2 reproduit l'exemple de l'article de Glickman au
  centième près ; le chiffrement Web Push reproduit l'exemple de la RFC 8291 octet pour octet. Ces deux tests ont
  valu toute la confiance qu'on peut mettre dans du code cryptographique ou statistique écrit à la main.
- **Le serveur reste seul juge** dès que le résultat compte : l'examen blanc, les duels et le profil public ne montrent
  que ce que le serveur a corrigé et chronométré. Les cotes ne quittent jamais le serveur.

## Ce qu'on change

- **Une requête sur une route voisine d'une autre ressource peut tomber en 404.** JAX-RS choisit d'abord la classe dont
  le chemin est le plus précis : une ressource sur `/api/v1` ne reçoit pas `/api/v1/me/…` tant que `MeResource` existe.
  Règle : un chemin de classe aussi précis que possible, une classe par famille de routes.
- **Les tests de deux joueurs passent par les services.** Une identité de test par méthode empêche de jouer un duel à
  deux par l'API ; le test d'API couvre un joueur, les services couvrent le duel complet.
- **Le classé en direct interroge le serveur toutes les deux secondes.** Suffisant pour commencer ; le passage aux
  WebSockets du mode live est noté dans l'ADR 0046, à faire si le trafic le demande.

## Reste à faire

- Le déploiement (phase 1, étape 7), qui fixera aussi le fournisseur OIDC et les clés VAPID de production.
- Relire « Programmer de zéro » dans l'application, puis le publier (une PR de contenu).
- Phase 4 (entreprise) et phase 5 (options), selon CLAUDE.md.
