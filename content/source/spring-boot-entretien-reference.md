---
subject: spring-boot
title: Spring Boot
category: frameworks
position: 30
status: published
---

# Spring Boot : le document de référence pour les entretiens techniques

> Pour préparer un entretien : Spring Boot et l'écosystème Spring (configuration, injection, Spring Data, Spring Security, Spring Cloud, Spring Batch).
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (les ponts entre symptômes et notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ces chapitres viennent du document Java, dont ils ont été sortis pour devenir un sujet à part entière (ADR 0025).

═══════════════════════════════════════════
# 1. SPRING BOOT
═══════════════════════════════════════════

### Vue d'ensemble

Spring est né en 2003 en réaction à la lourdeur de Java EE. Son idée fondatrice est l'**inversion de contrôle** : plutôt que le code crée et assemble ses propres dépendances, un conteneur s'en charge. C'est la mise en œuvre concrète du principe de Dependency Inversion, le code devient testable, découplé, et reconfigurable sans recompilation.

Spring Boot (2014) ajoute une seconde idée : la **convention plutôt que la configuration**. Au lieu de déclarer explicitement chaque composant, le framework observe ce qui est présent dans le classpath et configure automatiquement ce qui va avec. Une dépendance web dans le pom suffit à obtenir un serveur configuré.

Ce qu'un entretien teste vraiment sur Spring : est-ce que tu comprends **ce qui se passe sous le capot** (proxies, cycle de vie des beans, portées) ou est-ce que tu poses des annotations en espérant que ça marche. Les questions sur `@Transactional` et l'injection sont exactement là pour ça.

### Cours

**Le conteneur IoC** instancie les beans, résout et injecte leurs dépendances, gère leur cycle de vie. Un **bean** est simplement un objet géré par ce conteneur.

**Stéréotypes** : `@Component` (générique), `@Service` (métier), `@Repository` (accès données, avec traduction des exceptions techniques en exceptions Spring), `@Controller`/`@RestController` (web). Techniquement équivalents, sémantiquement distincts, la distinction sert la lisibilité et permet un ciblage par AOP.

**Les trois injections :**
- **Constructeur** (recommandé) : dépendances garanties non nulles à la création, champs `final` possibles, testable sans démarrer Spring, et un constructeur qui gonfle signale une classe qui fait trop.
- **Setter** : pour les dépendances réellement optionnelles ou reconfigurables.
- **Champ** (`@Autowired` sur l'attribut) : compact mais déconseillé, dépendances invisibles depuis l'extérieur, `final` impossible, tests nécessitant de la réflexion.

**Portées** : `singleton` (défaut, une instance par contexte), `prototype` (nouvelle à chaque injection), `request`, `session`, `application`. Un singleton n'est **thread-safe que s'il est stateless** : c'est la règle d'or : pas d'état mutable dans un bean partagé.

**Cycle de vie** : instanciation → injection → `@PostConstruct` → utilisation → `@PreDestroy`.

**Auto-configuration** : Spring Boot évalue des conditions (`@ConditionalOnClass`, `@ConditionalOnMissingBean`) et configure ce qui manque. On peut exclure ce qu'on ne veut pas. `spring-boot-starter-*` regroupe les dépendances cohérentes d'un besoin.

**Configuration** : `application.yml`, profils par environnement (`application-prod.yml`), `@ConfigurationProperties` pour mapper un bloc de configuration sur un objet typé, préférable à des `@Value` éparpillés.

**Spring MVC** : `@RestController`, `@GetMapping`/`@PostMapping`, `@PathVariable` (URL), `@RequestParam` (query), `@RequestBody` (corps). Gestion d'erreurs centralisée avec `@ControllerAdvice` + `@ExceptionHandler`.

**Validation** (Bean Validation) : `@NotNull`, `@NotBlank`, `@Size`, `@Email`, `@Min`/`@Max`, `@Pattern` sur le DTO, activée par `@Valid` sur le paramètre. On peut écrire des contraintes personnalisées. La validation se fait **en entrée**, au plus tôt.

**Spring Data JPA** : les repositories sont des interfaces dont l'implémentation est générée. Les **query methods** sont dérivées du nom (`findByEmailAndActiveTrue`), et `@Query` permet du JPQL ou du SQL natif. Les **projections** (interface ou record) permettent de ne charger que certaines colonnes.

**@Transactional** ouvre une transaction, commit à la fin, rollback sur exception unchecked. Points critiques :
- Il fonctionne par **proxy AOP** : un appel interne `this.methode()` ne passe pas par le proxy, donc l'annotation est ignorée. Cause n°1 des transactions qui ne s'appliquent pas.
- Rollback par défaut sur unchecked seulement ; il faut `rollbackFor` pour les checked.
- Une exception avalée par un `catch` empêche tout rollback.
- La méthode ne doit être ni `private`, ni `final`, ni `static` : le proxy ne peut pas l'intercepter. Avant Spring 6 elle devait même être `public` ; depuis, les méthodes `protected` et package-private sont aussi prises en charge avec les proxys CGLIB.
- **Propagation** : `REQUIRED` (défaut, rejoint), `REQUIRES_NEW` (suspend et ouvre une nouvelle), `MANDATORY`, `NESTED`.
- Une transaction doit rester **courte** : jamais d'appel réseau ou de traitement long à l'intérieur, sous peine de garder une connexion et des verrous.

**AOP** : aspects, pointcuts, advices (`@Before`, `@After`, `@Around`). C'est le mécanisme derrière `@Transactional`, `@Cacheable`, `@Async`, `@Secured`, et il partage la même limite du proxy.

**Autres briques utiles** : `@Async` (exécution asynchrone, nécessite `@EnableAsync`), `@Scheduled` (tâches planifiées), abstraction de **cache** (`@Cacheable`, `@CacheEvict`), **Actuator** (endpoints de santé, métriques, info, `/actuator/health` pour Kubernetes), `RestClient` (client synchrone moderne depuis Spring 6.1), `WebClient` (réactif), `RestTemplate` (historique, déclaré déprécié dans Spring Framework 7 au profit de RestClient), et en Boot 4 les clients déclaratifs `@HttpExchange`.

**Spring Security** : une **chaîne de filtres** traverse chaque requête. Chaque filtre a un rôle (extraction du token, authentification, autorisation). Le `SecurityContext` porte l'utilisateur authentifié. Sécurité au niveau méthode avec `@PreAuthorize`. Pour une API stateless, un filtre JWT vérifie la signature et peuple le contexte.

**BeanFactory vs ApplicationContext** : `BeanFactory` est le conteneur minimal (création et injection, paresseuse) ; `ApplicationContext` l'étend avec les événements, l'internationalisation, le chargement de ressources, l'AOP, et instancie les singletons au démarrage (ce qui révèle les erreurs de configuration immédiatement). En pratique on utilise toujours un ApplicationContext.

**`@SpringBootApplication`** = `@SpringBootConfiguration` (une `@Configuration`) + `@EnableAutoConfiguration` + `@ComponentScan` (à partir du package de la classe, d'où l'importance de la placer à la racine).

**@Component vs @Bean** : `@Component` (et ses stéréotypes) se pose sur **vos** classes, détectées par le scan ; `@Bean` se pose sur une méthode d'une classe `@Configuration` et sert pour les objets dont on ne contrôle pas le code (une bibliothèque tierce) ou qui demandent une construction particulière. Dans une `@Configuration` "complète" (`proxyBeanMethods = true`, défaut), Spring proxyfie la classe pour qu'un appel entre méthodes `@Bean` renvoie le singleton plutôt qu'une nouvelle instance ; le mode *lite* (`proxyBeanMethods = false`) évite ce proxy et démarre plus vite.

**Plusieurs implémentations d'une interface** : `@Primary` (celle par défaut), `@Qualifier("nom")` (choix explicite), injection de `List<Interface>` (toutes, ordonnées par `@Order`) ou `Map<String, Interface>` (indexées par nom de bean). Sans désambiguïsation : `NoUniqueBeanDefinitionException`. Bean absent : `NoSuchBeanDefinitionException` (ou `Optional<T>` / `ObjectProvider<T>` pour une dépendance optionnelle).

**Dépendances circulaires** : A dépend de B qui dépend de A. Avec l'injection par constructeur, Spring ne peut pas les résoudre ; depuis Spring Boot 2.6 elles sont **interdites par défaut** même avec l'injection par champ. C'est un signal de conception : extraire la partie commune dans un troisième bean, ou passer par des événements. `@Lazy` sur un des points d'injection est un contournement, pas une solution.

**Injecter un prototype dans un singleton** : le prototype n'est créé qu'une fois, à l'injection. Pour obtenir une nouvelle instance à chaque usage : `ObjectProvider<T>.getObject()`, une méthode `@Lookup`, ou un proxy de portée.

**Points d'extension du cycle de vie** : `BeanFactoryPostProcessor` (modifie les **définitions** avant création, ex. résolution des placeholders), `BeanPostProcessor` (modifie les **instances** après création, ex. création des proxys AOP), interfaces `*Aware` (`ApplicationContextAware`...), `InitializingBean`/`DisposableBean` (équivalents de `@PostConstruct`/`@PreDestroy`), `SmartLifecycle` (démarrage/arrêt ordonné), `CommandLineRunner`/`ApplicationRunner` (code exécuté une fois le contexte prêt), événements `ApplicationReadyEvent`, `ContextClosedEvent`.

**Ordre de priorité de la configuration** (du plus fort au plus faible, simplifié) : arguments de ligne de commande → variables d'environnement et propriétés système → fichiers `application-{profil}.yml` → `application.yml` → valeurs par défaut. Hors du jar, un fichier `application.yml` à côté du jar l'emporte sur celui du classpath. *Relaxed binding* : `MON_SERVICE_URL` (variable d'environnement) alimente `mon-service.url`. `@Value("${cle:defaut}")` avec valeur par défaut, et SpEL avec `#{...}`. `@Profile("prod")` active un bean selon le profil ; `spring.profiles.active=prod`.

**Auto-configuration, sous le capot** : les classes listées dans `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (anciennement `spring.factories`) sont évaluées avec leurs conditions (`@ConditionalOnClass`, `@ConditionalOnMissingBean`, `@ConditionalOnProperty`, `@ConditionalOnWebApplication`). `--debug` (ou `debug=true`) affiche le **rapport des conditions** : ce qui a été configuré et pourquoi. Écrire son propre starter : une auto-configuration + des propriétés + des conditions `OnMissingBean` pour rester surchargeable.

**Serveurs embarqués** : Tomcat par défaut, Jetty en alternative (Undertow n'est plus supporté à partir de Spring Boot 4) ; Netty pour WebFlux. Le jar exécutable contient le serveur : on ne déploie plus de WAR dans un serveur d'application (sauf contrainte).

**Cycle d'une requête Spring MVC** : Filtres servlet → `DispatcherServlet` (Front Controller) → `HandlerMapping` (trouve le contrôleur) → `HandlerInterceptor.preHandle` → `HandlerAdapter` → résolution des arguments (`@PathVariable`, `@RequestBody` via les `HttpMessageConverter`, validation) → méthode du contrôleur → conversion de la valeur retournée (Jackson) → `postHandle` → `afterCompletion`. En cas d'exception : `HandlerExceptionResolver`, dont `@ControllerAdvice`.

**Filter vs Interceptor vs AOP** : le **Filter** (servlet) agit sur toutes les requêtes HTTP, avant Spring MVC (sécurité, CORS, compression, correlation ID) ; l'**Interceptor** connaît le contrôleur ciblé (mesures, contrôles par handler) ; l'**aspect AOP** s'applique aux méthodes des beans, indépendamment du web.

**Web, compléments** : `@RestController` = `@Controller` + `@ResponseBody` ; `ResponseEntity<T>` pour maîtriser statut et en-têtes ; `@ResponseStatus` ; `ProblemDetail` (RFC 9457) comme corps d'erreur standard (`spring.mvc.problemdetails.enabled=true`) ; `@RequestHeader`, `@CookieValue`, `MultipartFile` pour l'upload ; `@CrossOrigin` ou configuration CORS globale ; `@Validated` sur la classe pour la validation des paramètres simples (`@Min` sur un `@RequestParam`) et les groupes de validation.

**@Transactional, toutes les options** :
| Propagation | Comportement |
|---|---|
| `REQUIRED` (défaut) | rejoint la transaction en cours, sinon en crée une |
| `REQUIRES_NEW` | suspend la transaction en cours et en ouvre une nouvelle, indépendante |
| `SUPPORTS` | rejoint s'il y en a une, sinon s'exécute sans |
| `NOT_SUPPORTED` | suspend la transaction en cours et s'exécute sans |
| `MANDATORY` | exige une transaction existante, sinon exception |
| `NEVER` | exige qu'il n'y en ait pas, sinon exception |
| `NESTED` | point de sauvegarde (savepoint) dans la transaction en cours |

Autres attributs : `isolation`, `readOnly = true` (optimisations : pas de dirty checking en Hibernate, routage possible vers un réplica), `timeout`, `rollbackFor`, `noRollbackFor`. Piège de `REQUIRED` : si une méthode interne lève une exception attrapée par l'appelant, la transaction commune est **marquée rollback-only**, et le commit final lève `UnexpectedRollbackException`. Alternative programmatique : `TransactionTemplate`. Pour exécuter du code **après** commit (envoyer un email seulement si la donnée est bien enregistrée) : `@TransactionalEventListener(phase = AFTER_COMMIT)`.

**@Async, les pièges** : nécessite `@EnableAsync`, passe par un proxy (pas d'appel interne), doit retourner `void` ou `CompletableFuture` ; une exception dans une méthode `void` est perdue sans `AsyncUncaughtExceptionHandler` ; l'executor par défaut doit être configuré (taille, file) ; le contexte (sécurité, transaction, MDC) n'est pas propagé automatiquement.

**@Scheduled** : `fixedRate` (intervalle entre les débuts), `fixedDelay` (intervalle entre la fin d'une exécution et le début de la suivante), `cron`. Nécessite `@EnableScheduling`. Un seul thread par défaut : une tâche longue bloque les autres. En multi-instance, chaque instance exécute la tâche : ShedLock ou CronJob Kubernetes.

**Virtual threads dans Spring Boot** : `spring.threads.virtual.enabled=true` (Boot 3.2+) fait servir les requêtes Tomcat et exécuter `@Async`/`@Scheduled` sur des virtual threads.

**Spring AOT et natif** : Spring Boot 3+ peut générer au build le code d'initialisation des beans (traitement AOT) et compiler en natif avec GraalVM (`mvn -Pnative native:compile`, ou `spring-boot:build-image`). Conditions : les `@Conditional` sont évalués au build, la configuration du classpath est figée.

**DevTools** : redémarrage automatique en développement, LiveReload, valeurs par défaut adaptées au dev ; désactivé automatiquement dans un jar en production.

**Actuator, endpoints principaux** : `/actuator/health` (avec groupes `liveness` et `readiness` pour Kubernetes), `/info`, `/metrics`, `/prometheus`, `/env`, `/loggers` (changer un niveau de log à chaud), `/threaddump`, `/heapdump`, `/mappings`, `/beans`, `/conditions`, `/startup`. Par défaut seul `health` est exposé en HTTP ; exposer `env` ou `heapdump` publiquement est une faille.

**Spring Boot 4 / Spring Framework 7 en bref** (novembre 2025) : Java 17 minimum (Java 25 recommandé), Jakarta EE 11, **Jackson 3** par défaut, annotations de nullité **JSpecify** dans tout le portefeuille, **versioning d'API** natif (`spring.mvc.apiversion.*`, attribut `version` sur les mappings), **clients HTTP déclaratifs** (`@HttpExchange` sur une interface, auto-configurés), `@Retryable` et `@ConcurrencyLimit` intégrés au framework (activés par `@EnableResilientMethods`), RestTemplate déclaré déprécié au profit de `RestClient`, auto-configurations et starters découpés en modules plus fins (par exemple `spring-boot-starter-webmvc`), starter OpenTelemetry, JUnit 6 et `@MockitoBean`.

### En situation

- **"Comment optimiser un projet Spring ?"** → démarrage, données, web, cache, résilience.
- **"Ma transaction ne rollback pas"** → appel interne, exception checked, exception avalée.
- **"Comment structurer une application Spring ?"** → couches, DTOs, découpage par domaine.
- **"Le démarrage est lent"** → scan trop large, auto-configurations inutiles.
- **"Comment exposer l'état de santé du service ?"** → Actuator + probes Kubernetes.

### Questions techniques

**Q : C'est quoi l'IoC ?** R : Le conteneur crée et assemble les objets à la place du code. La DI en est la mise en œuvre.

**Q : Pourquoi l'injection par constructeur ?** R : Dépendances garanties, `final` possible, testable sans Spring, dépendances explicites.

**Q : Un bean singleton est-il thread-safe ?** R : Seulement s'il est stateless.

**Q : Cycle de vie d'un bean ?** R : Instanciation → injection → @PostConstruct → utilisation → @PreDestroy.

**Q : Pourquoi @Transactional ne marche pas sur un appel interne ?** R : L'appel `this.methode()` ne passe pas par le proxy AOP.

**Q : Rollback sur quelles exceptions ?** R : Unchecked et Error ; il faut `rollbackFor` pour les checked.

**Q : REQUIRED vs REQUIRES_NEW ?** R : Rejoindre la transaction courante vs la suspendre et en ouvrir une nouvelle.

**Q : À quoi sert @ConfigurationProperties ?** R : Mapper un bloc de configuration sur un objet typé, plutôt que des @Value dispersés.

**Q : Comment valider une entrée ?** R : Bean Validation sur le DTO (`@NotBlank`, `@Size`) + `@Valid` sur le paramètre.

**Q : À quoi sert Actuator ?** R : Exposer santé, métriques et informations, notamment pour les probes Kubernetes.

**Q : RestTemplate, RestClient ou WebClient ?** R : RestClient pour du synchrone (successeur de RestTemplate, déprécié dans Spring 7), WebClient pour du réactif ou du streaming ; en Boot 4, les interfaces déclaratives @HttpExchange.

**Q : Comment fonctionne Spring Security ?** R : Une chaîne de filtres traverse chaque requête ; le SecurityContext porte l'utilisateur authentifié.

**Q : BeanFactory vs ApplicationContext ?** R : Conteneur minimal paresseux vs conteneur complet (événements, AOP, i18n) qui instancie les singletons au démarrage.

**Q : Que contient @SpringBootApplication ?** R : @SpringBootConfiguration, @EnableAutoConfiguration et @ComponentScan.

**Q : @Component vs @Bean ?** R : Sur une classe détectée par le scan vs sur une méthode de @Configuration, pour des objets tiers ou à construction particulière.

**Q : À quoi sert proxyBeanMethods ?** R : À garantir qu'un appel entre méthodes @Bean renvoie le singleton (true) ; false évite le proxy et démarre plus vite.

**Q : Comment choisir entre deux implémentations ?** R : @Primary, @Qualifier, ou injection d'une List/Map de toutes les implémentations.

**Q : Spring accepte-t-il les dépendances circulaires ?** R : Non par défaut depuis Boot 2.6 ; c'est un défaut de conception à corriger.

**Q : Comment obtenir un nouveau prototype à chaque usage dans un singleton ?** R : ObjectProvider, @Lookup ou un proxy de portée.

**Q : BeanPostProcessor vs BeanFactoryPostProcessor ?** R : Agit sur les instances (proxys AOP) vs sur les définitions avant création.

**Q : CommandLineRunner, à quoi ça sert ?** R : Exécuter du code une fois le contexte démarré.

**Q : Qui gagne entre variable d'environnement et application.yml ?** R : La variable d'environnement.

**Q : Comment savoir pourquoi une auto-configuration ne s'est pas appliquée ?** R : Avec --debug, le rapport des conditions.

**Q : Où sont déclarées les auto-configurations ?** R : Dans META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports.

**Q : Décris le cycle d'une requête Spring MVC.** R : Filtres, DispatcherServlet, HandlerMapping, interceptors, HandlerAdapter, résolution des arguments, contrôleur, conversion de la réponse, afterCompletion.

**Q : Filter vs Interceptor ?** R : Le filtre agit au niveau servlet avant Spring MVC ; l'interceptor connaît le handler ciblé.

**Q : @Controller vs @RestController ?** R : @RestController ajoute @ResponseBody : la valeur retournée est sérialisée dans le corps.

**Q : C'est quoi ProblemDetail ?** R : La représentation Spring du format d'erreur RFC 9457.

**Q : Les propagations de transaction ?** R : REQUIRED, REQUIRES_NEW, SUPPORTS, NOT_SUPPORTED, MANDATORY, NEVER, NESTED.

**Q : REQUIRES_NEW vs NESTED ?** R : Transaction indépendante (commit séparé) vs savepoint dans la même transaction.

**Q : Pourquoi une UnexpectedRollbackException ?** R : Une méthode interne a marqué la transaction commune rollback-only, et l'appelant tente de committer.

**Q : Intérêt de readOnly = true ?** R : Hibernate désactive le dirty checking et le flush ; routage possible vers un réplica.

**Q : Comment exécuter du code seulement après le commit ?** R : @TransactionalEventListener(phase = AFTER_COMMIT).

**Q : Pièges de @Async ?** R : Proxy (pas d'appel interne), exceptions perdues en void, executor à configurer, contexte non propagé.

**Q : fixedRate vs fixedDelay ?** R : Intervalle entre les débuts vs entre la fin et le début suivant.

**Q : Comment activer les virtual threads dans Spring Boot ?** R : spring.threads.virtual.enabled=true (Boot 3.2+).

**Q : Endpoints Actuator dangereux à exposer ?** R : env, heapdump, threaddump, configprops, beans : ils révèlent secrets et internes.

**Q : Quels changements majeurs dans Spring Boot 4 ?** R : Jackson 3, JSpecify, versioning d'API, clients HTTP déclaratifs, @Retryable intégré, RestTemplate déprécié, modularisation, Jakarta EE 11.

**Q : Quel serveur embarqué par défaut ?** R : Tomcat (Netty pour WebFlux) ; Undertow n'est plus supporté en Boot 4.

### Questions de situation

**Q : "Comment tu optimises un projet Spring / Quarkus ?"** *(très fréquent)*
R : "Je commencerais par **mesurer** pour savoir quel axe compte, optimiser au hasard fait perdre du temps.

**Le démarrage** : restreindre le scan de composants aux packages utiles, exclure les auto-configurations inutiles. Structurellement, c'est une raison fréquente de passer à Quarkus (ou à Spring AOT et GraalVM natif) : Quarkus résout l'injection au build-time plutôt que par réflexion au démarrage, donc il démarre en une fraction de seconde, ce qui compte beaucoup en Kubernetes où les pods montent et descendent souvent.

**La couche données**, où se joue l'essentiel : éliminer les N+1 avec `JOIN FETCH` ou `@EntityGraph` ; dimensionner correctement le pool de connexions, trop petit il bloque, trop grand il sature la base ; utiliser des **projections** plutôt que de charger des entités complètes pour trois champs ; activer le batch pour les insertions massives.

**La couche web** : compression gzip, pagination systématique avec une taille maximale imposée côté serveur, et des DTOs ciblés plutôt que des graphes d'objets sérialisés entiers.

**Le cache** là où une donnée est lue bien plus souvent qu'écrite, avec un TTL adapté.

**La mémoire** : heap cohérent avec la limite du conteneur, GC adapté au profil de latence.

Et un axe souvent oublié : la **résilience**. Optimiser ce n'est pas que la vitesse, c'est aussi ne pas s'effondrer. Timeouts partout, circuit breaker, retry avec backoff."

**Q : "Une méthode @Transactional ne rollback pas."**
R : "Trois causes, dans l'ordre de fréquence.
D'abord l'**appel interne** : si la méthode est appelée depuis la même classe via `this.methode()`, l'appel ne traverse pas le proxy Spring et l'annotation est ignorée. C'est la cause numéro un. Correction : extraire dans un autre bean, ou s'auto-injecter le proxy.
Ensuite le **type d'exception** : par défaut le rollback ne se déclenche que sur les unchecked. Une exception checked laisse committer. Il faut préciser `rollbackFor`.
Enfin l'**exception avalée** : un `catch` qui ne relance pas fait que Spring ne voit aucune erreur et commit normalement.
Je vérifierais aussi que la méthode n'est ni `private` ni `final` : le proxy ne peut pas l'intercepter (et avant Spring 6, seules les méthodes `public` l'étaient)."

**Q : "Un endpoint renvoie une réponse énorme et lente."**
R : "Je regarderais ce qui est réellement sérialisé. Le cas typique : on retourne une entité JPA avec toutes ses relations, et la sérialisation déclenche le chargement en cascade d'un graphe entier.
Correction en trois temps : un **DTO ciblé** limité aux champs utiles ; une **projection** côté requête pour ne récupérer que ces colonnes en base ; et de la **pagination** si c'est une liste, avec une taille maximale imposée côté serveur, sinon un client peut demander 100 000 éléments.
J'activerais la compression gzip, qui coûte peu et divise souvent le poids du JSON par cinq. Et je vérifierais l'absence de **N+1 caché** derrière la sérialisation, fréquent quand Jackson déclenche le chargement de relations lazy."

**Q : "Une méthode @Transactional appelle une autre méthode qui échoue, l'exception est attrapée, et pourtant tout est annulé avec une UnexpectedRollbackException."**
R : "La méthode appelée est elle aussi transactionnelle en `REQUIRED`, donc elle partage la même transaction. Quand elle lève une exception qui traverse son proxy, Spring marque la transaction **rollback-only**. L'appelant attrape l'exception et continue, mais au moment du commit, Spring découvre le marquage et lève `UnexpectedRollbackException`.
Si l'échec de l'opération interne doit être toléré sans compromettre le reste, deux options : la passer en `REQUIRES_NEW` (elle aura sa propre transaction, son échec n'affecte pas la principale), ou ne pas la rendre transactionnelle et gérer l'erreur avant qu'elle ne traverse un proxy transactionnel. Et je documenterais le choix, parce que c'est une sémantique subtile."

**Q : "Tu dois envoyer un email de confirmation après la création d'une commande, mais seulement si la commande est bien enregistrée."**
R : "Envoyer l'email dans la méthode transactionnelle est faux dans les deux sens : si la transaction échoue après l'envoi, le client reçoit une confirmation pour une commande inexistante ; et l'appel réseau allonge la transaction.
Je publierais un événement `CommandeCreee` et je le traiterais avec `@TransactionalEventListener(phase = AFTER_COMMIT)`, éventuellement `@Async` pour ne pas retarder la réponse. Si l'envoi doit être garanti même en cas de crash juste après le commit, je passerais au **pattern Outbox** : l'événement est écrit en base dans la même transaction, et un processus séparé l'envoie de façon fiable."

**Q : "Comment tu écrirais ton propre starter Spring Boot pour une bibliothèque interne ?"**
R : "Un module d'auto-configuration avec une classe `@AutoConfiguration` déclarée dans le fichier `AutoConfiguration.imports`, des propriétés typées via `@ConfigurationProperties` (avec métadonnées pour l'autocomplétion dans l'IDE), et des beans conditionnés par `@ConditionalOnClass` (la bibliothèque est présente), `@ConditionalOnProperty` (activée) et surtout `@ConditionalOnMissingBean`, pour que l'application puisse toujours remplacer un bean par le sien.
Puis un module starter vide qui tire l'auto-configuration et les dépendances. Je le testerais avec `ApplicationContextRunner`, qui permet de vérifier chaque combinaison de conditions sans démarrer d'application complète."

═══════════════════════════════════════════
# 2. SPRING AVANCÉ : DATA, SECURITY, CLOUD, BATCH
═══════════════════════════════════════════

### Vue d'ensemble

Au-delà du cœur (IoC, MVC, transactions), l'écosystème Spring couvre presque tous les besoins d'une application d'entreprise : l'accès aux données (Spring Data), la sécurité (Spring Security), les systèmes distribués (Spring Cloud), les traitements de masse (Spring Batch). En entretien, Spring Data et Spring Security sont quasiment systématiques ; Spring Cloud apparaît dès qu'on parle microservices.

Le fil conducteur reste le même : Spring fournit des **abstractions** configurables par convention, et les problèmes surviennent quand on ignore ce qu'elles font. Une méthode de repository dérivée qui génère une requête inefficace, une configuration de sécurité qui laisse passer une URL, un `open-in-view` qui masque des N+1 : savoir ce qui se passe sous le capot reste la compétence évaluée.

### Cours

**Spring Data JPA.**
- Hiérarchie : `Repository` → `CrudRepository` → `PagingAndSortingRepository` → `JpaRepository` (ajoute `flush`, `saveAllAndFlush`, opérations batch). `ListCrudRepository` renvoie des `List` plutôt que des `Iterable`.
- **Requêtes dérivées** du nom de méthode : `findByEmail`, `findByStatutAndDateCreationAfter`, `findTop10ByOrderByScoreDesc`, `existsByEmail`, `countByStatut`, `deleteByStatut`, mots-clés `Between`, `LessThan`, `Like`, `Containing`, `In`, `IsNull`, `IgnoreCase`, `OrderBy`. Au-delà de deux ou trois critères, le nom devient illisible : passer à `@Query`.
- `@Query("select u from User u where u.email = :email")` en JPQL, ou `nativeQuery = true`.
- Écritures : `@Modifying` + `@Transactional` sur une requête `update`/`delete` ; `clearAutomatically = true` pour vider le contexte de persistance qui contiendrait sinon des entités périmées. Une requête bulk ne déclenche pas les callbacks ni les cascades JPA.
- **Pagination** : un paramètre `Pageable` (`PageRequest.of(page, taille, Sort.by("nom"))`) ; retour `Page<T>` (exécute une requête `count` en plus, coûteuse sur une grosse table) ou `Slice<T>` (sait seulement s'il y a une page suivante, sans count). Pour de gros volumes : pagination par curseur (keyset), avec `ScrollPosition`/`Window` (Spring Data 3.1+) ou une requête `where id > :dernierId`.
- **Projections** : interface (`interface UserResume { String getNom(); }`), classe/record DTO (`select new com.x.UserDto(u.nom) ...` ou détection par constructeur), projections dynamiques (`<T> List<T> findBy(..., Class<T> type)`).
- **Specifications** (`JpaSpecificationExecutor`) et **Querydsl** pour des recherches multicritères construites dynamiquement ; *Query by Example*.
- `@EntityGraph(attributePaths = {"lignes"})` sur une méthode pour charger des relations en une requête.
- **Auditing** : `@EnableJpaAuditing`, `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy` (via un `AuditorAware`).
- **`save()`** : appelle `persist` si l'entité est considérée **nouvelle** (identifiant null, ou `Persistable.isNew()`), sinon `merge`, qui commence par un SELECT. Avec des identifiants attribués manuellement, chaque `save` déclenche donc un SELECT inutile. Dans une transaction, modifier une entité managée suffit : appeler `save` n'est pas nécessaire (dirty checking).
- `deleteAll()` ou `deleteByX` chargent les entités puis les suppriment une par une ; `deleteAllInBatch()` émet une seule requête.
- **`spring.jpa.open-in-view`** vaut `true` par défaut (avec un avertissement au démarrage) : la session Hibernate reste ouverte pendant toute la requête HTTP, y compris la sérialisation. Ça évite les `LazyInitializationException` mais masque des N+1 et garde une connexion plus longtemps. Bonne pratique : le désactiver et charger explicitement ce dont on a besoin.
- **JdbcClient** (Spring 6.1) : API fluide au-dessus de JDBC (`jdbcClient.sql("...").param("id", id).query(User.class).single()`), successeur pratique de `JdbcTemplate`. **Spring Data JDBC** : un ORM simple sans lazy loading ni contexte de persistance, centré sur les agrégats DDD.
- Autres modules : Spring Data MongoDB, Redis, Elasticsearch, R2DBC, Cassandra.

**Spring Security, l'architecture.**
- Le conteneur de servlets appelle `DelegatingFilterProxy`, qui délègue à `FilterChainProxy`, qui choisit la `SecurityFilterChain` correspondant à la requête et exécute sa chaîne de filtres (CSRF, authentification, gestion des exceptions, autorisation...).
- **Authentification** : un filtre extrait les identifiants et construit un `Authentication` non authentifié, que l'`AuthenticationManager` (implémenté par `ProviderManager`) confie à un `AuthenticationProvider` (par exemple `DaoAuthenticationProvider`, qui utilise un `UserDetailsService` et un `PasswordEncoder`). En cas de succès, l'`Authentication` authentifié est placé dans le `SecurityContextHolder` (stocké par défaut dans un ThreadLocal).
- **Configuration moderne** : un bean `SecurityFilterChain` (la classe `WebSecurityConfigurerAdapter` a été supprimée en Spring Security 6) :
```java
@Bean
SecurityFilterChain api(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())                       // API stateless par jeton
        .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health/**").permitAll()
            .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
            .anyRequest().authenticated())
        .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
        .build();
}
```
- **Ordre des règles** : la première règle qui correspond s'applique ; `anyRequest()` en dernier.
- **Mots de passe** : `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (bcrypt par défaut, préfixe `{bcrypt}` permettant de migrer d'algorithme), jamais `NoOpPasswordEncoder`.
- **CSRF** : activé par défaut ; à garder pour une application à session et cookies, désactivable pour une API stateless authentifiée par jeton dans l'en-tête `Authorization` (le navigateur n'envoie pas ce jeton automatiquement).
- **CORS** : configuré via `http.cors(...)` et un `CorsConfigurationSource` ; doit être traité avant l'authentification pour que les requêtes *preflight* OPTIONS passent.
- **OAuth2 Resource Server** : l'API valide des JWT émis par un serveur d'autorisation (Keycloak, Auth0, Entra ID...). Configuration : `spring.security.oauth2.resourceserver.jwt.issuer-uri`, Spring récupère les clés publiques (JWKS) et vérifie signature, expiration et émetteur. Un `JwtAuthenticationConverter` mappe les claims vers des autorités (rôles). **OAuth2 Client** : l'application agit pour le compte d'un utilisateur (login OIDC) ou s'authentifie elle-même (client credentials) pour appeler d'autres API.
- **Sécurité au niveau méthode** : `@EnableMethodSecurity` puis `@PreAuthorize("hasRole('ADMIN')")`, `@PreAuthorize("#id == authentication.name")`, `@PostAuthorize`, `@Secured`. Même limite de proxy que `@Transactional`.
- **hasRole vs hasAuthority** : `hasRole('ADMIN')` vérifie l'autorité `ROLE_ADMIN` (préfixe ajouté automatiquement) ; `hasAuthority('SCOPE_read')` vérifie l'autorité exacte.
- **401 vs 403** : l'`AuthenticationEntryPoint` produit la réponse quand l'utilisateur n'est pas authentifié (401) ; l'`AccessDeniedHandler` quand il est authentifié mais non autorisé (403).
- **Tests** : `@WithMockUser(roles = "ADMIN")`, `SecurityMockMvcRequestPostProcessors.jwt()`.
- Le `SecurityContext` n'est pas propagé automatiquement aux threads `@Async` (utiliser `DelegatingSecurityContextExecutor`) ; en WebFlux il vit dans le contexte Reactor.

**Spring Cloud, les briques** :
| Besoin | Brique |
|---|---|
| Configuration centralisée | Spring Cloud Config (serveur adossé à Git), ou ConfigMaps/Vault |
| Découverte de services | Eureka, Consul (inutile en Kubernetes, qui fournit DNS et Services) |
| Passerelle d'API | Spring Cloud Gateway (variantes WebFlux et MVC) |
| Équilibrage côté client | Spring Cloud LoadBalancer |
| Clients HTTP déclaratifs | OpenFeign (désormais en maintenance), remplacé par les clients `@HttpExchange` de Spring |
| Résilience | Spring Cloud Circuit Breaker (Resilience4j) |
| Messagerie | Spring Cloud Stream (binders Kafka, RabbitMQ) |
| Traçage distribué | Micrometer Tracing (Spring Cloud Sleuth est abandonné) |

En Kubernetes, une partie de Spring Cloud devient redondante (découverte, configuration, équilibrage sont fournis par la plateforme), ce qui simplifie les architectures.

**Spring Batch** : traitements par lots fiables. Un `Job` est composé de `Step` ; un step *chunk-oriented* lit (`ItemReader`), transforme (`ItemProcessor`) et écrit (`ItemWriter`) par paquets, chaque paquet dans une transaction. Le `JobRepository` (tables en base) mémorise l'avancement, ce qui permet de **reprendre** un job échoué là où il s'est arrêté, de gérer *skip* (ignorer des éléments invalides) et *retry*. Paramètres de job (`JobParameters`) qui identifient une instance. Partitionnement et multi-threading pour paralléliser.

**Spring Integration** (patterns d'intégration d'entreprise : canaux, routeurs, transformateurs, adaptateurs fichiers/FTP/JMS), **Spring Modulith** (monolithe modulaire vérifié), **Spring AI** (abstractions pour les modèles de langage et les bases vectorielles), **Spring Authorization Server** (construire son propre serveur OAuth2/OIDC), **Spring gRPC** (support officiel récent).

**Tester Spring, compléments** : `@WebMvcTest(UserController.class)` avec `MockMvc` (ou `MockMvcTester` avec AssertJ, Spring 6.2), `@DataJpaTest` (transaction annulée après chaque test), `@MockitoBean` pour remplacer un bean, `@ServiceConnection` (Boot 3.1+) qui branche automatiquement un conteneur Testcontainers, `@DynamicPropertySource`, `ApplicationContextRunner` pour tester une auto-configuration, cache des contextes de test (éviter de multiplier les configurations différentes, qui forcent des redémarrages).

### En situation

- **"LazyInitializationException hors transaction"** → charger explicitement (fetch join, entity graph, DTO), pas d'open-in-view comme rustine.
- **"Une recherche multicritère dynamique"** → Specifications, Querydsl, ou Criteria API.
- **"La pagination devient lente sur les dernières pages"** → offset coûteux + count → Slice, keyset.
- **"Sécuriser une API avec Keycloak"** → OAuth2 Resource Server + JWT + mapping des rôles.
- **"Chaque save() fait un SELECT"** → identifiants attribués manuellement → Persistable/isNew.
- **"Un import nocturne de millions de lignes"** → Spring Batch par chunks avec reprise.

### Questions techniques

**Q : JpaRepository vs CrudRepository ?** R : JpaRepository ajoute pagination, tri, flush et opérations batch.

**Q : Comment fonctionnent les requêtes dérivées ?** R : Spring analyse le nom de la méthode (findBy, And, OrderBy...) et génère la requête au démarrage.

**Q : À quoi sert @Modifying ?** R : À indiquer qu'une @Query est une mise à jour ou une suppression ; avec @Transactional, et clearAutomatically pour éviter des entités périmées.

**Q : Page vs Slice ?** R : Page exécute une requête count pour connaître le total ; Slice sait seulement s'il existe une page suivante.

**Q : Pourquoi la pagination par offset ralentit-elle ?** R : La base doit lire et ignorer toutes les lignes précédentes ; la pagination par curseur l'évite.

**Q : Types de projections Spring Data ?** R : Interface, classe/record DTO, dynamique.

**Q : Que fait save() ?** R : persist si l'entité est nouvelle, sinon merge (précédé d'un SELECT).

**Q : Faut-il appeler save() après avoir modifié une entité chargée dans la transaction ?** R : Non, le dirty checking suffit.

**Q : deleteAll() vs deleteAllInBatch() ?** R : Chargement puis suppression une par une (callbacks) vs une seule requête DELETE.

**Q : Pourquoi désactiver open-in-view ?** R : Il garde la session ouverte pendant toute la requête, masque les N+1 et retient une connexion plus longtemps.

**Q : JdbcTemplate vs JdbcClient ?** R : Même rôle ; JdbcClient (6.1) offre une API fluide plus concise.

**Q : Spring Data JPA vs Spring Data JDBC ?** R : ORM complet avec contexte de persistance et lazy loading vs mapping simple d'agrégats sans magie.

**Q : Décris la chaîne de Spring Security.** R : DelegatingFilterProxy → FilterChainProxy → SecurityFilterChain et ses filtres ; l'authentification aboutit dans le SecurityContextHolder.

**Q : Rôle de l'AuthenticationManager ?** R : Déléguer l'authentification aux AuthenticationProvider configurés (via ProviderManager).

**Q : Qu'a remplacé WebSecurityConfigurerAdapter ?** R : La déclaration d'un bean SecurityFilterChain (Spring Security 6).

**Q : Quand désactiver CSRF ?** R : Pour une API stateless authentifiée par jeton en en-tête ; le garder avec des sessions et cookies.

**Q : hasRole vs hasAuthority ?** R : hasRole ajoute le préfixe ROLE_ ; hasAuthority compare l'autorité exacte.

**Q : Comment Spring valide-t-il un JWT ?** R : Il récupère les clés publiques de l'émetteur (JWKS) et vérifie signature, expiration, émetteur et audience éventuelle.

**Q : Où est stocké le SecurityContext ?** R : Dans un ThreadLocal par défaut (stratégie du SecurityContextHolder).

**Q : Comment sécuriser une méthode de service ?** R : @EnableMethodSecurity et @PreAuthorize.

**Q : 401 vs 403 en Spring Security ?** R : AuthenticationEntryPoint (non authentifié) vs AccessDeniedHandler (non autorisé).

**Q : Quelles briques Spring Cloud deviennent inutiles en Kubernetes ?** R : Découverte (Eureka), souvent la configuration centralisée et l'équilibrage côté client.

**Q : Par quoi Sleuth a-t-il été remplacé ?** R : Micrometer Tracing.

**Q : Les composants d'un step Spring Batch ?** R : ItemReader, ItemProcessor, ItemWriter, exécutés par chunks.

**Q : Comment Spring Batch permet-il la reprise ?** R : Le JobRepository persiste l'état d'exécution ; un job relancé reprend au dernier chunk validé.

**Q : À quoi sert @ServiceConnection ?** R : À brancher automatiquement un conteneur Testcontainers sur la configuration Spring Boot.

### Questions de situation

**Q : "Implémente une recherche de produits avec filtres optionnels (catégorie, prix min, prix max, texte)."**
R : "Avec des **Specifications** : une méthode par critère, qui renvoie `null` si le filtre est absent, et je les combine :
```java
Specification<Produit> spec = Specification.allOf(
    categorie(filtre.categorie()),
    prixMin(filtre.min()),
    prixMax(filtre.max()),
    texte(filtre.q()));
Page<Produit> page = repository.findAll(spec, pageable);
```
Chaque critère est testable isolément et la requête générée reste paramétrée, donc sans risque d'injection. J'imposerais une taille de page maximale, je vérifierais les index sur les colonnes filtrées, et pour une recherche textuelle sérieuse (pertinence, fautes de frappe) je passerais par un moteur dédié comme Elasticsearch/OpenSearch ou la recherche plein texte de PostgreSQL."

**Q : "Un utilisateur authentifié parvient à consulter les commandes d'un autre en changeant l'identifiant dans l'URL."**
R : "C'est un contrôle d'accès défaillant au niveau de l'objet (IDOR), la première faille de l'OWASP. L'authentification fonctionne, l'autorisation sur la **ressource** n'est pas vérifiée.
Correction immédiate : vérifier côté serveur que la commande appartient à l'utilisateur, soit dans la requête elle-même (`findByIdAndClientId(id, utilisateurCourant)`, qui renvoie 404 sinon), soit par `@PreAuthorize` avec une expression ou un bean de vérification. Renvoyer 404 plutôt que 403 évite de révéler l'existence de la ressource.
Puis je chercherais le même défaut sur tous les endpoints exposant un identifiant, et j'ajouterais des tests d'autorisation systématiques : un utilisateur A ne doit jamais pouvoir lire, modifier ni supprimer une ressource de B."
