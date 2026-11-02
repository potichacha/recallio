# 0024. Robustesse du mode live : relève de l'hôte, mémoire bornée, métriques, test de charge

- Statut : accepté
- Date : 2026-11-02

## Contexte

Les parties live vivent en mémoire et dépendent de connexions qui se coupent : téléphones mis en veille, onglets
fermés, hôte qui s'en va (ADR 0021 à 0023). La phase 3 se termine par leur robustesse, des limites, une mesure de la
charge et des métriques (CLAUDE.md, section 9).

## Décision

**Départ de l'hôte.** L'hôte n'a qu'un rôle : démarrer la partie. S'il quitte la salle d'attente, le rôle passe au
joueur présent arrivé le plus tôt, sinon personne ne pourrait démarrer. Une fois la partie démarrée, son départ ne
change rien : le serveur seul la fait avancer (ADR 0022).

**Déconnexions.** Un joueur qui se reconnecte reprend la partie où elle en est et garde ses points (ADR 0022 et 0023) ;
celui qui s'en va sans répondre ne retient pas la correction des autres.

**Mémoire bornée.** Une partie quitte la mémoire d'elle-même, sans compter sur le départ du dernier joueur :

- une salle d'attente que personne n'a rejointe, cinq minutes après son ouverture ;
- une partie finie, dix minutes après sa fin (le temps de regarder le podium), même si des onglets restent ouverts ;
- une partie que tous les joueurs ont quittée, aussitôt.

Chaque partie programme ces vérifications sur le minuteur existant (`LiveScheduler`) ; pas de tâche périodique ni de
nouvelle dépendance.

**Limites.** 50 joueurs par partie (ADR 0021), une partie ouverte par groupe, et **200 parties en mémoire** pour toute
l'instance (`recallio.live.max-open-games`) : au-delà, **409** `live.too-many-games`, un refus temporaire.

**Métriques** (Micrometer, déjà prévu par la stack, exposé au format Prometheus sur `/q/metrics`) :

| Métrique | Type | Ce qu'elle dit |
|---|---|---|
| `recallio.live.games.open` | jauge | parties en mémoire (charge, fuites éventuelles) |
| `recallio.live.connections` | jauge | connexions WebSocket ouvertes sur des parties |
| `recallio.live.games.started`, `.finished` | compteurs | usage, et parties abandonnées en cours (l'écart) |
| `recallio.live.answers` | compteur | réponses reçues |

**Test de charge.** Un test automatisé (`LiveLoadTest`), dans la suite habituelle : **100 connexions WebSocket** suivent
la même partie ; chacune doit recevoir chaque étape jusqu'à la fin, et la plus lente reçoit la première question moins
de deux secondes après le démarrage. Il met à l'épreuve la diffusion, qui est le chemin coûteux ; un test plus large
(plusieurs parties, plusieurs instances) se fera sur l'environnement de déploiement.

## Conséquences

- `/q/metrics` n'est ouvert qu'en développement et en test ; en production, son accès (réseau interne ou
  authentification du collecteur) se décidera avec l'hébergement.
- Le seuil de 200 parties est une borne prudente pour une instance ; les métriques diront s'il faut l'ajuster.
- La reprise d'une partie après un redémarrage du backend reste hors périmètre (ADR 0021).
