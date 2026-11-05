---
subject: jakarta-ee
title: Jakarta EE & MicroProfile
category: frameworks
position: 50
status: draft
---

# Jakarta EE & MicroProfile : le document de référence pour les entretiens techniques

> Pour préparer un entretien : Jakarta EE, servlets et MicroProfile.
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (les ponts entre symptômes et notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ces chapitres viennent du document Java, dont ils ont été sortis pour devenir un sujet à part entière (ADR 0025).

═══════════════════════════════════════════
# 1. JAKARTA EE, SERVLETS & MICROPROFILE
═══════════════════════════════════════════

### Vue d'ensemble

Avant Spring et Quarkus, il y avait **J2EE**, devenu Java EE : un ensemble de spécifications standard pour l'entreprise (servlets, EJB, JPA, JMS...), implémentées par des serveurs d'application. En 2017, Oracle a confié Java EE à la fondation Eclipse, où il est devenu **Jakarta EE**. Faute de pouvoir utiliser la marque "Java", les packages `javax.*` ont été renommés en `jakarta.*` (Jakarta EE 9, 2020) : c'est la cause du grand chantier de migration de Spring Boot 2 vers 3.

Même si on code en Spring Boot, on utilise des spécifications Jakarta au quotidien : JPA (`jakarta.persistence`), Bean Validation (`jakarta.validation`), l'API Servlet sur laquelle repose Spring MVC, les annotations `@PostConstruct` et `@Transactional`. Quarkus, lui, est construit directement sur CDI, Jakarta REST et MicroProfile. Connaître ces standards, c'est comprendre la base commune de tout l'écosystème.

### Cours

**Historique** : J2EE (1999) → Java EE 5 (2006, annotations, JPA, simplification) → Java EE 6 et 7 (CDI, JAX-RS) → Java EE 8 (2017) → Jakarta EE 8 (2019, mêmes API sous Eclipse) → **Jakarta EE 9** (2020, `javax` → `jakarta`) → Jakarta EE 10 (2022, profil *Core* pour les microservices) → **Jakarta EE 11** (2025, Java 17 minimum, support des virtual threads dans Jakarta Concurrency, nouvelle spécification **Jakarta Data** pour des repositories à la Spring Data, base de Spring Framework 7).

**Les spécifications principales** :
| Spécification | Ancien nom | Rôle | Implémentation courante |
|---|---|---|---|
| Jakarta Servlet | Servlet API | requêtes HTTP de bas niveau | Tomcat, Jetty |
| Jakarta REST | JAX-RS | API REST annotées | RESTEasy, Jersey |
| Jakarta CDI | CDI | injection de dépendances, portées | Weld, ArC (Quarkus) |
| Jakarta Persistence | JPA | ORM | Hibernate, EclipseLink |
| Jakarta Validation | Bean Validation | contraintes déclaratives | Hibernate Validator |
| Jakarta Transactions | JTA | transactions, y compris distribuées | Narayana |
| Jakarta Messaging | JMS | messagerie | ActiveMQ Artemis |
| Jakarta JSON Binding / Processing | JSON-B / JSON-P | JSON | Yasson, Parsson |
| Jakarta Security | | authentification, autorisation | |
| Jakarta Faces | JSF | interfaces web par composants | Mojarra |
| Jakarta Enterprise Beans | EJB | composants transactionnels (legacy) | |
| Jakarta Data | | repositories (EE 11) | |

**Profils** : *Platform* (tout), *Web Profile* (le sous-ensemble web), *Core Profile* (minimal pour microservices et compilation native).

**Serveurs** : WildFly / JBoss EAP (Red Hat), Payara (issu de GlassFish), Open Liberty / WebSphere Liberty (IBM), Apache TomEE, Eclipse GlassFish. **Conteneur de servlets vs serveur d'application** : Tomcat et Jetty n'implémentent que Servlet (et quelques specs web) ; un serveur d'application implémente la plateforme complète.

**L'API Servlet** : une servlet a un cycle de vie `init()` → `service()` (qui dispatche vers `doGet`, `doPost`...) → `destroy()`. **Une seule instance** par servlet, appelée par **plusieurs threads** en parallèle : une servlet ne doit pas avoir d'état mutable en champ (exactement comme un singleton Spring). Les **filtres** (`Filter`, `doFilter` + `chain.doFilter`) entourent les requêtes ; les **listeners** réagissent aux événements (démarrage du contexte, création de session). `HttpSession` stocke un état côté serveur identifié par un cookie `JSESSIONID`, ce qui crée une affinité de session à éviter en architecture stateless. Servlet 3.0 a apporté les annotations (`@WebServlet`) et le traitement asynchrone ; Spring MVC n'est qu'une grande servlet, le `DispatcherServlet`.

**EJB, à connaître pour le legacy** : `@Stateless` (sans état, mis en pool), `@Stateful` (état conversationnel par client), `@Singleton` (une instance), `@MessageDriven` (consommateur JMS). Transactions gérées par le conteneur, sécurité déclarative, appels distants. Largement remplacés par CDI + `@Transactional`.

**JTA et XA** : JTA permet une transaction englobant plusieurs ressources (deux bases, une base et un broker JMS) grâce au protocole **XA**, une implémentation du commit à deux phases. Correct mais lent et complexe ; en microservices, on lui préfère sagas et outbox.

**Comparaison des modèles d'injection** :
| | Spring | CDI / Jakarta |
|---|---|---|
| Injection | `@Autowired` (ou constructeur implicite) | `@Inject` (aussi accepté par Spring) |
| Singleton | portée par défaut `singleton` | `@ApplicationScoped` (proxy) ou `@Singleton` |
| Par requête | `@RequestScope` | `@RequestScoped` |
| Fabrique | `@Bean` | `@Produces` |
| Choix d'implémentation | `@Qualifier`, `@Primary` | qualifiers CDI, `@Alternative` |
| Configuration | `@Value`, `@ConfigurationProperties` | `@ConfigProperty` (MicroProfile) |

**MicroProfile** : un ensemble de spécifications pensées pour les microservices, au-dessus du Core Profile : **Config**, **Health** (liveness/readiness), **Fault Tolerance** (`@Retry`, `@Timeout`, `@CircuitBreaker`, `@Bulkhead`, `@Fallback`, `@Asynchronous`), **OpenAPI**, **REST Client**, **JWT Authentication**, **Telemetry** (basé sur OpenTelemetry, qui a remplacé OpenTracing et l'ancienne spécification Metrics dans les versions récentes). Implémenté par Quarkus, Open Liberty, Payara, WildFly, Helidon.

**Autres frameworks du même créneau** : **Micronaut** (injection résolue à la compilation, comme Quarkus), **Helidon** (Oracle, Helidon SE fonctionnel et Helidon MP MicroProfile, conçu autour des virtual threads dans ses versions récentes), **Vert.x** (boîte à outils réactive), **Javalin** et **Spark** (micro-frameworks web).

### En situation

- **"Migration Spring Boot 2 vers 3 : rien ne compile"** → javax → jakarta, à traiter avec OpenRewrite.
- **"Une servlet renvoie des données d'un autre utilisateur"** → état mutable en champ d'une instance partagée.
- **"Transaction sur deux bases"** → XA/JTA possible mais coûteux ; souvent mieux de revoir le découpage.
- **"Pourquoi Quarkus utilise @Inject et pas @Autowired ?"** → il implémente le standard CDI.

### Questions techniques

**Q : Java EE vs Jakarta EE ?** R : Le même ensemble de spécifications, transféré d'Oracle à la fondation Eclipse en 2017 et renommé.

**Q : Pourquoi javax est-il devenu jakarta ?** R : Oracle a conservé la marque Java ; Jakarta EE 9 a dû renommer les packages.

**Q : Qu'apporte Jakarta EE 11 ?** R : Java 17 minimum, Jakarta Data, support des virtual threads dans Concurrency.

**Q : JAX-RS s'appelle comment aujourd'hui ?** R : Jakarta REST (implémentations RESTEasy, Jersey).

**Q : Conteneur de servlets vs serveur d'application ?** R : Tomcat/Jetty n'implémentent que la partie web ; un serveur d'application implémente toute la plateforme.

**Q : Cycle de vie d'une servlet ?** R : init, service (doGet, doPost...), destroy.

**Q : Une servlet est-elle thread-safe ?** R : Une seule instance sert toutes les requêtes en parallèle : elle ne doit pas avoir d'état mutable.

**Q : Filter vs Listener ?** R : Le filtre entoure le traitement des requêtes ; le listener réagit à des événements du cycle de vie.

**Q : Pourquoi éviter HttpSession dans une architecture scalable ?** R : L'état côté serveur impose une affinité de session ou une réplication.

**Q : Les types d'EJB ?** R : Stateless, Stateful, Singleton, Message-Driven.

**Q : C'est quoi XA ?** R : Le protocole de transaction distribuée en deux phases utilisé par JTA pour plusieurs ressources.

**Q : @Inject vs @Autowired ?** R : Standard Jakarta vs annotation Spring ; Spring accepte les deux.

**Q : C'est quoi MicroProfile ?** R : Des spécifications pour microservices : Config, Health, Fault Tolerance, OpenAPI, REST Client, JWT, Telemetry.

**Q : Quels frameworks implémentent MicroProfile ?** R : Quarkus, Open Liberty, Payara, WildFly, Helidon MP.

**Q : Micronaut vs Quarkus vs Spring ?** R : Micronaut et Quarkus résolvent l'injection à la compilation (démarrage rapide) ; Spring à l'exécution, avec l'écosystème le plus large.

**Q : javax → jakarta, c'est lié au JDK ?** R : Non, c'est un changement de Jakarta EE 9 (licence de la marque Java) ; il concerne les API EE comme Servlet, JPA, Bean Validation.

### Questions de situation

**Q : "Tu dois migrer une application Spring Boot 2.7 vers Spring Boot 3 ou 4."**
R : "Par étapes. D'abord monter en Java 17 minimum, puis passer en Boot 2.7 à jour et supprimer les avertissements de dépréciation, car beaucoup d'API dépréciées en 2.x sont supprimées en 3.
Le gros du travail est le renommage `javax` → `jakarta` dans le code et dans les dépendances : toutes les bibliothèques doivent avoir une version compatible Jakarta. **OpenRewrite** automatise une grande partie de la migration (recettes Spring Boot 3 et 4).
Ensuite les changements fonctionnels : configuration de Spring Security réécrite en `SecurityFilterChain`, Hibernate 6 (changements de génération d'identifiants et de types), propriétés renommées (le module `spring-boot-properties-migrator` les signale au démarrage), Micrometer Tracing à la place de Sleuth. Pour Boot 4 en plus : Jackson 3, modules de starters renommés, `@MockitoBean`.
Je migrerais sur une branche avec une CI complète, et je déploierais progressivement en surveillant les erreurs."

═══════════════════════════════════════════
# GLOSSAIRE DES SIGLES ET TERMES
═══════════════════════════════════════════

| Terme | Signification |
|---|---|
| **CDI** | Contexts and Dependency Injection : injection de dépendances standard Jakarta |
| **EJB** | Enterprise JavaBeans : composants Java EE historiques |
| **JMS** | Java (Jakarta) Messaging : API de messagerie standard |
| **JTA / XA** | Transactions Jakarta / protocole de transaction distribuée |
