---
subject: conception
title: Conception & qualité logicielle
category: fundamentals
position: 200
status: draft
---

# Conception & qualité logicielle : le document de référence pour les entretiens techniques

> Pour préparer un entretien : principes SOLID, design patterns, architecture logicielle, clean code et méthodes de travail.
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (les ponts entre symptômes et notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ces chapitres viennent du document Java, dont ils ont été sortis pour devenir un sujet à part entière (ADR 0025).

═══════════════════════════════════════════
# 1. PRINCIPES SOLID
═══════════════════════════════════════════

### Vue d'ensemble

**Qu'est-ce que SOLID, globalement ?**

SOLID est un acronyme regroupant cinq principes de conception orientée objet, popularisés par Robert C. Martin ("Uncle Bob") au début des années 2000 à partir de travaux des années 90. Ce ne sont ni des règles de syntaxe ni des obligations : ce sont des **heuristiques** : des lignes directrices qui aident à décider comment découper un système.

**Le problème qu'ils résolvent.** Un logiciel qui vit est un logiciel qui change. Le coût dominant n'est pas l'écriture initiale, c'est la **modification**. Un code mal structuré présente trois symptômes bien identifiés :
- **Rigidité** : un changement simple oblige à modifier beaucoup de fichiers.
- **Fragilité** : modifier un endroit casse quelque chose ailleurs, sans lien apparent.
- **Immobilité** : impossible de réutiliser un morceau, il traîne trop de dépendances avec lui.

SOLID vise précisément ces trois maux. Chaque principe attaque une cause :
- **SRP** limite l'impact d'un changement (contre la fragilité) en s'assurant qu'une classe n'a qu'une raison de changer.
- **OCP** permet d'ajouter sans modifier (contre la rigidité).
- **LSP** garantit que le polymorphisme tient ses promesses (contre la fragilité liée à l'héritage).
- **ISP** évite les dépendances inutiles (contre l'immobilité).
- **DIP** inverse le sens des dépendances pour découpler le métier de la technique (contre les trois).

**Le fil conducteur.** Si on devait résumer SOLID en une idée : *isoler ce qui change de ce qui ne change pas, et dépendre d'abstractions stables plutôt que d'implémentations volatiles.* Les cinq principes convergent tous vers ça.

**La nuance à savoir dire en entretien.** SOLID n'est pas une religion. Appliqué à l'excès, il produit une explosion d'interfaces et de classes minuscules, de la sur-ingénierie qui nuit à la lisibilité. Un évaluateur expérimenté apprécie qu'un candidat sache dire : *"j'applique SOLID là où le code est amené à évoluer, pas partout par principe."*

### Cours

**S, Single Responsibility Principle**
> Une classe ne devrait avoir qu'**une seule raison de changer**.

La formulation exacte compte : ce n'est pas "une classe ne fait qu'une chose", c'est "une classe répond aux besoins d'un seul acteur". Une classe qui gère la persistance ET le formatage d'un rapport change quand la base évolue **et** quand le métier change de format, deux raisons, deux acteurs, deux responsabilités.

```java
// Violation : 3 raisons de changer
class UserService {
    void createUser(...)      // métier
    void sendEmail(...)       // infrastructure mail
    String generateReport(...) // reporting
}
// Correction : UserService, EmailService, ReportGenerator
```
Bénéfice concret : la testabilité. Une classe à responsabilité unique se teste sans monter tout un décor.

**O, Open/Closed Principle**
> Ouvert à l'**extension**, fermé à la **modification**.

On doit pouvoir ajouter un comportement en écrivant du **nouveau** code, pas en modifiant l'existant, car modifier l'existant, c'est risquer de casser ce qui marchait. La mise en œuvre passe par le polymorphisme : une interface stable, des implémentations qui s'ajoutent.

```java
// Violation : chaque nouveau type modifie la méthode
if (type.equals("VISA")) ... else if (type.equals("PAYPAL")) ...

// Respect : une interface, une classe par type
interface PaymentStrategy { void pay(Order o); }
```
Signal d'alerte : une cascade de `if/else` ou un `switch` sur un type qu'on doit rouvrir à chaque évolution.

**L, Liskov Substitution Principle**
> Un objet d'une sous-classe doit pouvoir **remplacer** son parent sans casser le programme.

C'est le principe le plus subtil. Il dit qu'hériter ne suffit pas : il faut respecter le **contrat** du parent, ne pas renforcer les préconditions, ne pas affaiblir les postconditions, préserver les invariants.

L'exemple canonique : `Square extends Rectangle`. Mathématiquement un carré **est** un rectangle, mais si `setWidth` doit aussi modifier la hauteur pour rester carré, alors un code qui manipule un Rectangle en supposant largeur et hauteur indépendantes casse. La sous-classe n'est pas substituable.

Autres violations fréquentes : une sous-classe qui lève une exception là où le parent n'en levait pas, ou qui ne fait rien (`throw new UnsupportedOperationException()`) pour une méthode héritée qui n'a pas de sens chez elle.

**I, Interface Segregation Principle**
> Aucun client ne doit dépendre de méthodes qu'il n'utilise pas.

Une grosse interface fourre-tout force ses implémentations à fournir des méthodes qui ne les concernent pas, et couple les clients à des évolutions dont ils n'ont que faire.

```java
// Violation
interface Worker { void work(); void eat(); void sleep(); }
class Robot implements Worker { ... }   // eat() et sleep() n'ont aucun sens

// Correction
interface Workable { void work(); }
interface Feedable { void eat(); }
```

**D, Dependency Inversion Principle**
> Les modules de haut niveau ne doivent pas dépendre des modules de bas niveau ; **les deux doivent dépendre d'abstractions**.

C'est le principe le plus structurant. Sans lui, la logique métier (haut niveau, stable, précieuse) dépend de la technique (bas niveau, volatile) : le métier connaît MySQL, connaît le format du fichier, connaît le client HTTP. Changer un détail technique impose alors de toucher au métier.

L'inversion consiste à faire **définir l'interface par le module de haut niveau**, et à l'implémenter par le bas niveau. Le sens de la dépendance s'inverse : ce n'est plus le métier qui dépend de la base, c'est l'adaptateur base qui dépend du contrat défini par le métier.

```java
// Violation : le métier connaît l'implémentation
class OrderService { private MySQLRepository repo = new MySQLRepository(); }

// Respect : le métier définit et reçoit une abstraction
class OrderService {
    private final OrderRepository repo;          // interface
    OrderService(OrderRepository repo) { ... }   // injectée
}
```
C'est le fondement de l'**injection de dépendances** et de l'**architecture hexagonale**.

**Au-delà de SOLID**, quelques principes qui reviennent en entretien :
- **DRY** (Don't Repeat Yourself) : une connaissance a une seule représentation. Attention : ça concerne la connaissance, pas le code qui se ressemble par hasard.
- **KISS** : la solution la plus simple qui marche.
- **YAGNI** : n'implémente pas ce dont tu n'as pas besoin maintenant.
- **Law of Demeter** : ne parle qu'à tes voisins directs, éviter `a.getB().getC().getD()`, qui couple à toute une chaîne.

**Autres principes souvent cités** :
- **Composition over inheritance** (Gang of Four) : préférer assembler des comportements plutôt qu'en hériter.
- **Separation of Concerns** : chaque partie du code traite une préoccupation distincte (présentation, métier, persistance).
- **Principe de moindre surprise** : une méthode fait ce que son nom annonce, sans effet de bord caché.
- **Command-Query Separation** : une méthode soit modifie l'état (commande), soit renvoie une information (requête), pas les deux.
- **Fail fast** et **programmation défensive** aux frontières (entrées externes), confiance à l'intérieur du domaine.
- **GRASP** (Larman) : Information Expert (la responsabilité va à la classe qui possède l'information), Creator, Controller, Low Coupling, High Cohesion, Polymorphism, Pure Fabrication, Indirection, Protected Variations.
- **Principes de packages** (Martin) : *Acyclic Dependencies* (pas de cycles entre packages), *Stable Dependencies* (dépendre vers ce qui est stable), *Common Closure* (ce qui change ensemble est regroupé).

**SOLID et Spring** : l'injection de dépendances est le DIP outillé ; l'injection d'une `List<Strategy>` ou `Map<String, Strategy>` rend l'OCP naturel ; les interfaces fines (ISP) facilitent les mocks ; un bean qui reçoit 10 dépendances par constructeur signale une violation du SRP.

**Exemple LSP du JDK** : `List.of()` renvoie une List dont `add` lève UnsupportedOperationException. Tout code qui suppose une liste modifiable casse : c'est une entorse assumée au LSP, compensée par la documentation.

### En situation

- **"Cette classe fait 800 lignes"** → SRP.
- **"Ajouter un type oblige à modifier 5 fichiers"** → OCP.
- **"Ce code est intestable"** → DIP manquant.
- **"Cette sous-classe se comporte bizarrement"** → LSP.
- **"Comment structurer un projet proprement ?"** → SOLID comme grille de décision.

### Questions techniques

**Q : Qu'est-ce que SOLID, globalement ?** R : Cinq heuristiques de conception OO visant à rendre le code modifiable sans le casser. Elles combattent la rigidité, la fragilité et l'immobilité, en isolant ce qui change et en dépendant d'abstractions.

**Q : Explique le SRP.** R : Une classe = une seule raison de changer, donc les besoins d'un seul acteur.

**Q : Explique l'OCP.** R : Ajouter un comportement par du nouveau code, sans modifier l'existant. Mise en œuvre par le polymorphisme.

**Q : Explique le LSP.** R : Une sous-classe doit pouvoir remplacer son parent sans casser le programme, en respectant son contrat.

**Q : Exemple de violation du LSP.** R : Square héritant de Rectangle : setWidth modifie la hauteur, ce qui casse un code supposant les dimensions indépendantes.

**Q : Explique l'ISP.** R : Aucun client ne dépend de méthodes qu'il n'utilise pas ; préférer plusieurs interfaces fines.

**Q : Explique le DIP.** R : Haut et bas niveau dépendent d'abstractions ; l'interface est définie par le haut niveau et implémentée par le bas.

**Q : Lien DIP / injection de dépendances ?** R : La DI est la mise en œuvre du DIP.

**Q : DRY, KISS, YAGNI ?** R : Une connaissance = une représentation / la solution la plus simple / n'implémente pas ce qui n'est pas nécessaire.

**Q : SOLID s'applique-t-il seulement aux classes ?** R : Non, aussi aux modules et à l'architecture, découper un monolithe par domaine, c'est du SRP architectural.

**Q : C'est quoi la Command-Query Separation ?** R : Une méthode modifie l'état ou renvoie une valeur, pas les deux.

**Q : Que dit le principe des dépendances acycliques ?** R : Le graphe de dépendances entre packages ou modules ne doit pas contenir de cycle.

**Q : C'est quoi l'Information Expert de GRASP ?** R : Attribuer une responsabilité à la classe qui détient l'information nécessaire.

**Q : Un exemple de violation du LSP dans le JDK ?** R : Les listes immuables (List.of) dont add lève UnsupportedOperationException.

**Q : Quel signal indique une violation du SRP dans un bean Spring ?** R : Un constructeur qui reçoit de très nombreuses dépendances.

**Q : Comment Spring facilite-t-il l'OCP ?** R : En injectant automatiquement toutes les implémentations d'une interface (List ou Map de beans).

**Q : Que privilégier : forte cohésion ou faible couplage ?** R : Les deux à la fois ; ils vont ensemble.

### Questions de situation

**Q : "Une classe de 800 lignes gère la création d'utilisateurs, l'envoi d'emails et la génération de rapports."**
R : "Violation nette du SRP : trois responsabilités répondant à trois acteurs différents. Concrètement, modifier la logique d'email risque de casser la création d'utilisateur, et la classe est intestable unitairement, pour tester la création il faut aussi mocker le mail et le reporting.
Je découperais en trois classes, `UserService` dépendant d'`EmailService` par injection d'interface.
Mais pas d'un coup en production : j'écrirais d'abord des **tests de caractérisation** capturant le comportement actuel, puis j'extrairais une responsabilité à la fois en vérifiant que les tests passent. Un refactoring sans filet sur du code métier, c'est comme ça qu'on casse la prod."

**Q : "Chaque nouveau mode de paiement oblige à modifier une méthode pleine de if."**
R : "Violation de l'Open/Closed : le code n'est pas fermé à la modification, donc chaque ajout risque de casser les modes existants.
La solution est le pattern **Strategy** : une interface `PaymentStrategy`, une implémentation par mode. Le code appelant reçoit la bonne stratégie et l'exécute sans savoir laquelle.
Pour la sélection, une Factory, ou en Spring/Quarkus l'injection d'une `Map<String, PaymentStrategy>` où le framework injecte automatiquement toutes les implémentations. Ajouter un mode devient : créer une classe. Zéro modification de l'existant.
Bénéfice secondaire : chaque stratégie devient testable isolément, alors qu'avant il fallait exercer toute la cascade."

**Q : "Une classe instancie son repository avec `new`. Pourquoi c'est un problème ?"**
R : "Deux problèmes. **Intestable** : impossible de substituer un mock, donc tout test devient un test d'intégration nécessitant une vraie base. Et **rigide** : changer d'implémentation ou ajouter du cache oblige à modifier la classe métier, alors que ça ne la concerne pas.
La correction, c'est le DIP : la classe déclare une dépendance à une **interface**, reçue par **constructeur**, et le framework injecte l'implémentation.
Résultat : le métier ignore la techno de persistance, on injecte un mock en test, et changer d'implémentation ne touche pas le métier. J'utilise le constructeur plutôt qu'un champ, parce qu'il garantit que la dépendance existe à la création et permet de la déclarer `final`."

**Q : "Est-ce qu'il faut appliquer SOLID partout ?"**
R : "Non, et c'est important de le dire. Appliqué mécaniquement, SOLID produit une prolifération d'interfaces à implémentation unique et de classes minuscules, de la sur-ingénierie qui rend le code plus dur à suivre, pas plus facile.
J'applique ces principes là où le code est **amené à évoluer** : les points de variation identifiés, les frontières entre métier et technique. Sur du code stable et simple, une classe directe reste préférable.
Le vrai critère c'est YAGNI : je n'introduis une abstraction que quand j'ai au moins deux implémentations réelles ou une raison claire d'anticiper. Une interface créée 'au cas où' est une dette, pas un investissement."

**Q : "Un collègue dit que SOLID, c'est de la théorie inutile. Que lui réponds-tu ?"**
R : "Que je comprends la réaction quand SOLID est appliqué de façon dogmatique, avec des interfaces partout. Mais chaque principe répond à une douleur concrète qu'il a probablement déjà vécue : une classe qu'on n'ose plus modifier (SRP), un `if/else` qu'il faut rouvrir à chaque nouveau cas (OCP), du code impossible à tester sans base de données (DIP).
Je lui proposerais de les utiliser comme des **questions** en revue de code plutôt que comme des règles : 'combien de raisons cette classe a-t-elle de changer ?', 'peut-on ajouter ce cas sans toucher l'existant ?'. Ça ancre la discussion dans le code réel."

═══════════════════════════════════════════
# 2. DESIGN PATTERNS
═══════════════════════════════════════════

### Vue d'ensemble

Les design patterns sont des **solutions éprouvées à des problèmes de conception récurrents**, catalogués en 1994 par le "Gang of Four". Leur valeur est double : ils évitent de réinventer une solution, et surtout ils fournissent un **vocabulaire partagé** : dire "on met un Adapter ici" transmet en trois mots ce qui demanderait un paragraphe.

Ils se répartissent en trois familles selon le problème traité : **créationnels** (comment créer des objets sans se coupler aux classes concrètes), **structuraux** (comment composer des objets pour former des structures plus grandes), **comportementaux** (comment répartir les responsabilités et organiser les interactions).

Un point de maturité à savoir exprimer : les patterns ne sont pas un but. Les appliquer sans nécessité produit de la complexité gratuite. Et certains ont perdu de leur pertinence, le Singleton est largement remplacé par la portée des beans dans un conteneur d'injection.

### Cours

**Créationnels**

**Singleton** : une seule instance, accès global. En Java moderne, l'implémentation propre est un `enum` (thread-safe et protégé contre la sérialisation). En pratique, avec Spring ou Quarkus on ne l'écrit presque jamais : les beans sont déjà des singletons gérés, et un singleton codé à la main est difficile à tester (état global, pas d'injection).

**Factory Method** : déléguer la création à une méthode, quand le type dépend du contexte. Le client demande "un paiement Visa" sans connaître `VisaPayment`. Bénéfice : découplage et centralisation de la logique de création.

**Abstract Factory** : créer des **familles** d'objets cohérents entre eux (par exemple tous les composants d'un thème d'interface).

**Builder** : construire pas à pas un objet complexe, surtout quand beaucoup d'attributs sont optionnels. Il élimine le "constructeur télescopique" et rend l'appel auto-documenté. Il permet aussi de valider la cohérence globale dans le `build()`.

**Structuraux**

**Adapter** : convertir une interface en une autre attendue par le client. C'est le traducteur entre deux mondes incompatibles. En DDD, c'est aussi la base de l'*anti-corruption layer* qui empêche un modèle externe de contaminer le domaine.

**Decorator** : ajouter des responsabilités en **enveloppant** un objet, sans modifier sa classe ni son interface. Contrairement à l'héritage, ça se fait à l'exécution et se compose : `new Cached(new Logged(new RealService()))`.

**Proxy** : contrôler l'accès à un objet, lazy loading, cache, sécurité, journalisation, appel distant. Même interface que l'objet réel. C'est le mécanisme derrière `@Transactional` : Spring interpose un proxy qui ouvre la transaction avant d'appeler la vraie méthode. D'où le fait qu'un appel interne `this.methode()` ne déclenche rien : il ne passe pas par le proxy.

**Facade** : offrir une interface simple à un sous-système complexe. Réduit le couplage entre le client et les détails internes.

**Composite** : traiter uniformément un objet simple et un groupe d'objets (une arborescence de fichiers et de dossiers).

**Comportementaux**

**Strategy** : encapsuler des algorithmes interchangeables derrière une interface commune et les sélectionner à l'exécution. C'est l'outil de référence pour respecter l'Open/Closed.

**Observer** : notifier automatiquement des abonnés lors d'un changement d'état. Base des systèmes d'événements. Dans un contexte distribué, l'équivalent est le publish/subscribe via un broker.

**Template Method** : définir le squelette d'un algorithme dans une méthode (généralement `final`), en laissant des étapes aux sous-classes. Utile pour des traitements dont la structure est fixe mais les détails varient.

**Chain of Responsibility** : faire passer une requête par une chaîne de traitants, chacun pouvant la traiter ou la passer au suivant. C'est exactement le modèle des filtres de servlet et de Spring Security.

**Command** : encapsuler une requête dans un objet, ce qui permet de la mettre en file, de la journaliser, de l'annuler.

**State** : changer le comportement d'un objet selon son état interne. Structurellement proche de Strategy, mais l'intention diffère : Strategy choisit un algorithme, State reflète une transition.

**Les autres patterns du Gang of Four** (23 au total) :
- **Prototype** (créationnel) : créer un objet en copiant un modèle existant, utile quand la construction est coûteuse.
- **Bridge** (structurel) : séparer une abstraction de son implémentation pour les faire varier indépendamment (JDBC : l'API d'un côté, les drivers de l'autre).
- **Flyweight** (structurel) : partager des objets immuables pour économiser la mémoire (le cache d'Integer, le String Pool).
- **Iterator** (comportemental) : parcourir une collection sans exposer sa structure (`Iterator`, `for-each`).
- **Mediator** : centraliser les interactions entre objets pour éviter qu'ils se connaissent tous.
- **Memento** : capturer et restaurer l'état d'un objet (annuler/rétablir).
- **Visitor** : ajouter des opérations à une hiérarchie sans la modifier, par double dispatch. En Java moderne, une hiérarchie `sealed` et un `switch` exhaustif à patterns le remplacent souvent avec bien moins de code.
- **Interpreter** : représenter une grammaire et l'évaluer (expressions SpEL, moteurs de règles).

**Singleton thread-safe, les implémentations** : initialisation **eager** (`private static final Instance I = new Instance();`, simple et sûre si la création est peu coûteuse), idiome **holder** (paresseux et sans synchronisation), **double-checked locking** avec `volatile`, et **enum** (la plus robuste : thread-safe, résistante à la sérialisation et à la réflexion). Le singleton par méthode `synchronized` est correct mais paie un verrou à chaque appel.

**Patterns hors GoF fréquents en entretien** :
- **DAO vs Repository** : le DAO est orienté table et opérations techniques (insert, update) ; le Repository est orienté domaine et collection d'agrégats (Spring Data utilise le terme Repository).
- **DTO** : objet de transfert sans logique, qui découple le contrat d'API du modèle interne.
- **Service Layer**, **Unit of Work** (le contexte de persistance JPA en est un), **Identity Map** (le cache de premier niveau), **Lazy Load** (les proxys Hibernate).
- **Specification** : encapsuler une règle de sélection combinable (`and`, `or`), utilisé par Spring Data JPA.
- **Null Object** : un objet qui ne fait rien plutôt qu'un null (une liste vide, un `NoOpLogger`).
- **Front Controller** : un point d'entrée unique pour toutes les requêtes (le `DispatcherServlet` de Spring MVC).
- **Dependency Injection** et son anti-pattern opposé, le **Service Locator** (aller chercher ses dépendances dans un registre global cache les dépendances et complique les tests).

**Patterns dans le JDK** :
| Pattern | Exemple dans le JDK |
|---|---|
| Builder | `StringBuilder`, `HttpRequest.newBuilder()`, `Stream.builder()` |
| Factory method | `List.of()`, `Integer.valueOf()`, `Calendar.getInstance()` |
| Singleton | `Runtime.getRuntime()` |
| Adapter | `Arrays.asList()`, `InputStreamReader` |
| Decorator | `BufferedInputStream`, `Collections.unmodifiableList()` |
| Proxy | `java.lang.reflect.Proxy` |
| Flyweight | cache d'`Integer`, String Pool |
| Iterator | `Iterator` |
| Strategy | `Comparator` |
| Template method | `AbstractList`, `HttpServlet.service()` |
| Observer | listeners, `PropertyChangeListener` (Observable est déprécié) |
| Command | `Runnable`, `Callable` |
| Chain of responsibility | `FilterChain` des servlets |

**Patterns dans Spring** : Factory (`BeanFactory`), Singleton (portée par défaut), Proxy (AOP, transactions), Template Method (`JdbcTemplate`, `RestTemplate`, `TransactionTemplate`), Observer (`ApplicationEvent` et `@EventListener`), Strategy (les implémentations injectées), Adapter (`HandlerAdapter`), Front Controller (`DispatcherServlet`), Chain of Responsibility (`SecurityFilterChain`).

**Anti-patterns à savoir nommer** : *God Object* (une classe qui fait tout), *Spaghetti code*, *Golden Hammer* (appliquer la même solution à tout), *Copy-paste programming*, *Lava flow* (code mort que personne n'ose supprimer), *Singletonite* (état global partout), *Magic numbers*, *Primitive obsession*, *Distributed monolith* (des microservices tellement couplés qu'il faut les déployer ensemble).

### En situation

- **"Rendre ce comportement configurable"** → Strategy.
- **"Intégrer une API au format incompatible"** → Adapter.
- **"Ajouter logging/cache sans toucher le métier"** → Decorator ou Proxy (AOP).
- **"Ce constructeur a 12 paramètres"** → Builder.
- **"Notifier plusieurs composants d'un changement"** → Observer / événements.
- **"Enchaîner des traitements de filtrage"** → Chain of Responsibility.

### Questions techniques

**Q : Les 3 familles ?** R : Créationnels, structuraux, comportementaux.

**Q : Pourquoi une Factory plutôt qu'un `new` ?** R : Pour découpler le client des classes concrètes et choisir le type à l'exécution.

**Q : Factory vs Builder ?** R : Factory = quel objet créer ; Builder = comment construire un objet complexe.

**Q : Decorator vs héritage ?** R : Le Decorator ajoute à l'exécution et se compose ; l'héritage fige à la compilation.

**Q : Où utilise-t-on le Proxy en Spring ?** R : `@Transactional`, `@Cacheable`, `@Async`, d'où le fait qu'un appel interne ne les déclenche pas.

**Q : Strategy vs Template Method ?** R : Composition (algo injecté) vs héritage (étapes redéfinies).

**Q : Strategy vs State ?** R : Structure proche, intention différente : choisir un algorithme vs refléter un état interne.

**Q : Où voit-on Chain of Responsibility ?** R : Les filtres HTTP et la chaîne de filtres de Spring Security.

**Q : Le Singleton est-il encore pertinent ?** R : Rarement à la main : les conteneurs d'injection gèrent déjà des instances uniques, et un singleton codé est difficile à tester.

**Q : Combien de patterns dans le catalogue GoF ?** R : 23, répartis en créationnels, structuraux et comportementaux.

**Q : Quelle est la meilleure implémentation d'un singleton en Java ?** R : L'enum (thread-safe, sérialisation et réflexion maîtrisées), ou l'idiome holder pour l'initialisation paresseuse.

**Q : DAO vs Repository ?** R : Orienté table et opérations techniques vs orienté domaine et collection d'agrégats.

**Q : C'est quoi le pattern Flyweight ? Un exemple ?** R : Partager des objets immuables pour économiser la mémoire ; le cache d'Integer ou le String Pool.

**Q : C'est quoi le pattern Bridge ? Un exemple ?** R : Découpler une abstraction de son implémentation ; JDBC et ses drivers.

**Q : Par quoi peut-on remplacer un Visitor en Java moderne ?** R : Une hiérarchie sealed et un switch exhaustif avec pattern matching.

**Q : Quel pattern est le DispatcherServlet ?** R : Front Controller.

**Q : Quel pattern est JdbcTemplate ?** R : Template Method : le squelette (connexion, gestion d'erreurs, fermeture) est fixe, la requête et le mapping varient.

**Q : Pourquoi le Service Locator est-il un anti-pattern ?** R : Il cache les dépendances d'une classe et complique les tests, contrairement à l'injection.

**Q : C'est quoi un Null Object ?** R : Un objet neutre qui remplace null pour éviter les vérifications (liste vide, logger qui ne fait rien).

**Q : Quel pattern illustre Comparator ?** R : Strategy.

**Q : Quel pattern illustre BufferedInputStream ?** R : Decorator.

**Q : C'est quoi un monolithe distribué ?** R : Des services séparés mais si couplés qu'ils doivent évoluer et se déployer ensemble : les inconvénients des deux mondes.

### Questions de situation

**Q : "Intégrer une API externe dont le format ne correspond pas à ton modèle."**
R : "Un **Adapter**, isolé dans la couche infrastructure. Mon domaine définit une interface exprimant son besoin, dans son propre vocabulaire, et l'adapter l'implémente en traduisant vers le format externe.
L'intérêt majeur : le format externe ne contamine jamais le métier. Si le fournisseur change son API ou si on change de fournisseur, seul l'adapter bouge. C'est un anti-corruption layer au sens DDD.
Exemple typique : un Adapter qui convertit des messages Protobuf (ou le format XML d'un partenaire) en objets métier, ce qui garde la logique indépendante du protocole de transport."

**Q : "Ajouter du logging et de la mesure de temps sans polluer le code métier."**
R : "Avec Spring ou Quarkus, l'**AOP** : un aspect `@Around` déclenché par une annotation custom. Le framework crée un proxy qui intercale le comportement autour de l'appel. Le métier reste propre, et c'est exactement le mécanisme de `@Transactional`.
Sans AOP, le **Decorator** fait la même chose manuellement : une classe implémentant la même interface, qui ajoute le comportement et délègue.
Je préciserais un point d'attention : sur un chemin critique appelé très fréquemment, l'instrumentation a un coût. Je mesurerais l'impact plutôt que de supposer qu'il est nul."

**Q : "Un objet de configuration a 12 champs dont 9 optionnels."**
R : "Un **Builder**. Sans lui, on tombe soit dans le constructeur télescopique (une explosion de constructeurs par combinaison) soit dans un constructeur à 12 paramètres où personne ne sait ce que signifie le cinquième booléen.
Avec un builder, l'appelant ne renseigne que ce qu'il veut et le code s'auto-documente par les noms de méthodes. On peut aussi **valider la cohérence globale** dans le `build()` : champs obligatoires présents, combinaisons valides, ce qui garantit qu'on ne produit jamais un objet à moitié construit.
Lombok avec `@Builder` génère tout ça. Si l'objet est simple et immutable, un record peut suffire ; dès qu'il y a beaucoup d'optionnels, le builder reste plus lisible."

**Q : "Le code de calcul des frais de livraison contient un énorme switch sur le pays et le transporteur."**
R : "J'extrairais chaque règle dans une **Strategy** derrière une interface `CalculFrais`, avec une méthode `supports(pays, transporteur)` et une méthode `calculer(commande)`. Un sélecteur parcourt les stratégies injectées par le conteneur et prend la première qui s'applique, éventuellement ordonnée avec `@Order`.
Si certaines règles se combinent (remise, majoration), une **Chain of Responsibility** ou une liste de **Decorators** est plus adaptée qu'une stratégie unique. Et si les règles sont surtout des **données** (des grilles tarifaires qui changent chaque mois), je ne les coderais pas du tout : une table de configuration ou un moteur de règles évite un déploiement à chaque changement de tarif."

═══════════════════════════════════════════
# 3. ARCHITECTURE LOGICIELLE
═══════════════════════════════════════════

### Vue d'ensemble

L'architecture, c'est l'ensemble des décisions **structurantes et coûteuses à changer** : le découpage en composants, la façon dont ils communiquent, où vivent les données, comment on déploie. Elle ne traite pas de la qualité du code ligne à ligne, mais de la capacité du système à **évoluer, tenir la charge et rester compréhensible** à mesure qu'il grandit et que l'équipe grossit.

Le principe fondateur est toujours le même : **séparer les préoccupations** et **contrôler le sens des dépendances**. Ce qui est stable et précieux (le métier) ne doit pas dépendre de ce qui est volatile (la technique).

Une architecture n'est jamais "bonne" dans l'absolu : elle est adaptée à un contexte, taille de l'équipe, contraintes de charge, criticité, maturité opérationnelle. Un évaluateur cherche à savoir si tu comprends les **compromis**, pas si tu connais le nom du dernier pattern à la mode. La réponse "ça dépend, voici de quoi" vaut mieux que "les microservices, évidemment".

### Cours

## 18.1. Les styles architecturaux

**Architecture en couches (layered)** : le classique : présentation → métier → accès aux données. Chaque couche ne parle qu'à celle du dessous. Simple, universellement comprise. Limite : les dépendances pointent vers la base de données, donc le métier finit par en dépendre ; et une évolution fonctionnelle traverse toutes les couches.

**MVC** : Model (données et logique), View (affichage), Controller (orchestration). Historiquement pour les interfaces ; côté serveur, le contrôleur REST en est l'héritier.

**Architecture hexagonale (Ports & Adapters)** : le domaine au centre, l'infrastructure en périphérie. Le domaine définit des **ports** (interfaces exprimant ses besoins et ses services), l'infrastructure fournit des **adapters** (REST, JPA, client HTTP, messaging). Les dépendances pointent **vers l'intérieur** : le métier ne connaît rien de la technique.

On distingue les adapters **primaires/driving** (qui pilotent l'application : contrôleur REST, consommateur de messages, CLI) et **secondaires/driven** (que l'application pilote : base, API externe).

Bénéfices : le métier est testable sans infrastructure, on peut changer de techno sans le toucher, et les frontières sont explicites. Coût : plus de fichiers et d'indirections, justifié sur un système durable, excessif sur un CRUD simple.

**Clean Architecture / Onion** : même idée avec plus de cercles concentriques (entités, cas d'usage, adaptateurs, frameworks) et la même règle : les dépendances vont vers le centre.

**Event-Driven** : les composants communiquent par événements plutôt que par appels directs. Fort découplage, bonne absorption des pics, mais flux global plus difficile à suivre et cohérence différée.

**Microservices** : voir 18.3.

**Serverless / FaaS** : du code exécuté à la demande, sans gérer de serveur. Excellente élasticité et facturation à l'usage, mais démarrages à froid et contraintes d'exécution.

## 18.2. Attributs de qualité et compromis

Une architecture se juge sur des **attributs de qualité** (les "-ilités") qui s'opposent souvent :

| Attribut | Question |
|---|---|
| Performance | Temps de réponse, débit |
| Scalabilité | Absorber plus de charge |
| Disponibilité | Résister aux pannes |
| Maintenabilité | Coût du changement |
| Testabilité | Facilité à vérifier |
| Sécurité | Résistance aux attaques |
| Observabilité | Capacité à diagnostiquer |

Les compromis classiques : la cohérence forte s'oppose à la disponibilité (CAP) ; le découplage augmente la complexité opérationnelle ; le cache améliore la latence mais introduit un risque de données périmées ; la redondance améliore la disponibilité mais coûte cher.

**Théorème CAP** : dans un système distribué, on ne peut garantir simultanément Cohérence, Disponibilité et tolérance au Partitionnement. Le partitionnement réseau étant inévitable, le vrai choix est entre C et A **pendant** une partition.

## 18.3. Monolithe vs Microservices

**Monolithe** : une application déployée d'un bloc. Avantages réels et souvent sous-estimés : simplicité de développement et de déploiement, transactions ACID naturelles, appels en mémoire sans latence, debug direct. Limites à l'échelle : déploiements risqués et couplés, équipes qui se bloquent, scaling global uniquement, base de code difficile à appréhender.

**Monolithe modulaire** : le compromis souvent le plus sage. Des modules à frontières explicites dans un même déploiement. On obtient l'essentiel des bénéfices d'organisation sans la complexité distribuée, et on prépare une extraction éventuelle.

**Microservices** : services indépendants, chacun avec son déploiement et idéalement sa base. Avantages : scaling ciblé, déploiements indépendants, autonomie des équipes, isolation des pannes, liberté technologique. Coûts réels : latence réseau, transactions distribuées, observabilité indispensable, cohérence différée, complexité opérationnelle qui exige une vraie maturité DevOps.

**Quand extraire ?** Les bonnes raisons sont organisationnelles ou de charge : des équipes qui se bloquent mutuellement, des besoins de scaling très différents, un besoin d'isolation. Les mauvaises : "c'est moderne", ou espérer qu'un découpage réglera une dette technique, un monolithe mal structuré donne des microservices mal structurés, avec le réseau en plus.

**Comment extraire ?** Le pattern **Strangler Fig** : on intercepte progressivement le trafic vers le nouveau service, fonctionnalité par fonctionnalité, jusqu'à ce que l'ancien soit vidé de sa substance. Jamais de réécriture big bang.

**Sur quel critère découper ?** Par **bounded context** métier, pas par couche technique. Un service "pricing" complet vaut mieux qu'un service "base de données" et un service "logique".

## 18.4. Domain-Driven Design

Le DDD propose de structurer le logiciel autour du **métier** plutôt que de la technique, avec un **ubiquitous language** : un vocabulaire unique partagé entre développeurs et experts métier, présent jusque dans le code.

**Concepts tactiques :**
- **Entity** : identité propre et durable (un User reste le même si son nom change).
- **Value Object** : défini uniquement par ses attributs, immutable (Money, Address).
- **Aggregate** : groupe d'objets formant une unité de cohérence, accessible uniquement par son **Aggregate Root**, qui garantit les invariants. Frontière naturelle d'une transaction.
- **Repository** : abstraction de persistance, exprimée dans le langage du domaine.
- **Domain Service** : logique métier qui n'appartient naturellement à aucune entité.
- **Domain Event** : fait métier significatif survenu (`OrderPlaced`).

**Concepts stratégiques :**
- **Bounded Context** : frontière dans laquelle un modèle est cohérent. Un "User" en Authentification n'est pas celui de la Facturation, et c'est très bien.
- **Context Map** : comment les contextes se relient (noyau partagé, client/fournisseur, **anti-corruption layer** pour se protéger d'un modèle externe).

## 18.5. Patterns distribués

- **API Gateway** : point d'entrée unique (routage, authentification, limitation de débit, agrégation).
- **BFF (Backend For Frontend)** : une façade par type de client (web, mobile), qui adapte les données à ses besoins.
- **Service Mesh** : proxies sidecar gérant la communication (mTLS, retry, circuit breaker) sans code applicatif.
- **Service Discovery** : les services se trouvent dynamiquement (en Kubernetes, c'est le DNS interne).
- **Circuit Breaker** : couper les appels vers un service défaillant. Trois états : fermé (normal), ouvert (coupé), semi-ouvert (test de reprise).
- **Bulkhead** : cloisonner les ressources pour qu'une dépendance défaillante n'épuise pas tout le pool.
- **Saga** : transaction distribuée en étapes locales avec compensations. Deux variantes : chorégraphie (chacun réagit aux événements, simple mais flux diffus) et orchestration (un chef d'orchestre pilote, plus traçable).
- **CQRS** : séparer le modèle d'écriture du modèle de lecture, pour les optimiser indépendamment.
- **Event Sourcing** : stocker la suite des événements plutôt que l'état final ; l'état se reconstruit en les rejouant. Historique complet et auditable, mais complexité forte et rejeu à gérer.
- **Outbox pattern** : pour publier un événement de façon fiable, on l'écrit dans une table dans la même transaction que la donnée métier, et un processus le publie ensuite, évite le cas "commit en base mais message perdu".
- **Idempotence** : un consommateur doit pouvoir traiter deux fois le même message sans effet double. Indispensable en messaging.

## 18.6. The Twelve-Factor App

Douze principes (Heroku, 2011) pour des applications cloud-native :
1. **Codebase** : un dépôt versionné, plusieurs déploiements.
2. **Dependencies** : déclarées explicitement (pom.xml), jamais supposées présentes sur la machine.
3. **Config** : dans l'environnement (variables), jamais dans le code.
4. **Backing services** : base, cache, broker sont des ressources attachées, interchangeables par configuration.
5. **Build, release, run** : étapes strictement séparées ; une release = un build + une config, immuable.
6. **Processes** : processus stateless ; l'état va dans un backing service.
7. **Port binding** : l'application expose elle-même son port (serveur embarqué).
8. **Concurrency** : on scale en ajoutant des processus.
9. **Disposability** : démarrage rapide, arrêt propre sur SIGTERM.
10. **Dev/prod parity** : environnements aussi proches que possible.
11. **Logs** : écrits sur la sortie standard comme un flux d'événements, collectés par la plateforme.
12. **Admin processes** : tâches ponctuelles (migrations) exécutées comme des processus distincts du même code.

## 18.7. Documenter une architecture

**Modèle C4** (Simon Brown) : quatre niveaux de zoom. *Context* (le système et ses utilisateurs/systèmes externes), *Containers* (applications, bases, files), *Components* (les blocs internes d'un conteneur), *Code* (rarement dessiné). **ADR** (Architecture Decision Record) : contexte, options, décision, conséquences, un fichier par décision, versionné avec le code. **Arc42** : un gabarit de documentation complet.

## 18.8. Cohérence et données distribuées

- **Modèles de cohérence** : forte (toute lecture voit la dernière écriture), *read-your-writes* (on voit au moins ses propres écritures), monotone, à terme (*eventual*).
- **PACELC** : extension de CAP. En cas de Partition, choisir entre A et C ; *Else* (fonctionnement normal), choisir entre Latence et Cohérence. Plus réaliste que CAP, car on arbitre la latence en permanence.
- **BASE** (Basically Available, Soft state, Eventual consistency) : l'opposé philosophique d'ACID, typique du NoSQL distribué.
- **Commit à deux phases (2PC)** : un coordinateur demande à chaque participant de se préparer, puis de committer. Garantit l'atomicité mais est bloquant (si le coordinateur tombe entre les deux phases, les participants restent verrouillés) et peu performant ; en microservices on préfère les sagas.
- **Identifiants distribués** : auto-incrément impossible à grande échelle sans coordination ; UUID v4 (aléatoire, mauvais pour les index B-tree car insertions dispersées), **UUID v7** (préfixé par le temps, triable, bien meilleur en clé primaire), Snowflake (horodatage + machine + séquence).
- **Hachage cohérent** : répartir des clés sur N nœuds de façon à ce qu'ajouter ou retirer un nœud ne déplace qu'une fraction des clés (caches distribués, bases partitionnées).
- **Verrou distribué** : base de données (`SELECT FOR UPDATE`, verrous consultatifs PostgreSQL), Redis (`SET NX PX`, avec les réserves connues sur Redlock), ZooKeeper/etcd. Toujours avec une expiration et un *fencing token* pour se protéger d'un détenteur qui se croit encore propriétaire.
- **Élection de leader** : un seul nœud exécute une tâche (job planifié) ; Kubernetes Lease, ShedLock en base, etcd.

## 18.9. Styles de communication

| | REST/HTTP | gRPC | Messaging (Kafka, RabbitMQ) | GraphQL |
|---|---|---|---|---|
| Couplage temporel | oui | oui | non | oui |
| Format | JSON (texte) | Protobuf (binaire) | libre (JSON, Avro...) | JSON |
| Contrat | OpenAPI | `.proto` strict | schéma optionnel | schéma |
| Idéal pour | API publiques, universel | service à service, performance, streaming | événements, découplage, pics | clients variés aux besoins de données différents |

**Trois usages des événements** : *event notification* (un signal minimal, le consommateur rappelle la source pour le détail), *event-carried state transfer* (l'événement contient les données, le consommateur garde sa copie), *event sourcing* (les événements sont la source de vérité).

## 18.10. Résilience, la boîte à outils complète

Timeout, retry avec backoff exponentiel et **jitter** (aléa pour éviter que tous les clients réessaient au même instant), circuit breaker, bulkhead, rate limiter, fallback, **load shedding** (rejeter volontairement une partie du trafic pour protéger le reste), **dégradation gracieuse**, file d'attente tampon, idempotence. En Java : **Resilience4j** (modules CircuitBreaker, Retry, RateLimiter, Bulkhead, TimeLimiter), MicroProfile Fault Tolerance (Quarkus), `@Retryable`/`@ConcurrencyLimit` (Spring Framework 7). Danger des **retries en cascade** : 3 retries à chaque niveau sur 4 niveaux = jusqu'à 81 appels sur le service du bas. On ne réessaie qu'à un seul niveau, avec un budget.

## 18.11. Méthode de *system design* en entretien

1. **Clarifier les exigences** fonctionnelles (que fait le système ?) et non fonctionnelles (volume, latence, disponibilité, cohérence).
2. **Estimer** les ordres de grandeur : utilisateurs, requêtes par seconde (1 million de requêtes par jour ≈ 12 par seconde en moyenne, prévoir un pic ×5 à ×10), volume de stockage.
3. **Définir l'API** et le **modèle de données**.
4. **Dessiner l'architecture de haut niveau** : clients, load balancer, services, base, cache, files.
5. **Approfondir** les points difficiles : goulots, scaling, pannes, cohérence.
6. **Annoncer les compromis** et ce qu'on ferait ensuite.

Exemple court, **raccourcisseur d'URL** : écriture rare, lecture massive (ratio 100:1) ; un identifiant court en base62 obtenu à partir d'un compteur ou d'un hash ; une table clé → URL ; un cache devant la base pour les liens populaires ; redirection 301 (mise en cache par le navigateur, moins de statistiques) ou 302 (chaque clic passe par le serveur) ; un enregistrement asynchrone des clics vers un pipeline d'analytics.

Exemple court, **rate limiter** : algorithme *token bucket* (un seau se remplit à débit fixe, chaque requête consomme un jeton, autorise des rafales) ; compteurs dans Redis (opérations atomiques ou script Lua) pour un état partagé entre instances ; réponse 429 avec `Retry-After` ; placé au niveau de l'API Gateway.

## 18.12. Monolithe modulaire en pratique

**Spring Modulith** : vérifie par des tests que les modules (packages de premier niveau) ne dépendent que des API publiques les uns des autres, documente les dépendances, et fournit une publication d'événements entre modules persistée (registre d'événements, proche de l'outbox). **ArchUnit** : tests unitaires qui vérifient des règles d'architecture (le domaine ne dépend pas de l'infrastructure, pas de cycles entre packages).

### En situation

- **"Comment tu construis l'architecture d'un projet ?"** → couches, DTOs, découpage par domaine.
- **"Faut-il découper ce monolithe ?"** → vraies raisons, strangler fig, bounded context.
- **"Comment faire communiquer deux services ?"** → sync vs async selon le besoin.
- **"Transaction sur plusieurs services ?"** → saga + compensations.
- **"Éviter la propagation d'une panne ?"** → timeout, circuit breaker, bulkhead.
- **"Comment tu justifies un choix d'architecture ?"** → attributs de qualité et compromis.

### Questions techniques

**Q : C'est quoi l'architecture, au fond ?** R : Les décisions structurantes et coûteuses à changer : découpage, communication, données, déploiement.

**Q : Architecture en couches vs hexagonale ?** R : En couches, les dépendances descendent vers la base ; en hexagonale, elles pointent vers le domaine, qui ignore la technique.

**Q : Adapter primaire vs secondaire ?** R : Primaire = pilote l'application (REST, CLI) ; secondaire = piloté par elle (base, API externe).

**Q : Inconvénients des microservices ?** R : Latence réseau, transactions distribuées, observabilité obligatoire, cohérence différée, complexité opérationnelle.

**Q : C'est quoi le Strangler Fig ?** R : Extraire progressivement d'un monolithe en détournant le trafic fonctionnalité par fonctionnalité, sans réécriture big bang.

**Q : Sur quel critère découper des microservices ?** R : Par bounded context métier, pas par couche technique.

**Q : Entity vs Value Object ?** R : Identité durable vs défini par ses attributs et immutable.

**Q : C'est quoi un Aggregate Root ?** R : Le seul point d'entrée d'un agrégat, garant de ses invariants et frontière de transaction.

**Q : C'est quoi un anti-corruption layer ?** R : Une couche de traduction empêchant un modèle externe de contaminer le domaine.

**Q : Les 3 états d'un circuit breaker ?** R : Fermé, ouvert, semi-ouvert.

**Q : C'est quoi le pattern Outbox ?** R : Écrire l'événement en base dans la même transaction que la donnée, puis le publier, pour ne jamais perdre un message.

**Q : CQRS et Event Sourcing, obligatoirement ensemble ?** R : Non, ils sont indépendants, même s'ils se combinent souvent.

**Q : Théorème CAP ?** R : Cohérence, Disponibilité, tolérance au Partitionnement, on n'en garantit que deux, et le vrai choix est C ou A pendant une partition.

**Q : Citez des principes du 12-factor app.** R : Config dans l'environnement, processus stateless, backing services attachés, logs sur stdout, arrêt propre, parité dev/prod, build/release/run séparés.

**Q : C'est quoi le modèle C4 ?** R : Quatre niveaux de diagrammes : Context, Containers, Components, Code.

**Q : C'est quoi PACELC ?** R : En cas de partition, A ou C ; sinon, latence ou cohérence.

**Q : ACID vs BASE ?** R : Cohérence forte et transactions vs disponibilité et cohérence à terme.

**Q : Pourquoi éviter le 2PC en microservices ?** R : Bloquant, fragile face à la panne du coordinateur et peu performant ; on lui préfère les sagas.

**Q : Pourquoi l'UUID v7 plutôt que v4 en clé primaire ?** R : Il est ordonné dans le temps, donc les insertions restent localisées dans l'index B-tree.

**Q : C'est quoi le hachage cohérent ?** R : Une répartition des clés telle qu'ajouter ou retirer un nœud n'en déplace qu'une petite fraction.

**Q : C'est quoi un fencing token ?** R : Un numéro croissant associé à un verrou distribué, qui permet à la ressource de rejeter un ancien détenteur.

**Q : Event notification vs event-carried state transfer ?** R : Un signal minimal qui oblige à rappeler la source vs un événement qui porte les données.

**Q : Pourquoi ajouter du jitter aux retries ?** R : Pour éviter que tous les clients réessaient au même moment et recréent un pic.

**Q : C'est quoi le load shedding ?** R : Rejeter volontairement une partie de la charge pour protéger le service.

**Q : Pourquoi les retries à plusieurs niveaux sont-ils dangereux ?** R : Ils se multiplient (amplification) et achèvent le service défaillant.

**Q : Les modules de Resilience4j ?** R : CircuitBreaker, Retry, RateLimiter, Bulkhead, TimeLimiter (et Cache).

**Q : Les étapes d'un exercice de system design ?** R : Exigences, estimations, API et données, architecture de haut niveau, approfondissement, compromis.

**Q : 1 million de requêtes par jour, combien par seconde ?** R : Environ 12 en moyenne ; prévoir un pic nettement supérieur.

**Q : À quoi sert ArchUnit ?** R : À tester automatiquement des règles d'architecture (dépendances entre couches, cycles).

**Q : Algorithme token bucket ?** R : Des jetons ajoutés à débit fixe, une requête consomme un jeton ; il autorise des rafales jusqu'à la taille du seau.

**Q : C'est quoi un modèle anémique ?** R : Des entités sans comportement dont toute la logique est dans des services.

### Questions de situation

**Q : "Comment tu construis l'architecture d'un nouveau projet backend ?"**
R : "Je pars des **contraintes** avant des solutions : volume attendu, criticité, taille de l'équipe, contraintes de délai. L'architecture doit y répondre, pas afficher des patterns.

Sur le squelette, un **découpage en couches** à responsabilités strictes : le contrôleur reçoit, valide le format et délègue, aucune logique métier ; le service porte la logique et la transaction ; le repository fait uniquement la persistance.

Entre les couches, des **DTOs**, jamais des entités JPA exposées. Sinon le contrat public est couplé au schéma de base, et une migration de colonne casse les clients.

Pour les packages, je découpe **par domaine métier** (`pricing/`, `booking/`) plutôt que par couche technique. Ça garde une fonctionnalité au même endroit, limite le couplage, et prépare une extraction : le package devient le service.

Sur les dépendances : injection par constructeur, sur des interfaces, ce qui rend chaque couche testable isolément.

Si le système est durable et le métier riche, je pousserais vers l'**hexagonal** : le domaine au centre définissant ses ports, l'infrastructure en adapters. Sur un CRUD simple, ce serait de la sur-ingénierie.

Et transversalement : configuration externalisée par environnement, secrets dans un vault, observabilité prévue dès le départ, pas ajoutée après coup."

**Q : "Un monolithe devient difficile à maintenir. Faut-il passer aux microservices ?"**
R : "Je challengerais la question, parce que les microservices ne résolvent pas la dette technique : un monolithe mal structuré donne des microservices mal structurés, avec le réseau en plus.
Je chercherais la **vraie cause**. Si c'est du couplage interne, la réponse est un refactoring vers un **monolithe modulaire** : des modules à frontières nettes, sans complexité distribuée. C'est souvent suffisant et bien moins risqué.
Les raisons qui justifient réellement l'extraction sont surtout organisationnelles : des équipes qui se bloquent sur les déploiements, des besoins de **scaling très différents** entre parties, ou un besoin d'**isolation des pannes**.
Si c'est justifié, je n'extrais jamais en big bang. J'applique le **Strangler Fig** : un service à la fois, en commençant par un bounded context à frontière nette et faible couplage, avec un routage progressif du trafic.
Démarche réaliste : extraire un premier domaine à frontière nette (l'éligibilité, la tarification ou la facturation par exemple) vers un service dédié, façade par façade, en validant la parité de comportement par rejeu de trafic de production avant la bascule."

**Q : "Une commande doit débiter un compte, réserver du stock et notifier, sur trois services."**
R : "Transaction distribuée. Il n'existe pas de rollback global, le commit à deux phases est bloquant et fragile, on l'évite.
J'utiliserais une **Saga** : une suite d'étapes locales, chacune avec sa **compensation**. Si la réservation échoue après le débit, on déclenche un remboursement plutôt qu'un rollback.
Entre **chorégraphie** et **orchestration**, je prendrais l'orchestration sur un flux critique comme un paiement : un orchestrateur pilote les étapes, ce qui rend le flux lisible et traçable. La chorégraphie est plus simple à démarrer mais le parcours global devient difficile à suivre quand il grossit.
Deux exigences techniques : les consommateurs doivent être **idempotents**, car un message peut être livré deux fois ; et pour publier de façon fiable, j'utiliserais le **pattern Outbox** (écrire l'événement dans la même transaction que la donnée métier, puis le publier) sinon on peut committer en base et perdre le message.
Enfin, l'**eventual consistency** est un choix à valider avec le métier : pendant un instant le système est dans un état intermédiaire, et il faut que ce soit acceptable fonctionnellement."

**Q : "Un service dont tu dépends devient très lent et le tien tombe aussi."**
R : "Panne en cascade, et le problème n'est pas seulement chez le voisin : mon service n'est pas protégé.
En urgence, je vérifierais les **timeouts**. Sans timeout, chaque appel lent immobilise un thread ; le pool sature ; et mon service cesse de répondre même pour des requêtes sans rapport avec la dépendance défaillante. C'est de loin la cause la plus fréquente.
Sur le fond, un **circuit breaker** : au-delà d'un taux d'échec, il coupe les appels, ce qui protège le service en panne d'être achevé par les retries et libère mes ressources, puis il teste périodiquement la reprise.
Un **fallback** pour offrir une dégradation fonctionnelle plutôt qu'une erreur brute : valeur par défaut, donnée en cache, fonctionnalité réduite.
Et un **bulkhead** pour cloisonner : un pool dédié par dépendance, afin qu'une seule ne consomme pas toutes les ressources.
En Quarkus, tout ça existe en annotations MicroProfile : `@Timeout`, `@CircuitBreaker`, `@Fallback`, `@Bulkhead`."

**Q : "Comment tu justifies un choix d'architecture face à un manager ?"**
R : "Je le formulerais en **compromis**, pas en préférence technique. Concrètement : voici les options, ce que chacune apporte, ce qu'elle coûte, et quels risques elle fait porter.
Par exemple pour des microservices : gain d'autonomie des équipes et de scaling ciblé ; coût en complexité opérationnelle, en observabilité à construire, et en compétences DevOps nécessaires. Face à ça, un monolithe modulaire coûte moins et livre plus vite, mais plafonnera si l'équipe double.
Je m'appuierais sur les **attributs de qualité** attendus (disponibilité visée, temps de réponse cible, fréquence de déploiement souhaitée) parce que ce sont des critères discutables avec le métier, contrairement à des arguments purement techniques.
Et je documenterais la décision, idéalement sous forme d'**ADR** (Architecture Decision Record) : le contexte, les options envisagées, le choix, les conséquences. Ça évite qu'on rejoue le débat dans six mois sans se souvenir des raisons."

**Q : "Conçois un système de notifications (email, SMS, push) pour une application à forte audience."**
R : "Je clarifierais d'abord : volume (millions par jour ?), latence acceptable (une notification de sécurité est urgente, une newsletter non), garanties (au moins une fois, pas de doublon visible ?).
Architecture : les services métier publient un événement 'notification demandée' dans **Kafka**, pour découpler la production des envois et absorber les pics. Un service de notification consomme, applique les préférences de l'utilisateur et les règles (horaires, fréquence maximale), puis dépose dans une file **par canal**, parce que chaque fournisseur (SMTP, SMS, push) a ses propres limites de débit et pannes.
Chaque worker de canal applique **rate limiting**, **retry avec backoff**, **circuit breaker** vers le fournisseur, et une **DLQ** pour les échecs définitifs. L'**idempotence** est assurée par un identifiant de notification enregistré avant l'envoi, pour qu'un message rejoué ne soit pas envoyé deux fois.
Je séparerais les priorités (urgent vs marketing) dans des topics distincts pour qu'une campagne massive ne retarde jamais une alerte de sécurité. Et je mesurerais le lag par file et le taux d'échec par fournisseur."

**Q : "Deux instances de ton service exécutent le même job planifié et envoient deux fois les factures."**
R : "Le `@Scheduled` s'exécute sur chaque instance : avec plusieurs replicas, il faut une coordination. Options : **ShedLock**, qui pose un verrou en base avec une durée maximale, pour qu'une seule instance exécute le job ; un **Kubernetes CronJob** qui sort le job de l'application ; ou une élection de leader.
Mais je rendrais surtout le traitement **idempotent** : marquer chaque facture comme envoyée dans la même transaction que la décision d'envoi, pour qu'une double exécution, qui arrivera un jour (redémarrage, verrou expiré), soit sans effet."

═══════════════════════════════════════════
# 4. QUALITÉ, CLEAN CODE & MÉTHODES DE TRAVAIL
═══════════════════════════════════════════

### Vue d'ensemble

Un code se lit bien plus souvent qu'il ne s'écrit. La qualité ne se juge donc pas à l'élégance d'une solution, mais au **coût de la prochaine modification** : combien de temps pour comprendre, combien de risque de casser. Le *clean code* (Robert C. Martin), le *refactoring* (Martin Fowler) et l'analyse statique sont trois outils au service de ce même objectif.

Côté méthodes, les recruteurs vérifient que tu sais travailler en équipe : comment se déroule un sprint, ce qu'est une Definition of Done, comment se passe une revue de code, comment on estime. Ce ne sont pas des questions pièges, mais une réponse vague ("on fait de l'agile") trahit vite un manque d'expérience. Une réponse concrète ("daily de 15 minutes, sprints de deux semaines, revue obligatoire par un pair avant merge") rassure.

### Cours

**Règles de clean code** :
- **Nommage** : des noms qui révèlent l'intention (`joursDepuisDerniereConnexion` plutôt que `d`), pas d'abréviations obscures, un vocabulaire métier cohérent, des noms de méthodes en verbes et de classes en noms.
- **Fonctions** courtes, qui font une seule chose, à un seul niveau d'abstraction, avec peu de paramètres (au-delà de trois, un objet paramètre) et sans **argument booléen** qui sépare deux comportements (faire deux méthodes).
- **Retours anticipés** (*guard clauses*) plutôt que des `if` imbriqués sur cinq niveaux.
- **Commentaires** : ils expliquent le *pourquoi*, jamais le *quoi* (le code doit le dire) ; un commentaire qui paraphrase le code devient faux à la première modification.
- **Pas de nombres magiques** : des constantes nommées.
- **Gestion d'erreur** séparée de la logique, pas de `null` retourné quand une collection vide ou un Optional conviennent.
- **Règle du boy-scout** : laisser le code un peu plus propre qu'on ne l'a trouvé.

**Code smells** (Fowler) : méthode trop longue, classe trop grosse, code dupliqué, liste de paramètres trop longue, *feature envy* (une méthode qui utilise surtout les données d'une autre classe), *primitive obsession* (des String pour un email, un montant, un identifiant au lieu de types dédiés), *data clumps* (les mêmes trois paramètres toujours ensemble), *shotgun surgery* (un changement oblige à toucher dix classes), *divergent change* (une classe modifiée pour des raisons sans rapport), *switch statements* répétés sur un type, généralité spéculative, code mort, chaînes de messages (`a.getB().getC()`).

**Refactorings courants** : extraire une méthode ou une classe, renommer, introduire un objet paramètre, remplacer une condition par du polymorphisme, remplacer un code type par une classe ou une enum, déplacer une méthode, *inline* (supprimer une indirection inutile), encapsuler un champ, introduire une variable explicative. Règle : un refactoring **ne change pas le comportement**, se fait par petits pas, avec les tests au vert à chaque étape, et dans des commits séparés des changements fonctionnels.

**Dette technique** : le coût futur d'un raccourci pris aujourd'hui. Le quadrant de Fowler distingue la dette **délibérée** ou **involontaire**, **prudente** ("on livre maintenant et on reprendra") ou **imprudente** ("pas le temps de faire de la conception"). Elle se gère comme une dette financière : visible (dans le backlog), mesurée, remboursée régulièrement (une part de chaque sprint), et contractée en connaissance de cause.

**Métriques** : complexité **cyclomatique** (nombre de chemins indépendants dans une méthode), complexité **cognitive** (SonarQube, mesure la difficulté de lecture en pénalisant l'imbrication), couverture de tests, duplication, couplage afférent/efférent. Des indicateurs pour orienter, pas des objectifs à atteindre à tout prix (loi de Goodhart : une mesure qui devient un objectif cesse d'être une bonne mesure).

**Analyse statique** : **SonarQube/SonarCloud** (bugs, vulnérabilités, code smells, *quality gate* qui bloque le merge), SonarLint dans l'IDE, **Checkstyle** (style), **PMD** (mauvaises pratiques), **SpotBugs** (bugs probables dans le bytecode), **Error Prone** (vérifications à la compilation), **NullAway** avec les annotations **JSpecify** (`@NullMarked`, `@Nullable`) pour détecter les NPE à la compilation. Formatage automatique (Spotless, google-java-format) pour supprimer les débats de style.

**Revue de code** : des pull requests **petites** (quelques centaines de lignes au plus), une description qui explique le pourquoi, l'automatisation de tout ce qui peut l'être (format, tests, analyse) pour que l'humain se concentre sur la conception, la lisibilité et les cas limites. Commenter le code et non la personne, distinguer bloquant et suggestion, poser des questions plutôt qu'imposer. L'auteur reste responsable de son code ; le relecteur partage la responsabilité de ce qui est mergé.

**Le manifeste agile** (2001), quatre valeurs : les individus et leurs interactions plutôt que les processus et les outils ; un logiciel qui fonctionne plutôt qu'une documentation exhaustive ; la collaboration avec le client plutôt que la négociation contractuelle ; l'adaptation au changement plutôt que le suivi d'un plan. ("Plutôt que" : les seconds ont de la valeur, les premiers en ont davantage.)

**Scrum** :
- **Rôles** : *Product Owner* (porte la vision, priorise le backlog), *Scrum Master* (facilite, lève les obstacles, garant du cadre), *Developers* (l'équipe qui réalise, auto-organisée).
- **Événements** : le **sprint** (1 à 4 semaines, souvent 2), le *sprint planning* (que livrer et comment), le *daily scrum* (15 minutes, synchronisation sur l'objectif du sprint), la *sprint review* (démonstration de l'incrément aux parties prenantes), la *rétrospective* (amélioration du fonctionnement de l'équipe).
- **Artefacts** : *product backlog*, *sprint backlog*, **incrément** ; engagements associés : objectif de produit, objectif de sprint, **Definition of Done**.

**Kanban** : un flux continu sans sprints, un tableau visuel, des **limites de travail en cours** (WIP) par colonne pour finir avant de commencer, des métriques de flux (*lead time* de la demande à la livraison, *cycle time* du début du travail à la livraison, débit). Adapté au support, à l'exploitation et aux flux imprévisibles.

**User stories** : "En tant que [rôle], je veux [action] afin de [bénéfice]", avec des **critères d'acceptation** (souvent en Gherkin : *Given / When / Then*). Critères INVEST : Indépendante, Négociable, porteuse de Valeur, Estimable, Small (petite), Testable. **Definition of Ready** : ce qui doit être clair avant de commencer ; **Definition of Done** : ce qui doit être vrai pour considérer un élément terminé (code revu, tests, documentation, déployé en recette...).

**Estimation** : *story points* (effort relatif, incluant complexité et incertitude, souvent sur une suite de Fibonacci), *planning poker* (estimation simultanée puis discussion des écarts, qui révèle les malentendus), tailles de t-shirt, vélocité (points livrés par sprint, pour prévoir, jamais pour comparer des équipes). Le mouvement *#NoEstimates* préfère découper finement et compter les éléments.

**Culture DevOps** : casser le mur entre développement et exploitation ("you build it, you run it"), automatiser, mesurer, partager. Le cadre **CALMS** (Culture, Automation, Lean, Measurement, Sharing). Les quatre **métriques DORA** de performance de livraison : fréquence de déploiement, délai de mise en production d'un changement (*lead time for changes*), taux d'échec des changements, temps de restauration après incident (un cinquième indicateur de fiabilité a été ajouté plus tard). **Post-mortem sans blâme** après incident : chronologie, causes contributives, actions, sans chercher de coupable.

**Versioning sémantique** (SemVer) : `MAJEUR.MINEUR.CORRECTIF`. Majeur pour un changement incompatible, mineur pour une fonctionnalité rétrocompatible, correctif pour une correction rétrocompatible. Les *Conventional Commits* (`feat:`, `fix:`, `feat!:` ou `BREAKING CHANGE`) permettent de calculer la version et le changelog automatiquement.

**Pratiques de collaboration** : *pair programming* (pilote et navigateur), *mob programming* (toute l'équipe sur un poste), TDD, *trunk-based development*, documentation vivante (README, ADR, OpenAPI, tests lisibles comme spécification).

### En situation

- **"Comment tu reconnais du code de mauvaise qualité ?"** → code smells nommés, avec leur conséquence.
- **"Le PO veut toujours des fonctionnalités et la dette s'accumule"** → rendre la dette visible et chiffrée.
- **"Tu n'es pas d'accord avec un commentaire de revue"** → argumenter sur des faits, escalader en dernier recours.
- **"Comment estimes-tu une tâche que tu ne connais pas ?"** → découper, spike, fourchette.
- **"Décris ton fonctionnement d'équipe"** → rituels concrets, DoD, revue.

### Questions techniques

**Q : Qu'est-ce qu'un code propre ?** R : Un code facile à lire, à comprendre et à modifier sans risque ; il exprime son intention.

**Q : Que doit expliquer un commentaire ?** R : Le pourquoi (contexte, décision, contrainte), pas le quoi.

**Q : Pourquoi éviter un paramètre booléen ?** R : Il signale une méthode qui fait deux choses et rend l'appel illisible (`creer(true)`).

**Q : C'est quoi une guard clause ?** R : Un retour anticipé en début de méthode qui évite l'imbrication.

**Q : Citez des code smells.** R : Méthode longue, grosse classe, duplication, feature envy, primitive obsession, shotgun surgery, data clumps, longue liste de paramètres.

**Q : C'est quoi la primitive obsession ?** R : Utiliser des types primitifs (String, int) là où un type métier (Email, Montant) porterait validation et sens.

**Q : C'est quoi le shotgun surgery ?** R : Un changement unique qui oblige à modifier de nombreuses classes.

**Q : Un refactoring peut-il changer le comportement ?** R : Non, par définition ; sinon c'est une modification fonctionnelle.

**Q : C'est quoi la dette technique ?** R : Le surcoût futur induit par un raccourci de conception ou de code pris aujourd'hui.

**Q : Complexité cyclomatique vs cognitive ?** R : Nombre de chemins d'exécution vs difficulté de lecture (pénalise l'imbrication).

**Q : C'est quoi une quality gate ?** R : Un ensemble de seuils (bugs, couverture, duplication sur le nouveau code) qui bloque le merge s'ils ne sont pas respectés.

**Q : SpotBugs vs Checkstyle vs PMD ?** R : Bugs probables dans le bytecode / respect du style / mauvaises pratiques dans le source.

**Q : Les trois rôles Scrum ?** R : Product Owner, Scrum Master, Developers.

**Q : Les événements Scrum ?** R : Sprint, sprint planning, daily scrum, sprint review, rétrospective.

**Q : Review vs rétrospective ?** R : Inspecter le produit avec les parties prenantes vs inspecter le fonctionnement de l'équipe.

**Q : C'est quoi la Definition of Done ?** R : La liste des critères qu'un élément doit remplir pour être considéré terminé.

**Q : Scrum vs Kanban ?** R : Itérations à durée fixe avec rôles et rituels vs flux continu avec limites de travail en cours.

**Q : Que signifie INVEST ?** R : Indépendante, Négociable, Valeur, Estimable, Small, Testable.

**Q : Pourquoi les story points plutôt que des heures ?** R : Ils estiment un effort relatif incluant l'incertitude, plus stable que des durées absolues.

**Q : Lead time vs cycle time ?** R : De la demande à la livraison vs du début du travail à la livraison.

**Q : Les métriques DORA ?** R : Fréquence de déploiement, délai de mise en production, taux d'échec des changements, temps de restauration.

**Q : C'est quoi un post-mortem sans blâme ?** R : Une analyse d'incident centrée sur les causes systémiques et les actions, sans désigner de coupable.

**Q : Que signifie une version 3.2.1 en SemVer ?** R : Majeure 3 (compatibilité), mineure 2 (fonctionnalités compatibles), correctif 1.

**Q : C'est quoi Gherkin ?** R : Une syntaxe Given/When/Then pour écrire des critères d'acceptation exécutables (Cucumber).

### Questions de situation

**Q : "Le Product Owner refuse systématiquement de prioriser le remboursement de la dette technique."**
R : "Je traduirais la dette en **impact métier**, parce que 'le code est sale' n'est pas un argument pour un PO. Par exemple : 'chaque évolution du module de facturation prend trois jours au lieu d'un', 'ce composant a causé quatre incidents ce trimestre', 'personne d'autre que X ne peut le modifier'.
Je proposerais ensuite un mécanisme régulier plutôt qu'un grand chantier : une part fixe de chaque sprint (souvent 15 à 20 %), ou le remboursement intégré aux stories qui touchent le code concerné. Et je rendrais la dette **visible** dans le backlog, avec une estimation, pour qu'elle soit arbitrée comme le reste et non oubliée."

**Q : "En revue de code, un collègue senior rejette ta PR pour une raison avec laquelle tu n'es pas d'accord."**
R : "Je commencerais par m'assurer d'avoir compris sa préoccupation, en lui posant la question plutôt qu'en défendant ma solution. Souvent il voit un risque que je n'ai pas vu.
Si je reste en désaccord, j'argumenterais sur des **faits** : un test qui montre le comportement, une mesure, une référence à une convention de l'équipe. Si ça reste une affaire de préférence, je me rangerais à la convention de l'équipe ou à son avis, en proposant d'en discuter collectivement pour fixer une règle. L'objectif n'est pas de gagner la discussion, c'est que le code mergé soit bon et que la relation reste saine."

**Q : "On te demande une estimation pour une tâche dans une techno que tu ne connais pas."**
R : "Je refuserais de donner un chiffre précis à l'aveugle, mais je ne bloquerais pas non plus. Je proposerais un **spike** à durée limitée (une demi-journée ou une journée) pour explorer et lever les inconnues principales, puis une estimation sérieuse.
Si on a besoin d'un ordre de grandeur immédiat, je donnerais une **fourchette** avec les hypothèses qui la fondent ('entre 3 et 8 jours selon que la bibliothèque X gère nativement Y'), et je découperais la tâche pour isoler la partie incertaine. Annoncer clairement l'incertitude est plus utile qu'un chiffre faussement précis."

═══════════════════════════════════════════
# GLOSSAIRE DES SIGLES ET TERMES
═══════════════════════════════════════════

| Terme | Signification |
|---|---|
| **ADR** | Architecture Decision Record : document court qui consigne une décision d'architecture |
| **API Gateway** | Point d'entrée unique devant des services (routage, authentification, limitation) |
| **BFF** | Backend For Frontend : une façade par type de client |
| **CQRS** | Command Query Responsibility Segregation : modèles d'écriture et de lecture séparés |
| **DDD** | Domain-Driven Design : conception centrée sur le métier |
| **DI / IoC** | Dependency Injection / Inversion of Control |
| **DIP** | Dependency Inversion Principle (le D de SOLID) |
| **DoD / DoR** | Definition of Done / Definition of Ready |
| **DRY, KISS, YAGNI** | Don't Repeat Yourself, Keep It Simple, You Aren't Gonna Need It |
| **DTO** | Data Transfer Object : objet de transfert découplé du modèle interne |
| **GoF** | Gang of Four : auteurs du catalogue des 23 design patterns |
| **ISP** | Interface Segregation Principle (le I de SOLID) |
| **LSP** | Liskov Substitution Principle (le L de SOLID) |
| **MVC** | Model-View-Controller |
| **OCP** | Open/Closed Principle (le O de SOLID) |
| **SOLID** | SRP, OCP, LSP, ISP, DIP |
| **SRP** | Single Responsibility Principle (le S de SOLID) |
| **WIP** | Work In Progress : travail en cours (limites Kanban) |
