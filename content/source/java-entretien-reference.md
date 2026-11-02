---
subject: java
title: Java
category: languages
position: 10
status: published
---

# Java & son écosystème : le document de référence pour les entretiens techniques

> Un document pour préparer un entretien Java de tous niveaux (junior à senior) : le langage, la JVM, la bibliothèque standard et l'outillage Java (tests, build).
> Les frameworks, les données, le DevOps, la sécurité, le cloud, la conception, les algorithmes et les questions comportementales ont chacun leur propre document dans le catalogue (ADR 0025).
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (les ponts entre symptômes et notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ctrl+F : un mot-clé (`HashMap`, `volatile`, `OutOfMemoryError`...), un sigle (voir le glossaire en fin de document), ou "Questions de situation" pour ne lire qu'un type de bloc.
> Informations à jour à l'automne 2026 : JDK 25 (dernière LTS) et JDK 27, JUnit 6.

---

## SOMMAIRE

**MÉTHODE UNIVERSELLE** : le réflexe des mises en situation

**PARTIE I : JAVA CORE** : 1. JAVA : FONDAMENTAUX · 2. JAVA : CLASSES, OBJETS & LANGAGE AVANCÉ · 3. JAVA : CHAÎNES, NOMBRES, DATES & TEXTE · 4. COLLECTIONS · 5. STREAMS & LAMBDAS · 6. CONCURRENCE & MULTITHREADING · 7. PROGRAMMATION RÉACTIVE · 8. MÉMOIRE & GARBAGE COLLECTOR · 9. JVM INTERNALS, PERFORMANCE & OUTILLAGE · 10. EXCEPTIONS · 11. GENERICS · 12. I/O, NIO, SÉRIALISATION & JSON · 13. JAVA MODERNE : RECORDS, SEALED, PATTERN MATCHING & VERSIONS · 14. QUESTIONS PIÈGES : QU'AFFICHE CE CODE ?

**PARTIE II : OBJET, TESTS & BUILD** : 15. PROGRAMMATION ORIENTÉE OBJET · 16. TESTS · 17. MAVEN, GRADLE & BUILD

**PARTIE III : PRATIQUE** : 18. EXERCICES DE LIVE CODING CORRIGÉS

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

### QCM

**Q : Où vivent les objets créés avec `new` ?**
- [ ] Dans la pile (stack) du thread
- [x] Dans le tas (heap)
- [ ] Dans le Metaspace
- [ ] Dans les registres du processeur
R : Les objets sont alloués dans le tas, partagé par les threads et géré par le ramasse-miettes. La pile ne contient que les variables locales et les références vers ces objets ; le Metaspace contient les métadonnées des classes.

**Q : Quelles affirmations sur `==` et `equals()` sont vraies ?**
- [x] Pour des objets, `==` compare les références
- [x] `equals()` peut être redéfinie pour comparer le contenu
- [ ] `==` appelle `equals()` en interne
- [ ] `Object.equals()` compare le contenu des champs
R : `==` teste l'identité (même objet en mémoire). `Object.equals()` fait la même chose par défaut ; `String`, `Integer` ou un `record` la redéfinissent pour comparer le contenu, et `hashCode()` doit alors être redéfinie avec elle.

**Q : Que vaut `Integer.valueOf(127) == Integer.valueOf(127)` ?**
- [x] `true`
- [ ] `false`
- [ ] Le code ne compile pas
R : `Integer.valueOf` réutilise des instances en cache pour les valeurs de -128 à 127 : les deux appels renvoient le même objet. Avec 128, le résultat serait `false` : il faut comparer des `Integer` avec `equals()`.

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
# 16. TESTS
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
# 17. MAVEN, GRADLE & BUILD
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
# 18. EXERCICES DE LIVE CODING CORRIGÉS
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
