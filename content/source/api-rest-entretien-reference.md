---
subject: api-rest
title: API REST
category: fundamentals
position: 160
status: published
---

# API REST : le document de référence pour les entretiens techniques

> Pour préparer un entretien : conception d'API REST et HTTP.
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (les ponts entre symptômes et notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ces chapitres viennent du document Java, dont ils ont été sortis pour devenir un sujet à part entière (ADR 0025).

═══════════════════════════════════════════
# 1. API REST
═══════════════════════════════════════════

### Vue d'ensemble

REST a été formalisé par Roy Fielding en 2000 comme un **style architectural**, pas un protocole. Son intuition : le Web fonctionne à très grande échelle grâce à quelques contraintes simples, et une API qui les respecte hérite des mêmes propriétés, cacheable, scalable, évolutive.

Les contraintes clés : une interface **uniforme** (mêmes verbes HTTP pour tout le monde), le **stateless** (chaque requête est autonome, donc n'importe quel serveur peut la traiter, c'est ce qui rend le scaling horizontal trivial), et la **manipulation de ressources** identifiées par des URIs plutôt que d'appels de procédures.

En pratique, la plupart des API dites REST n'implémentent pas toutes les contraintes (HATEOAS notamment est rare). Ce n'est pas grave : ce qui compte est de savoir **pourquoi** ces conventions existent, notamment pourquoi l'idempotence n'est pas un détail académique mais une propriété qui évite les doublons de paiement.

### Cours

**Ressources et verbes** : les URIs désignent des ressources (noms, au pluriel), les verbes HTTP disent quoi en faire.

| Verbe | Rôle | Idempotent | Safe |
|---|---|---|---|
| GET | lire | oui | oui |
| POST | créer | non | non |
| PUT | remplacer | oui | non |
| PATCH | modifier partiellement | pas garanti | non |
| DELETE | supprimer | oui | non |

**Safe** = ne modifie rien. **Idempotent** = rejouer donne le même état final. C'est fondamental en environnement réseau : un client qui subit un timeout ne sait pas si sa requête a abouti, et son réflexe est de réessayer.

**Codes de statut :**
| Code | Sens |
|---|---|
| 200 / 201 / 204 | OK / Créé (+ header Location) / Pas de contenu |
| 400 | Requête mal formée (syntaxe) |
| 401 / 403 | Non authentifié / Authentifié mais pas autorisé |
| 404 / 409 | Introuvable / Conflit (doublon, version) |
| 422 | Syntaxe correcte, validation métier échouée |
| 429 | Trop de requêtes (rate limiting) |
| 500 / 502 / 503 | Erreur serveur / Service amont en erreur / Indisponible |

**Conventions d'URI** : noms au pluriel, pas de verbes (`GET /users`, pas `/getUsers`). Sous-ressources : `/users/{id}/orders`. Filtres et pagination en query : `?status=active&page=0&size=20`.

**Pagination** : offset/limit (simple, mais coûteux et instable sur de gros volumes) ou **cursor-based** (on passe un curseur pointant le dernier élément vu, stable et performant, recommandé sur de gros jeux de données).

**Versioning** : par URI (`/api/v1/`, le plus courant et le plus lisible), par header (`Accept: application/vnd.api.v1+json`, plus pur mais moins pratique), ou par paramètre.

**Compatibilité** : ajouter un champ optionnel ou un endpoint est rétrocompatible ; supprimer, renommer, changer un type ou rendre un champ obligatoire ne l'est pas et impose une nouvelle version.

**Idempotence sur POST** : le POST n'est pas idempotent par nature, mais on peut le rendre sûr avec une **clé d'idempotence** transmise en header. Le serveur mémorise la clé et, si elle revient, renvoie le résultat de la première exécution au lieu de recréer. Indispensable sur les paiements et les réservations.

**HATEOAS** : les réponses embarquent des liens vers les actions possibles. Élégant, rarement implémenté.

**Autres styles** : **GraphQL** (le client choisit exactement les champs, évite le sur- et sous-chargement, mais complexifie le cache et la limitation de débit), **gRPC** (binaire via Protobuf, très performant, contrat fort, idéal en communication interne mais peu adapté à un navigateur).

**Documentation** : OpenAPI/Swagger, générée depuis le code, sert à la fois de contrat et de support de test.

**Autres codes HTTP à connaître** :
| Code | Sens |
|---|---|
| 202 Accepted | requête acceptée, traitement asynchrone en cours |
| 301 / 308 | redirection permanente (308 conserve la méthode et le corps) |
| 302 / 307 | redirection temporaire (307 conserve la méthode) |
| 304 Not Modified | la ressource n'a pas changé (cache conditionnel) |
| 405 Method Not Allowed | verbe non supporté sur cette ressource |
| 406 Not Acceptable | impossible de produire le format demandé (`Accept`) |
| 412 Precondition Failed | une condition (`If-Match`) n'est pas remplie |
| 413 / 415 | corps trop volumineux / type de contenu non supporté |
| 504 Gateway Timeout | le service amont n'a pas répondu à temps |

Les familles : 1xx information, 2xx succès, 3xx redirection, 4xx erreur du client, 5xx erreur du serveur.

**Cache HTTP** : `Cache-Control: max-age=3600` (réutiliser sans revalider pendant une heure), `no-cache` (on peut stocker, mais il faut **revalider** avant usage), `no-store` (ne rien stocker, pour les données sensibles), `private` (cache du navigateur uniquement) vs `public` (proxys et CDN aussi). **Validation** : le serveur envoie un `ETag` (empreinte de la version) ou `Last-Modified` ; le client renvoie `If-None-Match` / `If-Modified-Since` ; le serveur répond **304** sans corps si rien n'a changé.

**Concurrence optimiste en HTTP** : le client lit la ressource avec son `ETag`, puis envoie `PUT` avec `If-Match: "etag"` ; si quelqu'un l'a modifiée entre-temps, le serveur répond **412 Precondition Failed**. C'est l'équivalent HTTP du `@Version` de JPA.

**Formats de PATCH** : *JSON Merge Patch* (RFC 7396, on envoie un document partiel, `null` signifie suppression, simple mais ne gère pas les tableaux finement) et *JSON Patch* (RFC 6902, une liste d'opérations `add`, `remove`, `replace`, `move`, `test`).

**Modèle de maturité de Richardson** : niveau 0 (un seul endpoint, du RPC sur HTTP), niveau 1 (des ressources), niveau 2 (les verbes et codes HTTP utilisés correctement, là où se situent la plupart des API), niveau 3 (HATEOAS, des liens hypermédia qui guident le client).

**Opérations longues** : répondre **202 Accepted** avec un en-tête `Location` vers une ressource de suivi (`/jobs/42`) que le client interroge, ou notifier par **webhook** à la fin. Un webhook doit être signé (HMAC du corps avec un secret partagé, envoyé en en-tête) pour que le destinataire vérifie l'origine, être rejoué en cas d'échec, et le destinataire doit être idempotent.

**OpenAPI** : *contract-first* (on écrit la spécification, on génère le code serveur et les clients avec openapi-generator : le contrat est discuté avant d'être codé) vs *code-first* (la spécification est générée depuis les annotations, springdoc-openapi en Spring, smallrye-openapi en Quarkus). Le contrat sert aussi aux tests de non-régression d'API et à la génération de clients.

**Conventions de conception** : identifiants opaques dans les URI (pas d'auto-incrément devinable sur une API publique), dates en ISO 8601, noms de champs cohérents (camelCase en général), enveloppe de pagination (`content`, `page`, `size`, `totalElements` ou `nextCursor`), filtres et tri en paramètres (`?sort=nom,desc`), actions qui ne rentrent pas dans le CRUD exprimées comme des sous-ressources (`POST /commandes/42/annulation`). Déprécier proprement : en-têtes `Deprecation` et `Sunset`.

**HEAD et OPTIONS** : HEAD renvoie uniquement les en-têtes d'un GET ; OPTIONS décrit les méthodes autorisées et sert aux requêtes *preflight* CORS.

**gRPC en détail** : contrat dans un fichier `.proto`, sérialisation Protobuf, transport HTTP/2. Quatre modes : unaire, streaming serveur, streaming client, bidirectionnel. **Deadlines** propagées entre services, codes de statut propres (`NOT_FOUND`, `UNAVAILABLE`, `DEADLINE_EXCEEDED`). Compatibilité : on n'ajoute que de nouveaux numéros de champ, on ne réutilise jamais un numéro supprimé (`reserved`). Peu adapté au navigateur (gRPC-Web ou une passerelle), illisible sans outils (grpcurl).

**GraphQL en détail** : un schéma typé, un seul endpoint, des *queries* (lecture), *mutations* (écriture) et *subscriptions* (temps réel). Le serveur exécute des *resolvers* par champ, d'où un risque de **N+1** (résolu par le pattern **DataLoader**, qui regroupe les chargements). Protections nécessaires : limite de profondeur et de complexité des requêtes, requêtes persistées. Les erreurs sont renvoyées dans le corps avec un statut 200, et le cache HTTP est peu exploitable. En Java : Spring for GraphQL, SmallRye GraphQL.

**Temps réel** : *polling* (simple, gaspilleur), *long polling*, **SSE** (Server-Sent Events, flux serveur vers client sur HTTP, reconnexion automatique, idéal pour des notifications à sens unique), **WebSocket** (bidirectionnel, protocole distinct après une négociation HTTP, adapté au chat ou aux jeux).

### En situation

- **"Conçois une API pour X"** → ressources, endpoints, codes, pagination, idempotence.
- **"Éviter les doublons de création"** → clé d'idempotence.
- **"Faire évoluer sans casser les clients"** → rétrocompatibilité et versioning.
- **"Protéger contre les abus"** → rate limiting, pagination bornée.
- **"REST ou gRPC ?"** → contexte d'appel, performance, clients.

### Questions techniques

**Q : Pourquoi le stateless facilite le scaling ?** R : Chaque requête est autonome, donc n'importe quelle instance peut la traiter, pas d'affinité de session.

**Q : PUT vs PATCH ?** R : PUT remplace la ressource entière ; PATCH modifie les champs fournis.

**Q : Qu'est-ce qui est idempotent ?** R : GET, PUT, DELETE. POST non.

**Q : 401 vs 403 ?** R : Non authentifié vs authentifié mais non autorisé.

**Q : 400 vs 422 ?** R : Syntaxe invalide vs syntaxe valide mais règle métier violée.

**Q : Pagination offset vs cursor ?** R : Offset simple mais coûteux et instable sur gros volumes ; cursor stable et performant.

**Q : Comment rendre un POST idempotent ?** R : Avec une clé d'idempotence en header, mémorisée côté serveur.

**Q : Quels changements sont rétrocompatibles ?** R : Ajouter un champ optionnel ou un endpoint. Pas supprimer, renommer ou changer un type.

**Q : REST vs GraphQL vs gRPC ?** R : REST universel et cacheable ; GraphQL flexible côté client mais cache complexe ; gRPC binaire et performant, idéal en interne.

**Q : 301 vs 302 vs 307 vs 308 ?** R : Permanente vs temporaire ; 307 et 308 conservent obligatoirement la méthode et le corps.

**Q : À quoi sert le code 202 ?** R : Indiquer qu'une requête est acceptée pour un traitement asynchrone.

**Q : no-cache vs no-store ?** R : Stockage autorisé mais revalidation obligatoire vs aucun stockage.

**Q : Comment fonctionne un ETag ?** R : Le serveur fournit une empreinte de version ; le client la renvoie dans If-None-Match et reçoit 304 si rien n'a changé.

**Q : Comment éviter qu'un PUT écrase une modification concurrente ?** R : If-Match avec l'ETag lu ; le serveur répond 412 si la ressource a changé.

**Q : JSON Patch vs JSON Merge Patch ?** R : Liste d'opérations (RFC 6902) vs document partiel fusionné (RFC 7396).

**Q : Les niveaux de Richardson ?** R : 0 RPC sur HTTP, 1 ressources, 2 verbes et codes HTTP, 3 HATEOAS.

**Q : Comment exposer une opération longue ?** R : 202 + Location vers une ressource de suivi, ou un webhook en fin de traitement.

**Q : Comment sécuriser un webhook ?** R : Signature HMAC du corps avec un secret partagé, vérifiée par le destinataire, plus un horodatage contre le rejeu.

**Q : Contract-first vs code-first ?** R : La spécification OpenAPI d'abord puis génération du code, vs spécification générée depuis le code.

**Q : 406 vs 415 ?** R : Le serveur ne peut pas produire le format demandé (Accept) vs il ne comprend pas le format envoyé (Content-Type).

**Q : Les quatre modes gRPC ?** R : Unaire, streaming serveur, streaming client, bidirectionnel.

**Q : Comment faire évoluer un message Protobuf sans casse ?** R : Ajouter des champs avec de nouveaux numéros, ne jamais réutiliser un numéro supprimé.

**Q : Comment GraphQL résout-il le problème N+1 ?** R : Avec des DataLoaders qui regroupent les chargements par lot.

**Q : SSE vs WebSocket ?** R : Flux unidirectionnel serveur → client sur HTTP vs canal bidirectionnel.

**Q : À quoi sert une requête OPTIONS ?** R : Connaître les méthodes autorisées ; c'est la requête preflight du CORS.

### Questions de situation

**Q : "Conçois une API de réservation de vol."**
R : "Je pars des **ressources** : vols, réservations, passagers. Puis les endpoints :
- `GET /flights?date=&destination=&page=&size=`, recherche paginée.
- `GET /flights/{id}`, détail.
- `POST /bookings`, création, réponse **201** avec header `Location`.
- `GET /bookings/{id}` et `DELETE /bookings/{id}`.

Sur les **codes** : 201 à la création, 404 si le vol n'existe pas, 409 s'il n'y a plus de place, 422 si une règle métier échoue.

Le point que je soulèverais spontanément, c'est l'**idempotence de la création**. Sur une réservation c'est critique : si le client subit un timeout et rejoue, il ne doit pas obtenir deux réservations et deux débits. Une **clé d'idempotence** en header résout ça, le serveur mémorise la clé et renvoie le résultat initial.

J'ajouterais la validation des entrées, un format d'erreur cohérent avec un code applicatif stable, le versioning dès le départ, et une taille de page maximale imposée côté serveur."

**Q : "Comment faire évoluer une API sans casser les clients ?"**
R : "Je distinguerais les changements **rétrocompatibles** des changements cassants.
Rétrocompatible, donc sans nouvelle version : ajouter un champ optionnel en réponse, ajouter un endpoint, ajouter un paramètre facultatif. Les clients ignorent ce qu'ils ne connaissent pas, à condition d'être tolérants, ce qu'il faut documenter comme attendu.
Cassant : supprimer ou renommer un champ, changer un type, rendre un paramètre obligatoire, modifier la sémantique d'un code de retour. Là il faut **versionner** (le plus souvent par URI) et faire coexister les deux versions le temps de la migration, avec une date de fin de support annoncée. Ça a un coût de maintenance réel, donc on ne versionne pas à la légère.
En amont, la meilleure protection est de ne jamais exposer directement ses entités internes : passer par des DTOs permet de faire évoluer le modèle sans toucher au contrat."

**Q : "Un client appelle ton API 10 000 fois par minute et sature le service."**
R : "Je mettrais du **rate limiting**, idéalement au niveau de l'API Gateway pour rejeter le trafic avant qu'il ne consomme des ressources applicatives : un quota par client et par fenêtre, un **429** au-delà avec un header `Retry-After`.
Mais je chercherais aussi **pourquoi** il appelle autant. Souvent c'est un défaut de conception d'API : le client fait du polling faute de notification, ou N appels unitaires faute d'endpoint de batch. Dans ce cas la vraie réponse est de lui offrir un endpoint bulk ou des webhooks, pas seulement de le bloquer.
Je vérifierais enfin qu'un **cache HTTP** est en place si les données s'y prêtent : avec les bons headers, une part du trafic n'atteint même plus le service."

**Q : "Une API publique doit supporter des millions de lectures de catalogue produit par jour."**
R : "Le levier principal est le **cache HTTP** à tous les niveaux : `Cache-Control: public, max-age=...` adapté à la fréquence de mise à jour, des `ETag` pour les revalidations à 304, et un **CDN** devant l'API pour que la majorité des requêtes n'atteigne jamais le serveur.
Côté serveur : un cache applicatif ou Redis pour les lectures qui passent, des réponses paginées par curseur, de la compression, et des projections qui ne chargent que les champs exposés.
L'invalidation se fait par expiration courte, ou par purge ciblée du CDN lors d'une modification. Et un rate limiting par clé d'API pour protéger le service des abus."
