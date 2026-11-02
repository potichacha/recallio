---
subject: cache
title: Cache
category: data
position: 90
status: published
---

# Cache : le document de référence pour les entretiens techniques

> Pour préparer un entretien : stratégies de cache, invalidation, Redis.
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (les ponts entre symptômes et notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ces chapitres viennent du document Java, dont ils ont été sortis pour devenir un sujet à part entière (ADR 0025).

═══════════════════════════════════════════
# 1. CACHE
═══════════════════════════════════════════

### Vue d'ensemble

Le cache est souvent le meilleur rapport gain/effort en performance : il évite un travail plutôt que de l'accélérer. Une donnée servie depuis la mémoire n'atteint ni la base, ni le réseau.

Mais il introduit un problème structurel : la **cohérence**. Dès qu'une donnée est copiée, elle peut devenir périmée. D'où la boutade attribuée à Phil Karlton, les deux choses difficiles en informatique sont l'invalidation de cache et le nommage des variables. La vraie question n'est jamais "faut-il cacher ?" mais **"quelle péremption est acceptable, et comment j'invalide ?"**

### Cours

**Niveaux de cache** : navigateur → CDN → cache HTTP (reverse proxy) → cache applicatif local (Caffeine) → cache distribué (Redis) → cache de base (buffer pool). Chaque niveau évite du travail au suivant.

**Local vs distribué** : un cache local (dans la JVM) est très rapide mais **dupliqué par instance** : donc incohérent entre pods et perdu au redémarrage. Un cache distribué (Redis) est partagé et cohérent, au prix d'un appel réseau.

**Stratégies :**
- **Cache-aside** (le plus courant) : l'application interroge le cache ; si absent, elle lit la base et alimente le cache.
- **Read-through** : le cache lui-même va chercher la donnée manquante.
- **Write-through** : l'écriture passe par le cache qui met la base à jour, cohérent mais plus lent.
- **Write-behind** : écriture différée en base, rapide mais risque de perte.

**Invalidation :**
- **TTL** : expiration par durée. Simple et robuste, au prix d'une fenêtre de péremption acceptée.
- **Invalidation explicite** : on supprime l'entrée à l'écriture. Plus juste, mais il faut penser à tous les chemins d'écriture.
- **Versionnement de clé** : on change la clé plutôt que d'invalider.

**Politiques d'éviction** quand le cache est plein : LRU (le moins récemment utilisé), LFU (le moins fréquemment), FIFO.

**Pièges classiques :**
- **Cache stampede** : de nombreuses entrées expirent en même temps et tous les clients tapent la base simultanément. Défenses : TTL avec un jitter aléatoire, verrou de recalcul, rafraîchissement anticipé.
- **Cacher ce qui ne devrait pas l'être** : données personnelles, données à cohérence forte.
- **Cache sans limite de taille** : c'est une fuite mémoire déguisée, toujours borner par taille ou par TTL.
- **Clé de cache mal conçue** : oublier un critère (la locale, l'utilisateur) fait servir la donnée d'un autre.

**Ce qu'il faut cacher** : les données lues beaucoup plus souvent qu'écrites, coûteuses à produire, et tolérantes à une légère péremption. Référentiels, résultats de calculs, réponses d'API externes.

**Métrique clé** : le **hit ratio**. Un cache à 20% de hits ajoute de la complexité pour presque rien.

**Caffeine** : le cache local de référence en Java (successeur de Guava Cache). Éviction par taille (`maximumSize`) ou poids, expiration (`expireAfterWrite`, `expireAfterAccess`, ou personnalisée), rafraîchissement asynchrone (`refreshAfterWrite` : l'ancienne valeur reste servie pendant le recalcul), statistiques (`recordStats`), politique **W-TinyLFU** qui obtient un meilleur taux de succès que le LRU pur. `LoadingCache` et `AsyncLoadingCache` évitent les calculs concurrents d'une même clé.

**Abstraction de cache Spring** : `@EnableCaching`, `@Cacheable(value = "produits", key = "#id")`, `@CachePut` (met à jour sans court-circuiter la méthode), `@CacheEvict(allEntries = true)`, `@Caching`, conditions `condition`/`unless` (`unless = "#result == null"`). Fournisseurs : Caffeine, Redis, Hazelcast, JCache. Pièges : appel interne qui contourne le proxy, clé par défaut construite sur tous les paramètres, objets mutables mis en cache (un appelant qui modifie l'objet renvoyé modifie le cache local), sérialisation nécessaire pour Redis, `sync = true` pour éviter le stampede local.

**Redis comme cache distribué** : TTL par clé (`SET cle valeur EX 300`), politiques d'éviction quand la mémoire est pleine (`maxmemory-policy` : `allkeys-lru`, `allkeys-lfu`, `volatile-ttl`, `noeviction`), mono-thread pour les commandes (une commande lente comme `KEYS *` bloque tout, utiliser `SCAN`), clients Java Lettuce (défaut de Spring Data Redis) et Jedis, Redisson (structures distribuées, verrous). Haute disponibilité par Sentinel, partitionnement par Redis Cluster (16 384 *slots*). Licence : Redis a changé de licence en 2024 puis est revenu à une licence open source (AGPL) en 2025 ; **Valkey** est le fork open source soutenu par la fondation Linux.

**Problèmes supplémentaires** :
- **Cache penetration** : des requêtes répétées pour des clés **inexistantes** traversent toujours le cache. Défense : mettre en cache l'absence (valeur "vide" avec TTL court), ou un **filtre de Bloom** (structure probabiliste qui dit "absent à coup sûr" ou "peut-être présent").
- **Hot key** : une clé extrêmement demandée sature un nœud du cache distribué ; défense : cache local devant (deux niveaux), réplication de la clé.
- **Cache à deux niveaux** : local (Caffeine, nanosecondes) + distribué (Redis, millisecondes), avec invalidation diffusée (pub/sub) pour limiter les incohérences entre instances.
- **Incohérence base/cache** : mettre à jour la base puis **supprimer** l'entrée du cache (plutôt que la réécrire, ce qui évite qu'une écriture concurrente plus ancienne réinsère une valeur périmée) ; pour une cohérence plus forte, invalider via CDC.

**Cache HTTP et CDN** : `Cache-Control`, `ETag`, revalidation (voir le chapitre REST) ; un CDN met en cache près des utilisateurs le contenu statique et les réponses d'API publiques, avec purge ciblée.

**Cache de second niveau Hibernate** : voir le chapitre JPA.

### En situation

- **"Comment tu réduis la charge sur la base ?"** → cache aux bons endroits.
- **"Des données périmées s'affichent"** → stratégie d'invalidation.
- **"Le cache consomme trop de mémoire"** → éviction, bornage.
- **"Pic de charge à l'expiration"** → cache stampede.

### Questions techniques

**Q : Cache local vs distribué ?** R : Local très rapide mais dupliqué et incohérent entre instances ; distribué partagé au prix d'un appel réseau.

**Q : C'est quoi le cache-aside ?** R : L'application lit le cache, et en cas d'absence lit la base puis alimente le cache.

**Q : TTL ou invalidation explicite ?** R : TTL simple avec péremption acceptée ; invalidation plus juste mais nécessite de couvrir tous les chemins d'écriture.

**Q : Politiques d'éviction ?** R : LRU, LFU, FIFO.

**Q : C'est quoi le cache stampede ?** R : Expiration simultanée de nombreuses entrées, provoquant un afflux sur la base. Défense : jitter, verrou, rafraîchissement anticipé.

**Q : Que ne faut-il pas cacher ?** R : Les données à cohérence forte et les données personnelles sensibles.

**Q : Quelle métrique surveiller ?** R : Le hit ratio, un cache peu efficace ajoute de la complexité sans gain.

**Q : Pourquoi Caffeine plutôt qu'une ConcurrentHashMap ?** R : Éviction par taille, expiration, rafraîchissement, statistiques et chargement sans calcul concurrent.

**Q : expireAfterWrite vs refreshAfterWrite ?** R : L'entrée disparaît après le délai vs elle est recalculée en arrière-plan tandis que l'ancienne valeur reste servie.

**Q : @Cacheable vs @CachePut vs @CacheEvict ?** R : Court-circuite la méthode si la valeur est en cache / exécute toujours et met à jour / supprime des entrées.

**Q : Pourquoi éviter KEYS * en production Redis ?** R : Commande en O(n) sur un serveur mono-thread : elle bloque toutes les autres ; utiliser SCAN.

**Q : Politiques d'éviction de Redis ?** R : allkeys-lru, allkeys-lfu, volatile-lru, volatile-ttl, allkeys-random, noeviction.

**Q : C'est quoi la cache penetration ?** R : Des requêtes sur des clés inexistantes qui atteignent toujours la base ; parade : cacher l'absence ou un filtre de Bloom.

**Q : C'est quoi un filtre de Bloom ?** R : Une structure probabiliste compacte qui répond "absent" avec certitude ou "peut-être présent".

**Q : Faut-il mettre à jour ou supprimer l'entrée de cache après une écriture en base ?** R : La supprimer, ce qui limite les risques de réinsérer une valeur périmée.

**Q : C'est quoi Valkey ?** R : Le fork open source de Redis soutenu par la fondation Linux.

### Questions de situation

**Q : "Quand tu mets du cache, et quels risques ?"**
R : "Je cache quand trois conditions sont réunies : la donnée est **lue bien plus souvent qu'écrite**, elle est **coûteuse à produire**, et une légère **péremption est acceptable** fonctionnellement. Ce dernier point est une décision métier, pas technique, il faut la valider.
Le risque principal est l'**invalidation**. Un cache mal invalidé sert des données périmées, et ce genre de bug est difficile à reproduire. Je combine donc un TTL, qui garantit une borne même si j'oublie un cas, et une invalidation explicite sur les écritures pour la justesse.
Deux autres pièges : le **cache stampede**, quand tout expire au même moment et que la base prend un afflux (j'ajoute un jitter sur les TTL ; et le **cache non borné**, qui est une fuite mémoire déguisée) toujours une taille maximale avec éviction LRU.
Et je surveillerais le **hit ratio** : si le cache n'est utile que dans 20% des cas, il ajoute de la complexité pour presque rien, mieux vaut l'enlever."

**Q : "Après une mise à jour d'un produit, certaines instances affichent encore l'ancien prix pendant plusieurs minutes."**
R : "Un cache **local** par instance : l'instance qui a traité l'écriture a invalidé son cache, les autres non. Trois corrections possibles : réduire le TTL local à une durée acceptable pour le métier ; diffuser l'invalidation à toutes les instances (pub/sub Redis, un topic Kafka) ; ou passer à un cache distribué unique, cohérent entre instances, au prix d'un appel réseau. Pour un prix, je validerais avec le métier quelle péremption est acceptable : si c'est zéro, cette donnée ne devrait peut-être pas être mise en cache côté local du tout."
