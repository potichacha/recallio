---
subject: quarkus
title: Quarkus
category: frameworks
position: 40
status: draft
---

# Quarkus : le document de référence pour les entretiens techniques

> Pour préparer un entretien : Quarkus, son mode dev, le build time, CDI, la compilation native et les extensions.
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (les ponts entre symptômes et notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ces chapitres viennent du document Java, dont ils ont été sortis pour devenir un sujet à part entière (ADR 0025).

═══════════════════════════════════════════
# 1. QUARKUS
═══════════════════════════════════════════

### Vue d'ensemble

Quarkus (Red Hat, 2019) part d'un constat : les frameworks Java historiques ont été conçus pour des serveurs d'application démarrés une fois et laissés en place des mois. Le cloud a inversé le modèle, des conteneurs éphémères, créés et détruits en permanence, où **le temps de démarrage et l'empreinte mémoire deviennent des critères de premier plan**, à la fois pour la réactivité du scaling et pour la facture d'infrastructure.

La réponse de Quarkus est structurelle : déplacer au **build-time** ce que les autres font au **runtime**. Le scan des annotations, la résolution des dépendances CDI, la construction des métadonnées sont calculés à la compilation et figés dans le binaire. À l'exécution, il ne reste presque plus de réflexion, d'où un démarrage en dizaines de millisecondes et une mémoire réduite de moitié ou plus. La compilation native GraalVM pousse la logique jusqu'au bout.

Quarkus s'appuie sur des **standards** (CDI, JAX-RS, MicroProfile) plutôt que sur des API propriétaires, ce qui limite l'enfermement.

### Cours

**CDI (via ArC)** : l'injection standard Java, résolue au build-time.

| Portée | Proxy | Instanciation |
|---|---|---|
| `@ApplicationScoped` | oui | paresseuse, une instance |
| `@Singleton` | non | immédiate, une instance |
| `@RequestScoped` | oui | une par requête |
| `@Dependent` | non | une par point d'injection |

`@ApplicationScoped` est le défaut recommandé : le proxy permet l'initialisation paresseuse et les intercepteurs. `@Singleton` évite le proxy, marginalement plus rapide à l'accès, mais sans intercepteurs.

Autres annotations utiles : `@Produces` (méthode fabriquant un bean), `@Named`/`@Qualifier` (distinguer plusieurs implémentations d'une même interface), `@Startup` (initialisation au démarrage).

**REST avec JAX-RS** : `@Path`, `@GET`/`@POST`, `@PathParam`, `@QueryParam`, `@Produces`, `@Consumes`. Gestion d'erreurs par `ExceptionMapper`.

**MicroProfile** :
- **Config** : `@ConfigProperty(name="ma.cle")`, avec surcharge par variables d'environnement.
- **Health** : `/q/health/live` et `/q/health/ready`, consommés par Kubernetes.
- **Metrics** : endpoint Prometheus via Micrometer.
- **Fault Tolerance** : `@Retry`, `@Timeout`, `@CircuitBreaker`, `@Fallback`, `@Bulkhead`.
- **OpenAPI** : spécification générée automatiquement.
- **REST Client** : `@RegisterRestClient` sur une interface, l'implémentation est générée.

**Panache** : surcouche simplifiant JPA, en style *active record* (l'entité porte ses méthodes) ou *repository*. Réduit fortement le code de persistance.

**Réactif avec Mutiny** : `Uni` (zéro ou un résultat) et `Multi` (flux). Le modèle réactif permet de servir beaucoup de requêtes concurrentes avec peu de threads, en s'appuyant sur l'event loop de Vert.x. Utile pour de l'I/O intensif ; inutile (voire contre-productif en complexité) sur un service classique à charge modérée.

**Dev mode** : `quarkus dev` offre le rechargement à chaud, et les **Dev Services** démarrent automatiquement une base ou un Kafka en conteneur pour le développement et les tests.

**Compilation native** : via GraalVM, en AOT. Démarrage en millisecondes et empreinte minimale, au prix d'un build long et de contraintes sur la réflexion (à déclarer explicitement).

**Comparaison synthétique :**

| | Quarkus | Spring Boot |
|---|---|---|
| Injection | CDI/ArC, build-time | Spring IoC, runtime |
| REST | JAX-RS | Spring MVC |
| Démarrage | ~0,5 s JVM, ~10 ms natif | quelques secondes |
| Mémoire | faible | plus élevée |
| Écosystème | plus restreint | très vaste et mature |

**Le modèle de build de Quarkus** : une application est un assemblage d'**extensions** (`quarkus-rest`, `quarkus-hibernate-orm-panache`, `quarkus-smallrye-health`, `quarkus-oidc`...). Chaque extension a une partie *deployment* qui s'exécute **au build** (analyse du code avec Jandex, l'index des annotations, génération de bytecode, préparation des métadonnées natives) et une partie *runtime* minimale. Lister et ajouter : `quarkus extension list`, `quarkus extension add`.

**Threads : event loop vs worker.** Quarkus repose sur Vert.x. Les requêtes peuvent s'exécuter sur un thread d'**I/O** (event loop, à ne jamais bloquer) ou sur un thread **worker**. Avec **Quarkus REST** (anciennement RESTEasy Reactive), la règle est déduite de la signature : une méthode qui retourne un type réactif (`Uni`, `Multi`) s'exécute sur l'event loop, une méthode à type classique sur un worker. On force avec `@Blocking` / `@NonBlocking`, et `@RunOnVirtualThread` exécute la méthode sur un virtual thread. Bloquer l'event loop déclenche un avertissement *thread blocked* ou une `BlockingOperationNotAllowedException`.

**Configuration** : `application.properties` (ou YAML via extension), profils intégrés avec préfixe `%dev.`, `%test.`, `%prod.` (`%prod.quarkus.datasource.jdbc.url=...`), surcharge par variables d'environnement (`QUARKUS_DATASOURCE_JDBC_URL`), `@ConfigProperty` pour une valeur, `@ConfigMapping` sur une interface pour un groupe typé. Certaines propriétés sont **fixées au build** (elles ne se changent pas au runtime sans reconstruire), ce qui surprend en venant de Spring.

**CDI dans Quarkus (ArC), spécificités** : injection préférée en champ **package-private** (l'injection dans un champ `private` fonctionne mais oblige à de la réflexion, coûteuse en natif) ; les beans non utilisés sont **supprimés au build** (attention aux beans recherchés dynamiquement, à annoter `@Unremovable`) ; `@DefaultBean` (bean remplaçable par l'application), `@IfBuildProfile` / `@UnlessBuildProfile`, `@LookupIfProperty`, `@Alternative` + `@Priority`. Intercepteurs avec `@InterceptorBinding` + `@Interceptor` + `@AroundInvoke`, décorateurs CDI, événements avec `Event<T>.fire()` et `@Observes` / `@ObservesAsync`. Cycle de vie : observer `StartupEvent` et `ShutdownEvent`, ou `@Startup`.

**Persistance** : Hibernate ORM avec **Panache** en deux styles. *Active record* : `class Personne extends PanacheEntity` (identifiant fourni, méthodes statiques `Personne.find("nom", nom).list()`, `persist()`). *Repository* : `class PersonneRepository implements PanacheRepository<Personne>`. Requêtes simplifiées (`find("statut = ?1 and age > ?2", s, 18)`), pagination (`page(Page.of(0, 20))`). Hibernate Reactive + Panache réactif pour une pile non bloquante. Transactions avec `@Transactional` (Jakarta).

**Autres briques courantes** : REST Client (`@RegisterRestClient(configKey = "api-x")` + URL en configuration), `quarkus-oidc` (Keycloak, sécurisation par jeton), SmallRye Reactive Messaging pour Kafka (`@Incoming("commandes")`, `@Outgoing`, `Emitter<T>` pour publier depuis du code impératif), `quarkus-scheduler` (`@Scheduled(every = "10s")`), Qute (templates), gRPC, Micrometer et OpenTelemetry, cache (`@CacheResult`).

**Outillage développeur** : `quarkus dev` (rechargement à chaud), **Dev UI** (`/q/dev-ui` : configuration, beans, endpoints, Dev Services), **continuous testing** (les tests impactés se relancent à chaque modification), Dev Services (PostgreSQL, Kafka, Keycloak lancés automatiquement en conteneur).

**Packaging** : *fast-jar* par défaut (`target/quarkus-app/`, démarrage optimisé), *uber-jar* en option, exécutable natif avec `./mvnw package -Dnative` (ou `-Dquarkus.native.container-build=true` pour compiler dans un conteneur sans installer GraalVM/Mandrel). Génération d'images de conteneur et de manifestes Kubernetes par extensions (`quarkus-container-image-jib`, `quarkus-kubernetes`).

**Tests** : `@QuarkusTest` (application démarrée une fois pour toute la suite), `@InjectMock`, `@TestProfile` (profil de test dédié), `@QuarkusIntegrationTest` (teste l'artefact final, jar ou natif, en boîte noire), `@QuarkusTestResource` pour gérer des dépendances.

**Compatibilité Spring** : des extensions acceptent les annotations Spring (`@Autowired`, `@RestController`, Spring Data JPA) pour faciliter une migration, mais seulement un sous-ensemble.

### En situation

- **"Pourquoi Quarkus plutôt que Spring Boot ?"** → cloud-native, démarrage, mémoire, coût.
- **"Comment rendre un service résilient ?"** → Fault Tolerance.
- **"Comment Kubernetes sait que le service est prêt ?"** → health checks.
- **"Quand utiliser le réactif ?"** → I/O intensif, forte concurrence.

### Questions techniques

**Q : Pourquoi Quarkus démarre plus vite ?** R : Résolution CDI et scan d'annotations au build-time, donc peu de réflexion au démarrage.

**Q : @ApplicationScoped vs @Singleton ?** R : Proxy et instanciation paresseuse (intercepteurs possibles) vs instance directe sans proxy.

**Q : C'est quoi MicroProfile ?** R : Un ensemble de specs microservices : Config, Health, Metrics, Fault Tolerance, OpenAPI, REST Client.

**Q : Les annotations de Fault Tolerance ?** R : @Retry, @Timeout, @CircuitBreaker, @Fallback, @Bulkhead.

**Q : C'est quoi Panache ?** R : Une surcouche JPA simplifiant la persistance, en style active record ou repository.

**Q : Uni vs Multi en Mutiny ?** R : Uni = zéro ou un résultat ; Multi = un flux d'éléments.

**Q : Contraintes de la compilation native ?** R : Build plus long et réflexion à déclarer explicitement.

**Q : Que fait une extension Quarkus au build ?** R : Elle analyse l'index des annotations (Jandex), génère le code d'initialisation et prépare les métadonnées natives, pour ne laisser presque rien au runtime.

**Q : Event loop vs worker thread en Quarkus ?** R : Le thread d'I/O Vert.x ne doit jamais bloquer ; les traitements bloquants vont sur un worker.

**Q : Comment Quarkus REST choisit-il le thread ?** R : Selon la signature : type réactif → event loop, type classique → worker ; modifiable avec @Blocking/@NonBlocking.

**Q : À quoi sert @RunOnVirtualThread ?** R : Exécuter un endpoint sur un virtual thread.

**Q : Comment déclarer une valeur différente en production ?** R : Avec le préfixe de profil : `%prod.ma.cle=valeur`.

**Q : @ConfigProperty vs @ConfigMapping ?** R : Une valeur isolée vs un groupe de propriétés typé sur une interface.

**Q : Pourquoi éviter l'injection dans un champ private en Quarkus ?** R : Elle nécessite de la réflexion, coûteuse surtout en natif ; on préfère package-private ou le constructeur.

**Q : Pourquoi un bean peut-il disparaître en Quarkus ?** R : Les beans jugés inutilisés sont supprimés au build ; @Unremovable les conserve.

**Q : Active record vs repository avec Panache ?** R : L'entité porte les méthodes de persistance (statiques) vs une classe dédiée qui les porte.

**Q : Comment consommer un topic Kafka en Quarkus ?** R : SmallRye Reactive Messaging avec @Incoming sur une méthode.

**Q : C'est quoi la Dev UI ?** R : Une console de développement (/q/dev-ui) qui expose configuration, beans, endpoints et Dev Services.

**Q : @QuarkusTest vs @QuarkusIntegrationTest ?** R : Test dans la JVM avec injection vs test en boîte noire de l'artefact final (jar ou natif).

**Q : Comment compiler en natif sans installer GraalVM ?** R : `-Dquarkus.native.container-build=true`, qui compile dans un conteneur (Mandrel).

**Q : Que signifie @Blocking en Quarkus ?** R : La méthode s'exécute sur un thread worker et non sur l'event loop.

### Questions de situation

**Q : "Pourquoi choisir Quarkus plutôt que Spring Boot ?"**
R : "Ça dépend du contexte, je ne dirais pas que l'un domine dans l'absolu.

Quarkus se justifie sur du **cloud-native conteneurisé** : démarrage en une fraction de seconde et empreinte mémoire réduite de moitié ou plus. Concrètement en Kubernetes, un pod qui démarre en 0,5 seconde absorbe un pic bien mieux qu'un pod à cinq secondes, et on loge plus d'instances sur les mêmes ressources, donc un coût d'infrastructure moindre. C'est l'argument qui revient le plus souvent quand une équipe le retient pour de nouveaux microservices.

Spring Boot conserve un avantage réel : un **écosystème beaucoup plus vaste**, une communauté immense, et une compatibilité avec pratiquement toutes les librairies du marché. Sur un projet avec de nombreuses intégrations tierces, ou une équipe déjà experte, c'est souvent le choix rationnel.

Mon critère : scaling fréquent et sensibilité au coût d'infra → Quarkus. Écosystème riche, contraintes d'intégration, expertise existante → Spring Boot."

**Q : "Comment tu rends un service résilient aux pannes de ses dépendances ?"**
R : "Quatre mécanismes combinés, tous disponibles en annotations MicroProfile.

**Timeout** d'abord, sur tout appel externe sans exception. Sans lui, un service lent immobilise des threads, le pool sature, et mon service tombe alors qu'il va bien lui-même. C'est la protection la plus importante et la plus souvent négligée.

**Retry** avec backoff exponentiel pour les erreurs transitoires, mais uniquement sur des opérations **idempotentes** : rejouer un paiement serait catastrophique.

**Circuit breaker** : au-delà d'un taux d'échec, on coupe. Ça protège le service défaillant d'être achevé par les retries et libère mes ressources.

**Fallback** : valeur par défaut, donnée en cache ou fonctionnalité réduite, pour offrir une dégradation plutôt qu'une erreur.

Et un **bulkhead** pour cloisonner les ressources par dépendance."

**Q : "Un endpoint Quarkus provoque des avertissements 'thread blocked' et le service ralentit."**
R : "Un traitement bloquant (JDBC, client HTTP synchrone, fichier) s'exécute sur un thread d'**event loop**, souvent parce que la méthode retourne un `Uni` alors qu'elle appelle du code bloquant, ou qu'elle a été annotée `@NonBlocking`.
Correction : soit rendre toute la chaîne non bloquante (Hibernate Reactive, REST Client réactif), soit déplacer l'exécution sur un worker avec `@Blocking`, ou sur un virtual thread avec `@RunOnVirtualThread`. Le message d'avertissement contient la pile du thread bloqué, qui désigne précisément l'appel fautif."
