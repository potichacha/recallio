---
subject: java
title: Java
category: languages
position: 10
status: published
---

# Java & son écosystème : le document de référence pour les entretiens techniques

> Un document unique pour préparer un entretien Java de tous niveaux (junior à senior) : langage, JVM, conception, frameworks, données, DevOps, sécurité, cloud, algorithmes et questions comportementales.
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (les ponts entre symptômes et notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ctrl+F : un mot-clé (`HashMap`, `@Transactional`, `OOMKilled`, `PKCE`...), un sigle (voir le glossaire en fin de document), ou "Questions de situation" pour ne lire qu'un type de bloc.
> Informations à jour à l'automne 2026 : JDK 25 (dernière LTS) et JDK 27, Spring Boot 4 / Spring Framework 7, JUnit 6, OWASP Top 10 2025.

---

## SOMMAIRE

**MÉTHODE UNIVERSELLE** : le réflexe des mises en situation

**PARTIE I : JAVA CORE** : 1. JAVA : FONDAMENTAUX · 2. JAVA : CLASSES, OBJETS & LANGAGE AVANCÉ · 3. JAVA : CHAÎNES, NOMBRES, DATES & TEXTE · 4. COLLECTIONS · 5. STREAMS & LAMBDAS · 6. CONCURRENCE & MULTITHREADING · 7. PROGRAMMATION RÉACTIVE · 8. MÉMOIRE & GARBAGE COLLECTOR · 9. JVM INTERNALS, PERFORMANCE & OUTILLAGE · 10. EXCEPTIONS · 11. GENERICS · 12. I/O, NIO, SÉRIALISATION & JSON · 13. JAVA MODERNE : RECORDS, SEALED, PATTERN MATCHING & VERSIONS · 14. QUESTIONS PIÈGES : QU'AFFICHE CE CODE ?

**PARTIE II : CONCEPTION** : 15. PROGRAMMATION ORIENTÉE OBJET · 16. PRINCIPES SOLID · 17. DESIGN PATTERNS · 18. ARCHITECTURE LOGICIELLE · 19. QUALITÉ, CLEAN CODE & MÉTHODES DE TRAVAIL

**PARTIE III : FRAMEWORKS & DONNÉES** : 20. SPRING BOOT · 21. SPRING AVANCÉ : DATA, SECURITY, CLOUD, BATCH · 22. QUARKUS · 23. JAKARTA EE, SERVLETS & MICROPROFILE · 24. API REST · 25. BASES DE DONNÉES · 26. JPA, HIBERNATE & JDBC EN PROFONDEUR · 27. SQL PRATIQUE · 28. TESTS

**PARTIE IV : DEVOPS** : 29. GIT · 30. MAVEN, GRADLE & BUILD · 31. DOCKER · 32. KUBERNETES & OPENSHIFT · 33. CI/CD & GITOPS · 34. OBSERVABILITÉ · 35. SÉCURITÉ · 36. RÉSEAU & LINUX · 37. CLOUD & INFRASTRUCTURE AS CODE

**PARTIE V : COMPLÉMENTS** : 38. MESSAGING & KAFKA · 39. CACHE · 40. ALGORITHMES & COMPLEXITÉ · 41. EXERCICES DE LIVE CODING CORRIGÉS

**PARTIE VI : SOFT SKILLS** : 42. QUESTIONS COMPORTEMENTALES & PRÉSENTER SON EXPÉRIENCE

**GLOSSAIRE** · **RÉCAP FINAL**

═══════════════════════════════════════════
# MÉTHODE UNIVERSELLE : MISES EN SITUATION
═══════════════════════════════════════════

Toute question ouverte se répond avec le même réflexe. L'évaluateur n'attend pas LA bonne réponse, il évalue ta démarche.

**1. CADRER** : pose une question avant de répondre.
> "Ça dépend, c'est sur tous les endpoints ou un seul ? Depuis un déploiement récent ? Sous charge ou tout le temps ?"

**2. MESURER** : jamais d'optimisation à l'aveugle.
> "Je mesure d'abord pour localiser le goulot : métriques, traces, logs."

**3. AGIR** : du moins au plus coûteux, une chose à la fois.

**4. VALIDER** : remesurer et protéger contre la régression.

**Les réflexes verbaux :** *"Je mesure avant d'optimiser"* · *"Je regarde le p95, pas la moyenne"* · *"Je ne sais pas, mais voici comment je chercherais"*.

═══════════════════════════════════════════
# 1. JAVA : FONDAMENTAUX
═══════════════════════════════════════════

### Vue d'ensemble

Java est un langage **compilé vers un bytecode** exécuté par une machine virtuelle (la JVM), ce qui lui donne sa portabilité : le même bytecode tourne sur Windows, Linux ou macOS. C'est un langage **fortement typé** (les types sont vérifiés à la compilation) et **orienté objet**, avec une gestion **automatique de la mémoire**.

Les questions de fondamentaux en entretien tournent presque toujours autour de trois thèmes : **la distinction valeur/référence** (d'où viennent les pièges `==`, le passage de paramètres, l'immutabilité), **le contrat des objets** (equals/hashCode, sans quoi les collections ne fonctionnent pas), et **la portée/visibilité** (qui peut voir quoi). Ce ne sont pas des détails académiques : ce sont les causes réelles de la majorité des bugs subtils en production.

### Cours

**Les 8 types primitifs** : `byte` (8 bits), `short` (16), `int` (32), `long` (64), `float` (32), `double` (64), `char` (16, Unicode), `boolean`. Chacun a un **wrapper** objet (`int`↔`Integer`). Le passage automatique entre les deux est l'**autoboxing**/**unboxing**.

**Valeur vs référence.** Un primitif stocke sa valeur ; une variable d'objet stocke une **référence** (adresse) vers le heap. Conséquence directe : `==` compare les valeurs pour les primitifs et les **adresses** pour les objets.

**Le piège du cache Integer :**
```java
Integer a = 127, b = 127;  a == b;  // true  → cache [-128, 127]
Integer c = 128, d = 128;  c == d;  // false → objets distincts
c.equals(d);                        // true
```
Java met en cache les Integer de -128 à 127 (optimisation mémoire pour les petites valeurs fréquentes). Hors de cette plage, chaque autoboxing crée un nouvel objet.

**String** est immutable : toute "modification" crée un nouvel objet. Trois raisons : la sécurité (un chemin ou une URL ne peut être altéré après validation), la thread-safety gratuite, et le **String Pool** : une zone où Java déduplique les littéraux identiques. D'où `"a" == "a"` vrai, mais `new String("a") == "a"` faux.

Pour construire des chaînes : `StringBuilder` (mutable, non synchronisé, rapide) ou `StringBuffer` (synchronisé, plus lent). Concaténer avec `+=` dans une boucle est en O(n²) : chaque itération recopie toute la chaîne.

**Le contrat equals/hashCode.** Si `a.equals(b)` est vrai, `a.hashCode()` doit égaler `b.hashCode()`. L'inverse n'est pas exigé (deux objets différents peuvent partager un hash, c'est une collision, c'est normal). Ce contrat existe parce que les collections hash procèdent en deux temps : le hashCode localise le bucket, puis equals identifie l'élément exact dans ce bucket. Redéfinir l'un sans l'autre casse ce mécanisme et fait "disparaître" les objets.

**Passage de paramètres.** Java passe **toujours par valeur**. Pour un objet, la valeur transmise est une copie de la référence : on peut donc modifier l'objet pointé (`list.add(x)` est visible par l'appelant), mais réassigner la variable locale (`list = new ArrayList<>()`) n'affecte pas l'appelant.

**Visibilité** : quatre niveaux, du plus ouvert au plus fermé :
| Modificateur | Même classe | Même package | Sous-classe | Partout |
|---|---|---|---|---|
| `public` | ✓ | ✓ | ✓ | ✓ |
| `protected` | ✓ | ✓ | ✓ | ✗ |
| *(aucun)* = package-private | ✓ | ✓ | ✗ | ✗ |
| `private` | ✓ | ✗ | ✗ | ✗ |

Le piège : **package-private ne s'écrit avec aucun mot-clé**.

**`final`** : variable (pas de réassignation), méthode (pas d'override), classe (pas d'héritage). `final` fige la référence, pas le contenu de l'objet.

**`static`** : lié à la classe et non à l'instance. Un champ static est partagé ; une méthode static n'a pas de `this`. Un bloc static s'exécute une fois au chargement de la classe.

**Piège classique, final / finally / finalize** : mot-clé d'immutabilité / bloc toujours exécuté après try-catch / ancienne méthode appelée avant le GC (dépréciée, ne jamais utiliser).

**JDK, JRE, JVM.** La **JVM** exécute le bytecode (une implémentation par OS, d'où le "write once, run anywhere"). Le **JRE** = JVM + bibliothèques standard, de quoi *exécuter*. Le **JDK** = JRE + outils de développement (`javac`, `jar`, `javadoc`, `jshell`, `jcmd`, `jlink`...), de quoi *développer*. Depuis Java 11, Oracle ne distribue plus de JRE séparé : on livre un JDK, ou un runtime réduit fabriqué avec `jlink`. Mémo : JDK ⊃ JRE ⊃ JVM.

**Du source à l'exécution** : `javac Hello.java` produit `Hello.class` (bytecode) ; `java Hello` démarre la JVM, qui charge la classe, la vérifie, l'interprète puis compile à la volée (JIT) les méthodes chaudes. Depuis Java 11 on peut lancer directement un fichier source (`java Hello.java`), et depuis Java 22 un programme multi-fichiers.

**Le point d'entrée** : historiquement `public static void main(String[] args)`. Depuis Java 25 (JEP 512, "compact source files and instance main methods"), un fichier peut contenir directement `void main() { IO.println("Bonjour"); }` sans classe déclarée explicitement : utile pour l'apprentissage et les scripts.

**Taille et valeurs par défaut des primitifs** :
| Type | Taille | Plage / remarque | Défaut (champ) |
|---|---|---|---|
| `byte` | 8 bits | -128 à 127 | 0 |
| `short` | 16 bits | -32 768 à 32 767 | 0 |
| `int` | 32 bits | environ ±2,1 milliards | 0 |
| `long` | 64 bits | suffixe `L` | 0L |
| `float` | 32 bits | suffixe `f`, ~7 chiffres significatifs | 0.0f |
| `double` | 64 bits | ~15-16 chiffres significatifs | 0.0 |
| `char` | 16 bits | unité UTF-16, non signé (0 à 65 535) | '\u0000' |
| `boolean` | non spécifiée | true / false | false |

Les **champs** reçoivent une valeur par défaut ; les **variables locales** non : les lire sans les initialiser est une erreur de compilation.

**Opérateurs à connaître** : `&&`/`||` court-circuitent (le côté droit n'est pas évalué si inutile), `&`/`|` évaluent toujours les deux côtés. `i++` retourne la valeur puis incrémente, `++i` incrémente puis retourne. `>>` décale en conservant le signe, `>>>` remplit avec des zéros. L'opérateur ternaire `cond ? a : b`. `instanceof` teste le type à l'exécution (faux pour `null`).

**Conversions** : élargissement implicite (`int` → `long` → `float` → `double`), rétrécissement par cast explicite avec perte possible (`(byte) 200` vaut -56). Les affectations composées (`b += 1`) contiennent un cast implicite, contrairement à `b = b + 1` qui ne compile pas pour un `byte`.

**Le cache des wrappers** : Integer, Short, Byte, Long et Character (0 à 127) ont un cache ; Boolean a deux instances. La borne haute du cache Integer peut être augmentée avec `-XX:AutoBoxCacheMax`, raison de plus pour ne jamais compter dessus. Les constructeurs `new Integer(5)` sont dépréciés (pour suppression) : on utilise `Integer.valueOf(5)`. Attention à l'**unboxing d'un null** : `int x = mapDesInteger.get("absent");` lève une NullPointerException.

**Le String Pool** est dans le heap depuis Java 7 (il était dans le PermGen avant). `intern()` place une chaîne dans le pool et renvoie l'instance canonique.

**Contrat equals complet** : réflexif (`a.equals(a)`), symétrique, transitif, cohérent dans le temps, et `a.equals(null)` est faux. Recette : tester `this == o`, puis le type (`getClass()` pour une égalité stricte, `instanceof` si on accepte les sous-classes, ce qui menace la symétrie), puis comparer les champs significatifs avec `Objects.equals`. `hashCode` : `Objects.hash(champ1, champ2)`. Les records, Lombok `@EqualsAndHashCode` et les IDE le génèrent.

**`var`** (Java 10) : inférence du type des variables **locales** uniquement (pas les champs, pas les paramètres de méthode, pas le type de retour). Le type reste statique et fixé à la compilation : `var` n'est pas du typage dynamique. Interdit sans initialiseur ou avec `null`.

**Les tableaux** sont des objets, de taille fixe, covariants (`Object[] o = new String[1]; o[0] = 1;` compile mais lève `ArrayStoreException`). `length` est un champ (pas une méthode), `Arrays.toString`, `Arrays.equals`, `Arrays.sort`, `Arrays.fill`, `Arrays.copyOf`. Un tableau `final` reste modifiable.

**Les structures de contrôle** : `switch` classique avec *fall-through* (sans `break`, on continue dans le cas suivant) vs `switch` en expression avec `->` (Java 14, pas de fall-through, peut retourner une valeur, `yield` dans un bloc). Labels pour sortir d'une boucle imbriquée : `outer: for (...) { for (...) { break outer; } }`. Le `switch` fonctionne sur int, char, String, enum, et depuis Java 21 sur n'importe quel type avec le pattern matching.

**varargs** : `void log(String... msgs)` reçoit un tableau ; un seul varargs par méthode, en dernière position.

### En situation

- **"Ce code compile mais se comporte mal"** → `==` sur Integer, ou equals sans hashCode.
- **"Un objet mis dans un HashSet devient introuvable"** → hashCode incohérent, ou champ mutable utilisé comme clé.
- **"Ce traitement de texte rame"** → concaténation en boucle → StringBuilder.
- **"Comment rendre une classe thread-safe simplement ?"** → l'immutabilité.

### Questions techniques

**Q : Combien de types primitifs ?** R : 8, byte, short, int, long, float, double, char, boolean.

**Q : `==` sur primitifs vs objets ?** R : Valeurs pour les primitifs, références pour les objets.

**Q : Explique le cache Integer.** R : Java cache les Integer de -128 à 127 ; dans cette plage `==` est true, au-delà false.

**Q : Pourquoi String est immutable ?** R : Sécurité, thread-safety, et déduplication via le String Pool.

**Q : Le contrat equals/hashCode ?** R : Si equals est vrai, hashCode doit être égal, sinon les collections hash perdent les objets.

**Q : Comment fait-on du package-private ?** R : En ne mettant aucun modificateur.

**Q : Java passe par valeur ou référence ?** R : Toujours par valeur ; pour un objet, c'est une copie de la référence.

**Q : `final List` rend-il immutable ?** R : Non, on peut toujours `add`. Final fige la référence.

**Q : final / finally / finalize ?** R : Immutabilité / bloc toujours exécuté / méthode pré-GC dépréciée.

**Q : JDK vs JRE vs JVM ?** R : JVM = exécute le bytecode ; JRE = JVM + bibliothèques standard ; JDK = JRE + outils de développement (javac, jar, jcmd...).

**Q : Java est-il compilé ou interprété ?** R : Les deux : compilé en bytecode par javac, puis interprété et compilé à la volée en code natif par le JIT de la JVM.

**Q : Pourquoi Java est-il portable ?** R : Le bytecode est indépendant de la plateforme ; seule la JVM est spécifique à chaque OS.

**Q : Taille d'un int, d'un long, d'un char ?** R : 32 bits, 64 bits, 16 bits (unité UTF-16).

**Q : Valeur par défaut d'une variable locale ?** R : Aucune : il faut l'initialiser, sinon erreur de compilation. Les champs, eux, ont une valeur par défaut (0, false, null).

**Q : `&&` vs `&` ?** R : `&&` court-circuite ; `&` évalue toujours les deux opérandes (et sert aussi d'opérateur bit à bit).

**Q : `i++` vs `++i` ?** R : Post-incrément (retourne l'ancienne valeur) vs pré-incrément (retourne la nouvelle).

**Q : `>>` vs `>>>` ?** R : Décalage arithmétique (conserve le signe) vs logique (remplit de zéros).

**Q : Pourquoi `byte b = 1; b = b + 1;` ne compile pas mais `b += 1` oui ?** R : `b + 1` est promu en int ; l'affectation composée contient un cast implicite.

**Q : Que fait `Integer.valueOf` par rapport à `new Integer` ?** R : valueOf utilise le cache (-128 à 127) ; le constructeur est déprécié pour suppression.

**Q : Quand l'autoboxing provoque-t-il une NPE ?** R : À l'unboxing d'un wrapper null, par exemple `int x = map.get(cle)` quand la clé est absente.

**Q : Où se trouve le String Pool ?** R : Dans le heap depuis Java 7.

**Q : Que fait `intern()` ?** R : Renvoie l'instance canonique de la chaîne dans le pool, en l'y ajoutant si besoin.

**Q : Les 5 propriétés du contrat equals ?** R : Réflexivité, symétrie, transitivité, cohérence, et `equals(null)` faux.

**Q : `getClass()` ou `instanceof` dans equals ?** R : getClass garantit la symétrie entre classe et sous-classe ; instanceof est plus souple mais peut casser la symétrie si une sous-classe ajoute des champs.

**Q : `var` rend-il Java dynamiquement typé ?** R : Non, le type est inféré et figé à la compilation ; uniquement pour les variables locales.

**Q : Que se passe-t-il avec `Object[] o = new String[1]; o[0] = 1;` ?** R : Ça compile (tableaux covariants) mais lève ArrayStoreException à l'exécution.

**Q : Comment comparer le contenu de deux tableaux ?** R : `Arrays.equals` (ou `Arrays.deepEquals` pour des tableaux imbriqués) ; `==` et `equals` comparent les références.

**Q : Switch classique vs switch expression ?** R : Le classique a du fall-through et nécessite des `break` ; l'expression (`->`) n'en a pas, retourne une valeur et doit être exhaustive.

**Q : Sur quels types peut-on faire un switch ?** R : Historiquement int (et types plus petits), char, String, enum, wrappers ; depuis Java 21, tout type via le pattern matching.

**Q : Comment sortir de deux boucles imbriquées d'un coup ?** R : Avec un `break` étiqueté (`break outer;`), ou en extrayant le code dans une méthode avec un `return`.

**Q : Que sont les varargs ?** R : Un nombre variable d'arguments reçus sous forme de tableau ; un seul par méthode, en dernière position.

**Q : Peut-on surcharger `main` ?** R : Oui, mais la JVM n'appelle que la signature standard (ou, depuis Java 25, la méthode main d'instance selon les règles de JEP 512).

**Q : Java a-t-il des pointeurs ?** R : Pas d'arithmétique de pointeurs ; il a des références, gérées par la JVM.

**Q : Qu'est-ce qu'un littéral `1_000_000` ?** R : Les underscores dans les littéraux numériques (Java 7) améliorent la lisibilité sans effet sur la valeur.

### Questions de situation

**Q : "Deux objets identiques sont considérés différents dans une HashMap. Comment tu investigues ?"**
R : "Je vérifierais d'abord que `equals` et `hashCode` sont redéfinis **tous les deux** et de façon **cohérente** : sur les mêmes champs. Si equals compare l'email mais que hashCode se base sur l'id, deux objets 'égaux' tombent dans des buckets différents et ne se rencontrent jamais.
Je vérifierais ensuite que les champs utilisés sont **immutables** : si on modifie un champ après insertion, le hashCode change, l'objet reste physiquement dans l'ancien bucket, et il devient introuvable même par lui-même. C'est pour ça qu'une clé de map doit toujours être immutable."

**Q : "Comment tu conçois une classe thread-safe sans synchronisation ?"**
R : "L'**immutabilité** : une classe immutable est thread-safe par construction, sans verrou, donc sans contention ni risque de deadlock.
Concrètement : tous les champs `final`, aucun setter, initialisation complète dans le constructeur, classe `final` pour empêcher qu'une sous-classe casse l'invariant, et **copies défensives** si un champ est une collection ou un objet mutable, sinon l'appelant garde une référence sur l'intérieur et peut le modifier.
C'est l'approche de `String`, de `Integer` et des records. Plus simple et plus performant que de synchroniser."

**Q : "Un `Map<String, Integer>` provoque une NullPointerException sur une ligne qui ne contient aucun appel de méthode visible."**
R : "Je soupçonnerais un **unboxing implicite** : `int total = compteurs.get(cle);` déballe le résultat, et si la clé est absente la map renvoie `null`, d'où la NPE. Depuis Java 14, le message de la NPE est explicite (*helpful NullPointerException*) et dit précisément ce qui était null.
La correction : `compteurs.getOrDefault(cle, 0)`, ou `merge(cle, 1, Integer::sum)` pour incrémenter. Plus généralement, je reste attentif aux mélanges primitifs/wrappers dans les opérateurs ternaires et les comparaisons."

**Q : "Explique ce qui se passe entre le moment où tu tapes `java -jar app.jar` et l'exécution de ton code."**
R : "Le lanceur démarre la JVM, lit le manifest du jar pour trouver la classe principale, puis le **class loader** charge cette classe : lecture du bytecode, vérification, préparation des champs statiques, et initialisation (exécution des blocs static).
Le code commence ensuite à s'exécuter en mode **interprété**. La JVM compte les appels et les boucles, et les méthodes chaudes sont compilées en code natif par le **JIT** (d'abord C1, rapide à compiler, puis C2, plus optimisé). C'est pour ça qu'une application Java a une phase de *warm-up* : ses performances s'améliorent pendant les premières minutes.
Parallèlement, le GC gère le heap et les threads système (GC, JIT, finalisation) tournent à côté du thread main."

═══════════════════════════════════════════
# 2. JAVA : CLASSES, OBJETS & LANGAGE AVANCÉ
═══════════════════════════════════════════

### Vue d'ensemble

Ce chapitre regroupe tout ce qui concerne la **mécanique des classes** : comment un objet est construit et initialisé, ce que contient la classe `Object` dont tout hérite, les différentes sortes de classes (enums, classes imbriquées, anonymes), et les outils "méta" du langage (annotations, réflexion, proxys dynamiques).

Ces sujets reviennent souvent sous forme de questions pièges ("dans quel ordre s'exécutent ces blocs ?", "qu'affiche ce constructeur ?"), mais ils ont aussi une portée pratique : la réflexion et les proxys sont exactement ce que Spring, Hibernate et Jackson utilisent en coulisses. Comprendre ces mécanismes, c'est comprendre pourquoi les frameworks se comportent comme ils le font.

### Cours

**La classe `Object`** : racine de toutes les classes. Ses méthodes : `equals`, `hashCode`, `toString` (par défaut `NomClasse@hashHexa`), `getClass`, `clone` (protected), `wait`/`notify`/`notifyAll` (coordination entre threads), `finalize` (dépréciée pour suppression depuis Java 18, JEP 421). Le `hashCode` par défaut est un *identity hash*, sans lien avec le contenu.

**Constructeurs.**
- Si aucune classe ne déclare de constructeur, le compilateur ajoute un constructeur par défaut sans argument. Dès qu'on en déclare un, ce constructeur implicite disparaît.
- `this(...)` appelle un autre constructeur de la même classe ; `super(...)` celui du parent. Si on n'écrit rien, le compilateur insère `super()`, ce qui échoue si le parent n'a pas de constructeur sans argument.
- Historiquement `this(...)`/`super(...)` devait être la première instruction. Depuis Java 25 (JEP 513, *flexible constructor bodies*), on peut placer avant des instructions qui n'utilisent pas `this` (validation d'arguments, calculs).
- Les constructeurs ne s'héritent pas. Un constructeur `private` sert aux classes utilitaires, aux singletons et aux fabriques statiques.

**Ordre d'initialisation** (question piège classique) :
1. Au chargement de la classe : champs et blocs `static` du parent, puis ceux de l'enfant (une seule fois).
2. À chaque `new` : champs d'instance et blocs d'instance du parent, puis corps du constructeur du parent ; ensuite champs et blocs d'instance de l'enfant, puis corps du constructeur de l'enfant.

Piège associé : appeler une méthode redéfinissable dans un constructeur. La version de l'enfant s'exécute alors que ses champs ne sont pas encore initialisés (ils valent null ou 0).

**Redéfinition (override), les règles exactes** : même nom et mêmes paramètres ; type de retour identique ou **covariant** (sous-type) ; visibilité égale ou plus large ; pas de nouvelle exception checked plus large. On ne redéfinit pas une méthode `private` (invisible), `static` (elle est **masquée**, résolue selon le type déclaré) ou `final`. `@Override` fait vérifier tout ça par le compilateur. Les **champs** ne sont jamais polymorphes : `parent.champ` lit le champ du type déclaré.

**Résolution de la surcharge** : le compilateur choisit la méthode la plus spécifique, en essayant dans l'ordre : correspondance exacte, puis élargissement de primitif (`int` → `long`), puis boxing (`int` → `Integer`), puis varargs. Avec `f(Object)` et `f(String)`, l'appel `f(null)` choisit `f(String)` ; avec `f(String)` et `f(Integer)`, `f(null)` est ambigu et ne compile pas.

**Enums.** Une enum est une classe dont les instances sont fixées à la compilation. Elle peut avoir des champs, un constructeur (implicitement privé), des méthodes, implémenter des interfaces, et même une implémentation de méthode par constante :
```java
enum Operation {
    PLUS("+") { int apply(int a, int b) { return a + b; } },
    MOINS("-") { int apply(int a, int b) { return a - b; } };
    private final String symbole;
    Operation(String s) { this.symbole = s; }
    abstract int apply(int a, int b);
}
```
Méthodes fournies : `values()`, `valueOf(String)` (lève IllegalArgumentException si le nom n'existe pas), `name()`, `ordinal()`. Ne jamais persister l'`ordinal()` (réordonner les constantes corrompt les données) : en JPA, `@Enumerated(EnumType.STRING)`. Une enum hérite implicitement de `java.lang.Enum`, donc ne peut pas étendre une autre classe. Comparaison sûre avec `==`. Collections dédiées et très efficaces : `EnumSet` (vecteur de bits) et `EnumMap` (tableau indexé par l'ordinal).

**Classes imbriquées** :
| Type | Accès à l'instance englobante | Usage typique |
|---|---|---|
| Classe imbriquée `static` | non | Builder, classe auxiliaire, regroupement logique |
| Classe interne (non static) | oui, référence implicite `Outer.this` | itérateurs, vues sur l'objet englobant |
| Classe locale | oui + variables locales effectivement finales | logique ponctuelle dans une méthode |
| Classe anonyme | idem | implémentation ponctuelle (avant les lambdas) |

Une classe interne retient une référence vers son instance englobante, source de fuites mémoire (par exemple un listener interne enregistré dans un registre global). D'où la règle : rendre la classe imbriquée `static` sauf si on a vraiment besoin de l'instance englobante. On instancie une classe interne avec `outer.new Inner()`.

**Lambda vs classe anonyme** : une lambda n'implémente qu'une interface fonctionnelle, n'a pas d'état propre, et `this` y désigne l'instance englobante ; dans une classe anonyme, `this` désigne l'objet anonyme. Une classe anonyme produit un fichier `.class` dédié ; une lambda est liée à l'exécution via `invokedynamic`. Les deux ne capturent que des variables locales **effectivement finales**.

**Évolution des interfaces** : constantes implicitement `public static final` ; Java 8 ajoute les méthodes `default` et `static` (les méthodes statiques d'interface ne s'héritent pas, on les appelle via le nom de l'interface) ; Java 9 ajoute les méthodes `private` pour factoriser du code entre méthodes default. Les **interfaces marqueurs** (`Serializable`, `Cloneable`, `RandomAccess`) n'ont aucune méthode et signalent une capacité ; les annotations jouent aujourd'hui souvent ce rôle.

**clone()** : nécessite `Cloneable`, sinon `CloneNotSupportedException`. Par défaut, copie **superficielle** (les références sont partagées). Mécanisme jugé mal conçu (pas d'appel de constructeur, exception checked, cast). On préfère un **constructeur de copie** ou une méthode de fabrique, et une copie profonde explicite si nécessaire.

**Annotations.** Des métadonnées attachées au code. Une annotation se déclare avec `@interface` et des méta-annotations :
- `@Retention` : `SOURCE` (disparaît à la compilation, ex. `@Override`), `CLASS` (dans le bytecode mais invisible à l'exécution, défaut), `RUNTIME` (lisible par réflexion, ce qu'utilisent Spring et JPA).
- `@Target` : où elle peut se poser (TYPE, METHOD, FIELD, PARAMETER...).
- `@Inherited`, `@Repeatable`, `@Documented`.

Annotations standards : `@Override`, `@Deprecated(since, forRemoval)`, `@SuppressWarnings`, `@FunctionalInterface`, `@SafeVarargs`. Deux façons de les exploiter : à l'**exécution** par réflexion (Spring, JUnit, Jackson), ou à la **compilation** par un *annotation processor* qui génère du code (Lombok, MapStruct, Dagger, le métamodèle JPA). Le second ne coûte rien à l'exécution et fonctionne bien avec la compilation native.

**Réflexion** : inspecter et manipuler les classes à l'exécution. `Class.forName("...")`, `getDeclaredFields()`, `getDeclaredMethods()`, `getDeclaredConstructor().newInstance()`, `method.invoke(obj, args)`, `field.setAccessible(true)`. Usage : frameworks d'injection, ORM, sérialisation, tests. Coûts : performance (moins optimisable par le JIT), perte de sécurité de type, fragilité aux renommages, et incompatibilité partielle avec GraalVM natif (il faut déclarer ce qui est réfléchi). Depuis Java 16/17, l'**encapsulation forte** du JDK interdit par défaut l'accès réflexif aux internes du JDK (`--add-opens` pour forcer). `MethodHandle` (Java 7) est une alternative plus rapide et typée.

**Proxys dynamiques** : `java.lang.reflect.Proxy` crée à l'exécution un objet implémentant une ou des **interfaces**, dont chaque appel passe par un `InvocationHandler`. Pour proxifier une **classe** (sans interface), il faut générer une sous-classe par manipulation de bytecode : CGLIB ou ByteBuddy. C'est exactement ce que fait Spring pour `@Transactional` : proxy JDK si le bean est utilisé via une interface (option), proxy CGLIB sinon (défaut de Spring Boot). Conséquence : les méthodes `final` ou `private` ne peuvent pas être interceptées par un proxy CGLIB.

**Lombok et MapStruct.** Lombok génère getters, setters, constructeurs, builders, equals/hashCode par annotation processor (`@Getter`, `@RequiredArgsConstructor`, `@Builder`, `@Value` pour une classe immutable, `@Slf4j`). Piège : `@Data` sur une entité JPA génère un equals/hashCode sur tous les champs, y compris les relations lazy, et un `toString` qui peut déclencher des chargements en cascade ou des récursions infinies. MapStruct génère à la compilation le code de mapping entre entités et DTOs, sans réflexion.

**Upcasting et downcasting** : l'upcast (enfant vers parent) est implicite et toujours sûr ; le downcast est explicite et peut lever `ClassCastException`. Depuis Java 16, `if (obj instanceof Chien c)` combine test et cast. Un recours fréquent à `instanceof` sur ses propres types est souvent le signe qu'il manque du polymorphisme (ou qu'il faut une hiérarchie sealed avec un switch exhaustif).

**Règles des fichiers sources** : une classe de premier niveau est `public` ou package-private (jamais `private` ni `protected`) ; au plus une classe `public` par fichier, et elle porte le nom du fichier.

### En situation

- **"Ce constructeur affiche null alors que le champ est initialisé"** → méthode redéfinie appelée depuis le constructeur du parent.
- **"Fuite mémoire avec des listeners"** → classe interne non static qui retient l'objet englobant.
- **"Les valeurs d'enum en base sont décalées après une modification"** → ordinal persisté au lieu du nom.
- **"Pourquoi @Transactional ne marche pas sur une méthode final ?"** → le proxy CGLIB ne peut pas la redéfinir.
- **"Comment un framework trouve-t-il mes classes annotées ?"** → scan du classpath + réflexion, ou indexation au build.

### Questions techniques

**Q : Quelles sont les méthodes de la classe Object ?** R : equals, hashCode, toString, getClass, clone, wait, notify, notifyAll, finalize (dépréciée).

**Q : Que renvoie toString par défaut ?** R : Le nom de la classe, '@', puis le hash en hexadécimal.

**Q : Quand le compilateur ajoute-t-il un constructeur par défaut ?** R : Uniquement si la classe ne déclare aucun constructeur.

**Q : Que se passe-t-il si le parent n'a pas de constructeur sans argument ?** R : L'enfant doit appeler explicitement `super(args)`, sinon erreur de compilation.

**Q : Les constructeurs s'héritent-ils ?** R : Non.

**Q : Ordre d'initialisation lors d'un `new Enfant()` ?** R : Static parent, static enfant (au premier chargement), puis instance parent + constructeur parent, puis instance enfant + constructeur enfant.

**Q : Pourquoi ne pas appeler une méthode redéfinissable dans un constructeur ?** R : La version de la sous-classe s'exécute avant l'initialisation de ses champs.

**Q : Peut-on redéfinir une méthode static ?** R : Non, elle est masquée : l'appel est résolu selon le type déclaré.

**Q : Peut-on redéfinir une méthode private ?** R : Non, elle n'est pas visible ; une méthode de même signature dans l'enfant est une méthode indépendante.

**Q : C'est quoi un type de retour covariant ?** R : Une redéfinition peut retourner un sous-type du type de retour du parent.

**Q : Peut-on réduire la visibilité dans une redéfinition ?** R : Non, seulement l'élargir.

**Q : Les champs sont-ils polymorphes ?** R : Non, leur accès dépend du type déclaré.

**Q : Ordre de résolution de la surcharge ?** R : Exact, élargissement, boxing, varargs.

**Q : Une enum peut-elle avoir un constructeur public ?** R : Non, il est implicitement privé.

**Q : Une enum peut-elle hériter d'une classe ?** R : Non (elle étend déjà Enum) ; elle peut implémenter des interfaces.

**Q : Pourquoi ne pas persister ordinal() ?** R : Réordonner ou insérer une constante change les ordinaux et corrompt les données existantes.

**Q : EnumMap et EnumSet, pourquoi ?** R : Implémentations compactes et très rapides basées sur l'ordinal (tableau, vecteur de bits).

**Q : Classe imbriquée static vs classe interne ?** R : La classe interne détient une référence vers l'instance englobante ; la static non.

**Q : Différence entre `this` dans une lambda et dans une classe anonyme ?** R : Lambda : l'instance englobante ; anonyme : l'objet anonyme lui-même.

**Q : Pourquoi une variable capturée doit-elle être effectivement finale ?** R : La lambda capture une copie de la valeur ; autoriser la modification créerait une incohérence (et des problèmes de concurrence).

**Q : Une interface peut-elle avoir des méthodes private ?** R : Oui depuis Java 9, pour factoriser du code entre méthodes default.

**Q : Les méthodes static d'interface s'héritent-elles ?** R : Non, on les appelle via le nom de l'interface.

**Q : Copie superficielle vs profonde ?** R : Superficielle : les références internes sont partagées ; profonde : les objets référencés sont aussi copiés.

**Q : Pourquoi éviter clone() ?** R : Pas d'appel de constructeur, copie superficielle, exception checked, cast ; on préfère un constructeur de copie.

**Q : Les trois RetentionPolicy ?** R : SOURCE, CLASS (défaut), RUNTIME.

**Q : Annotation processor vs réflexion ?** R : Le processor travaille à la compilation et génère du code (zéro coût runtime) ; la réflexion lit les annotations à l'exécution.

**Q : Inconvénients de la réflexion ?** R : Performance, perte de typage, fragilité, encapsulation forte du JDK, configuration nécessaire en natif.

**Q : Proxy JDK vs CGLIB ?** R : Le proxy JDK implémente des interfaces ; CGLIB génère une sous-classe, donc ne peut pas intercepter les méthodes final ou private.

**Q : Pourquoi éviter @Data de Lombok sur une entité JPA ?** R : equals/hashCode/toString sur toutes les relations : chargements lazy déclenchés, récursions, hashCode instable.

**Q : Peut-on déclarer une classe de premier niveau private ?** R : Non : public ou package-private uniquement.

**Q : Qu'est-ce qu'une interface marqueur ?** R : Une interface sans méthode qui signale une capacité (Serializable, Cloneable).

**Q : Peut-on instancier une classe abstraite ?** R : Non, mais on peut créer une sous-classe anonyme : `new Abstraite() { ... }`.

### Questions de situation

**Q : "Explique comment Spring peut ajouter une transaction autour de ta méthode sans que tu écrives de code."**
R : "Au démarrage, Spring détecte l'annotation `@Transactional` sur le bean et, au lieu d'injecter l'objet réel, il injecte un **proxy**. Avec Spring Boot c'est par défaut un proxy CGLIB : une sous-classe générée à l'exécution qui redéfinit les méthodes publiques.
Quand j'appelle la méthode, c'est le proxy qui reçoit l'appel : il ouvre la transaction, délègue à l'objet réel, puis commit ou rollback selon le résultat.
Ça explique les trois limites connues : un appel interne via `this` ne passe pas par le proxy ; une méthode `final` ne peut pas être redéfinie par la sous-classe générée ; une méthode `private` n'est pas visible. Le même mécanisme sert à `@Cacheable`, `@Async` et `@PreAuthorize`."

**Q : "Tu dois modéliser des types d'opérations dont chacun a un comportement différent. Enum, hiérarchie de classes ou sealed ?"**
R : "Si la liste est **fermée** et que les opérations sont des **singletons sans état variable**, une enum avec une implémentation par constante est idéale : compacte, sérialisable, utilisable dans un switch, avec EnumMap/EnumSet.
Si chaque cas porte des **données différentes** par instance, une interface **sealed** avec des records : le compilateur garantit l'exhaustivité des switch.
Et si la liste doit rester **ouverte** à des extensions (plugins, autres modules), une interface classique avec des implémentations injectées, éventuellement découvertes par `ServiceLoader` ou par le conteneur d'injection."

**Q : "Une classe utilitaire est instanciée partout alors qu'elle n'a que des méthodes static."**
R : "Je lui donnerais un constructeur `private` (éventuellement qui lève une exception pour bloquer aussi la réflexion) et je la déclarerais `final`. Ça documente l'intention et empêche l'instanciation et l'héritage.
Mais je me demanderais aussi si ces méthodes statiques ne gênent pas les tests : une méthode statique ne se mocke pas facilement. Si elle dépend d'un état externe (horloge, configuration, réseau), mieux vaut en faire un bean injecté."

═══════════════════════════════════════════
# 3. JAVA : CHAÎNES, NOMBRES, DATES & TEXTE
═══════════════════════════════════════════

### Vue d'ensemble

Manipuler du texte, des nombres et des dates paraît trivial, et c'est précisément pour ça que ces sujets produisent tant de bugs : une facture fausse d'un centime parce qu'on a calculé en `double`, un rendez-vous décalé d'une heure au changement d'heure, un `split` qui "perd" des colonnes vides, un affichage cassé parce que l'encodage n'a pas été précisé.

Les recruteurs adorent ces questions parce qu'elles séparent ceux qui connaissent les pièges de ceux qui les découvriront en production. Les trois messages à retenir : **jamais de `double` pour de l'argent**, **toujours `java.time` et jamais `Date`/`Calendar`**, **toujours préciser l'encodage**.

### Cours

**API String essentielle** : `length()`, `charAt(i)`, `substring(debut, fin)` (fin exclue), `indexOf`, `lastIndexOf`, `contains`, `startsWith`/`endsWith`, `equalsIgnoreCase`, `compareTo` (ordre lexicographique), `toUpperCase`/`toLowerCase`, `replace` (littéral) vs `replaceAll` (expression régulière), `split(regex)`, `String.join(", ", liste)`, `String.format` / `"..".formatted(...)` (Java 15), `chars()` (IntStream), `toCharArray()`.

Ajouts Java 11 : `isBlank()`, `strip()`/`stripLeading()`/`stripTrailing()` (conscients de l'Unicode, contrairement à `trim()` qui ne retire que les caractères ≤ espace), `lines()`, `repeat(n)`. Java 12 : `indent`, `transform`. Java 15 : text blocks `"""`.

**Pièges de split** : l'argument est une **regex** (`"a.b".split(".")` renvoie un tableau vide, il faut `split("\\.")` ou `Pattern.quote`). Les chaînes vides **finales** sont supprimées : `"a,b,,".split(",")` a 2 éléments, alors que `"a,,b".split(",")` en a 3. Pour les conserver : `split(",", -1)`.

**Concaténation** : depuis Java 9, `+` est compilé via `invokedynamic` (`StringConcatFactory`), efficace pour une expression isolée. Le problème O(n²) reste valable **dans une boucle**, d'où `StringBuilder`. `StringBuilder` a `append`, `insert`, `delete`, `reverse`, `setLength(0)` pour réutiliser.

**Compact Strings** (Java 9) : une String stocke ses caractères dans un `byte[]` en Latin-1 (1 octet par caractère) quand c'est possible, sinon en UTF-16 (2 octets). Gain mémoire important sur la plupart des applications.

**Unicode** : un `char` est une unité UTF-16, pas forcément un caractère complet. Les caractères hors du plan de base (beaucoup d'emojis) occupent **deux** `char` (paire de substitution) : `"😀".length()` vaut 2. Pour compter les vrais caractères : `codePointCount`, ou `codePoints()`. `toUpperCase()` sans `Locale` dépend de la locale par défaut (en turc, `"i".toUpperCase()` donne `İ`) : utiliser `toUpperCase(Locale.ROOT)` pour des traitements techniques.

**Encodage** : toujours expliciter le charset (`new String(bytes, StandardCharsets.UTF_8)`, `Files.readString(path, UTF_8)`). Depuis Java 18 (JEP 400), UTF-8 est le charset par défaut sur toutes les plateformes, mais expliciter reste la bonne pratique pour la lisibilité et la portabilité.

**Switch sur String** : compilé via `hashCode()` puis `equals()`. `"Aa"` et `"BB"` ont le même hashCode (2112), exemple classique de collision.

**String templates** : proposés en preview en Java 21 et 22, puis **retirés** en Java 23 pour être repensés. À citer si on vous en parle, sans prétendre qu'ils existent dans le langage.

**Expressions régulières** : `Pattern.compile` est coûteux et le `Pattern` est thread-safe, donc on le compile une fois (constante static) ; le `Matcher` n'est pas thread-safe. Groupes `(...)`, groupes nommés `(?<nom>...)`, quantificateurs gourmands (`.*`) vs réticents (`.*?`). Danger : le **backtracking catastrophique** (ReDoS), une regex mal écrite comme `(a+)+$` peut prendre un temps exponentiel sur une entrée malveillante.

**Nombres entiers** :
- Le **débordement** est silencieux : `Integer.MAX_VALUE + 1 == Integer.MIN_VALUE`. Pour le détecter : `Math.addExact`, `multiplyExact`, `toIntExact` (lèvent ArithmeticException).
- `Math.abs(Integer.MIN_VALUE)` est négatif (pas de valeur positive représentable).
- Division entière tronquée vers zéro (`7 / 2 == 3`, `-7 / 2 == -3`) ; le signe du modulo suit le dividende (`-7 % 3 == -1`) ; `Math.floorMod` donne un résultat toujours positif pour un diviseur positif.
- Division entière par zéro : `ArithmeticException`.
- Littéraux : `0x1F` (hexa), `0b1010` (binaire), `017` (octal, piège : `017 == 15`), `1_000_000`.

**Nombres flottants (IEEE 754)** : `0.1 + 0.2 != 0.3` parce que 0.1 n'a pas de représentation binaire exacte. Division par zéro : `Infinity` ou `NaN`, pas d'exception. `NaN != NaN` (tester avec `Double.isNaN`). `Double.compare` et `Double.equals` traitent NaN comme égal à lui-même et distinguent 0.0 de -0.0. `Math.round(-2.5) == -2` et `Math.round(2.5) == 3` (arrondi vers +∞ à .5).

**BigDecimal** pour l'argent et tout calcul décimal exact :
- Construire depuis une **String** ou `BigDecimal.valueOf(double)`, jamais `new BigDecimal(0.1)` qui capture l'imprécision binaire (0.1000000000000000055511151231257827...).
- Immutable : `a.add(b)` retourne un nouvel objet, il faut récupérer le résultat.
- `equals` compare **valeur et échelle** (`2.0` ≠ `2.00`) ; `compareTo` compare la valeur seule. Donc attention en clé de HashMap ou dans un HashSet.
- `divide` sans arrondi lève ArithmeticException si le résultat est infini (`1/3`) : préciser `divide(b, 2, RoundingMode.HALF_UP)`. `RoundingMode.HALF_EVEN` (arrondi bancaire) minimise le biais cumulé.
- Alternative : stocker les montants en **centimes** dans un `long`.
- `BigInteger` pour les entiers arbitrairement grands (cryptographie, factorielles).

**Aléatoire** : `Random` (prévisible, partagé entre threads avec contention), `ThreadLocalRandom.current()` (performant en concurrence), `SecureRandom` (cryptographiquement sûr : jetons, mots de passe, sels). Depuis Java 17, l'interface `RandomGenerator` unifie les générateurs.

**Dates : pourquoi `java.time`** (Java 8, JSR 310). `java.util.Date` et `Calendar` sont mutables, non thread-safe, avec des mois indexés à partir de 0 et une API confuse. `SimpleDateFormat` n'est **pas thread-safe** : partagé en champ static, il produit des dates corrompues sous charge. `java.time` est immutable, thread-safe et explicite.

| Type | Représente | Exemple |
|---|---|---|
| `LocalDate` | une date sans heure ni fuseau | une date de naissance |
| `LocalTime` | une heure sans date | l'heure d'ouverture d'un magasin |
| `LocalDateTime` | date + heure sans fuseau | une heure "murale", ambiguë |
| `ZonedDateTime` | date + heure + fuseau (ZoneId, règles DST) | un rendez-vous à Paris |
| `OffsetDateTime` | date + heure + décalage fixe (+02:00) | un horodatage d'API (ISO 8601) |
| `Instant` | un point absolu sur la ligne du temps (UTC) | un timestamp technique |
| `Duration` | une durée en secondes/nanos | un timeout |
| `Period` | une durée en années/mois/jours | "dans 3 mois" |

Règles pratiques : **stocker et échanger en UTC** (`Instant`, ou `timestamptz` en PostgreSQL), convertir dans le fuseau de l'utilisateur seulement à l'affichage ; formater avec `DateTimeFormatter` (immutable, thread-safe) ; calculer un écart avec `ChronoUnit.DAYS.between(a, b)` ; ajuster avec `TemporalAdjusters` (premier lundi du mois). Le **changement d'heure** crée des heures inexistantes (au printemps) et des heures doublées (à l'automne) : `ZonedDateTime` les gère, `LocalDateTime` non. `Duration.ofDays(1)` vaut toujours 24 h, alors que `Period.ofDays(1)` ajoute un jour calendaire (23 ou 25 h un jour de changement d'heure).

**Testabilité** : injecter un `java.time.Clock` plutôt qu'appeler `LocalDate.now()` en dur ; en test, `Clock.fixed(...)` rend le code déterministe.

**Formatage des nombres** : `NumberFormat`/`DecimalFormat` dépendent de la locale (virgule ou point décimal) et ne sont pas thread-safe.

### En situation

- **"Une facture est fausse d'un centime"** → calcul en double → BigDecimal ou centimes en long.
- **"Des dates aléatoirement incohérentes sous charge"** → SimpleDateFormat partagé entre threads.
- **"Un rendez-vous est décalé d'une heure"** → LocalDateTime sans fuseau, ou conversion de fuseau oubliée.
- **"Le parsing CSV perd les colonnes vides de fin"** → split sans limite négative.
- **"Des caractères accentués deviennent illisibles"** → encodage implicite.
- **"Un compteur devient négatif"** → débordement d'int.

### Questions techniques

**Q : trim() vs strip() ?** R : trim retire les caractères ≤ U+0020 ; strip retire les espaces Unicode (Java 11).

**Q : isEmpty() vs isBlank() ?** R : isEmpty : longueur 0 ; isBlank : vide ou uniquement des espaces.

**Q : replace vs replaceAll ?** R : replace remplace un littéral ; replaceAll interprète le premier argument comme une regex.

**Q : Pourquoi `"a.b".split(".")` renvoie un tableau vide ?** R : Le point est un métacaractère regex qui correspond à tout ; il faut l'échapper.

**Q : Que renvoie `"a,b,,".split(",").length` ?** R : 2, les chaînes vides finales sont supprimées (sauf avec une limite négative).

**Q : Le `+` sur les String est-il toujours à éviter ?** R : Non, hors boucle il est optimisé (invokedynamic depuis Java 9) ; c'est en boucle qu'il faut StringBuilder.

**Q : C'est quoi les Compact Strings ?** R : Depuis Java 9, une String Latin-1 est stockée sur 1 octet par caractère au lieu de 2.

**Q : Pourquoi `"😀".length()` vaut 2 ?** R : Le caractère est hors du plan de base et occupe une paire de substitution UTF-16.

**Q : Quel est l'encodage par défaut depuis Java 18 ?** R : UTF-8 sur toutes les plateformes (JEP 400).

**Q : Pattern et Matcher sont-ils thread-safe ?** R : Pattern oui, Matcher non.

**Q : C'est quoi un ReDoS ?** R : Un déni de service causé par une regex au backtracking exponentiel sur une entrée malveillante.

**Q : Que vaut `Integer.MAX_VALUE + 1` ?** R : Integer.MIN_VALUE : le débordement est silencieux.

**Q : Comment détecter un débordement ?** R : Math.addExact, multiplyExact, toIntExact, qui lèvent ArithmeticException.

**Q : Que vaut `-7 % 3` ?** R : -1, le signe suit le dividende ; Math.floorMod(-7, 3) vaut 2.

**Q : `10 / 0` vs `10.0 / 0` ?** R : ArithmeticException vs Infinity.

**Q : Pourquoi `0.1 + 0.2 != 0.3` ?** R : 0.1 et 0.2 n'ont pas de représentation binaire exacte en IEEE 754.

**Q : Comment tester qu'un double est NaN ?** R : Double.isNaN(x), car NaN != NaN.

**Q : Que vaut `Math.round(-2.5)` ?** R : -2 (arrondi vers +∞ pour les .5).

**Q : Pourquoi ne pas utiliser `new BigDecimal(0.1)` ?** R : Il capture la valeur binaire imprécise ; utiliser `new BigDecimal("0.1")` ou `BigDecimal.valueOf(0.1)`.

**Q : `equals` vs `compareTo` sur BigDecimal ?** R : equals compare valeur et échelle (2.0 ≠ 2.00) ; compareTo compare uniquement la valeur.

**Q : Que se passe-t-il avec `BigDecimal.ONE.divide(BigDecimal.valueOf(3))` ?** R : ArithmeticException (développement décimal infini) : il faut une échelle et un RoundingMode.

**Q : C'est quoi HALF_EVEN ?** R : L'arrondi bancaire : au plus proche, et en cas d'égalité vers le chiffre pair, pour éviter un biais cumulé.

**Q : Random vs ThreadLocalRandom vs SecureRandom ?** R : Générique / performant en concurrence / cryptographiquement sûr.

**Q : Pourquoi abandonner Date et Calendar ?** R : Mutables, non thread-safe, mois à partir de 0, API confuse.

**Q : SimpleDateFormat est-il thread-safe ?** R : Non ; DateTimeFormatter l'est.

**Q : LocalDateTime vs ZonedDateTime vs Instant ?** R : Sans fuseau / avec fuseau et règles de changement d'heure / point absolu en UTC.

**Q : Duration vs Period ?** R : Durée exacte en secondes/nanos vs durée calendaire en années/mois/jours.

**Q : Comment rendre testable du code qui utilise la date courante ?** R : Injecter un Clock et utiliser Clock.fixed en test.

**Q : Dans quel format stocker des dates en base ?** R : En UTC (Instant / timestamp with time zone), conversion au fuseau de l'utilisateur à l'affichage.

**Q : Pourquoi préciser une Locale dans toUpperCase ?** R : Le résultat dépend de la locale (cas du i turc) ; Locale.ROOT pour les traitements techniques.

### Questions de situation

**Q : "Tu dois calculer des montants avec TVA et remises. Comment tu t'y prends ?"**
R : "Jamais de `double` : l'erreur de représentation binaire s'accumule et produit des écarts d'un centime, inacceptables en comptabilité.
J'utiliserais `BigDecimal` construit depuis des String ou `valueOf`, avec une **règle d'arrondi explicite** et validée avec le métier : à quelle étape on arrondit (par ligne ou sur le total, ce qui ne donne pas le même résultat) et avec quel mode (HALF_UP est l'usage commercial, HALF_EVEN limite le biais statistique).
Je ferais attention à `equals` qui tient compte de l'échelle : pour comparer, `compareTo`. Et je stockerais en base dans un type décimal exact (`NUMERIC(19,4)` par exemple), pas un FLOAT.
Alternative pour un système simple : manipuler des centimes dans un `long`, plus rapide et sans ambiguïté, à condition de bien gérer les divisions."

**Q : "Des utilisateurs dans plusieurs pays réservent des créneaux. Comment tu gères les dates ?"**
R : "Je distinguerais l'**instant** de la **représentation**. En base et entre services, je stocke des instants en UTC (`Instant`, `timestamptz`) : c'est sans ambiguïté et comparable.
Le fuseau de l'utilisateur, ou celui du lieu du rendez-vous, est une donnée à part entière (un `ZoneId` comme `Europe/Paris`, pas un décalage fixe, parce que le décalage change avec l'heure d'été).
La conversion se fait à la frontière : à l'affichage et à la saisie, via `ZonedDateTime`. Pour un rendez-vous futur récurrent, il peut être plus juste de stocker l'heure locale + le ZoneId, car les règles de fuseau d'un pays peuvent changer entre la prise du rendez-vous et sa date.
Côté API, de l'ISO 8601 avec décalage (`2026-03-29T10:00:00+02:00`). Et en test, un `Clock` injecté pour couvrir les cas de changement d'heure."

**Q : "Un import CSV produit des colonnes décalées sur certaines lignes."**
R : "Je regarderais trois suspects. Le `split(",")` qui supprime les colonnes vides en fin de ligne, d'où un décalage : `split(",", -1)` les conserve. Les **valeurs contenant des virgules** entre guillemets, qu'un split naïf coupe en deux : il faut un vrai parseur CSV (OpenCSV, Apache Commons CSV, Jackson CSV). Et l'**encodage** : un BOM UTF-8 en tête de fichier ou un fichier en Windows-1252 lu en UTF-8 abîme la première colonne ou les accents.
La leçon générale : le CSV a l'air simple mais a une spécification (RFC 4180), et on ne le parse pas à la main."

═══════════════════════════════════════════
# 4. COLLECTIONS
═══════════════════════════════════════════

### Vue d'ensemble

Le framework Collections, c'est la boîte à outils standard pour stocker et manipuler des groupes d'objets. Son intérêt n'est pas seulement pratique : **choisir la bonne structure est souvent la décision de performance la plus importante** d'un programme. Passer d'une List à un HashSet pour une recherche fait tomber un traitement de O(n²) à O(n), un gain de plusieurs milliers sur de gros volumes, pour une ligne de code.

L'architecture repose sur des **interfaces** (List, Set, Map, Queue) qui décrivent un contrat, et des **implémentations** qui font des compromis différents entre vitesse d'accès, vitesse d'insertion, ordre et mémoire. Une question d'entretien sur les collections teste en réalité si tu sais raisonner en complexité et en pattern d'accès, pas si tu connais les noms de classes.

### Cours

**La hiérarchie.** `Collection` regroupe List, Set et Queue. `Map` est à part, elle ne l'étend pas, parce qu'elle stocke des paires et non des éléments.

- **List** : ordonnée, doublons autorisés, accès par index.
- **Set** : pas de doublons.
- **Queue/Deque** : file d'attente, ajout/retrait aux extrémités.
- **Map** : associations clé → valeur.

**ArrayList vs LinkedList.** ArrayList est un tableau redimensionnable : accès par index en O(1), insertion au milieu en O(n) (il faut décaler). Quand elle est pleine, elle alloue un tableau ~1,5× plus grand et recopie, d'où une insertion en fin "O(1) amortie". LinkedList est une liste doublement chaînée : insertion en O(1) *si on a déjà le nœud*, mais accès par index en O(n). **En pratique ArrayList gagne presque toujours** : la localité mémoire d'un tableau est bien meilleure pour le cache CPU, et LinkedList paie un surcoût mémoire par élément (deux pointeurs). Pour une file, `ArrayDeque` bat LinkedList.

**HashMap en interne** : la question la plus posée. C'est un tableau de **buckets**. À chaque `put` :
1. Calcul de `hashCode()` de la clé.
2. Application d'un hash secondaire (mélange les bits hauts, pour mieux répartir).
3. Index du bucket : `hash & (n-1)`, d'où l'exigence que `n` soit une **puissance de 2**, ce qui transforme un modulo coûteux en opération bit-à-bit.
4. Si collision, on parcourt le bucket et on compare avec `equals()`.
5. Au-delà du **load factor** (0.75), la table double de taille et tout est redistribué (**rehashing**), opération coûteuse.

Depuis Java 8, un bucket contenant plus de 8 éléments est converti en **arbre rouge-noir** (si la table compte au moins 64 cases, sinon elle est simplement agrandie), ce qui ramène le pire cas de O(n) à O(log n), une protection contre les attaques par collision de hash.

**Les variantes** suivent une logique constante : la version "Hash" est la plus rapide sans ordre, la version "Linked" conserve l'ordre d'insertion, la version "Tree" maintient un tri.

| | Non ordonné | Ordre d'insertion | Trié |
|---|---|---|---|
| **Map** | HashMap O(1) | LinkedHashMap | TreeMap O(log n) |
| **Set** | HashSet O(1) | LinkedHashSet | TreeSet O(log n) |

**Collections immuables** : `List.of()`, `Set.of()`, `Map.of()` depuis Java 9. Elles lèvent une exception à toute tentative de modification, utile pour exposer des données sans risque.

**Comparable vs Comparator.** Comparable définit l'**ordre naturel** dans la classe elle-même (`compareTo`). Comparator définit un ordre **externe**, et on peut en avoir plusieurs : `Comparator.comparing(User::getAge).thenComparing(User::getName)`.

**Fail-fast.** Les itérateurs classiques lèvent `ConcurrentModificationException` si la collection est modifiée pendant l'itération, c'est une protection, pas un bug. Pour supprimer pendant un parcours : `iterator.remove()` ou `removeIf()`.

**Collections concurrentes.** `ConcurrentHashMap` (verrouillage fin par bucket : CAS sur un bucket vide, `synchronized` sur son premier nœud sinon ; les segments n'existaient que jusqu'à Java 7), `CopyOnWriteArrayList` (copie à chaque écriture, idéale si beaucoup de lectures et peu d'écritures), `BlockingQueue` (producteur/consommateur).

**HashMap : chiffres et détails à connaître.** Capacité initiale 16, load factor 0.75 (donc redimensionnement au-delà de 12 éléments), capacité toujours puissance de 2. La conversion d'un bucket en arbre (seuil `TREEIFY_THRESHOLD` = 8) n'a lieu que si la table fait au moins 64 cases (`MIN_TREEIFY_CAPACITY`) ; sinon la table est simplement agrandie. Un arbre redevient liste en dessous de 6 éléments. Pour l'arbre, les clés gagnent à implémenter `Comparable`. HashMap n'est pas thread-safe : un accès concurrent non synchronisé peut perdre des entrées ou corrompre la structure. `HashSet` est implémenté par une `HashMap` dont les valeurs sont un objet factice.

**Méthodes modernes de Map** (Java 8) : `getOrDefault`, `putIfAbsent`, `computeIfAbsent(k, k -> new ArrayList<>())` (idéal pour les maps de listes), `computeIfPresent`, `compute`, `merge(k, 1, Integer::sum)` (idéal pour compter), `forEach`, `replaceAll`, `entrySet().removeIf(...)`. Itérer sur `entrySet()` plutôt que `keySet()` + `get()`, qui fait une recherche de trop par élément.

**ConcurrentHashMap en détail.** Jusqu'à Java 7 : des *segments* verrouillés indépendamment. Depuis Java 8 : plus de segments ; l'insertion dans un bucket vide se fait par **CAS**, sinon par `synchronized` sur le **premier nœud du bucket** ; les lectures sont sans verrou (champs volatile) ; la taille est maintenue par des compteurs répartis (comme `LongAdder`). Les opérations `compute`, `computeIfAbsent` et `merge` sont **atomiques** par clé, mais la fonction passée ne doit pas être longue ni modifier la map elle-même. `size()` est une estimation sous concurrence. Ni clé ni valeur `null` (sinon impossible de distinguer "absent" de "vaut null" sans verrou). Itérateurs **faiblement cohérents** : jamais de ConcurrentModificationException, mais ils peuvent ne pas refléter les modifications récentes.

**Synchronized vs concurrent.** `Collections.synchronizedMap(map)` verrouille chaque méthode sur un seul verrou : correct mais contentieux, et l'itération doit être synchronisée manuellement. `Hashtable` et `Vector` sont les ancêtres synchronisés, considérés comme legacy. Les collections de `java.util.concurrent` utilisent des verrous fins ou du lock-free.

**Fail-fast vs fail-safe / weakly consistent.** Les itérateurs d'ArrayList ou HashMap comparent un compteur de modifications (`modCount`) et lèvent ConcurrentModificationException, **même en mono-thread** si on modifie la collection pendant une boucle for-each. Les itérateurs de CopyOnWriteArrayList travaillent sur un instantané ; ceux de ConcurrentHashMap sont faiblement cohérents.

**Vues et immutabilité, les pièges.**
- `Arrays.asList(tab)` : liste de **taille fixe** adossée au tableau (`set` modifie le tableau, `add`/`remove` lèvent UnsupportedOperationException). Avec un `int[]`, on obtient une `List<int[]>` d'un seul élément.
- `List.of(...)`, `Set.of`, `Map.of` : vraiment immuables, **refusent null** (NPE), et `Set.of`/`Map.of` refusent les doublons (IllegalArgumentException). L'ordre d'itération de `Set.of`/`Map.of` est volontairement non spécifié.
- `Collections.unmodifiableList(l)` : une **vue** en lecture seule ; si la liste d'origine change, la vue change aussi. `List.copyOf(l)` fait une copie immuable.
- `stream().toList()` (Java 16) renvoie une liste non modifiable ; `collect(Collectors.toList())` ne garantit rien (en pratique une ArrayList modifiable).
- `subList` est une vue : modifier la liste d'origine structurellement invalide la sous-liste.

**Autres implémentations utiles.**
- `ArrayDeque` : pile et file, plus rapide que `Stack` (legacy, synchronisée, hérite de Vector) et que LinkedList. Méthodes `push`/`pop`/`peek` (pile), `offer`/`poll` (file). N'accepte pas null.
- `PriorityQueue` : tas binaire, `poll()` renvoie le plus petit selon l'ordre naturel ou un Comparator ; insertion/retrait O(log n), consultation du minimum O(1). Son itérateur n'est **pas** trié.
- `TreeMap`/`TreeSet` implémentent `NavigableMap`/`NavigableSet` : `floorKey`, `ceilingKey`, `headMap`, `tailMap`, `subMap`, `firstKey`, `pollFirst`. Refusent null en clé avec l'ordre naturel. L'unicité y est définie par `compareTo` == 0, pas par equals : un Comparator incohérent avec equals "perd" des éléments.
- `LinkedHashMap(capacité, 0.75f, true)` en mode accès-ordonné + redéfinition de `removeEldestEntry` = un cache LRU en dix lignes.
- `EnumMap`, `EnumSet` : pour des clés enum, très compacts.
- `WeakHashMap` : clés tenues par des références faibles, l'entrée disparaît quand la clé n'est plus référencée ailleurs (caches de métadonnées). `IdentityHashMap` : compare les clés avec `==`.
- `ConcurrentSkipListMap` : map triée et concurrente. `ConcurrentLinkedQueue` : file non bloquante.
- `BitSet` : ensemble de bits compact.

**SequencedCollection** (Java 21) : interface commune aux collections ordonnées (List, Deque, LinkedHashSet, SortedSet...) avec `getFirst()`, `getLast()`, `addFirst`, `addLast`, `removeFirst`, `reversed()`. Idem `SequencedMap` (`firstEntry`, `pollLastEntry`).

**Collections utilitaires** : `Collections.sort`, `reverse`, `shuffle`, `frequency`, `max`/`min`, `nCopies`, `emptyList()`, `singletonList()`, `swap`, `binarySearch` (sur une liste triée).

**RandomAccess** : interface marqueur implémentée par ArrayList (pas LinkedList), qui permet à des algorithmes de choisir l'accès par index.

**Coût mémoire** : une `ArrayList<Integer>` de 1 million d'éléments coûte des dizaines de mégaoctets (chaque Integer est un objet avec en-tête), contre 4 Mo pour un `int[]`. Sur de gros volumes numériques : tableaux de primitifs, ou bibliothèques spécialisées (Eclipse Collections, fastutil).

### En situation

- **"Cette recherche est lente"** → `list.contains()` est O(n) → HashSet.
- **"ConcurrentModificationException"** → modification pendant itération.
- **"Comment tu choisis ta structure ?"** → selon le pattern d'accès dominant.
- **"Collection partagée entre threads"** → ConcurrentHashMap plutôt que synchronisation manuelle.

### Questions techniques

**Q : ArrayList vs LinkedList ?** R : Accès O(1)/insertion milieu O(n) vs accès O(n)/insertion O(1). ArrayList par défaut.

**Q : Comment fonctionne une HashMap ?** R : Tableau de buckets, index via `hash & (n-1)`, collision → chaînage + equals, load factor 0.75 → rehashing, >8 éléments → arbre.

**Q : Pourquoi une capacité en puissance de 2 ?** R : Pour remplacer le modulo par une opération bit-à-bit.

**Q : Rôle de l'arbre rouge-noir depuis Java 8 ?** R : Ramener le pire cas d'un bucket saturé de O(n) à O(log n).

**Q : HashMap accepte null ?** R : Oui (une clé null). ConcurrentHashMap et Hashtable non.

**Q : Comparable vs Comparator ?** R : Ordre naturel interne vs ordre externe, plusieurs possibles.

**Q : Supprimer pendant une itération ?** R : `iterator.remove()` ou `removeIf()`.

**Q : Capacité initiale et load factor d'une HashMap ?** R : 16 et 0.75, donc redimensionnement à 13 éléments.

**Q : Quand un bucket devient-il un arbre ?** R : Au-delà de 8 éléments, si la table fait au moins 64 cases ; sinon la table est agrandie.

**Q : Comment est implémenté HashSet ?** R : Par une HashMap dont les clés sont les éléments et les valeurs un objet factice.

**Q : ConcurrentHashMap utilise-t-elle encore des segments ?** R : Non depuis Java 8 : CAS sur bucket vide, synchronized sur le premier nœud sinon, lectures sans verrou.

**Q : Pourquoi ConcurrentHashMap refuse null ?** R : Pour qu'un get qui renvoie null signifie sans ambiguïté "absent", sans verrou supplémentaire.

**Q : computeIfAbsent sur ConcurrentHashMap est-il atomique ?** R : Oui pour la clé concernée ; la fonction doit rester courte et ne pas modifier la map.

**Q : HashMap vs Hashtable ?** R : Hashtable est synchronisée, refuse null et est legacy ; HashMap n'est pas synchronisée et accepte une clé null.

**Q : synchronizedMap vs ConcurrentHashMap ?** R : Un verrou global sur chaque méthode vs verrouillage fin et lectures sans verrou.

**Q : Peut-on avoir une ConcurrentModificationException sans threads ?** R : Oui, en modifiant une collection pendant qu'on l'itère avec for-each.

**Q : Fail-fast vs fail-safe ?** R : Fail-fast lève une exception si la collection change ; fail-safe/faiblement cohérent travaille sur un instantané ou tolère la modification.

**Q : Que se passe-t-il avec `Arrays.asList(1,2,3).add(4)` ?** R : UnsupportedOperationException : la liste est de taille fixe.

**Q : `List.of` accepte-t-il null ?** R : Non, NullPointerException.

**Q : unmodifiableList vs List.copyOf ?** R : Vue en lecture seule qui reflète l'original vs copie immuable indépendante.

**Q : `toList()` vs `collect(Collectors.toList())` ?** R : toList() renvoie une liste non modifiable ; Collectors.toList() ne garantit rien (en pratique modifiable).

**Q : Pourquoi ne pas utiliser Stack ?** R : Legacy, synchronisée, hérite de Vector ; ArrayDeque est plus rapide.

**Q : L'itérateur d'une PriorityQueue est-il trié ?** R : Non, seul poll() respecte l'ordre.

**Q : Comment TreeSet détermine-t-il les doublons ?** R : Par compareTo ou le Comparator (résultat 0), pas par equals.

**Q : Comment faire un cache LRU simple ?** R : LinkedHashMap en mode accès-ordonné avec removeEldestEntry.

**Q : À quoi sert WeakHashMap ?** R : Associer des données à des clés sans empêcher leur garbage collection.

**Q : IdentityHashMap ?** R : Une map qui compare les clés avec == au lieu d'equals.

**Q : Méthodes de SequencedCollection ?** R : getFirst, getLast, addFirst, addLast, removeFirst, removeLast, reversed (Java 21).

**Q : Comment compter les occurrences avec une Map ?** R : `map.merge(cle, 1, Integer::sum)`.

**Q : Pourquoi itérer sur entrySet plutôt que keySet ?** R : On évite une recherche get() supplémentaire par élément.

**Q : Une clé de HashMap doit-elle être immutable ?** R : Ses champs utilisés dans hashCode/equals doivent l'être, sinon l'entrée devient introuvable.

**Q : Comment trier une liste par plusieurs critères ?** R : `list.sort(Comparator.comparing(User::getNom).thenComparing(User::getAge, Comparator.reverseOrder()))`.

**Q : Comment gérer les null dans un Comparator ?** R : `Comparator.nullsFirst(...)` ou `nullsLast(...)`.

### Questions de situation

**Q : "Une vérification d'appartenance dans une liste de 100 000 éléments est appelée en boucle. C'est très lent."**
R : "Le problème est algorithmique : `list.contains()` est en O(n), et appelé dans une boucle sur n éléments, on obtient du O(n²).
La correction tient en une ligne : convertir la liste en `HashSet` **une seule fois avant** la boucle. La recherche passe en O(1) et le total en O(n). Sur 100 000 éléments, c'est un facteur de plusieurs milliers.
Le principe général : dès qu'on fait beaucoup de recherches d'appartenance, il faut une structure basée sur le hash. Et si l'ordre compte, LinkedHashSet préserve l'ordre d'insertion sans perdre le O(1)."

**Q : "Comment tu choisis entre les implémentations de Map ?"**
R : "Je pars du **pattern d'accès dominant**.
Accès rapide par clé, sans besoin d'ordre → HashMap, c'est le défaut.
Besoin de préserver l'ordre d'insertion, par exemple pour une sortie déterministe ou un cache LRU → LinkedHashMap, qui propose même un mode accès-ordonné conçu pour ça.
Besoin de clés triées ou de requêtes par intervalle → TreeMap, en acceptant O(log n).
Map partagée entre threads → ConcurrentHashMap, parce qu'elle verrouille par bucket au lieu de bloquer toute la structure comme le ferait une map synchronisée.
Et si je connais approximativement la taille finale, je dimensionne la capacité initiale pour éviter des rehashings répétés."

**Q : "Tu dois traiter un fichier de millions de lignes sans saturer la mémoire."**
R : "Le principe est de ne jamais tout charger. Je lirais en **streaming**, ligne par ligne, avec `Files.lines()` qui retourne un Stream paresseux, en traitant et en écrivant au fil de l'eau.
Pour les agrégations, j'utiliserais des structures dont la taille ne croît pas avec le volume : des compteurs, ou une map de clés distinctes si leur nombre est borné.
Et systématiquement du try-with-resources pour garantir la fermeture du flux, sur des millions de lignes, une fuite de descripteur se paie vite."

**Q : "Implémente un cache LRU de capacité fixe."**
R : "La solution idiomatique s'appuie sur `LinkedHashMap` en mode **accès-ordonné** :
```java
class LruCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacite;
    LruCache(int capacite) {
        super(16, 0.75f, true);      // true = ordre d'accès
        this.capacite = capacite;
    }
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacite;    // évince le moins récemment utilisé
    }
}
```
Chaque `get` déplace l'entrée en fin de liste, et l'entrée la plus ancienne est retirée automatiquement. Toutes les opérations sont en O(1).
Elle n'est pas thread-safe : en concurrence, je l'envelopperais ou, mieux, j'utiliserais **Caffeine**, qui offre éviction par taille, TTL et une politique plus efficace que le LRU pur. Si on me demande de l'écrire sans LinkedHashMap : une HashMap vers des nœuds d'une liste doublement chaînée, avec déplacement en tête à chaque accès."

**Q : "Tu dois regrouper des commandes par client et calculer le total par client."**
R : "Avec une boucle et une map : `totaux.merge(c.clientId(), c.montant(), BigDecimal::add)`. Ou en stream : `groupingBy(Commande::clientId, reducing(BigDecimal.ZERO, Commande::montant, BigDecimal::add))`.
Si la volumétrie est très importante, ce calcul a sa place en base avec un `GROUP BY` : on ne rapatrie pas un million de lignes pour les additionner en Java."

**Q : "Une liste partagée est lue en permanence par des dizaines de threads et modifiée une fois par heure."**
R : "C'est le cas d'école de `CopyOnWriteArrayList` : les lectures sont sans verrou et ne voient jamais d'état intermédiaire, et le coût de la copie à chaque écriture est négligeable vu la fréquence.
Une alternative tout aussi bonne : une référence `volatile` (ou `AtomicReference`) vers une **liste immuable**, remplacée en bloc à chaque mise à jour. Les lecteurs lisent toujours une version cohérente. À l'inverse, avec des écritures fréquentes, CopyOnWrite deviendrait catastrophique."

═══════════════════════════════════════════
# 5. STREAMS & LAMBDAS
═══════════════════════════════════════════

### Vue d'ensemble

Java 8 a introduit la **programmation fonctionnelle** dans un langage jusque-là purement impératif. L'idée centrale : décrire **ce qu'on veut obtenir** plutôt que **comment l'obtenir** étape par étape. Une boucle `for` avec un accumulateur devient un pipeline `filter → map → collect`.

L'intérêt n'est pas seulement esthétique. Un stream est **lazy** (rien ne s'exécute avant l'opération terminale, ce qui permet des optimisations comme le court-circuit), **composable** (on chaîne les transformations), et **parallélisable** (le même code peut tourner sur plusieurs threads en changeant un mot). En contrepartie, un stream mal utilisé peut être plus lent qu'une boucle, et un pipeline trop long devient illisible, le bon usage est un équilibre.

`Optional`, introduit en même temps, répond à un autre problème : le `null`, décrit par son inventeur comme son "erreur à un milliard de dollars". Optional rend l'absence de valeur **explicite dans le type**, donc visible par le compilateur et par le lecteur.

### Cours

**Interface fonctionnelle** : une interface à une seule méthode abstraite, implémentable par une lambda. Les principales :

| Interface | Signature | Rôle |
|---|---|---|
| `Function<T,R>` | `R apply(T)` | transformer |
| `Predicate<T>` | `boolean test(T)` | tester |
| `Consumer<T>` | `void accept(T)` | consommer |
| `Supplier<T>` | `T get()` | fournir |
| `BiFunction<T,U,R>` | `R apply(T,U)` | transformer à deux entrées |
| `UnaryOperator<T>` | `T apply(T)` | transformer sans changer de type |

**Method references** : `x -> print(x)` devient `System.out::println`. Quatre formes : statique (`Math::abs`), sur type (`String::toLowerCase`), sur instance (`out::println`), constructeur (`ArrayList::new`).

**Le pipeline.** Un stream se compose d'une **source**, d'opérations **intermédiaires** (lazy, retournent un stream) et d'une opération **terminale** (déclenche l'exécution).

- Intermédiaires : `filter`, `map`, `flatMap`, `sorted`, `distinct`, `limit`, `skip`, `peek`.
- Terminales : `collect`, `forEach`, `reduce`, `count`, `min`/`max`, `findFirst`/`findAny`, `anyMatch`/`allMatch`/`noneMatch`.

Un stream ne se consomme **qu'une fois**.

**map vs flatMap** : `map` transforme 1→1. `flatMap` transforme 1→stream puis **aplatit** : c'est ce qu'on utilise pour transformer une liste de listes en liste simple, ou pour chaîner des Optional.

**reduce** agrège les éléments en une seule valeur : `stream.reduce(0, Integer::sum)`. Trois formes selon qu'on fournit une valeur initiale et un combineur pour le parallèle.

**Collectors** : le vrai levier de puissance :
```java
groupingBy(User::getCity)                              // Map<City, List<User>>
groupingBy(User::getCity, counting())                  // Map<City, Long>
groupingBy(User::getCity, mapping(User::getName, toList()))
partitioningBy(u -> u.getAge() >= 18)                  // Map<Boolean, List<User>>
joining(", ", "[", "]")                                // "[a, b, c]"
toMap(User::getEmail, u -> u, (a, b) -> a)             // merge function obligatoire si doublons
averagingInt(User::getAge) / summingInt(...)
```
Le second argument de `groupingBy` s'appelle un **downstream collector** : il traite chaque groupe, ce qui évite de construire des listes intermédiaires inutiles.

**Optional.** `of` (lève NPE si null) vs `ofNullable` (retourne vide). Récupération : `orElse(valeur)`, `orElseGet(fournisseur)` (lazy, à préférer si le défaut est coûteux à calculer), `orElseThrow`. Chaînage avec `map` et `flatMap`.

Anti-patterns : `if (opt.isPresent()) opt.get()` reproduit exactement le null check qu'on voulait éviter. Et Optional ne doit **jamais** être un paramètre de méthode ni un champ de classe, uniquement un type de retour.

**Streams parallèles.** `parallelStream()` distribue le traitement sur le ForkJoinPool commun. Bénéfique si trois conditions sont réunies : gros volume, opérations CPU coûteuses, traitements indépendants. À éviter avec de l'I/O (les threads du pool commun se bloquent et pénalisent toute l'application), des effets de bord, ou de petites collections où le coût de découpage dépasse le gain.

**Créer des streams** : `collection.stream()`, `Stream.of(a, b)`, `Arrays.stream(tab)`, `Stream.iterate(1, x -> x * 2)` (infini, à borner avec `limit`), `Stream.iterate(1, x -> x < 100, x -> x * 2)` (Java 9, avec condition d'arrêt), `Stream.generate(supplier)`, `IntStream.range(0, 10)` / `rangeClosed`, `Files.lines(path)`, `string.chars()`, `Stream.ofNullable(x)` (Java 9), `Stream.concat(a, b)`.

**Opérations ajoutées depuis Java 8** : `takeWhile` / `dropWhile` (Java 9, s'arrêtent au premier élément qui ne satisfait pas la condition), `mapMulti` (Java 16), `toList()` (Java 16, liste non modifiable).

**Collectors avancés** : `toSet`, `toCollection(TreeSet::new)`, `toUnmodifiableList`, `groupingBy(f, TreeMap::new, downstream)` (choisir le type de map), `collectingAndThen(toList(), Collections::unmodifiableList)`, `filtering` et `flatMapping` (Java 9, en downstream), `teeing(c1, c2, fusion)` (Java 12, deux collecteurs en un passage), `summarizingInt` (count, sum, min, max, average d'un coup), `maxBy(comparator)` (renvoie un Optional), `reducing`.

**Stream Gatherers** (Java 24, JEP 485) : `stream.gather(...)` permet des opérations intermédiaires personnalisées, avec état. Gatherers fournis : `windowFixed(n)` (paquets de n), `windowSliding(n)` (fenêtres glissantes), `fold`, `scan` (cumul progressif), `mapConcurrent(n, f)` (appliquer f en parallèle sur des virtual threads avec une concurrence maximale, en conservant l'ordre).

**Streams de primitifs** : `IntStream`, `LongStream`, `DoubleStream` évitent le boxing. `mapToInt(User::getAge).sum()`, `average()` (renvoie un `OptionalDouble`), `summaryStatistics()`, `boxed()` pour revenir à un `Stream<Integer>`. Sur de gros volumes numériques, le boxing coûte cher (allocation + GC).

**Ordre et court-circuit** : `findFirst` respecte l'ordre de rencontre, `findAny` peut renvoyer n'importe quel élément (plus rapide en parallèle). `limit`, `findFirst`, `anyMatch`, `allMatch`, `noneMatch` court-circuitent : sur un stream infini, ils permettent de terminer. `sorted` et `distinct` sont des opérations **à état** : `sorted` doit tout lire avant d'émettre le premier élément.

**Exécution verticale** : les éléments traversent le pipeline un par un (filter → map → ... pour le premier, puis le second), pas étape par étape sur toute la collection. D'où l'intérêt de placer les `filter` tôt.

**peek** : prévu pour le débogage. Il n'est exécuté que pour les éléments réellement traités, et depuis Java 9 `count()` peut ne pas exécuter le pipeline du tout si la taille est connue à l'avance (source SIZED sans filtre). Ne jamais y mettre de logique métier.

**Effets de bord** : interdits dans les lambdas de stream (modifier une liste externe depuis `forEach` ou `map`). Correct en séquentiel par chance, faux en parallèle. On collecte plutôt avec un Collector.

**Lambdas, règles fines** :
- Une lambda ne peut lancer une **exception checked** que si l'interface fonctionnelle la déclare. Les interfaces standard (`Function`, etc.) ne le font pas : il faut l'envelopper (try/catch qui relance une unchecked) ou définir sa propre interface fonctionnelle.
- Composition : `f.andThen(g)` (f puis g), `f.compose(g)` (g puis f), `Function.identity()`, `predicate.and/or/negate()`, `Predicate.not(String::isBlank)` (Java 11).
- `Comparator.comparing(User::getAge).reversed()` inverse tout le comparateur ; pour n'inverser qu'un critère, `thenComparing(User::getNom, Comparator.reverseOrder())`.
- Interfaces spécialisées pour les primitifs : `IntFunction`, `ToIntFunction`, `IntPredicate`, `IntUnaryOperator`, `IntBinaryOperator`, `BinaryOperator<T>`, `BiConsumer`, `BiPredicate`.

**Optional, le reste de l'API** : `ifPresent`, `ifPresentOrElse` (Java 9), `or(() -> autreOptional)` (Java 9), `stream()` (Java 9, pour aplatir une liste d'Optional avec flatMap), `isEmpty()` (Java 11), `orElseThrow()` sans argument (Java 10, préférable à `get()`), `filter`. Ne jamais retourner `null` à la place d'un Optional. `OptionalInt`/`OptionalDouble` pour les primitifs. Optional n'est pas sérialisable.

**Performance** : un stream n'est pas plus rapide qu'une boucle ; sur de petits volumes il est même légèrement plus lent (création des objets du pipeline). Il gagne en lisibilité et en composabilité. Les streams parallèles utilisent le ForkJoinPool commun (taille = nombre de cœurs - 1) ; une source qui se découpe mal (LinkedList, `Stream.iterate`) parallélise mal, contrairement à ArrayList ou un tableau.

### En situation

- **"Ce code est illisible avec des boucles imbriquées"** → pipeline de stream.
- **"On peut paralléliser ce traitement ?"** → oui si CPU-bound et sans effet de bord.
- **"Des NPE partout"** → Optional en retour + validation en entrée.
- **"Ce stream plante sur des clés dupliquées"** → `toMap` sans merge function.

### Questions techniques

**Q : C'est quoi une interface fonctionnelle ?** R : Une interface à une seule méthode abstraite, implémentable par lambda.

**Q : Intermédiaire vs terminale ?** R : Intermédiaire = lazy, retourne un stream. Terminale = déclenche l'exécution.

**Q : map vs flatMap ?** R : map transforme 1→1 ; flatMap transforme 1→stream et aplatit.

**Q : Peut-on réutiliser un stream ?** R : Non, IllegalStateException.

**Q : C'est quoi un downstream collector ?** R : Le second argument de groupingBy, qui traite chaque groupe (counting, mapping, summing).

**Q : Piège du toMap ?** R : Exception sur clés dupliquées, fournir une merge function.

**Q : orElse vs orElseGet ?** R : orElse évalue toujours le défaut ; orElseGet ne l'évalue que si nécessaire (lazy).

**Q : Où ne PAS utiliser Optional ?** R : En paramètre ou en champ. Uniquement en type de retour.

**Q : Comment créer un stream infini ?** R : `Stream.iterate` ou `Stream.generate`, borné ensuite par `limit` ou `takeWhile`.

**Q : takeWhile vs filter ?** R : filter teste tous les éléments ; takeWhile s'arrête au premier élément qui échoue.

**Q : findFirst vs findAny ?** R : findFirst respecte l'ordre ; findAny peut renvoyer n'importe quel élément, plus rapide en parallèle.

**Q : Une opération stateful ?** R : sorted, distinct : elles ont besoin de voir plusieurs éléments (voire tous) avant d'émettre.

**Q : Les éléments traversent-ils le pipeline étape par étape ou un par un ?** R : Un par un (exécution verticale), ce qui permet le court-circuit.

**Q : Pourquoi peek n'affiche-t-il parfois rien ?** R : Il n'y a pas d'opération terminale, ou count() a calculé la taille sans exécuter le pipeline (Java 9+).

**Q : Comment éviter le boxing dans un stream numérique ?** R : IntStream/LongStream/DoubleStream via mapToInt, etc.

**Q : Que renvoie `IntStream.average()` ?** R : Un OptionalDouble (vide si le stream est vide).

**Q : À quoi sert Collectors.teeing ?** R : Combiner deux collecteurs en un seul passage puis fusionner leurs résultats (Java 12).

**Q : Comment choisir le type de map d'un groupingBy ?** R : `groupingBy(f, TreeMap::new, downstream)`.

**Q : C'est quoi un Gatherer ?** R : Une opération intermédiaire personnalisée avec état (Java 24) : windowFixed, windowSliding, scan, fold, mapConcurrent.

**Q : Pourquoi une lambda ne peut-elle pas lancer d'IOException dans un map ?** R : Function.apply ne déclare aucune exception checked ; il faut l'envelopper ou créer son interface.

**Q : andThen vs compose ?** R : f.andThen(g) applique f puis g ; f.compose(g) applique g puis f.

**Q : Comment inverser un seul critère de tri ?** R : `thenComparing(extracteur, Comparator.reverseOrder())`.

**Q : Comment aplatir une liste d'Optional ?** R : `liste.stream().flatMap(Optional::stream)`.

**Q : get() vs orElseThrow() sur Optional ?** R : Même comportement, mais orElseThrow() exprime explicitement le risque d'exception ; get() est déconseillé.

**Q : Un stream est-il plus rapide qu'une boucle ?** R : Pas en général ; il apporte lisibilité et composabilité. Il faut mesurer.

**Q : Quel pool utilise parallelStream ?** R : Le ForkJoinPool commun, dimensionné au nombre de cœurs moins un.

**Q : Pourquoi les effets de bord sont-ils dangereux dans un stream ?** R : Ils rendent le résultat dépendant de l'ordre d'exécution et sont incorrects en parallèle.

### Questions de situation

**Q : "Un collègue veut mettre parallelStream() partout. Qu'est-ce que tu lui dis ?"**
R : "Que ça risque d'empirer les choses. Le parallélisme n'aide que si le volume est important, les opérations coûteuses en CPU, et les traitements indépendants.
Sur une petite collection, le coût de découpage et de fusion dépasse le gain. Et c'est franchement nocif sur de l'**I/O** : les threads du ForkJoinPool commun se bloquent à attendre, et comme ce pool est partagé par toute l'application, ça ralentit des traitements sans aucun rapport. Pour de l'I/O concurrent, il faut un CompletableFuture avec un pool dédié, ou des virtual threads.
Attention aussi aux opérations avec état ou effet de bord : accumuler dans une collection non concurrente depuis un stream parallèle produit des résultats corrompus.
Et dans tous les cas : mesurer avant/après, sinon on ne fait que croire."

**Q : "Retourne, pour chaque ville, le nombre d'utilisateurs actifs."**
R : "Je filtrerais puis grouperais avec un downstream collector :
```java
users.stream()
     .filter(User::isActive)
     .collect(groupingBy(User::getCity, counting()));
```
Ça produit directement une `Map<String, Long>`. L'intérêt du downstream, c'est qu'on ne construit jamais les listes intermédiaires : on compte au fil de l'eau."

**Q : "Une méthode retourne parfois null et provoque des NPE en cascade."**
R : "Je changerais la signature pour retourner `Optional<T>`. L'absence devient explicite dans le contrat et le compilateur force l'appelant à la traiter.
Côté appelant, je chaînerais avec `map` et `orElseThrow` plutôt qu'un `isPresent/get`, qui ne fait que déguiser le null check.
Mais attention à ne pas généraliser : Optional en paramètre ou en champ d'entité est un anti-pattern. Et quand le null vient d'une source externe, la bonne réponse est de **valider en entrée** plutôt que de propager l'incertitude dans tout le code."

**Q : "Donne le salaire moyen par département, trié par département, uniquement pour les employés actifs."**
R :
```java
Map<String, Double> moyenne = employes.stream()
    .filter(Employe::actif)
    .collect(groupingBy(Employe::departement, TreeMap::new,
                        averagingDouble(Employe::salaire)));
```
"La TreeMap trie par clé. Si je voulais le meilleur salarié de chaque département : `groupingBy(Employe::departement, maxBy(comparing(Employe::salaire)))`, qui donne des Optional ; avec `collectingAndThen(maxBy(...), Optional::get)` pour les déballer, sachant qu'un groupe n'est jamais vide."

**Q : "Transforme une liste de commandes, chacune avec ses lignes, en liste unique de produits distincts triés."**
R :
```java
List<String> produits = commandes.stream()
    .flatMap(c -> c.lignes().stream())
    .map(Ligne::produit)
    .distinct()
    .sorted()
    .toList();
```
"`flatMap` aplatit les listes de lignes ; `distinct` utilise equals/hashCode ; `toList()` renvoie une liste non modifiable, ce qui est généralement ce qu'on veut pour un résultat."

**Q : "Un traitement lit un fichier de logs de 5 Go et doit compter les erreurs par code."**
R : "Je lirais en flux avec `Files.lines(path)` dans un **try-with-resources** (le stream tient un descripteur de fichier ouvert), puis `filter` sur les lignes d'erreur, `map` pour extraire le code, et `groupingBy(code, counting())`.
La mémoire reste bornée par le nombre de codes distincts, pas par la taille du fichier, puisque `Files.lines` est paresseux.
Si c'est trop lent, je mesurerais avant de paralléliser : la lecture disque est souvent le goulot, et un stream parallèle sur des lignes de fichier se découpe moins bien qu'une collection en mémoire."

═══════════════════════════════════════════
# 6. CONCURRENCE & MULTITHREADING
═══════════════════════════════════════════

### Vue d'ensemble

La concurrence, c'est faire progresser plusieurs traitements en même temps. C'est indispensable (un serveur web traite des milliers de requêtes simultanées) mais c'est aussi **la source de bugs la plus difficile à diagnostiquer**, parce que ces bugs sont non déterministes : ils n'apparaissent que sous certaines conditions de timing, ne se reproduisent pas en test, et disparaissent quand on ajoute un log.

Deux problèmes fondamentaux à comprendre. La **visibilité** : chaque thread peut travailler sur une copie cachée d'une variable, donc une modification faite par l'un peut rester invisible à l'autre. Et l'**atomicité** : une opération qui paraît indivisible en code (`count++`) est en réalité plusieurs instructions, et un autre thread peut s'intercaler au milieu.

Java propose une gamme d'outils qui vont du plus bas niveau (`synchronized`, `volatile`) au plus haut (`ExecutorService`, `CompletableFuture`, collections concurrentes). **La bonne pratique moderne est de rester au niveau le plus haut possible** : on ne manipule quasiment jamais de threads à la main.

### Cours

**Thread** : un fil d'exécution. On implémente `Runnable` plutôt que d'étendre `Thread` (ça laisse la possibilité d'hériter d'autre chose). `start()` lance un nouveau thread ; appeler `run()` directement l'exécute simplement dans le thread courant.

**synchronized** : garantit qu'un seul thread à la fois entre dans le bloc, via un verrou (moniteur) sur un objet. Un bloc `synchronized(lock)` est préférable à une méthode synchronisée, parce qu'il réduit la zone critique et évite de verrouiller sur `this` (que du code externe pourrait aussi verrouiller).

**volatile** : garantit la **visibilité** : les lectures/écritures vont directement en mémoire principale, pas dans un cache local. Mais **pas l'atomicité** : `count++` reste incorrect, car c'est lire + incrémenter + écrire.

**Classes atomiques** (`AtomicInteger`, `AtomicLong`, `AtomicReference`) : opérations atomiques **sans verrou**, basées sur l'instruction CPU **CAS** (Compare-And-Swap) : "si la valeur est toujours X, remplace-la par Y, sinon réessaie". Très performant sous contention modérée.

**Verrous explicites** : `ReentrantLock` (comme synchronized mais avec `tryLock`, timeout, équité), `ReadWriteLock` (plusieurs lecteurs simultanés, un seul écrivain, idéal quand les lectures dominent).

**Outils de coordination** : `CountDownLatch` (attendre que N tâches se terminent), `Semaphore` (limiter le nombre d'accès concurrents), `CyclicBarrier` (synchroniser des threads à un point de rendez-vous).

**ExecutorService** : un pool de threads réutilisables. On soumet des tâches plutôt que de créer des threads.
- `newFixedThreadPool(n)` : n threads, file d'attente illimitée.
- `newCachedThreadPool()` : crée à la demande, recycle après 60s.
- `newSingleThreadExecutor()` : exécution séquentielle garantie.
- `newScheduledThreadPool()` : tâches planifiées/récurrentes.

Toujours appeler `shutdown()`. En production, on préfère souvent construire un `ThreadPoolExecutor` explicitement pour maîtriser la taille de la file, une file illimitée peut masquer une surcharge jusqu'à l'OutOfMemory.

**CompletableFuture** : l'asynchrone composable.
```java
CompletableFuture.supplyAsync(() -> fetch())
    .thenApply(this::parse)          // transformer (map)
    .thenCompose(this::enrichAsync)  // enchaîner un autre future (flatMap)
    .exceptionally(ex -> fallback()) // gérer l'erreur
```
`allOf` attend tous les futures, `anyOf` le premier. C'est l'outil pour paralléliser des appels indépendants.

**ThreadLocal** : une variable propre à chaque thread. Utile pour propager un contexte (identifiant de trace, utilisateur courant) sans le passer en paramètre. **Danger** : dans un pool, le thread est recyclé et garde la valeur, d'où des fuites mémoire et des fuites de données entre requêtes. Il faut impérativement nettoyer avec `remove()`.

**Deadlock** : deux threads détiennent chacun un verrou et attendent celui de l'autre. Prévention : acquérir les verrous **toujours dans le même ordre**, utiliser des timeouts (`tryLock`), réduire le nombre de verrous.

**Virtual threads (Java 21)** : threads légers gérés par la JVM et non par l'OS. Quand un virtual thread bloque sur une I/O, la JVM le **démonte** de son carrier thread, qui reste disponible. On peut en créer des millions. Ils transforment le modèle "un thread par requête" en solution viable pour de forts volumes I/O. Inutiles pour du CPU-bound. (Le *pinning* sur `synchronized` a disparu en Java 24, voir plus bas.)

**Cycle de vie d'un thread** (`Thread.State`) : `NEW` (créé, pas démarré) → `RUNNABLE` (en exécution ou prêt) → `BLOCKED` (attend un moniteur `synchronized`) / `WAITING` (`wait()`, `join()`, `LockSupport.park()` sans délai) / `TIMED_WAITING` (`sleep`, `wait(ms)`, `join(ms)`) → `TERMINATED`. Ces états apparaissent dans un thread dump : beaucoup de threads BLOCKED sur le même verrou = contention ; beaucoup de WAITING sur une socket = attente réseau.

**Runnable vs Callable** : `Runnable.run()` ne retourne rien et ne peut pas lever d'exception checked ; `Callable.call()` retourne une valeur et peut lever. Soumis à un ExecutorService, un Callable renvoie un `Future<T>` : `get()` (bloquant, éventuellement avec timeout), `cancel(true)`, `isDone()`. Une exception levée dans la tâche est encapsulée dans une `ExecutionException` au `get()` ; avec `execute(runnable)`, elle remonte au `UncaughtExceptionHandler` ; avec `submit` sans `get()`, elle est **silencieusement perdue**.

**wait / notify** : à appeler uniquement en détenant le moniteur de l'objet (sinon `IllegalMonitorStateException`). `wait()` relâche le verrou et attend ; `notify()` réveille un thread, `notifyAll()` tous. Toujours attendre dans une **boucle** `while (!condition) wait();` à cause des réveils intempestifs (*spurious wakeups*). En code moderne on préfère les outils de `java.util.concurrent` (BlockingQueue, CountDownLatch, Condition).

**sleep vs wait** : `sleep` garde les verrous détenus et est une méthode statique de Thread ; `wait` relâche le moniteur et appartient à Object.

**Interruption** : mécanisme **coopératif**. `thread.interrupt()` positionne un drapeau ; les méthodes bloquantes (`sleep`, `wait`, `join`, `BlockingQueue.take`) lèvent alors `InterruptedException` et **effacent** le drapeau. Règle : ne jamais avaler une InterruptedException ; soit la propager, soit restaurer le drapeau avec `Thread.currentThread().interrupt()`. C'est ainsi qu'un `shutdownNow()` ou un `Future.cancel(true)` peut arrêter une tâche.

**Thread démon** : un thread `setDaemon(true)` n'empêche pas la JVM de s'arrêter ; il est tué sans exécuter ses `finally`. Utile pour des tâches de fond, dangereux pour des écritures.

**Java Memory Model et happens-before.** Sans synchronisation, le compilateur, le JIT et le CPU peuvent réordonner les instructions, et un thread peut ne jamais voir une écriture d'un autre. Le JMM garantit la visibilité uniquement via des relations *happens-before* :
- la libération d'un verrou happens-before son acquisition suivante (synchronized, Lock) ;
- une écriture `volatile` happens-before les lectures suivantes de cette variable (et rend visibles toutes les écritures faites avant elle) ;
- `thread.start()` happens-before toute action du thread démarré ; toutes les actions d'un thread happen-before le retour de `join()` ;
- la fin d'un constructeur happens-before l'utilisation des champs `final` par un autre thread (si `this` ne s'est pas échappé pendant la construction) ;
- les opérations des collections concurrentes et des classes atomiques établissent aussi des happens-before.

**Data race vs race condition** : une *data race* est un accès concurrent non synchronisé à une variable dont au moins un en écriture (comportement imprévisible au sens du JMM). Une *race condition* est un défaut logique où le résultat dépend de l'entrelacement (par exemple un *check-then-act* : `if (!map.containsKey(k)) map.put(k, v)`, même sur une ConcurrentHashMap). On peut avoir une race condition sans data race.

**Double-checked locking** : le champ doit être `volatile`, sinon un autre thread peut voir une référence vers un objet partiellement construit.
```java
private static volatile Config instance;
static Config get() {
    Config c = instance;
    if (c == null) {
        synchronized (Config.class) {
            c = instance;
            if (c == null) instance = c = new Config();
        }
    }
    return c;
}
```
Plus simple et tout aussi paresseux : l'idiome **holder** (`private static class Holder { static final Config I = new Config(); }`), qui repose sur la garantie d'initialisation des classes par la JVM. Ou une `enum`.

**Réentrance** : `synchronized` et `ReentrantLock` sont réentrants, un thread qui détient déjà le verrou peut le reprendre (compteur). Avec un `Lock`, toujours `lock()` avant le try et `unlock()` dans le `finally`.

**Autres verrous et outils** : `Condition` (plusieurs files d'attente sur un même Lock, `await`/`signal`) ; `StampedLock` (Java 8, lecture **optimiste** sans verrou validée a posteriori, non réentrant) ; `Phaser` (barrière réutilisable à participants variables) ; `Exchanger`.

**LongAdder vs AtomicLong** : sous forte contention en écriture, `LongAdder` répartit le compteur sur plusieurs cellules et les additionne à la lecture : bien plus rapide pour des compteurs de métriques, moins adapté si on lit souvent la valeur exacte. `AtomicLong` reste préférable pour un compteur lu aussi souvent qu'écrit, ou pour générer des identifiants.

**Le problème ABA** : un CAS vérifie que la valeur vaut toujours A, mais elle a pu passer à B puis revenir à A entre-temps. Pour des structures lock-free, `AtomicStampedReference` ajoute un numéro de version.

**BlockingQueue, les implémentations** : `ArrayBlockingQueue` (bornée, un tableau), `LinkedBlockingQueue` (optionnellement bornée, **illimitée par défaut**), `SynchronousQueue` (capacité zéro, transfert direct, utilisée par `newCachedThreadPool`), `PriorityBlockingQueue`, `DelayQueue` (éléments disponibles après un délai), `LinkedTransferQueue`. Méthodes : `put`/`take` (bloquent), `offer`/`poll` (avec ou sans timeout, ne bloquent pas indéfiniment).

**ThreadPoolExecutor, les paramètres** : `corePoolSize`, `maximumPoolSize`, `keepAliveTime`, la **file**, une `ThreadFactory` (nommer les threads, indispensable pour lire un thread dump), et une **politique de rejet** quand file et pool sont pleins : `AbortPolicy` (défaut, lève RejectedExecutionException), `CallerRunsPolicy` (la tâche s'exécute dans le thread appelant, ce qui ralentit naturellement le producteur : une forme de backpressure), `DiscardPolicy`, `DiscardOldestPolicy`. Piège : les threads au-delà du core ne sont créés **que si la file est pleine** ; avec une file illimitée, `maximumPoolSize` ne sert jamais.

**Dimensionner un pool** : pour du CPU pur, environ le nombre de cœurs (`Runtime.getRuntime().availableProcessors()`). Pour de l'I/O : cœurs × (1 + temps d'attente / temps de calcul), formule de Goetz. Dans tous les cas, la limite réelle est souvent la ressource aval (pool de connexions de la base) : inutile d'avoir 200 threads pour 20 connexions.

**ForkJoinPool** : pool à **vol de travail** (*work stealing*) : chaque thread a sa file, et un thread inactif vole des tâches aux autres. Conçu pour les tâches récursives découpables (`RecursiveTask<T>` qui retourne un résultat, `RecursiveAction` sans résultat, méthodes `fork()`/`join()`/`compute()`). Le pool commun sert aux streams parallèles et aux méthodes `async` de CompletableFuture sans executor.

**CompletableFuture, le reste de l'API** : `thenAccept` (consommer), `thenRun`, `thenCombine` (combiner deux futures indépendants), `handle` (résultat **ou** erreur, retourne une valeur), `whenComplete` (effet de bord, ne change pas le résultat), `exceptionallyCompose` (Java 12), `orTimeout` et `completeOnTimeout` (Java 9), `join()` (comme get mais lève une exception unchecked). Les variantes `xxxAsync` sans executor utilisent le ForkJoinPool commun : en production, on passe un executor dédié. Une exception dans une étape saute les étapes suivantes jusqu'au premier `exceptionally`/`handle`.

**ExecutorService et try-with-resources** : depuis Java 19, ExecutorService implémente `AutoCloseable` ; `close()` attend la fin des tâches soumises. Sinon : `shutdown()` (n'accepte plus de tâches, termine celles en cours) puis `awaitTermination(timeout)`, puis `shutdownNow()` (interrompt) si nécessaire.

**Virtual threads en détail** (Java 21) :
- Création : `Thread.ofVirtual().start(r)`, `Thread.startVirtualThread(r)`, ou `Executors.newVirtualThreadPerTaskExecutor()` (un nouveau virtual thread par tâche).
- Ils s'exécutent sur un petit pool de *carrier threads* (un ForkJoinPool dédié, de la taille du nombre de cœurs). Au blocage sur une I/O, le virtual thread est démonté et sa pile sauvegardée dans le heap.
- **Ne jamais les mettre en pool** : ils sont jetables, on en crée un par tâche. Pour limiter la concurrence vers une ressource (une base, une API), on utilise un `Semaphore`, pas un pool.
- **Pinning** : un virtual thread reste collé à son carrier pendant un appel natif (JNI) ou, **jusqu'à Java 23**, pendant un bloc `synchronized`. Java 24 (JEP 491) a supprimé le pinning lié à `synchronized` : sur un JDK récent, `synchronized` n'est plus un problème. Sur Java 21, on préférait `ReentrantLock` dans le code qui bloque sous verrou. Détection : `-Djdk.tracePinnedThreads` (Java 21-23), événements JFR `jdk.VirtualThreadPinned`.
- `ThreadLocal` fonctionne mais devient coûteux avec des millions de threads, et les caches basés sur ThreadLocal (un buffer réutilisé par thread) perdent leur sens.
- Utiles pour l'I/O bloquante (HTTP, JDBC), inutiles pour le calcul pur. Spring Boot (3.2+) : `spring.threads.virtual.enabled=true` ; Quarkus : `@RunOnVirtualThread`.

**ScopedValue** (finalisé en Java 25, JEP 506) : alternative immuable et bornée à `ThreadLocal` pour partager un contexte (utilisateur, transaction) le long d'une pile d'appels, y compris vers des threads enfants. La valeur n'existe que pendant l'exécution de `ScopedValue.where(CLE, valeur).run(...)` : pas de fuite possible, pas de `remove()` à oublier.

**Structured concurrency** (en preview depuis Java 21, septième preview en Java 27) : `StructuredTaskScope` traite un groupe de sous-tâches comme une unité. Si une sous-tâche échoue, les autres sont annulées ; la portée ne se termine pas tant que ses sous-tâches tournent ; les threads apparaissent rattachés à leur parent dans les dumps. C'est l'équivalent structuré de "lancer N appels en parallèle et tout annuler si l'un échoue". En preview : il faut `--enable-preview`, et l'API a changé d'une version à l'autre.

**Autres problèmes de concurrence** : le **livelock** (les threads réagissent l'un à l'autre en boucle sans progresser, comme deux personnes qui s'écartent du même côté), la **famine** (*starvation*, un thread n'obtient jamais la ressource, par exemple avec un verrou non équitable), l'**inversion de priorité**.

**Niveaux de sûreté** à savoir nommer : immuable (String), thread-safe sans condition (ConcurrentHashMap), thread-safe sous condition (Collections.synchronizedList : l'itération doit être synchronisée par l'appelant), non thread-safe (ArrayList), hostile.

### En situation

- **"Compteur incohérent sous charge"** → race condition → AtomicInteger.
- **"L'appli se fige aléatoirement"** → deadlock ou pool saturé → thread dump.
- **"Comment accélérer une série d'appels externes ?"** → CompletableFuture en parallèle.
- **"Ce bean garde un état"** → singleton partagé, problème de concurrence.
- **"10 000 connexions simultanées"** → virtual threads ou réactif.

### Questions techniques

**Q : start() vs run() ?** R : start() crée un thread ; run() s'exécute dans le thread courant.

**Q : Que fait volatile ?** R : Garantit la visibilité, pas l'atomicité.

**Q : Pourquoi volatile ne suffit pas pour un compteur ?** R : `count++` = 3 opérations non atomiques.

**Q : C'est quoi le CAS ?** R : Compare-And-Swap, instruction CPU atomique utilisée par les classes Atomic, sans verrou.

**Q : C'est quoi un deadlock ?** R : Deux threads attendent mutuellement un verrou détenu par l'autre.

**Q : ReentrantLock vs synchronized ?** R : ReentrantLock ajoute tryLock, timeout, équité et plusieurs conditions.

**Q : thenApply vs thenCompose ?** R : thenApply transforme (map) ; thenCompose enchaîne un autre future (flatMap).

**Q : Danger de ThreadLocal ?** R : Dans un pool, le thread recyclé garde la valeur → fuite mémoire et de données. Nettoyer avec remove().

**Q : C'est quoi un virtual thread ?** R : Un thread léger géré par la JVM, démonté de son carrier quand il bloque sur I/O.

**Q : Les états d'un thread ?** R : NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED.

**Q : BLOCKED vs WAITING ?** R : BLOCKED attend un moniteur synchronized ; WAITING attend une notification ou un autre thread (wait, join, park).

**Q : Runnable vs Callable ?** R : Callable retourne une valeur et peut lever une exception checked ; Runnable non.

**Q : Que devient une exception dans une tâche soumise par submit ?** R : Elle est stockée dans le Future et relevée enveloppée dans ExecutionException au get() ; sans get(), elle est perdue.

**Q : Pourquoi appeler wait() dans une boucle ?** R : À cause des réveils intempestifs et parce que la condition a pu changer entre la notification et le réveil.

**Q : sleep vs wait ?** R : sleep garde les verrous ; wait relâche le moniteur. wait doit être appelé en détenant le moniteur.

**Q : notify vs notifyAll ?** R : Réveille un thread arbitraire vs tous les threads en attente ; notifyAll est plus sûr si plusieurs conditions partagent le même moniteur.

**Q : Comment bien gérer une InterruptedException ?** R : La propager, ou restaurer le drapeau avec Thread.currentThread().interrupt() ; ne jamais l'avaler.

**Q : C'est quoi un thread démon ?** R : Un thread qui n'empêche pas l'arrêt de la JVM ; il est tué sans exécuter ses finally.

**Q : C'est quoi la relation happens-before ?** R : La garantie du JMM qu'une action est visible et ordonnée avant une autre (verrou, volatile, start, join, champs final).

**Q : Data race vs race condition ?** R : Accès concurrent non synchronisé à une variable vs défaut logique dépendant de l'entrelacement (check-then-act).

**Q : Pourquoi volatile est-il indispensable dans le double-checked locking ?** R : Sans lui, un thread peut voir une référence vers un objet partiellement construit.

**Q : Alternative au double-checked locking ?** R : L'idiome holder (classe interne static), ou une enum.

**Q : synchronized est-il réentrant ?** R : Oui, un thread peut réacquérir un verrou qu'il détient déjà.

**Q : C'est quoi StampedLock ?** R : Un verrou avec un mode de lecture optimiste sans blocage, validé a posteriori ; non réentrant.

**Q : LongAdder vs AtomicLong ?** R : LongAdder répartit les écritures sous contention (idéal pour des compteurs de métriques) ; AtomicLong pour une valeur lue souvent ou des identifiants.

**Q : C'est quoi le problème ABA ?** R : Un CAS réussit alors que la valeur a changé puis est revenue à l'identique ; solution : AtomicStampedReference.

**Q : Pourquoi `if (!map.containsKey(k)) map.put(k, v)` est faux même avec une ConcurrentHashMap ?** R : C'est un check-then-act non atomique ; utiliser putIfAbsent ou computeIfAbsent.

**Q : ArrayBlockingQueue vs LinkedBlockingQueue ?** R : Bornée sur tableau vs chaînée, illimitée par défaut (risque de saturation mémoire).

**Q : C'est quoi une SynchronousQueue ?** R : Une file de capacité zéro : chaque put attend un take.

**Q : Les politiques de rejet de ThreadPoolExecutor ?** R : AbortPolicy, CallerRunsPolicy, DiscardPolicy, DiscardOldestPolicy.

**Q : Pourquoi maximumPoolSize ne sert à rien avec une file illimitée ?** R : Les threads au-delà du core ne sont créés que quand la file est pleine, ce qui n'arrive jamais.

**Q : Comment dimensionner un pool ?** R : CPU-bound : nombre de cœurs ; I/O-bound : cœurs × (1 + attente/calcul) ; plafonné par la ressource aval.

**Q : Pourquoi nommer les threads d'un pool ?** R : Pour rendre les thread dumps et les logs lisibles.

**Q : C'est quoi le work stealing ?** R : Les threads inactifs d'un ForkJoinPool volent des tâches dans les files des threads occupés.

**Q : RecursiveTask vs RecursiveAction ?** R : Avec résultat vs sans résultat.

**Q : thenCombine vs thenCompose ?** R : Combiner deux futures indépendants vs enchaîner un future qui dépend du résultat du précédent.

**Q : handle vs whenComplete vs exceptionally ?** R : handle transforme résultat ou erreur ; whenComplete observe sans modifier ; exceptionally ne traite que l'erreur.

**Q : Quel pool utilisent les méthodes async sans executor ?** R : Le ForkJoinPool commun ; en production, passer un executor dédié.

**Q : get() vs join() sur CompletableFuture ?** R : get lève des exceptions checked (ExecutionException, InterruptedException) ; join lève une CompletionException unchecked.

**Q : Comment arrêter proprement un ExecutorService ?** R : shutdown, awaitTermination avec timeout, puis shutdownNow si nécessaire ; ou close() depuis Java 19.

**Q : Faut-il mettre les virtual threads en pool ?** R : Non, on en crée un par tâche ; pour limiter la concurrence on utilise un Semaphore.

**Q : C'est quoi le pinning ?** R : Un virtual thread qui reste attaché à son carrier pendant un appel natif ou, avant Java 24, un bloc synchronized.

**Q : synchronized est-il toujours un problème avec les virtual threads ?** R : Plus depuis Java 24 (JEP 491) ; sur Java 21 à 23, ReentrantLock évitait le pinning.

**Q : Pourquoi ScopedValue plutôt que ThreadLocal ?** R : Immuable, borné à une portée d'exécution, sans fuite ni remove() à oublier, et adapté aux millions de virtual threads (final en Java 25).

**Q : C'est quoi la structured concurrency ?** R : Traiter un groupe de sous-tâches comme une unité avec annulation et gestion d'erreur communes ; encore en preview.

**Q : Livelock vs deadlock ?** R : Deadlock : les threads sont bloqués ; livelock : ils s'agitent sans progresser.

**Q : C'est quoi la famine ?** R : Un thread qui n'obtient jamais la ressource dont il a besoin.

### Questions de situation

**Q : "Un compteur donne des valeurs incohérentes en prod mais pas en test."**
R : "C'est une **race condition**, qui n'apparaît que sous concurrence réelle. `count++` n'est pas atomique : deux threads lisent la même valeur avant d'incrémenter, et une incrémentation est perdue.
`volatile` ne corrige pas ça, il ne traite que la visibilité, pas l'atomicité. La solution simple est `AtomicInteger.incrementAndGet()`, qui s'appuie sur une instruction CPU atomique sans verrou.
Ce cas illustre bien pourquoi les bugs de concurrence ne se reproduisent presque jamais en test : ils dépendent d'un entrelacement de threads qui n'arrive que sous charge."

**Q : "Un endpoint fait 5 appels externes de 200ms chacun, la réponse met 1 seconde."**
R : "Les appels sont séquentiels alors qu'ils semblent indépendants. Je les parallélise avec des `CompletableFuture` lancés ensemble, puis `allOf` pour attendre. Le temps total passe de la somme au maximum, soit environ 200ms.
Trois précautions. Vérifier qu'ils sont **vraiment indépendants** : si l'un consomme le résultat du précédent, on ne peut pas. Utiliser un **pool dédié** plutôt que le ForkJoinPool commun, pour ne pas impacter le reste. Et mettre un **timeout individuel** sur chaque appel, sinon un service lent bloque toute la réponse.
Si le volume d'appels concurrents est élevé, les virtual threads de Java 21 simplifient encore le modèle."

**Q : "L'application se fige de temps en temps, sans erreur dans les logs."**
R : "Un gel silencieux oriente vers un blocage de threads. L'outil clé est le **thread dump** pris pendant l'incident : il montre l'état de chaque thread, et la JVM détecte et signale explicitement les deadlocks.
Trois causes fréquentes. Un **deadlock** entre verrous acquis dans des ordres différents. Un **pool de connexions épuisé** parce que des connexions ne sont pas rendues, souvent une transaction qui ne se ferme pas. Et surtout des **appels externes sans timeout** : chaque appel lent immobilise un thread, le pool du serveur sature, et l'application ne répond plus du tout, même pour des requêtes sans rapport.
C'est pour ça que la règle 'un timeout sur tout appel réseau' n'admet aucune exception."

**Q : "Avec des virtual threads, ton service fait des milliers d'appels simultanés à une base qui sature."**
R : "C'est l'effet pervers classique : les virtual threads suppriment la limite naturelle qu'imposait la taille du pool de threads. Avant, 200 threads = au plus 200 requêtes concurrentes vers la base ; maintenant, des milliers.
La limite doit être **explicite et placée devant la ressource** : la taille du pool de connexions JDBC en est déjà une (les virtual threads attendront une connexion), et pour une API externe sans pool, un `Semaphore` qui borne les appels simultanés.
Je vérifierais aussi les timeouts d'acquisition de connexion pour échouer vite plutôt que d'empiler des attentes, et je surveillerais le nombre de threads en attente du pool. Les virtual threads changent le coût d'un thread, pas la capacité des systèmes en aval."

**Q : "Implémente un producteur-consommateur."**
R : "Avec une `BlockingQueue` **bornée**, qui gère toute la synchronisation :
```java
BlockingQueue<Tache> file = new ArrayBlockingQueue<>(1000);
// producteur
file.put(tache);              // bloque si la file est pleine : backpressure
// consommateur
while (!Thread.currentThread().isInterrupted()) {
    Tache t = file.take();    // bloque si la file est vide
    traiter(t);
}
```
La borne est essentielle : une file illimitée masque un consommateur trop lent jusqu'à l'OutOfMemory. Pour arrêter proprement, j'interromprais les consommateurs (take lève InterruptedException), ou j'enverrais une *poison pill*, un élément spécial qui signifie 'fin'. Pour plusieurs consommateurs, un ExecutorService qui lit la file convient très bien."

**Q : "Des tâches soumises à un ExecutorService échouent sans aucune trace dans les logs."**
R : "Typique d'un `submit()` dont personne ne lit le Future : l'exception est capturée dans le Future et jamais relevée. Avec `execute()`, elle serait au moins passée à l'`UncaughtExceptionHandler`.
Corrections possibles : récupérer et consommer les Future, entourer le corps de la tâche d'un try/catch qui logge, utiliser CompletableFuture avec un `exceptionally` ou `whenComplete` qui logge, ou surcharger `afterExecute` dans un ThreadPoolExecutor personnalisé. Et nommer les threads via une ThreadFactory pour que les logs disent d'où vient l'erreur."

**Q : "Comment tu conçois un cache de calculs coûteux accédé par de nombreux threads, sans calculer deux fois la même clé ?"**
R : "`ConcurrentHashMap.computeIfAbsent(cle, this::calculer)` garantit qu'une seule exécution a lieu par clé, les autres threads attendant le résultat. Attention : pendant le calcul, d'autres opérations sur la même zone de la map peuvent être bloquées, donc la fonction doit rester raisonnablement rapide et ne jamais modifier la map.
Pour un calcul long ou de l'I/O, je stockerais plutôt des `CompletableFuture` : `computeIfAbsent(cle, k -> CompletableFuture.supplyAsync(() -> calculer(k), executor))`. L'insertion est instantanée et tous les demandeurs partagent le même future.
Et en pratique, si j'ai besoin d'expiration et de taille maximale, Caffeine fait exactement ça avec `AsyncLoadingCache`."

═══════════════════════════════════════════
# 7. PROGRAMMATION RÉACTIVE
═══════════════════════════════════════════

### Vue d'ensemble

Le modèle classique "un thread par requête" est simple : le code s'écrit de haut en bas, et quand il attend la base ou une API, le thread attend aussi. Le problème est qu'un thread de plateforme coûte cher (environ 1 Mo de pile, une ressource de l'OS) : avec des milliers de connexions qui attendent surtout le réseau, on épuise les threads bien avant le CPU.

La programmation réactive répond par un modèle **non bloquant** : quelques threads (une *event loop*) traitent les événements au fil de l'eau, et le code décrit des **flux de données** sur lesquels on enchaîne des transformations. La donnée "arrive" et déclenche la suite. Le second apport est la **backpressure** : le consommateur indique au producteur combien d'éléments il peut absorber, ce qui évite d'être submergé.

Le prix est élevé : un style de code différent, des stack traces difficiles à lire, une chaîne entièrement non bloquante (un seul appel bloquant gèle l'event loop) et une courbe d'apprentissage réelle. Depuis les **virtual threads** (Java 21), une grande partie du bénéfice de scalabilité s'obtient avec du code impératif classique : la question "réactif ou pas ?" se pose donc différemment aujourd'hui.

### Cours

**Reactive Streams** : une spécification (intégrée au JDK depuis Java 9 sous `java.util.concurrent.Flow`) avec quatre interfaces : `Publisher` (émet), `Subscriber` (reçoit : `onSubscribe`, `onNext`, `onError`, `onComplete`), `Subscription` (`request(n)` pour la backpressure, `cancel()`), `Processor` (les deux à la fois). Un flux se termine par **exactement un** signal terminal : erreur ou complétion.

**Project Reactor** (utilisé par Spring WebFlux) :
- `Mono<T>` : 0 ou 1 élément. `Flux<T>` : 0 à N éléments.
- **Rien ne se passe avant `subscribe()`** : un pipeline réactif est une description, comme un stream sans opération terminale.
- Flux *cold* (chaque abonné rejoue la source, ex. une requête HTTP) vs *hot* (la source émet indépendamment des abonnés, ex. des événements ; `share()`, `publish()`, `Sinks`).

**Opérateurs essentiels** :
| Opérateur | Rôle |
|---|---|
| `map` | transformation synchrone 1 → 1 |
| `flatMap` | transformation asynchrone 1 → Publisher, **concurrente**, ordre non garanti |
| `concatMap` | comme flatMap mais **séquentiel**, ordre préservé |
| `switchMap` | annule le traitement précédent quand un nouvel élément arrive (autocomplétion) |
| `filter`, `take`, `skip`, `distinct` | sélection |
| `zip`, `zipWith` | combiner des sources élément par élément (attendre plusieurs appels) |
| `merge`, `concat` | fusionner des flux (entrelacé vs à la suite) |
| `buffer`, `window` | regrouper des éléments |
| `onErrorReturn`, `onErrorResume` | valeur ou flux de secours en cas d'erreur |
| `retryWhen(Retry.backoff(3, Duration.ofMillis(100)))` | réessayer avec backoff |
| `timeout` | échouer si pas de réponse dans le délai |
| `doOnNext`, `doOnError` | effets de bord pour le log (l'équivalent de peek) |

**Schedulers** : `Schedulers.parallel()` (calcul, taille = cœurs), `Schedulers.boundedElastic()` (pour **isoler du code bloquant** inévitable), `Schedulers.immediate()`. `subscribeOn` choisit le thread sur lequel la source s'exécute (quel que soit l'endroit où on l'écrit dans la chaîne) ; `publishOn` bascule les opérateurs **suivants** sur un autre scheduler.

**La règle d'or : ne jamais bloquer.** Un `Thread.sleep`, un appel JDBC classique ou un `block()` sur une event loop Netty immobilise un des rares threads et effondre le débit de toute l'application. Outils : `BlockHound` détecte les appels bloquants en test ; pour un appel bloquant inévitable, `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`.

**Stratégies de backpressure** quand le consommateur est plus lent : bufferiser (`onBackpressureBuffer`, avec une borne), abandonner (`onBackpressureDrop`), ne garder que le dernier (`onBackpressureLatest`), ou échouer (`onBackpressureError`).

**Spring WebFlux** : serveur Netty par défaut (event loop), contrôleurs annotés renvoyant des `Mono`/`Flux` ou endpoints fonctionnels (`RouterFunction`), client `WebClient`. Pour la base : **R2DBC** (pilotes réactifs) et Spring Data R2DBC ; JPA/JDBC sont bloquants et ne conviennent pas. Contexte : le `ThreadLocal` ne fonctionne plus (le traitement change de thread), Reactor propose un `Context` propagé dans la chaîne, et la bibliothèque *context-propagation* fait le pont pour le MDC et le tracing.

**Mutiny** (Quarkus) : `Uni<T>` (0 ou 1) et `Multi<T>` (flux), API orientée événements et lisible : `uni.onItem().transform(f)`, `uni.onFailure().retry().atMost(3)`, `uni.ifNoItem().after(Duration.ofSeconds(2)).fail()`. Quarkus distingue les threads d'**I/O** (event loop Vert.x, où on ne bloque jamais) des threads **worker** (`@Blocking`) ; bloquer sur l'event loop lève une `BlockingOperationNotAllowedException` ou un avertissement de thread bloqué.

**RxJava** : l'autre grande bibliothèque (Observable, Single, Maybe, Completable, Flowable), historiquement très utilisée sur Android.

**Débogage** : les stack traces réactives montrent les internes de la bibliothèque plutôt que votre code. Aides : `checkpoint("description")`, `Hooks.onOperatorDebug()` (coûteux, en dev), l'agent `ReactorDebugAgent`, des `log()` ponctuels.

**Réactif ou virtual threads ?**
| Critère | Réactif | Virtual threads |
|---|---|---|
| Style de code | déclaratif, chaînes d'opérateurs | impératif classique |
| Scalabilité I/O | excellente | excellente |
| Backpressure | native | à construire (sémaphores, files bornées) |
| Streaming, événements continus | très adapté | moins naturel |
| Débogage, stack traces | difficile | normal |
| Bibliothèques bloquantes (JDBC) | interdites | acceptées |

Synthèse : pour un service CRUD classique qui doit tenir beaucoup de connexions, les virtual threads donnent l'essentiel du gain sans réécrire le code. Le réactif garde l'avantage sur le **streaming** (SSE, WebSocket, flux Kafka), la composition fine d'événements et la backpressure de bout en bout.

### En situation

- **"Le service WebFlux s'effondre sous une charge modérée"** → un appel bloquant sur l'event loop.
- **"Faut-il passer en réactif pour tenir la charge ?"** → d'abord mesurer ; les virtual threads sont souvent suffisants.
- **"L'ordre des éléments n'est pas respecté"** → flatMap au lieu de concatMap.
- **"Le MDC disparaît des logs"** → ThreadLocal perdu au changement de thread, utiliser le Context.
- **"Rien ne s'exécute"** → pas de subscribe.

### Questions techniques

**Q : Qu'apporte la programmation réactive ?** R : Un modèle non bloquant qui sert beaucoup de requêtes avec peu de threads, et une backpressure native.

**Q : Les quatre interfaces de Reactive Streams ?** R : Publisher, Subscriber, Subscription, Processor.

**Q : C'est quoi la backpressure ?** R : Le mécanisme par lequel le consommateur demande au producteur un nombre limité d'éléments (request(n)).

**Q : Mono vs Flux ?** R : 0..1 élément vs 0..N éléments.

**Q : Que se passe-t-il si on ne s'abonne pas ?** R : Rien : le pipeline n'est qu'une description.

**Q : Cold vs hot publisher ?** R : Cold rejoue la source pour chaque abonné ; hot émet indépendamment des abonnés.

**Q : map vs flatMap vs concatMap ?** R : Transformation synchrone ; asynchrone concurrente sans ordre garanti ; asynchrone séquentielle qui préserve l'ordre.

**Q : À quoi sert switchMap ?** R : Annuler le traitement en cours quand un nouvel élément arrive (recherche à la frappe).

**Q : subscribeOn vs publishOn ?** R : subscribeOn choisit le thread d'exécution de la source ; publishOn change le thread des opérateurs suivants.

**Q : Que faire d'un appel bloquant inévitable dans un flux réactif ?** R : L'isoler sur Schedulers.boundedElastic().

**Q : Pourquoi ne jamais appeler block() dans un contrôleur WebFlux ?** R : Il immobilise un thread de l'event loop et effondre le débit.

**Q : Peut-on utiliser JPA avec WebFlux ?** R : Pas correctement, JPA est bloquant ; utiliser R2DBC ou isoler les appels.

**Q : Comment propager le contexte (MDC, trace) en réactif ?** R : Via le Context de Reactor et la bibliothèque context-propagation, pas via ThreadLocal.

**Q : Uni vs Multi ?** R : Les équivalents Mutiny de Mono et Flux.

**Q : Que signifie @Blocking en Quarkus ?** R : La méthode s'exécute sur un thread worker et non sur l'event loop.

**Q : Les virtual threads rendent-ils le réactif obsolète ?** R : Non, mais ils couvrent l'essentiel du besoin de scalabilité I/O ; le réactif garde l'avantage sur le streaming et la backpressure.

**Q : Comment déboguer un flux Reactor ?** R : checkpoint(), log(), Hooks.onOperatorDebug() en dev, ReactorDebugAgent.

### Questions de situation

**Q : "Un endpoint doit agréger trois appels de services en parallèle avec un timeout global."**
R : "En Reactor, `Mono.zip(appelA(), appelB(), appelC())` lance les trois en parallèle et attend les trois résultats. J'ajouterais un `timeout` sur chaque appel (pour qu'un service lent n'empoisonne pas les autres) et un `timeout` global, plus un `onErrorResume` par appel si une réponse partielle est acceptable fonctionnellement.
En impératif avec des virtual threads, l'équivalent est trois `CompletableFuture` sur un executor de virtual threads, puis `allOf(...).orTimeout(...)` ; ou la `StructuredTaskScope` si on accepte une API en preview, qui annule automatiquement les autres appels si l'un échoue."

**Q : "L'équipe hésite à migrer un service Spring MVC vers WebFlux pour la performance."**
R : "Je demanderais d'abord ce qu'on mesure comme limite. Si c'est le nombre de requêtes concurrentes qui attendent de l'I/O, activer les **virtual threads** dans Spring Boot (`spring.threads.virtual.enabled=true`) donne souvent le gain recherché sans toucher au code.
Une migration vers WebFlux implique de remplacer toute la chaîne bloquante (JPA par R2DBC, clients HTTP, bibliothèques), de former l'équipe et d'accepter un débogage plus difficile. Elle se justifie si le service fait du **streaming**, a besoin de backpressure de bout en bout, ou s'intègre dans une chaîne déjà réactive. Sinon c'est beaucoup de risque pour un gain qu'on peut obtenir autrement."

**Q : "Un service WebFlux devient très lent dès 50 requêtes simultanées alors que le CPU est bas."**
R : "CPU bas et débit effondré sur WebFlux, c'est le symptôme typique d'un **appel bloquant sur l'event loop** : quelques threads seulement, et chacun reste coincé à attendre.
Je prendrais un thread dump pour voir ce que font les threads `reactor-http-nio` : s'ils sont dans un appel JDBC, un `block()`, un client HTTP synchrone ou un `Thread.sleep`, le coupable est trouvé. BlockHound en test permet de le détecter systématiquement.
La correction est de rendre l'appel non bloquant, ou à défaut de l'isoler sur `boundedElastic`."

═══════════════════════════════════════════
# 8. MÉMOIRE & GARBAGE COLLECTOR
═══════════════════════════════════════════

### Vue d'ensemble

La JVM libère le développeur de la gestion manuelle de la mémoire, plus de `malloc`/`free`, plus de pointeurs qui traînent. En échange, il faut comprendre **comment elle gère cette mémoire**, parce que les problèmes qui en découlent (fuites, pauses de GC, OutOfMemory) sont parmi les plus courants en production.

Le modèle repose sur une **hypothèse générationnelle** : la grande majorité des objets meurent jeunes. La JVM en tire parti en séparant la mémoire en zones et en collectant beaucoup plus souvent la zone des jeunes objets, avec un algorithme optimisé pour ça. Comprendre cette logique explique la plupart des comportements observés en prod.

En environnement conteneurisé, une couche s'ajoute : la JVM et Kubernetes ont chacun leur vision de la mémoire disponible, et les faire coïncider est une source d'incidents classique.

### Cours

**Les zones mémoire :**
- **Heap** : tous les objets. Partagé entre threads, géré par le GC.
- **Stack** : une par thread. Contient les frames de méthodes (variables locales, références). Libérée automatiquement au retour de méthode. Un débordement donne `StackOverflowError` (typiquement une récursion infinie).
- **Metaspace** : métadonnées des classes. En mémoire **native** (hors heap) depuis Java 8, où il a remplacé le PermGen.
- **Hors-heap** également : les stacks de threads, les buffers directs (NIO), le code compilé par le JIT.

**Organisation du heap :**
- **Young Generation** : Eden (où naissent les objets) + deux Survivor spaces.
- **Old Generation** : les objets ayant survécu à plusieurs collectes.

**Cycle de vie d'un objet** : allocation dans Eden → un **Minor GC** copie les survivants dans un Survivor → après plusieurs survies, **promotion** en Old Gen → nettoyé plus tard par un **Major GC**, plus coûteux.

**Le Garbage Collector** libère les objets **inaccessibles** depuis les GC roots (variables locales actives, champs static, threads vivants). Un objet peut être référencé et pourtant inutile, c'est exactement ce qu'est une fuite mémoire en Java.

`System.gc()` n'est qu'une **suggestion**, jamais un ordre.

**Les algorithmes de GC :**
- **Serial** : mono-thread, petites applications.
- **Parallel** : optimisé pour le débit, pauses plus longues.
- **G1** (défaut depuis Java 9, et sur toutes les machines depuis Java 27) : découpe le heap en régions et cible un temps de pause maximal (`-XX:MaxGCPauseMillis`). Bon compromis général.
- **ZGC / Shenandoah** : pauses sous la milliseconde même sur de très gros heaps, en travaillant de façon concurrente à l'application.

**Fuites mémoire, les causes récurrentes :**
- Une collection `static` qui grossit sans purge.
- Un cache sans limite de taille ni TTL.
- Des listeners/callbacks jamais désenregistrés.
- Un `ThreadLocal` non nettoyé dans un pool de threads.
- Des ressources non fermées (d'où le try-with-resources systématique).
- Une classe interne non-static qui retient son instance englobante.

**Diagnostic** : `-XX:+HeapDumpOnOutOfMemoryError` pour capturer automatiquement, puis analyse avec Eclipse MAT (quelles classes occupent la mémoire, et surtout **quelles références les retiennent**).

**Options principales** : `-Xms` (heap initial), `-Xmx` (heap max), `-XX:+UseG1GC`, `-Xlog:gc*` (logs GC).

**En conteneur** : depuis Java 10, la JVM détecte les limites du conteneur. On peut utiliser `-XX:MaxRAMPercentage=75` plutôt qu'un `-Xmx` fixe, ce qui évite l'erreur classique d'un heap supérieur à la limite du pod. Il faut toujours laisser de la marge pour le hors-heap, sinon Kubernetes tue le conteneur (**OOMKilled**) alors que la JVM se croyait dans les clous.

**Comment fonctionne un GC, les briques de base.** Le *marquage* parcourt le graphe depuis les GC roots et marque les objets vivants. Ensuite trois stratégies : *sweep* (libérer ce qui n'est pas marqué, mais la mémoire se fragmente), *compact* (tasser les vivants, coûteux), *copy* (copier les vivants dans une zone vide, idéal quand peu survivent, d'où son usage dans la Young Generation). Une pause **stop-the-world** arrête tous les threads applicatifs ; elle a lieu à un **safepoint**. Les GC modernes font l'essentiel du travail en concurrence avec l'application et réduisent les pauses au minimum.

**Allocation rapide** : chaque thread alloue dans son propre tampon de l'Eden (**TLAB**, *thread-local allocation buffer*), simple incrément de pointeur sans synchronisation. C'est pourquoi allouer en Java est très rapide ; ce qui coûte, c'est la survie des objets.

**Escape analysis** : si le JIT prouve qu'un objet ne sort pas de la méthode, il peut le remplacer par ses champs en registres (*scalar replacement*) : l'objet n'est jamais alloué dans le heap.

**Les GC disponibles aujourd'hui** :
| GC | Activation | Profil |
|---|---|---|
| Serial | `-XX:+UseSerialGC` | mono-thread, petits heaps, conteneurs minuscules |
| Parallel | `-XX:+UseParallelGC` | débit maximal, pauses plus longues (batch) |
| G1 | défaut | compromis débit/latence, heap découpé en régions, pause cible |
| ZGC | `-XX:+UseZGC` | pauses sous la milliseconde jusqu'à des To de heap ; générationnel par défaut depuis Java 23, mode non générationnel supprimé en Java 24 |
| Shenandoah | `-XX:+UseShenandoahGC` | faibles pauses, compactage concurrent ; mode générationnel en production depuis Java 25 |
| Epsilon | `-XX:+UseEpsilonGC` | aucune collecte (tests de performance, programmes très courts) |

G1 était le défaut depuis Java 9, sauf sur les petites machines (moins de 2 CPU ou moins d'environ 1,8 Go de RAM) où la JVM choisissait Serial. Depuis Java 27 (JEP 523), G1 est le défaut **partout**. Point important en conteneur : avec une limite CPU à 1 cœur sur un JDK plus ancien, la JVM sélectionnait silencieusement Serial.

**G1 en détail** : heap découpé en régions de taille égale (1 à 32 Mo), chacune Eden, Survivor, Old ou *Humongous* (objets dépassant la moitié d'une région, alloués directement en Old : de gros tableaux fréquents dégradent G1). Collectes *young*, puis *mixed* (young + quelques régions Old les plus rentables, d'où "Garbage First"), marquage concurrent de l'Old. Réglage principal : `-XX:MaxGCPauseMillis` (200 ms par défaut), un objectif et pas une garantie.

**Les types de références** (`java.lang.ref`) :
- **Forte** : la référence normale, empêche la collecte.
- **Soft** (`SoftReference`) : collectée seulement si la mémoire manque ; pour des caches sensibles à la mémoire (comportement peu prévisible, on préfère un cache borné).
- **Weak** (`WeakReference`) : collectée au prochain GC si plus aucune référence forte ; base de `WeakHashMap`.
- **Phantom** (`PhantomReference`) : jamais déréférençable, sert à être notifié après la collecte pour libérer une ressource ; c'est ce qu'utilise `java.lang.ref.Cleaner` (Java 9), le remplaçant de `finalize()`.

**finalize()** : déprécié pour suppression (JEP 421, Java 18). Imprévisible (aucune garantie d'exécution), ralentit le GC, peut ressusciter des objets. On utilise try-with-resources et, en dernier filet, un Cleaner.

**Les variantes d'OutOfMemoryError** et ce qu'elles signifient :
- `Java heap space` : heap plein (fuite ou sous-dimensionnement).
- `GC overhead limit exceeded` : la JVM passe presque tout son temps à collecter sans rien récupérer.
- `Metaspace` : trop de classes chargées (fuite de class loaders, génération de classes dynamiques).
- `unable to create native thread` : limite de threads de l'OS ou mémoire native épuisée.
- `Direct buffer memory` : buffers NIO hors heap (`-XX:MaxDirectMemorySize`).
- `Requested array size exceeds VM limit`.
- Et hors JVM : le **OOMKilled** de Kubernetes ou l'OOM killer Linux, qui tuent le processus sans aucune trace côté Java.

**La mémoire native** : la consommation réelle d'une JVM = heap + metaspace + code cache (JIT) + piles de threads (environ 1 Mo chacune par défaut, `-Xss`) + buffers directs + structures internes du GC + bibliothèques natives. Le **Native Memory Tracking** (`-XX:NativeMemoryTracking=summary` puis `jcmd <pid> VM.native_memory summary`) détaille chaque poste.

**Compact Object Headers** : l'en-tête de chaque objet passe de 12 à 8 octets (JEP 519, produit en Java 25 avec `-XX:+UseCompactObjectHeaders`, **activé par défaut en Java 27** avec JEP 534). Gain typique de 10 à 20 % de heap sur des applications à petits objets.

**String deduplication** (`-XX:+UseStringDeduplication`, G1 et autres) : le GC fusionne les tableaux de caractères identiques de String distinctes.

**Lire un log GC** (`-Xlog:gc*:file=gc.log:time,uptime`) : fréquence et durée des pauses, quantité récupérée, niveau du heap après collecte. Outils : GCeasy, GCViewer, JDK Mission Control.

### En situation

- **"Le service consomme de plus en plus de mémoire"** → fuite vs sous-dimensionnement.
- **"Le pod est OOMKilled"** → heap supérieur à la limite, ou hors-heap oublié.
- **"L'application a des à-coups réguliers"** → pauses GC.
- **"Comment tu dimensionnes la mémoire ?"** → mesurer sous charge, marge hors-heap.

### Questions techniques

**Q : Heap vs stack ?** R : Heap = objets, partagé, géré par GC. Stack = frames de méthodes, une par thread.

**Q : C'est quoi le Metaspace ?** R : La zone native des métadonnées de classes (remplace PermGen depuis Java 8).

**Q : Sur quoi repose la séparation Young/Old Gen ?** R : L'hypothèse générationnelle : la plupart des objets meurent jeunes.

**Q : Quel GC par défaut et pourquoi ?** R : G1 depuis Java 9 : découpage en régions et pause cible configurable.

**Q : System.gc() force-t-il le GC ?** R : Non, c'est une suggestion.

**Q : Fuite mémoire malgré le GC ?** R : Des objets restent référencés inutilement (static, cache, listeners, ThreadLocal).

**Q : C'est quoi StackOverflowError ?** R : Un débordement de la pile, typiquement une récursion infinie.

**Q : MaxRAMPercentage ?** R : Fixe le heap en pourcentage de la mémoire disponible, pratique en conteneur.

**Q : C'est quoi une pause stop-the-world ?** R : Une phase où tous les threads applicatifs sont arrêtés (à un safepoint) pour que le GC travaille.

**Q : Mark-sweep vs mark-compact vs copying ?** R : Libérer sur place (fragmentation) / tasser les survivants / copier les survivants dans une zone vide.

**Q : Pourquoi l'allocation est-elle rapide en Java ?** R : Grâce aux TLAB : chaque thread alloue par simple incrément de pointeur dans son tampon.

**Q : C'est quoi l'escape analysis ?** R : Une optimisation du JIT qui évite d'allouer dans le heap un objet qui ne sort pas de la méthode.

**Q : Quel GC pour quelles contraintes ?** R : Parallel pour le débit, G1 en général, ZGC/Shenandoah pour des pauses minimales sur gros heap, Serial pour les très petites machines.

**Q : Quel est le GC par défaut en Java 27 ?** R : G1 dans tous les environnements (JEP 523) ; avant, Serial était choisi sur les petites machines.

**Q : C'est quoi un objet humongous dans G1 ?** R : Un objet dépassant la moitié d'une région, alloué directement en Old Generation.

**Q : Que contrôle MaxGCPauseMillis ?** R : L'objectif de durée de pause de G1 (200 ms par défaut), pas une garantie.

**Q : ZGC est-il générationnel ?** R : Oui par défaut depuis Java 23, et le mode non générationnel a été supprimé en Java 24.

**Q : Strong, soft, weak, phantom ?** R : Empêche la collecte / collectée si la mémoire manque / collectée au prochain GC / notification après collecte (Cleaner).

**Q : Par quoi remplacer finalize() ?** R : try-with-resources, et un Cleaner en dernier recours.

**Q : Les causes possibles d'OutOfMemoryError ?** R : Heap plein, GC overhead, Metaspace, threads natifs, mémoire directe, taille de tableau.

**Q : Que consomme une JVM en dehors du heap ?** R : Metaspace, code cache, piles de threads, buffers directs, structures du GC, bibliothèques natives.

**Q : Comment mesurer la mémoire native ?** R : Native Memory Tracking (-XX:NativeMemoryTracking=summary) et jcmd VM.native_memory.

**Q : C'est quoi les compact object headers ?** R : Des en-têtes d'objet de 8 octets au lieu de 12, par défaut depuis Java 27, qui réduisent l'occupation du heap.

**Q : Taille par défaut d'une pile de thread ?** R : Environ 512 Ko à 1 Mo selon la plateforme, réglable avec -Xss.

**Q : Pourquoi un OOMKilled ne laisse-t-il pas de trace Java ?** R : Le noyau tue le processus de l'extérieur ; la JVM n'a pas l'occasion de lever d'erreur ni de produire un heap dump.

**Q : Que faire en production lors d'un OutOfMemoryError ?** R : Capturer un heap dump (-XX:+HeapDumpOnOutOfMemoryError) et souvent arrêter la JVM (-XX:+ExitOnOutOfMemoryError) pour que l'orchestrateur la redémarre proprement.

### Questions de situation

**Q : "Ce service consomme de plus en plus de mémoire jusqu'à planter."**
R : "D'abord **qualifier** : fuite ou sous-dimensionnement ? Je regarde la courbe mémoire dans le temps. Une fuite dessine un escalier ascendant, le niveau bas après chaque GC monte progressivement. Un simple sous-dimensionnement oscille mais reste stable.
Si c'est une fuite, je **capture un heap dump**, idéalement automatiquement via `-XX:+HeapDumpOnOutOfMemoryError`, et je l'analyse avec Eclipse MAT. Ce qui m'intéresse n'est pas seulement quelles classes occupent la place, mais **quel chemin de références les retient** : c'est ça qui désigne le coupable.
Les suspects habituels : collection static qui grossit, cache sans limite ni TTL, listeners non désenregistrés, ThreadLocal non nettoyé dans un pool.
Et je vérifierais la configuration : un `-Xmx` supérieur à la limite mémoire du pod fait tuer le conteneur par Kubernetes. En conteneur, `-XX:MaxRAMPercentage` est plus sûr qu'une valeur fixe."

**Q : "Comment tu dimensionnes la mémoire d'un service en production ?"**
R : "Je mesure plutôt que d'estimer. Je fais tourner sous charge représentative et j'observe le **niveau bas du heap après GC** une fois stabilisé, c'est le vrai besoin, pas le pic qui inclut les objets en attente de collecte.
Ensuite je fixe le heap avec une marge, et surtout je m'assure que la limite du conteneur est **supérieure** au heap, parce que la JVM consomme aussi du hors-heap : metaspace, stacks de threads, buffers natifs, code compilé. Une règle empirique courante est de prévoir environ 25% au-dessus du heap.
Enfin je mets une alerte sur les OOMKilled et sur la durée cumulée des pauses GC, si le GC prend plus de quelques pourcents du temps, c'est le signe d'un heap trop petit ou d'un taux d'allocation trop élevé."

**Q : "Le pod est OOMKilled alors que le heap Java est loin d'être plein."**
R : "Si le heap n'est pas plein, c'est la **mémoire native** qui dépasse la limite du conteneur. Je listerais les postes : metaspace, piles de threads (des centaines de threads × 1 Mo), buffers directs (Netty, NIO), code cache, structures du GC.
J'activerais le Native Memory Tracking pour voir quel poste grossit. Les causes fréquentes : un `-Xmx` trop proche de la limite du pod, un nombre de threads qui explose, une bibliothèque qui alloue des buffers directs sans limite, ou une fuite de class loaders qui gonfle le metaspace.
Correction : réduire le heap (via `MaxRAMPercentage` autour de 70-75 %), borner ce qui peut l'être (`MaxMetaspaceSize`, `MaxDirectMemorySize`, taille des pools de threads) et garder une marge réelle entre la somme et la limite du conteneur."

**Q : "Les temps de réponse ont des pics réguliers de plusieurs centaines de millisecondes."**
R : "Des pics réguliers évoquent des **pauses GC**. Je commencerais par les logs GC (`-Xlog:gc*`) ou JFR pour corréler les pics avec des pauses.
Si ce sont des collectes Young fréquentes : le taux d'allocation est trop élevé, je profilerais les allocations pour trouver qui produit autant d'objets. Si ce sont des Full GC : l'Old Generation se remplit, fuite possible ou heap trop petit. Si c'est G1 avec des objets humongous : de gros tableaux alloués souvent.
Selon le diagnostic : réduire les allocations, agrandir le heap, ajuster la pause cible de G1, ou passer à **ZGC** si l'objectif est une latence très basse et qu'on accepte un léger surcoût CPU."

═══════════════════════════════════════════
# 9. JVM INTERNALS, PERFORMANCE & OUTILLAGE
═══════════════════════════════════════════

### Vue d'ensemble

La JVM est bien plus qu'un interpréteur : c'est un environnement d'exécution qui charge les classes à la demande, observe le code pendant qu'il tourne et le recompile en code natif optimisé à partir de ce qu'elle observe. C'est ce qui donne à Java des performances proches du natif sur la durée, au prix d'un démarrage plus lent et d'une phase de chauffe.

Pour un poste confirmé ou senior, on attend trois choses : comprendre le **chargement des classes** (d'où viennent les `ClassNotFoundException` et les conflits de versions), comprendre le **JIT** (pourquoi une application accélère avec le temps, pourquoi les micro-benchmarks mentent), et savoir **outiller un diagnostic** en production (thread dump, heap dump, JFR, profiler). Le sujet du **démarrage** (AOT, CDS, GraalVM natif, CRaC) est devenu central avec le cloud.

### Cours

**Chargement des classes.** Une classe est chargée **à la demande**, au premier usage actif. Trois chargeurs principaux, organisés hiérarchiquement :
- **Bootstrap** : classes du cœur du JDK (`java.base`), implémenté en natif.
- **Platform** (anciennement *Extension* avant Java 9) : autres modules du JDK.
- **Application** (ou *System*) : le classpath / module path de l'application.

Modèle de **délégation parent** : un chargeur demande d'abord à son parent ; il ne charge lui-même que si le parent échoue. Cela garantit qu'on ne remplace pas `java.lang.String` par une version maison. Les serveurs d'application et certains frameworks inversent localement ce modèle (*child-first*) pour isoler les applications.

Les phases : **chargement** (lire le bytecode, créer l'objet `Class`) → **liaison** (vérification du bytecode, préparation des champs static à leur valeur par défaut, résolution des références symboliques) → **initialisation** (exécution des initialiseurs static, une seule fois, de façon thread-safe).

Une classe est identifiée par son **nom + son chargeur** : la même classe chargée par deux chargeurs différents donne deux types incompatibles (`ClassCastException: X cannot be cast to X`). Les **fuites de class loaders** (une référence statique qui retient un chargeur après un redéploiement) remplissent le Metaspace.

**ClassNotFoundException vs NoClassDefFoundError** : la première est levée quand on charge dynamiquement une classe absente (`Class.forName`, réflexion). La seconde survient quand une classe présente à la **compilation** est introuvable à l'**exécution**, ou quand son initialisation statique a échoué précédemment (la première fois on obtient `ExceptionInInitializerError`, les fois suivantes `NoClassDefFoundError`). `NoSuchMethodError` signale une version de librairie différente entre compilation et exécution.

**ServiceLoader** (SPI) : mécanisme standard de découverte d'implémentations déclarées dans `META-INF/services/` ou via `provides` dans `module-info`. C'est ainsi que JDBC trouve les drivers ou que SLF4J trouve son implémentation.

**Bytecode.** Un jeu d'instructions pour une machine à pile, lisible avec `javap -c -v MaClasse.class`. Les instructions d'appel : `invokestatic`, `invokevirtual` (méthode d'instance, dispatch dynamique), `invokeinterface`, `invokespecial` (constructeurs, méthodes private, super), `invokedynamic` (Java 7, liaison différée, utilisé pour les lambdas, la concaténation de String et les records). Le *constant pool* regroupe les littéraux et références symboliques. Manipulation de bytecode : ASM, ByteBuddy, Javassist, et l'API standard **Class-File** (finalisée en Java 24, JEP 484).

**Java agents** : un jar chargé par `-javaagent:agent.jar` dont la méthode `premain` reçoit une `Instrumentation`, capable de transformer le bytecode des classes au chargement. C'est ainsi que fonctionnent les APM et l'agent OpenTelemetry Java : instrumentation sans modifier le code.

**Le JIT.** La JVM démarre en interprétant le bytecode et compte les invocations et les itérations de boucles. Au-delà de seuils, une méthode est compilée :
- **C1** (*client*) : compile vite, optimisations légères, et insère du profilage.
- **C2** (*server*) : compile lentement, optimisations agressives à partir du profil observé.
- **Compilation étagée** (*tiered*, défaut) : interprété → C1 → C2 selon la chaleur du code.

Optimisations principales : **inlining** (la plus importante, elle ouvre toutes les autres), dévirtualisation (un appel polymorphe qui ne voit qu'un type devient un appel direct), escape analysis, déroulage de boucles, élimination de code mort et de vérifications de bornes, élision et fusion de verrous, *intrinsics* (méthodes remplacées par des instructions CPU dédiées). Si une hypothèse devient fausse (un nouveau type apparaît), le code est **désoptimisé** et retombe en interprété. **OSR** (*on-stack replacement*) permet de compiler une boucle longue pendant qu'elle tourne.

Conséquences pratiques : il y a une **phase de chauffe** ; les premières requêtes après un déploiement sont plus lentes ; un benchmark naïf (une boucle chronométrée avec `System.nanoTime`) mesure n'importe quoi : code éliminé car son résultat est inutilisé, compilation en cours de mesure, OSR.

**JMH** (*Java Microbenchmark Harness*) est l'outil de référence : `@Benchmark`, itérations de chauffe (`@Warmup`), plusieurs JVM (`@Fork`), `Blackhole` pour consommer les résultats et empêcher l'élimination de code mort, modes (débit, temps moyen).

**Le démarrage, enjeu du cloud.**
- **CDS / AppCDS** (*Class Data Sharing*) : archive des métadonnées de classes déjà chargées, réutilisée au démarrage suivant (et partageable entre JVM).
- **Projet Leyden / AOT cache** : Java 24 (JEP 483) met en cache les classes chargées et liées lors d'une exécution d'entraînement ; Java 25 simplifie la création du cache (JEP 514) et y ajoute les profils de méthodes (JEP 515) pour accélérer la chauffe du JIT ; Java 26 (JEP 516) étend le cache d'objets à tous les GC. Gain de démarrage significatif sans changer le modèle de programmation.
- **CRaC** (*Coordinated Restore at Checkpoint*) : prendre un instantané d'une JVM chaude et la restaurer en quelques millisecondes (proposé par certaines distributions, supporté par Spring) ; AWS Lambda SnapStart repose sur la même idée.
- **GraalVM Native Image** : compilation AOT de toute l'application en un exécutable natif. Hypothèse du **monde fermé** : tout le code atteignable est déterminé au build par analyse statique. Démarrage en millisecondes et mémoire réduite ; mais build long et gourmand, réflexion/proxys/ressources/sérialisation à **déclarer** (métadonnées d'accessibilité, que Quarkus et Spring AOT génèrent en grande partie), pas de JIT (performances de pointe inférieures sans optimisation guidée par profil), et certaines bibliothèques incompatibles. Pertinent pour les CLI, le serverless et le *scale-to-zero*.

**Disposition des objets en mémoire** : un en-tête (*mark word* : hash, état du verrou, âge GC ; et un pointeur de classe), puis les champs, avec un alignement sur 8 octets. Sur un heap de moins de 32 Go, les références sont compressées sur 32 bits (*compressed oops*) : passer de 31 à 33 Go de heap peut donc réduire la mémoire utile. Un `Integer` occupe environ 16 octets contre 4 pour un `int`. Avec les compact object headers (défaut en Java 27), l'en-tête passe à 8 octets.

**Outils de diagnostic du JDK** :
| Outil | Usage |
|---|---|
| `jps` | lister les JVM en cours |
| `jcmd <pid> ...` | le couteau suisse : `Thread.print`, `GC.heap_dump`, `GC.class_histogram`, `VM.flags`, `VM.native_memory`, `JFR.start` |
| `jstack` | thread dump |
| `jmap` | heap dump, histogramme d'objets |
| `jstat -gcutil <pid> 1s` | statistiques GC en continu |
| `jinfo` | flags et propriétés d'une JVM |
| `jconsole`, VisualVM | supervision graphique via JMX |
| **JFR** (Java Flight Recorder) | enregistrement continu à très faible surcoût (environ 1 %), utilisable en production |
| JDK Mission Control | analyse des enregistrements JFR |
| async-profiler | profilage CPU/allocations/verrous, *flame graphs* |
| Eclipse MAT | analyse de heap dumps (dominators, chemins vers les GC roots) |

Un **thread dump** se lit en cherchant : les threads BLOCKED et le verrou qu'ils attendent, les deadlocks signalés en fin de dump, les piles identiques répétées (beaucoup de threads au même endroit = goulot), les threads d'un pool tous en attente réseau. On en prend **plusieurs à quelques secondes d'intervalle** pour distinguer un état figé d'un état transitoire.

Un **flame graph** se lit en largeur : plus une boîte est large, plus la méthode (et ce qu'elle appelle) consomme ; on cherche les plateaux larges en haut des piles.

**Flags JVM utiles** : `-Xms`/`-Xmx` (heap), `-Xss` (pile), `-XX:MaxRAMPercentage`, `-XX:MaxMetaspaceSize`, `-XX:+UseZGC` / `-XX:+UseG1GC`, `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps`, `-XX:+ExitOnOutOfMemoryError`, `-Xlog:gc*`, `-XX:ActiveProcessorCount=n` (forcer le nombre de CPU vus), `-XX:+PrintFlagsFinal` (toutes les valeurs effectives), `-XX:StartFlightRecording`. Les options `-X` sont non standard, les `-XX` avancées et susceptibles de changer.

**Distributions du JDK** : OpenJDK est le code source de référence ; les builds courantes sont Eclipse Temurin, Amazon Corretto, Azul Zulu, Microsoft Build of OpenJDK, Red Hat build of OpenJDK, BellSoft Liberica, ainsi qu'Oracle OpenJDK et Oracle JDK (dont les conditions de licence ont varié au fil des années, d'où l'usage fréquent de builds OpenJDK gratuites en entreprise). GraalVM est une JVM et une chaîne de compilation native. Autres langages de la JVM : Kotlin, Scala, Groovy, Clojure.

**Méthode de diagnostic de performance** : symptôme → métriques (CPU, GC, threads, latence p99) → hypothèse → outil adapté (profiler CPU si CPU haut, profil d'allocation si GC élevé, thread dump si CPU bas et latence haute, JFR pour une vue d'ensemble) → correction → nouvelle mesure. CPU bas + latence haute = on **attend** quelque chose (verrou, I/O, pool épuisé) ; CPU haut = on **calcule** trop (ou le GC tourne en boucle).

### En situation

- **"NoClassDefFoundError en production, pas en local"** → dépendance absente du packaging, ou initialisation statique en échec.
- **"Les premières requêtes après déploiement sont lentes"** → chauffe du JIT → warm-up, AOT cache, CDS.
- **"Mon benchmark montre que X est 100 fois plus rapide"** → probable élimination de code mort → JMH.
- **"CPU à 100 % en production"** → profiler (async-profiler, JFR) ou thread dumps répétés.
- **"Démarrage trop lent pour l'autoscaling"** → AOT cache, CRaC, natif, ou framework build-time.

### Questions techniques

**Q : Les trois class loaders principaux ?** R : Bootstrap, Platform (ex-Extension), Application (System).

**Q : C'est quoi la délégation parent ?** R : Un chargeur demande d'abord à son parent et ne charge lui-même qu'en cas d'échec, ce qui protège les classes du JDK.

**Q : Les phases du chargement de classe ?** R : Chargement, liaison (vérification, préparation, résolution), initialisation.

**Q : Quand une classe est-elle initialisée ?** R : Au premier usage actif : instanciation, appel d'une méthode static, accès à un champ static non constant, réflexion, initialisation d'une sous-classe.

**Q : ClassNotFoundException vs NoClassDefFoundError ?** R : Chargement dynamique d'une classe absente vs classe connue à la compilation mais introuvable (ou en échec d'initialisation) à l'exécution.

**Q : Que signifie `X cannot be cast to X` ?** R : La même classe a été chargée par deux class loaders différents.

**Q : C'est quoi une fuite de class loader ?** R : Un chargeur retenu par une référence (thread, static, ThreadLocal) après un redéploiement : ses classes restent en Metaspace.

**Q : À quoi sert invokedynamic ?** R : À lier un appel à l'exécution ; utilisé pour les lambdas, la concaténation de String, les records.

**Q : Comment lire le bytecode d'une classe ?** R : `javap -c -v`.

**Q : C'est quoi un java agent ?** R : Un jar chargé avec -javaagent qui peut transformer le bytecode au chargement (APM, OpenTelemetry).

**Q : C1 vs C2 ?** R : C1 compile vite avec peu d'optimisations ; C2 compile lentement avec des optimisations agressives basées sur le profil.

**Q : C'est quoi la compilation étagée ?** R : Le passage progressif interprété → C1 → C2 selon la chaleur du code.

**Q : Quelle est l'optimisation JIT la plus importante ?** R : L'inlining, qui permet toutes les autres.

**Q : C'est quoi une désoptimisation ?** R : Le retour au code interprété quand une hypothèse du JIT devient fausse.

**Q : Pourquoi une application Java a-t-elle une phase de chauffe ?** R : Le code commence interprété et n'est compilé et optimisé qu'après avoir été observé.

**Q : Pourquoi utiliser JMH ?** R : Pour neutraliser les pièges des micro-benchmarks : chauffe, élimination de code mort, OSR, variance entre JVM.

**Q : C'est quoi CDS ?** R : Class Data Sharing : une archive de métadonnées de classes réutilisée pour accélérer le démarrage.

**Q : Qu'apporte le projet Leyden ?** R : Un cache AOT des classes chargées, liées et des profils, produit par une exécution d'entraînement (Java 24 à 26).

**Q : C'est quoi l'hypothèse du monde fermé de GraalVM ?** R : Tout le code atteignable est connu au build ; la réflexion et le chargement dynamique doivent être déclarés.

**Q : Inconvénients du natif GraalVM ?** R : Build long, configuration de la réflexion, pas de JIT donc performances de pointe moindres, compatibilité des bibliothèques.

**Q : C'est quoi CRaC ?** R : Checkpoint/restore d'une JVM chaude pour redémarrer en millisecondes.

**Q : C'est quoi les compressed oops ?** R : Des références sur 32 bits pour des heaps de moins de 32 Go, qui économisent de la mémoire.

**Q : Quel outil pour un thread dump ? Un heap dump ?** R : jcmd Thread.print ou jstack ; jcmd GC.heap_dump ou jmap.

**Q : Pourquoi prendre plusieurs thread dumps ?** R : Pour distinguer un blocage durable d'un état transitoire.

**Q : C'est quoi JFR ?** R : Java Flight Recorder, un enregistreur d'événements de la JVM à très faible surcoût, utilisable en production.

**Q : Comment lire un flame graph ?** R : La largeur d'une boîte représente le temps passé ; on cherche les plateaux larges.

**Q : CPU bas et latence haute, ça évoque quoi ?** R : Une attente : verrou, I/O, pool de connexions épuisé.

**Q : Qu'affiche -XX:+PrintFlagsFinal ?** R : Toutes les valeurs effectives des flags de la JVM.

**Q : Différence OpenJDK / Oracle JDK / Temurin ?** R : OpenJDK est le projet source ; Oracle JDK et Temurin (et Corretto, Zulu...) en sont des distributions, avec des licences et des supports différents.

### Questions de situation

**Q : "En production, le CPU est à 100 % sur une instance. Comment tu trouves la cause ?"**
R : "D'abord savoir **qui** consomme : est-ce le code applicatif ou le GC ? `jstat -gcutil` ou les métriques GC le disent vite. Si le GC tourne en boucle, c'est un problème mémoire (heap trop petit, fuite) et je bascule sur le diagnostic mémoire.
Si c'est l'application, je profile : **async-profiler** en mode CPU pendant 30 secondes produit un flame graph, ou un enregistrement **JFR** à faible surcoût. À défaut, plusieurs thread dumps à quelques secondes d'intervalle, croisés avec `top -H` pour identifier les threads les plus consommateurs (l'identifiant natif `nid` du dump en hexadécimal correspond au TID).
Les coupables classiques : une boucle infinie, une regex au backtracking catastrophique, une sérialisation énorme, un algorithme quadratique sur des données qui ont grossi, des logs excessifs."

**Q : "Le démarrage de ton service prend 25 secondes, ce qui pénalise l'autoscaling."**
R : "Je mesurerais d'abord où passe le temps : chargement des classes, initialisation des beans, connexions aux dépendances, migrations de base au démarrage. Spring Boot fournit un suivi du démarrage (`ApplicationStartup` / l'endpoint startup d'Actuator).
Ensuite plusieurs leviers, du moins au plus structurant : supprimer les initialisations inutiles ou les rendre paresseuses, sortir les migrations de schéma du démarrage applicatif, activer **CDS ou le cache AOT** du JDK (gain important sans changer le code), utiliser **CRaC** si l'environnement le permet, et enfin la compilation **native** (Spring AOT + GraalVM, ou Quarkus) qui ramène le démarrage à moins d'une seconde mais demande un travail de compatibilité.
Je n'oublierais pas la readiness probe : même rapide, le pod ne doit recevoir du trafic qu'une fois prêt."

**Q : "Un collègue a écrit un benchmark montrant qu'une méthode est 50 fois plus rapide après sa modification."**
R : "Je serais prudent : un gain de ce facteur sur une micro-optimisation est souvent un artefact. Le JIT a pu éliminer le code dont le résultat n'est jamais utilisé, la mesure a pu inclure la compilation, ou les deux versions ont tourné dans la même JVM avec des profils différents.
Je referais la mesure avec **JMH** : plusieurs forks, des itérations de chauffe, un `Blackhole` pour consommer les résultats. Et surtout, je vérifierais que cette méthode compte dans le temps de réponse réel : un gain de 50 fois sur 0,1 % du temps total ne change rien pour l'utilisateur."

═══════════════════════════════════════════
# 10. EXCEPTIONS
═══════════════════════════════════════════

### Vue d'ensemble

Le mécanisme d'exceptions sépare le **chemin nominal** du **traitement des erreurs**. Sans lui, chaque appel devrait retourner un code d'erreur et chaque appelant le vérifier, c'est le modèle du C, verbeux et facile à ignorer. Avec les exceptions, l'erreur remonte automatiquement jusqu'à un niveau capable de la traiter.

Java a fait un choix unique parmi les grands langages : les **checked exceptions**, que le compilateur oblige à gérer. L'intention était bonne (rendre impossible d'ignorer une erreur prévisible) mais l'expérience a montré des effets pervers : signatures alourdies, propagation en cascade, et surtout des `catch` vides écrits pour faire taire le compilateur. C'est pourquoi les frameworks modernes (Spring, Quarkus) privilégient les unchecked avec une gestion centralisée.

La question clé en entretien n'est pas de réciter la hiérarchie, mais de savoir **où** traiter une erreur et **quoi** en faire.

### Cours

**Hiérarchie :**
```
Throwable
├── Error              → problèmes JVM graves, on ne catche pas
│   ├── OutOfMemoryError
│   └── StackOverflowError
└── Exception
    ├── (checked)      → IOException, SQLException...
    └── RuntimeException (unchecked)
        ├── NullPointerException
        ├── IllegalArgumentException
        ├── IllegalStateException
        └── IndexOutOfBoundsException
```

**Checked** : le compilateur impose de les catcher ou de les déclarer. Elles représentent des conditions **externes prévisibles** (fichier absent, réseau coupé).
**Unchecked** : aucune contrainte. Elles représentent des **erreurs de programmation** (null non vérifié, argument invalide).

**Ordre des catch** : du plus **spécifique** au plus **général**. Un catch général placé avant un catch spécifique rend ce dernier inatteignable, et Java refuse de compiler.

**Multi-catch** : `catch (IOException | SQLException e)` pour un traitement commun.

**finally** : toujours exécuté, même en cas de return dans le try (il s'exécute avant que le return soit effectif). Ne jamais faire de `return` dans un finally : il écraserait silencieusement celui du try, y compris une exception en cours.

**try-with-resources** : ferme automatiquement toute ressource implémentant `AutoCloseable`, dans l'ordre inverse de déclaration. Si une exception survient dans le try **et** dans le close, celle du close est attachée comme **suppressed** à l'exception principale (`getSuppressed()`).

**Exceptions personnalisées** : créer des exceptions métier explicites (`UserNotFoundException`, `InsufficientFundsException`) rend le code auto-documenté et permet un mapping précis vers des codes HTTP.

**Chaînage** : quand on enveloppe une exception, toujours passer la cause, `throw new ServiceException("msg", e)`, sinon on perd la trace d'origine, qui est justement l'information utile.

**Anti-patterns majeurs :**
- `catch (Exception e) {}` : avale l'erreur, aucun diagnostic possible.
- Catcher trop large : on attrape aussi les bugs qu'on voulait voir échouer.
- `log.error(e.getMessage())` au lieu de `log.error("msg", e)` : on perd le stacktrace.
- Utiliser les exceptions pour du contrôle de flux normal : c'est coûteux et illisible.

**Fail fast** : valider les préconditions en début de méthode et lever immédiatement. Une erreur détectée près de sa cause est infiniment plus facile à corriger qu'une erreur qui se manifeste trois couches plus loin.

**throw vs throws** : `throw` lève une exception ; `throws` déclare dans la signature les exceptions checked qu'une méthode peut propager.

**Les exceptions courantes à reconnaître** : `NullPointerException`, `IllegalArgumentException` (argument invalide), `IllegalStateException` (objet dans un état qui ne permet pas l'opération), `UnsupportedOperationException` (collection immuable), `ArrayIndexOutOfBoundsException` / `IndexOutOfBoundsException`, `ClassCastException`, `NumberFormatException` (sous-classe d'IllegalArgumentException), `ArithmeticException`, `ConcurrentModificationException`, `IOException` et `UncheckedIOException`, `InterruptedException`, `ExecutionException`, `TimeoutException`, `SQLException`. Côté Error : `OutOfMemoryError`, `StackOverflowError`, `NoClassDefFoundError`, `ExceptionInInitializerError`, `AssertionError`.

**Helpful NullPointerException** (Java 14, activé par défaut depuis 15) : le message indique exactement ce qui était null, par exemple `Cannot invoke "String.length()" because "user.getName()" is null`.

**try-with-resources, compléments** : depuis Java 9, on peut y placer une variable déjà déclarée si elle est effectivement finale (`try (reader) { ... }`). On peut combiner plusieurs ressources séparées par `;`. Implémenter `AutoCloseable` (dont `close()` peut lever `Exception`) ou `Closeable` (`IOException`, et `close()` doit être idempotent).

**Quand finally ne s'exécute pas** : `System.exit()`, crash de la JVM, `kill -9`, thread démon tué à l'arrêt de la JVM, boucle infinie dans le try.

**Exceptions et redéfinition** : une méthode redéfinie ne peut pas déclarer d'exception checked nouvelle ou plus large que celle du parent ; elle peut en déclarer moins, ou des unchecked.

**Exception dans un initialiseur static** : la première tentative lève `ExceptionInInitializerError`, la classe est marquée en échec, et toute utilisation ultérieure lève `NoClassDefFoundError`. Diagnostic trompeur : il faut chercher la première erreur dans les logs.

**Coût des exceptions** : la construction d'une exception remplit la stack trace (`fillInStackTrace`), ce qui est coûteux avec des piles profondes. Raison de plus pour ne pas les utiliser comme contrôle de flux. Le JIT peut aussi, pour des exceptions très fréquentes, omettre la stack trace (`-XX:-OmitStackTraceInFastThrow` pour la conserver), d'où des NPE sans pile dans des logs de production.

**Exceptions checked dans les lambdas** : impossible de lever une IOException depuis un `Function` ; on l'enveloppe dans une `UncheckedIOException` ou on écrit une interface fonctionnelle qui déclare l'exception.

**Traduire les exceptions aux frontières** : une couche ne doit pas exposer les exceptions techniques de la couche inférieure (une `SQLException` qui remonte jusqu'au contrôleur). Spring le fait avec sa hiérarchie `DataAccessException` (unchecked) ; dans le code métier, on enveloppe dans une exception du domaine en conservant la cause.

**Alternative aux exceptions pour les erreurs métier attendues** : un type résultat explicite, par exemple une interface sealed `Result<T>` avec `Success<T>` et `Failure`, traité par un switch exhaustif. Plus verbeux, mais l'appelant ne peut pas ignorer l'échec. Utile quand l'échec est un cas normal (validation de saisie), moins quand c'est exceptionnel.

**Format d'erreur HTTP standard** : RFC 9457 (*Problem Details*, qui remplace la RFC 7807) avec `type`, `title`, `status`, `detail`, `instance` et des extensions. Spring (6+) le supporte via `ProblemDetail` et `ErrorResponse`.

**Assertions** : `assert condition : message;` n'est actif qu'avec `-ea`, donc jamais pour valider des entrées en production ; on utilise `Objects.requireNonNull(x, "message")` et des exceptions explicites.

### En situation

- **"On perd des erreurs"** → exceptions avalées, logs sans stacktrace.
- **"Comment structurer la gestion d'erreur d'une API ?"** → exceptions métier + handler global + format cohérent.
- **"Ce code fuit des connexions"** → try-with-resources.
- **"Checked ou unchecked ?"** → position argumentée.

### Questions techniques

**Q : Checked vs unchecked ?** R : Checked = obligation de gérer, conditions externes. Unchecked = erreurs de programmation.

**Q : Error vs Exception ?** R : Error = grave, non récupérable. Exception = potentiellement gérable.

**Q : Ordre des catch ?** R : Du plus spécifique au plus général, sinon erreur de compilation.

**Q : finally et return ?** R : finally s'exécute avant que le return soit effectif ; un return dans finally écrase celui du try.

**Q : Exception dans try ET close ?** R : Celle du close devient suppressed, accessible via getSuppressed().

**Q : Pourquoi chaîner les exceptions ?** R : Pour conserver la cause d'origine et son stacktrace.

**Q : C'est quoi le fail fast ?** R : Valider tôt et lever immédiatement, pour détecter l'erreur près de sa cause.

**Q : throw vs throws ?** R : throw lève une exception ; throws déclare celles qu'une méthode peut propager.

**Q : NumberFormatException est-elle checked ?** R : Non, c'est une sous-classe d'IllegalArgumentException, donc unchecked.

**Q : IllegalArgumentException vs IllegalStateException ?** R : Argument invalide vs état de l'objet incompatible avec l'opération.

**Q : Qu'est-ce qu'une helpful NPE ?** R : Un message de NPE qui précise quelle expression était null (Java 14+).

**Q : finally s'exécute-t-il toujours ?** R : Non : pas en cas de System.exit, crash, kill -9, ou thread démon tué.

**Q : Une méthode redéfinie peut-elle lever une exception checked plus large ?** R : Non, seulement la même, une sous-classe, aucune, ou des unchecked.

**Q : Que se passe-t-il si un bloc static lève une exception ?** R : ExceptionInInitializerError la première fois, puis NoClassDefFoundError à chaque usage.

**Q : Pourquoi les exceptions sont-elles coûteuses ?** R : Principalement à cause du remplissage de la stack trace.

**Q : Pourquoi voit-on parfois des NPE sans stack trace en production ?** R : Le JIT omet la pile des exceptions très fréquentes (OmitStackTraceInFastThrow).

**Q : AutoCloseable vs Closeable ?** R : Closeable étend AutoCloseable, lève IOException et doit avoir un close idempotent.

**Q : Comment lever une exception checked depuis une lambda ?** R : L'envelopper dans une unchecked (UncheckedIOException) ou définir une interface fonctionnelle qui la déclare.

**Q : C'est quoi la RFC 9457 ?** R : Le format standard Problem Details pour les erreurs d'API HTTP (successeur de la RFC 7807).

**Q : Faut-il utiliser assert pour valider des entrées ?** R : Non, les assertions sont désactivées par défaut ; utiliser des exceptions ou Objects.requireNonNull.

**Q : Peut-on catcher une Error ?** R : Techniquement oui, mais on ne sait généralement pas s'en remettre ; à éviter sauf pour logger avant de laisser remonter.

### Questions de situation

**Q : "Comment tu structures la gestion des erreurs dans une API REST ?"**
R : "Sur trois niveaux.
D'abord des **exceptions métier explicites** (`UserNotFoundException`, `InsufficientStockException`) plutôt que des exceptions génériques. Le code devient auto-documenté et chaque cas se mappe naturellement sur un statut HTTP.
Ensuite un **handler global** : `@ControllerAdvice` en Spring, `ExceptionMapper` en JAX-RS. Il centralise la traduction exception → réponse HTTP, ce qui évite d'avoir des try/catch dans chaque contrôleur.
Enfin un **format d'erreur cohérent** : un code applicatif stable (pour que le client puisse réagir programmatiquement), un message lisible, et un identifiant de corrélation permettant de retrouver la trace complète dans les logs.
Point non négociable : **jamais de stacktrace exposée** en production, elle révèle technos, versions et chemins internes. Le détail va dans les logs serveur, le client reçoit un message générique."

**Q : "Un collègue écrit `catch (Exception e) {}` un peu partout."**
R : "C'est le pire anti-pattern de gestion d'erreur, pour deux raisons cumulées.
Il **avale** l'erreur : le problème se produit, personne ne le sait, et on obtient des comportements incohérents impossibles à diagnostiquer parce qu'il ne reste aucune trace.
Et il attrape **trop large** : un `catch (Exception)` capture aussi les bugs de programmation comme les NullPointerException, qu'on aurait voulu voir échouer bruyamment plutôt que masquer.
Ce que je proposerais : catcher uniquement les exceptions qu'on sait effectivement traiter, laisser remonter le reste vers le handler global, et si on catch, toujours logger avec le stacktrace, `log.error(\"contexte\", e)` et pas `e.getMessage()`, sinon on perd l'information qui sert vraiment."

**Q : "Checked ou unchecked, tu es de quel bord ?"**
R : "Les deux ont une logique. Les checked forcent l'appelant à traiter le cas, ce qui est vertueux sur le papier.
Mais en pratique elles alourdissent les signatures, se propagent en cascade jusqu'à polluer des couches qui n'ont rien à voir, et finissent souvent enveloppées, ou pire, avalées, ce qui va exactement à l'encontre de l'intention.
La tendance moderne, celle de Spring et Quarkus, privilégie les unchecked avec une gestion centralisée. C'est l'approche que je suis : des exceptions métier unchecked, un handler global, et un compilateur qui ne pollue pas chaque signature.
Ce qui compte au fond, ce n'est pas qu'une erreur soit **déclarée** partout, c'est qu'elle soit **visible et traitée** quelque part."

**Q : "Une erreur intermittente apparaît dans les logs comme une NullPointerException sans aucune stack trace."**
R : "C'est l'optimisation du JIT qui, pour une exception levée très souvent au même endroit, finit par réutiliser une instance préallouée sans pile. Il faut chercher la **première occurrence** dans les logs, qui elle avait sa stack trace, ou redémarrer avec `-XX:-OmitStackTraceInFastThrow` pour la conserver.
Et la fréquence elle-même est une information : une NPE levée assez souvent pour que le JIT l'optimise, c'est un vrai bug, pas un incident isolé."

**Q : "Comment tu gères une erreur métier attendue, par exemple un solde insuffisant ?"**
R : "Je distinguerais les cas. Si l'échec doit interrompre le traitement et remonter jusqu'à l'API, une **exception métier** unchecked explicite (`SoldeInsuffisantException`) avec un handler global qui la traduit en 409 ou 422 avec un corps Problem Details.
Si l'échec est un résultat normal que l'appelant **doit** traiter (une validation de formulaire avec plusieurs erreurs, par exemple), je préfère un type de retour explicite, une interface sealed `Resultat` avec `Succes` et `Echec`, parce que le compilateur force à traiter les deux cas.
Dans tous les cas : un code d'erreur stable pour le client, pas de stack trace exposée, et un identifiant de corrélation pour retrouver la trace."

═══════════════════════════════════════════
# 11. GENERICS
═══════════════════════════════════════════

### Vue d'ensemble

Avant Java 5, une `List` contenait des `Object` : on devait caster à la sortie, et une erreur de type ne se découvrait qu'à l'exécution, sous forme de `ClassCastException`. Les generics déplacent cette vérification **à la compilation** : le compilateur garantit qu'une `List<String>` ne contiendra jamais autre chose.

Le compromis retenu par Java s'appelle le **type erasure** : pour rester compatible avec le code existant, les informations de type générique sont effacées après compilation. Cela explique la plupart des limitations surprenantes qu'on rencontre, impossible d'instancier `new T()`, de tester `instanceof List<String>`, ou de surcharger deux méthodes qui ne diffèrent que par leur paramètre générique.

### Cours

**Classe générique** : `class Box<T> { T content; }`. **Méthode générique** : `<T> T identity(T x)`, le paramètre de type se déclare avant le type de retour.

**Bounded types** : `<T extends Number>` restreint T à Number ou ses sous-types, ce qui permet d'appeler les méthodes de Number. Bornes multiples : `<T extends Comparable<T> & Serializable>`.

**Wildcards** :
- `List<?>` : liste de type inconnu, on peut lire des Object, pas écrire.
- `List<? extends Number>` : Number ou sous-type. On peut **lire** un Number, mais pas écrire (on ne sait pas quel sous-type exact).
- `List<? super Integer>` : Integer ou super-type. On peut **écrire** un Integer, mais la lecture ne donne que des Object.

**PECS** : *Producer Extends, Consumer Super*. Si la structure te **produit** des données (tu lis), utilise `extends`. Si elle **consomme** tes données (tu écris), utilise `super`.

**Type erasure** : à l'exécution, `List<String>` et `List<Integer>` sont le même type brut `List`. Conséquences :
- `new T()` et `new T[]` sont interdits.
- `instanceof List<String>` est interdit.
- Deux méthodes `f(List<String>)` et `f(List<Integer>)` ont la même signature après effacement → ne compilent pas ensemble.
- Contournement courant : passer un `Class<T>` en paramètre quand on a besoin du type à l'exécution.

**Invariance des generics vs covariance des tableaux.** `String[]` est un sous-type d'`Object[]` (covariance, vérifiée à l'exécution par ArrayStoreException), mais `List<String>` n'est **pas** un sous-type de `List<Object>` (invariance, vérifiée à la compilation). Si c'était le cas, on pourrait ajouter un Integer dans une liste de String. Les wildcards (`? extends`, `? super`) réintroduisent une variance contrôlée.

**Raw types** : `List` sans paramètre, conservé pour la compatibilité avec le code d'avant Java 5. On perd toute vérification ; le compilateur émet un avertissement *unchecked*. À ne jamais utiliser dans du code neuf.

**Pollution du heap** : une variable de type paramétré qui référence un objet d'un autre type (via raw types ou casts non vérifiés). L'erreur n'apparaît que plus tard, sous forme de ClassCastException à un endroit sans rapport. Les varargs génériques en sont une source classique, d'où `@SafeVarargs` pour certifier qu'une méthode ne fait rien de dangereux avec son tableau.

**Méthodes génériques et inférence** : `Collections.<String>emptyList()` (type explicite) vs `Collections.emptyList()` (inféré depuis le contexte). L'opérateur diamant `new ArrayList<>()` (Java 7) infère le type depuis la déclaration, et fonctionne avec les classes anonymes depuis Java 9.

**Bornes récursives** : `<T extends Comparable<T>>` signifie "un type comparable à lui-même" ; on voit aussi `<T extends Comparable<? super T>>` pour accepter un type dont le parent implémente Comparable. Le pattern `Enum<E extends Enum<E>>` ou les builders hiérarchiques utilisent la même idée.

**Capturer le type à l'exécution malgré l'effacement** : passer un `Class<T>` (`repository.find(id, User.class)`), ou un *type token* basé sur une sous-classe anonyme qui conserve l'information générique dans sa signature : `new TypeReference<List<User>>() {}` (Jackson), `ParameterizedTypeReference` (Spring). La réflexion peut lire les types génériques déclarés dans les signatures (`getGenericSuperclass`), pas ceux des instances.

**Méthodes pont (bridge methods)** : quand une classe implémente `Comparable<Integer>`, le compilateur génère une méthode `compareTo(Object)` qui caste puis appelle `compareTo(Integer)`, pour préserver le polymorphisme après effacement.

**Ce qui n'est pas autorisé avec un paramètre de type** : primitifs (`List<int>`, il faut `List<Integer>`), champs static de type T, `new T()`, `new T[]`, `instanceof T`, catch d'un type générique, classes génériques qui étendent Throwable.

**Wildcard capture** : on ne peut rien insérer (sauf null) dans une `List<?>`. Pour écrire une méthode qui échange deux éléments d'une `List<?>`, on délègue à une méthode privée générique `<T> void swapHelper(List<T> l, ...)` qui "capture" le type.

**Les valhalla / value types** (projet Valhalla, encore en développement) visent à permettre des types valeur sans identité et, à terme, des génériques sur primitifs. À citer comme perspective, sans le présenter comme disponible.

### En situation

- **"Comment écrire un utilitaire réutilisable ?"** → generics + bounded types.
- **"Comment concevoir une couche d'accès générique ?"** → `Repository<T, ID>`.
- **"Pourquoi je ne peux pas faire instanceof List<String> ?"** → type erasure.

### Questions techniques

**Q : À quoi servent les generics ?** R : Sécurité de type à la compilation et réutilisabilité, sans cast.

**Q : C'est quoi un bounded type ?** R : Une contrainte sur le paramètre de type : `<T extends Number>`.

**Q : C'est quoi PECS ?** R : Producer Extends, Consumer Super, extends pour lire, super pour écrire.

**Q : C'est quoi le type erasure ?** R : L'effacement des types génériques à la compilation, pour compatibilité ascendante.

**Q : Limitations du type erasure ?** R : Pas de `new T()`, `new T[]`, `instanceof List<String>`, ni surcharge par type générique.

**Q : `List<String>` est-il un sous-type de `List<Object>` ?** R : Non, les generics sont invariants.

**Q : Pourquoi les tableaux sont-ils covariants et pas les generics ?** R : Historique : les tableaux vérifient le type à l'exécution (ArrayStoreException) ; les generics, effacés, ne le peuvent pas, donc l'invariance protège à la compilation.

**Q : C'est quoi un raw type ?** R : Un type générique utilisé sans paramètre, conservé pour la compatibilité ; il désactive les vérifications.

**Q : C'est quoi la pollution du heap ?** R : Une variable paramétrée qui pointe vers un objet du mauvais type, révélée plus tard par une ClassCastException.

**Q : À quoi sert @SafeVarargs ?** R : À certifier qu'une méthode à varargs génériques ne fait rien de dangereux avec le tableau, pour supprimer l'avertissement.

**Q : Que signifie `<T extends Comparable<? super T>>` ?** R : Un type comparable à lui-même ou à l'un de ses super-types.

**Q : Comment Jackson connaît-il le type `List<User>` malgré l'effacement ?** R : Par un type token : une sous-classe anonyme de TypeReference qui conserve le type dans sa signature.

**Q : C'est quoi une méthode pont ?** R : Une méthode synthétique générée pour préserver le polymorphisme après effacement des types.

**Q : Peut-on écrire `List<int>` ?** R : Non, les paramètres de type doivent être des types référence.

**Q : Peut-on ajouter un élément dans une `List<?>` ?** R : Seulement null.

**Q : `List<?>` vs `List<Object>` ?** R : List<?> accepte une liste de n'importe quel type mais en lecture seule ; List<Object> n'accepte que des listes d'Object mais autorise l'écriture.

**Q : Que fait l'opérateur diamant ?** R : Il infère les arguments de type depuis la déclaration (`new ArrayList<>()`).

**Q : Exemple de PECS dans le JDK ?** R : `Collections.copy(List<? super T> dest, List<? extends T> src)`.

### Questions de situation

**Q : "Conçois une couche d'accès aux données réutilisable pour plusieurs entités."**
R : "Une interface générique paramétrée par le type d'entité et celui de l'identifiant : `Repository<T, ID>`, avec les opérations communes, findById, findAll, save, delete. Chaque entité définit ensuite sa propre interface qui étend la générique et ajoute ses méthodes spécifiques.
C'est exactement le modèle de Spring Data JPA avec `JpaRepository<User, Long>`.
Trois bénéfices : le contrat commun est écrit une seule fois ; la sécurité de type est préservée, pas de cast, pas de `Object` ; et comme on dépend d'une interface, on respecte le Dependency Inversion, donc on peut substituer une implémentation en mémoire pour les tests."

**Q : "Écris une méthode qui renvoie le maximum d'une collection de n'importe quel type comparable."**
R :
```java
static <T extends Comparable<? super T>> T max(Collection<? extends T> elements) {
    Iterator<? extends T> it = elements.iterator();
    T max = it.next();                 // NoSuchElementException si vide : à documenter
    while (it.hasNext()) {
        T e = it.next();
        if (e.compareTo(max) > 0) max = e;
    }
    return max;
}
```
"La borne `Comparable<? super T>` accepte un type dont le parent définit la comparaison (par exemple une sous-classe de `LocalDate`), et `? extends T` en entrée applique PECS : la collection produit des T. C'est presque exactement la signature de `Collections.max`. En pratique j'utiliserais `stream().max(Comparator.naturalOrder())` qui renvoie un Optional et règle le cas de la collection vide."

═══════════════════════════════════════════
# 12. I/O, NIO, SÉRIALISATION & JSON
═══════════════════════════════════════════

### Vue d'ensemble

Toute application backend lit et écrit : fichiers, sockets, flux HTTP, messages. Java propose deux générations d'API : `java.io` (1996), orientée **flux** d'octets ou de caractères, bloquante et simple ; et `java.nio` (Java 1.4, enrichie en Java 7 avec NIO.2), orientée **buffers** et **canaux**, avec des entrées-sorties non bloquantes et une API moderne de fichiers (`Path`, `Files`).

La **sérialisation** (transformer un objet en octets ou en texte, puis l'inverse) est le second sujet du chapitre. La sérialisation Java native est aujourd'hui considérée comme un **risque de sécurité majeur** et un mauvais format d'échange ; dans le monde réel, on sérialise en JSON (Jackson), et en binaire avec des formats à schéma (Protobuf, Avro).

### Cours

**java.io : flux d'octets et de caractères.**
- Octets : `InputStream` / `OutputStream` (`FileInputStream`, `ByteArrayInputStream`, `BufferedInputStream`...).
- Caractères : `Reader` / `Writer` (`FileReader`, `BufferedReader`, `InputStreamReader` qui fait le pont octets → caractères **avec un charset**).
- Pattern **Decorator** : on empile les flux, `new BufferedReader(new InputStreamReader(in, UTF_8))`.
- Le **buffering** est essentiel : lire octet par octet sans buffer déclenche un appel système par octet.

**NIO.2, l'API à utiliser pour les fichiers** (Java 7+) :
```java
Path p = Path.of("data", "in.txt");          // Paths.get avant Java 11
String tout = Files.readString(p);            // Java 11, UTF-8 par défaut
List<String> lignes = Files.readAllLines(p);  // tout en mémoire
try (Stream<String> s = Files.lines(p)) {     // paresseux, à fermer
    s.filter(l -> l.contains("ERROR")).count();
}
Files.writeString(p, "contenu", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
Files.move(src, dst, StandardCopyOption.ATOMIC_MOVE);
Files.walk(dossier).filter(Files::isRegularFile)...   // à fermer aussi
Files.exists(p); Files.size(p); Files.createDirectories(p); Files.delete(p);
```
`File` (java.io) est l'ancienne API : messages d'erreur pauvres (renvoie `false` au lieu d'une exception), pas de gestion des liens symboliques ni des attributs.

**Écriture atomique d'un fichier** : écrire dans un fichier temporaire du même répertoire, puis `Files.move(tmp, cible, ATOMIC_MOVE)`. Un lecteur ne voit jamais un fichier à moitié écrit.

**NIO : buffers, canaux, sélecteurs.** Un `ByteBuffer` a une capacité, une position et une limite (`flip()` pour passer de l'écriture à la lecture, `clear()`/`compact()`). Les **canaux** (`FileChannel`, `SocketChannel`) lisent et écrivent dans des buffers. Un `Selector` surveille de nombreux canaux non bloquants avec un seul thread : c'est la base de Netty, donc de WebFlux, Vert.x et de nombreux serveurs. Buffers **directs** (`allocateDirect`) : hors heap, évitent une copie lors des I/O, mais coûteux à allouer et comptés dans la mémoire native. `FileChannel.map` permet le *memory-mapped file*. `transferTo` exploite le *zero-copy* de l'OS.

**Fichiers volumineux** : ne jamais `readAllBytes`/`readAllLines` un fichier de plusieurs Go ; lire en flux (`Files.lines`, `BufferedReader.readLine`, ou un `InputStream` par blocs).

**WatchService** : surveiller les modifications d'un répertoire.

**Client HTTP du JDK** (Java 11, `java.net.http.HttpClient`) : synchrone (`send`) ou asynchrone (`sendAsync` → CompletableFuture), HTTP/2, et HTTP/3 depuis Java 26 (JEP 517). Remplace `HttpURLConnection`.

**Sérialisation Java native** : `implements Serializable` (interface marqueur), `ObjectOutputStream.writeObject` / `ObjectInputStream.readObject`.
- `serialVersionUID` : identifiant de version de la classe. S'il n'est pas déclaré, il est calculé à partir de la structure de la classe, et la moindre modification rend les anciennes données illisibles (`InvalidClassException`).
- `transient` : champ exclu de la sérialisation (mot de passe, cache, champ dérivé) ; il vaut sa valeur par défaut à la désérialisation. Les champs `static` ne sont pas sérialisés non plus.
- Le constructeur de la classe sérialisable n'est **pas** appelé à la désérialisation (celui du premier parent non sérialisable l'est).
- Personnalisation : `writeObject`/`readObject` privés, `readResolve` (préserver un singleton), `writeReplace`, `Externalizable` (contrôle total).
- **Risque de sécurité** : désérialiser des données non fiables permet d'exécuter du code via des "gadget chains" présentes dans le classpath (failles célèbres dans Apache Commons Collections, nombreuses CVE). Parade : ne jamais désérialiser de données non fiables, ou filtrer avec `ObjectInputFilter` (Java 9). Les architectes du JDK la décrivent eux-mêmes comme une erreur historique.
- Autres défauts : format lié à Java, fragile aux évolutions, verbeux.

**JSON avec Jackson** (bibliothèque de référence, utilisée par Spring et largement par Quarkus) :
- `ObjectMapper` : coûteux à créer, **thread-safe** une fois configuré, à réutiliser (bean partagé).
- `writeValueAsString(obj)`, `readValue(json, User.class)`, `readValue(json, new TypeReference<List<User>>() {})`, `readTree` pour un `JsonNode` générique.
- Annotations : `@JsonProperty`, `@JsonIgnore`, `@JsonInclude(NON_NULL)`, `@JsonFormat`, `@JsonCreator`, `@JsonIgnoreProperties(ignoreUnknown = true)`, `@JsonTypeInfo`/`@JsonSubTypes` (polymorphisme).
- Les records sont supportés nativement.
- Dates : module `jackson-datatype-jsr310` pour `java.time`, et désactiver `WRITE_DATES_AS_TIMESTAMPS` pour obtenir de l'ISO 8601.
- Tolérance : `FAIL_ON_UNKNOWN_PROPERTIES` à false côté client pour supporter les évolutions d'API (lecteur tolérant).
- Sécurité : ne jamais activer le *default typing* polymorphe sur des données non fiables (même famille de failles que la sérialisation Java).
- **Jackson 3** (défaut de Spring Boot 4) : nouveau package `tools.jackson` (au lieu de `com.fasterxml.jackson` pour l'essentiel ; les annotations gardent leur package), `JsonMapper` immuable construit par builder, exceptions unchecked, valeurs par défaut modernisées (dates en ISO par défaut).
- Alternatives : JSON-B (standard Jakarta, Yasson), Gson.

**Formats binaires** : **Protobuf** (schéma `.proto`, compact, rapide, base de gRPC, évolution par numéros de champs), **Avro** (schéma, très utilisé avec Kafka et un *schema registry*), MessagePack, CBOR. Avantages : taille, vitesse, contrat fort ; inconvénients : illisible à l'œil, outillage.

**XML** : JAXB (sorti du JDK en Java 11, disponible en dépendance Jakarta XML Binding), parseurs DOM (tout en mémoire), SAX et StAX (en flux). Faille classique : **XXE** (*XML External Entity*), un document qui référence un fichier local ou une URL ; parade : désactiver les DTD et entités externes sur les parseurs.

### En situation

- **"Le traitement d'un gros fichier fait exploser la mémoire"** → lecture en flux au lieu de tout charger.
- **"Les anciennes données sérialisées ne se relisent plus"** → serialVersionUID absent ou modifié.
- **"Un client casse dès qu'on ajoute un champ à l'API"** → FAIL_ON_UNKNOWN_PROPERTIES côté client.
- **"Créer un ObjectMapper à chaque requête"** → coûteux, à partager.
- **"Un audit signale une faille de désérialisation"** → données non fiables désérialisées, gadget chains.

### Questions techniques

**Q : InputStream vs Reader ?** R : Flux d'octets vs flux de caractères (décodés avec un charset).

**Q : Pourquoi bufferiser les flux ?** R : Pour éviter un appel système par octet ou caractère lu.

**Q : Quel pattern illustre l'empilement des flux java.io ?** R : Le Decorator.

**Q : File vs Path/Files ?** R : File est l'ancienne API (erreurs silencieuses, peu de fonctions) ; Path/Files (NIO.2) est plus riche et lève des exceptions explicites.

**Q : readAllLines vs lines ?** R : Tout en mémoire vs flux paresseux à fermer.

**Q : Comment écrire un fichier de façon atomique ?** R : Écrire dans un fichier temporaire puis Files.move avec ATOMIC_MOVE.

**Q : C'est quoi un Selector ?** R : Un objet NIO qui surveille de nombreux canaux non bloquants depuis un seul thread (base de Netty).

**Q : Buffer direct vs heap ?** R : Hors heap, évite une copie lors des I/O, mais coûteux à allouer et compté en mémoire native.

**Q : Que fait flip() sur un ByteBuffer ?** R : Il passe du mode écriture au mode lecture (limite = position, position = 0).

**Q : C'est quoi le zero-copy ?** R : Transférer des données entre fichier et socket sans les copier en espace utilisateur (FileChannel.transferTo).

**Q : Quel client HTTP utiliser dans le JDK ?** R : java.net.http.HttpClient (Java 11), synchrone ou asynchrone, HTTP/2 et HTTP/3 (Java 26).

**Q : Rôle de serialVersionUID ?** R : Identifier la version de la classe sérialisée ; s'il change, la désérialisation échoue.

**Q : Que fait transient ?** R : Exclut le champ de la sérialisation.

**Q : Les champs static sont-ils sérialisés ?** R : Non.

**Q : Le constructeur est-il appelé à la désérialisation ?** R : Non pour la classe sérialisable ; seul celui du premier parent non sérialisable l'est.

**Q : Comment préserver un singleton sérialisable ?** R : readResolve qui renvoie l'instance unique, ou utiliser une enum.

**Q : Pourquoi la désérialisation Java est-elle dangereuse ?** R : Des données malveillantes peuvent déclencher des gadget chains et exécuter du code ; parade : ne pas désérialiser de données non fiables, ObjectInputFilter.

**Q : Serializable vs Externalizable ?** R : Sérialisation automatique vs contrôle total via writeExternal/readExternal (constructeur public sans argument requis).

**Q : ObjectMapper est-il thread-safe ?** R : Oui une fois configuré ; il faut le réutiliser.

**Q : Comment désérialiser une `List<User>` avec Jackson ?** R : Avec un TypeReference.

**Q : Comment ignorer les champs inconnus ?** R : `@JsonIgnoreProperties(ignoreUnknown = true)` ou désactiver FAIL_ON_UNKNOWN_PROPERTIES.

**Q : Qu'est-ce qui change avec Jackson 3 ?** R : Package tools.jackson, JsonMapper immuable par builder, exceptions unchecked, nouvelles valeurs par défaut ; défaut de Spring Boot 4.

**Q : Protobuf vs JSON ?** R : Binaire compact, rapide, à schéma et contrat fort vs texte lisible, universel, sans schéma imposé.

**Q : C'est quoi une faille XXE ?** R : Un XML qui référence des entités externes (fichiers, URL) ; parade : désactiver DTD et entités externes.

### Questions de situation

**Q : "Tu dois importer chaque nuit un fichier CSV de 10 Go en base."**
R : "Jamais de chargement complet : lecture en **flux** avec un `BufferedReader` ou un parseur CSV en streaming, traitement **par lots** (par exemple 1 000 lignes), et insertion en **batch JDBC** dans une transaction par lot. Ainsi la mémoire reste constante et une erreur ne fait pas tout rejouer.
J'ajouterais une stratégie de reprise : enregistrer le dernier lot réussi pour redémarrer au bon endroit, et isoler les lignes invalides dans un fichier de rejet plutôt que d'interrompre tout l'import. Spring Batch fournit exactement ce modèle (reader, processor, writer par chunks, avec reprise).
Côté base, désactiver temporairement certains index ou utiliser l'import natif (`COPY` en PostgreSQL) peut diviser le temps de plusieurs fois."

**Q : "Un service historique échange des objets Java sérialisés avec un autre système. Qu'en penses-tu ?"**
R : "Deux problèmes. **Sécurité** : si les données peuvent venir d'une source non maîtrisée, la désérialisation native est une porte d'entrée pour l'exécution de code à distance. **Couplage** : le format est lié aux classes Java et à leurs versions, donc les deux systèmes doivent évoluer ensemble.
À court terme, je mettrais un `ObjectInputFilter` avec une liste blanche des classes attendues. À moyen terme, je migrerais vers un format à **contrat explicite** : JSON avec un schéma documenté, ou Protobuf/Avro si la performance compte, ce qui découple les deux systèmes et permet des évolutions compatibles."

═══════════════════════════════════════════
# 13. JAVA MODERNE : RECORDS, SEALED, PATTERN MATCHING & VERSIONS
═══════════════════════════════════════════

### Vue d'ensemble

Depuis Java 10 (2018), le langage sort une version tous les six mois, en mars et en septembre. Les **LTS** (support long terme) sont 8, 11, 17, 21 et 25 ; depuis Java 17 il y en a une tous les deux ans, la prochaine étant Java 29 (septembre 2027). Cette cadence a permis d'introduire progressivement des idées venues d'autres langages, avec un objectif constant : **réduire le code cérémoniel** et rendre l'intention plus explicite.

Deux directions structurantes. D'un côté, **modéliser les données** proprement : les records suppriment le boilerplate des porteurs de données immuables. De l'autre, **modéliser les alternatives** : les sealed classes permettent de déclarer un ensemble **fermé** de possibilités, ce qui, combiné au pattern matching, donne au compilateur les moyens de vérifier l'exhaustivité. C'est l'arrivée en Java de ce que les langages fonctionnels appellent les types algébriques.

### Cours

**Record (Java 16)** : `record Point(int x, int y) {}` génère constructeur canonique, accesseurs, `equals`, `hashCode`, `toString`. Un record est **implicitement final**, ses champs sont finaux, et il ne peut pas hériter d'une classe (mais peut implémenter des interfaces).

Validation via **compact constructor** :
```java
record User(String name, int age) {
    User {
        if (age < 0) throw new IllegalArgumentException();
        name = name.strip();   // normalisation avant assignation
    }
}
```
Usage : DTOs, value objects, résultats intermédiaires, clés de map (equals/hashCode corrects par construction).

**Sealed classes (Java 17)** :
```java
public sealed interface Shape permits Circle, Rectangle {}
record Circle(double r) implements Shape {}
record Rectangle(double w, double h) implements Shape {}
```
Chaque sous-type doit être `final`, `sealed` ou `non-sealed`. L'intérêt : le compilateur connaît la liste complète des cas.

**Pattern matching** :
```java
if (obj instanceof String s && s.length() > 3) { ... }   // Java 16

String describe(Shape s) {                                // Java 21
    return switch (s) {
        case Circle c    -> "cercle de rayon " + c.r();
        case Rectangle r -> "rectangle " + r.w() + "×" + r.h();
    };  // pas de default nécessaire : sealed + exhaustif
}
```
La combinaison sealed + switch exhaustif est puissante : ajouter un nouveau sous-type fait **échouer la compilation** de tous les switch qui ne le traitent pas. Un cas oublié devient impossible.

**Autres apports** : `var` (inférence locale, Java 10), text blocks `"""` (Java 15), `SequencedCollection` avec `getFirst()`/`getLast()` (Java 21), virtual threads (Java 21).

**Record patterns** (Java 21) : déstructurer un record directement dans un `instanceof` ou un `switch`, y compris de façon imbriquée.
```java
if (obj instanceof Point(int x, int y)) { ... }

String decrire(Forme f) {
    return switch (f) {
        case Cercle(var r) when r > 10 -> "grand cercle";   // garde avec when
        case Cercle c                   -> "cercle";
        case Rectangle(double w, double h) -> "rectangle " + w + "x" + h;
    };
}
```
`case null` est autorisé dans un switch à patterns (sinon un sujet null lève une NPE). L'ordre des `case` compte : un cas plus général placé avant un cas plus spécifique est une erreur de compilation (*dominance*).

**Variables et patterns anonymes** (Java 22, JEP 456) : `_` pour une variable ou une composante qu'on n'utilise pas : `catch (Exception _)`, `case Point(int x, _)`, `for (var _ : liste)`.

**Switch expressions** (Java 14) : `->`, plusieurs étiquettes (`case LUNDI, MARDI ->`), `yield` pour retourner une valeur depuis un bloc, exhaustivité obligatoire.

**Text blocks** (Java 15) : `"""` multi-lignes, indentation commune retirée automatiquement, `\` en fin de ligne pour ne pas insérer de saut, `\s` pour un espace significatif.

**Java 25, ce qui a été finalisé** : *compact source files* et méthodes `main` d'instance (JEP 512 : `void main() { IO.println("..."); }` sans déclaration de classe, classe `java.lang.IO`), *flexible constructor bodies* (JEP 513 : instructions avant `super()`), *module import declarations* (JEP 511 : `import module java.base;` importe tous les packages exportés d'un module), `ScopedValue` (JEP 506), API de dérivation de clés (KDF), compact object headers en option produit, Shenandoah générationnel, profilage et cache AOT (Leyden). Encore en preview : structured concurrency, patterns sur types primitifs, *stable values* (devenues *lazy constants*).

**Évolution du langage, version par version** (à connaître dans les grandes lignes) :
| Version | Année | Apports marquants |
|---|---|---|
| 5 | 2004 | generics, annotations, enums, autoboxing, for-each, varargs, java.util.concurrent |
| 7 | 2011 | try-with-resources, multi-catch, diamant `<>`, switch sur String, NIO.2, invokedynamic |
| **8 (LTS)** | 2014 | lambdas, streams, Optional, java.time, méthodes default, Metaspace, CompletableFuture |
| 9 | 2017 | modules (JPMS), jshell, `List.of`, méthodes private d'interface, compact strings, G1 par défaut |
| 10 | 2018 | `var`, début de la cadence semestrielle, JVM consciente des conteneurs |
| **11 (LTS)** | 2018 | `HttpClient`, méthodes String (`isBlank`, `strip`, `lines`, `repeat`), `Files.readString`, lancement direct d'un fichier source, retrait des modules Java EE (JAXB, JAX-WS) et de CORBA |
| 12-13 | 2019 | `Collectors.teeing`, preview des switch expressions et text blocks |
| 14 | 2020 | switch expressions, helpful NPE |
| 15 | 2020 | text blocks, ZGC et Shenandoah en production, retrait de Nashorn |
| 16 | 2021 | records, pattern matching `instanceof`, `Stream.toList()`, encapsulation forte du JDK |
| **17 (LTS)** | 2021 | sealed classes, `RandomGenerator`, encapsulation forte définitive |
| 18 | 2022 | UTF-8 par défaut, serveur web simple (`jwebserver`), finalize déprécié pour suppression |
| 19-20 | 2022-23 | previews : virtual threads, record patterns, structured concurrency |
| **21 (LTS)** | 2023 | virtual threads, pattern matching pour switch, record patterns, Sequenced Collections, ZGC générationnel (option) |
| 22 | 2024 | variables anonymes `_`, API Foreign Function & Memory (remplaçant de JNI), lancement multi-fichiers |
| 23 | 2024 | Javadoc en Markdown, ZGC générationnel par défaut, retrait des String templates |
| 24 | 2025 | Stream Gatherers, API Class-File, fin du pinning des virtual threads sur synchronized, cache AOT (Leyden), Security Manager désactivé définitivement |
| **25 (LTS)** | 2025 | compact source files et main d'instance, flexible constructor bodies, imports de modules, ScopedValue, compact object headers (option produit) |
| 26 | 2026 | HTTP/3 dans HttpClient, cache AOT avec tous les GC, retrait de l'API Applet, avertissements "final signifie final" |
| 27 | 2026 | G1 par défaut partout, compact object headers par défaut, échange de clés hybride post-quantique pour TLS 1.3, masquage de données sensibles dans JFR |

Cadence : une version en mars et une en septembre depuis Java 10. LTS : 8, 11, 17, 21, 25, la prochaine étant **Java 29** (septembre 2027). Une version non-LTS n'est supportée que six mois.

**Preview, incubator, expérimental** : une fonctionnalité **preview** est complète mais peut encore changer (activation `--enable-preview` à la compilation et à l'exécution) ; un module **incubator** (`jdk.incubator.*`, par exemple Vector API) est une API expérimentale ; une option **expérimentale** de la JVM nécessite `-XX:+UnlockExperimentalVMOptions`. Ne jamais dépendre d'une preview en production.

**JPMS, le système de modules** (Java 9). Un module est décrit par `module-info.java` :
```java
module com.exemple.facturation {
    requires java.sql;                     // dépendance
    requires transitive com.exemple.core;  // dépendance exposée aux utilisateurs du module
    exports com.exemple.facturation.api;   // packages accessibles aux autres modules
    opens com.exemple.facturation.model;   // accès réflexif autorisé (JPA, Jackson)
    uses com.exemple.spi.Plugin;           // consomme un service
    provides com.exemple.spi.Plugin with com.exemple.facturation.MonPlugin;
}
```
Apports : **encapsulation forte** (un package non exporté est inaccessible, même `public`), dépendances explicites vérifiées au démarrage, interdiction des *split packages* (un même package dans deux modules), et `jlink` pour fabriquer un runtime réduit aux seuls modules nécessaires (images Docker plus petites). Le JDK lui-même est modulaire (`java.base`, `java.sql`...). Pour la compatibilité : le code du classpath forme le *module anonyme* (*unnamed module*), et un jar non modulaire placé sur le module path devient un *module automatique*. En pratique, la plupart des applications d'entreprise restent sur le classpath ; JPMS sert surtout aux bibliothèques et au JDK.

**Migrer de Java 8 vers 17, 21 ou 25** : les obstacles habituels sont le retrait des modules Java EE du JDK en Java 11 (JAXB, JAX-WS, javax.annotation à ajouter en dépendances), l'encapsulation forte qui bloque les bibliothèques accédant aux internes du JDK (mettre à jour les bibliothèques, en dernier recours `--add-opens`), la mise à jour des outils de build et plugins (Maven compiler, Surefire, Lombok, Mockito, ByteBuddy), les API supprimées (Nashorn, `Thread.stop`, Security Manager), et, distinct du JDK, le passage de `javax.*` à `jakarta.*` imposé par Spring Boot 3+ et Jakarta EE 9+. Gains : performances et GC, virtual threads, langage modernisé, images plus légères, support de sécurité.

### En situation

- **"Comment réduire le boilerplate des DTOs ?"** → records.
- **"Comment modéliser un ensemble fermé de cas ?"** → sealed + switch exhaustif.
- **"Comment garantir qu'aucun cas n'est oublié ?"** → exhaustivité vérifiée par le compilateur.

### Questions techniques

**Q : C'est quoi un record ?** R : Une classe immutable compacte générant constructeur, accesseurs, equals, hashCode, toString.

**Q : Limites d'un record ?** R : Implicitement final, champs finaux, ne peut pas hériter d'une classe.

**Q : Comment valider dans un record ?** R : Avec un compact constructor.

**Q : C'est quoi une sealed class ?** R : Une classe restreignant ses sous-types via `permits`.

**Q : Intérêt de sealed + switch ?** R : Exhaustivité vérifiée par le compilateur, sans `default`.

**Q : C'est quoi var ?** R : L'inférence de type pour les variables locales (Java 10).

**Q : Quelles sont les versions LTS de Java ?** R : 8, 11, 17, 21, 25 ; la prochaine est 29 (septembre 2027).

**Q : Quelle est la cadence de publication ?** R : Une version tous les six mois (mars et septembre) depuis Java 10, une LTS tous les deux ans depuis Java 17.

**Q : C'est quoi un record pattern ?** R : La déstructuration d'un record dans un instanceof ou un switch : `case Point(int x, int y)`.

**Q : À quoi sert `when` dans un switch ?** R : C'est une garde : le cas ne s'applique que si la condition est vraie.

**Q : Un switch à patterns gère-t-il null ?** R : Oui avec un `case null` explicite, sinon une NPE est levée.

**Q : À quoi sert `_` depuis Java 22 ?** R : À nommer une variable ou une composante de pattern inutilisée.

**Q : C'est quoi yield ?** R : Le mot-clé qui retourne une valeur depuis un bloc dans une switch expression.

**Q : Qu'a apporté Java 25 au point d'entrée des programmes ?** R : Les compact source files et la méthode main d'instance : `void main()` sans déclarer de classe.

**Q : C'est quoi les flexible constructor bodies ?** R : La possibilité d'écrire des instructions avant `super()`/`this()`, tant qu'elles n'utilisent pas `this` (Java 25).

**Q : Les String templates existent-ils ?** R : Non, ils ont été en preview en Java 21-22 puis retirés en Java 23.

**Q : Preview vs incubator ?** R : Fonctionnalité du langage ou de l'API complète mais modifiable (--enable-preview) vs API expérimentale dans un module jdk.incubator.

**Q : Qu'apporte Java 27 ?** R : G1 par défaut partout, compact object headers par défaut, TLS 1.3 hybride post-quantique, masquage dans JFR.

**Q : À quoi sert module-info.java ?** R : Déclarer le nom du module, ses dépendances (requires), ses packages exportés et ouverts à la réflexion, les services utilisés et fournis.

**Q : exports vs opens ?** R : exports rend les types publics accessibles à la compilation et à l'exécution ; opens autorise l'accès réflexif profond.

**Q : C'est quoi requires transitive ?** R : Une dépendance automatiquement visible par les modules qui dépendent du module courant.

**Q : C'est quoi un split package ?** R : Un même package réparti dans deux modules, interdit par JPMS.

**Q : Unnamed module vs automatic module ?** R : Le code du classpath vs un jar non modulaire placé sur le module path.

**Q : À quoi sert jlink ?** R : Construire un runtime Java réduit aux modules nécessaires.

**Q : Que se passe-t-il pour JAXB en passant de Java 8 à 11 ?** R : Il a été retiré du JDK ; il faut l'ajouter en dépendance (Jakarta XML Binding).

**Q : javax → jakarta, c'est lié au JDK ?** R : Non, c'est un changement de Jakarta EE 9 (licence de la marque Java) ; il concerne les API EE comme Servlet, JPA, Bean Validation.

**Q : Combien de temps une version non-LTS est-elle supportée ?** R : Six mois, jusqu'à la version suivante.

### Questions de situation

**Q : "Tu modélises les statuts d'une commande, chacun portant des données différentes. Comment ?"**
R : "Une interface **sealed** avec des **records** comme implémentations : `OrderStatus` autorisant `Pending`, `Shipped(trackingNumber)`, `Delivered(date)`, `Cancelled(reason)`.
Le gain arrive au traitement : un `switch` sur le statut est **exhaustif**. Le compilateur sait qu'il n'existe que ces quatre cas, donc pas de `default` artificiel. Et surtout, si quelqu'un ajoute un statut demain, **tous les switch qui ne le traitent pas cessent de compiler** : l'oubli devient impossible.
Comparé aux alternatives : une enum avec des champs optionnels force à porter des données nulles selon le cas ; une hiérarchie de classes ouverte ne donne aucune garantie d'exhaustivité, et un cas oublié tombe silencieusement dans le `default`."

**Q : "Ton équipe est sur Java 17. Faut-il passer à 21 ou à 25 ?"**
R : "Je viserais directement la **25**, la LTS la plus récente : elle cumule les apports de 21 (virtual threads, pattern matching complet, sequenced collections) et ceux de 22 à 25 (fin du pinning sur `synchronized`, gatherers, ScopedValue, cache AOT pour le démarrage, compact object headers). Le coût de migration de 17 vers 25 n'est pas beaucoup plus élevé que vers 21.
Je vérifierais la compatibilité de l'écosystème : version du framework (Spring Boot 3.x/4.x, Quarkus 3.x supportent Java 25), outils de build, bibliothèques qui manipulent le bytecode (Lombok, Mockito, ByteBuddy, Jacoco) qui doivent connaître le format de classe.
Démarche : compiler et tester sur le nouveau JDK en CI en parallèle, corriger les avertissements, déployer progressivement en surveillant GC et latence. Et ne pas adopter de fonctionnalité en preview."

**Q : "Comment le pattern matching change-t-il ta façon de concevoir ?"**
R : "Il rend viable une approche orientée **données** à côté de l'orientée objet. Avec une hiérarchie `sealed` de records et un `switch` exhaustif, j'exprime un type algébrique : un ensemble fermé de cas, chacun avec ses données, et le compilateur vérifie que tous les cas sont traités.
C'est particulièrement adapté quand les **opérations** évoluent plus souvent que les **types** (états d'une commande, résultats d'un calcul, messages d'un protocole) : ajouter une opération = écrire un switch, sans toucher aux classes. À l'inverse, quand les types évoluent souvent et que les opérations sont stables, le polymorphisme classique reste meilleur. C'est le vieux compromis du *expression problem*, et Java donne maintenant les deux outils."

═══════════════════════════════════════════
# 14. QUESTIONS PIÈGES : QU'AFFICHE CE CODE ?
═══════════════════════════════════════════

### Vue d'ensemble

Beaucoup de tests techniques (QCM en ligne, tests de présélection d'ESN, premiers entretiens) contiennent des extraits de code dont il faut prédire la sortie. Ils ne mesurent pas la capacité à concevoir un système, mais la connaissance précise des règles du langage : promotion des types, cache des wrappers, ordre d'évaluation, sémantique de `finally`, comportement des collections.

Méthode pour les aborder : lire lentement, repérer le **type exact** de chaque expression (int ? Integer ? String ?), se demander si l'évaluation a lieu à la **compilation** ou à l'**exécution**, et se méfier de tout ce qui ressemble à un piège évident. Toutes les sorties ci-dessous ont été vérifiées sur un JDK 21.

### Cours

**Les familles de pièges** :
- **Égalité** : `==` sur des objets, cache Integer/Long (-128 à 127), constantes de compilation pour les String.
- **Arithmétique** : division entière, débordement silencieux, flottants IEEE 754, arrondis, promotion en int.
- **Évaluation** : `i++` vs `++i`, concaténation de gauche à droite, court-circuit, ternaire et unboxing.
- **Contrôle** : fall-through du switch, `return` dans `finally`.
- **Objets** : surcharge résolue à la compilation, méthodes static masquées, champs non polymorphes, méthode redéfinie appelée depuis un constructeur.
- **Collections** : `remove(int)` vs `remove(Object)`, `Arrays.asList`, collections immuables, modification pendant l'itération.
- **Streams** : paresse, opération terminale absente.

### En situation

- **QCM chronométré** → repérer le type de chaque expression avant de calculer.
- **"Ce code compile-t-il ?"** → penser aux règles de cast implicite, de dominance des catch, d'ambiguïté de surcharge.
- **En entretien oral** → expliquer la règle derrière la réponse, pas seulement la réponse.

### Questions techniques

**Q : `System.out.println(0.1 + 0.2 == 0.3);`** R : `false` : imprécision binaire des flottants.

**Q :**
```java
String s1 = "Java";
String s2 = "Ja" + "va";
String part = "Ja";
String s3 = part + "va";
final String fp = "Ja";
String s4 = fp + "va";
System.out.println((s1 == s2) + " " + (s1 == s3) + " " + (s1 == s4));
```
R : `true false true`. `"Ja" + "va"` et `fp + "va"` (fp est une constante final) sont calculés à la **compilation** et pointent vers le pool ; `part + "va"` est calculé à l'exécution et crée un nouvel objet.

**Q :**
```java
Integer a = 1000, b = 1000;
System.out.println((a == b) + " " + a.equals(b) + " " + (a <= b && a >= b));
```
R : `false true true`. `==` compare les références (hors cache) ; `<=` et `>=` déballent en int.

**Q : `Long x = 127L, y = 127L, z = 128L, w = 128L;` que valent `x == y` et `z == w` ?** R : `true` et `false` : Long a aussi un cache de -128 à 127.

**Q : `int i = 0; i = i++; System.out.println(i);`** R : `0`. `i++` renvoie 0, puis l'incrément a lieu, puis l'affectation écrase avec 0.

**Q : `int k = 1; int r = k++ + ++k;` que valent r et k ?** R : r = 4 (1 + 3), k = 3.

**Q : `System.out.println(1 + 2 + "3" + 4 + 5);`** R : `3345` : évaluation de gauche à droite, 1 + 2 = 3, puis concaténation.

**Q : `char c = 'A'; System.out.println(c + 1); System.out.println((char)(c + 1)); System.out.println("" + c + 1);`** R : `66`, `B`, `A1`.

**Q : `System.out.println('a' + 'b');`** R : `195` : deux char additionnés donnent un int. Avec `"" + 'a' + 'b'` on obtient `ab`.

**Q : `Math.round(-2.5)`, `Math.round(2.5)`, `Math.round(-2.6)` ?** R : `-2`, `3`, `-3` (arrondi vers +∞ à .5).

**Q : `System.out.println(Math.abs(Integer.MIN_VALUE));`** R : `-2147483648` : la valeur positive n'est pas représentable.

**Q : `System.out.println(Integer.MAX_VALUE + 1);`** R : `-2147483648` : débordement silencieux.

**Q : `(int) 3.99` et `(int) -3.99` ?** R : `3` et `-3` : le cast tronque vers zéro.

**Q : `System.out.println(1 / 2 * 2.0 + " " + 1 / 2.0 * 2);`** R : `0.0 1.0` : `1 / 2` est une division entière qui vaut 0.

**Q : `System.out.println(10 / 3 + " " + 10 % 3 + " " + (-10 % 3));`** R : `3 1 -1`.

**Q : `10.0 / 0` et `0.0 / 0` ?** R : `Infinity` et `NaN`, sans exception (alors que `10 / 0` lève ArithmeticException).

**Q : `Double.NaN == Double.NaN` et `Double.valueOf(Double.NaN).equals(Double.NaN)` ?** R : `false` et `true`.

**Q : `byte b = (byte) 200; System.out.println(b);`** R : `-56` : 200 - 256.

**Q : `short s = 1; s = s + 1;` compile-t-il ?** R : Non, `s + 1` est un int. En revanche `s += 1;` compile (cast implicite) et donne 2.

**Q : `System.out.println(017);`** R : `15` : un littéral commençant par 0 est en octal.

**Q :**
```java
static int f() { int x = 1; try { return x; } finally { x = 2; } }
static int g() { try { return 1; } finally { return 2; } }
```
R : `f()` renvoie `1` (la valeur de retour est figée avant le finally) ; `g()` renvoie `2` (le return du finally écrase celui du try).

**Q :**
```java
switch (1) {
    case 1: System.out.print("1");
    case 2: System.out.print("2");
    default: System.out.println("d");
}
```
R : `12d` : sans `break`, l'exécution continue dans les cas suivants (fall-through).

**Q :**
```java
List<Integer> l = new ArrayList<>(List.of(1, 2, 3));
l.remove(1);
System.out.println(l);
l.remove(Integer.valueOf(1));
System.out.println(l);
```
R : `[1, 3]` puis `[3]`. `remove(1)` supprime à l'**index** 1 ; `remove(Integer.valueOf(1))` supprime la **valeur** 1.

**Q :**
```java
List<String> l = new ArrayList<>(List.of("a", "b", "c"));
for (String x : l) if (x.equals("b")) l.remove(x);
System.out.println(l);
```
R : `[a, c]` **sans exception** : en supprimant l'avant-dernier élément, `hasNext()` renvoie false et la vérification n'a pas lieu. Avec `"a"`, on obtient une ConcurrentModificationException. Le comportement correct est de toujours utiliser `removeIf` ou `iterator.remove()`.

**Q :**
```java
Set<Short> s = new HashSet<>();
for (short i = 0; i < 100; i++) { s.add(i); s.remove(i - 1); }
System.out.println(s.size());
```
R : `100` : `i - 1` est un int, autoboxé en Integer, qui n'est jamais égal à un Short. Aucun élément n'est supprimé.

**Q : `Arrays.asList(new int[]{1, 2, 3}).size()` et `Arrays.asList(new Integer[]{1, 2, 3}).size()` ?** R : `1` et `3` : un `int[]` est un seul objet, la liste obtenue est une `List<int[]>`.

**Q : `new int[]{1}.equals(new int[]{1})` et `Arrays.equals(new int[]{1}, new int[]{1})` ?** R : `false` et `true`.

**Q : `Arrays.asList(1, 2, 3).add(4);`** R : UnsupportedOperationException (liste de taille fixe).

**Q : `Map.of("a", 1, "a", 2);`** R : IllegalArgumentException (clé dupliquée). Et `List.of(1, null)` lève une NullPointerException.

**Q : `Stream.of("a", "a").collect(Collectors.toMap(x -> x, x -> 1));`** R : IllegalStateException (clé dupliquée sans fonction de fusion).

**Q : `Stream.of(1, 2).toList().add(3);`** R : UnsupportedOperationException : toList() renvoie une liste non modifiable.

**Q : `Stream.of(1, 2, 3).peek(System.out::println).map(x -> x * 2);`** R : Rien n'est affiché : pas d'opération terminale.

**Q : `List.of(1, 2, 3).stream().peek(System.out::println).count();`** R : Aucun peek n'est affiché (Java 9+) et le résultat vaut 3 : la taille est connue sans parcourir le pipeline.

**Q :**
```java
static void o(Object x) { System.out.println("Object"); }
static void o(String x) { System.out.println("String"); }
o(null);
```
R : `String` : la surcharge la plus spécifique est choisie. Avec `o(String)` et `o(Integer)`, l'appel `o(null)` ne compilerait pas (ambiguïté).

**Q :**
```java
class Parent { String name = "parent"; static String s() { return "Parent.s"; } }
class Child extends Parent { String name = "child"; static String s() { return "Child.s"; } }
Parent p = new Child();
System.out.println(p.name + " " + ((Child) p).name + " " + p.s());
```
R : `parent child Parent.s` : les champs et les méthodes static sont résolus selon le **type déclaré**.

**Q :**
```java
class Parent { Parent() { init(); } void init() {} }
class Child extends Parent {
    String v = "set";
    void init() { System.out.println("init v=" + v); }
}
new Child();
```
R : `init v=null` : le constructeur du parent appelle la version redéfinie avant l'initialisation des champs de l'enfant.

**Q : `String n = null; System.out.println(n + "a");`** R : `nulla`. Et `null instanceof Object` vaut `false`.

**Q : `String.valueOf((Object) null)` et `String.valueOf((char[]) null)` ?** R : `"null"` et NullPointerException. Attention : `String.valueOf(null)` sans cast choisit la surcharge `char[]` (la plus spécifique) et lève une NPE.

**Q :**
```java
Integer nul = null;
boolean cond = true;
Integer r = cond ? nul : 0;
```
R : NullPointerException : un opérande est `Integer`, l'autre `int`, donc le type du ternaire est `int` et `nul` est déballé. À l'inverse, `Integer r2 = false ? 1 : null;` donne `null` sans exception (type Integer).

**Q : `new BigDecimal("2.0").equals(new BigDecimal("2.00"))` et `compareTo` ?** R : `false` et `0` : equals compare aussi l'échelle.

**Q : `System.out.println(new BigDecimal(0.1));`** R : `0.1000000000000000055511151231257827021181583404541015625`.

**Q : `"Aa".hashCode() == "BB".hashCode()` ?** R : `true` (2112) : collision classique.

**Q : `"a,b,,".split(",").length`, `"a,,b".split(",").length`, `"a,b,,".split(",", -1).length`, `"a.b".split(".").length` ?** R : `2`, `3`, `4`, `0`.

**Q : `String t = "hello"; t.toUpperCase(); System.out.println(t);`** R : `hello` : String est immuable et le résultat n'a pas été récupéré.

**Q : `new StringBuilder("x").equals(new StringBuilder("x"))` ?** R : `false` : StringBuilder ne redéfinit pas equals.

**Q : `int[] a = {1, 2, 3}; int[] b = a; b[0] = 9; System.out.println(a[0]);`** R : `9` : les deux variables référencent le même tableau.

**Q : `Optional.ofNullable(null).map(String::length).orElse(-1)`** R : `-1`. Et `Optional.of(null)` lève une NullPointerException.

**Q : Ce code compile-t-il ?**
```java
try { ... } catch (Exception e) { ... } catch (IOException e) { ... }
```
R : Non : le second catch est inatteignable, il faut placer le plus spécifique en premier.

**Q : `Thread t = new Thread(() -> System.out.println(Thread.currentThread().getName())); t.run();`** R : `main` : run() s'exécute dans le thread courant ; il faut start().

### Questions de situation

**Q : "En entretien, on te montre un code et tu n'es pas sûr de la réponse."**
R : "Je raisonnerais à voix haute en appliquant les règles que je connais : 'cette expression est un int parce que...', 'cette concaténation est évaluée à la compilation parce que les deux opérandes sont des constantes'. Même si je me trompe sur le résultat final, l'évaluateur voit que je connais les mécanismes.
Et si on me demande ce que je ferais en vrai : je l'exécuterais dans `jshell` en dix secondes. Le but de ces questions n'est pas de remplacer un compilateur, mais de vérifier qu'on reconnaît les situations dangereuses quand on les rencontre dans une revue de code."

═══════════════════════════════════════════
# 15. PROGRAMMATION ORIENTÉE OBJET
═══════════════════════════════════════════

### Vue d'ensemble

L'orienté objet est né d'un constat : à mesure qu'un programme grossit, ce qui coûte cher n'est pas d'écrire du code, c'est de le **modifier sans rien casser**. La programmation procédurale sépare les données des fonctions qui les manipulent ; n'importe quelle fonction peut modifier n'importe quelle donnée, et le nombre de dépendances explose.

L'OOP répond en **regroupant données et comportements** dans des objets qui protègent leur propre état. Chaque objet devient responsable de sa cohérence, et le reste du programme interagit avec lui par un contrat plutôt qu'en manipulant ses entrailles.

Les quatre piliers ne sont pas quatre règles indépendantes : ils forment un ensemble cohérent. L'**encapsulation** protège l'état, l'**abstraction** définit le contrat, l'**héritage** et le **polymorphisme** permettent de faire varier l'implémentation derrière ce contrat sans que l'appelant s'en aperçoive. C'est cette dernière propriété (écrire du code qui fonctionne avec des types qui n'existaient pas encore quand on l'a écrit) qui rend l'OOP puissant.

### Cours

**Encapsulation.** Cacher l'état interne, exposer des comportements contrôlés. Champs privés, méthodes qui valident avant de modifier. Ce n'est pas de la cosmétique : un compte bancaire dont le solde est public peut devenir négatif sans qu'aucune règle ne s'applique. Encapsuler, c'est garantir qu'un objet ne peut jamais se retrouver dans un état invalide.

**Héritage.** Une classe enfant récupère attributs et méthodes du parent. Java n'autorise qu'un seul parent (héritage simple) mais plusieurs interfaces. L'héritage exprime une relation **"est un"**.

**Composition plutôt qu'héritage.** L'héritage crée le couplage le plus fort du langage : l'enfant dépend des détails internes du parent, et une modification du parent peut casser tous les enfants (le "problème de la classe de base fragile"). La composition exprime **"a un"** : on assemble des objets plutôt que d'hériter d'eux. C'est plus flexible (on peut changer un composant à l'exécution) et ça évite les hiérarchies profondes ingérables. La règle usuelle : n'utiliser l'héritage que quand la sous-classe est **réellement substituable** au parent (cf. Liskov).

**Polymorphisme.** Un même appel se comporte différemment selon le type réel.
- **Overloading** (surcharge) : même nom, paramètres différents. Résolu à la **compilation**, selon le type déclaré.
- **Overriding** (redéfinition) : même signature dans une sous-classe. Résolu à l'**exécution**, selon le type réel, c'est le "dynamic dispatch".

```java
Animal a = new Dog();
a.sound();   // appelle Dog.sound(), pas Animal.sound()
```

**Abstraction.** Définir un contrat sans imposer le comment.

**Classe abstraite vs interface :**

| | Classe abstraite | Interface |
|---|---|---|
| État (champs) | oui | seulement `static final` |
| Constructeur | oui | non |
| Méthodes concrètes | oui | `default` / `static` (Java 8+) |
| Héritage | simple | multiple |
| Sens | "est un", famille proche | "sait faire", contrat transversal |

Le critère de choix : ai-je du **code ou de l'état à partager** entre classes proches (→ abstraite), ou un **contrat** que des classes non apparentées peuvent remplir (→ interface) ? En pratique on penche vers l'interface, qui préserve l'unique droit d'héritage et laisse plus de liberté.

**Problème du diamant** : si deux interfaces fournissent la même méthode `default`, la classe implémentante doit la redéfinir pour lever l'ambiguïté.

**Relations entre objets** : association (relation simple), agrégation (le contenu survit au conteneur, une équipe et ses joueurs), composition (le contenu meurt avec le conteneur, une commande et ses lignes).

**Classe vs objet** : la classe est le plan (type, attributs, méthodes), l'objet une instance concrète en mémoire, avec son état et son identité.

**Cohésion et couplage** : une classe **cohésive** regroupe des éléments qui travaillent ensemble vers un même but ; le **couplage** mesure la dépendance entre classes. L'objectif constant : forte cohésion, faible couplage. C'est la boussole derrière SRP, l'encapsulation et la composition.

**Tell, don't ask** : plutôt que de demander son état à un objet pour décider à sa place (`if (compte.getSolde() >= montant) compte.setSolde(...)`), lui demander d'agir (`compte.debiter(montant)`), et le laisser protéger ses invariants. Des getters/setters systématiques sur tous les champs ne sont pas de l'encapsulation : ils exposent l'état sous un autre nom.

**Modèle anémique** : des entités sans comportement (que des getters/setters) et toute la logique dans des services. Courant et pragmatique sur du CRUD, critiqué par le DDD sur un métier riche, où la logique doit vivre au plus près des données qu'elle protège.

**Polymorphisme ad hoc, paramétrique, d'inclusion** : la surcharge (ad hoc), les generics (paramétrique), l'héritage/les interfaces avec redéfinition (d'inclusion, le "vrai" polymorphisme objet).

**Liaison statique vs dynamique** : la surcharge, les méthodes `static`, `private` et `final` sont liées à la compilation ; les méthodes d'instance redéfinissables sont liées à l'exécution (table de méthodes virtuelles).

**Héritage multiple** : interdit pour les classes (problème du diamant sur l'état), autorisé pour les interfaces ; depuis les méthodes `default`, la classe doit trancher explicitement en cas de conflit avec `Interface.super.methode()`. Règles de résolution : la classe gagne toujours sur l'interface ; l'interface la plus spécifique gagne ; sinon conflit à résoudre.

**Classe `final`** : empêche l'héritage (String, les wrappers, les records). Utile pour garantir l'immutabilité ou un comportement que personne ne doit modifier.

**Programmer vers une interface** : déclarer `List<User> users = new ArrayList<>()` plutôt que `ArrayList<User>`, et dépendre de `PaymentGateway` plutôt que de `StripePaymentGateway`.

### En situation

- **"Cette hiérarchie devient ingérable"** → trop d'héritage → composition.
- **"Comment partager du comportement entre classes non liées ?"** → interface + default.
- **"Comment rendre ce code extensible ?"** → programmer sur des abstractions.
- **"Cette classe expose tous ses champs"** → encapsulation rompue, état incohérent possible.

### Questions techniques

**Q : Les 4 piliers ?** R : Encapsulation, Héritage, Polymorphisme, Abstraction.

**Q : Pourquoi préférer la composition ?** R : L'héritage couple fortement (classe de base fragile) et rigidifie ; la composition est plus flexible.

**Q : Overloading vs overriding ?** R : Surcharge résolue à la compilation selon le type déclaré ; redéfinition résolue à l'exécution selon le type réel.

**Q : Classe abstraite vs interface ?** R : Abstraite = état + code partagé, héritage simple. Interface = contrat transversal, implémentation multiple.

**Q : Problème du diamant ?** R : Deux `default` identiques → la classe doit redéfinir pour trancher.

**Q : Agrégation vs composition ?** R : Le contenu survit au conteneur / meurt avec lui.

**Q : Classe vs objet ?** R : Le plan vs une instance avec son état et son identité.

**Q : Peut-on instancier une interface ?** R : Non, mais on peut instancier une classe anonyme ou une lambda qui l'implémente.

**Q : Une classe abstraite peut-elle ne contenir aucune méthode abstraite ?** R : Oui, pour empêcher son instanciation directe.

**Q : Une classe non abstraite peut-elle avoir une méthode abstraite ?** R : Non.

**Q : Une classe abstraite peut-elle avoir un constructeur ?** R : Oui, appelé par les sous-classes via super().

**Q : Java supporte-t-il l'héritage multiple ?** R : Pas de classes ; d'interfaces oui.

**Q : Comment appeler la méthode default d'une interface précise en cas de conflit ?** R : `MonInterface.super.methode()`.

**Q : Que privilégier : forte cohésion ou faible couplage ?** R : Les deux à la fois ; ils vont ensemble.

**Q : C'est quoi "tell, don't ask" ?** R : Demander à l'objet d'agir plutôt que de lire son état pour décider à sa place.

**Q : Les getters/setters sont-ils de l'encapsulation ?** R : Pas en soi : des setters sur tout l'état contournent les invariants.

**Q : C'est quoi un modèle anémique ?** R : Des entités sans comportement dont toute la logique est dans des services.

**Q : Liaison statique vs dynamique ?** R : Résolue à la compilation (surcharge, static, private, final) vs à l'exécution (redéfinition).

**Q : Pourquoi déclarer une variable avec le type de l'interface ?** R : Pour pouvoir changer d'implémentation sans modifier le code appelant.

### Questions de situation

**Q : "Une classe de base a 8 sous-classes et chaque besoin ajoute une méthode dans la base."**
R : "Symptôme d'héritage sur-utilisé : la base devient un fourre-tout, et chaque modification impacte les 8 sous-classes, donc chaque ajout est risqué. C'est exactement le problème de la classe de base fragile.
Je basculerais vers la **composition** : chaque classe assemble les comportements dont elle a besoin, injectés sous forme d'interfaces. Un comportement qui varie devient une Strategy injectée plutôt qu'une méthode surchargée.
Le gain : ajouter un comportement ne touche plus le code existant (c'est de l'Open/Closed) et on peut combiner librement au lieu d'être prisonnier d'une hiérarchie figée.
Je procéderais progressivement, en commençant par le comportement qui varie le plus, et avec des tests de caractérisation avant de toucher quoi que ce soit."

**Q : "Comment tu décides entre classe abstraite et interface ?"**
R : "Deux questions. Ai-je du **code ou de l'état à partager** entre des classes vraiment apparentées ? Si oui, classe abstraite : elle peut porter des champs, un constructeur, des méthodes concrètes.
Est-ce que je définis un **contrat** que des classes potentiellement sans lien vont remplir ? Alors interface, et ça autorise l'implémentation multiple, ce que l'héritage simple interdit.
Par défaut je penche vers l'interface : elle ne consomme pas l'unique droit d'héritage de la classe, et depuis Java 8 les méthodes `default` permettent déjà de partager du comportement. La classe abstraite reste pertinente quand il y a un vrai **état** commun à initialiser."

**Q : "Explique les 4 piliers de l'OOP avec un seul exemple."**
R : "Un système de paiement. **Abstraction** : une interface `MoyenPaiement` avec `payer(montant)`, qui dit *quoi* sans dire *comment*. **Encapsulation** : une classe `CarteBancaire` garde son numéro et son plafond privés et refuse un paiement au-delà du plafond, aucun code extérieur ne peut contourner la règle. **Héritage** : une `CarteBancairePremium` étend `CarteBancaire` en relevant le plafond, en restant substituable. **Polymorphisme** : le service de commande appelle `moyen.payer(total)` sans savoir s'il manipule une carte, un virement ou un portefeuille électronique ; ajouter un nouveau moyen ne modifie pas ce service."

═══════════════════════════════════════════
# 16. PRINCIPES SOLID
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
# 17. DESIGN PATTERNS
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
# 18. ARCHITECTURE LOGICIELLE
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
# 19. QUALITÉ, CLEAN CODE & MÉTHODES DE TRAVAIL
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
# 20. SPRING BOOT
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
# 21. SPRING AVANCÉ : DATA, SECURITY, CLOUD, BATCH
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

═══════════════════════════════════════════
# 22. QUARKUS
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

═══════════════════════════════════════════
# 23. JAKARTA EE, SERVLETS & MICROPROFILE
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

### Questions de situation

**Q : "Tu dois migrer une application Spring Boot 2.7 vers Spring Boot 3 ou 4."**
R : "Par étapes. D'abord monter en Java 17 minimum, puis passer en Boot 2.7 à jour et supprimer les avertissements de dépréciation, car beaucoup d'API dépréciées en 2.x sont supprimées en 3.
Le gros du travail est le renommage `javax` → `jakarta` dans le code et dans les dépendances : toutes les bibliothèques doivent avoir une version compatible Jakarta. **OpenRewrite** automatise une grande partie de la migration (recettes Spring Boot 3 et 4).
Ensuite les changements fonctionnels : configuration de Spring Security réécrite en `SecurityFilterChain`, Hibernate 6 (changements de génération d'identifiants et de types), propriétés renommées (le module `spring-boot-properties-migrator` les signale au démarrage), Micrometer Tracing à la place de Sleuth. Pour Boot 4 en plus : Jackson 3, modules de starters renommés, `@MockitoBean`.
Je migrerais sur une branche avec une CI complète, et je déploierais progressivement en surveillant les erreurs."

═══════════════════════════════════════════
# 24. API REST
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

═══════════════════════════════════════════
# 25. BASES DE DONNÉES
═══════════════════════════════════════════

### Vue d'ensemble

La base de données est presque toujours le **goulot d'étranglement** d'une application backend, et la cause la plus fréquente des problèmes de performance. Deux raisons : c'est le seul composant réellement partagé (on peut multiplier les instances applicatives, pas si facilement la base), et c'est là que se paient les accès disque.

Le modèle relationnel repose sur une idée forte : les données sont structurées en tables reliées, et le moteur garantit leur **cohérence** via des contraintes et des transactions ACID. Le prix à payer est une certaine rigidité de schéma et une scalabilité horizontale difficile, c'est ce qui a motivé l'émergence du NoSQL, qui relâche certaines garanties pour gagner en souplesse et en distribution.

En Java, l'ORM (JPA/Hibernate) fait le pont entre objets et tables. Il fait gagner énormément de temps, mais il **masque le SQL généré** : et la majorité des problèmes de performance viennent de là : on écrit trois lignes de code qui déclenchent mille requêtes.

### Cours

**SQL vs NoSQL.** Relationnel (PostgreSQL, MySQL, Oracle) : schéma structuré, jointures, ACID, idéal pour des données fortement reliées. NoSQL : document (MongoDB, données semi-structurées), clé-valeur (Redis, cache, sessions, très rapide), colonne (Cassandra, écriture massive, distribution), graphe (Neo4j, relations complexes).

**ACID** : Atomicity (tout ou rien), Consistency (d'un état valide à un autre), Isolation (les transactions concurrentes ne se perturbent pas), Durability (un commit survit à un crash).

**Niveaux d'isolation** et anomalies évitées :
| Niveau | Dirty read | Non-repeatable read | Phantom |
|---|---|---|---|
| READ_UNCOMMITTED | possible | possible | possible |
| READ_COMMITTED (défaut usuel) | évité | possible | possible |
| REPEATABLE_READ | évité | évité | possible |
| SERIALIZABLE | évité | évité | évité |

Plus on monte, plus c'est sûr et plus le débit baisse.

**Index.** Un index (généralement un B-tree) accélère la lecture au prix d'écritures plus lentes et d'espace disque. À créer sur les colonnes des `WHERE`, `JOIN` et `ORDER BY` fréquents. Un **index composite** `(a, b)` sert pour `a` seul ou `a, b`, mais pas pour `b` seul, c'est la *leftmost prefix rule*. Un **index couvrant** contient toutes les colonnes de la requête, qui n'a alors même plus besoin de lire la table.

Attention : un index n'est pas gratuit. Sur-indexer une table à forte écriture dégrade les performances.

**Plan d'exécution** : `EXPLAIN` (ou `EXPLAIN ANALYZE`) montre comment le moteur compte exécuter la requête. Ce qu'on cherche : un *sequential scan* là où un index devrait servir, un mauvais ordre de jointure, ou une estimation de cardinalité très éloignée du réel, signe de statistiques obsolètes.

**JPA / Hibernate.** JPA est la spécification, Hibernate l'implémentation dominante.
- **Cycle de vie d'une entité** : transient (nouvelle, non gérée) → managed (attachée au contexte de persistance, les modifications sont détectées automatiquement) → detached (contexte fermé) → removed.
- Le **contexte de persistance** est un cache de premier niveau : dans une même transaction, deux `findById` du même id ne déclenchent qu'une requête.
- **Dirty checking** : modifier une entité managée suffit, Hibernate génère l'UPDATE au flush.
- **Fetch LAZY vs EAGER** : LAZY charge à la demande (défaut pour les collections), EAGER charge immédiatement (défaut pour `@ManyToOne`). L'EAGER par défaut est un piège classique : il charge des graphes entiers sans qu'on le demande.

**Le problème N+1** : charger N entités puis, pour chacune, une requête pour ses relations. Une boucle innocente déclenche des centaines de requêtes. Solutions : `JOIN FETCH` en JPQL, `@EntityGraph`, `@BatchSize` (chargement par lots), ou une projection qui ne récupère que le nécessaire.

**Verrouillage** : **optimiste** (`@Version`, on vérifie au commit que la version n'a pas changé, sinon exception ; idéal quand les conflits sont rares) ou **pessimiste** (`SELECT FOR UPDATE`, on verrouille dès la lecture ; réduit le débit et expose aux deadlocks).

**Pool de connexions** (HikariCP) : les connexions sont coûteuses à créer, on les réutilise. Dimensionnement délicat, trop petit, les threads attendent ; trop grand, multiplié par le nombre de pods, on dépasse la limite du serveur de base.

**Migrations** : Flyway (SQL versionné) ou Liquibase (changelog). Versionnées avec le code. Jamais `ddl-auto: update` en production.

**Normalisation** : éviter la redondance et les anomalies de mise à jour.
- **1NF** : valeurs atomiques, pas de groupes répétés (pas de colonne "telephones" contenant une liste).
- **2NF** : 1NF + chaque attribut non clé dépend de **toute** la clé (pas d'une partie d'une clé composite).
- **3NF** : 2NF + aucun attribut non clé ne dépend d'un autre attribut non clé (pas de dépendance transitive : la ville ne doit pas être stockée avec le code postal dans la table client si le code postal la détermine).
- **BCNF** : version plus stricte de la 3NF.
La **dénormalisation** volontaire (dupliquer une donnée pour éviter une jointure coûteuse, une table de reporting) est un compromis assumé pour la lecture, au prix de la cohérence à maintenir.

**Clés et contraintes** : clé primaire (unique, non nulle), clé **naturelle** (un numéro de sécurité sociale, risquée car elle peut changer) vs **technique**/*surrogate* (identifiant sans signification, préférable), clé candidate, clé étrangère (intégrité référentielle, avec `ON DELETE CASCADE` / `RESTRICT` / `SET NULL`), contraintes `UNIQUE`, `NOT NULL`, `CHECK`, `DEFAULT`. Les contraintes en base sont la dernière ligne de défense : la validation applicative ne suffit pas en concurrence (deux inserts simultanés du même email passent tous les deux la vérification applicative ; seul un index unique les départage).

**Types d'index** : **B-tree** (défaut, égalité et intervalles, tri), **hash** (égalité seule), sous PostgreSQL **GIN** (tableaux, JSONB, recherche plein texte), **GiST** (géométrie, intervalles), **BRIN** (très grosses tables triées naturellement, comme des logs par date), index **partiel** (`WHERE actif = true`), index sur **expression** (`lower(email)`), index **unique**. Index **clustered** (la table est physiquement rangée selon l'index, c'est le cas de la clé primaire en InnoDB/MySQL) vs non clustered. **Sélectivité** : un index sur une colonne à faible cardinalité (un booléen) est rarement utilisé, le moteur préfère un scan.

**Lire un plan d'exécution (PostgreSQL)** : *Seq Scan* (lecture complète), *Index Scan* (index puis table), *Index Only Scan* (l'index suffit, index couvrant), *Bitmap Index/Heap Scan* (combine plusieurs index, beaucoup de lignes), jointures *Nested Loop* (bonne avec peu de lignes et un index sur la table interne), *Hash Join* (grosses tables sans ordre), *Merge Join* (entrées triées). Comparer `rows` estimé et `actual rows` : un grand écart signale des statistiques obsolètes.

**MVCC** (Multi-Version Concurrency Control, PostgreSQL, Oracle, InnoDB) : chaque transaction voit un **instantané** cohérent ; les écritures créent de nouvelles versions de lignes au lieu d'écraser. Conséquence : **les lecteurs ne bloquent pas les écrivains, et inversement**. Les anciennes versions sont nettoyées plus tard (VACUUM en PostgreSQL ; une transaction très longue empêche ce nettoyage et fait gonfler les tables).

**Verrous et deadlocks en base** : verrous de ligne posés par `UPDATE`/`DELETE`/`SELECT FOR UPDATE`. Deux transactions qui verrouillent les mêmes lignes dans un ordre différent créent un **deadlock** ; la base le détecte et annule l'une d'elles (l'application doit pouvoir réessayer). Prévention : toujours modifier les lignes dans le même ordre, transactions courtes. `SELECT ... FOR UPDATE SKIP LOCKED` permet à plusieurs workers de se répartir une file de tâches en base sans se bloquer. Les niveaux d'isolation réels diffèrent selon le moteur : PostgreSQL est en READ COMMITTED par défaut et son REPEATABLE READ empêche aussi les lectures fantômes ; MySQL InnoDB est en REPEATABLE READ par défaut.

**Anomalies supplémentaires** : *lost update* (deux transactions lisent puis écrasent), *write skew* (deux transactions lisent un même invariant et écrivent chacune une ligne différente qui le viole, seul SERIALIZABLE l'empêche).

**Réplication, partitionnement, sharding** :
- **Réplication** : copies de la base. Primaire/réplicas (lectures réparties, latence de réplication donc lectures possiblement périmées), synchrone (aucune perte, plus lent) vs asynchrone. Sert la disponibilité et la lecture.
- **Partitionnement** : découper une grosse table en partitions **sur le même serveur** (par date, par plage) ; les requêtes ne lisent que les partitions concernées et on supprime une vieille partition instantanément.
- **Sharding** : répartir les données sur **plusieurs serveurs** selon une clé (client, région). Scalabilité en écriture, mais jointures et transactions inter-shards difficiles, et le choix de la clé est quasi définitif.
- Scalabilité **verticale** (plus grosse machine, simple, plafonnée) vs **horizontale** (plus de machines, complexe).

**Vues, procédures, triggers** : une **vue** est une requête nommée ; une **vue matérialisée** stocke le résultat (rapide en lecture, à rafraîchir). Les **procédures stockées** et **triggers** placent de la logique en base : performants mais difficiles à tester, versionner et déboguer ; à réserver à des besoins précis (audit, intégrité).

**NoSQL, choisir et modéliser** :
- **Document** (MongoDB) : on modélise selon les **requêtes** ; on **embarque** ce qui est lu ensemble (une commande et ses lignes), on **référence** ce qui est partagé ou volumineux. Index secondaires, transactions multi-documents possibles mais coûteuses.
- **Clé-valeur** (Redis) : structures (string, hash, list, set, sorted set pour des classements, stream), TTL, opérations atomiques ; persistance RDB (instantanés) et AOF (journal) ; haute disponibilité par Sentinel ou Cluster.
- **Colonnes larges** (Cassandra) : écriture massive, distribution sans maître, modélisation **une table par requête**, cohérence réglable par requête.
- **Recherche** (Elasticsearch/OpenSearch) : **index inversé** (mot → documents), pertinence, agrégations ; jamais comme source de vérité, on l'alimente depuis la base principale.
- **Graphe** (Neo4j) : relations à plusieurs niveaux (réseaux sociaux, recommandations).
- **Séries temporelles** (InfluxDB, TimescaleDB).
- PostgreSQL couvre beaucoup de besoins "NoSQL" avec `JSONB`, la recherche plein texte et des extensions (pgvector pour la recherche vectorielle).

**CDC** (*Change Data Capture*, Debezium) : lire le journal de transactions de la base (WAL, binlog) pour publier chaque modification comme un événement, sans modifier l'application. Base possible du pattern outbox.

**Sauvegarde** : sauvegardes complètes + journaux pour une restauration à un instant donné (*point-in-time recovery*), objectifs **RPO** (perte de données maximale acceptable) et **RTO** (durée maximale de restauration), et surtout des tests de restauration réguliers.

### En situation

- **"Cette requête est lente"** → EXPLAIN, index, N+1, volume ramené.
- **"Modification concurrente"** → verrouillage optimiste vs pessimiste.
- **"Migration sans coupure"** → changements additifs, compatibilité des deux versions.
- **"La base sature"** → optimiser, cache, réplicas, pool.

### Questions techniques

**Q : Que signifie ACID ?** R : Atomicity, Consistency, Isolation, Durability.

**Q : Dirty read, non-repeatable read, phantom ?** R : Lire du non commité / relire et obtenir autre chose / voir apparaître de nouvelles lignes.

**Q : Cycle de vie d'une entité JPA ?** R : Transient → managed → detached → removed.

**Q : C'est quoi le dirty checking ?** R : Hibernate détecte les modifications d'une entité managée et génère l'UPDATE au flush.

**Q : Fetch LAZY vs EAGER ?** R : À la demande vs immédiatement. EAGER par défaut sur @ManyToOne est un piège.

**Q : C'est quoi le N+1 et comment le résoudre ?** R : 1 requête + N requêtes pour les relations. Résolu par JOIN FETCH, @EntityGraph, @BatchSize.

**Q : Leftmost prefix rule ?** R : Un index `(a,b)` sert pour `a` ou `a,b`, jamais pour `b` seul.

**Q : C'est quoi un index couvrant ?** R : Un index contenant toutes les colonnes de la requête, qui évite de lire la table.

**Q : Optimiste vs pessimiste ?** R : Vérification de version au commit vs verrou dès la lecture.

**Q : Pourquoi un pool de connexions ?** R : Créer une connexion est coûteux ; on les réutilise. À dimensionner selon la capacité de la base.

**Q : Les trois premières formes normales ?** R : 1NF valeurs atomiques ; 2NF dépendance de toute la clé ; 3NF pas de dépendance transitive entre attributs non clés.

**Q : Pourquoi dénormaliser ?** R : Pour accélérer les lectures en évitant des jointures, au prix de la redondance.

**Q : Clé naturelle vs technique ?** R : Une donnée métier qui identifie (peut changer) vs un identifiant sans signification (stable).

**Q : Pourquoi une contrainte unique en base même si l'application vérifie ?** R : Deux requêtes concurrentes peuvent passer la vérification applicative ; seule la base garantit l'unicité.

**Q : Index B-tree vs hash ?** R : Égalité, intervalles et tri vs égalité uniquement.

**Q : C'est quoi un index partiel ?** R : Un index qui ne couvre que les lignes vérifiant une condition.

**Q : Pourquoi un index sur un booléen est-il souvent inutile ?** R : Sa sélectivité est trop faible ; le moteur préfère un scan.

**Q : Index clustered ?** R : La table est physiquement ordonnée selon cet index (une seule par table).

**Q : Seq Scan vs Index Scan vs Index Only Scan ?** R : Lecture de toute la table / index puis table / index seul.

**Q : C'est quoi le MVCC ?** R : Chaque transaction lit un instantané ; les écritures créent des versions, donc lecteurs et écrivains ne se bloquent pas.

**Q : Pourquoi une transaction très longue est-elle nocive en PostgreSQL ?** R : Elle retient des verrous et empêche le VACUUM de nettoyer les anciennes versions.

**Q : Comment la base gère-t-elle un deadlock ?** R : Elle le détecte et annule une des transactions, qu'il faut pouvoir réessayer.

**Q : À quoi sert SKIP LOCKED ?** R : À ignorer les lignes déjà verrouillées, pour que plusieurs workers consomment une file en base sans se bloquer.

**Q : Niveau d'isolation par défaut de PostgreSQL et MySQL ?** R : READ COMMITTED pour PostgreSQL, REPEATABLE READ pour MySQL InnoDB.

**Q : C'est quoi un write skew ?** R : Deux transactions qui violent ensemble un invariant en modifiant chacune une ligne différente ; seul SERIALIZABLE l'empêche.

**Q : Réplication vs partitionnement vs sharding ?** R : Copies des données / découpage d'une table sur un serveur / répartition des données sur plusieurs serveurs.

**Q : Vue vs vue matérialisée ?** R : Requête nommée recalculée à chaque appel vs résultat stocké à rafraîchir.

**Q : Embarquer ou référencer en MongoDB ?** R : Embarquer ce qui est lu ensemble et borné ; référencer ce qui est partagé, volumineux ou croissant.

**Q : Les structures de données de Redis ?** R : String, hash, list, set, sorted set, stream, bitmap, HyperLogLog.

**Q : RDB vs AOF en Redis ?** R : Instantanés périodiques vs journal de toutes les écritures (plus durable).

**Q : C'est quoi un index inversé ?** R : Une structure qui associe chaque terme aux documents qui le contiennent (Elasticsearch).

**Q : C'est quoi le CDC ?** R : Capturer les modifications depuis le journal de la base pour les publier en événements (Debezium).

**Q : RPO vs RTO ?** R : Perte de données maximale acceptable vs durée maximale d'indisponibilité.

### Questions de situation

**Q : "Une requête qui prenait 50 ms en prend 3 secondes."**
R : "Je procéderais par élimination.
D'abord : **qu'est-ce qui a changé ?** Volume de données, déploiement modifiant la requête, index supprimé ? Une dégradation soudaine a presque toujours une cause identifiable.
Ensuite le **plan d'exécution** avec `EXPLAIN ANALYZE`. Je cherche un scan séquentiel là où un index devrait servir, un mauvais ordre de jointure, ou une estimation de cardinalité très fausse, auquel cas les statistiques sont obsolètes et un `ANALYZE` peut suffire.
Le scénario le plus fréquent : la table a grossi, et une requête qui scannait mille lignes en scanne un million. L'index qui n'était pas nécessaire à petite échelle le devient.
Je vérifierais aussi côté application qu'il ne s'agit pas d'un **N+1** : une lenteur apparente peut cacher des centaines de requêtes. Les logs SQL ou une trace le montrent immédiatement.
Et si la requête est correcte mais ramène trop de données, la réponse est la pagination ou une projection ciblée."

**Q : "Deux utilisateurs modifient la même commande, l'un écrase l'autre."**
R : "C'est un *lost update*. Deux stratégies selon la fréquence des conflits.
Conflits **rares** : le cas courant : **verrouillage optimiste** avec un champ `@Version`. Au commit, Hibernate vérifie que la version en base est celle qui avait été lue ; sinon il lève une `OptimisticLockException`. On n'a bloqué personne et on détecte au bon moment. Côté API, je remonterais un **409 Conflict** en demandant de recharger, c'est aussi la meilleure expérience, l'utilisateur voit les modifications de l'autre avant de refaire les siennes.
Conflits **fréquents** et rejeu coûteux : **verrouillage pessimiste** avec `SELECT FOR UPDATE`. Mais ça bloque les autres transactions, réduit le débit et expose aux deadlocks, donc en dernier recours.
Dans les deux cas, je garderais la transaction la plus courte possible : jamais d'appel réseau à l'intérieur."

**Q : "Comment faire une migration de schéma sans interruption ?"**
R : "Le point clé : pendant un déploiement progressif, **l'ancienne et la nouvelle version du code tournent simultanément**. Le schéma doit être compatible avec les deux.
Donc des migrations **progressives en plusieurs déploiements**. Pour renommer une colonne, jamais de `RENAME` direct : d'abord ajouter la nouvelle et écrire dans les deux ; puis déployer le code qui lit la nouvelle et migrer les données existantes ; enfin, quand plus aucune version n'utilise l'ancienne, la supprimer.
Règle générale : les changements **additifs** sont sûrs (colonne nullable, index, table). Les changements **destructifs** doivent être différés.
J'utiliserais Flyway ou Liquibase versionné avec le code, et jamais `ddl-auto: update` en production.
Attention enfin aux migrations **lourdes** : créer un index sur une grosse table peut la verrouiller longtemps. Sur PostgreSQL, `CREATE INDEX CONCURRENTLY` évite ça."

**Q : "La base sature sous la charge."**
R : "Du moins au plus coûteux.
**Optimiser l'existant** : identifier les requêtes les plus consommatrices via les statistiques de la base, corriger les N+1, ajouter les index manquants, éliminer ce qui ramène trop de données. C'est presque toujours là que se trouve le gros du gain.
**Réduire le nombre d'appels** : du cache applicatif ou distribué sur les données lues souvent et modifiées rarement, une donnée servie depuis Redis n'atteint jamais la base.
**Vérifier la configuration** : le pool de connexions multiplié par le nombre de pods peut dépasser la limite du serveur.
**Répartir** : des réplicas en lecture pour décharger le primaire, efficace quand le trafic est majoritairement en lecture, en acceptant un léger décalage de réplication.
Et en dernier recours seulement, **partitionner ou sharder** : structurant, difficile à défaire, et complique toutes les requêtes transverses.
Le fil directeur : optimiser avant de scaler, sinon on paie plus cher pour le même problème."

**Q : "Deux utilisateurs s'inscrivent au même instant avec le même email et les deux comptes sont créés."**
R : "La vérification 'l'email existe-t-il ?' suivie de l'insertion est un **check-then-act** : deux transactions concurrentes passent la vérification avant que l'une ou l'autre n'insère. Aucun niveau d'isolation usuel ne l'empêche en READ COMMITTED.
La correction est une **contrainte unique** en base sur l'email (normalisé en minuscules, éventuellement via un index sur `lower(email)`). L'application traduit la violation de contrainte en réponse 409. Je garderais la vérification applicative pour un message d'erreur agréable dans le cas courant, mais c'est la base qui garantit l'invariant."

**Q : "Une table de 2 milliards de lignes d'événements devient ingérable."**
R : "Je regarderais d'abord les **usages** : quelles requêtes, sur quelle période ? Le plus souvent on interroge les données récentes et on garde l'historique pour l'audit.
Dans ce cas, un **partitionnement par date** (mensuel par exemple) : les requêtes sur la période récente ne lisent qu'une partition, les index restent petits, et purger ou archiver une période revient à détacher une partition, instantanément, au lieu d'un `DELETE` massif.
J'archiverais les partitions anciennes vers un stockage moins cher (stockage objet en Parquet, entrepôt analytique). Pour l'analytique lourde, une base orientée colonnes ou un entrepôt dédié serait plus adaptée qu'une base transactionnelle. Le sharding n'interviendrait que si une seule machine ne suffit plus en écriture."

═══════════════════════════════════════════
# 26. JPA, HIBERNATE & JDBC EN PROFONDEUR
═══════════════════════════════════════════

### Vue d'ensemble

JPA (Jakarta Persistence) est la spécification de l'ORM en Java, Hibernate son implémentation dominante. L'ORM promet de manipuler des objets plutôt que des lignes, et il tient cette promesse pour l'essentiel du code CRUD. Mais il le fait au prix d'une **abstraction qui fuit** : les relations, le chargement paresseux, le cache de premier niveau et le moment exact où les requêtes partent ne sont pas visibles dans le code, et c'est là que naissent les N+1, les `LazyInitializationException` et les mises à jour fantômes.

En entretien, on attend de savoir mapper correctement une relation, expliquer le cycle de vie d'une entité et le contexte de persistance, diagnostiquer un N+1, et savoir quand **ne pas** utiliser l'ORM (traitements de masse, rapports). JDBC reste la couche sous-jacente : savoir l'utiliser directement fait partie des bases.

### Cours

**JDBC, les fondamentaux** :
```java
String sql = "SELECT id, nom FROM client WHERE ville = ?";
try (Connection c = dataSource.getConnection();
     PreparedStatement ps = c.prepareStatement(sql)) {
    ps.setString(1, ville);
    try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            liste.add(new Client(rs.getLong("id"), rs.getString("nom")));
        }
    }
}
```
- **`PreparedStatement` vs `Statement`** : requête précompilée avec paramètres liés, donc **protection contre l'injection SQL** et réutilisation du plan. Ne jamais concaténer une valeur dans une requête.
- Transactions : `connection.setAutoCommit(false)`, puis `commit()` / `rollback()` ; par défaut chaque requête est committée seule.
- **Batch** : `ps.addBatch()` dans une boucle puis `executeBatch()`, des ordres de grandeur plus rapide que des inserts individuels.
- Toujours obtenir les connexions d'un **pool** (`DataSource`, HikariCP) et tout fermer en try-with-resources.
- `CallableStatement` pour les procédures stockées. `ResultSet` : curseur avant uniquement par défaut, `setFetchSize` pour lire de gros résultats par paquets.

**Mapping d'une entité** : `@Entity`, `@Table`, `@Id`, `@Column(nullable, length, unique)`, `@Enumerated(EnumType.STRING)`, `@Embedded`/`@Embeddable` (value object stocké dans les colonnes de l'entité, par exemple une adresse), `@Transient` (non persisté), `@Lob`, `@Version`. JPA exige un constructeur sans argument (au moins protected) et une classe non final (pour les proxys de chargement paresseux). Les **records** ne peuvent pas être des entités (immuables, final), mais servent très bien de projections et d'`@Embeddable` depuis Hibernate 6.2.

**Génération des identifiants** :
| Stratégie | Fonctionnement | Remarque |
|---|---|---|
| `IDENTITY` | colonne auto-incrémentée | l'ID n'est connu qu'après l'INSERT, ce qui **désactive le batch d'insertion** d'Hibernate |
| `SEQUENCE` | séquence en base | recommandé ; avec un `allocationSize` (50 par défaut), Hibernate réserve des plages d'IDs et peut batcher |
| `TABLE` | table de compteurs | lent, à éviter |
| `UUID` | `@GeneratedValue` avec `UUID` (JPA 3.1) ou `@UuidGenerator` | pas d'aller-retour en base ; préférer un UUID v7 ordonné |

**Relations** :
- `@ManyToOne` (le côté le plus fréquent, porte la clé étrangère), `@OneToMany`, `@OneToOne`, `@ManyToMany` (table de jointure ; dès qu'elle porte des attributs, la remplacer par une entité intermédiaire avec deux `@ManyToOne`).
- **Côté propriétaire** : celui qui détient la clé étrangère (le `@ManyToOne`) ; l'autre côté déclare `mappedBy` et n'est **pas** pris en compte pour l'écriture. Dans une relation bidirectionnelle, il faut **synchroniser les deux côtés** en mémoire, par des méthodes utilitaires (`commande.ajouterLigne(l)` qui fait `lignes.add(l)` et `l.setCommande(this)`).
- **Valeurs par défaut du fetch** : `@ManyToOne` et `@OneToOne` en **EAGER**, `@OneToMany` et `@ManyToMany` en **LAZY**. Bonne pratique : tout passer en `LAZY` (`@ManyToOne(fetch = FetchType.LAZY)`) et charger explicitement.
- Un `@OneToMany` **unidirectionnel** sans `@JoinColumn` crée une table de jointure inutile : préférer le bidirectionnel ou le `@ManyToOne` seul.
- **Cascade** : `PERSIST`, `MERGE`, `REMOVE`, `REFRESH`, `DETACH`, `ALL`. À poser du parent vers les enfants qu'il possède (une commande et ses lignes), jamais sur un `@ManyToOne` (supprimer une ligne ne doit pas supprimer la commande, ni le client).
- `orphanRemoval = true` : retirer un enfant de la collection le supprime en base.

**Cycle de vie et opérations de l'EntityManager** :
- `persist(e)` : une entité *transient* devient *managed* (INSERT au flush).
- `find(Classe, id)` : charge (ou renvoie depuis le contexte) ; `getReference` renvoie un proxy sans requête immédiate.
- `merge(e)` : copie l'état d'une entité *detached* dans une entité managée (qu'il charge si besoin) et **renvoie cette copie** ; l'objet passé reste détaché. Piège classique : continuer à modifier l'objet d'origine.
- `remove(e)` : *managed* → *removed* (DELETE au flush).
- `detach`, `clear`, `refresh`.
- **flush** : synchronisation du contexte vers la base (envoi des SQL), qui n'est pas un commit. Mode `AUTO` par défaut : flush avant le commit et avant une requête qui pourrait lire des données modifiées.

**Le contexte de persistance** = cache de premier niveau + *unit of work* + *identity map* : dans une transaction, une ligne correspond à un seul objet, les modifications sont détectées par **dirty checking** (comparaison avec un instantané pris au chargement) et regroupées au flush. Conséquence : charger 100 000 entités dans une transaction remplit la mémoire et rend chaque flush coûteux. Pour les traitements de masse : traiter par paquets avec `flush()` + `clear()` réguliers, activer `hibernate.jdbc.batch_size`, `order_inserts`/`order_updates`, ou utiliser une `StatelessSession`, JDBC, ou des requêtes bulk (`update ... where`).

**LazyInitializationException** : on accède à une relation paresseuse alors que la session est fermée (après la transaction, dans le contrôleur, pendant la sérialisation JSON). Solutions : charger ce qu'il faut dans le service (fetch join, entity graph), mapper vers un DTO **dans** la transaction. Mauvaises solutions : passer en EAGER (charge tout, partout), activer `open-in-view` (masque le problème), `hibernate.enable_lazy_load_no_trans` (une requête et une connexion par accès).

**N+1, toutes les solutions** : `JOIN FETCH` en JPQL (`select c from Commande c join fetch c.lignes where ...`), `@EntityGraph`, `@BatchSize(size = 50)` ou `hibernate.default_batch_fetch_size` (charge les collections par lots avec un `IN`), `@Fetch(FetchMode.SUBSELECT)`, ou une projection DTO. **Détecter** : activer les logs SQL en développement (`spring.jpa.show-sql`, ou mieux le logger `org.hibernate.SQL` et les statistiques Hibernate), compter les requêtes dans les tests (datasource-proxy, Hypersistence Utils).

**Pièges du fetch join** : deux collections en `JOIN FETCH` dans la même requête produisent un produit cartésien, et Hibernate lève `MultipleBagFetchException` si ce sont des `List` (*bags*) ; charger une collection par requête, ou utiliser des `Set`. Combiner `JOIN FETCH` sur une collection et pagination fait paginer **en mémoire** (avertissement *HHH90003004*) : paginer d'abord sur les identifiants, puis charger les collections pour ces identifiants.

**Requêtes** : **JPQL** (sur les entités et leurs attributs, pas sur les tables), **Criteria API** (construction programmatique typée, avec le métamodèle généré `Client_`), **SQL natif** (`createNativeQuery`), **requêtes nommées** (`@NamedQuery`, vérifiées au démarrage). Projections DTO : `select new com.x.ClientDto(c.id, c.nom) from Client c`, ou des records avec Hibernate 6.

**Héritage** :
| Stratégie | Schéma | Avantages / inconvénients |
|---|---|---|
| `SINGLE_TABLE` (défaut) | une table, colonne discriminante | rapide, pas de jointure ; colonnes nullables |
| `JOINED` | une table par classe, jointures | normalisé ; jointures à chaque lecture |
| `TABLE_PER_CLASS` | une table par classe concrète | requêtes polymorphes en UNION, lentes |
| `@MappedSuperclass` | pas d'entité parente | partage de champs communs (id, audit) sans polymorphisme en requête |

**equals et hashCode d'une entité** : ne pas les baser sur un identifiant généré (null avant persist, donc le hashCode change après l'insertion et l'entité se perd dans un HashSet). Options : une **clé métier** naturelle et immuable (un numéro de commande), ou un identifiant attribué à la création (UUID), ou equals sur l'id avec un hashCode **constant** par classe. Et jamais de `@Data` de Lombok sur une entité.

**Cache de second niveau** : partagé entre sessions (au niveau de l'`EntityManagerFactory`), via un fournisseur (Ehcache, Infinispan, Caffeine via JCache), activé par entité avec `@Cacheable` / `@Cache(usage = READ_WRITE)`. Utile pour des données de référence très lues et rarement modifiées ; dangereux si d'autres applications écrivent dans la base (le cache ne le sait pas) ou en multi-instance sans cache distribué. Le **cache de requêtes** stocke les identifiants résultats d'une requête et s'invalide à chaque modification des tables concernées.

**Verrouillage** : optimiste avec `@Version` (`OptimisticLockException`), pessimiste avec `LockModeType.PESSIMISTIC_WRITE` (traduit en `SELECT ... FOR UPDATE`), avec un timeout de verrou.

**Callbacks et écouteurs** : `@PrePersist`, `@PostPersist`, `@PreUpdate`, `@PostLoad`, `@PreRemove`, `@EntityListeners` (audit). Ils ne sont pas déclenchés par les requêtes bulk ni le SQL natif.

**Hibernate 6 et 7** : Hibernate 6 a réécrit le moteur de requêtes (SQM, meilleur support du SQL), changé certaines stratégies de génération d'identifiants et le mapping de types ; Hibernate 7 implémente Jakarta Persistence 3.2 (base de Spring Boot 4). **`ddl-auto`** (`create`, `create-drop`, `update`, `validate`, `none`) : jamais `update` en production, `validate` vérifie que le schéma (géré par Flyway/Liquibase) correspond aux entités.

**Quand ne pas utiliser l'ORM** : rapports et agrégations complexes, imports/exports massifs, requêtes très spécifiques au moteur. Alternatives : JDBC/`JdbcClient`, **jOOQ** (SQL typé généré depuis le schéma), **MyBatis** (SQL écrit à la main, mapping automatique).

### En situation

- **"Une page déclenche 300 requêtes"** → N+1 → fetch join, entity graph, batch size, DTO.
- **"LazyInitializationException dans le contrôleur"** → charger dans le service, mapper en DTO dans la transaction.
- **"L'insertion de 100 000 lignes prend 10 minutes"** → IDENTITY empêche le batch, contexte qui grossit → SEQUENCE, batch_size, flush/clear, ou JDBC.
- **"Une modification n'est pas sauvegardée"** → entité détachée et merge dont on ignore le retour, ou modification hors transaction.
- **"Une modification est sauvegardée alors qu'on n'a pas appelé save"** → dirty checking sur une entité managée.
- **"Les entités disparaissent d'un HashSet après persist"** → hashCode basé sur un id généré.

### Questions techniques

**Q : JPA vs Hibernate ?** R : La spécification vs son implémentation principale.

**Q : PreparedStatement vs Statement ?** R : Paramètres liés et requête précompilée (protection contre l'injection, réutilisation du plan) vs SQL concaténé.

**Q : Comment faire un insert massif en JDBC ?** R : addBatch puis executeBatch, dans une transaction.

**Q : Pourquoi IDENTITY empêche-t-il le batch d'insertion ?** R : Hibernate doit exécuter chaque INSERT immédiatement pour connaître l'identifiant généré.

**Q : Quelle stratégie d'ID recommander ?** R : SEQUENCE avec un allocationSize adapté, ou un UUID v7 attribué par l'application.

**Q : Pourquoi une entité ne peut-elle pas être final ?** R : Hibernate crée des sous-classes proxy pour le chargement paresseux.

**Q : C'est quoi le côté propriétaire d'une relation ?** R : Celui qui porte la clé étrangère ; c'est lui qui est pris en compte à l'écriture, l'autre déclare mappedBy.

**Q : Fetch par défaut des relations ?** R : ManyToOne et OneToOne en EAGER, OneToMany et ManyToMany en LAZY.

**Q : Pourquoi mettre les ManyToOne en LAZY ?** R : Pour éviter de charger des graphes entiers et des N+1 cachés.

**Q : Où placer une cascade REMOVE ?** R : Du parent vers les enfants qu'il possède, jamais vers une entité partagée (ManyToOne).

**Q : cascade REMOVE vs orphanRemoval ?** R : Supprimer les enfants avec le parent vs supprimer un enfant retiré de la collection.

**Q : persist vs merge ?** R : Rend managée une entité nouvelle vs copie l'état d'une entité détachée dans une entité managée et renvoie celle-ci.

**Q : find vs getReference ?** R : Charge l'entité (ou null) vs renvoie un proxy sans requête immédiate (exception à l'accès si inexistante).

**Q : flush vs commit ?** R : Envoi des SQL à la base vs validation définitive de la transaction.

**Q : C'est quoi le contexte de persistance ?** R : Le cache de premier niveau et l'unit of work d'une session : une instance par ligne, dirty checking, écriture au flush.

**Q : Comment traiter un million d'entités sans saturer la mémoire ?** R : Par paquets avec flush/clear, batch JDBC, StatelessSession, ou requêtes bulk.

**Q : Causes et solutions d'une LazyInitializationException ?** R : Accès à une relation lazy session fermée ; charger explicitement (fetch join, entity graph) ou mapper en DTO dans la transaction.

**Q : Toutes les solutions au N+1 ?** R : JOIN FETCH, @EntityGraph, @BatchSize / default_batch_fetch_size, SUBSELECT, projections DTO.

**Q : C'est quoi une MultipleBagFetchException ?** R : L'erreur levée quand on fetch-joint plusieurs collections de type List dans une même requête.

**Q : Que se passe-t-il avec JOIN FETCH + pagination sur une collection ?** R : Hibernate pagine en mémoire après avoir tout chargé ; paginer d'abord les identifiants.

**Q : JPQL vs SQL ?** R : JPQL interroge les entités et attributs, portable ; SQL interroge les tables.

**Q : Les stratégies d'héritage JPA ?** R : SINGLE_TABLE, JOINED, TABLE_PER_CLASS, et @MappedSuperclass sans polymorphisme.

**Q : Pourquoi ne pas baser hashCode sur l'id généré ?** R : Il est null avant persist, donc le hashCode change et l'entité devient introuvable dans un Set.

**Q : Cache de premier vs second niveau ?** R : Par session/transaction, toujours actif vs partagé entre sessions, optionnel et configuré par entité.

**Q : Risque du cache de second niveau ?** R : Données périmées si la base est modifiée par d'autres applications ou par du SQL natif.

**Q : Les callbacks sont-ils déclenchés par une requête bulk ?** R : Non, ni les callbacks ni les cascades.

**Q : Valeurs de ddl-auto et laquelle en production ?** R : create, create-drop, update, validate, none ; validate ou none, avec Flyway/Liquibase.

**Q : Les records peuvent-ils être des entités ?** R : Non (immuables et final), mais ils servent de projections et d'embeddables.

**Q : Quand préférer jOOQ ou MyBatis ?** R : Pour un SQL complexe et maîtrisé (rapports, requêtes spécifiques) où l'ORM gêne plus qu'il n'aide.

### Questions de situation

**Q : "Mappe une commande avec ses lignes de commande et son client."**
R :
```java
@Entity
class Commande {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE) Long id;
    @Version Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    Client client;                                   // pas de cascade vers le client

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    List<LigneCommande> lignes = new ArrayList<>();

    void ajouterLigne(LigneCommande l) { lignes.add(l); l.commande = this; }
    void retirerLigne(LigneCommande l) { lignes.remove(l); l.commande = null; }
}

@Entity
class LigneCommande {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE) Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    Commande commande;                               // côté propriétaire
    String produit; int quantite;
}
```
"La commande est l'agrégat : elle possède ses lignes, d'où la cascade et `orphanRemoval`. Le client est une entité indépendante, donc ni cascade ni EAGER. Les méthodes d'ajout synchronisent les deux côtés. `@Version` protège des modifications concurrentes, et la séquence permet le batch d'insertion des lignes."

**Q : "Un traitement nocturne met à jour le statut de 2 millions de commandes et dure des heures."**
R : "Si la règle s'exprime en SQL, une **requête bulk** unique (`update Commande c set c.statut = :s where c.date < :d`) fait le travail en secondes, sans charger aucune entité. Attention : elle contourne les callbacks, le `@Version` et le cache, ce qu'il faut accepter ou compenser.
Si chaque commande demande une logique Java, je traiterais **par paquets** : lecture paginée par curseur sur l'identifiant, traitement, `flush()` et `clear()` tous les 500 à 1 000 éléments pour que le contexte de persistance ne grossisse pas, `hibernate.jdbc.batch_size` activé pour regrouper les UPDATE, une transaction par paquet pour pouvoir reprendre. Spring Batch apporte ce cadre avec la reprise sur erreur."

**Q : "Les logs montrent un UPDATE sur des entités que personne n'a modifiées."**
R : "C'est le dirty checking qui détecte une différence. Causes fréquentes : un setter ou une méthode `@PostLoad` qui normalise une valeur au chargement, un type mutable mal comparé (un `Date`, un tableau, un champ JSON converti), un `AttributeConverter` dont la conversion aller-retour ne redonne pas exactement la même valeur, ou une collection remplacée par une nouvelle instance.
Je chercherais quelles colonnes changent dans l'UPDATE, et je corrigerais la source. Pour une lecture pure, `@Transactional(readOnly = true)` désactive le dirty checking et supprime le problème au passage."

═══════════════════════════════════════════
# 27. SQL PRATIQUE
═══════════════════════════════════════════

### Vue d'ensemble

Même sur un poste où l'ORM fait l'essentiel, savoir écrire et lire du SQL reste attendu, pour comprendre ce qu'Hibernate génère, pour diagnostiquer une lenteur, et parce que certains traitements (rapports, agrégations, migrations) s'écrivent bien mieux directement en SQL.

L'ordre d'exécution logique d'une requête explique la plupart des erreurs courantes : `FROM` → `JOIN` → `WHERE` → `GROUP BY` → `HAVING` → `SELECT` → `ORDER BY` → `LIMIT`. C'est parce que `WHERE` s'exécute avant `GROUP BY` qu'on ne peut pas y filtrer sur un agrégat, il faut `HAVING`. Et c'est parce que `SELECT` vient après `GROUP BY` qu'on ne peut sélectionner qu'une colonne groupée ou agrégée.

### Cours

**Jointures :**
```sql
INNER JOIN   -- uniquement les lignes correspondantes des deux côtés
LEFT JOIN    -- toutes les lignes de gauche, NULL à droite si pas de correspondance
RIGHT JOIN   -- l'inverse
FULL JOIN    -- tout des deux côtés
CROSS JOIN   -- produit cartésien
```
Astuce fréquente en entretien : trouver les lignes **sans correspondance** se fait par `LEFT JOIN ... WHERE b.id IS NULL`.

**Agrégation :**
```sql
SELECT city, COUNT(*) AS nb, AVG(age) AS moyenne
FROM users
WHERE active = true          -- filtre les lignes AVANT groupement
GROUP BY city
HAVING COUNT(*) > 10         -- filtre les groupes APRÈS agrégation
ORDER BY nb DESC
LIMIT 10;
```
Fonctions : `COUNT`, `SUM`, `AVG`, `MIN`, `MAX`. Attention : `COUNT(*)` compte les lignes, `COUNT(colonne)` ignore les NULL.

**Sous-requêtes :**
```sql
-- dans le WHERE
SELECT * FROM users WHERE id IN (SELECT user_id FROM orders WHERE total > 100);

-- corrélée (exécutée par ligne, attention aux performances)
SELECT u.* FROM users u
WHERE EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id);

-- dans le FROM (table dérivée)
SELECT * FROM (SELECT city, COUNT(*) n FROM users GROUP BY city) t WHERE t.n > 5;
```
`EXISTS` est souvent plus performant que `IN` sur de gros volumes, car il s'arrête au premier résultat trouvé.

**CTE (Common Table Expression)** : bien plus lisible que des sous-requêtes imbriquées :
```sql
WITH ventes_par_client AS (
    SELECT client_id, SUM(montant) AS total
    FROM commandes GROUP BY client_id
)
SELECT c.nom, v.total
FROM clients c JOIN ventes_par_client v ON v.client_id = c.id
WHERE v.total > 1000;
```

**Fonctions de fenêtrage** : la partie qui distingue un candidat :
```sql
SELECT nom, service, salaire,
       RANK()       OVER (PARTITION BY service ORDER BY salaire DESC) AS rang,
       AVG(salaire) OVER (PARTITION BY service)                       AS moy_service,
       LAG(salaire) OVER (ORDER BY date_embauche)                     AS precedent
FROM employes;
```
Contrairement à `GROUP BY`, une fonction de fenêtrage **ne réduit pas le nombre de lignes** : elle ajoute une valeur calculée sur un ensemble lié à chaque ligne. Principales : `ROW_NUMBER`, `RANK`, `DENSE_RANK`, `LAG`/`LEAD`, `SUM/AVG OVER`.

Cas d'usage typique en entretien : *"le top 3 par catégorie"*, impossible proprement avec `GROUP BY`, immédiat avec `ROW_NUMBER() OVER (PARTITION BY categorie ORDER BY ...)` filtré dans une CTE.

**NULL** : `NULL` n'est égal à rien, pas même à lui-même. On teste avec `IS NULL` / `IS NOT NULL`. `COALESCE(a, b, 'défaut')` retourne la première valeur non nulle. Piège : `NOT IN` avec un NULL dans la liste ne retourne jamais rien.

**Modification :**
```sql
INSERT INTO t (a, b) VALUES (1, 2);
UPDATE t SET a = 1 WHERE id = 5;         -- ne JAMAIS oublier le WHERE
DELETE FROM t WHERE id = 5;
-- upsert PostgreSQL
INSERT INTO t (id, val) VALUES (1, 'x')
ON CONFLICT (id) DO UPDATE SET val = EXCLUDED.val;
```

**Transactions** : `BEGIN` → opérations → `COMMIT` ou `ROLLBACK`.

**Performance** : `EXPLAIN ANALYZE` pour le plan réel. Éviter `SELECT *` (charge des colonnes inutiles et empêche les index couvrants), éviter une fonction sur une colonne indexée dans le `WHERE` (`WHERE YEAR(date) = 2024` empêche l'usage de l'index, préférer `WHERE date >= '2024-01-01' AND date < '2025-01-01'`).

**Familles de commandes** : **DDL** (`CREATE`, `ALTER`, `DROP`, `TRUNCATE`), **DML** (`SELECT`, `INSERT`, `UPDATE`, `DELETE`, `MERGE`), **DCL** (`GRANT`, `REVOKE`), **TCL** (`COMMIT`, `ROLLBACK`, `SAVEPOINT`).

**DELETE vs TRUNCATE vs DROP** : `DELETE` supprime des lignes (avec `WHERE`, journalisé ligne par ligne, déclenche les triggers, annulable) ; `TRUNCATE` vide toute la table d'un coup (très rapide, réinitialise souvent les séquences, transactionnel en PostgreSQL mais pas en Oracle ni en MySQL) ; `DROP` supprime la table elle-même.

**Opérations ensemblistes** : `UNION` (fusionne et **déduplique**, donc tri ou hachage coûteux), `UNION ALL` (fusionne sans dédupliquer, à préférer quand c'est possible), `INTERSECT`, `EXCEPT` (`MINUS` en Oracle).

**CASE** : `SUM(CASE WHEN statut = 'PAYEE' THEN montant ELSE 0 END)` permet de faire des colonnes conditionnelles (pivot manuel). PostgreSQL propose aussi `COUNT(*) FILTER (WHERE ...)`.

**Requête récursive** (hiérarchie employé → manager, arborescence de catégories) :
```sql
WITH RECURSIVE hierarchie AS (
    SELECT id, nom, manager_id, 1 AS niveau FROM employes WHERE manager_id IS NULL
    UNION ALL
    SELECT e.id, e.nom, e.manager_id, h.niveau + 1
    FROM employes e JOIN hierarchie h ON e.manager_id = h.id
)
SELECT * FROM hierarchie;
```

**Auto-jointure** (employés et leur manager) :
```sql
SELECT e.nom AS employe, m.nom AS manager
FROM employes e LEFT JOIN employes m ON e.manager_id = m.id;
```

**Pagination par curseur (keyset)** :
```sql
SELECT * FROM commandes
WHERE (date_creation, id) < (:derniere_date, :dernier_id)
ORDER BY date_creation DESC, id DESC
LIMIT 20;
```
Avec un index sur `(date_creation, id)`, chaque page coûte le même prix, contrairement à `OFFSET 100000` qui lit et jette 100 000 lignes.

**Agrégats et NULL** : `SUM`, `AVG`, `MIN`, `MAX`, `COUNT(colonne)` ignorent les NULL ; `AVG` d'une colonne avec des NULL ne compte pas ces lignes au dénominateur. `SUM` sur zéro ligne renvoie NULL, d'où `COALESCE(SUM(x), 0)`. Agrégation de chaînes : `STRING_AGG(nom, ', ')` (PostgreSQL, SQL Server), `GROUP_CONCAT` (MySQL), `LISTAGG` (Oracle). Sous-totaux : `GROUP BY ROLLUP(a, b)`, `CUBE`, `GROUPING SETS`.

**Fonctions de fenêtrage, compléments** : différence entre `ROW_NUMBER` (1, 2, 3, 4 même en cas d'égalité), `RANK` (1, 2, 2, 4) et `DENSE_RANK` (1, 2, 2, 3). Cumul : `SUM(montant) OVER (ORDER BY date ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)`. Moyenne glissante sur 7 lignes : `ROWS BETWEEN 6 PRECEDING AND CURRENT ROW`. `NTILE(4)` pour des quartiles, `FIRST_VALUE`/`LAST_VALUE`. On ne peut pas filtrer sur une fonction de fenêtrage dans le `WHERE` (elle est calculée après) : passer par une CTE ou une sous-requête.

**Upsert selon les bases** : `INSERT ... ON CONFLICT DO UPDATE` (PostgreSQL), `INSERT ... ON DUPLICATE KEY UPDATE` (MySQL), `MERGE` (standard SQL, Oracle, SQL Server, PostgreSQL 15+).

**Dates** : `CURRENT_DATE`, `NOW()`, `DATE_TRUNC('month', d)` (PostgreSQL), `EXTRACT(YEAR FROM d)`, intervalles `d + INTERVAL '7 days'`. Toujours filtrer par intervalle demi-ouvert (`>= debut AND < fin`) plutôt que `BETWEEN` sur des timestamps.

**Injection SQL** : l'exemple classique `"SELECT * FROM users WHERE nom = '" + nom + "'"` avec `nom = ' OR '1'='1`. Défense : requêtes paramétrées, partout, y compris en JPQL et en SQL natif ; un nom de colonne ou un sens de tri dynamique ne peut pas être paramétré, il se valide contre une liste blanche.

### En situation

- **"Écris une requête qui..."** → jointure, agrégation, fenêtrage.
- **"Cette requête est lente"** → EXPLAIN, index, SELECT *, fonction sur colonne.
- **"Trouve les doublons"** → GROUP BY + HAVING COUNT(*) > 1.
- **"Top N par catégorie"** → fonction de fenêtrage.

### Questions techniques

**Q : Ordre d'exécution logique d'une requête ?** R : FROM → JOIN → WHERE → GROUP BY → HAVING → SELECT → ORDER BY → LIMIT.

**Q : WHERE vs HAVING ?** R : WHERE filtre les lignes avant groupement ; HAVING filtre les groupes après agrégation.

**Q : INNER vs LEFT JOIN ?** R : INNER garde les correspondances des deux côtés ; LEFT garde tout à gauche avec NULL à droite.

**Q : Comment trouver les lignes sans correspondance ?** R : `LEFT JOIN ... WHERE droite.id IS NULL`.

**Q : COUNT(*) vs COUNT(colonne) ?** R : COUNT(*) compte les lignes ; COUNT(colonne) ignore les NULL.

**Q : IN vs EXISTS ?** R : EXISTS s'arrête au premier résultat, souvent plus performant sur de gros volumes.

**Q : Différence GROUP BY / fonction de fenêtrage ?** R : GROUP BY réduit le nombre de lignes ; la fenêtre ajoute une valeur calculée sans réduire.

**Q : Comment faire un top 3 par catégorie ?** R : `ROW_NUMBER() OVER (PARTITION BY cat ORDER BY x DESC)` dans une CTE, puis filtrer sur le rang.

**Q : Piège du NULL ?** R : NULL n'égale rien, même pas NULL. Tester avec IS NULL ; attention à NOT IN avec des NULL.

**Q : Pourquoi éviter SELECT * ?** R : Colonnes inutiles transférées, index couvrants inutilisables, et fragilité au changement de schéma.

**Q : DDL vs DML ?** R : Définition de la structure (CREATE, ALTER, DROP) vs manipulation des données (SELECT, INSERT, UPDATE, DELETE).

**Q : DELETE vs TRUNCATE vs DROP ?** R : Supprime des lignes (filtrable, triggers) / vide la table rapidement / supprime la table.

**Q : UNION vs UNION ALL ?** R : UNION déduplique (coûteux), UNION ALL garde tout.

**Q : ROW_NUMBER vs RANK vs DENSE_RANK ?** R : 1,2,3,4 / 1,2,2,4 / 1,2,2,3 en cas d'égalité.

**Q : Comment trouver le deuxième salaire le plus élevé ?** R : `SELECT DISTINCT salaire FROM employes ORDER BY salaire DESC LIMIT 1 OFFSET 1`, ou `DENSE_RANK() = 2` dans une CTE, ou `MAX(salaire) WHERE salaire < (SELECT MAX(salaire) ...)`.

**Q : Comment trouver le N-ième salaire par département ?** R : DENSE_RANK() OVER (PARTITION BY departement ORDER BY salaire DESC) dans une CTE, filtré sur le rang N.

**Q : Comment parcourir une hiérarchie en SQL ?** R : Avec une CTE récursive (WITH RECURSIVE).

**Q : Pourquoi OFFSET est-il lent sur de grandes valeurs ?** R : La base lit et ignore toutes les lignes précédentes ; la pagination keyset l'évite.

**Q : Que renvoie SUM sur aucune ligne ?** R : NULL ; utiliser COALESCE(SUM(x), 0).

**Q : Peut-on filtrer sur ROW_NUMBER() dans le WHERE ?** R : Non, la fenêtre est calculée après ; passer par une CTE ou une sous-requête.

**Q : Comment faire un cumul progressif ?** R : SUM(x) OVER (ORDER BY date ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW).

**Q : Comment se protéger d'un tri dynamique injectable ?** R : Valider la colonne et le sens contre une liste blanche, car ils ne peuvent pas être paramétrés.

**Q : Comment faire un upsert en PostgreSQL ?** R : INSERT ... ON CONFLICT (cle) DO UPDATE SET ...

### Questions de situation

**Q : "Écris une requête donnant, pour chaque ville, le nombre de commandes des clients actifs, uniquement les villes avec plus de 10 commandes."**
R :
```sql
SELECT c.ville, COUNT(o.id) AS nb_commandes
FROM clients c
JOIN commandes o ON o.client_id = c.id
WHERE c.actif = true
GROUP BY c.ville
HAVING COUNT(o.id) > 10
ORDER BY nb_commandes DESC;
```
"Le point à expliquer : `WHERE` filtre les clients **avant** le groupement, `HAVING` filtre les villes **après** l'agrégation. Inverser les deux ne fonctionnerait pas, parce que l'agrégat n'existe pas encore au moment du `WHERE`."

**Q : "Comment tu trouves les doublons dans une table ?"**
R :
```sql
SELECT email, COUNT(*) 
FROM users 
GROUP BY email 
HAVING COUNT(*) > 1;
```
"Et pour récupérer les lignes complètes, ou n'en garder qu'une, j'utiliserais une fonction de fenêtrage :
```sql
WITH numerotes AS (
  SELECT id, email, ROW_NUMBER() OVER (PARTITION BY email ORDER BY id) AS rn
  FROM users
)
DELETE FROM users WHERE id IN (SELECT id FROM numerotes WHERE rn > 1);
```
Ça conserve la première occurrence de chaque email et supprime les suivantes."

**Q : "Cette requête est lente, quelles pistes sans changer le schéma ?"**
R : "D'abord `EXPLAIN ANALYZE` pour voir ce que fait réellement le moteur.
Les problèmes fréquents et corrigeables côté requête : un `SELECT *` qui charge des colonnes inutiles et empêche d'utiliser un index couvrant ; une **fonction appliquée sur une colonne indexée** dans le `WHERE`, qui rend l'index inutilisable, `WHERE YEAR(date) = 2024` doit devenir un intervalle `date >= '2024-01-01' AND date < '2025-01-01'` ; une sous-requête corrélée exécutée pour chaque ligne, qu'on peut souvent réécrire en jointure ; ou un `NOT IN` sur une sous-requête contenant des NULL, qu'on remplace par `NOT EXISTS`.
Je vérifierais aussi la présence d'un `LIMIT` : ramener un million de lignes pour n'en afficher vingt est un gaspillage complet.
Et si le plan montre des estimations très éloignées du réel, un `ANALYZE` pour rafraîchir les statistiques peut suffire."

**Q : "Pour chaque client, donne sa dernière commande."**
R :
```sql
WITH classees AS (
    SELECT o.*, ROW_NUMBER() OVER (PARTITION BY client_id ORDER BY date_creation DESC) AS rn
    FROM commandes o
)
SELECT * FROM classees WHERE rn = 1;
```
"Version portable avec fenêtrage. En PostgreSQL, `SELECT DISTINCT ON (client_id) * FROM commandes ORDER BY client_id, date_creation DESC` fait la même chose. Un index sur `(client_id, date_creation DESC)` rend la requête efficace."

**Q : "Calcule le chiffre d'affaires mensuel et son évolution par rapport au mois précédent."**
R :
```sql
WITH mensuel AS (
    SELECT DATE_TRUNC('month', date_creation) AS mois, SUM(montant) AS ca
    FROM commandes
    GROUP BY 1
)
SELECT mois, ca,
       ca - LAG(ca) OVER (ORDER BY mois) AS evolution,
       ROUND(100.0 * (ca - LAG(ca) OVER (ORDER BY mois)) / NULLIF(LAG(ca) OVER (ORDER BY mois), 0), 1) AS evolution_pct
FROM mensuel
ORDER BY mois;
```
"`LAG` récupère la valeur de la ligne précédente, et `NULLIF(..., 0)` évite une division par zéro. Le premier mois a une évolution NULL, ce qui est correct."

**Q : "Trouve les clients qui n'ont jamais commandé."**
R : "Trois écritures : `LEFT JOIN commandes o ON o.client_id = c.id WHERE o.id IS NULL`, `WHERE NOT EXISTS (SELECT 1 FROM commandes o WHERE o.client_id = c.id)`, ou `NOT IN (SELECT client_id FROM commandes)`. J'éviterais la troisième : si une ligne de la sous-requête a un `client_id` NULL, `NOT IN` ne renvoie plus rien. `NOT EXISTS` est la plus sûre et en général aussi performante que la jointure."

═══════════════════════════════════════════
# 28. TESTS
═══════════════════════════════════════════

### Vue d'ensemble

Les tests ne servent pas d'abord à "prouver que ça marche", ils servent à **pouvoir changer le code sans peur**. Une base de code sans tests devient rapidement figée : personne n'ose y toucher, donc la dette s'accumule. C'est pour ça que la testabilité est un critère de conception à part entière, et pourquoi l'injection de dépendances compte autant.

La **pyramide des tests** exprime un compromis économique : les tests unitaires sont rapides, précis et peu coûteux, donc on en met beaucoup ; les tests bout en bout sont lents, instables et chers à maintenir, donc on en garde peu, juste assez pour couvrir les parcours critiques. L'anti-pattern inverse, le "cornet de glace" (beaucoup d'E2E, peu d'unitaires), donne des suites lentes que l'équipe finit par ignorer.

### Cours

**Niveaux :**
- **Unitaire** : une classe isolée, dépendances mockées. Millisecondes. Localise précisément la panne.
- **Intégration** : plusieurs composants réels ensemble (couche web, base). Plus lent, plus réaliste.
- **Bout en bout** : le système complet du point de vue utilisateur. Lent et fragile.
- **Tests de contrat** : vérifient que deux services respectent l'interface convenue (Pact), utile en microservices pour éviter des E2E lourds.

**JUnit 5 / 6** : `@Test`, `@BeforeEach`/`@AfterEach`, `@DisplayName`, `@ParameterizedTest` (même test sur plusieurs jeux de données), `@Nested` (regrouper), `assertThrows`, `assertAll`. AssertJ pour des assertions fluides et lisibles (`assertThat(x).isEqualTo(y)`).

**Mockito** : `@Mock`, `@InjectMocks`, `when(...).thenReturn(...)`, `verify(...)`, `ArgumentCaptor` pour inspecter les arguments reçus.
- **Stub** : renvoie des valeurs prédéfinies.
- **Mock** : stub + vérification des interactions.
- **Spy** : objet réel dont on remplace certaines méthodes.
- **Fake** : implémentation simplifiée réelle (repository en mémoire).

**Structure AAA / Given-When-Then** : préparer, exécuter, vérifier. Un test doit avoir une seule raison d'échouer, et son nom doit décrire le comportement attendu (`shouldRejectOrderWhenStockInsufficient`).

**Spring** : `@SpringBootTest` (contexte complet), `@WebMvcTest` (couche web seule, plus rapide), `@DataJpaTest` (couche persistance), `MockMvc` pour simuler des requêtes, `@MockitoBean` pour remplacer un bean du contexte (il remplace `@MockBean`, déprécié en Boot 3.4 et supprimé en Boot 4).

**Quarkus** : `@QuarkusTest`, REST-Assured (`given().when().get().then().statusCode(200)`), `@InjectMock`, et les Dev Services qui démarrent automatiquement les dépendances.

**Testcontainers** : lance de vraies dépendances (PostgreSQL, Kafka) dans Docker le temps des tests. Nettement préférable à une base en mémoire : H2 ne se comporte pas comme PostgreSQL, et les différences se découvrent en production.

**Couverture** : mesurée par JaCoCo. Indicateur utile mais trompeur, 100% de couverture ne prouve rien sur la pertinence des assertions. Mieux vaut une couverture ciblée sur la logique métier et les cas limites.

**TDD** : Red (test qui échoue) → Green (code minimal) → Refactor. Au-delà de la vérification, ça force à concevoir depuis l'usage, ce qui produit des API plus simples.

**Tests de caractérisation** : sur du legacy sans tests, on capture d'abord le comportement **actuel** (sans juger s'il est correct) pour détecter tout changement lors du refactoring.

**JUnit 6** (septembre 2025) : Java 17 minimum, numéros de version unifiés entre Platform, Jupiter et Vintage, annotations JSpecify, API d'annulation, et suppression d'anciens modules ; l'API Jupiter (`@Test`, `@BeforeEach`...) est la même que JUnit 5, donc la migration est généralement une montée de version. JUnit 4 (`@RunWith`, `@Before`) reste exécutable via le moteur Vintage, désormais déprécié.

**JUnit, annotations complémentaires** : `@BeforeAll`/`@AfterAll` (static, une fois par classe, sauf avec `@TestInstance(PER_CLASS)`), `@Disabled`, `@Tag` (filtrer des catégories en CI), `@Timeout`, `@RepeatedTest`, `@TempDir` (répertoire temporaire injecté), `@TestMethodOrder`, `@ExtendWith` (extensions, par exemple `MockitoExtension`), sources de paramètres : `@ValueSource`, `@CsvSource`, `@MethodSource`, `@EnumSource`, `@NullAndEmptySource`. `assertThrows` renvoie l'exception pour vérifier son message ; `assertTimeout`. Une instance de la classe de test est créée **par méthode** par défaut : les champs ne sont pas partagés entre tests.

**Mockito, compléments** : `when(...).thenThrow(...)`, `thenAnswer` (réponse calculée), `doReturn(...).when(spy)` (pour les spies et les méthodes void), `doThrow` pour une méthode void, `verify(mock, times(2))`, `never()`, `verifyNoMoreInteractions`, matchers `any()`, `eq()`, `argThat(...)` (si un argument utilise un matcher, tous doivent en utiliser), `@Captor`. Mode **strict** par défaut avec `MockitoExtension` : un stub inutilisé fait échouer le test (`UnnecessaryStubbingException`), ce qui garde les tests propres. Mockito peut mocker les classes finales et les méthodes statiques (`mockStatic`, dans un try-with-resources) avec le *mock maker inline*, devenu le défaut depuis Mockito 5 ; mais le besoin de mocker du statique est souvent un signal de conception. Ne pas mocker ce qu'on ne possède pas : on enveloppe une bibliothèque tierce dans un adaptateur, et on mocke l'adaptateur.

**Spring, compléments** : `@MockitoBean` et `@MockitoSpyBean` (Spring Framework 6.2) remplacent `@MockBean`/`@SpyBean` de Spring Boot (dépréciés en 3.4, retirés en Boot 4). `@SpringBootTest(webEnvironment = RANDOM_PORT)` avec `TestRestTemplate` ou `WebTestClient`/`RestTestClient` pour un vrai serveur. `@DataJpaTest` annule la transaction après chaque test (attention : du coup le flush n'a parfois pas lieu, et certaines contraintes ne sont pas vérifiées, d'où des `flush()` explicites). `@Sql` pour charger des données. Le **cache de contextes** de test : chaque combinaison différente de configuration (`@MockitoBean`, profils, propriétés) crée un nouveau contexte ; trop de variations ralentissent fortement la suite.

**Doubles de tests HTTP** : **WireMock** (serveur HTTP simulé, avec des scénarios d'erreur, de latence et de réponses par requête), MockWebServer (OkHttp), `MockRestServiceServer` (Spring).

**Tests asynchrones** : jamais de `Thread.sleep` (lent et instable) ; **Awaitility** : `await().atMost(5, SECONDS).until(() -> repo.count() == 1)`.

**Tester le temps** : injecter un `Clock` ; tester l'aléatoire : injecter le générateur ou une graine fixe.

**Tests d'architecture** : **ArchUnit** (`noClasses().that().resideInAPackage("..domaine..").should().dependOnClassesThat().resideInAPackage("..infrastructure..")`).

**Tests de mutation** : **PIT** (pitest) modifie le code (inverse une condition, remplace un retour) et vérifie qu'au moins un test échoue. Un mutant qui "survit" révèle une assertion absente. Bien meilleur indicateur de la qualité des tests que la couverture.

**Tests par propriétés** : jqwik ; on décrit une propriété (trier une liste deux fois donne le même résultat que la trier une fois) et l'outil génère des centaines d'entrées, y compris les cas limites.

**BDD** : Cucumber, scénarios en Gherkin (*Given/When/Then*) lisibles par le métier, reliés à du code (*step definitions*). Utile si le métier lit et co-écrit les scénarios ; sinon c'est une couche de coût en plus.

**Tests de contrat, détail** : *consumer-driven* (Pact : le consommateur écrit ses attentes, le fournisseur les vérifie dans sa CI) ou *provider-driven* (Spring Cloud Contract : le fournisseur publie des contrats d'où sont générés tests et stubs).

**Tests de performance** : outils **Gatling** (scénarios en code, rapports détaillés), **JMeter**, **k6** ; types : test de **charge** (le volume attendu), de **stress** (au-delà, trouver le point de rupture), d'**endurance** (*soak*, plusieurs heures pour révéler fuites mémoire et dérives), de **pic** (*spike*). Mesurer percentiles et taux d'erreur, avec des données et un environnement représentatifs.

**Tests de sécurité** : SAST (analyse du code), DAST (attaque de l'application en fonctionnement, OWASP ZAP), SCA (dépendances vulnérables), fuzzing.

**Tests instables (flaky)** : causes habituelles : dépendance à l'ordre d'exécution ou à un état partagé, temps et timeouts, concurrence, dépendance au réseau, données aléatoires. On les isole et on les corrige, on ne les relance pas en boucle jusqu'au vert.

**Nommage et structure** : un test documente un comportement : `doitRefuserLaCommandeQuandLeStockEstInsuffisant` ou `@DisplayName`. Un test = un comportement ; des données de test explicites (*test data builders*, *Object Mother*) ; pas de logique (boucles, conditions) dans les tests.

**Le modèle "trophée"** (Kent C. Dodds) : une alternative à la pyramide qui met l'accent sur les tests d'intégration, jugés plus rentables pour des applications où l'essentiel de la valeur est dans l'assemblage. Avec Testcontainers, l'intégration est devenue rapide et fiable, ce qui rend ce débat concret.

### En situation

- **"Comment tu testes ce code ?"** → stratégie par niveau.
- **"Ce code est intestable"** → dépendances en dur → injection.
- **"Refactorer sans casser"** → tests de caractérisation d'abord.
- **"Les tests sont trop lents"** → rééquilibrer la pyramide, séparer les cycles.
- **"Valider une migration"** → non-régression, rejeu de trafic.

### Questions techniques

**Q : Pourquoi la pyramide ?** R : Compromis économique, beaucoup de tests rapides et précis, peu de tests lents et fragiles.

**Q : Stub vs mock vs spy vs fake ?** R : Valeurs prédéfinies / + vérification / objet réel partiellement remplacé / implémentation simplifiée réelle.

**Q : @SpringBootTest vs @WebMvcTest ?** R : Contexte complet vs couche web seule, plus rapide.

**Q : Pourquoi Testcontainers plutôt qu'H2 ?** R : Parce qu'H2 ne se comporte pas comme la base de production, et les écarts se découvrent en prod.

**Q : C'est quoi un test de contrat ?** R : Une vérification que deux services respectent l'interface convenue, sans monter un E2E complet.

**Q : Faut-il 100% de couverture ?** R : Non, la couverture ne mesure pas la qualité des assertions. Viser le métier critique.

**Q : C'est quoi un test de caractérisation ?** R : Un test capturant le comportement actuel d'un code legacy, pour sécuriser un refactoring.

**Q : JUnit 5 vs JUnit 6 ?** R : Même API Jupiter ; JUnit 6 impose Java 17, unifie les versions et retire d'anciens modules.

**Q : @BeforeEach vs @BeforeAll ?** R : Avant chaque test vs une fois pour la classe (méthode static par défaut).

**Q : Comment écrire un test paramétré ?** R : @ParameterizedTest avec @ValueSource, @CsvSource, @MethodSource ou @EnumSource.

**Q : JUnit crée-t-il une instance de la classe de test par méthode ?** R : Oui par défaut, sauf @TestInstance(PER_CLASS).

**Q : doReturn vs when().thenReturn() ?** R : doReturn n'appelle pas la vraie méthode (indispensable avec un spy ou une méthode void) ; when appelle la méthode lors du stubbing.

**Q : C'est quoi UnnecessaryStubbingException ?** R : L'erreur du mode strict de Mockito pour un stub jamais utilisé.

**Q : Peut-on mocker une méthode statique ?** R : Oui avec mockStatic (mock maker inline), mais c'est souvent un signal de conception.

**Q : Que remplace @MockitoBean ?** R : @MockBean de Spring Boot, déprécié en 3.4 et supprimé en Boot 4.

**Q : Pourquoi trop de @MockitoBean différents ralentissent-ils les tests ?** R : Chaque configuration différente crée un nouveau contexte Spring au lieu de réutiliser le contexte en cache.

**Q : Piège de @DataJpaTest ?** R : La transaction est annulée et le flush peut ne pas avoir lieu, donc certaines contraintes ne sont jamais vérifiées.

**Q : Comment tester un appel HTTP sortant ?** R : WireMock (ou MockRestServiceServer), en simulant aussi les erreurs et la latence.

**Q : Comment tester du code asynchrone sans sleep ?** R : Avec Awaitility.

**Q : C'est quoi un test de mutation ?** R : Introduire des modifications dans le code et vérifier qu'un test échoue ; un mutant survivant révèle un test insuffisant (PIT).

**Q : Pact vs Spring Cloud Contract ?** R : Contrats écrits par le consommateur vs contrats publiés par le fournisseur.

**Q : Test de charge vs stress vs endurance ?** R : Volume attendu / au-delà jusqu'à la rupture / longue durée pour révéler les dérives.

**Q : SAST vs DAST vs SCA ?** R : Analyse du code source / attaque de l'application en fonctionnement / analyse des dépendances.

**Q : Causes d'un test flaky ?** R : Ordre et état partagé, temps, concurrence, réseau, aléatoire.

**Q : Pourquoi ne pas mocker une bibliothèque tierce directement ?** R : On fige des hypothèses sur un code qu'on ne maîtrise pas ; on l'enveloppe dans un adaptateur qu'on mocke.

### Questions de situation

**Q : "Comment tu testes une application backend concrètement ?"**
R : "Par niveau, selon la pyramide.
La **base** : des tests unitaires sur la logique métier, sans base ni réseau, dépendances mockées. C'est là que je mets le plus d'effort, parce qu'ils tournent à chaque compilation et donnent un retour immédiat. Ils exigent un code découplé, d'où l'importance de l'injection par constructeur.
Le **milieu** : les endpoints avec `@QuarkusTest` et REST-Assured, ou `@SpringBootTest` et MockMvc ; et la persistance avec **Testcontainers** sur une vraie base, parce qu'une base en mémoire masque les différences de comportement.
Le **sommet** : très peu de tests bout en bout, réservés aux parcours critiques.
Je regarderais aussi ce que les tests **vérifient réellement** : une couverture élevée sur des getters ne prouve rien. Je préfère cibler les règles métier et les cas limites, valeurs nulles, listes vides, bornes, erreurs."

**Q : "Refactorer une classe critique sans tests."**
R : "Je ne toucherais pas au code avant d'avoir un filet.
Première étape : des **tests de caractérisation** qui capturent le comportement **actuel**, y compris ses bizarreries. Le but n'est pas de juger si c'est correct, mais de détecter tout changement.
Si la classe est trop couplée pour être testée, j'applique d'abord des refactorings **sûrs** pour l'ouvrir : extraire une dépendance et l'injecter. Ce sont des transformations mécaniques à faible risque.
Ensuite je refactore **par petits pas**, tests relancés à chaque étape. Jamais une réécriture d'un bloc, si ça casse, on ne sait pas quoi.
Sur une migration importante, j'ajouterais du **rejeu de trafic** : rejouer des requêtes réelles de production sur l'ancien et le nouveau système et comparer les résultats. C'est souvent le seul moyen de couvrir des cas qu'on n'aurait jamais imaginés en écrivant des tests."

**Q : "Les tests prennent 40 minutes, plus personne ne les lance."**
R : "Le problème n'est pas la durée en soi, c'est le **déséquilibre de la pyramide** : trop d'intégration et d'E2E, pas assez d'unitaires.
Je commencerais par mesurer où le temps est passé, souvent une poignée de tests concentre l'essentiel. Ceux-là montent un contexte complet pour tester une logique qui pourrait l'être unitairement, ou attendent des timeouts.
Les leviers : **convertir** en tests unitaires ceux qui n'ont pas besoin d'infrastructure ; **mutualiser** le contexte de test au lieu de le redémarrer par classe ; **paralléliser** l'exécution.
Et surtout **séparer les cycles** : les unitaires tournent en local à chaque commit et doivent rester sous la minute ; l'intégration dans la CI ; les E2E la nuit ou sur la branche d'intégration.
Le vrai enjeu est là : des tests qu'on ne lance pas ne protègent de rien."

**Q : "Comment tu testes un service qui appelle une API externe de paiement ?"**
R : "Par niveaux. En **unitaire**, le service dépend d'une interface `PasserellePaiement` que je mocke : je teste la logique (montants, règles, gestion des refus) sans réseau.
En **intégration**, je teste l'adaptateur HTTP réel contre **WireMock** : réponses nominales, mais surtout les cas difficiles, comme un 500, un timeout, une réponse mal formée, un 429 avec `Retry-After`, pour vérifier timeouts, retries et circuit breaker.
Si le fournisseur propose un **bac à sable**, quelques tests de bout en bout contre lui, dans un pipeline séparé parce qu'ils dépendent d'un tiers. Et l'idempotence se teste explicitement : rejouer la même requête ne doit pas débiter deux fois."

**Q : "Ton équipe vise 80 % de couverture, mais des bugs passent quand même."**
R : "Parce que la couverture mesure les lignes **exécutées**, pas les comportements **vérifiés** : un test sans assertion couvre autant qu'un bon test. Je lancerais un **test de mutation** avec PIT sur les modules critiques : il montrera concrètement les endroits où on peut casser le code sans qu'aucun test ne le remarque.
Ensuite je regarderais la nature des bugs échappés : sont-ils dans des cas limites (nulls, bornes, listes vides), dans l'intégration (requêtes SQL, sérialisation) ou dans la concurrence ? Chaque catégorie appelle un type de test différent. Et chaque bug corrigé reçoit un test de non-régression qui le reproduit d'abord."

═══════════════════════════════════════════
# 29. GIT
═══════════════════════════════════════════

### Vue d'ensemble

Git est un système de contrôle de version **distribué** : chaque développeur possède l'historique complet en local, ce qui permet de travailler et de committer sans réseau. Techniquement, Git n'enregistre pas des différences mais des **instantanés** du projet ; chaque commit pointe vers son ou ses parents, formant un graphe orienté acyclique. Une branche n'est qu'un **pointeur mobile** sur un commit, c'est pourquoi créer une branche est instantané.

Comprendre ce modèle éclaire tout le reste : `merge` crée un commit ayant deux parents, `rebase` recrée de nouveaux commits ailleurs dans le graphe (d'où le danger sur une branche partagée), et `reset` déplace simplement un pointeur (d'où la possibilité de récupérer via `reflog`).

### Cours

**Les trois zones** : working directory (fichiers modifiés) → staging area / index (`git add`) → repository (`git commit`).

**fetch vs pull** : `fetch` récupère sans intégrer ; `pull` = `fetch` + `merge` (ou `rebase` avec `--rebase`).

**merge vs rebase** : `merge` crée un commit de fusion et conserve l'historique réel avec ses embranchements. `rebase` rejoue tes commits par-dessus la cible, produisant un historique linéaire, mais il crée de **nouveaux** commits. D'où la règle d'or : **jamais rebaser une branche déjà partagée**, sous peine de casser le dépôt des autres.

**reset** : `--soft` (annule le commit, garde les changements indexés), `--mixed` (garde les changements non indexés), `--hard` (perd tout). **revert** crée un commit qui annule un commit antérieur, c'est la seule option sûre sur une branche partagée.

**Outils** : `cherry-pick` (appliquer un commit précis ailleurs), `stash` (mettre de côté), `rebase -i` (réorganiser, squash, reword), `reflog` (historique de HEAD, permet de récupérer après un `reset --hard`), `bisect` (recherche dichotomique du commit fautif), `blame` (qui a modifié chaque ligne), `tag` (marquer une version).

**Stratégies de branching** :
- **Git Flow** : main, develop, feature, release, hotfix. Adapté aux releases planifiées et versionnées ; lourd pour du déploiement continu.
- **GitHub Flow** : main + branches de fonctionnalité, PR, merge. Simple, adapté au déploiement continu.
- **Trunk-Based** : tout le monde intègre sur le tronc très fréquemment, les fonctionnalités incomplètes sont masquées par des **feature flags**. Exige une bonne CI, mais c'est le modèle des équipes à haute fréquence de déploiement.

**Bonnes pratiques** : commits **atomiques** (un commit = un changement cohérent), messages explicites (idéalement conventionnels : `feat:`, `fix:`), branches courtes, revue systématique.

**Conflits** : ils surviennent quand deux branches modifient les mêmes lignes. On les résout en comprenant les deux intentions, pas en choisissant au hasard.

**Objets Git** : *blob* (contenu d'un fichier), *tree* (répertoire), *commit* (un tree + parents + auteur + message), *tag* annoté. Chaque objet est identifié par un hash de son contenu (SHA-1, migration vers SHA-256 en cours). **HEAD** pointe vers la branche courante ; en **HEAD détaché**, il pointe directement vers un commit, et les commits faits dans cet état peuvent être perdus si on ne crée pas de branche.

**Types de merge** : *fast-forward* (la branche cible n'a pas avancé, Git déplace simplement le pointeur, pas de commit de fusion), `--no-ff` (force un commit de fusion pour garder la trace de la branche), *squash merge* (tous les commits de la branche deviennent un seul, historique propre mais détail perdu), *three-way merge* (commit de fusion à partir de l'ancêtre commun).

**Commandes modernes et utiles** : `git switch` / `git restore` (Git 2.23, séparent les deux rôles de `checkout`), `git commit --amend` (modifier le dernier commit, à éviter s'il est poussé), `git rebase -i HEAD~5` (squash, fixup, reword, réordonner), `git commit --fixup <sha>` + `git rebase -i --autosquash`, `git rebase --onto`, `git stash push -m "..."` / `stash pop` / `stash list`, `git worktree add` (plusieurs branches extraites en même temps), `git log --oneline --graph --all`, `git diff --staged`, `git blame -w`, `git reflog`, `git cherry-pick -x`, `git clean -fd` (fichiers non suivis), `git push --force-with-lease` (force push qui échoue si quelqu'un a poussé entre-temps, à préférer à `--force`), `git tag -a v1.2.0 -m "..."`.

**.gitignore** : n'affecte que les fichiers non encore suivis ; pour un fichier déjà commité, `git rm --cached fichier` puis l'ajouter au .gitignore.

**Hooks** : scripts exécutés à certains moments (`pre-commit` pour formater ou détecter des secrets, `commit-msg` pour vérifier le format du message, `pre-push`). Côté client, ils sont contournables (`--no-verify`) : les règles impératives se vérifient aussi en CI.

**Signature des commits** (GPG ou SSH) : prouve l'auteur ; souvent exigée sur les branches protégées.

**Protection de branche** : interdire le push direct sur main, exiger une PR, des relecteurs, une CI verte, un historique linéaire.

**Submodules vs monorepo vs polyrepo** : les submodules (un dépôt dans un dépôt, figé à un commit) sont réputés pénibles ; un **monorepo** facilite les changements transverses et le partage de code mais demande des outils de build incrémental ; le **polyrepo** isole les équipes au prix de la coordination des versions.

**Fichiers volumineux** : Git LFS.

### En situation

- **"Décris ton workflow"** → feature → PR → review → merge.
- **"Un secret a été poussé"** → révoquer d'abord, nettoyer ensuite.
- **"Trouver le commit fautif"** → bisect.
- **"Annuler un commit déjà poussé"** → revert, jamais reset.

### Questions techniques

**Q : Git stocke des diffs ou des snapshots ?** R : Des instantanés ; les diffs sont calculés à l'affichage.

**Q : C'est quoi une branche techniquement ?** R : Un pointeur mobile sur un commit, d'où sa création instantanée.

**Q : merge vs rebase ?** R : Commit de fusion préservant l'historique vs réécriture linéaire créant de nouveaux commits.

**Q : Pourquoi ne pas rebaser une branche partagée ?** R : Elle réécrit l'historique et casse les copies locales des autres.

**Q : reset --soft / --mixed / --hard ?** R : Garde indexé / garde non indexé / perd les changements.

**Q : reset vs revert ?** R : reset réécrit l'historique ; revert ajoute un commit d'annulation, sûr sur du partagé.

**Q : À quoi sert reflog ?** R : Récupérer des commits "perdus" après un reset ou un rebase raté.

**Q : Trunk-based, comment gérer une fonctionnalité incomplète ?** R : Avec des feature flags, qui découplent le déploiement de l'activation.

**Q : C'est quoi HEAD ?** R : Le pointeur vers la branche (ou le commit) courant.

**Q : C'est quoi un HEAD détaché ?** R : HEAD pointe directement sur un commit et non sur une branche ; les nouveaux commits risquent d'être perdus.

**Q : Fast-forward vs merge commit ?** R : Simple déplacement du pointeur quand la cible n'a pas divergé vs commit à deux parents.

**Q : Squash merge, avantages et inconvénients ?** R : Historique propre, un commit par fonctionnalité ; perte du détail des commits intermédiaires.

**Q : --force vs --force-with-lease ?** R : Écrase sans condition vs refuse si la branche distante a évolué depuis votre dernière récupération.

**Q : Comment arrêter de suivre un fichier déjà commité ?** R : git rm --cached puis l'ajouter au .gitignore.

**Q : switch/restore vs checkout ?** R : Les deux nouvelles commandes séparent changer de branche et restaurer des fichiers, que checkout mélangeait.

**Q : À quoi sert git worktree ?** R : Avoir plusieurs branches extraites simultanément dans des répertoires différents.

**Q : Pourquoi les hooks ne suffisent-ils pas ?** R : Ils sont locaux et contournables ; les règles doivent aussi être vérifiées en CI.

**Q : git stash pop vs apply ?** R : pop applique puis supprime l'entrée ; apply la conserve.

**Q : Monorepo, avantages ?** R : Changements atomiques transverses, partage de code, outillage unifié.

### Questions de situation

**Q : "Un commit parmi 200 a introduit un bug, on ne sait pas lequel."**
R : "**`git bisect`**, qui fait une recherche dichotomique. Je marque un commit bon et un mauvais, Git me place au milieu, je teste, je réponds, et il itère. Sur 200 commits ça converge en 8 étapes environ.
Si le bug est détectable par un script, `git bisect run` automatise entièrement la recherche.
Ça illustre aussi pourquoi les **commits atomiques** comptent : si chaque commit fait une seule chose, bisect pointe directement le changement fautif. Avec des commits fourre-tout de 2 000 lignes, on sait juste dans quel paquet chercher."

**Q : "Un mot de passe a été poussé il y a trois commits."**
R : "La première action, et c'est le réflexe essentiel : **révoquer le secret immédiatement**. Dès qu'il a été poussé, il doit être considéré comme compromis, il est dans l'historique local de tous ceux qui ont pull, potentiellement dans des caches de CI, des sauvegardes, des logs. Nettoyer le dépôt ne le décompromet pas.
Ensuite seulement, nettoyer l'historique avec `git filter-repo` ou BFG, ce qui implique un force-push et une coordination avec toute l'équipe puisqu'on réécrit l'historique.
En prévention : secrets dans un vault, `.gitignore` couvrant les fichiers de configuration locale, et un scan automatique dans la CI ou un hook de pré-commit qui détecte les patterns de secrets avant qu'ils ne partent."

**Q : "Tu as commité sur main au lieu d'une branche, sans avoir poussé."**
R : "Je crée une branche à cet endroit pour garder le travail (`git branch ma-feature`), puis je ramène main sur la version distante (`git reset --hard origin/main`), et je continue sur `ma-feature`. Rien n'est perdu, et si j'avais un doute, `git reflog` permettrait de retrouver n'importe quel état précédent."

**Q : "Ta branche a 30 commits 'wip' et 'fix typo' avant la revue."**
R : "Un `git rebase -i` sur la base de la branche pour regrouper en quelques commits cohérents (squash, fixup) avec des messages explicites, puis un `push --force-with-lease` puisque la branche n'est utilisée que par moi. Si l'équipe fait du squash merge, ce nettoyage est moins important, mais il facilite toujours la relecture commit par commit."

═══════════════════════════════════════════
# 30. MAVEN, GRADLE & BUILD
═══════════════════════════════════════════

### Vue d'ensemble

Maven résout un problème qui paraît trivial mais qui ne l'est pas : **construire un projet de façon reproductible**, avec les bonnes versions de dizaines de librairies qui dépendent elles-mêmes d'autres librairies. Sa philosophie est la **convention plutôt que la configuration** : une structure de répertoires standard, un cycle de vie standard, et un fichier descriptif (`pom.xml`) plutôt qu'un script de build.

Le point qui pose problème en pratique (et qui tombe en entretien) ce sont les **dépendances transitives** : ta dépendance A tire B, qui tire C en version 1.2, alors que ta dépendance D tire C en version 2.0. Savoir diagnostiquer et arbitrer ces conflits fait partie du métier.

### Cours

**Structure standard** : `src/main/java`, `src/main/resources`, `src/test/java`, `target/` pour la sortie.

**Coordonnées** d'un artefact : `groupId:artifactId:version` (+ packaging, classifier). La version `SNAPSHOT` désigne une version en développement, re-téléchargée régulièrement.

**Cycle de vie** (phases principales, exécutées en séquence) :
`validate` → `compile` → `test` → `package` → `verify` → `install` (dépôt local) → `deploy` (dépôt distant). Lancer `mvn package` exécute toutes les phases précédentes.

**Scopes de dépendances :**
| Scope | Compilation | Tests | Runtime | Packagé |
|---|---|---|---|---|
| `compile` (défaut) | ✓ | ✓ | ✓ | ✓ |
| `provided` | ✓ | ✓ | ✗ | ✗ |
| `runtime` | ✗ | ✓ | ✓ | ✓ |
| `test` | ✗ | ✓ | ✗ | ✗ |
| `import` | (|) | (|) (BOM only) |

`provided` sert quand le conteneur fournit la dépendance (API servlet). `runtime` pour un driver JDBC, nécessaire à l'exécution mais pas à la compilation.

**Dépendances transitives** : Maven résout automatiquement les dépendances de vos dépendances. En cas de conflit de version, il applique la règle du **"nearest wins"** : la version la plus proche dans l'arbre l'emporte, et à profondeur égale, la première déclarée.

**Diagnostic** : `mvn dependency:tree` affiche l'arbre complet et signale les versions écartées. C'est l'outil de référence pour un `NoSuchMethodError` en production, symptôme typique d'une version de librairie différente à l'exécution de celle attendue à la compilation.

**Maîtriser les versions :**
- `<dependencyManagement>` : fixe les versions sans déclarer les dépendances ; les modules enfants héritent.
- **BOM** (Bill of Materials) : un pom importé qui fixe un ensemble cohérent de versions, c'est ce que fait `spring-boot-dependencies`.
- `<exclusions>` : retirer une dépendance transitive indésirable.

**Projet multi-modules** : un pom parent avec `<modules>`, chaque module ayant son pom. Permet de partager configuration et versions, et de construire tout l'ensemble en une commande.

**Profils** : `<profiles>` activés par `-P`, par propriété ou par environnement, pour varier la configuration du build.

**Plugins courants** : `compiler` (version Java), `surefire` (tests unitaires), `failsafe` (tests d'intégration), `jacoco` (couverture), `spring-boot`/`quarkus` (packaging exécutable), `enforcer` (imposer des règles, par exemple interdire les versions en conflit).

**Reproductibilité** : `mvn -o` (hors ligne), verrouillage des versions (pas de `LATEST` ni de plages), et cache du dépôt local `~/.m2` dans la CI pour accélérer les builds.

**Gradle** : alternative avec un DSL (Groovy/Kotlin), builds incrémentaux et cache, généralement plus rapide mais moins standardisé.

**Phases, goals, plugins** : un **plugin** fournit des **goals** (`compiler:compile`, `surefire:test`) ; chaque phase du cycle de vie est liée à des goals selon le packaging. On peut lancer une phase (`mvn verify`) ou un goal directement (`mvn dependency:tree`). Il existe trois cycles de vie : `clean`, `default` (de validate à deploy) et `site`. `mvn clean install` : nettoyer puis tout construire jusqu'au dépôt local.

**Options utiles** : `-DskipTests` (compile les tests sans les exécuter) vs `-Dmaven.test.skip=true` (ne les compile même pas), `-pl module -am` (construire un module et ses dépendances dans un multi-modules), `-T 1C` (un thread par cœur), `-o` (hors ligne), `-U` (forcer la mise à jour des SNAPSHOT), `-X` (debug), `-q`, `-P profil`, `-Dprop=valeur`. Commandes de diagnostic : `mvn dependency:tree -Dverbose`, `dependency:analyze` (dépendances déclarées inutilisées ou utilisées non déclarées), `help:effective-pom` (le pom final après héritage et profils), `versions:display-dependency-updates`.

**Maven Wrapper** (`mvnw`) : un script versionné qui télécharge la bonne version de Maven, pour des builds identiques sur tous les postes et en CI.

**Dépôts** : le dépôt local `~/.m2/repository`, Maven Central, et un **gestionnaire de dépôts** d'entreprise (Nexus, Artifactory) qui sert de proxy/cache et héberge les artefacts internes. `settings.xml` (`~/.m2/settings.xml`) contient les miroirs, les identifiants et les profils propres à la machine ; jamais de secrets dans le pom.

**Release** : passer de `1.3.0-SNAPSHOT` à `1.3.0`, tagger, déployer, puis passer à `1.4.0-SNAPSHOT` (maven-release-plugin, ou de plus en plus une version calculée par la CI à partir des tags et des conventional commits). Un artefact de release est **immuable** : on ne republie jamais la même version.

**Parent vs BOM** : un pom **parent** transmet configuration, plugins et versions par héritage (un seul parent possible, `spring-boot-starter-parent`) ; un **BOM** importé en `<scope>import</scope>` dans le `dependencyManagement` ne transmet que des versions, et on peut en importer plusieurs.

**Maven 4** introduit notamment la séparation entre le pom de build et le pom "consommateur" publié, et un meilleur support des multi-modules ; Maven 3.9 reste très répandu.

**Gradle en détail** : un build est un graphe de **tâches** (`./gradlew build`, `test`, `bootJar`), écrit en Kotlin DSL (`build.gradle.kts`) ou Groovy. Configurations de dépendances : `implementation` (dépendance interne au module, non exposée à ses consommateurs, ce qui limite les recompilations), `api` (exposée, plugin `java-library`), `compileOnly` (équivalent de provided), `runtimeOnly`, `testImplementation`. Performance : builds **incrémentaux**, **cache de build** local et distant (réutiliser les sorties d'une tâche déjà exécutée ailleurs), démon Gradle, *configuration cache*. Catalogues de versions (`libs.versions.toml`) pour centraliser les versions. Plateformes (`platform(...)`) pour importer un BOM. Gradle résout les conflits en prenant par défaut la **version la plus haute**, contrairement au "nearest wins" de Maven.

| | Maven | Gradle |
|---|---|---|
| Description | XML déclaratif | DSL Kotlin/Groovy programmable |
| Conventions | fortes, très standardisé | flexibles |
| Performance | correcte | incrémental et cache de build, souvent plus rapide |
| Conflits de versions | la plus proche gagne | la plus haute gagne |
| Courbe d'apprentissage | faible | plus forte |

**Builds reproductibles** : même source → même binaire octet par octet (`project.build.outputTimestamp` dans Maven), utile pour la vérification de la chaîne d'approvisionnement.

### En situation

- **"NoSuchMethodError en production"** → conflit de versions transitives.
- **"Le build est lent en CI"** → cache du dépôt local, parallélisation, découpage.
- **"Comment tu gères les versions sur un multi-modules ?"** → parent + dependencyManagement + BOM.
- **"Comment séparer tests unitaires et d'intégration ?"** → surefire vs failsafe.

### Questions techniques

**Q : Phases principales du cycle Maven ?** R : validate, compile, test, package, verify, install, deploy.

**Q : Différence entre les scopes compile, provided, runtime, test ?** R : Partout / fourni par le conteneur à l'exécution / nécessaire seulement à l'exécution / seulement pour les tests.

**Q : Comment Maven résout un conflit de versions ?** R : "Nearest wins", la plus proche dans l'arbre, puis la première déclarée.

**Q : Comment diagnostiquer un conflit ?** R : `mvn dependency:tree`.

**Q : C'est quoi un BOM ?** R : Un pom importé fixant un ensemble cohérent de versions (ex : spring-boot-dependencies).

**Q : dependencyManagement vs dependencies ?** R : Le premier fixe les versions sans ajouter la dépendance ; le second l'ajoute réellement.

**Q : surefire vs failsafe ?** R : Tests unitaires en phase test vs tests d'intégration en phase verify.

**Q : Que signifie SNAPSHOT ?** R : Une version en développement, re-téléchargée régulièrement, à proscrire en production.

**Q : Phase vs goal ?** R : Une étape du cycle de vie vs une action fournie par un plugin et liée à une phase.

**Q : -DskipTests vs -Dmaven.test.skip=true ?** R : Compile les tests sans les lancer vs ne les compile pas.

**Q : Comment construire un seul module et ses dépendances ?** R : `mvn -pl module -am install`.

**Q : À quoi sert help:effective-pom ?** R : Afficher le pom final après héritage, BOM et profils.

**Q : À quoi sert le Maven Wrapper ?** R : Garantir une version identique de Maven partout sans installation préalable.

**Q : Rôle de Nexus ou Artifactory ?** R : Proxy et cache des dépôts publics, hébergement des artefacts internes.

**Q : Que contient settings.xml ?** R : La configuration propre à la machine : miroirs, identifiants, profils.

**Q : Parent vs BOM ?** R : Héritage de configuration et versions (un seul parent) vs import de versions uniquement (plusieurs possibles).

**Q : implementation vs api en Gradle ?** R : Dépendance non exposée aux consommateurs vs exposée (transitive à la compilation).

**Q : Comment Gradle résout-il un conflit de versions ?** R : Par défaut, la version la plus haute.

**Q : Pourquoi Gradle est-il souvent plus rapide ?** R : Builds incrémentaux, cache de build réutilisable, démon.

**Q : Peut-on republier une version de release ?** R : Non, une release est immuable ; on publie une nouvelle version.

### Questions de situation

**Q : "L'application lève un NoSuchMethodError en production mais compile sans problème."**
R : "Symptôme classique d'un **conflit de dépendances transitives** : la version présente à l'exécution n'est pas celle avec laquelle on a compilé.
Je lancerais `mvn dependency:tree` pour visualiser l'arbre et repérer quelle librairie est présente en plusieurs versions et laquelle a été retenue par la règle du 'nearest wins'.
La correction dépend du cas : soit **exclure** la version indésirable de la dépendance qui la tire, soit **imposer** la bonne version dans `dependencyManagement`, soit aligner tout le monde via un **BOM**.
En prévention, le plugin **enforcer** avec la règle de convergence des dépendances fait échouer le build quand un conflit apparaît, plutôt que de le découvrir en production.
Et je vérifierais que l'environnement d'exécution n'ajoute pas lui-même des librairies au classpath, c'est une source fréquente d'écart entre local et production."

**Q : "Le build met 15 minutes en CI."**
R : "Je mesurerais d'abord où le temps passe.
Le levier le plus efficace en général : **cacher le dépôt local `~/.m2`** entre les exécutions, sinon on retélécharge toutes les dépendances à chaque build.
Ensuite le **cache des layers Docker** si on construit une image, en ordonnant le Dockerfile pour que les dépendances soient copiées avant le code source.
Puis la **parallélisation** : `mvn -T` pour construire les modules en parallèle, et faire tourner ensemble les étapes indépendantes du pipeline.
Enfin, séparer les cycles : sur une pull request, build et tests rapides ; les tests longs et l'analyse complète sur la branche d'intégration.
Si la lenteur vient des tests, c'est un problème de pyramide plutôt que de build."

**Q : "Un build passe sur ton poste mais échoue en CI."**
R : "Je chercherais une différence d'environnement : version de Maven ou du JDK (d'où le wrapper et une toolchain explicite), un artefact présent dans mon `~/.m2` local mais absent du dépôt de l'entreprise (une dépendance installée à la main), un SNAPSHOT différent, un profil activé localement, une variable d'environnement, ou des tests qui dépendent de l'ordre, du fuseau horaire ou de la locale.
Je reproduirais dans un conteneur propre avec la même image que la CI et `-U` pour forcer les mises à jour : si ça échoue aussi, c'est mon poste qui masquait le problème."

═══════════════════════════════════════════
# 31. DOCKER
═══════════════════════════════════════════

### Vue d'ensemble

Docker répond à un problème vieux comme le déploiement : *"ça marche sur ma machine"*. Il empaquette l'application **avec tout son environnement d'exécution** (librairies système, dépendances, configuration) dans une image immuable qui se comporte identiquement partout.

Techniquement, un conteneur n'est pas une machine virtuelle : il n'y a pas d'OS invité. C'est un **processus isolé** sur le noyau de l'hôte, grâce à deux mécanismes Linux : les **namespaces** (isolation de la vue : processus, réseau, système de fichiers) et les **cgroups** (limitation des ressources : CPU, mémoire). D'où sa légèreté et son démarrage instantané comparé à une VM.

L'autre apport majeur est l'**immuabilité** : on ne modifie pas un conteneur en production, on en déploie un nouveau. Cela rend les déploiements reproductibles et les rollbacks triviaux.

### Cours

**Image vs conteneur** : l'image est un template en lecture seule, le conteneur une instance en exécution avec une couche inscriptible par-dessus.

**Layers** : chaque instruction du Dockerfile crée une couche, mise en cache et partagée entre images. D'où deux règles : ordonner **du moins au plus changeant** (dépendances avant code source) pour maximiser le cache, et savoir que supprimer un fichier dans une couche ultérieure ne réduit pas la taille, il reste dans la couche précédente.

**Multi-stage build** : la pratique la plus impactante :
```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline        # couche cachée tant que le pom ne change pas
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/app.jar app.jar
RUN addgroup -S app && adduser -S app -G app
USER app
ENTRYPOINT ["java","-jar","app.jar"]
```
Le stage de build contient le JDK et Maven ; l'image finale seulement un JRE et le jar. On passe couramment de 1,5 Go à 200 Mo.

**Bonnes pratiques** : image de base minimale (alpine, distroless) pour réduire taille et surface d'attaque ; utilisateur **non-root** ; `.dockerignore` pour exclure `target/`, `.git/` ; un processus par conteneur ; ne jamais mettre de secret dans une image (il reste dans les couches).

**CMD vs ENTRYPOINT** : ENTRYPOINT définit la commande principale, CMD ses arguments par défaut, remplaçables au `docker run`.

**Volumes** : le conteneur est éphémère ; les données persistantes vont dans un volume.

**Réseau** : chaque conteneur a sa pile réseau. Dans un réseau Docker, les conteneurs se joignent par leur **nom de service** : `localhost` dans un conteneur désigne le conteneur lui-même, erreur très fréquente.

**Docker Compose** : orchestrer plusieurs conteneurs localement via un YAML (application + base + cache).

**Sécurité** : scanner les images (Trivy, Snyk), rebâtir régulièrement pour récupérer les correctifs de la base, épingler les versions d'images plutôt que d'utiliser `latest`.

**Instructions du Dockerfile, les nuances** :
- `COPY` vs `ADD` : ADD sait aussi extraire des archives locales et télécharger des URL ; on préfère COPY, plus prévisible.
- `ARG` (variable disponible au **build** uniquement) vs `ENV` (variable présente aussi à l'**exécution**). Un secret passé en ARG reste visible dans l'historique de l'image : utiliser les secrets de BuildKit (`RUN --mount=type=secret`).
- `RUN` exécute au build ; `CMD`/`ENTRYPOINT` à l'exécution.
- **Forme exec vs forme shell** : `ENTRYPOINT ["java", "-jar", "app.jar"]` (exec) lance java comme **PID 1**, qui reçoit directement le SIGTERM ; `ENTRYPOINT java -jar app.jar` (shell) lance un `sh -c`, qui ne transmet pas toujours les signaux : l'application est tuée brutalement après le délai de grâce. Si un script de démarrage est nécessaire, finir par `exec java ...`.
- `HEALTHCHECK` (Docker seul, ignoré par Kubernetes qui a ses probes), `EXPOSE` (documentaire), `WORKDIR`, `USER`, `LABEL`.
- **BuildKit** : cache de dépendances avec `RUN --mount=type=cache,target=/root/.m2 mvn package`, builds parallèles, multi-architecture avec `docker buildx` (images amd64 et arm64).

**Construire une image Java sans Dockerfile** : **Jib** (plugin Maven/Gradle, sans démon Docker, couches séparées dépendances/ressources/classes), **Buildpacks** (`mvn spring-boot:build-image`, image optimisée automatiquement). Les **jars en couches** de Spring Boot (`java -Djarmode=tools -jar app.jar extract --layers`) permettent de séparer dépendances et code pour un meilleur cache.

**Images de base** : `eclipse-temurin:<version>-jre`, variantes `-alpine` (musl, très légère, parfois des différences de comportement natif), images **distroless** (sans shell ni gestionnaire de paquets, surface d'attaque minimale, mais débogage plus difficile), images `ubi` (Red Hat, pour OpenShift). Épingler par **digest** (`@sha256:...`) pour une reproductibilité totale.

**Stockage** : *volume* nommé géré par Docker (persistance), *bind mount* (un répertoire de l'hôte monté, pratique en développement), *tmpfs* (en mémoire).

**Réseau** : `bridge` (défaut, réseau isolé avec NAT ; sur un réseau utilisateur, résolution DNS par nom de conteneur), `host` (partage la pile réseau de l'hôte), `none`. Publier un port : `-p 8080:8080` (hôte:conteneur).

**Commandes essentielles** : `docker build -t app:1.0 .`, `docker run -d --name app -p 8080:8080 -e VAR=x app:1.0`, `docker ps -a`, `docker logs -f app`, `docker exec -it app sh`, `docker inspect`, `docker stats`, `docker images`, `docker rmi`, `docker system prune`, `docker history image` (voir les couches et leur taille), `docker compose up -d` / `down` / `logs`.

**Docker Compose** : `depends_on` n'attend par défaut que le **démarrage** du conteneur, pas que le service soit prêt ; avec `condition: service_healthy` et un healthcheck, on attend vraiment. Fichiers d'override pour varier par environnement, profils.

**Écosystème** : standard **OCI** (format d'image et runtime, indépendant de Docker), runtimes **containerd** et CRI-O (utilisés par Kubernetes, qui ne passe plus par le démon Docker depuis la version 1.24), **Podman** (sans démon, conteneurs rootless), registries (Docker Hub, GHCR, Harbor, ECR, ACR).

**JVM en conteneur, la checklist** : JDK récent (détection des limites cgroups v2), `-XX:MaxRAMPercentage=70` à `75`, `-XX:ActiveProcessorCount` si la limite CPU est fractionnaire, `ExitOnOutOfMemoryError`, logs sur stdout, utilisateur non root, arrêt propre (exec form), éventuellement CDS/AOT cache pour le démarrage.

### En situation

- **"Notre image fait 1,5 Go"** → multi-stage, base minimale, layers.
- **"Le build Docker est lent"** → ordre des instructions, cache.
- **"Ça marche en local mais pas en conteneur"** → config, réseau, chemins, ressources.
- **"Comment sécuriser une image ?"** → non-root, base minimale, scan, pas de secret.

### Questions techniques

**Q : Conteneur vs VM ?** R : Le conteneur est un processus isolé sur le noyau hôte (namespaces + cgroups) ; la VM virtualise un OS complet.

**Q : Quels mécanismes Linux sous-tendent les conteneurs ?** R : Les namespaces (isolation) et les cgroups (limitation de ressources).

**Q : Image vs conteneur ?** R : Template en lecture seule vs instance en exécution avec couche inscriptible.

**Q : Pourquoi le multi-stage ?** R : Ne garder que l'artefact final dans l'image, sans les outils de build, taille et surface d'attaque réduites.

**Q : Pourquoi ordonner les instructions ?** R : Pour maximiser le cache des couches.

**Q : Supprimer un fichier réduit-il l'image ?** R : Non, s'il a été ajouté dans une couche précédente.

**Q : CMD vs ENTRYPOINT ?** R : Commande principale vs arguments par défaut remplaçables.

**Q : Pourquoi non-root ?** R : Limiter les dégâts en cas de compromission du conteneur.

**Q : Peut-on mettre un secret dans une image ?** R : Non, il reste consultable dans les couches.

**Q : COPY vs ADD ?** R : COPY copie ; ADD sait aussi extraire des archives et télécharger, donc on préfère COPY.

**Q : ARG vs ENV ?** R : Variable de build uniquement vs variable présente aussi à l'exécution.

**Q : Forme exec vs forme shell de l'ENTRYPOINT ?** R : Le processus est PID 1 et reçoit les signaux vs il est lancé par un shell qui peut ne pas transmettre le SIGTERM.

**Q : Comment ne pas laisser de secret dans une image ?** R : Secrets BuildKit (RUN --mount=type=secret), jamais ARG/ENV/COPY d'un secret.

**Q : C'est quoi Jib ?** R : Un outil qui construit des images Java depuis Maven/Gradle sans Dockerfile ni démon Docker.

**Q : Alpine vs distroless ?** R : Image minimale avec shell (libc musl) vs image sans shell ni gestionnaire de paquets, encore plus réduite.

**Q : Volume vs bind mount ?** R : Stockage géré par Docker vs répertoire de l'hôte monté.

**Q : depends_on garantit-il que la base est prête ?** R : Non, seulement démarrée ; il faut condition: service_healthy avec un healthcheck.

**Q : Kubernetes utilise-t-il encore Docker ?** R : Non depuis 1.24 : il utilise containerd ou CRI-O, et exécute les mêmes images OCI.

**Q : Podman vs Docker ?** R : Podman fonctionne sans démon et en rootless, avec une CLI compatible.

**Q : Comment voir le poids de chaque couche ?** R : docker history.

**Q : Pourquoi épingler une image par digest ?** R : Un tag peut être déplacé ; le digest garantit exactement le même contenu.

### Questions de situation

**Q : "Votre image fait 1,5 Go, réduis-la."**
R : "Du plus au moins efficace.
Le **multi-stage build** d'abord : si l'image contient le JDK, Maven et les sources, c'est le gros du poids. Un stage compile, le stage final ne garde que le jar sur un JRE. On passe typiquement à quelques centaines de mégas.
L'**image de base** ensuite : alpine ou distroless réduit encore et diminue la surface d'attaque, avec moins de paquets à patcher.
Les **couches** : regrouper installation et nettoyage dans une même instruction, puisque supprimer plus tard ne réduit rien.
Et un **`.dockerignore`** correct, pour ne pas envoyer `target/` ou `.git/` dans le contexte de build.
Bénéfices concrets : déploiements plus rapides, moins de bande passante, moins de vulnérabilités à suivre."

**Q : "L'application marche en local mais pas dans le conteneur."**
R : "Je chercherais les différences d'environnement, dans cet ordre.
La **configuration** : un fichier de propriétés ou une variable présente en local et absente du conteneur. Je vérifierais avec `docker exec env`.
Le **réseau**, cause très fréquente : `localhost` dans un conteneur désigne le conteneur lui-même, pas l'hôte. Une base joignable en local sur `localhost:5432` doit être référencée par son nom de service dans le réseau Docker.
Les **chemins et permissions** : un chemin absolu qui n'existe pas dans l'image, ou un conteneur non-root sans droit d'écriture là où il essaie.
Les **ressources** : si la JVM ne respecte pas la limite mémoire du conteneur, celui-ci est tué, OOMKilled.
En pratique je lance un shell avec `docker exec -it` et je lis `docker logs`. L'erreur est souvent explicite, mais personne n'est allé la lire."

**Q : "Ton application dans un conteneur met 30 secondes à s'arrêter et perd des requêtes."**
R : "Trente secondes, c'est le délai de grâce par défaut : le processus n'a pas réagi au SIGTERM et a été tué par SIGKILL. Cause probable : l'ENTRYPOINT en forme **shell**, donc java n'est pas PID 1 et ne reçoit pas le signal, ou un script qui lance java sans `exec`.
Correction : forme exec, ou `exec java ...` dans le script, voire un mini-init comme `tini`. Ensuite, côté application : activer l'arrêt gracieux (`server.shutdown=graceful` en Spring Boot) pour finir les requêtes en cours, et en Kubernetes un `preStop` court pour laisser le temps au retrait du pod des endpoints."

═══════════════════════════════════════════
# 32. KUBERNETES & OPENSHIFT
═══════════════════════════════════════════

### Vue d'ensemble

Docker résout l'empaquetage d'une application ; Kubernetes résout l'**exploitation de centaines de conteneurs** sur un parc de machines. Sans orchestrateur, il faudrait décider à la main où placer chaque conteneur, le relancer quand il meurt, répartir le trafic, gérer les mises à jour.

Son principe fondateur est la **réconciliation déclarative** : on décrit l'**état désiré** ("je veux 3 instances de cette image"), et des contrôleurs comparent en boucle l'état réel à l'état désiré et agissent pour combler l'écart. On ne donne jamais d'ordre impératif, c'est ce qui rend le système auto-réparateur, et c'est aussi le fondement du GitOps.

Le corollaire pratique : pour que Kubernetes fasse bien son travail, il a besoin d'informations que **l'application doit lui fournir** : des sondes de santé pour savoir si elle va bien, des déclarations de ressources pour la placer correctement, et un arrêt propre pour ne pas couper de requêtes.

### Cours

**Objets principaux :**
- **Pod** : plus petite unité déployable ; un ou plusieurs conteneurs partageant réseau et volumes. Éphémère par nature.
- **ReplicaSet** : maintient N copies d'un pod.
- **Deployment** : déclare l'état désiré et pilote les ReplicaSets lors des mises à jour.
- **Service** : point d'accès réseau stable devant des pods dont les IP changent. `ClusterIP` (interne), `NodePort`, `LoadBalancer`.
- **Ingress** : routage HTTP externe par host/chemin (l'équivalent OpenShift s'appelle **Route**). Son successeur officiel est la **Gateway API** (voir plus bas).
- **ConfigMap** / **Secret** : configuration non sensible / sensible (encodée base64, **pas chiffrée** par défaut, il faut activer le chiffrement au repos ou utiliser un vault externe).
- **Namespace** : isolation logique (dev, staging, prod).
- **StatefulSet** : pour les applications à état, identité stable, stockage persistant attaché, ordre de démarrage garanti.
- **DaemonSet** : un pod par nœud (agents de logs, de monitoring).
- **Job / CronJob** : traitement ponctuel ou planifié.

**Sondes (probes)** : le contrat entre l'application et l'orchestrateur :
- **liveness** : "es-tu vivant ?", en échec, le pod est **redémarré**.
- **readiness** : "es-tu prêt à recevoir du trafic ?", en échec, le pod est **retiré du Service** sans redémarrage.
- **startup** : protège les applications lentes à démarrer, en suspendant les deux autres le temps du démarrage.

Erreur classique : une liveness avec un délai initial trop court tue une application qui n'a pas fini de démarrer, en boucle.

**Ressources** : **requests** = ce qui est réservé et sert au placement ; **limits** = plafond. Dépasser la limite mémoire tue le conteneur (**OOMKilled**) ; dépasser la limite CPU ne tue pas mais **throttle** : le service ralentit silencieusement, ce qui est plus difficile à diagnostiquer.

**HPA** (Horizontal Pod Autoscaler) : ajuste le nombre de replicas selon une métrique (CPU, mémoire, ou métrique personnalisée). Prérequis : l'application doit être **stateless**, sinon les requêtes d'un même utilisateur tombant sur des pods différents cassent.

**Mise à jour** : le rolling update remplace progressivement les pods (`maxSurge`, `maxUnavailable`). `kubectl rollout undo` revient à la version précédente.

**Arrêt propre** : à la suppression d'un pod, Kubernetes envoie un SIGTERM puis attend un délai de grâce. L'application doit cesser d'accepter de nouvelles requêtes et terminer celles en cours. Un `preStop` hook avec une courte pause laisse le temps au retrait des tables de routage.

**Réseau** : chaque pod a sa propre IP ; les Services fournissent un nom DNS interne stable. Les **NetworkPolicies** restreignent qui peut parler à qui.

**Sécurité** : ServiceAccount et RBAC (qui a le droit de faire quoi sur l'API), SecurityContext (non-root, système de fichiers en lecture seule).

**OpenShift** = Kubernetes + Routes, ImageStreams, BuildConfig (Source-to-Image), sécurité renforcée par défaut (SCC, interdiction du root), console web intégrée, opérateurs préinstallés.

**Architecture d'un cluster** :
- **Plan de contrôle** : `kube-apiserver` (point d'entrée unique, toute action passe par lui), **etcd** (base clé-valeur qui stocke tout l'état du cluster), `kube-scheduler` (choisit le nœud de chaque pod selon ressources, affinités, contraintes), `kube-controller-manager` (les boucles de réconciliation : Deployments, ReplicaSets, nœuds...), `cloud-controller-manager`.
- **Nœuds** : `kubelet` (agent qui fait tourner les pods et exécute les probes), le runtime de conteneurs (containerd, CRI-O), `kube-proxy` (règles réseau des Services, en iptables, IPVS ou eBPF selon le plugin réseau).
- **Plugin réseau CNI** (Calico, Cilium, Flannel) qui donne une IP à chaque pod et applique les NetworkPolicies.

**DNS interne** : un Service `api` dans le namespace `boutique` est joignable en `api.boutique.svc.cluster.local` (ou simplement `api` depuis le même namespace). Un Service **headless** (`clusterIP: None`) renvoie directement les IP des pods (utilisé par les StatefulSets).

**Exposition vers l'extérieur** : `Ingress` (règles HTTP, nécessite un contrôleur), et désormais **Gateway API** (`GatewayClass`, `Gateway`, `HTTPRoute`), son successeur officiel : modèle par rôles (infrastructure vs équipes applicatives), routage plus expressif (poids, en-têtes, gRPC, TCP), portable entre implémentations. Le contrôleur communautaire **Ingress NGINX a été retiré en mars 2026** (plus de correctifs de sécurité), ce qui pousse les équipes à migrer vers Gateway API ou un autre contrôleur.

**Stockage** : `PersistentVolume` (une ressource de stockage), `PersistentVolumeClaim` (une demande de stockage par une application), `StorageClass` (provisionnement dynamique selon un type : SSD, réseau). Modes d'accès : ReadWriteOnce, ReadOnlyMany, ReadWriteMany.

**Classes de QoS** des pods, qui déterminent l'ordre d'éviction en cas de pression sur le nœud : **Guaranteed** (requests = limits pour tous les conteneurs, évincé en dernier), **Burstable** (requests < limits), **BestEffort** (aucune ressource déclarée, évincé en premier).

**Placement** : `nodeSelector`, **affinités** et anti-affinités (de nœud ou de pod : par exemple répartir les replicas sur des nœuds et des zones différents), **taints et tolerations** (un nœud "repousse" les pods qui ne tolèrent pas sa marque : nœuds GPU, nœuds dédiés), `topologySpreadConstraints`.

**Disponibilité** : `PodDisruptionBudget` (nombre minimal de pods disponibles pendant une maintenance volontaire, comme la vidange d'un nœud), plusieurs replicas répartis, readiness correcte, anti-affinité.

**Autoscaling** : **HPA** (nombre de pods), **VPA** (ajuste requests/limits d'un pod), **Cluster Autoscaler** ou Karpenter (ajoute des nœuds), **KEDA** (scale sur des métriques d'événements, comme le lag Kafka ou la longueur d'une file, y compris jusqu'à zéro). Redimensionnement en place des ressources d'un pod (*in-place resize*), arrivé récemment dans les versions stables.

**Conteneurs auxiliaires** : *init containers* (s'exécutent jusqu'au bout avant les conteneurs principaux : attendre une dépendance, préparer des fichiers), *sidecars* (tournent à côté : proxy de service mesh, agent de logs ; Kubernetes les supporte nativement comme init containers avec `restartPolicy: Always`).

**Extensibilité** : **CRD** (Custom Resource Definitions, de nouveaux types d'objets) et **opérateurs** (contrôleurs qui encodent le savoir-faire d'exploitation d'un logiciel : bases de données, Kafka avec Strimzi, certificats avec cert-manager).

**Sécurité, compléments** : *Pod Security Standards* (`privileged`, `baseline`, `restricted`) appliqués par l'admission *Pod Security* par namespace (les PodSecurityPolicies ont été supprimées en 1.25) ; `securityContext` (`runAsNonRoot`, `readOnlyRootFilesystem`, `allowPrivilegeEscalation: false`, capabilities supprimées) ; RBAC (`Role`/`ClusterRole` + `RoleBinding`) ; secrets externalisés (External Secrets Operator vers Vault ou un gestionnaire cloud, Sealed Secrets pour les stocker chiffrés dans Git) ; politique d'admission (Kyverno, OPA Gatekeeper) ; signature d'images.

**Statuts d'erreur fréquents** : `Pending` (aucun nœud n'a les ressources, ou PVC non lié, ou taint non toléré : `kubectl describe pod` le dit), `ImagePullBackOff` / `ErrImagePull` (image ou tag inexistant, registry privé sans `imagePullSecrets`), `CrashLoopBackOff` (le conteneur démarre puis meurt), `OOMKilled`, `Evicted` (pression mémoire ou disque sur le nœud), `CreateContainerConfigError` (ConfigMap ou Secret référencé absent).

**kubectl, les commandes à connaître** : `get pods -o wide`, `describe pod`, `logs -f <pod> [-c conteneur] [--previous]`, `exec -it <pod> -- sh`, `port-forward svc/api 8080:80`, `apply -f`, `delete`, `rollout status|history|undo deployment/api`, `scale --replicas=3`, `top pods` (metrics-server), `get events --sort-by=.lastTimestamp`, `explain`, `-n namespace`, `config use-context`, `debug` (conteneur éphémère de débogage dans un pod distroless).

**Service mesh** : Istio, Linkerd. Proxies (sidecar Envoy, ou mode *ambient* sans sidecar d'Istio) qui apportent mTLS automatique, retries, timeouts, répartition fine du trafic (canary), observabilité, sans code applicatif. Coût : complexité et ressources.

**Helm vs Kustomize, détail** : Helm est un gestionnaire de paquets (charts versionnés, templates Go, `values.yaml`, gestion des releases et rollback `helm rollback`, dépendances) ; Kustomize applique des **surcharges** (overlays par environnement) sur des manifestes YAML purs, sans templating, intégré à `kubectl apply -k`. On les combine aussi.

**Configuration** : une ConfigMap montée en **volume** est mise à jour dans le pod (avec un délai), mais une ConfigMap injectée en **variables d'environnement** ne change qu'au redémarrage du pod. Pour forcer un redémarrage quand la configuration change : une annotation de hash dans le template du Deployment (pratique courante avec Helm).

### En situation

- **"Un pod redémarre en boucle"** → logs `--previous`, événements, OOMKilled, probes.
- **"Comment scaler ?"** → HPA, stateless, limite de la base.
- **"Déployer sans coupure"** → rolling update + readiness + arrêt propre.
- **"Dimensionner requests et limits"** → mesurer, marge mémoire, méfiance sur le throttling CPU.

### Questions techniques

**Q : Quel est le principe de fonctionnement de Kubernetes ?** R : La réconciliation déclarative : des contrôleurs comparent l'état réel à l'état désiré et agissent en boucle.

**Q : Pod vs Deployment ?** R : Le pod est l'unité d'exécution éphémère ; le Deployment déclare et maintient l'état désiré via des ReplicaSets.

**Q : À quoi sert un Service ?** R : Fournir un point d'accès réseau stable devant des pods dont les IP changent.

**Q : liveness vs readiness vs startup ?** R : Vivant (redémarrage) / prêt (retrait du Service) / protège le démarrage lent.

**Q : requests vs limits ?** R : Réservé pour le placement vs plafond. Dépassement mémoire = kill ; dépassement CPU = throttling.

**Q : Un Secret est-il chiffré ?** R : Non, seulement encodé en base64 par défaut.

**Q : StatefulSet vs Deployment ?** R : Identité stable et stockage persistant par pod, pour les applications à état.

**Q : Prérequis d'un HPA ?** R : Une application stateless, et des métriques disponibles.

**Q : Que se passe-t-il à la suppression d'un pod ?** R : SIGTERM, puis délai de grâce ; l'application doit terminer proprement ses requêtes.

**Q : OpenShift vs Kubernetes ?** R : Routes, ImageStreams, BuildConfig, sécurité renforcée (non-root), console intégrée.

**Q : Les composants du plan de contrôle ?** R : kube-apiserver, etcd, kube-scheduler, kube-controller-manager (et cloud-controller-manager).

**Q : Rôle d'etcd ?** R : Stocker tout l'état du cluster (base clé-valeur distribuée et cohérente).

**Q : Rôle du kubelet ?** R : Faire tourner les pods sur le nœud et exécuter les probes.

**Q : Quel est le nom DNS d'un Service ?** R : nom.namespace.svc.cluster.local.

**Q : C'est quoi un Service headless ?** R : Un Service sans IP virtuelle dont le DNS renvoie directement les IP des pods.

**Q : Ingress vs Gateway API ?** R : Règles HTTP simples dépendantes d'annotations du contrôleur vs API plus riche, portable et organisée par rôles, successeur officiel.

**Q : Pourquoi migrer depuis Ingress NGINX ?** R : Le contrôleur communautaire a été retiré en mars 2026 et ne reçoit plus de correctifs de sécurité.

**Q : PV vs PVC vs StorageClass ?** R : Ressource de stockage vs demande de stockage vs type de stockage pour le provisionnement dynamique.

**Q : Les classes de QoS ?** R : Guaranteed, Burstable, BestEffort ; elles déterminent l'ordre d'éviction.

**Q : Taint vs affinité ?** R : Le nœud repousse les pods non tolérants vs le pod exprime une préférence ou une exigence de placement.

**Q : À quoi sert un PodDisruptionBudget ?** R : Garantir un nombre minimal de pods disponibles pendant les interruptions volontaires.

**Q : HPA vs VPA vs KEDA ?** R : Nombre de pods selon une métrique / ressources d'un pod / scaling sur des événements externes, jusqu'à zéro.

**Q : Init container vs sidecar ?** R : S'exécute jusqu'au bout avant l'application vs tourne en parallèle pendant toute la vie du pod.

**Q : C'est quoi un opérateur ?** R : Un contrôleur personnalisé qui automatise l'exploitation d'un logiciel via des ressources CRD.

**Q : Qu'a remplacé les PodSecurityPolicies ?** R : Les Pod Security Standards appliqués par l'admission Pod Security (PSP supprimées en 1.25).

**Q : Causes d'un pod Pending ?** R : Ressources insuffisantes, PVC non lié, taint non toléré, contraintes d'affinité impossibles.

**Q : Causes d'un ImagePullBackOff ?** R : Image ou tag inexistant, registry inaccessible, identifiants (imagePullSecrets) manquants.

**Q : Comment déboguer un pod distroless sans shell ?** R : kubectl debug avec un conteneur éphémère.

**Q : Comment accéder à un service du cluster depuis son poste ?** R : kubectl port-forward.

**Q : Helm vs Kustomize ?** R : Paquets templatisés versionnés avec gestion de releases vs surcharges de YAML purs par environnement.

**Q : Une ConfigMap modifiée est-elle prise en compte sans redémarrage ?** R : Montée en volume oui (avec délai) ; en variables d'environnement non.

**Q : Qu'apporte un service mesh ?** R : mTLS, retries, timeouts, routage fin et observabilité, sans modifier le code.

### Questions de situation

**Q : "Un pod est en CrashLoopBackOff."**
R : "Démarche systématique.
Les **logs** d'abord : `kubectl logs <pod> --previous`. Le `--previous` est essentiel, il donne les logs de l'instance qui vient de crasher, pas de celle qui redémarre.
Puis `kubectl describe pod` pour les **événements** et le motif de terminaison. Si c'est **OOMKilled**, le conteneur dépasse sa limite mémoire, souvent parce que le heap Java est réglé au niveau de la limite sans laisser de place au hors-heap.
Si c'est un **échec de liveness**, la sonde est peut-être mal réglée : un délai initial trop court tue l'application avant qu'elle ait fini de démarrer, c'est précisément le rôle d'une **startup probe**.
Autres causes : variable d'environnement ou secret manquant, dépendance indisponible au démarrage, erreur de configuration.
Le fil : logs, puis événements, puis configuration."

**Q : "Comment tu configures requests et limits ?"**
R : "Je mesure sous charge représentative plutôt que d'estimer.
Les **requests** correspondent à la consommation normale, c'est ce que Kubernetes réserve pour placer le pod. Sous-évaluer expose aux évictions, surévaluer gaspille le cluster.
Les **limits** sont le plafond acceptable. Sur la **mémoire**, dépasser tue le conteneur : il faut une vraie marge, et un heap Java réglé en dessous pour laisser la place au metaspace, aux stacks de threads et aux buffers natifs, environ 25% de marge est une règle courante.
Sur le **CPU**, dépasser ne tue pas mais **throttle** : le service devient lent sans erreur, ce qui est bien plus difficile à diagnostiquer. C'est pourquoi certains recommandent de ne pas fixer de limite CPU du tout, en s'appuyant sur les requests pour le partage équitable.
Et je mettrais un monitoring sur le throttling et les OOMKilled pour ajuster dans le temps plutôt que de figer des valeurs."

**Q : "Déployer une nouvelle version sans aucune coupure."**
R : "Plusieurs briques.
Côté **Kubernetes** : rolling update, avec `maxUnavailable` à zéro si on ne veut aucune perte de capacité, et surtout des **readiness probes** justes, sans elles, le trafic part vers un pod qui n'a pas fini de démarrer et les utilisateurs voient des erreurs.
Côté **arrêt propre** : gestion du SIGTERM pour terminer les requêtes en cours, et un `preStop` avec une courte pause pour laisser le temps au retrait des tables de routage. Sinon on coupe des requêtes en vol.
Côté **stratégie**, si le changement est risqué : du **canary** via Argo Rollouts, une petite part du trafic, observation des métriques d'erreur et de latence, progression par paliers, rollback immédiat sinon.
Et le point le plus négligé : la **base de données**. Pendant le déploiement, les deux versions du code tournent en même temps, donc le schéma doit être compatible avec les deux. D'où les migrations progressives : ajouter avant de supprimer, jamais de changement destructif en une étape."

**Q : "Un pod reste en Pending."**
R : "`kubectl describe pod` en premier : la section Events indique presque toujours la raison. 'Insufficient memory/cpu' : aucun nœud n'a assez de ressources **demandées** (les requests, pas la consommation réelle) ; je réduirais des requests surévaluées ou j'ajouterais des nœuds (l'autoscaler du cluster devrait le faire). 'had taint that the pod didn't tolerate' : il manque une toleration. 'unbound PersistentVolumeClaim' : le stockage n'a pas pu être provisionné (StorageClass, zone). Ou une anti-affinité impossible à satisfaire avec le nombre de nœuds disponibles."

**Q : "Comment rendre un service hautement disponible sur Kubernetes ?"**
R : "Au moins trois replicas, répartis sur plusieurs nœuds et zones par anti-affinité ou `topologySpreadConstraints`, pour qu'une panne de nœud ou de zone ne les emporte pas tous. Un **PodDisruptionBudget** pour que les maintenances ne vident pas le service. Des **readiness probes** justes et un arrêt propre pour les déploiements sans coupure. Des requests réalistes pour un placement fiable, et un HPA pour absorber la charge.
Puis les dépendances : un service n'est pas plus disponible que sa base ou ses appels externes, d'où timeouts, circuit breakers et une base elle-même répliquée."

═══════════════════════════════════════════
# 33. CI/CD & GITOPS
═══════════════════════════════════════════

### Vue d'ensemble

L'intégration continue répond à un problème historique : quand des développeurs travaillent des semaines chacun de son côté, l'intégration finale est douloureuse et imprévisible ("integration hell"). La CI inverse la logique (**intégrer très souvent, en petits incréments, avec une validation automatique**) de sorte que chaque écart est détecté immédiatement, quand il est encore petit.

Le déploiement continu prolonge l'idée jusqu'à la production. L'intuition contre-intuitive est que **déployer plus souvent réduit le risque** : de petits changements fréquents sont plus faciles à valider, à comprendre et à annuler qu'une grosse livraison trimestrielle.

Le **GitOps** ajoute une dernière pièce : décrire l'état désiré de l'infrastructure dans Git et laisser un opérateur réconcilier le cluster. On retrouve exactement le modèle déclaratif de Kubernetes, appliqué au déploiement, avec un historique auditable et un rollback qui n'est qu'un `git revert`.

### Cours

**CI** : à chaque commit, build + tests + analyse automatiques. Prérequis : une suite de tests fiable et rapide.

**Continuous Delivery** : le code est toujours déployable, la mise en production reste un acte volontaire.
**Continuous Deployment** : tout ce qui passe la validation part automatiquement en production.

**Pipeline typique** : checkout → build → tests unitaires → analyse qualité/sécurité → build image → publication registry → déploiement dev → tests d'intégration → promotion vers les environnements supérieurs.

**Jenkins** : serveur d'automatisation, pipeline déclaré en `Jenkinsfile` versionné avec le code. Alternatives : GitLab CI, GitHub Actions, Tekton.

**GitOps** : Git est la source unique de vérité de l'infrastructure. Un opérateur (**ArgoCD**, Flux) surveille le dépôt et synchronise le cluster. Bénéfices : audit complet (chaque changement est un commit signé et revu), rollback par `git revert`, **détection de drift** (alerte si le cluster diverge de Git), et suppression des `kubectl apply` manuels.

Bonne pratique : **séparer le dépôt applicatif du dépôt de manifests**, pour que la CI applicative n'ait pas les droits sur l'infrastructure.

**Helm** : package manager Kubernetes. Un **chart** est un ensemble de templates paramétrés par un `values.yaml`, versionnable et réutilisable entre environnements. Alternative : Kustomize (surcharges sans templating).

**Stratégies de déploiement :**
- **Rolling** : remplacement progressif, par défaut.
- **Blue-Green** : deux environnements complets, bascule instantanée du trafic, rollback immédiat, mais double le coût d'infrastructure.
- **Canary** : une fraction du trafic sur la nouvelle version, observation, progression graduelle. Le meilleur compromis risque/coût.
- **Feature flags** : découpler le déploiement de l'activation. On livre du code désactivé, puis on l'active pour une population ciblée, indispensable en trunk-based.

**Argo Rollouts** : automatise canary et blue-green avec **analyse de métriques** : la progression n'a lieu que si les indicateurs restent sains.

**Sécurité du pipeline** : secrets dans un vault et jamais dans le code ni les logs, scan des images, principe du moindre privilège pour les comptes de service, signature des artefacts.

**Les plateformes de CI** :
- **Jenkins** : pipeline **déclaratif** (`pipeline { agent ...; stages { stage('Build') { steps { sh 'mvn -B verify' } } }; post { always { junit '...' } } }`, structuré et lisible) ou **scripté** (Groovy libre, plus puissant, plus difficile à maintenir) ; agents (nœuds d'exécution, souvent des pods Kubernetes éphémères), *shared libraries* pour mutualiser du code de pipeline entre dépôts, plugins nombreux (et à maintenir).
- **GitHub Actions** : *workflows* YAML dans `.github/workflows`, déclencheurs (`on: push`, `pull_request`, `workflow_dispatch`, `schedule`), *jobs* exécutés sur des *runners*, *steps* qui utilisent des *actions* réutilisables (`actions/checkout`, `actions/setup-java` avec cache Maven), matrices (plusieurs versions de JDK), secrets et environnements protégés, authentification OIDC vers le cloud sans secret longue durée.
- **GitLab CI** : `.gitlab-ci.yml`, *stages* et *jobs*, *runners*, artefacts et cache, environnements, *merge request pipelines*.
- **Tekton** (natif Kubernetes, tâches et pipelines en CRD), Azure DevOps, CircleCI.

**Artefacts vs cache** : un **artefact** est un produit du pipeline transmis à une étape suivante ou conservé (le jar, un rapport de tests) ; le **cache** accélère des exécutions futures (dépôt Maven), il peut être perdu sans conséquence.

**Principe "build once, deploy many"** : on construit **une seule fois** l'image, identifiée par une version immuable (ou le SHA du commit), et c'est **la même image** qui est promue de dev en recette puis en production ; seule la configuration change. Reconstruire pour chaque environnement casse la garantie que ce qui a été testé est ce qui part en production.

**Qualité et sécurité dans le pipeline** (*shift left*) : tests et couverture, **SonarQube** avec quality gate, **SAST** (analyse du code : Sonar, Semgrep, CodeQL), **SCA** (dépendances vulnérables : OWASP Dependency-Check, Dependabot, Renovate, Snyk), scan de l'image (Trivy, Grype), détection de secrets (gitleaks), **SBOM** (*Software Bill of Materials*, inventaire des composants au format CycloneDX ou SPDX), **signature** des images et attestations de provenance (Sigstore/cosign, cadre SLSA), DAST sur l'environnement de recette.

**ArgoCD, compléments** : une `Application` relie un dépôt (chemin, révision) à une destination (cluster, namespace) ; synchronisation manuelle ou automatique, `prune` (supprimer ce qui n'est plus dans Git), `selfHeal` (annuler une modification manuelle), *sync waves* et *hooks* pour ordonner (migrations avant déploiement), pattern **app of apps** ou `ApplicationSet` pour gérer de nombreux environnements et clusters. **Flux** est l'autre outil GitOps majeur.

**Promotion entre environnements en GitOps** : un répertoire ou une branche par environnement dans le dépôt de manifestes ; promouvoir = une PR qui met à jour le tag de l'image dans l'environnement suivant (automatisable avec Argo CD Image Updater ou par la CI), avec revue pour la production.

**Bases de données dans le pipeline** : migrations Flyway/Liquibase exécutées avant le déploiement de la nouvelle version (job dédié ou hook de synchronisation), toujours rétrocompatibles avec la version qui tourne encore.

### En situation

- **"Décris ton pipeline"** → du commit au pod.
- **"Rollback en urgence"** → revert, rollout undo, bascule canary.
- **"Le pipeline est trop lent"** → cache, parallélisation, découpage.
- **"Comment sécuriser le pipeline ?"** → secrets, scans, droits.

### Questions techniques

**Q : Quel problème résout la CI ?** R : L'integration hell, en intégrant souvent et en petits incréments, les écarts sont détectés tôt.

**Q : Delivery vs Deployment ?** R : Mise en production manuelle vs automatique.

**Q : Principe du GitOps ?** R : L'état désiré est dans Git ; un opérateur réconcilie le cluster en continu.

**Q : Avantages du GitOps ?** R : Audit, rollback par revert, détection de drift, plus de kubectl manuel.

**Q : C'est quoi un chart Helm ?** R : Un ensemble de templates Kubernetes paramétrés par values.yaml, versionnable.

**Q : Blue-green vs canary ?** R : Deux environnements avec bascule instantanée (coûteux) vs progression graduelle du trafic.

**Q : À quoi servent les feature flags ?** R : Découpler le déploiement de l'activation, livrer du code inactif, l'activer ensuite.

**Q : Pourquoi séparer dépôt applicatif et dépôt de manifests ?** R : Pour que la CI applicative n'ait pas les droits sur l'infrastructure.

**Q : Pipeline Jenkins déclaratif vs scripté ?** R : Structure imposée et lisible vs Groovy libre plus puissant mais moins maintenable.

**Q : À quoi servent les shared libraries Jenkins ?** R : Mutualiser du code de pipeline entre de nombreux dépôts.

**Q : Les éléments d'un workflow GitHub Actions ?** R : Déclencheurs, jobs, runners, steps, actions réutilisables, secrets, matrices.

**Q : Artefact vs cache ?** R : Produit du pipeline transmis ou conservé vs accélérateur d'exécutions futures, jetable.

**Q : C'est quoi "build once, deploy many" ?** R : Construire une seule image et promouvoir la même d'environnement en environnement, seule la configuration changeant.

**Q : C'est quoi un SBOM ?** R : L'inventaire des composants et dépendances d'un logiciel (CycloneDX, SPDX).

**Q : À quoi sert cosign ?** R : Signer et vérifier des images de conteneur et des attestations (Sigstore).

**Q : SAST vs SCA ?** R : Analyse du code maison vs analyse des dépendances tierces.

**Q : prune et selfHeal dans ArgoCD ?** R : Supprimer les ressources retirées de Git / annuler les modifications manuelles faites sur le cluster.

**Q : C'est quoi le pattern app of apps ?** R : Une Application ArgoCD qui déclare d'autres Applications, pour gérer de nombreux services et environnements.

**Q : Comment s'authentifier au cloud depuis une CI sans secret stocké ?** R : Avec la fédération OIDC : la CI obtient un jeton éphémère échangé contre des droits cloud.

### Questions de situation

**Q : "Décris ton pipeline de déploiement de bout en bout."**
R : "Du commit au pod en production.
Je pousse sur une branche, ce qui déclenche la CI (**Jenkins**, GitHub Actions ou GitLab CI) : compilation Maven, tests unitaires et d'intégration, analyse de qualité et de couverture. Si tout passe, construction d'une **image Docker** en multi-stage et publication dans le registry interne, taguée avec la version.
Le tag est ensuite mis à jour dans le **dépôt GitOps** où vivent les manifests et les values Helm. C'est ce commit qui constitue la véritable déclaration de déploiement.
**ArgoCD** détecte le changement et synchronise le cluster Kubernetes (ou OpenShift). Pour les déploiements sensibles, **Argo Rollouts** fait du canary : une fraction du trafic, observation des métriques, progression.
L'intérêt du modèle : personne ne fait de `kubectl apply` manuel. Chaque changement d'infrastructure est un commit, donc revu, auditable et réversible, un rollback est un `git revert`."

**Q : "Une version déployée provoque des erreurs en production."**
R : "La priorité est de **rétablir le service**, pas de comprendre le bug. On diagnostique après.
Rollback immédiat : en GitOps, un `git revert` du commit de déploiement, et ArgoCD resynchronise. Si le temps presse davantage, `kubectl rollout undo` agit directement, mais il faut ensuite réaligner Git, sinon ArgoCD détectera un drift et remettra la version fautive.
Avec un canary, c'est plus simple encore : on annule la progression, le trafic revient sur la version stable, et l'impact s'est limité au pourcentage exposé.
Le point d'attention reste la **base de données** : si la version fautive a appliqué une migration destructive, revenir sur le code ne suffit pas, d'où la règle des migrations toujours rétrocompatibles.
Une fois le service rétabli, analyse à froid : logs, traces, métriques, reproduction en test. Et une question de fond : pourquoi la CI et le canary ne l'ont pas détecté ? La réponse renforce les garde-fous."

**Q : "Conçois un pipeline pour un microservice Java, du commit à la production."**
R : "Sur chaque **pull request** : build Maven avec cache, tests unitaires et d'intégration (Testcontainers), analyse Sonar avec quality gate, scan des dépendances et détection de secrets. Rapide, sous dix minutes, et bloquant pour le merge.
Au **merge sur main** : construction d'**une** image en multi-stage, tag immuable (version ou SHA), scan de l'image, génération du SBOM, signature, publication dans le registry. Puis mise à jour du tag dans le dépôt GitOps de l'environnement de dev, ArgoCD déploie, tests de fumée et tests de contrat.
**Promotion** vers la recette puis la production par PR sur le dépôt GitOps, la même image, avec revue pour la production. En production, **canary** avec Argo Rollouts et analyse automatique des métriques d'erreur et de latence, rollback automatique si elles se dégradent.
Transversalement : secrets dans un vault, comptes de service à moindre privilège, et suivi des métriques DORA pour améliorer le pipeline lui-même."

═══════════════════════════════════════════
# 34. OBSERVABILITÉ
═══════════════════════════════════════════

### Vue d'ensemble

Le **monitoring** répond à des questions qu'on a prévues à l'avance ("le CPU dépasse-t-il 80% ?"). L'**observabilité** vise plus haut : pouvoir répondre à des questions qu'on n'avait **pas** anticipées, à partir de ce que le système émet. C'est devenu indispensable avec les microservices : dans un monolithe, une pile d'appels suffit à comprendre ; dans un système distribué, une requête traverse dix services et aucun ne détient l'histoire complète.

Les trois piliers se complètent plutôt qu'ils ne se substituent : les **métriques** disent qu'il y a un problème et à quelle ampleur, les **traces** disent où, les **logs** disent quoi exactement. Un bon dispositif permet de passer de l'un à l'autre, d'une alerte à la trace concernée, puis aux logs de la requête.

### Cours

**Logs** : événements textuels horodatés.
- Niveaux : TRACE < DEBUG < INFO < WARN < ERROR.
- **Structured logging** (JSON) : chaque log porte des champs (timestamp, niveau, service, traceId, utilisateur), ce qui rend l'indexation et la recherche efficaces.
- **MDC** (Mapped Diagnostic Context) : attacher automatiquement un identifiant de corrélation à tous les logs d'une requête.
- Stack : collecte (Fluentd, Filebeat) → stockage (Elasticsearch, Loki) → visualisation (Kibana, Grafana).
- Bonnes pratiques : ne jamais logger de données sensibles, éviter le logging excessif dans les chemins chauds, toujours logger le stacktrace avec l'erreur.

**Métriques** : valeurs numériques agrégées dans le temps. Prometheus collecte par **scraping** d'un endpoint exposé par l'application.
- **Counter** : ne fait qu'augmenter (nombre de requêtes, d'erreurs).
- **Gauge** : monte et descend (connexions actives, taille de file).
- **Histogram** : distribution, permettant de calculer des percentiles (p50, p95, p99).
- **Summary** : percentiles calculés côté application.

Le réflexe important : raisonner en **percentiles**, pas en moyenne. Une moyenne de 100 ms peut cacher que 5% des utilisateurs attendent 3 secondes.

**Traces** : le parcours d'une requête à travers les services. Une **trace** regroupe des **spans**, chacun représentant une opération (appel HTTP, requête SQL). La **propagation de contexte** transmet l'identifiant de trace entre services via des en-têtes HTTP standardisés (`traceparent`).

**OpenTelemetry** est le standard unifié d'instrumentation (SDK + protocole), indépendant du backend (Jaeger, Tempo, Zipkin). C'est ce qui évite de s'enfermer chez un fournisseur.

**Les 4 signaux dorés** (Google SRE) : latence, trafic, erreurs, saturation. C'est la base d'un tableau de bord utile.

**SLI / SLO / SLA** : l'indicateur mesuré, l'objectif interne visé, l'engagement contractuel envers le client. Un **error budget** dérive du SLO : si l'objectif est 99,9%, on tolère 0,1% d'indisponibilité, et ce budget arbitre entre livrer vite et stabiliser.

**Alerting** : alerter sur des **symptômes visibles par l'utilisateur** (taux d'erreur, latence) plutôt que sur des causes possibles (CPU élevé, qui peut être parfaitement normal). Trop d'alertes non actionnables produisent de la fatigue d'alerte, et les vraies passent inaperçues.

**Logging en Java** : **SLF4J** est une façade (l'API qu'on utilise dans le code : `LoggerFactory.getLogger(...)`), avec une implémentation au choix : **Logback** (défaut de Spring Boot), **Log4j 2** (performant, appenders asynchrones), ou `java.util.logging`. Utiliser les **placeholders** `log.debug("Commande {} créée pour {}", id, client)` plutôt que la concaténation, pour ne pas construire la chaîne si le niveau est désactivé ; passer l'exception en dernier argument pour avoir la stack trace. Les ponts (`jul-to-slf4j`, `log4j-over-slf4j`) unifient les logs des bibliothèques.

**Log4Shell** (CVE-2021-44228, décembre 2021) : Log4j 2 interprétait des expressions `${jndi:ldap://...}` contenues dans les messages loggés, permettant l'exécution de code à distance simplement en faisant logger une chaîne (un en-tête User-Agent par exemple). Leçons : ne jamais interpréter des données utilisateur, connaître ses dépendances transitives (SBOM), pouvoir patcher vite.

**Quoi logger, à quel niveau** : ERROR pour ce qui demande une intervention, WARN pour une anomalie gérée, INFO pour les événements métier significatifs et le cycle de vie, DEBUG pour le diagnostic. Jamais de mot de passe, jeton, numéro de carte ou donnée personnelle (RGPD) ; masquer si nécessaire. Logger une exception **une seule fois**, à l'endroit où elle est traitée (sinon la même erreur apparaît cinq fois).

**Méthodes d'analyse de métriques** : **RED** pour les services (*Rate*, *Errors*, *Duration*) et **USE** pour les ressources (*Utilization*, *Saturation*, *Errors*), complémentaires des quatre signaux dorés.

**Micrometer** : la façade de métriques de l'écosystème Java (le "SLF4J des métriques"), utilisée par Spring Boot et Quarkus, avec des exporteurs vers Prometheus, Datadog, OTLP... Types : `Counter`, `Gauge`, `Timer` (durée + nombre, avec histogrammes de percentiles), `DistributionSummary`. **Observation API** (Micrometer 1.10+) : une seule instrumentation qui produit à la fois métriques et traces.

**Cardinalité** : chaque combinaison de labels d'une métrique crée une série temporelle. Mettre un identifiant utilisateur ou une URL complète en label (au lieu du gabarit `/users/{id}`) crée des millions de séries et fait exploser Prometheus. Les labels doivent avoir peu de valeurs possibles.

**PromQL de base** : `rate(http_server_requests_seconds_count[5m])` (débit), `histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket[5m])))` (p95), `sum by (status) (...)`.

**OpenTelemetry, compléments** : API + SDK + protocole **OTLP** + **Collector** (reçoit, transforme, échantillonne et exporte vers un ou plusieurs backends). L'**agent Java** instrumente automatiquement HTTP, JDBC, Kafka, etc., sans modifier le code. **Échantillonnage** : *head-based* (décidé au début de la trace, simple) vs *tail-based* (décidé à la fin dans le collector, permet de garder toutes les traces en erreur ou lentes). **Exemplars** : relier un point de métrique à une trace précise. Propagation via l'en-tête W3C `traceparent` (et `baggage` pour des données de contexte).

**Corrélation** : injecter le `traceId` dans le MDC pour qu'il apparaisse dans chaque ligne de log, ce qui permet de passer d'une trace aux logs correspondants.

**Stacks courantes** : Prometheus + Grafana + Alertmanager ; Loki (logs) ; Tempo ou Jaeger (traces) ; ELK/OpenSearch ; solutions commerciales (Datadog, Dynatrace, New Relic, Elastic APM). **Profilage continu** (Pyroscope, async-profiler en continu, JFR) comme quatrième signal.

**Alerting, compléments** : alerter sur le **taux de consommation de l'error budget** (*burn rate*) plutôt que sur un seuil instantané : une alerte rapide si le budget mensuel se consume en quelques heures, une alerte lente si la dérive est durable. Chaque alerte doit être actionnable et liée à un *runbook*.

### En situation

- **"Comment tu sais que ton service va bien ?"** → signaux dorés, SLO, alertes.
- **"Débugger un problème qui n'arrive qu'en prod"** → logs, traces, corrélation.
- **"Où est le goulot dans une chaîne de services ?"** → distributed tracing.
- **"Que logger et à quel niveau ?"** → stratégie de logging.

### Questions techniques

**Q : Monitoring vs observabilité ?** R : Répondre à des questions prévues vs pouvoir répondre à des questions non anticipées.

**Q : Les 3 piliers ?** R : Logs (quoi), métriques (combien), traces (où).

**Q : Types de métriques Prometheus ?** R : Counter, Gauge, Histogram, Summary.

**Q : Pourquoi les percentiles plutôt que la moyenne ?** R : La moyenne masque les cas lents qui dégradent l'expérience d'une minorité d'utilisateurs.

**Q : Trace vs span ?** R : Parcours complet d'une requête vs opération individuelle dans ce parcours.

**Q : C'est quoi la propagation de contexte ?** R : La transmission du traceId entre services via les en-têtes HTTP.

**Q : Les 4 signaux dorés ?** R : Latence, trafic, erreurs, saturation.

**Q : SLI, SLO, SLA ?** R : Indicateur mesuré, objectif interne, engagement contractuel.

**Q : Sur quoi alerter ?** R : Sur des symptômes visibles par l'utilisateur, pas sur des causes potentielles.

**Q : SLF4J vs Logback ?** R : La façade (API) vs une implémentation.

**Q : Pourquoi utiliser des placeholders dans les logs ?** R : Pour ne pas construire la chaîne si le niveau est désactivé.

**Q : C'était quoi Log4Shell ?** R : Une faille de Log4j 2 qui exécutait du code via des expressions JNDI contenues dans les messages loggés.

**Q : Pourquoi logger une exception une seule fois ?** R : Pour éviter des doublons trompeurs ; on la logge là où elle est traitée.

**Q : RED vs USE ?** R : Débit, erreurs, durée pour les services vs utilisation, saturation, erreurs pour les ressources.

**Q : C'est quoi Micrometer ?** R : La façade de métriques (et d'observations) de l'écosystème Java, multi-backends.

**Q : Pourquoi ne pas mettre un identifiant utilisateur en label ?** R : Explosion de la cardinalité : une série temporelle par valeur.

**Q : Comment calculer un p95 en PromQL ?** R : histogram_quantile(0.95, ...) sur les buckets d'un histogramme.

**Q : Rôle du collector OpenTelemetry ?** R : Recevoir, transformer, échantillonner et exporter la télémétrie vers les backends.

**Q : Head-based vs tail-based sampling ?** R : Décision au début de la trace vs à la fin, ce qui permet de garder les traces en erreur ou lentes.

**Q : Comment relier logs et traces ?** R : En injectant le traceId dans le MDC de chaque log.

**Q : C'est quoi un burn rate ?** R : La vitesse de consommation de l'error budget, base d'alertes plus pertinentes que des seuils.

### Questions de situation

**Q : "Débugger un problème qui n'arrive qu'en production."**
R : "Cas le plus délicat, puisqu'on ne peut pas reproduire à volonté.
Je commencerais par **rassembler les preuves** : logs, métriques et traces au moment de l'incident. Avec du tracing distribué, je peux suivre une requête problématique de bout en bout et identifier le service ou l'appel responsable, c'est souvent ce qui fait gagner le plus de temps.
Puis **caractériser** : tous les utilisateurs ou un sous-ensemble ? Quelle fréquence ? Corrélation avec un déploiement, un pic de charge, une heure précise ? Beaucoup de bugs 'impossibles' sont des problèmes de concurrence qui n'apparaissent que sous charge réelle.
Ensuite **identifier les écarts prod/dev** : volume de données, configuration, latence réseau, ressources, versions. Le bug est presque toujours dans un de ces écarts.
Si je ne trouve pas, j'**instrumente** : logs ciblés ou métrique sur le chemin suspect, puis redéploiement pour collecter. Mieux vaut un déploiement d'observabilité qu'une correction devinée.
Exemple classique : un tracing OpenTelemetry qui fonctionne en local mais pas une fois déployé sur Kubernetes, à cause de la propagation de contexte entre services ou de la configuration du collector."

**Q : "Que surveilles-tu pour savoir qu'un service va bien ?"**
R : "Je partirais des **signaux visibles par l'utilisateur** plutôt que de métriques techniques isolées.
Les quatre essentiels : **taux d'erreur**, **latence** en p95 et p99 (pas la moyenne), **débit**, et **saturation** (CPU, mémoire, pool de connexions, taille des files).
Au-delà, quelques métriques **métier** valent souvent mieux que dix métriques d'infrastructure : par exemple le nombre de commandes validées par minute. Une chute là-dessus révèle un problème qu'aucune métrique système ne montrerait.
Pour les **alertes**, je les baserais sur des symptômes : 'le taux d'erreur dépasse X%' plutôt que 'le CPU dépasse 80%', qui peut être parfaitement normal. Et je définirais un **SLO** avec un error budget, ce qui donne un critère objectif pour arbitrer entre livrer et stabiliser.
Enfin, je m'assurerais que chaque log porte un **identifiant de corrélation**, pour relier un ticket utilisateur à la trace exacte de sa requête."

**Q : "La facture de l'outil d'observabilité a triplé en un mois."**
R : "Je chercherais trois causes. Une **explosion de cardinalité** des métriques (un label à valeurs illimitées ajouté récemment), visible en listant les métriques au plus grand nombre de séries. Un **volume de logs** devenu excessif (un DEBUG oublié en production, une boucle qui logge, des stack traces répétées). Une absence d'**échantillonnage** des traces.
Corrections : supprimer ou regrouper les labels fautifs, revoir les niveaux de log et filtrer au collecteur, échantillonner les traces en tail-based en gardant les erreurs. Et mettre en place un suivi du volume par service pour détecter la prochaine dérive avant la facture."

═══════════════════════════════════════════
# 35. SÉCURITÉ
═══════════════════════════════════════════

### Vue d'ensemble

La sécurité applicative repose sur une idée simple mais exigeante : **ne jamais faire confiance aux entrées**, et **ne jamais accorder plus de droits que nécessaire**. La majorité des failles ne viennent pas d'une cryptographie cassée mais de choses banales, une validation absente, un contrôle d'accès oublié, un secret laissé dans un dépôt.

Deux principes structurants. La **défense en profondeur** : plusieurs couches indépendantes, pour qu'une faille unique ne compromette pas tout. Et le **moindre privilège** : chaque composant, chaque compte n'obtient que ce dont il a strictement besoin, ce qui limite les dégâts en cas de compromission.

L'OWASP Top 10 classe les risques web par fréquence et impact. Depuis plusieurs éditions, c'est le **contrôle d'accès défaillant** qui arrive en tête, pas une attaque sophistiquée, simplement une vérification d'autorisation manquante côté serveur.

### Cours

**Authentification** (qui es-tu) vs **autorisation** (qu'as-tu le droit de faire). Deux étapes distinctes, souvent confondues.

**OWASP Top 10, les catégories essentielles** (classement complet de l'édition 2025 plus bas) :
- **Broken Access Control** : un utilisateur accède à des ressources qui ne sont pas les siennes en modifiant un identifiant dans l'URL. La vérification doit être faite **côté serveur, pour chaque ressource**, pas seulement masquée dans l'interface.
- **Cryptographic Failures** : données sensibles en clair, algorithmes obsolètes, absence de HTTPS.
- **Injection** : SQL, commandes système, LDAP. Défense : requêtes **paramétrées**, jamais de concaténation.
- **Insecure Design** : failles de conception, absence de modélisation des menaces.
- **Security Misconfiguration** : endpoints d'administration exposés, valeurs par défaut, stacktraces visibles.

**XSS** : injection de JavaScript dans une page consultée par d'autres. Défense : échapper les sorties, Content-Security-Policy.
**CSRF** : faire exécuter une action à un utilisateur authentifié à son insu. Défense : jeton anti-CSRF, cookies `SameSite`.

**JWT** : jeton signé composé de trois parties encodées en base64 (header, payload, signature). Il est **stateless** : le serveur vérifie la signature sans consulter de session, ce qui facilite le scaling. Points de vigilance : le payload est **lisible par tous** (jamais de secret dedans), la révocation est difficile (d'où des durées de vie courtes + refresh token), et il faut valider l'algorithme côté serveur pour éviter l'attaque `alg: none`.

**OAuth 2.0** est un protocole d'**autorisation déléguée** (autoriser une application à agir en votre nom) ; **OpenID Connect** ajoute la couche d'**authentification** par-dessus.

**RBAC** (par rôles) vs **ABAC** (par attributs : département, heure, propriété de la ressource), plus fin mais plus complexe.

**Mots de passe** : hashés avec un algorithme **lent et salé** (bcrypt, argon2, scrypt). La lenteur est volontaire : elle rend le brute force coûteux. Le sel empêche les rainbow tables.

**Secrets** : dans un gestionnaire dédié (HashiCorp Vault, Secrets Manager cloud), jamais dans le code, une image ou un dépôt. Rotation régulière.

**Entre services** : **mTLS** pour authentifier mutuellement les services (souvent fourni par un service mesh), approche **zero trust** : on ne fait pas confiance à un appel simplement parce qu'il vient du réseau interne.

**Autres mesures** : rate limiting, validation stricte des entrées, HTTPS partout, en-têtes de sécurité (HSTS, CSP, X-Content-Type-Options), pas de stacktrace en production, dépendances scannées et tenues à jour (une grande part des compromissions passe par une librairie vulnérable).

**OWASP Top 10, édition 2025** (remplace l'édition 2021) :
1. **A01 Broken Access Control**, qui absorbe désormais le SSRF.
2. **A02 Security Misconfiguration** (en forte hausse).
3. **A03 Software Supply Chain Failures** : dépendances, outils de build, distribution (nouvelle catégorie, élargit "composants vulnérables").
4. **A04 Cryptographic Failures**.
5. **A05 Injection** (SQL, commandes, LDAP, XSS).
6. **A06 Insecure Design**.
7. **A07 Authentication Failures**.
8. **A08 Software or Data Integrity Failures** (désérialisation non sûre, mises à jour non signées).
9. **A09 Security Logging and Alerting Failures** (l'accent passe sur l'alerte : des logs sans alerte servent peu).
10. **A10 Mishandling of Exceptional Conditions** (nouvelle : erreurs mal gérées, comportements "fail open").

Il existe aussi un **OWASP API Security Top 10**, centré sur les API (autorisation au niveau objet, BOLA, en tête).

**Failles à savoir expliquer** :
- **IDOR / BOLA** : accès à l'objet d'un autre en changeant un identifiant.
- **SSRF** (*Server-Side Request Forgery*) : faire appeler par le serveur une URL choisie par l'attaquant (par exemple l'endpoint de métadonnées du cloud `169.254.169.254` qui délivre des identifiants). Défense : liste blanche de destinations, pas de suivi aveugle des redirections, blocage des plages internes.
- **Mass assignment** : lier directement le JSON reçu à une entité permet de modifier des champs non prévus (`"role": "ADMIN"`). Défense : des DTOs d'entrée explicites.
- **Path traversal** : `../../etc/passwd` dans un nom de fichier. Défense : normaliser le chemin et vérifier qu'il reste sous le répertoire autorisé.
- **Open redirect** : une redirection vers une URL fournie par l'utilisateur, utilisée pour le phishing.
- **XXE**, **désérialisation non sûre**, **ReDoS** (voir les chapitres I/O et chaînes).
- **Clickjacking** : afficher le site dans une iframe piégée ; défense `X-Frame-Options` ou CSP `frame-ancestors`.

**Encodage, hachage, chiffrement, signature** : l'**encodage** (Base64) n'est pas une protection, il est réversible par tous ; le **hachage** est à sens unique (SHA-256 pour l'intégrité, bcrypt/argon2 pour les mots de passe) ; le **chiffrement** est réversible avec une clé (symétrique **AES-GCM**, une même clé, rapide ; asymétrique **RSA/ECC**, clé publique et clé privée, plus lent, utilisé pour l'échange de clés et la signature) ; la **signature** prouve l'origine et l'intégrité (clé privée pour signer, publique pour vérifier) ; le **HMAC** prouve l'intégrité et l'authenticité avec un secret partagé.

**TLS, la poignée de main en bref** (TLS 1.3) : le client propose des algorithmes et une part d'échange de clés, le serveur répond avec la sienne et son certificat ; les deux dérivent une clé de session symétrique (échange de type Diffie-Hellman, qui assure la *forward secrecy*) ; le client vérifie la chaîne de certificats jusqu'à une autorité de confiance et le nom du serveur. Un seul aller-retour en TLS 1.3. Le JDK 27 ajoute un échange de clés hybride **post-quantique** (ML-KEM) pour TLS 1.3. En Java, les certificats de confiance sont dans le **truststore** (`cacerts`), l'identité du serveur dans le **keystore** (formats PKCS12, JKS) ; une erreur `PKIX path building failed` signifie que le certificat (ou une autorité intermédiaire) n'est pas reconnu.

**OAuth 2.0, les flux** :
- **Authorization Code + PKCE** : le flux standard pour toute application avec un utilisateur (web, mobile, SPA) ; PKCE protège l'échange du code contre l'interception.
- **Client Credentials** : service à service, sans utilisateur.
- **Device Code** : appareils sans clavier (télévision).
- **Refresh token** : obtenir un nouveau jeton d'accès sans redemander l'authentification.
- Obsolètes : *Implicit* et *Resource Owner Password* (déconseillés par OAuth 2.1).
Rôles : *resource owner* (l'utilisateur), *client* (l'application), *authorization server* (Keycloak, Auth0...), *resource server* (l'API). **OIDC** ajoute l'**ID token** (identité de l'utilisateur) et l'endpoint userinfo. *Scopes* (ce que le client peut faire) vs rôles (ce que l'utilisateur peut faire).

**JWT, compléments** : algorithmes **HS256** (secret partagé, chaque vérificateur peut aussi signer) vs **RS256/ES256** (clé privée pour signer, publiques distribuées via JWKS : adapté aux microservices). Claims à valider : signature, `exp`, `nbf`, `iss` (émetteur), `aud` (audience, que le jeton est bien destiné à ce service). Jeton **opaque** + introspection : révocation immédiate, mais un appel au serveur d'autorisation par vérification. Côté navigateur, stocker les jetons en cookie `HttpOnly` plutôt qu'en localStorage (exposé au XSS), ou utiliser un BFF qui garde les jetons côté serveur.

**Sessions et cookies** : attributs `HttpOnly` (inaccessible au JavaScript), `Secure` (HTTPS uniquement), `SameSite=Lax/Strict` (limite l'envoi cross-site, protection CSRF), durée limitée, régénération de l'identifiant de session après connexion (contre la fixation de session).

**CORS, détail** : une requête "non simple" (méthode PUT/DELETE, en-tête `Authorization`, JSON) déclenche une requête **preflight** OPTIONS ; le serveur répond avec `Access-Control-Allow-Origin` (jamais `*` avec des identifiants), `-Allow-Methods`, `-Allow-Headers`, `-Allow-Credentials`, `-Max-Age`. CORS ne protège pas le serveur, il protège l'utilisateur du navigateur.

**Rate limiting, algorithmes** : *fixed window* (simple, rafales aux frontières de fenêtres), *sliding window* (plus juste), *token bucket* (autorise des rafales contrôlées), *leaky bucket* (lisse le débit). En Java : Bucket4j, Resilience4j RateLimiter, ou au niveau de la passerelle.

**Crypto en Java** : `SecureRandom` pour tout jeton, `MessageDigest`, `Mac` (HMAC), `Cipher` avec `AES/GCM/NoPadding` (jamais ECB), `KeyStore`, comparaison en temps constant (`MessageDigest.isEqual`) pour les secrets. Ne jamais inventer son propre algorithme.

**Gestion des secrets en pratique** : HashiCorp Vault (secrets dynamiques à durée de vie courte, par exemple des identifiants de base générés à la demande), gestionnaires cloud, injection au runtime, rotation, audit des accès.

**Conformité** : **RGPD** (minimisation, finalité, droit à l'effacement, journal des traitements, chiffrement des données personnelles), PCI-DSS pour les données de carte.

### En situation

- **"Comment sécuriser une API ?"** → checklist en couches.
- **"Authentification entre microservices ?"** → JWT propagé, mTLS, zero trust.
- **"Un utilisateur accède aux données d'un autre"** → broken access control.
- **"Gérer les secrets dans un pipeline ?"** → vault, moindre privilège.

### Questions techniques

**Q : Authentification vs autorisation ?** R : Qui tu es vs ce que tu as le droit de faire.

**Q : Quelle est la faille la plus fréquente selon l'OWASP ?** R : Le contrôle d'accès défaillant.

**Q : Comment se protéger d'une injection SQL ?** R : Requêtes paramétrées, jamais de concaténation.

**Q : XSS vs CSRF ?** R : Injection de script exécuté chez d'autres vs action déclenchée à l'insu d'un utilisateur authentifié.

**Q : Peut-on mettre un secret dans un JWT ?** R : Non, le payload est encodé et lisible.

**Q : Comment révoquer un JWT ?** R : Difficile par nature, d'où des durées courtes, un refresh token, ou une liste de révocation.

**Q : OAuth 2.0 vs OIDC ?** R : Autorisation déléguée vs authentification ajoutée par-dessus.

**Q : Pourquoi bcrypt plutôt que SHA-256 ?** R : Parce qu'il est volontairement lent et salé, ce qui rend le brute force coûteux.

**Q : C'est quoi le zero trust ?** R : Ne pas accorder de confiance en fonction de la position réseau ; authentifier et autoriser chaque appel.

**Q : Le top 3 de l'OWASP 2025 ?** R : Broken Access Control, Security Misconfiguration, Software Supply Chain Failures.

**Q : Quelles nouveautés dans l'OWASP 2025 ?** R : Software Supply Chain Failures et Mishandling of Exceptional Conditions ; le SSRF est intégré au contrôle d'accès.

**Q : C'est quoi une faille SSRF ?** R : Le serveur effectue une requête vers une URL choisie par l'attaquant (services internes, métadonnées cloud).

**Q : C'est quoi le mass assignment ?** R : Lier directement une entrée à un objet interne et permettre la modification de champs non prévus ; défense : DTOs d'entrée.

**Q : Comment se protéger du path traversal ?** R : Normaliser le chemin et vérifier qu'il reste dans le répertoire autorisé.

**Q : Encodage vs hachage vs chiffrement ?** R : Réversible par tous / à sens unique / réversible avec une clé.

**Q : Symétrique vs asymétrique ?** R : Une seule clé partagée, rapide (AES) vs paire publique/privée, plus lente (RSA, ECC).

**Q : C'est quoi un HMAC ?** R : Une empreinte calculée avec un secret partagé, qui prouve intégrité et authenticité.

**Q : Pourquoi AES-GCM plutôt qu'ECB ?** R : ECB chiffre les blocs identiques de la même façon et laisse fuiter des motifs ; GCM chiffre et authentifie.

**Q : Que signifie "PKIX path building failed" ?** R : Le certificat présenté (ou une autorité intermédiaire) n'est pas dans le truststore.

**Q : Keystore vs truststore ?** R : Ma propre identité (clé privée et certificat) vs les certificats auxquels je fais confiance.

**Q : Quel flux OAuth pour une SPA ou une application mobile ?** R : Authorization Code avec PKCE.

**Q : Quel flux OAuth entre deux services ?** R : Client Credentials.

**Q : Pourquoi le flux Implicit est-il obsolète ?** R : Le jeton transite dans l'URL du navigateur, exposé ; PKCE le remplace.

**Q : ID token vs access token ?** R : Identité de l'utilisateur pour le client (OIDC) vs droit d'accès présenté à l'API.

**Q : HS256 vs RS256 ?** R : Secret partagé (tout vérificateur peut signer) vs clé privée pour signer et clés publiques pour vérifier.

**Q : Quels claims valider dans un JWT ?** R : Signature, exp, nbf, iss et aud.

**Q : JWT vs jeton opaque ?** R : Vérifiable localement sans appel mais difficile à révoquer vs révocable immédiatement mais nécessitant une introspection.

**Q : Où stocker un jeton dans un navigateur ?** R : En cookie HttpOnly, Secure, SameSite plutôt qu'en localStorage, ou côté serveur via un BFF.

**Q : Rôle de HttpOnly, Secure et SameSite ?** R : Inaccessible au JavaScript / envoyé uniquement en HTTPS / limite l'envoi cross-site.

**Q : Quand une requête déclenche-t-elle un preflight CORS ?** R : Quand elle n'est pas "simple" : méthode autre que GET/POST/HEAD, en-têtes personnalisés, JSON.

**Q : Peut-on combiner Allow-Origin * et Allow-Credentials ?** R : Non, le navigateur le refuse ; il faut une origine explicite.

**Q : Token bucket vs fixed window ?** R : Rafales contrôlées et débit moyen garanti vs compteur par fenêtre, avec des pics aux frontières.

**Q : Comment comparer deux secrets sans fuite temporelle ?** R : Avec une comparaison en temps constant (MessageDigest.isEqual).

### Questions de situation

**Q : "Comment tu sécurises une API REST ?"**
R : "Par couches.
**Authentification** : un mécanisme standard (JWT ou OAuth2/OIDC) plutôt qu'une solution maison. En stateless, un jeton signé à durée de vie courte avec refresh token.
**Autorisation**, et c'est le point critique : vérifiée **côté serveur sur chaque endpoint et pour chaque ressource**, pas seulement masquée dans l'interface. La faille la plus fréquente de l'OWASP est le contrôle d'accès défaillant : un utilisateur qui change un identifiant dans l'URL et lit les données d'un autre. Donc je vérifie systématiquement que l'utilisateur authentifié a le droit d'accéder à **cette ressource précise**.
**Validation des entrées** : tout ce qui vient du client est suspect, type, format, bornes. Et des requêtes paramétrées côté base.
**Transport** : HTTPS partout, sans exception.
**Limitation** : rate limiting sur les endpoints sensibles, taille de page bornée côté serveur.
**Exposition** : aucune stacktrace en production ; détail dans les logs, message générique au client.
**Secrets** : dans un vault, jamais dans le code ni dans une image.
Et en continu : scanner les dépendances, parce qu'une part importante des compromissions passe par une librairie vulnérable et non par le code maison."

**Q : "Comment gérer l'authentification entre microservices ?"**
R : "Deux besoins distincts.
L'**identité de l'utilisateur final** doit être propagée dans la chaîne, généralement par un JWT transmis de service en service. Chaque service vérifie la signature localement, sans appeler un serveur d'authentification, pas de point de contention. Le service terminal sait ainsi pour qui il travaille et applique ses règles d'autorisation.
L'**identité du service appelant** est un autre problème : comment le service B sait-il que l'appel vient bien de A et non d'un attaquant présent sur le réseau ? La réponse propre est le **mTLS** : chaque service porte un certificat, l'authentification est mutuelle. Un service mesh comme Istio le fournit de façon transparente, sans le coder dans chaque application.
Le principe de fond est le **zero trust** : ne pas considérer le réseau interne comme sûr.
Et le **moindre privilège** : des comptes de service distincts avec des droits limités, plutôt qu'un compte partagé tout-puissant."

**Q : "Une fonctionnalité permet à l'utilisateur de fournir l'URL d'une image que ton serveur télécharge."**
R : "C'est le cas d'école du **SSRF**. Sans précautions, un attaquant fournit `http://169.254.169.254/...` (les métadonnées du cloud, qui peuvent délivrer des identifiants), `http://localhost:8080/actuator`, ou l'adresse d'un service interne.
Défenses : n'autoriser que `https`, résoudre le nom et **refuser les adresses privées, locales et de lien local** (y compris après redirection, donc soit désactiver les redirections, soit revalider à chaque saut, et se méfier du *DNS rebinding*), une liste blanche de domaines si c'est possible, des limites de taille, de durée et de type de contenu. Idéalement, exécuter les téléchargements depuis un composant isolé sans accès au réseau interne, et activer IMDSv2 côté AWS."

**Q : "Comment tu stockerais des mots de passe utilisateurs ?"**
R : "Jamais en clair, jamais chiffrés de façon réversible, jamais avec un hachage rapide comme SHA-256 seul. Un algorithme **lent, salé et paramétrable** : argon2id ou bcrypt (avec un facteur de coût ajusté pour environ 100 ms à la vérification), scrypt ou PBKDF2 si une conformité l'impose. Le sel unique par mot de passe est généré par l'algorithme et stocké avec le hash.
En Spring, un `DelegatingPasswordEncoder`, qui préfixe l'algorithme et permet de migrer progressivement : à la connexion d'un utilisateur dont le hash utilise un ancien algorithme, on re-hache avec le nouveau.
Autour : limitation des tentatives, vérification contre les listes de mots de passe compromis, MFA, et de préférence déléguer l'authentification à un fournisseur d'identité (Keycloak, OIDC) plutôt que de la gérer soi-même."

═══════════════════════════════════════════
# 36. RÉSEAU & LINUX
═══════════════════════════════════════════

### Vue d'ensemble

Sur un poste DevOps, on attend une compréhension du **chemin complet** d'une requête : du navigateur au processus applicatif. Sans cette vision, beaucoup d'incidents restent inexplicables, un timeout n'a pas la même cause selon qu'il vient du DNS, de la poignée de main TLS, d'un pare-feu ou de l'application elle-même.

Côté Linux, il ne s'agit pas d'être administrateur système, mais d'être **autonome pour diagnostiquer** : voir ce qui tourne, ce qui écoute, ce qui consomme, et lire les logs.

### Cours

**Le chemin d'une requête** : résolution **DNS** (nom → IP) → établissement **TCP** (poignée de main en trois temps) → négociation **TLS** si HTTPS → requête **HTTP** → réponse.

**TCP vs UDP** : TCP garantit l'ordre et la livraison, avec contrôle de congestion, c'est la base du web. UDP n'offre aucune garantie mais est plus léger, utilisé pour le DNS, la vidéo, les jeux.

**HTTP** : `1.1` (une requête à la fois par connexion, d'où le keep-alive et les connexions multiples), `2` (multiplexage sur une seule connexion, compression des en-têtes), `3` (basé sur QUIC/UDP, meilleure résistance à la perte de paquets).

**En-têtes utiles** : `Content-Type`, `Authorization`, `Cache-Control`, `ETag` (validation de cache), `X-Forwarded-For` (IP d'origine derrière un proxy).

**TLS** : chiffre le transport et authentifie le serveur via un certificat signé par une autorité. **mTLS** ajoute l'authentification du client. Erreurs courantes : certificat expiré, nom qui ne correspond pas, chaîne de certification incomplète.

**CORS** : mécanisme du navigateur empêchant une page d'un domaine d'appeler une API d'un autre domaine sans autorisation explicite. C'est une protection **côté navigateur** : elle n'existe pas pour un appel serveur à serveur, ce qui explique qu'un `curl` fonctionne là où le navigateur refuse.

**Load balancing** : réparti selon différentes stratégies (tourniquet, moins de connexions, hachage d'IP). Peut être en **couche 4** (TCP, rapide, aveugle au contenu) ou **couche 7** (HTTP, capable de router selon le chemin ou les en-têtes).

**Proxy vs reverse proxy** : le premier agit pour le client, le second devant les serveurs (terminaison TLS, répartition, cache).

**Commandes Linux essentielles :**
```bash
ps aux | grep java          # processus en cours
top / htop                  # consommation temps réel
df -h / du -sh *            # espace disque
free -h                     # mémoire
netstat -tulpn / ss -tulpn  # ports en écoute
curl -v https://...         # requête HTTP détaillée
dig / nslookup              # résolution DNS
tail -f fichier.log         # suivre un log en direct
grep -r "erreur" .          # rechercher
journalctl -u service       # logs systemd
kill -15 PID                # arrêt propre (SIGTERM)
```

**Permissions** : `rwx` pour propriétaire/groupe/autres, `chmod`, `chown`. Sur un conteneur non-root, un problème d'écriture est presque toujours une histoire de permissions.

**Diagnostic Java sur un serveur** : `jps` (processus JVM), `jstack` (thread dump), `jmap` (heap dump), `jstat` (statistiques GC).

**Modèle OSI** (7 couches) et modèle TCP/IP :
| Couche OSI | Exemples |
|---|---|
| 7 Application | HTTP, DNS, SMTP, gRPC |
| 6 Présentation | encodage, chiffrement (TLS est souvent placé entre 4 et 7) |
| 5 Session | gestion des sessions |
| 4 Transport | TCP, UDP, QUIC |
| 3 Réseau | IP, ICMP (ping), routage |
| 2 Liaison | Ethernet, MAC, switchs |
| 1 Physique | câbles, Wi-Fi |

**Adressage IP** : IPv4 sur 32 bits, notation **CIDR** (`10.0.0.0/24` = 256 adresses, `/16` = 65 536), plages **privées** `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, `127.0.0.1` (boucle locale), `0.0.0.0` (toutes les interfaces : une application qui écoute sur `127.0.0.1` dans un conteneur n'est pas joignable de l'extérieur, il faut écouter sur `0.0.0.0`). **NAT** : traduction d'adresses privées vers une adresse publique. IPv6 sur 128 bits. Ports : 0-1023 réservés (22 SSH, 53 DNS, 80 HTTP, 443 HTTPS, 5432 PostgreSQL, 3306 MySQL, 6379 Redis, 9092 Kafka).

**TCP, compléments** : poignée de main SYN → SYN-ACK → ACK ; fermeture par FIN ; l'état **TIME_WAIT** retient le port quelque temps après fermeture (beaucoup de connexions courtes épuisent les ports, d'où l'intérêt du keep-alive et des pools de connexions) ; *connection refused* (rien n'écoute sur le port, réponse RST immédiate) vs *connection timeout* (aucune réponse, souvent un pare-feu qui ignore les paquets) vs *read timeout* (connecté, mais la réponse tarde).

**DNS** : enregistrements **A** (nom → IPv4), **AAAA** (IPv6), **CNAME** (alias vers un autre nom), **MX** (mail), **TXT** (vérifications, SPF), **NS**, **SRV**. **TTL** : durée de mise en cache ; un changement DNS met jusqu'au TTL à se propager. La JVM met elle-même en cache les résolutions (`networkaddress.cache.ttl`), ce qui a longtemps posé problème quand une IP changeait.

**Temps réel et HTTP** : HTTP/1.1 keep-alive (connexion réutilisée), *head-of-line blocking* (une requête lente bloque les suivantes sur la connexion), HTTP/2 multiplexage, HTTP/3 sur QUIC. WebSocket : passage du protocole via `Upgrade`.

**Linux, compléments** :
- **Processus et signaux** : `SIGTERM` (15, demande d'arrêt, interceptable), `SIGKILL` (9, arrêt immédiat, non interceptable), `SIGINT` (2, Ctrl+C), `SIGHUP` (1, souvent rechargement de configuration), `SIGQUIT` (3, pour une JVM : produit un **thread dump** sur la sortie standard, avec `kill -3 <pid>`). Processus **zombie** (terminé mais pas encore récupéré par son parent), PID 1 dans un conteneur.
- **Descripteurs de fichiers** : chaque socket et fichier ouvert en consomme un ; limite par processus (`ulimit -n`) ; erreur `Too many open files` = fuite de ressources non fermées ou limite trop basse. `lsof -p <pid>` liste les fichiers et sockets ouverts.
- **Mémoire** : `free -h` (la colonne *available* compte, le cache disque est récupérable), **OOM killer** du noyau qui tue un processus quand la mémoire manque (visible dans `dmesg`), swap (désastreux pour une JVM, le GC parcourant des pages swappées).
- **Charge** : *load average* (`uptime`, `top`) = nombre moyen de processus prêts ou en attente d'I/O, à comparer au nombre de cœurs. `vmstat 1`, `iostat -x 1` (attente disque), `top -H -p <pid>` (threads d'un processus).
- **Texte** : `grep -i -r -n`, `less`, `tail -f`, `head`, `wc -l`, `sort | uniq -c | sort -rn` (compter les occurrences), `awk '{print $9}'` (une colonne), `sed 's/a/b/g'`, `cut`, `find . -name "*.log" -mtime +7 -delete`, `xargs`, `jq` (JSON).
- **Réseau** : `ss -tulpn`, `curl -v`/`-I`/`-w` (mesures de temps), `nc -zv hote port` (tester un port), `dig`/`nslookup`, `ip a`, `ping`, `traceroute`, `tcpdump`.
- **Permissions** : `chmod 755` (rwx pour le propriétaire, r-x pour groupe et autres ; r=4, w=2, x=1), `chown user:groupe`, `sudo`, umask.
- **Services** : `systemctl start|stop|status|enable service`, `journalctl -u service -f`, `cron` (`crontab -e`, format `minute heure jour mois jour_semaine`).
- **Distant** : `ssh user@hote`, clés SSH (`ssh-keygen`, `authorized_keys`), `scp`, `rsync`, tunnel `ssh -L 5432:db:5432 bastion`.
- **Variables d'environnement** : `export VAR=x`, `env`, `printenv`, `.bashrc` ; redirections `>` (écraser), `>>` (ajouter), `2>&1` (stderr vers stdout), `|` (pipe), `&` (arrière-plan), `nohup`.

### En situation

- **"L'appel échoue en timeout"** → DNS, TCP, TLS, application ?
- **"Ça marche en curl mais pas dans le navigateur"** → CORS.
- **"Le disque est plein"** → df, du, rotation des logs.
- **"Le service ne répond plus"** → ps, ports en écoute, thread dump.

### Questions techniques

**Q : Étapes d'une requête HTTPS ?** R : DNS → TCP (handshake) → TLS → HTTP.

**Q : TCP vs UDP ?** R : Fiable et ordonné vs léger sans garantie.

**Q : Apport d'HTTP/2 ?** R : Multiplexage sur une connexion et compression des en-têtes.

**Q : C'est quoi le CORS ?** R : Une protection du navigateur contre les appels cross-origin non autorisés, inexistante en serveur à serveur.

**Q : Load balancing L4 vs L7 ?** R : Répartition TCP aveugle au contenu vs répartition HTTP capable de router selon le chemin.

**Q : Proxy vs reverse proxy ?** R : Agit pour le client vs se place devant les serveurs.

**Q : Comment voir les ports en écoute ?** R : `ss -tulpn` ou `netstat -tulpn`.

**Q : Comment prendre un thread dump ?** R : `jstack <pid>`.

**Q : Que fait `kill -15` ?** R : Envoie SIGTERM, permettant un arrêt propre, contrairement à `kill -9`.

**Q : Les 7 couches OSI ?** R : Physique, liaison, réseau, transport, session, présentation, application.

**Q : À quelle couche se situent TCP, IP et HTTP ?** R : Transport, réseau, application.

**Q : Combien d'adresses dans un /24 ?** R : 256.

**Q : Les plages d'adresses privées ?** R : 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16.

**Q : Pourquoi une application qui écoute sur 127.0.0.1 dans un conteneur est-elle injoignable ?** R : Elle n'écoute que sur la boucle locale du conteneur ; il faut 0.0.0.0.

**Q : Connection refused vs connection timeout ?** R : Rien n'écoute (rejet immédiat) vs aucune réponse (pare-feu, réseau, hôte injoignable).

**Q : C'est quoi TIME_WAIT ?** R : L'état d'une connexion fermée qui retient le port un moment ; trop de connexions courtes épuisent les ports.

**Q : Enregistrement A vs CNAME ?** R : Nom vers adresse IP vs alias vers un autre nom.

**Q : Pourquoi un changement DNS n'est-il pas immédiat ?** R : Les résolutions sont mises en cache pendant le TTL (y compris par la JVM).

**Q : SIGTERM vs SIGKILL ?** R : Demande d'arrêt interceptable vs arrêt immédiat non interceptable.

**Q : Que fait kill -3 sur une JVM ?** R : Il produit un thread dump sur la sortie standard sans arrêter le processus.

**Q : Que signifie "Too many open files" ?** R : La limite de descripteurs est atteinte, souvent à cause de ressources non fermées.

**Q : Comment voir les fichiers et sockets ouverts par un processus ?** R : lsof -p <pid>.

**Q : Comment interpréter le load average ?** R : Nombre moyen de processus actifs ou en attente d'I/O, à comparer au nombre de cœurs.

**Q : Que signifie chmod 644 ?** R : Lecture/écriture pour le propriétaire, lecture pour le groupe et les autres.

**Q : Comment compter les adresses IP les plus fréquentes dans un log d'accès ?** R : `awk '{print $1}' access.log | sort | uniq -c | sort -rn | head`.

**Q : Comment tester qu'un port distant est ouvert ?** R : `nc -zv hote port` ou `curl -v telnet://hote:port`.

**Q : Que fait 2>&1 ?** R : Redirige la sortie d'erreur vers la sortie standard.

### Questions de situation

**Q : "Un appel vers un service externe échoue en timeout. Comment tu remontes la chaîne ?"**
R : "Je testerais couche par couche, du bas vers le haut.
D'abord la **résolution DNS** avec `dig` : le nom se résout-il, et vers la bonne adresse ? Dans un cluster, une erreur de nom de service est fréquente.
Ensuite la **connectivité TCP** : est-ce que le port est joignable ? Un échec ici oriente vers un pare-feu ou une NetworkPolicy.
Puis **TLS** avec `curl -v` : certificat expiré, nom non concordant, chaîne incomplète ?
Enfin la couche **HTTP** : le service répond-il, avec quel code, en combien de temps ?
Si tout passe en `curl` depuis le pod mais échoue dans l'application, je regarde du côté de la configuration : mauvais port, proxy non pris en compte, ou timeout applicatif plus court que le temps de réponse normal du service.
La démarche générale : isoler la couche fautive avant de chercher une cause, sinon on tourne en rond."

**Q : "Le disque du serveur est plein en production."**
R : "En urgence, identifier ce qui occupe : `df -h` pour voir quelle partition, puis `du -sh *` en descendant l'arborescence pour localiser le coupable.
Les causes classiques : des **logs non pivotés** qui grossissent indéfiniment, des fichiers temporaires jamais nettoyés, ou des images et conteneurs Docker accumulés, `docker system prune` libère souvent beaucoup.
Attention à un piège : supprimer un fichier encore ouvert par un processus ne libère pas l'espace tant que le processus tourne. Il faut le redémarrer, ou tronquer le fichier plutôt que le supprimer.
Sur le fond, la vraie correction est la **rotation des logs** (logrotate), une politique de rétention, et une alerte sur le taux de remplissage avant d'atteindre 100%, parce qu'un disque plein en production provoque des pannes en cascade difficiles à diagnostiquer."

**Q : "Ton service lève des 'Too many open files' après quelques heures."**
R : "Une croissance progressive évoque une **fuite** plutôt qu'une limite mal réglée. Je regarderais `lsof -p <pid> | wc -l` dans le temps, puis le type des descripteurs : des sockets vers un même service (des clients HTTP créés à chaque appel et jamais fermés, des réponses dont le corps n'est pas consommé), des fichiers (des streams `Files.lines` non fermés).
Correction : try-with-resources systématique, un client HTTP partagé avec un pool, fermeture des réponses. Et en parallèle, une limite `ulimit -n` adaptée à la charge normale, avec une métrique et une alerte sur le nombre de descripteurs ouverts."

═══════════════════════════════════════════
# 37. CLOUD & INFRASTRUCTURE AS CODE
═══════════════════════════════════════════

### Vue d'ensemble

Le cloud transforme l'infrastructure en **service à la demande** : au lieu d'acheter des serveurs, on loue des ressources (calcul, stockage, bases managées, files de messages) facturées à l'usage et créées en quelques minutes par API. Pour un développeur backend, l'enjeu n'est pas de connaître les centaines de services de chaque fournisseur, mais de comprendre les modèles, les briques principales et les compromis : ce qu'on gagne en délégant l'exploitation, ce qu'on perd en contrôle et en portabilité, et ce que ça coûte.

L'**Infrastructure as Code** (IaC) applique au cloud les pratiques du code : l'infrastructure est décrite dans des fichiers versionnés, revue en pull request, appliquée de façon reproductible. Fini les serveurs configurés à la main que personne ne sait reconstruire.

### Cours

**Modèles de service** :
| Modèle | On gère | Le fournisseur gère | Exemples |
|---|---|---|---|
| IaaS | OS, runtime, application | matériel, réseau, virtualisation | machines virtuelles (EC2, Compute Engine) |
| CaaS | conteneurs, application | orchestration | Kubernetes managé (EKS, AKS, GKE) |
| PaaS | application et données | tout le reste | App Service, Cloud Run, Heroku, OpenShift |
| FaaS (serverless) | des fonctions | exécution et scaling, jusqu'à zéro | Lambda, Azure Functions, Cloud Functions |
| SaaS | l'usage | tout | Gmail, Salesforce |

**Modèle de responsabilité partagée** : le fournisseur sécurise *le* cloud (datacenters, matériel, hyperviseur) ; le client sécurise ce qu'il met *dans* le cloud (configuration, identités, données, chiffrement, correctifs de ses machines). La plupart des fuites de données cloud viennent d'erreurs de configuration du client (un bucket public).

**Géographie** : **régions** (zones géographiques indépendantes, choisies pour la latence et la réglementation, souveraineté des données) découpées en **zones de disponibilité** (datacenters séparés avec alimentation et réseau indépendants). Une application hautement disponible se répartit sur plusieurs zones ; la tolérance à la perte d'une région demande une architecture multi-régions, bien plus coûteuse.

**Correspondance des services principaux** :
| Besoin | AWS | Azure | GCP |
|---|---|---|---|
| Machines virtuelles | EC2 | Virtual Machines | Compute Engine |
| Kubernetes managé | EKS | AKS | GKE |
| Conteneurs serverless | Fargate, App Runner | Container Apps | Cloud Run |
| Fonctions | Lambda | Functions | Cloud Functions |
| Stockage objet | S3 | Blob Storage | Cloud Storage |
| Base relationnelle managée | RDS, Aurora | Azure SQL, Database for PostgreSQL | Cloud SQL, AlloyDB |
| NoSQL | DynamoDB | Cosmos DB | Firestore, Bigtable |
| Cache | ElastiCache | Azure Cache for Redis | Memorystore |
| Files et événements | SQS, SNS, EventBridge | Service Bus, Event Grid | Pub/Sub |
| Kafka managé | MSK | Event Hubs (compatible Kafka) | Managed Service for Kafka |
| Identités et droits | IAM | Entra ID, RBAC | IAM |
| Secrets | Secrets Manager | Key Vault | Secret Manager |
| Monitoring | CloudWatch | Monitor | Cloud Monitoring |
| Registry | ECR | ACR | Artifact Registry |
| Réseau privé | VPC | VNet | VPC |
| Équilibreur | ALB (L7), NLB (L4) | Application Gateway, Load Balancer | Cloud Load Balancing |
| CDN | CloudFront | Front Door | Cloud CDN |

**Stockage objet** (S3 et équivalents) : des objets dans des *buckets*, accessibles par HTTP, durabilité très élevée, quasi illimité, peu cher, classes de stockage (fréquent, rare, archive) et règles de cycle de vie. Ce n'est pas un système de fichiers : pas de modification partielle d'un objet. URL **pré-signées** pour laisser un client télécharger ou déposer un fichier directement, sans passer par le backend.

**Réseau cloud** : un **VPC** (réseau privé virtuel) découpé en **sous-réseaux** publics (exposés via une passerelle internet) et privés (bases, services internes, sortie via une passerelle NAT) ; **groupes de sécurité** (pare-feu par ressource, avec état) ; *peering* et *private endpoints* pour accéder aux services managés sans passer par internet ; bastion ou accès par session managée plutôt que SSH ouvert.

**Identités** : rôles IAM attribués aux **ressources** (un pod, une fonction) plutôt que des clés d'accès longue durée dans la configuration ; en Kubernetes, fédération d'identité entre ServiceAccount et rôle cloud (IRSA / Pod Identity sur EKS, Workload Identity sur AKS/GKE). Principe du moindre privilège, politiques explicites.

**Serverless et Java** : Lambda facture à l'invocation et scale à zéro, mais le **démarrage à froid** d'une JVM classique pénalise la latence. Réponses : **SnapStart** (instantané de la JVM initialisée, restauré à l'invocation), compilation **native** GraalVM (Quarkus, Spring Native, Micronaut), frameworks légers, mémoire allouée plus élevée (qui augmente aussi le CPU). Limites : durée d'exécution maximale, pas d'état local, connexions aux bases à gérer (proxy de connexions).

**Coûts (FinOps)** : on paie le calcul au temps, le stockage au volume, et souvent le **trafic sortant** (*egress*), poste fréquemment sous-estimé. Leviers : dimensionnement juste (*right-sizing*), autoscaling, instances réservées ou engagements d'usage pour la charge de base, instances *spot*/préemptibles (très bon marché mais interruptibles) pour le travail tolérant aux interruptions, arrêt des environnements hors heures ouvrées, étiquetage (*tags*) pour attribuer les coûts par équipe.

**Infrastructure as Code** :
- **Terraform** (et son fork open source **OpenTofu**) : langage déclaratif HCL, multi-fournisseurs via des *providers*. Cycle : `terraform init` → `plan` (affiche le différentiel entre le code et la réalité) → `apply`. L'**état** (*state*) mémorise la correspondance entre le code et les ressources réelles : il doit être **distant** (bucket, Terraform Cloud), **verrouillé** pendant un apply (pour éviter deux modifications simultanées) et protégé (il contient des données sensibles). **Modules** réutilisables, *workspaces* ou répertoires par environnement, `terraform import` pour reprendre une ressource existante, détection de **drift** (une modification manuelle) au `plan`.
- **Outils spécifiques** : CloudFormation et CDK (AWS), Bicep (Azure), Pulumi (IaC dans des langages généralistes, y compris Java), Crossplane (infrastructure pilotée par des ressources Kubernetes).
- **Provisionnement vs gestion de configuration** : Terraform crée l'infrastructure ; **Ansible** (sans agent, via SSH, *playbooks* YAML **idempotents**) configure des machines existantes. Avec des conteneurs et des images immuables, la gestion de configuration de serveurs devient moins centrale.
- **Infrastructure immuable** : on ne modifie pas un serveur en place, on en déploie un nouveau à partir d'une image, et on détruit l'ancien. Pas de dérive de configuration, rollback simple.

**Stratégies de migration vers le cloud** (les "R") : *rehost* (lift and shift, déplacer tel quel), *replatform* (petits ajustements, par exemple passer à une base managée), *refactor* (réarchitecturer en cloud-native), *repurchase* (remplacer par un SaaS), *retire*, *retain*.

**Multi-cloud et dépendance** : utiliser des services managés propriétaires accélère mais crée une dépendance (*vendor lock-in*). Kubernetes, les bases open source managées, OpenTelemetry et Terraform limitent cette dépendance, au prix de ne pas profiter de certains services spécifiques. Le multi-cloud "pour la portabilité" a un coût élevé : à justifier par un besoin réel (réglementation, négociation, résilience).

### En situation

- **"Pourquoi une base managée plutôt qu'une base sur une VM ?"** → sauvegardes, correctifs, haute disponibilité, réplicas gérés.
- **"La facture cloud explose"** → trafic sortant, ressources surdimensionnées ou oubliées, absence de tags.
- **"Une clé d'accès AWS a été poussée sur GitHub"** → révoquer immédiatement, rôles IAM plutôt que clés.
- **"Quelqu'un a modifié l'infrastructure à la main"** → drift détecté au plan, réaligner le code.
- **"Lambda en Java trop lente au premier appel"** → démarrage à froid → SnapStart, natif.

### Questions techniques

**Q : IaaS vs PaaS vs SaaS vs FaaS ?** R : On gère l'OS et au-dessus / l'application seule / on utilise un logiciel / on fournit des fonctions exécutées à la demande.

**Q : C'est quoi le modèle de responsabilité partagée ?** R : Le fournisseur sécurise l'infrastructure ; le client sécurise ses configurations, identités et données.

**Q : Région vs zone de disponibilité ?** R : Zone géographique indépendante vs datacenter isolé à l'intérieur d'une région.

**Q : Comment rendre un service résilient à la perte d'un datacenter ?** R : Le répartir sur plusieurs zones de disponibilité, avec des données répliquées.

**Q : C'est quoi une URL pré-signée ?** R : Une URL temporaire qui autorise un accès direct à un objet du stockage sans exposer d'identifiants.

**Q : Sous-réseau public vs privé ?** R : Accessible depuis internet via une passerelle vs sans exposition directe, sortie via NAT.

**Q : Pourquoi préférer des rôles IAM à des clés d'accès ?** R : Des identifiants temporaires attribués à la ressource, sans secret longue durée à stocker ni à faire fuiter.

**Q : Comment réduire le démarrage à froid d'une Lambda Java ?** R : SnapStart, compilation native, frameworks légers, plus de mémoire allouée.

**Q : Quel poste de coût est souvent sous-estimé ?** R : Le trafic réseau sortant.

**Q : Instances spot, pour quoi faire ?** R : Des charges tolérantes aux interruptions (batch, CI), à coût très réduit.

**Q : C'est quoi l'Infrastructure as Code ?** R : Décrire l'infrastructure dans du code versionné, revu et appliqué automatiquement.

**Q : Les commandes principales de Terraform ?** R : init, plan, apply (et destroy, import).

**Q : À quoi sert le state Terraform ?** R : À mémoriser la correspondance entre le code et les ressources réelles ; il doit être distant, verrouillé et protégé.

**Q : C'est quoi le drift ?** R : Un écart entre l'infrastructure réelle et sa description en code, souvent dû à une modification manuelle.

**Q : Terraform vs Ansible ?** R : Provisionner l'infrastructure (déclaratif, état) vs configurer des machines (playbooks idempotents, sans agent).

**Q : C'est quoi l'infrastructure immuable ?** R : Remplacer les serveurs au lieu de les modifier, à partir d'images versionnées.

**Q : Rehost vs refactor ?** R : Déplacer tel quel vs réarchitecturer pour le cloud.

**Q : Avantage et inconvénient des services managés propriétaires ?** R : Rapidité et exploitation déléguée vs dépendance au fournisseur.

### Questions de situation

**Q : "Tu dois déployer une application Spring Boot avec une base PostgreSQL sur AWS. Quelle architecture ?"**
R : "Un VPC sur deux ou trois zones de disponibilité, avec sous-réseaux publics pour l'équilibreur et privés pour l'application et la base. L'application en conteneurs, sur **ECS Fargate** si l'équipe veut éviter Kubernetes, ou **EKS** si elle a déjà la compétence et plusieurs services. Un **ALB** devant, avec HTTPS et un certificat géré.
La base sur **RDS PostgreSQL** en multi-AZ (bascule automatique), avec sauvegardes et restauration à un instant donné ; un réplica de lecture si nécessaire. Les secrets dans **Secrets Manager**, lus par l'application via son rôle IAM, sans clé dans la configuration. Logs et métriques vers CloudWatch ou un collecteur OpenTelemetry.
Le tout décrit en **Terraform**, avec un state distant verrouillé, et déployé par la CI. Je commencerais petit et ajusterais la taille à partir des mesures réelles plutôt que de surdimensionner."

**Q : "Lors d'un terraform plan, on voit qu'une ressource de production va être détruite puis recréée."**
R : "Je n'appliquerais pas avant d'avoir compris. Le plan l'indique avec le symbole `-/+` et l'attribut responsable (*forces replacement*) : souvent un attribut qui ne peut pas être modifié en place (un nom, un sous-réseau, un type de moteur), ou un renommage de ressource dans le code que Terraform interprète comme une suppression suivie d'une création.
Selon le cas : pour un renommage, un bloc `moved` (ou `terraform state mv`) qui dit à Terraform que c'est la même ressource ; pour un attribut immuable, planifier le remplacement avec migration de données ; et protéger les ressources critiques avec `prevent_destroy`. Le plan est revu en pull request précisément pour attraper ce genre de surprise."

═══════════════════════════════════════════
# 38. MESSAGING & KAFKA
═══════════════════════════════════════════

### Vue d'ensemble

La communication synchrone crée un **couplage temporel** : si le service appelé est indisponible, l'appelant l'est aussi. Le messaging brise ce couplage, l'émetteur dépose un message et poursuit son travail ; le consommateur le traite quand il peut. On gagne en résilience, en absorption des pics, et en capacité à ajouter des consommateurs sans toucher au producteur.

Le prix à payer est réel : le flux global devient plus difficile à suivre, la cohérence devient différée, et il faut gérer des cas nouveaux, un message livré deux fois, un message dans le désordre, un message qu'aucun consommateur n'arrive à traiter.

Deux familles d'outils. Les **brokers de messages** classiques (RabbitMQ) distribuent des messages qui disparaissent une fois consommés. **Kafka** fonctionne différemment : c'est un **journal ordonné et persistant** que les consommateurs lisent à leur rythme, chacun gardant sa position. Cette différence explique la plupart de ses propriétés, plusieurs consommateurs indépendants, possibilité de rejouer l'historique, très haut débit.

### Cours

**Concepts Kafka :**
- **Topic** : un flux nommé de messages.
- **Partition** : un topic est découpé en partitions, unité de parallélisme et d'ordre. **L'ordre n'est garanti qu'à l'intérieur d'une partition**, pas globalement.
- **Clé de partitionnement** : les messages de même clé vont dans la même partition, c'est ainsi qu'on garantit l'ordre par entité (tous les événements d'une commande donnée restent ordonnés entre eux).
- **Offset** : la position d'un message dans une partition. Le consommateur mémorise son offset, ce qui permet de reprendre où il en était.
- **Consumer group** : un groupe se répartit les partitions ; chaque partition n'est lue que par un consommateur du groupe. Ajouter des consommateurs au-delà du nombre de partitions ne sert à rien.
- **Réplication** : chaque partition est répliquée sur plusieurs brokers pour la tolérance aux pannes.
- **Rétention** : les messages sont conservés selon une durée ou une taille, indépendamment de leur consommation, d'où la possibilité de rejouer.

**Garanties de livraison :**
- **At most once** : au plus une fois, on peut perdre.
- **At least once** : au moins une fois, on peut recevoir des doublons, le mode courant.
- **Exactly once** : possible dans Kafka sous conditions (transactions), mais coûteux et limité.

En pratique on travaille en *at least once*, et on rend les **consommateurs idempotents** : traiter deux fois le même message doit produire le même résultat. C'est l'exigence la plus importante du messaging.

**Dead Letter Queue** : les messages qu'on n'arrive pas à traiter après plusieurs tentatives sont mis de côté plutôt que de bloquer indéfiniment la partition, et analysés séparément.

**Consumer lag** : l'écart entre le dernier message produit et celui traité. C'est **la métrique clé** : un lag qui croît signifie que la consommation ne suit pas le rythme de production.

**Pattern Outbox** : pour publier de façon fiable, on écrit l'événement dans une table de la base **dans la même transaction** que la donnée métier, puis un processus séparé le publie. Sans ça, on peut committer en base et perdre le message (ou l'inverse).

**RabbitMQ** en comparaison : routage plus riche (échanges, files, clés de routage), messages supprimés après consommation, adapté à de la distribution de tâches ; Kafka est adapté aux flux d'événements à fort volume avec rejeu.

**Architecture, KRaft** : historiquement, Kafka dépendait de **ZooKeeper** pour ses métadonnées et l'élection du contrôleur. Le mode **KRaft** (consensus Raft intégré) le remplace, et **Kafka 4.0 (2025) a supprimé ZooKeeper** : un cluster ne compte plus que des brokers et des contrôleurs Kafka.

**Réplication, détail** : chaque partition a un **leader** (qui reçoit écritures et lectures par défaut) et des **followers** ; l'**ISR** (*in-sync replicas*) est l'ensemble des réplicas à jour. Si le leader tombe, un réplica de l'ISR est élu.

**Producteur, les réglages qui comptent** :
- `acks=0` (n'attend rien, pertes possibles), `acks=1` (le leader a écrit, perte possible si le leader meurt avant réplication), `acks=all` (tous les réplicas ISR ont écrit, avec `min.insync.replicas=2` côté topic pour une vraie durabilité). Défaut `all` depuis Kafka 3.0.
- **Producteur idempotent** (`enable.idempotence=true`, défaut depuis 3.0) : évite les doublons et les inversions d'ordre causés par les retries du producteur.
- Débit : `batch.size`, `linger.ms` (attendre un peu pour remplir les lots), `compression.type` (lz4, zstd).
- **Transactions** (`transactional.id`) : écrire atomiquement dans plusieurs partitions et committer les offsets dans la même transaction, base de l'*exactly once* dans un pipeline lecture-traitement-écriture Kafka. Les consommateurs lisent alors en `isolation.level=read_committed`. L'exactly once ne s'étend pas aux effets externes (une écriture en base, un email) : là, il faut l'idempotence.
- Partitionnement : par hachage de la clé ; sans clé, répartition par lots entre partitions (*sticky partitioner*).

**Consommateur, les réglages qui comptent** :
- **Commit des offsets** : automatique (`enable.auto.commit=true`, périodique, risque de perdre ou de rejouer des messages en cas de crash) ou manuel **après traitement** (`commitSync`/`commitAsync`, ou `AckMode` en Spring Kafka), qui donne du *at least once*.
- `auto.offset.reset` : `earliest` ou `latest`, pour un groupe qui n'a pas encore d'offset.
- `max.poll.records`, `max.poll.interval.ms` : si le traitement d'un lot dépasse cet intervalle, le consommateur est considéré comme mort et exclu du groupe, d'où un **rebalancing** et le retraitement des messages. Cause fréquente de doublons en boucle.
- **Rebalancing** : redistribution des partitions quand un consommateur rejoint ou quitte le groupe ; le protocole **coopératif** (incrémental) évite d'arrêter tout le groupe ; `group.instance.id` (membre statique) évite un rebalancing lors d'un simple redémarrage.

**Rétention vs compaction** : la rétention supprime par **durée** ou **taille** ; la **compaction** (`cleanup.policy=compact`) garde au moins le **dernier message de chaque clé**, ce qui fait du topic une table d'état reconstituable (un message à valeur nulle, *tombstone*, supprime la clé).

**Ordre et retries côté consommateur** : réessayer un message en échec en bloquant la partition préserve l'ordre mais arrête tout ; envoyer vers un **topic de retry** (avec délai) puis une DLQ ne bloque pas, mais casse l'ordre pour la clé concernée. Choix à faire selon le besoin métier (Spring Kafka fournit `@RetryableTopic` et `DefaultErrorHandler`).

**Schémas** : **Schema Registry** (Confluent, Apicurio) avec Avro, Protobuf ou JSON Schema ; règles de compatibilité (*backward* : un nouveau consommateur lit les anciens messages ; *forward* ; *full*), qui empêchent un producteur de publier un message que les consommateurs ne sauraient pas lire.

**Écosystème** : **Kafka Connect** (connecteurs source et puits sans code : base de données, S3, Elasticsearch), **Debezium** (CDC des bases vers Kafka), **Kafka Streams** (bibliothèque Java de traitement de flux : `KStream`, `KTable`, jointures, fenêtres, état local), Flink (traitement de flux distribué), ksqlDB. Côté Java : client `kafka-clients`, **Spring Kafka** (`KafkaTemplate`, `@KafkaListener`, gestion des erreurs, transactions), SmallRye Reactive Messaging (Quarkus). **Files de type "queue"** : Kafka 4.0 a introduit en accès anticipé les *share groups* (KIP-932), où plusieurs consommateurs peuvent traiter les messages d'une même partition avec acquittement individuel.

**RabbitMQ, détail** : le producteur publie vers un **exchange**, qui route vers des **queues** selon des **bindings** : `direct` (clé de routage exacte), `topic` (motifs avec jokers `*` et `#`), `fanout` (diffusion à toutes les queues liées), `headers`. Acquittements manuels (`ack`/`nack`), *prefetch* pour la répartition équitable, files durables et messages persistants, *dead letter exchange*, TTL, files à priorité, *quorum queues* (répliquées, recommandées), *streams* (journal rejouable proche de Kafka). Protocole AMQP 0-9-1.

**Autres** : JMS (API standard Java, ActiveMQ Artemis), Apache Pulsar (séparation calcul/stockage, multi-tenant), NATS, files managées du cloud (SQS, Pub/Sub, Service Bus).

### En situation

- **"Sync ou async ?"** → selon le besoin de réponse immédiate.
- **"Comment garantir qu'un message n'est pas perdu ?"** → outbox, accusés, réplication.
- **"Le consommateur prend du retard"** → lag, partitions, parallélisme.
- **"Un message est traité deux fois"** → idempotence.

### Questions techniques

**Q : Kafka vs RabbitMQ ?** R : Kafka est un journal persistant relu à la demande ; RabbitMQ distribue des messages consommés puis supprimés.

**Q : Où l'ordre est-il garanti dans Kafka ?** R : À l'intérieur d'une partition seulement.

**Q : À quoi sert la clé de partitionnement ?** R : À envoyer les messages de même clé dans la même partition, donc à garantir l'ordre par entité.

**Q : C'est quoi un consumer group ?** R : Un ensemble de consommateurs se répartissant les partitions d'un topic.

**Q : Combien de consommateurs utiles par topic ?** R : Au maximum autant que de partitions ; au-delà, ils restent inactifs.

**Q : Les garanties de livraison ?** R : At most once, at least once, exactly once. En pratique at least once + idempotence.

**Q : C'est quoi le consumer lag ?** R : L'écart entre le dernier message produit et le dernier traité, la métrique de santé principale.

**Q : À quoi sert une DLQ ?** R : Isoler les messages non traitables pour ne pas bloquer le flux.

**Q : Le pattern Outbox ?** R : Écrire l'événement en base dans la même transaction que la donnée, puis le publier, pour ne jamais perdre un message.

**Q : Kafka a-t-il encore besoin de ZooKeeper ?** R : Non, Kafka 4.0 l'a supprimé au profit du mode KRaft.

**Q : C'est quoi l'ISR ?** R : L'ensemble des réplicas d'une partition à jour avec le leader.

**Q : acks=0, 1, all ?** R : Aucune attente / écriture sur le leader / écriture sur tous les réplicas ISR.

**Q : Rôle de min.insync.replicas ?** R : Nombre minimal de réplicas à jour pour accepter une écriture en acks=all.

**Q : Que garantit le producteur idempotent ?** R : Pas de doublon ni d'inversion d'ordre dus aux retries du producteur (par partition).

**Q : Kafka garantit-il l'exactly once vers une base de données ?** R : Non, seulement au sein de Kafka (transactions) ; vers l'extérieur il faut un consommateur idempotent.

**Q : Auto-commit vs commit manuel ?** R : Périodique, avec perte ou rejeu possible vs après traitement, pour du at least once maîtrisé.

**Q : Pourquoi un consommateur retraite-t-il les mêmes messages en boucle ?** R : Souvent un traitement plus long que max.poll.interval.ms : exclusion, rebalancing et rejeu.

**Q : C'est quoi un rebalancing ?** R : La redistribution des partitions entre les consommateurs d'un groupe.

**Q : Rétention vs compaction ?** R : Suppression par âge ou taille vs conservation du dernier message de chaque clé.

**Q : C'est quoi un tombstone ?** R : Un message à valeur nulle qui supprime une clé d'un topic compacté.

**Q : À quoi sert un Schema Registry ?** R : Stocker les schémas des messages et vérifier la compatibilité de leurs évolutions.

**Q : Kafka Connect vs Kafka Streams ?** R : Connecteurs d'intégration sans code vs bibliothèque Java de traitement de flux.

**Q : Les types d'exchange RabbitMQ ?** R : direct, topic, fanout, headers.

**Q : Retry bloquant vs topic de retry ?** R : Préserve l'ordre mais bloque la partition vs ne bloque pas mais casse l'ordre pour la clé.

### Questions de situation

**Q : "Comment tu choisis entre synchrone et asynchrone entre deux services ?"**
R : "Je pars du besoin métier : l'appelant a-t-il **besoin de la réponse pour continuer** ?
Si oui (vérifier un solde avant de valider une commande) c'est synchrone, en REST ou gRPC. Simple à comprendre et à débugger, mais couplage temporel : si l'appelé tombe, l'appelant est affecté. D'où timeouts et circuit breaker obligatoires.
Si non (envoyer une confirmation, indexer, générer un rapport) c'est asynchrone. Le consommateur peut être indisponible sans impacter l'appelant, et on absorbe mieux les pics.
Le coût de l'asynchrone : flux global plus difficile à suivre, ordre à gérer, idempotence des consommateurs obligatoire, et on ne sait pas immédiatement si le traitement a réussi.
Donc je ne le choisis pas par défaut, seulement quand le découplage apporte une valeur réelle."

**Q : "Un consommateur accumule du retard sur un topic."**
R : "Je regarderais d'abord le **consumer lag** pour mesurer l'ampleur et la tendance, stable, croissant, en pic ?
Ensuite la cause. Si le traitement par message est trop lent, j'optimiserais ce traitement, souvent un appel base ou réseau non batché. Si c'est un problème de débit, j'augmenterais le **parallélisme** : mais le plafond est le nombre de **partitions** : ajouter des consommateurs au-delà ne sert à rien, il faut alors repartitionner le topic.
Je vérifierais aussi qu'un message empoisonné ne bloque pas la partition en échouant en boucle, c'est exactement le rôle d'une **DLQ**.
Et je m'assurerais que le lag est **surveillé avec alerte**, parce qu'un retard qui grossit finit par dépasser la rétention, et là on perd des messages définitivement."

**Q : "Comment tu garantis qu'un événement de commande est traité une et une seule fois par le service de facturation ?"**
R : "Exactement une fois de bout en bout n'existe pas gratuitement ; je viserais **au moins une fois + idempotence**, ce qui produit l'effet d'exactement une fois.
Côté production : outbox pour ne jamais perdre l'événement, producteur idempotent et `acks=all`. Côté consommation : commit des offsets **après** traitement. Et l'idempotence : chaque événement porte un identifiant unique ; dans la **même transaction** que la création de la facture, j'enregistre cet identifiant dans une table des événements traités avec une contrainte unique. Un événement rejoué viole la contrainte et est ignoré. La clé de partitionnement est l'identifiant de commande, pour que tous les événements d'une commande restent ordonnés."

═══════════════════════════════════════════
# 39. CACHE
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

═══════════════════════════════════════════
# 40. ALGORITHMES & COMPLEXITÉ
═══════════════════════════════════════════

### Vue d'ensemble

En entretien backend, on teste rarement des algorithmes exotiques. Ce qu'on veut vérifier, c'est que tu sais **raisonner en complexité** (reconnaître qu'une boucle imbriquée sur un gros volume est un problème) et que tu connais les quelques patterns qui résolvent 80% des exercices courants.

La complexité prime sur la micro-optimisation : passer de O(n²) à O(n) sur 100 000 éléments gagne un facteur plusieurs milliers, là où optimiser des constantes gagne quelques pourcents.

### Cours

**Big O** : évolution du coût selon la taille d'entrée, dans le pire cas :
O(1) < O(log n) < O(n) < O(n log n) < O(n²) < O(2ⁿ) < O(n!)

**Complexités usuelles :**
| Structure | Accès | Recherche | Insertion | Suppression |
|---|---|---|---|---|
| Tableau / ArrayList | O(1) | O(n) | O(n) | O(n) |
| Liste chaînée | O(n) | O(n) | O(1)* | O(1)* |
| HashMap / HashSet |, | O(1) | O(1) | O(1) |
| TreeMap / TreeSet |, | O(log n) | O(log n) | O(log n) |
| Tas (heap) |, | O(n) | O(log n) | O(log n) |

\* si la position est déjà connue.

**Tris** : Java utilise **TimSort** pour les objets (fusion de merge sort et insertion sort, O(n log n), stable) et un quicksort à double pivot pour les primitifs.

**Parcours de graphes/arbres** : **BFS** (en largeur, avec une file, trouve le plus court chemin en nombre d'arêtes) et **DFS** (en profondeur, avec une pile ou la récursion, exploration exhaustive, détection de cycles).

**Patterns de résolution récurrents :**
- **HashMap pour la recherche** : transformer une recherche O(n) en O(1). Résout "two sum", détection de doublons, comptage d'occurrences.
- **Two pointers** : deux index sur une structure triée, souvent pour trouver une paire ou inverser en place. O(n).
- **Sliding window** : fenêtre glissante pour les problèmes de sous-séquence contiguë (plus longue sous-chaîne sans répétition, somme maximale). Évite de tout recalculer.
- **Programmation dynamique** : décomposer en sous-problèmes qui se chevauchent et mémoriser les résultats. Fibonacci, sac à dos, plus longue sous-séquence commune.
- **Récursion + backtracking** : explorer des combinaisons en revenant en arrière (permutations, sudoku).

**Réflexes en live coding :**
1. **Reformuler** l'énoncé pour vérifier la compréhension.
2. **Demander les contraintes** : taille des données, valeurs possibles, cas limites.
3. **Proposer une solution naïve** d'abord, puis l'optimiser, mieux vaut une solution qui marche qu'une élégante inachevée.
4. **Penser à voix haute** : c'est le raisonnement qui est évalué.
5. **Traiter les cas limites** : entrée vide, un seul élément, valeurs nulles, débordement.
6. **Annoncer la complexité** finale, en temps et en espace.

**Notations** : O (borne supérieure, pire cas), Ω (borne inférieure), Θ (borne exacte) ; complexité **amortie** (l'ajout dans une ArrayList est O(1) amorti malgré des redimensionnements en O(n)) ; complexité **spatiale** (mémoire supplémentaire, y compris la pile de récursion).

**Algorithmes de tri** :
| Tri | Moyen | Pire | Mémoire | Stable | Remarque |
|---|---|---|---|---|---|
| Bulle, sélection | O(n²) | O(n²) | O(1) | bulle oui, sélection non | pédagogique |
| Insertion | O(n²) | O(n²) | O(1) | oui | très bon sur des données presque triées ou petites |
| Fusion (merge sort) | O(n log n) | O(n log n) | O(n) | oui | diviser pour régner |
| Rapide (quicksort) | O(n log n) | O(n²) | O(log n) | non | très rapide en pratique, pivot crucial |
| Tas (heapsort) | O(n log n) | O(n log n) | O(1) | non | |
| Comptage, radix | O(n + k) | O(n + k) | O(n + k) | oui | entiers dans une plage bornée |

Un tri **stable** conserve l'ordre relatif des éléments égaux (important pour trier successivement par plusieurs critères). Un tri par comparaison ne peut pas faire mieux que O(n log n).

**Recherche dichotomique** : O(log n) sur un tableau trié ; `Arrays.binarySearch`, `Collections.binarySearch` (renvoient `-(point d'insertion) - 1` si absent). Calculer le milieu par `lo + (hi - lo) / 2` pour éviter le débordement de `(lo + hi) / 2`.

**Arbres** : arbre binaire de recherche (gauche < nœud < droite, O(log n) si équilibré, O(n) s'il dégénère en liste), arbres **équilibrés** (rouge-noir, utilisé par TreeMap ; AVL), parcours **préfixe** (nœud, gauche, droite), **infixe** (gauche, nœud, droite : donne l'ordre trié d'un BST), **postfixe**, **en largeur** (par niveaux, avec une file). B-tree et B+tree : arbres à grand degré, base des index de bases de données.

**Tas et PriorityQueue** : tas binaire, accès au minimum en O(1), insertion et extraction en O(log n), construction en O(n). Pattern **top K** : un tas de taille K (min-heap pour les K plus grands), complexité O(n log K).

**Graphes** : représentation par **liste d'adjacence** (`Map<Noeud, List<Noeud>>`, économe pour les graphes peu denses) ou **matrice d'adjacence** (accès O(1), mémoire O(V²)). Algorithmes : BFS/DFS en O(V + E), **Dijkstra** (plus court chemin à poids positifs, avec une PriorityQueue, O((V + E) log V)), Bellman-Ford (poids négatifs), **tri topologique** (ordonner des tâches selon leurs dépendances, d'un graphe orienté acyclique : résolution de dépendances Maven, ordre de démarrage), **union-find** (composantes connexes), détection de cycle.

**Autres structures** : pile (`ArrayDeque`, appels de fonctions, parenthèses), file (BFS, producteur-consommateur), **trie** (arbre de préfixes, autocomplétion), table de hachage, liste chaînée, matrice, bitset.

**Techniques** : diviser pour régner, glouton (choix localement optimal, correct seulement pour certains problèmes), programmation dynamique (**mémoïsation** descendante ou **tabulation** ascendante), backtracking, deux pointeurs, fenêtre glissante, sommes préfixes (somme d'un intervalle en O(1) après un pré-calcul O(n)), manipulation de bits (`x & (x - 1)` retire le bit de poids faible, XOR pour trouver l'élément unique).

**Récursion** : chaque appel consomme une frame de pile ; une récursion profonde (des dizaines de milliers de niveaux) provoque un `StackOverflowError`. Java n'optimise pas la récursion terminale : sur de grandes profondeurs, on écrit une version itérative avec une pile explicite.

### En situation

- **"Ce traitement est très lent"** → identifier la complexité avant d'optimiser.
- **"Écris une fonction qui..."** → méthode de live coding.
- **"Comment tu détectes des doublons ?"** → HashSet.

### Questions techniques

**Q : Classe les complexités.** R : O(1) < O(log n) < O(n) < O(n log n) < O(n²) < O(2ⁿ) < O(n!).

**Q : Complexité d'une recherche en HashMap vs TreeMap ?** R : O(1) en moyenne vs O(log n).

**Q : Quel tri utilise Java ?** R : TimSort pour les objets, quicksort double pivot pour les primitifs.

**Q : BFS vs DFS ?** R : Largeur avec une file (plus court chemin) vs profondeur avec une pile (exploration exhaustive).

**Q : C'est quoi la programmation dynamique ?** R : Décomposer en sous-problèmes qui se chevauchent et mémoriser les résultats.

**Q : Pattern sliding window ?** R : Une fenêtre glissante pour éviter de recalculer sur les sous-séquences contiguës.

**Q : O vs Θ vs Ω ?** R : Borne supérieure vs borne exacte vs borne inférieure.

**Q : C'est quoi la complexité amortie ?** R : Le coût moyen d'une opération sur une longue séquence, malgré des opérations ponctuellement coûteuses.

**Q : C'est quoi un tri stable ?** R : Un tri qui conserve l'ordre relatif des éléments égaux.

**Q : Pire cas du quicksort et comment l'éviter ?** R : O(n²) avec un mauvais pivot ; pivot aléatoire ou médiane de trois, ou double pivot.

**Q : Pourquoi le merge sort est-il utilisé pour les objets ?** R : Il est stable et garanti en O(n log n) (TimSort en est une variante adaptative).

**Q : Limite théorique d'un tri par comparaison ?** R : O(n log n).

**Q : Pourquoi écrire `lo + (hi - lo) / 2` ?** R : Pour éviter le débordement d'entier de `lo + hi`.

**Q : Quel parcours d'un BST donne l'ordre trié ?** R : Le parcours infixe.

**Q : Comment trouver les K plus grands éléments efficacement ?** R : Un min-heap de taille K, en O(n log K).

**Q : Liste vs matrice d'adjacence ?** R : Économe pour les graphes peu denses vs accès O(1) mais mémoire O(V²).

**Q : Quel algorithme pour le plus court chemin pondéré ?** R : Dijkstra pour des poids positifs, Bellman-Ford sinon.

**Q : À quoi sert un tri topologique ?** R : Ordonner des éléments selon leurs dépendances dans un graphe orienté acyclique.

**Q : Mémoïsation vs tabulation ?** R : Récursion avec cache vs remplissage itératif d'un tableau de sous-résultats.

**Q : Java optimise-t-il la récursion terminale ?** R : Non ; une récursion très profonde finit en StackOverflowError.

**Q : C'est quoi un trie ?** R : Un arbre de préfixes, idéal pour l'autocomplétion et la recherche par préfixe.

### Questions de situation

**Q : "Ce traitement met 10 minutes sur 100 000 éléments."**
R : "Je regarderais la **complexité algorithmique** avant toute micro-optimisation. Dix minutes sur 100 000 éléments évoque du quadratique, typiquement une boucle imbriquée, ou une recherche linéaire à l'intérieur d'une boucle.
Le réflexe : identifier la structure utilisée pour les recherches. Passer d'une List à un HashSet fait tomber la recherche de O(n) à O(1), et le traitement global de O(n²) à O(n), un facteur de plusieurs milliers à ce volume.
Je vérifierais aussi qu'il n'y a pas d'appel réseau ou de requête base **dans** la boucle, un N+1 déguisé. Dans ce cas la solution est de charger en une requête, ou de traiter par lots.
Et seulement si l'algorithme est déjà optimal, je regarderais la parallélisation ou le réglage JVM. Optimiser un mauvais algorithme, c'est repeindre une voiture sans moteur."

**Q : "Tu dois ordonner le démarrage de 50 microservices qui dépendent les uns des autres."**
R : "C'est un **tri topologique** sur le graphe des dépendances. Algorithme de Kahn : compter pour chaque service le nombre de dépendances non satisfaites, mettre dans une file ceux qui en ont zéro, et à chaque fois qu'un service est démarré, décrémenter le compteur de ceux qui dépendent de lui. Complexité O(V + E). S'il reste des services à la fin, il existe un **cycle**, qu'il faut signaler.
Mais en pratique je questionnerais le besoin : des services qui doivent démarrer dans un ordre strict sont fragiles. Mieux vaut que chacun démarre indépendamment et attende ses dépendances via ses readiness probes et des retries."

═══════════════════════════════════════════
# 41. EXERCICES DE LIVE CODING CORRIGÉS
═══════════════════════════════════════════

### Vue d'ensemble

Le *live coding* d'un entretien Java porte rarement sur des algorithmes avancés. On retrouve presque toujours les mêmes familles : manipulation de chaînes, recherche dans un tableau, comptage avec une Map, structures de pile et de liste chaînée, et de plus en plus des exercices de **Streams** sur une liste d'objets métier. Ce chapitre rassemble les exercices les plus fréquents, avec une solution idiomatique et sa complexité. Toutes les solutions ont été compilées et exécutées sur un JDK 21.

Rappel de méthode (voir le chapitre Algorithmes) : reformuler, demander les contraintes et les cas limites, proposer une solution simple, l'améliorer, annoncer la complexité, et **parler en codant**.

### Cours

**Les réflexes qui reviennent dans presque tous les exercices** :
- Rechercher ou compter → `HashMap`/`HashSet` (O(1)).
- Ordre d'apparition à conserver → `LinkedHashMap`/`LinkedHashSet`.
- Parcourir par les deux bouts → deux pointeurs.
- Sous-chaîne ou sous-tableau contigu → fenêtre glissante.
- Appariement, imbrication, retour arrière → pile (`ArrayDeque`).
- K meilleurs éléments → `PriorityQueue` de taille K.
- Tableau trié → recherche dichotomique.
- Cas limites à annoncer : null, vide, un seul élément, doublons, valeurs négatives, débordement.

**1. FizzBuzz** (O(n))
```java
static List<String> fizzBuzz(int n) {
    return IntStream.rangeClosed(1, n)
        .mapToObj(i -> i % 15 == 0 ? "FizzBuzz" : i % 3 == 0 ? "Fizz" : i % 5 == 0 ? "Buzz" : String.valueOf(i))
        .toList();
}
```
Tester 15 avant 3 et 5 est le piège. Une boucle `for` classique est tout aussi acceptable.

**2. Inverser une chaîne** (O(n))
```java
static String inverser(String s) {
    char[] c = s.toCharArray();
    for (int i = 0, j = c.length - 1; i < j; i++, j--) { char t = c[i]; c[i] = c[j]; c[j] = t; }
    return new String(c);
}
```
En une ligne : `new StringBuilder(s).reverse().toString()` (qui gère aussi correctement les paires de substitution Unicode). On demandera souvent la version manuelle.

**3. Palindrome en ignorant la ponctuation et la casse** (O(n), O(1) en mémoire)
```java
static boolean estPalindrome(String s) {
    int i = 0, j = s.length() - 1;
    while (i < j) {
        char a = s.charAt(i), b = s.charAt(j);
        if (!Character.isLetterOrDigit(a)) { i++; continue; }
        if (!Character.isLetterOrDigit(b)) { j--; continue; }
        if (Character.toLowerCase(a) != Character.toLowerCase(b)) return false;
        i++; j--;
    }
    return true;
}
```

**4. Anagrammes** (O(n))
```java
static boolean sontAnagrammes(String a, String b) {
    if (a.length() != b.length()) return false;
    Map<Character, Integer> compte = new HashMap<>();
    for (char c : a.toCharArray()) compte.merge(c, 1, Integer::sum);
    for (char c : b.toCharArray()) {
        Integer n = compte.merge(c, -1, Integer::sum);
        if (n < 0) return false;
    }
    return true;
}
```
Alternative : trier les deux tableaux de caractères et les comparer (O(n log n)). Pour l'alphabet a-z seul, un `int[26]` est encore plus rapide.

**5. Premier caractère non répété** (O(n))
```java
static Optional<Character> premierNonRepete(String s) {
    Map<Character, Long> freq = s.chars().mapToObj(c -> (char) c)
        .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
    return freq.entrySet().stream().filter(e -> e.getValue() == 1)
        .map(Map.Entry::getKey).findFirst();
}
```
La `LinkedHashMap` conserve l'ordre d'apparition : c'est le point clé. Pour `"aabbcdc"`, la réponse est `d`.

**6. Compter les mots d'un texte** (O(n))
```java
static Map<String, Long> compterMots(String texte) {
    return Arrays.stream(texte.toLowerCase().split("\\W+"))
        .filter(m -> !m.isBlank())
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
}
```
Attention : `\W` ne reconnaît pas les lettres accentuées comme des lettres ; pour du français, `split("[^\\p{L}\\p{N}]+")`.

**7. Fibonacci et factorielle**
```java
static long fibonacci(int n) {              // O(n) en temps, O(1) en mémoire
    if (n < 2) return n;
    long a = 0, b = 1;
    for (int i = 2; i <= n; i++) { long c = a + b; a = b; b = c; }
    return b;
}
static BigInteger factorielle(int n) {      // BigInteger : 21! dépasse déjà un long
    BigInteger r = BigInteger.ONE;
    for (int i = 2; i <= n; i++) r = r.multiply(BigInteger.valueOf(i));
    return r;
}
```
La version récursive naïve de Fibonacci est exponentielle (O(2ⁿ)) : à mentionner, puis corriger par mémoïsation ou par la version itérative. `fibonacci(92)` est le maximum qui tient dans un long.

**8. Nombres premiers**
```java
static boolean estPremier(int n) {                     // O(√n)
    if (n < 2) return false;
    for (int d = 2; (long) d * d <= n; d++) if (n % d == 0) return false;
    return true;
}
static List<Integer> crible(int max) {                 // crible d'Ératosthène, O(n log log n)
    boolean[] compose = new boolean[max + 1];
    List<Integer> premiers = new ArrayList<>();
    for (int i = 2; i <= max; i++) {
        if (compose[i]) continue;
        premiers.add(i);
        for (long j = (long) i * i; j <= max; j += i) compose[(int) j] = true;
    }
    return premiers;
}
```

**9. Two Sum : indices de deux nombres dont la somme vaut une cible** (O(n))
```java
static int[] twoSum(int[] t, int cible) {
    Map<Integer, Integer> vus = new HashMap<>();
    for (int i = 0; i < t.length; i++) {
        Integer j = vus.get(cible - t[i]);
        if (j != null) return new int[]{j, i};
        vus.put(t[i], i);
    }
    return new int[0];
}
```
La solution naïve à deux boucles est O(n²) ; la Map transforme la recherche du complément en O(1). Si le tableau est trié : deux pointeurs, O(1) en mémoire.

**10. Trouver les doublons** (O(n))
```java
static Set<Integer> doublons(int[] t) {
    Set<Integer> vus = new HashSet<>(), doublons = new LinkedHashSet<>();
    for (int x : t) if (!vus.add(x)) doublons.add(x);
    return doublons;
}
```
`Set.add` renvoie `false` si l'élément était déjà présent : c'est l'astuce.

**11. Recherche dichotomique** (O(log n))
```java
static int rechercheDichotomique(int[] t, int x) {
    int lo = 0, hi = t.length - 1;
    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;          // évite le débordement de lo + hi
        if (t[mid] == x) return mid;
        if (t[mid] < x) lo = mid + 1; else hi = mid - 1;
    }
    return -1;
}
```

**12. Parenthèses équilibrées** (O(n))
```java
static boolean bienParenthese(String s) {
    Deque<Character> pile = new ArrayDeque<>();
    Map<Character, Character> paires = Map.of(')', '(', ']', '[', '}', '{');
    for (char c : s.toCharArray()) {
        if (paires.containsValue(c)) pile.push(c);
        else if (paires.containsKey(c)) {
            if (pile.isEmpty() || pile.pop() != paires.get(c)) return false;
        }
    }
    return pile.isEmpty();
}
```
`"{[()()]}"` → vrai, `"([)]"` → faux, `"(("` → faux (la pile n'est pas vide à la fin, cas souvent oublié).

**13. Inverser une liste chaînée et détecter un cycle**
```java
static class Noeud { int val; Noeud suivant; Noeud(int v) { val = v; } }

static Noeud inverserListe(Noeud tete) {         // O(n), O(1) en mémoire
    Noeud precedent = null, courant = tete;
    while (courant != null) {
        Noeud suivant = courant.suivant;
        courant.suivant = precedent;
        precedent = courant;
        courant = suivant;
    }
    return precedent;
}

static boolean aUnCycle(Noeud tete) {             // algorithme de Floyd (lièvre et tortue)
    Noeud lent = tete, rapide = tete;
    while (rapide != null && rapide.suivant != null) {
        lent = lent.suivant;
        rapide = rapide.suivant.suivant;
        if (lent == rapide) return true;
    }
    return false;
}
```

**14. Fusionner deux tableaux triés** (O(n + m)) : la brique du tri fusion.
```java
static int[] fusionner(int[] a, int[] b) {
    int[] r = new int[a.length + b.length];
    int i = 0, j = 0, k = 0;
    while (i < a.length && j < b.length) r[k++] = a[i] <= b[j] ? a[i++] : b[j++];
    while (i < a.length) r[k++] = a[i++];
    while (j < b.length) r[k++] = b[j++];
    return r;
}
```

**15. Sous-tableau de somme maximale** (algorithme de Kadane, O(n))
```java
static int sommeMax(int[] t) {
    int meilleur = t[0], courant = t[0];
    for (int i = 1; i < t.length; i++) {
        courant = Math.max(t[i], courant + t[i]);   // prolonger ou recommencer
        meilleur = Math.max(meilleur, courant);
    }
    return meilleur;
}
```
Pour `{-2, 1, -3, 4, -1, 2, 1, -5, 4}` : 6 (le sous-tableau `4, -1, 2, 1`).

**16. Plus longue sous-chaîne sans caractère répété** (fenêtre glissante, O(n))
```java
static int plusLongueSansRepetition(String s) {
    Map<Character, Integer> dernierePos = new HashMap<>();
    int debut = 0, max = 0;
    for (int i = 0; i < s.length(); i++) {
        Integer p = dernierePos.put(s.charAt(i), i);
        if (p != null && p >= debut) debut = p + 1;   // on fait avancer le début de la fenêtre
        max = Math.max(max, i - debut + 1);
    }
    return max;
}
```
`"abcabcbb"` → 3, `"pwwkew"` → 3.

**17. Les K éléments les plus fréquents** (O(n log k))
```java
static List<Integer> topKFrequents(int[] t, int k) {
    Map<Integer, Integer> freq = new HashMap<>();
    for (int x : t) freq.merge(x, 1, Integer::sum);
    PriorityQueue<Map.Entry<Integer, Integer>> tas = new PriorityQueue<>(Map.Entry.comparingByValue());
    for (var e : freq.entrySet()) {
        tas.offer(e);
        if (tas.size() > k) tas.poll();              // on garde les k plus fréquents
    }
    List<Integer> r = new ArrayList<>();
    while (!tas.isEmpty()) r.add(tas.poll().getKey());
    Collections.reverse(r);
    return r;
}
```

**18. Nombre manquant de 0 à n** (O(n), O(1))
```java
static int nombreManquant(int[] t) {
    int n = t.length;
    long attendu = (long) n * (n + 1) / 2;
    long somme = 0;
    for (int x : t) somme += x;
    return (int) (attendu - somme);
}
```
Alternative élégante sans risque de débordement : un XOR de tous les indices et de toutes les valeurs.

**19. Deuxième plus grand élément distinct**
```java
static OptionalInt deuxiemePlusGrand(int[] t) {
    return Arrays.stream(t).boxed().distinct().sorted(Comparator.reverseOrder())
        .skip(1).mapToInt(Integer::intValue).findFirst();
}
```
Version en O(n) attendue souvent ensuite : un seul parcours en maintenant le premier et le deuxième maximum.

**20. Compression de chaîne** (`"aaabccdddd"` → `"a3b1c2d4"`, on renvoie l'original si ce n'est pas plus court)
```java
static String compresser(String s) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); ) {
        int j = i;
        while (j < s.length() && s.charAt(j) == s.charAt(i)) j++;
        sb.append(s.charAt(i)).append(j - i);
        i = j;
    }
    return sb.length() < s.length() ? sb.toString() : s;
}
```

**21. Exercices de Streams sur une liste d'employés**
```java
record Employe(String nom, String departement, double salaire, int age) {}

// Salaire moyen par département, trié par département
Map<String, Double> moyenne = emps.stream()
    .collect(groupingBy(Employe::departement, TreeMap::new, averagingDouble(Employe::salaire)));

// Nom de l'employé le mieux payé de chaque département
Map<String, String> mieuxPaye = emps.stream()
    .collect(groupingBy(Employe::departement, TreeMap::new,
        collectingAndThen(maxBy(comparingDouble(Employe::salaire)), o -> o.get().nom())));

// Séparer les plus de 40 ans des autres, noms joints par une virgule
Map<Boolean, String> parAge = emps.stream()
    .collect(partitioningBy(e -> e.age() > 40, mapping(Employe::nom, joining(","))));

// Trier par département puis salaire décroissant
List<String> tries = emps.stream()
    .sorted(comparing(Employe::departement).thenComparing(Employe::salaire, reverseOrder()))
    .map(Employe::nom).toList();

// Masse salariale totale, et deuxième salaire le plus élevé
double total = emps.stream().mapToDouble(Employe::salaire).sum();
Optional<String> deuxieme = emps.stream()
    .sorted(comparingDouble(Employe::salaire).reversed())
    .skip(1).findFirst().map(Employe::nom);
```

**22. Classiques de conception déjà traités ailleurs** : cache LRU (chapitre Collections), producteur-consommateur avec `BlockingQueue` (chapitre Concurrence), singleton thread-safe (chapitre Design Patterns : idiome holder ou enum), classe immuable (chapitre Fondamentaux : champs `final`, classe `final`, pas de setter, copies défensives), `equals`/`hashCode` corrects (chapitre Fondamentaux), compteur thread-safe (`AtomicInteger`, `LongAdder`), parallélisation d'appels avec `CompletableFuture.allOf` (chapitre Concurrence).

### En situation

- **"Écris une fonction qui..."** → reformuler, cas limites, solution simple, puis optimisée.
- **"Peux-tu faire mieux ?"** → presque toujours une HashMap, deux pointeurs ou un tri préalable.
- **"Et si les données ne tiennent pas en mémoire ?"** → traitement en flux, par lots, tri externe, ou délégation à la base.
- **"Et en concurrence ?"** → structures concurrentes, immutabilité, ou isolement par thread.

### Questions techniques

**Q : Complexité de Two Sum avec une HashMap ?** R : O(n) en temps, O(n) en mémoire.

**Q : Comment détecter un cycle dans une liste chaînée sans mémoire supplémentaire ?** R : Algorithme de Floyd : un pointeur lent et un rapide finissent par se rencontrer s'il y a un cycle.

**Q : Pourquoi le Fibonacci récursif naïf est-il lent ?** R : Il recalcule les mêmes sous-problèmes : complexité exponentielle.

**Q : Quelle structure pour vérifier des parenthèses ?** R : Une pile (ArrayDeque).

**Q : Comment trouver le premier élément non répété en conservant l'ordre ?** R : Compter dans une LinkedHashMap puis prendre la première entrée de compte 1.

**Q : Comment conserver uniquement les K plus grands éléments d'un flux ?** R : Un min-heap de taille K.

**Q : Quelle est l'idée de l'algorithme de Kadane ?** R : À chaque position, prolonger le sous-tableau courant ou en recommencer un, en gardant le meilleur.

**Q : Qu'est-ce qu'une fenêtre glissante ?** R : Deux indices qui délimitent une sous-séquence contiguë et avancent sans tout recalculer.

**Q : Pourquoi BigInteger pour une factorielle ?** R : 21! dépasse la capacité d'un long.

### Questions de situation

**Q : "Tu bloques sur un exercice en live coding."**
R : "Je le dirais, et je reviendrais au plus simple : écrire une solution naïve qui fonctionne, même en O(n²), plutôt que de rester sans rien. Je tracerais un petit exemple à la main pour voir le motif, et je me demanderais quelle structure rend la recherche en O(1) ou si trier d'abord simplifie le problème.
L'évaluateur préfère un candidat qui raisonne à voix haute et avance par étapes, quitte à accepter un indice, à un candidat silencieux. Accepter une suggestion et l'exploiter fait aussi partie de ce qui est évalué : c'est ce qui se passe en binôme au quotidien."

═══════════════════════════════════════════
# 42. QUESTIONS COMPORTEMENTALES & PRÉSENTER SON EXPÉRIENCE
═══════════════════════════════════════════

### Vue d'ensemble

Un entretien technique n'est jamais purement technique. L'évaluateur cherche aussi à savoir comment tu travailles : comment tu réagis à un incident, à un désaccord, à un échec, comment tu apprends, si tu sais expliquer ce que tu as fait. Ces questions se préparent autant que les questions de Java, avec **tes propres exemples**.

Ce chapitre ne contient volontairement aucune expérience personnelle : il donne les trames de réponse. À toi de préparer, pour chaque trame, un ou deux exemples réels tirés de tes projets, de ton alternance, de tes stages ou de projets personnels. Un exemple vécu, précis et honnête vaut dix définitions.

### Cours

**La méthode STAR** pour toute question qui commence par "parle-moi d'une fois où..." :
- **Situation** : le contexte en deux phrases (projet, équipe, enjeu).
- **Tâche** : ce qui t'incombait à toi.
- **Action** : ce que **tu** as fait, concrètement (le "je", pas le "on"), avec les choix et leurs raisons.
- **Résultat** : ce qui en est sorti, chiffré si possible, et ce que tu en as retenu.

Variante utile : **STAR + L** (*Learning*), en terminant par ce que tu ferais différemment.

**Se présenter en deux minutes** (le "présente-toi" d'ouverture) : présent (ton rôle, ta stack, ce que tu fais aujourd'hui) → passé (le parcours qui y mène, en une ou deux étapes marquantes) → futur (ce que tu cherches et pourquoi ce poste y correspond). Ne pas réciter le CV, que l'interlocuteur a sous les yeux.

**Présenter un projet** : contexte métier en une phrase, architecture en une phrase (et un schéma si un tableau est disponible), **ton périmètre exact**, la difficulté principale, comment tu l'as résolue, le résultat et une limite assumée. Prépare-toi aux relances : "pourquoi ce choix ?", "qu'aurais-tu fait autrement ?", "comment l'as-tu testé ?", "combien de requêtes par seconde ?". Ne cite pas une technologie que tu ne saurais pas expliquer.

**Les règles d'or** :
- **Précision sur le périmètre** : distinguer ce que tu as fait de ce que l'équipe a fait.
- **Honnêteté** : un résultat nuancé honnêtement inspire plus confiance qu'un succès survendu ; un évaluateur expérimenté détecte l'exagération en deux relances.
- **Concision** : deux minutes par réponse, puis laisser l'interlocuteur creuser.
- **Jamais de critique** d'un ancien employeur ou d'anciens collègues : décrire une situation, pas accuser des personnes.
- **Le "je ne sais pas"** est acceptable s'il est suivi de "voici comment je chercherais".

**Questions à préparer**, avec la trame attendue :
| Question | Ce qui est évalué | Trame |
|---|---|---|
| Ta plus grande difficulté technique | démarche de résolution | STAR, insister sur le diagnostic et la méthode |
| Un échec ou une erreur | lucidité, apprentissage | reconnaître sa part, la correction, ce qui a changé ensuite |
| Un conflit ou désaccord avec un collègue | communication | écouter, argumenter sur des faits, trouver une issue, relation préservée |
| Un désaccord avec ton lead | courage et loyauté | exprimer son avis argumenté, puis appliquer la décision prise (*disagree and commit*) |
| Une urgence en production | sang-froid, priorités | rétablir d'abord, communiquer, analyser ensuite, post-mortem |
| Apprendre vite une techno inconnue | autonomie | documentation officielle, petit prototype, demander aux bonnes personnes |
| Une tâche floue ou mal spécifiée | proactivité | poser des questions, reformuler par écrit, proposer une première version |
| Un retour négatif reçu | ouverture | l'accepter, l'appliquer, exemple concret de progression |
| Une réalisation dont tu es fier | motivation, impact | un projet où ton apport est clair et mesurable |
| Prioriser plusieurs demandes | organisation | critères (impact, urgence), transparence sur les arbitrages |
| Travailler avec un code legacy | pragmatisme | comprendre avant de changer, tests de caractérisation, petits pas |

**Questions de motivation** :
- *Pourquoi ce poste / cette entreprise ?* : relier un élément concret de l'offre ou de l'entreprise (stack, domaine métier, taille, culture) à ce que tu cherches. Se renseigner avant.
- *Pourquoi quitter ton poste actuel ?* : formuler vers l'avant (ce que tu cherches) plutôt que contre (ce que tu fuis).
- *Où te vois-tu dans 3 ou 5 ans ?* : une direction crédible (expertise technique, responsabilités d'architecture, encadrement) sans promettre ce que tu ne sais pas encore.
- *Tes qualités et défauts ?* : une qualité prouvée par un exemple ; un vrai défaut, mineur pour le poste, avec ce que tu fais pour le compenser.
- *Prétentions salariales* : connaître la fourchette du marché pour le poste, le niveau et la région, et donner une fourchette argumentée.

**Spécificités d'un entretien en ESN** : le recruteur évalue aussi ta capacité à être placé chez un client. Questions fréquentes : mobilité géographique, ouverture à différents secteurs et technologies, autonomie chez un client, façon de représenter l'entreprise, réaction à une période d'*intercontrat*. Les entretiens se font souvent en deux temps : avec l'ESN, puis avec le client final (qui pose les questions techniques sur ses propres sujets).

**Le test technique à la maison** : lire l'énoncé deux fois, poser les questions de clarification, livrer un code **propre et testé** plutôt qu'exhaustif, avec un README qui explique les choix, les hypothèses et ce qui aurait été fait avec plus de temps. La revue de ce test en entretien est l'occasion de montrer ta capacité à défendre et critiquer ton propre code.

**Les questions à poser à la fin** (toujours en avoir deux ou trois) : comment se passe une journée type dans l'équipe ? Comment se fait la revue de code et la mise en production ? Quelle est la plus grosse difficulté technique actuelle ? Comment est gérée la dette technique ? Comment s'organise la montée en compétence d'un nouvel arrivant ? Qu'est-ce qui distingue un bon développeur dans cette équipe ? Quelles sont les prochaines étapes du processus ?

**Communication pendant l'entretien technique** : penser à voix haute, cadrer avant de répondre, structurer ("trois points : ..."), donner un exemple concret pour chaque concept, admettre une incertitude, et vérifier que la réponse correspond à la question ("est-ce que je réponds à ce que vous vouliez savoir ?").

### En situation

- **"Présente-toi"** → présent, passé, futur, deux minutes.
- **"Parle-moi d'une fois où..."** → STAR, avec un exemple préparé.
- **"Pourquoi nous ?"** → un élément concret de l'entreprise relié à ton projet.
- **"As-tu des questions ?"** → toujours oui.

### Questions techniques

**Q : C'est quoi la méthode STAR ?** R : Situation, Tâche, Action, Résultat : une structure pour raconter une expérience concrète.

**Q : Combien de temps doit durer une présentation de soi ?** R : Environ deux minutes, en suivant présent, passé, futur.

**Q : Comment parler d'un échec ?** R : Le reconnaître, expliquer sa part de responsabilité, ce qui a été corrigé et ce qui a changé ensuite.

**Q : C'est quoi "disagree and commit" ?** R : Exprimer son désaccord argumenté, puis s'engager pleinement dans la décision prise.

**Q : Que faire si on ne connaît pas la réponse à une question ?** R : Le dire, puis expliquer comment on chercherait ou comment on raisonnerait.

**Q : Que contient un bon README de test technique ?** R : Comment lancer, les choix et hypothèses, les limites, et ce qui serait amélioré avec plus de temps.

### Questions de situation

**Q : "Parle-moi d'un problème technique difficile que tu as résolu."** *(trame)*
R : "[**Situation** : le système, l'enjeu, le symptôme observé, en deux phrases.] [**Tâche** : ce qu'on attendait de moi.] [**Action** : comment j'ai cherché, en insistant sur la démarche : reproduire, mesurer, émettre une hypothèse, la vérifier ; les fausses pistes écartées ; la solution retenue et pourquoi.] [**Résultat** : l'effet mesurable, et ce que j'en ai tiré, par exemple une meilleure compréhension d'un mécanisme ou un outil de diagnostic que j'utilise depuis.]"

**Q : "Comment réagis-tu si on te confie une tâche sur une technologie que tu ne connais pas, avec une échéance courte ?"**
R : "Je commencerais par cadrer ce qui est vraiment nécessaire pour l'échéance, puis par la documentation officielle et un prototype minimal pour valider le cœur du besoin rapidement. J'identifierais qui, dans l'équipe ou l'entreprise, connaît cette technologie pour lever les blocages vite plutôt que de rester bloqué seul.
Et je serais transparent sur les risques dès le début : si l'échéance me paraît intenable, je préfère le signaler tôt avec une proposition (réduire le périmètre, être épaulé) que de découvrir le retard à la fin."

═══════════════════════════════════════════
# GLOSSAIRE DES SIGLES ET TERMES
═══════════════════════════════════════════

Un index rapide pour le Ctrl+F : chaque terme renvoie à sa définition courte ; le détail est dans le chapitre correspondant.

| Terme | Signification |
|---|---|
| **2PC** | Two-Phase Commit : commit en deux phases d'une transaction distribuée (préparer, puis valider) |
| **ACID** | Atomicity, Consistency, Isolation, Durability : garanties des transactions relationnelles |
| **ADR** | Architecture Decision Record : document court qui consigne une décision d'architecture |
| **AMQP** | Advanced Message Queuing Protocol : protocole de messagerie de RabbitMQ |
| **AOP** | Aspect-Oriented Programming : comportement transversal (transactions, logs) appliqué par proxy |
| **AOT** | Ahead-Of-Time : compilation ou préparation avant l'exécution (GraalVM natif, Spring AOT, cache Leyden) |
| **API Gateway** | Point d'entrée unique devant des services (routage, authentification, limitation) |
| **APM** | Application Performance Monitoring : outils de supervision applicative |
| **BASE** | Basically Available, Soft state, Eventual consistency : alternative à ACID dans les systèmes distribués |
| **BFF** | Backend For Frontend : une façade par type de client |
| **BOLA / IDOR** | Broken Object Level Authorization / Insecure Direct Object Reference : accès à l'objet d'un autre |
| **BOM** | Bill of Materials : pom fixant un ensemble cohérent de versions |
| **CAP** | Consistency, Availability, Partition tolerance : théorème des systèmes distribués |
| **CAS** | Compare-And-Swap : instruction atomique à la base des classes Atomic |
| **CDC** | Change Data Capture : capture des modifications d'une base depuis son journal (Debezium) |
| **CDI** | Contexts and Dependency Injection : injection de dépendances standard Jakarta |
| **CDN** | Content Delivery Network : cache réparti géographiquement |
| **CDS** | Class Data Sharing : archive de classes pour accélérer le démarrage de la JVM |
| **CI/CD** | Continuous Integration / Continuous Delivery ou Deployment |
| **CIDR** | Notation d'une plage d'adresses IP (`10.0.0.0/24`) |
| **CNI** | Container Network Interface : plugin réseau de Kubernetes |
| **CORS** | Cross-Origin Resource Sharing : autorisation par le serveur des appels cross-origin dans le navigateur |
| **CQRS** | Command Query Responsibility Segregation : modèles d'écriture et de lecture séparés |
| **CRaC** | Coordinated Restore at Checkpoint : instantané et restauration d'une JVM chaude |
| **CRD** | Custom Resource Definition : nouveau type de ressource Kubernetes |
| **CSRF** | Cross-Site Request Forgery : action déclenchée à l'insu d'un utilisateur authentifié |
| **CSP** | Content Security Policy : en-tête qui restreint les sources de scripts et contenus |
| **DAST / SAST / SCA** | Tests de sécurité dynamiques / analyse statique du code / analyse des dépendances |
| **DDD** | Domain-Driven Design : conception centrée sur le métier |
| **DI / IoC** | Dependency Injection / Inversion of Control |
| **DIP** | Dependency Inversion Principle (le D de SOLID) |
| **DLQ** | Dead Letter Queue : file des messages non traitables |
| **DNS** | Domain Name System : résolution de noms en adresses |
| **DORA** | Métriques de performance de livraison (fréquence, délai, taux d'échec, temps de restauration) |
| **DoD / DoR** | Definition of Done / Definition of Ready |
| **DRY, KISS, YAGNI** | Don't Repeat Yourself, Keep It Simple, You Aren't Gonna Need It |
| **DTO** | Data Transfer Object : objet de transfert découplé du modèle interne |
| **EJB** | Enterprise JavaBeans : composants Java EE historiques |
| **ESN** | Entreprise de Services du Numérique |
| **ETag** | Empreinte de version d'une ressource HTTP pour le cache et la concurrence optimiste |
| **FaaS** | Function as a Service : serverless (Lambda) |
| **FFM** | Foreign Function & Memory API : appel de code natif sans JNI (Java 22) |
| **GC** | Garbage Collector : G1, ZGC, Shenandoah, Parallel, Serial, Epsilon |
| **GitOps** | Git comme source de vérité de l'infrastructure, réconciliée par un opérateur (ArgoCD, Flux) |
| **GoF** | Gang of Four : auteurs du catalogue des 23 design patterns |
| **gRPC** | Framework RPC de Google sur HTTP/2 et Protobuf |
| **HATEOAS** | Hypermedia As The Engine Of Application State : liens dans les réponses REST |
| **HPA / VPA** | Horizontal / Vertical Pod Autoscaler |
| **HMAC** | Hash-based Message Authentication Code : empreinte authentifiée par un secret |
| **IaC** | Infrastructure as Code (Terraform, Ansible) |
| **IaaS, PaaS, SaaS, CaaS** | Modèles de service cloud |
| **IAM** | Identity and Access Management : gestion des identités et droits |
| **ISP** | Interface Segregation Principle (le I de SOLID) |
| **ISR** | In-Sync Replicas : réplicas Kafka à jour |
| **JDBC** | Java Database Connectivity : API d'accès aux bases relationnelles |
| **JDK, JRE, JVM** | Kit de développement, environnement d'exécution, machine virtuelle |
| **JEP** | JDK Enhancement Proposal : proposition d'évolution du JDK |
| **JFR / JMC** | Java Flight Recorder / JDK Mission Control |
| **JIT** | Just-In-Time : compilation à la volée (C1, C2) |
| **JMH** | Java Microbenchmark Harness |
| **JMM** | Java Memory Model : règles de visibilité et d'ordonnancement (happens-before) |
| **JMS** | Java (Jakarta) Messaging : API de messagerie standard |
| **JNI** | Java Native Interface : appel de code natif (historique) |
| **JPA** | Java (Jakarta) Persistence API : ORM standard |
| **JPMS** | Java Platform Module System (Java 9, module-info.java) |
| **JTA / XA** | Transactions Jakarta / protocole de transaction distribuée |
| **JWT / JWKS** | JSON Web Token / ensemble de clés publiques pour vérifier les JWT |
| **KRaft** | Mode de consensus de Kafka sans ZooKeeper |
| **LSP** | Liskov Substitution Principle (le L de SOLID) |
| **LTS** | Long-Term Support : versions 8, 11, 17, 21, 25 (puis 29) |
| **MDC** | Mapped Diagnostic Context : contexte attaché aux logs (traceId) |
| **mTLS** | TLS mutuel : authentification des deux parties |
| **MVC** | Model-View-Controller |
| **MVCC** | Multi-Version Concurrency Control : lectures sur instantané sans bloquer les écritures |
| **N+1** | Une requête puis une requête par élément pour ses relations |
| **NIO** | New I/O : buffers, canaux, sélecteurs, Path/Files |
| **NMT** | Native Memory Tracking : suivi de la mémoire native de la JVM |
| **NPE** | NullPointerException |
| **OAuth 2.0 / OIDC** | Autorisation déléguée / couche d'authentification par-dessus |
| **OCI** | Open Container Initiative : standard d'images et de runtimes de conteneurs |
| **OCP** | Open/Closed Principle (le O de SOLID) |
| **OOM / OOMKilled** | OutOfMemoryError de la JVM / processus tué par le noyau ou Kubernetes |
| **ORM** | Object-Relational Mapping |
| **OSI** | Modèle réseau à sept couches |
| **OTLP / OTel** | OpenTelemetry Protocol / OpenTelemetry |
| **OWASP** | Open Worldwide Application Security Project (Top 10 2025) |
| **PACELC** | Extension de CAP : latence vs cohérence hors partition |
| **PDB** | PodDisruptionBudget |
| **PECS** | Producer Extends, Consumer Super (wildcards) |
| **PKCE** | Proof Key for Code Exchange : protection du flux Authorization Code |
| **POJO** | Plain Old Java Object |
| **PV / PVC** | PersistentVolume / PersistentVolumeClaim |
| **QoS** | Classes de qualité de service des pods (Guaranteed, Burstable, BestEffort) |
| **RBAC / ABAC** | Contrôle d'accès par rôles / par attributs |
| **RED / USE** | Rate-Errors-Duration / Utilization-Saturation-Errors |
| **ReDoS** | Déni de service par expression régulière |
| **RPO / RTO** | Perte de données maximale / durée de restauration maximale |
| **SBOM** | Software Bill of Materials : inventaire des composants d'un logiciel |
| **SemVer** | Versioning sémantique MAJEUR.MINEUR.CORRECTIF |
| **SLI / SLO / SLA** | Indicateur / objectif interne / engagement contractuel |
| **SOLID** | SRP, OCP, LSP, ISP, DIP |
| **SPI** | Service Provider Interface (ServiceLoader) |
| **SRP** | Single Responsibility Principle (le S de SOLID) |
| **SSE** | Server-Sent Events : flux serveur vers client sur HTTP |
| **SSRF** | Server-Side Request Forgery |
| **STAR** | Situation, Tâche, Action, Résultat |
| **TDD / BDD** | Test-Driven / Behavior-Driven Development |
| **TLAB** | Thread-Local Allocation Buffer |
| **TLS** | Transport Layer Security |
| **TTL** | Time To Live : durée de validité (cache, DNS) |
| **UUID v7** | Identifiant unique ordonné dans le temps, adapté aux clés primaires |
| **VPC** | Virtual Private Cloud : réseau privé cloud |
| **WIP** | Work In Progress : travail en cours (limites Kanban) |
| **XSS** | Cross-Site Scripting : injection de script dans une page |
| **XXE** | XML External Entity : faille des parseurs XML |

═══════════════════════════════════════════
# RÉCAP FINAL
═══════════════════════════════════════════

## Les phrases qui structurent une réponse

**Méthode** : *"Ça dépend, est-ce que c'est X ou Y ?"* · *"Je mesure avant d'optimiser."* · *"Je change une chose à la fois et je remesure."* · *"Je ne sais pas, mais voici comment je chercherais."*

**Performance** : *"Je regarde le p95, pas la moyenne."* · *"Statistiquement, c'est souvent la base de données."* · *"Scaler avant d'optimiser, c'est déplacer le problème en plus cher."*

**Prudence** : *"Je m'assure d'avoir des tests avant de refactorer."* · *"Je vérifie que la migration est compatible avec les deux versions du code."* · *"Je révoque le secret d'abord, je nettoie l'historique ensuite."*

**Conception** : *"Je dépends d'interfaces, pas d'implémentations."* · *"Je découpe par domaine, pas par couche technique."* · *"Un peu de duplication vaut mieux qu'un mauvais couplage."* · *"J'applique SOLID là où le code évolue, pas partout par principe."*

**Résilience** : *"Un timeout sur tout appel externe, sans exception."* · *"Optimiser, ce n'est pas que la vitesse, c'est aussi ne pas s'effondrer."*

## Les trois erreurs à ne pas commettre

**Réciter une définition sans exemple.** Chaque concept doit être relié à quelque chose que tu as fait ou vu. Prépare deux ou trois exemples tirés de tes propres projets (une migration, un incident, une optimisation, un pipeline) et relie-les aux concepts.

**Survendre.** Un évaluateur expérimenté détecte l'exagération en deux questions de relance. Assumer une limite (comme un benchmark non concluant) inspire plus confiance qu'un succès total.

**Répondre trop vite.** Sur une mise en situation, une question de cadrage vaut mieux qu'une solution immédiate. Prendre trois secondes pour structurer sa réponse est perçu comme de la rigueur, pas comme de l'hésitation.

## Le principe à retenir

Sur une mise en situation, **l'évaluateur n'attend pas LA bonne réponse** : il n'y en a souvent pas une seule. Il évalue comment tu réfléchis.

**Cadre → Mesure → Agis → Valide.**

---

*Fin du document.*
