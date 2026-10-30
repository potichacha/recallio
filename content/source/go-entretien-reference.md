# Go & son écosystème : le document de référence pour les entretiens techniques

> Un document unique pour préparer un entretien Go de tous niveaux (junior à senior) : langage, concurrence, runtime, conception, HTTP et gRPC, bases de données, tests, observabilité, sécurité, outillage, conteneurs et Kubernetes, algorithmes et questions comportementales.
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (des symptômes vers les notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ctrl+F : un mot-clé (`append`, `errgroup`, `GOMEMLIMIT`, `os.Root`...), un sigle (voir le glossaire en fin de document), ou "Questions de situation" pour ne lire qu'un type de bloc.
> Informations à jour à l'automne 2026 : Go 1.27 (août 2026) et Go 1.26, les deux versions supportées. Les questions pièges et les exercices de live coding ont été compilés et exécutés.

---

## SOMMAIRE

**MÉTHODE UNIVERSELLE** : le réflexe des mises en situation

**PARTIE I : LE LANGAGE GO** : 1. GO : FONDAMENTAUX DU LANGAGE · 2. TYPES COMPOSÉS : TABLEAUX, SLICES, MAPS, CHAÎNES, STRUCTS · 3. POINTEURS, MÉTHODES & RECEVEURS · 4. INTERFACES & COMPOSITION · 5. GESTION DES ERREURS : ERROR, WRAPPING, PANIC & RECOVER · 6. GENERICS (PARAMÈTRES DE TYPE) · 7. CONCURRENCE : GOROUTINES, CHANNELS & SELECT · 8. SYNCHRONISATION, ATOMIQUES & MODÈLE MÉMOIRE · 9. LE PACKAGE CONTEXT : ANNULATION, DÉLAIS & VALEURS · 10. LE RUNTIME : SCHEDULER, MÉMOIRE & GARBAGE COLLECTOR · 11. PERFORMANCE, BENCHMARKS & PROFILAGE · 12. GO MODERNE : ITÉRATEURS, NOUVEAUTÉS & VERSIONS · 13. BIBLIOTHÈQUE STANDARD ESSENTIELLE · 14. QUESTIONS PIÈGES : QU'AFFICHE CE CODE ?

**PARTIE II : CONCEPTION** : 15. GO IDIOMATIQUE : STYLE, NOMMAGE & CONCEPTION D'API · 16. DESIGN PATTERNS EN GO · 17. ARCHITECTURE & ORGANISATION D'UN PROJET GO

**PARTIE III : BACKEND & DONNÉES** : 18. HTTP : SERVEURS, CLIENTS & API REST · 19. gRPC, PROTOBUF & COMMUNICATION ENTRE SERVICES · 20. BASES DE DONNÉES EN GO · 21. TESTS EN GO · 22. CONFIGURATION, LOGS & OBSERVABILITÉ · 23. SÉCURITÉ EN GO

**PARTIE IV : OUTILLAGE & DEVOPS** : 24. MODULES, DÉPENDANCES & CHAÎNE D'OUTILS · 25. QUALITÉ DU CODE : FORMATAGE, ANALYSE STATIQUE & REVUE · 26. DOCKER, KUBERNETES & GO DANS LE CLOUD · 27. CI/CD, BUILD & PUBLICATION · 28. ÉCRIRE DES OUTILS EN LIGNE DE COMMANDE

**PARTIE V : COMPLÉMENTS** : 29. MICROSERVICES, MESSAGING & RÉSILIENCE EN GO · 30. ALGORITHMES & STRUCTURES DE DONNÉES EN GO · 31. EXERCICES DE LIVE CODING CORRIGÉS EN GO · 32. GO FACE AUX AUTRES LANGAGES

**PARTIE VI : SOFT SKILLS** : 33. QUESTIONS COMPORTEMENTALES & PRÉSENTER SON EXPÉRIENCE

**GLOSSAIRE** · **RÉCAP FINAL**

═══════════════════════════════════════════
# MÉTHODE UNIVERSELLE : MISES EN SITUATION
═══════════════════════════════════════════

**Le réflexe pour toute question de mise en situation** ("ton service est lent", "tu dois concevoir...", "un bug apparaît en production") :

**1. CADRER** : reformuler et poser une question de contexte (volume, contraintes, ce qui a changé récemment). Montrer qu'on ne fonce pas tête baissée.

**2. DIAGNOSTIQUER OU DÉCOMPOSER** : pour un problème, partir des symptômes et des métriques, émettre une hypothèse, choisir l'outil qui la vérifie (logs, métriques, pprof, trace, `-race`). Pour une conception, découper en sous-problèmes.

**3. PROPOSER** : une solution concrète, du plus simple au plus structurant, avec le code ou la bibliothèque Go qu'on utiliserait.

**4. ANNONCER LES COMPROMIS** : ce que la solution coûte, ses limites, et ce qu'on surveillerait ensuite.

En Go en particulier, l'évaluateur écoute si tu penses spontanément aux **erreurs**, au **contexte**, à la **concurrence** (fuites, courses de données) et à la **mesure**.

═══════════════════════════════════════════
# 1. GO : FONDAMENTAUX DU LANGAGE
═══════════════════════════════════════════

### Vue d'ensemble

Go a été conçu chez Google en 2007 (Robert Griesemer, Rob Pike, Ken Thompson) et publié en 2009, pour répondre à des problèmes très concrets : des builds C++ qui prenaient des dizaines de minutes, des bases de code immenses difficiles à lire, et des serveurs devant gérer énormément de connexions concurrentes. Ses choix découlent de ces contraintes : **compilation très rapide**, **langage volontairement petit** (25 mots-clés), **concurrence intégrée** (goroutines et channels), **binaire statique unique** facile à déployer, et une **bibliothèque standard** très complète pour le réseau.

La philosophie est la **simplicité délibérée** : pas d'héritage, pas d'exceptions, pas de surcharge, longtemps pas de generics (arrivés en 1.18), un seul style de formatage imposé par `gofmt`. L'idée est qu'un code Go écrit par n'importe qui doit ressembler à celui des autres, pour qu'une équipe de cent personnes puisse le lire sans effort. En entretien, on vérifie que tu as intégré cette philosophie : un candidat qui écrit du "Java en Go" (interfaces partout, hiérarchies, getters/setters) se repère vite.

Aujourd'hui Go est le langage de l'infrastructure cloud : Docker, Kubernetes, Terraform, Prometheus, etcd, Consul, Grafana (backend), Caddy et la plupart des outils CNCF sont écrits en Go. Il est aussi très présent dans les backends de services (API, microservices, plateformes de paiement, fintech).

### Cours

**Programme minimal** :
```go
package main

import "fmt"

func main() {
    fmt.Println("Bonjour")
}
```
Tout fichier appartient à un **package**. Le package `main` avec une fonction `main()` produit un exécutable. `go run main.go` compile et exécute ; `go build` produit un binaire ; `go test ./...` lance les tests ; `go vet` détecte des erreurs probables ; `gofmt`/`go fmt` formate.

**Visibilité par la casse** : un identifiant qui commence par une **majuscule** est **exporté** (visible hors du package), sinon il est privé au package. Il n'y a pas de mots-clés `public`/`private`. L'unité d'encapsulation est le **package**, pas le type.

**Déclaration de variables** :
```go
var a int          // valeur zéro : 0
var b = 42         // type inféré
c := "texte"       // déclaration courte, uniquement dans une fonction
var x, y = 1, 2
const Pi = 3.14159
```
`:=` déclare **et** initialise ; au moins une variable à gauche doit être nouvelle. Une variable locale **déclarée mais non utilisée** et un **import inutilisé** sont des **erreurs de compilation**, pas des avertissements.

**Valeurs zéro** : toute variable a une valeur dès sa déclaration. `0` pour les nombres, `false`, `""` pour les chaînes, `nil` pour les pointeurs, slices, maps, channels, fonctions et interfaces ; pour une struct, chaque champ à sa valeur zéro. Un principe central du design Go : **rendre la valeur zéro utile** (un `sync.Mutex` ou un `bytes.Buffer` utilisables sans initialisation).

**Types de base** : `bool`, `string`, `int`/`uint` (taille de mot de la machine, 64 bits en pratique), `int8`...`int64`, `uint8`...`uint64`, `uintptr`, `float32`, `float64`, `complex64`, `complex128`, `byte` (alias de `uint8`), `rune` (alias de `int32`, un point de code Unicode).

**Pas de conversion implicite** entre types numériques : `var i int = 1; var f float64 = i` ne compile pas, il faut `float64(i)`. Même `int` et `int64` sont incompatibles sans conversion. Les **constantes non typées** font exception : `const n = 10` peut être utilisée comme int, float64 ou durée selon le contexte, avec une précision arbitraire à la compilation.

**iota** : compteur qui démarre à 0 dans chaque bloc `const`, utilisé pour les énumérations :
```go
type Statut int
const (
    Brouillon Statut = iota // 0
    Publie                  // 1
    Archive                 // 2
)
const (
    _  = iota
    KB = 1 << (10 * iota)   // 1024
    MB                      // 1048576
)
```
Go n'a pas d'`enum` : on combine un type nommé et `iota`, avec souvent une méthode `String()` (générable par `stringer`).

**Contrôle de flux** :
- `if` sans parenthèses, accolades obligatoires, avec instruction d'initialisation : `if err := f(); err != nil { ... }` (la variable n'existe que dans le if/else).
- `for` est la **seule** boucle : `for i := 0; i < n; i++`, `for cond { }` (le while), `for { }` (infinie), `for i, v := range slice`, `for k, v := range m`, `for i := range 10` (Go 1.22), `for range n`.
- `switch` : pas de fall-through par défaut (le mot-clé `fallthrough` le force), plusieurs valeurs par `case`, `switch` sans expression comme suite de `if/else`, `switch` sur les types (`switch v := x.(type)`).
- `break` et `continue` avec labels pour les boucles imbriquées ; `goto` existe mais est rare.

**Variable de boucle (Go 1.22)** : depuis Go 1.22 (pour les modules qui déclarent `go 1.22` ou plus), **chaque itération a sa propre variable**. Avant, la variable était partagée par toutes les itérations, et les goroutines ou closures capturant `v` voyaient toutes la dernière valeur, un bug très classique. C'est encore une question d'entretien fréquente : il faut savoir expliquer l'ancien comportement et pourquoi il a changé.

**Fonctions** :
- Retours **multiples** : `func div(a, b int) (int, error)`.
- **Retours nommés** : `func f() (n int, err error)` ; un `return` nu renvoie leurs valeurs (à réserver aux fonctions courtes).
- **Variadiques** : `func somme(nums ...int) int`, appel avec `somme(xs...)` pour étaler un slice.
- Les fonctions sont des **valeurs de première classe** : affectables, passables en paramètre, retournables.
- **Closures** : une fonction anonyme capture les variables de son environnement **par référence** (elle voit et modifie la variable, pas une copie).
- Pas de surcharge, pas de valeurs par défaut, pas d'arguments nommés : on utilise des structs d'options ou le pattern *functional options*.
- **Tout est passé par valeur** : un paramètre est une copie. Passer un slice, une map ou un pointeur copie l'en-tête ou l'adresse, d'où l'impression de "passage par référence".

**defer** : planifie l'exécution d'un appel à la sortie de la fonction (normale ou par panic). Usages : fermer un fichier, libérer un verrou, `recover`.
```go
f, err := os.Open(chemin)
if err != nil { return err }
defer f.Close()
```
Règles : les `defer` s'exécutent en **ordre LIFO** ; les **arguments sont évalués au moment du `defer`**, pas à l'exécution ; un `defer` peut modifier les **retours nommés** ; un `defer` dans une boucle ne s'exécute qu'à la fin de la **fonction** (piège : fichiers ouverts qui s'accumulent). Le coût de `defer` est devenu négligeable depuis Go 1.14 (*open-coded defers*).

**init()** : une fonction `init()` par fichier (plusieurs possibles) s'exécute automatiquement après l'initialisation des variables du package, avant `main`. Ordre : dépendances d'abord, puis variables du package, puis `init`. À utiliser avec parcimonie : effets de bord cachés, difficile à tester. L'import `_ "pkg"` importe un package uniquement pour son `init` (pilotes de base de données, `net/http/pprof`).

**Identifiant blanc `_`** : ignore une valeur (`_, err := f()`), vérifie à la compilation qu'un type implémente une interface (`var _ io.Reader = (*MonType)(nil)`).

**Shadowing** : `:=` dans un bloc interne crée une **nouvelle** variable qui masque celle du bloc externe. Piège classique :
```go
var err error
if cond {
    x, err := f()   // nouvel err, celui du dessus reste nil
    _ = x
}
return err          // toujours nil
```
L'analyseur `shadow` (installable séparément) ou golangci-lint le détecte.

**Types nommés et alias** : `type Celsius float64` crée un **nouveau type** (méthodes propres, conversion explicite requise) ; `type Octet = byte` crée un **alias** (le même type). Les alias génériques sont possibles depuis Go 1.24.

**Formatage et style** : `gofmt` est non négociable, tabulations pour l'indentation, accolade ouvrante sur la même ligne (le point-virgule automatique l'impose). Noms courts et explicites (`i`, `r` pour un reader, `ctx`), pas de `getX` (on écrit `X()`), acronymes en capitales (`ServeHTTP`, `userID`), noms de packages courts, en minuscules, sans underscore ni pluriel (`http`, `strconv`).

**Commandes de la chaîne d'outils** : `go mod init`, `go get`, `go mod tidy`, `go build`, `go run`, `go test`, `go vet`, `go fmt`, `go doc`, `go generate`, `go install`, `go env`, `go list`, `go tool pprof`, `go fix` (modernisation automatique du code depuis Go 1.26).

**Compatibilité Go 1** : depuis Go 1.0 (2012), le code Go valide continue de compiler avec les nouvelles versions. Les changements de comportement nécessaires passent par la directive `go` du go.mod et les réglages `GODEBUG`.

### En situation

- **"Variable déclarée et non utilisée, le code ne compile pas"** → c'est voulu, Go refuse le code mort local.
- **"Mes goroutines affichent toutes la même valeur"** → capture de la variable de boucle avant Go 1.22.
- **"L'erreur retournée est toujours nil alors que la fonction échoue"** → shadowing de `err` avec `:=`.
- **"Trop de fichiers ouverts dans une boucle"** → `defer` dans la boucle, ne s'exécute qu'en fin de fonction.
- **"Comment faire un enum ?"** → type nommé + `iota` + méthode `String()`.

### Questions techniques

**Q : Qui a créé Go et pourquoi ?** R : Griesemer, Pike et Thompson chez Google (2007, public en 2009), pour des builds rapides, un code lisible à grande échelle et une concurrence simple.

**Q : Combien de mots-clés en Go ?** R : 25.

**Q : Comment rend-on un identifiant public ?** R : En le faisant commencer par une majuscule ; la visibilité est au niveau du package.

**Q : `var` vs `:=` ?** R : `var` fonctionne partout et permet de ne pas initialiser ; `:=` uniquement dans une fonction, déclare et initialise avec inférence.

**Q : Que se passe-t-il avec une variable locale non utilisée ?** R : Erreur de compilation, comme un import inutilisé.

**Q : C'est quoi la valeur zéro ?** R : La valeur qu'a toute variable non initialisée : 0, false, "", nil, ou une struct de valeurs zéro.

**Q : Pourquoi "rendre la valeur zéro utile" ?** R : Pour qu'un type soit utilisable sans constructeur (sync.Mutex, bytes.Buffer), ce qui simplifie l'API.

**Q : `byte` et `rune` ?** R : Alias de uint8 et int32 ; rune représente un point de code Unicode.

**Q : Go fait-il des conversions numériques implicites ?** R : Non, même entre int et int64 ; seules les constantes non typées s'adaptent au contexte.

**Q : C'est quoi iota ?** R : Un compteur de constantes qui repart de 0 dans chaque bloc const, utilisé pour les énumérations.

**Q : Combien de types de boucles en Go ?** R : Une seule, `for`, avec plusieurs formes (classique, condition, infinie, range).

**Q : Le switch a-t-il du fall-through ?** R : Non par défaut ; le mot-clé fallthrough le force.

**Q : Qu'a changé Go 1.22 pour les boucles ?** R : Chaque itération a sa propre variable, et on peut écrire `for i := range 10`.

**Q : Go supporte-t-il la surcharge de fonctions ?** R : Non, ni les paramètres par défaut.

**Q : Go passe-t-il les arguments par valeur ou par référence ?** R : Toujours par valeur ; un slice, une map ou un pointeur copient un en-tête ou une adresse.

**Q : C'est quoi une closure ?** R : Une fonction anonyme qui capture des variables de son environnement par référence.

**Q : Dans quel ordre s'exécutent plusieurs defer ?** R : En ordre inverse (LIFO).

**Q : Quand les arguments d'un defer sont-ils évalués ?** R : Au moment où le defer est déclaré.

**Q : Un defer peut-il modifier la valeur retournée ?** R : Oui, si la fonction a des retours nommés.

**Q : Pourquoi éviter defer dans une boucle ?** R : Les appels ne s'exécutent qu'à la fin de la fonction, les ressources s'accumulent ; extraire le corps dans une fonction.

**Q : Quand s'exécute init() ?** R : Après l'initialisation des variables du package et des dépendances, avant main.

**Q : À quoi sert `import _ "pkg"` ?** R : Importer un package uniquement pour les effets de bord de son init (pilotes, pprof).

**Q : C'est quoi le shadowing ?** R : Une variable déclarée dans un bloc interne avec le même nom qui masque la variable externe.

**Q : Type nommé vs alias ?** R : `type A B` crée un nouveau type distinct ; `type A = B` est le même type sous un autre nom.

**Q : Comment vérifier à la compilation qu'un type implémente une interface ?** R : `var _ Interface = (*Type)(nil)`.

**Q : Pourquoi pas de getX() en Go ?** R : Convention : un getter s'appelle X(), un setter SetX().

**Q : Que garantit la promesse de compatibilité Go 1 ?** R : Que le code valide continue de compiler et de fonctionner avec les nouvelles versions de Go 1.x.

**Q : Que fait go vet ?** R : Il détecte des erreurs probables qui compilent (Printf mal formé, copie de mutex, code inatteignable...).

### Questions de situation

**Q : "Pourquoi choisirais-tu Go plutôt que Java pour un nouveau service ?"**
R : "Go a des atouts nets pour certains services : un **binaire statique** sans runtime à installer, un démarrage en millisecondes et une empreinte mémoire faible (intéressant en conteneurs et pour l'autoscaling), une **concurrence** très simple à écrire avec les goroutines, une compilation rapide et un langage petit qu'une équipe maîtrise vite.
Java garde l'avantage sur un écosystème d'entreprise plus riche (Spring, Hibernate, intégration), des performances de pointe sur des traitements longs grâce au JIT, et un modèle objet plus adapté à un domaine métier très riche.
Je choisirais Go pour un service réseau, un proxy, un outil d'infrastructure, une CLI ou un microservice à forte concurrence ; je resterais en Java pour une application métier complexe déjà intégrée à l'écosystème Spring. Et je tiendrais compte de l'équipe : la meilleure techno est celle que l'équipe saura maintenir."

**Q : "Explique ce qui se passe quand tu exécutes `go build`."**
R : "La commande lit le `go.mod` pour connaître le module et ses dépendances, télécharge au besoin les modules manquants (via le proxy, vérifiés par `go.sum`), puis compile chaque package dans l'ordre des dépendances. Le compilateur produit du code machine directement, sans bytecode intermédiaire, et met en cache les résultats : un package inchangé n'est pas recompilé.
Le linker assemble ensuite tout, runtime compris (scheduler, GC), dans **un binaire statique** pour la plateforme cible, définie par `GOOS` et `GOARCH`. Sans cgo, ce binaire n'a aucune dépendance système, ce qui permet de le placer dans une image `scratch` ou distroless."

**Q : "Un collègue venant de Java crée une interface pour chaque struct et des getters partout. Qu'en dis-tu ?"**
R : "C'est un réflexe compréhensible, mais ce n'est pas idiomatique en Go. En Go, les interfaces sont **implicites** et se déclarent du côté de **celui qui les utilise**, quand on en a besoin (pour un test ou plusieurs implémentations réelles), et elles sont petites. Une interface créée par avance pour chaque struct ajoute une indirection sans valeur.
Pour les getters, si un champ n'a pas d'invariant à protéger, on l'exporte simplement ; si on a besoin d'un accesseur, il s'appelle `Nom()` et non `GetNom()`. Je lui recommanderais *Effective Go* et les *Go Code Review Comments*, et j'en discuterais en revue de code sur des cas concrets plutôt que par principe."

═══════════════════════════════════════════
# 2. TYPES COMPOSÉS : TABLEAUX, SLICES, MAPS, CHAÎNES, STRUCTS
═══════════════════════════════════════════

### Vue d'ensemble

Go n'a que quelques structures de données intégrées, mais il faut les connaître **dans leur représentation mémoire**, parce que c'est là que se cachent les bugs. Un slice n'est pas un tableau : c'est une petite vue (pointeur, longueur, capacité) sur un tableau sous-jacent, et deux slices peuvent partager les mêmes données sans qu'on s'en rende compte. Une map n'est pas thread-safe et son ordre d'itération est volontairement aléatoire. Une chaîne est une suite d'**octets** immuable, pas de caractères.

Ces trois points (aliasing des slices, maps et concurrence, octets vs runes) représentent une part énorme des questions d'entretien Go de niveau intermédiaire, et une part importante des bugs en production.

### Cours

**Tableaux** : taille fixe faisant partie du type (`[3]int` et `[4]int` sont des types différents). Un tableau est une **valeur** : l'affecter ou le passer à une fonction **copie tous ses éléments**. Les tableaux sont comparables avec `==` si leurs éléments le sont. Rarement utilisés directement, sauf comme clé de map ou buffer de taille fixe.

**Slices** : un en-tête de trois mots, `{pointeur vers le tableau, len, cap}`.
```go
s := []int{1, 2, 3}          // littéral
s2 := make([]int, 0, 10)     // len 0, cap 10
var s3 []int                 // nil slice : len 0, cap 0, s3 == nil
s4 := []int{}                // slice vide non nil
```
- `len(s)` : nombre d'éléments accessibles ; `cap(s)` : place disponible dans le tableau sous-jacent à partir du début du slice.
- **Slicing** `s[a:b]` crée un nouvel en-tête qui **partage** le même tableau. Modifier `sub[0]` modifie `s[a]`.
- **Expression à trois indices** `s[a:b:c]` limite la capacité à `c-a`, ce qui force le prochain `append` à réallouer et évite d'écraser les données d'origine.
- **append** : si `len < cap`, écrit dans le tableau existant (et peut donc écraser des données visibles par un autre slice) ; sinon alloue un nouveau tableau plus grand (environ ×2 pour les petits slices, croissance plus douce au-delà de 256 éléments), copie et renvoie un en-tête vers ce nouveau tableau. D'où la règle : **toujours réaffecter** : `s = append(s, x)`.
- `copy(dst, src)` copie `min(len(dst), len(src))` éléments et renvoie ce nombre.
- Un slice n'est **pas comparable** avec `==` (sauf à `nil`) ; on utilise `slices.Equal` (Go 1.21).
- **nil vs vide** : les deux ont len 0 et acceptent `append` et `range`. La différence apparaît en JSON (`null` vs `[]`) et dans les comparaisons à nil. Retourner un slice nil pour "aucun résultat" est idiomatique.
- **Fuite mémoire par sous-slice** : garder `petit := gros[:10]` maintient tout le gros tableau en mémoire. Copier (`slices.Clone`) si le gros n'est plus utile.
- **Préallouer** quand la taille est connue : `make([]T, 0, n)` évite des réallocations successives.
- Package `slices` (1.21+) : `Contains`, `Index`, `Sort`, `SortFunc`, `BinarySearch`, `Reverse`, `Max`, `Min`, `Compact`, `Delete`, `Insert`, `Clone`, `Equal`, `Concat` (1.22), et des fonctions à itérateurs (`slices.All`, `Values`, `Collect`, `Sorted`, 1.23).

**Supprimer un élément** : `s = append(s[:i], s[i+1:]...)` (conserve l'ordre, O(n)) ou, si l'ordre n'importe pas, `s[i] = s[len(s)-1]; s = s[:len(s)-1]` (O(1)). Avec `slices.Delete(s, i, i+1)` qui, depuis Go 1.22, met à zéro les éléments libérés en fin de slice pour éviter de retenir des pointeurs.

**Maps** : table de hachage `map[K]V`.
```go
m := map[string]int{"a": 1}
m2 := make(map[string]int, 100) // indication de capacité
v := m["absent"]                 // valeur zéro, pas d'erreur
v, ok := m["a"]                  // idiome "comma ok"
delete(m, "a")
clear(m)                         // Go 1.21 : vide la map
```
- La clé doit être **comparable** (`==` défini) : nombres, chaînes, booléens, pointeurs, channels, interfaces, tableaux et structs de types comparables. Pas de slices, maps ni fonctions comme clé.
- **Écrire dans une map nil panique** (`assignment to entry in nil map`) ; la lire renvoie la valeur zéro.
- L'**ordre d'itération est aléatoire** et volontairement variable d'une exécution à l'autre, pour qu'aucun code ne dépende d'un ordre. Pour un ordre stable : trier les clés (`slices.Sorted(maps.Keys(m))` en 1.23+).
- Les maps **ne sont pas sûres en concurrence** : une écriture concurrente avec une autre lecture ou écriture provoque une erreur **fatale** du runtime (`concurrent map writes`), non récupérable par `recover`. Protéger avec un `sync.Mutex`/`RWMutex`, ou utiliser `sync.Map` dans des cas précis.
- On **ne peut pas prendre l'adresse** d'un élément de map (`&m[k]` ne compile pas), parce que la map peut déplacer ses éléments. Pour modifier un champ d'une struct stockée par valeur : relire, modifier, réécrire, ou stocker des pointeurs.
- Une map est une référence vers une structure interne : la passer à une fonction permet à celle-ci de la modifier.
- Une map ne **rétrécit jamais** sa mémoire après suppression des éléments ; pour libérer, recréer la map.
- **Implémentation** : depuis Go 1.24, les maps utilisent un algorithme de type **Swiss Table** (groupes de cases et métadonnées comparées en parallèle), plus rapide que l'ancienne implémentation à buckets et débordement.
- Package `maps` : `Clone`, `Copy`, `Equal`, `DeleteFunc`, et les itérateurs `maps.Keys`, `maps.Values`, `maps.All` (1.23).
- Un **ensemble** se fait avec `map[T]struct{}` (struct vide de taille zéro) ou `map[T]bool`.

**Chaînes** : une `string` est une séquence **immuable d'octets**, en pratique en UTF-8. En mémoire : un pointeur et une longueur.
- `len(s)` renvoie le nombre d'**octets**, pas de caractères : `len("é")` vaut 2.
- `s[i]` renvoie un **octet** (`byte`).
- `for i, r := range s` décode l'UTF-8 et parcourt les **runes** ; `i` est l'index en octets du début de la rune.
- Nombre de caractères : `utf8.RuneCountInString(s)` ; conversion `[]rune(s)` (alloue).
- Conversion `[]byte(s)` et `string(b)` copient en général les données (le compilateur optimise certains cas, comme une clé de map ou une comparaison).
- `string(65)` donne `"A"` (le caractère de code 65), pas `"65"` : `go vet` le signale. Pour convertir un nombre en texte : `strconv.Itoa`.
- **Concaténation** : `+` alloue une nouvelle chaîne à chaque fois ; en boucle, utiliser `strings.Builder` (ou `bytes.Buffer`).
- Les littéraux bruts entre accents graves `` `...` `` ne traitent pas les échappements (pratique pour les regex et le JSON).
- Packages : `strings` (Contains, Split, Fields, Join, Replace, TrimSpace, HasPrefix, Cut, EqualFold, Builder...), `strconv` (Atoi, Itoa, ParseInt, FormatFloat, Quote), `unicode`, `unicode/utf8`, `bytes` (équivalents pour `[]byte`).

**Structs** :
```go
type Utilisateur struct {
    ID     int64
    Nom    string
    Email  string `json:"email,omitempty"` // tag de struct
    actif  bool                            // non exporté
}
u := Utilisateur{ID: 1, Nom: "Ana"}        // champs nommés : préférable
p := &Utilisateur{Nom: "Bob"}              // pointeur vers une struct
p.Nom = "Bobby"                            // déréférencement automatique
```
- Une struct est une **valeur** : l'affecter la copie entièrement.
- Comparable avec `==` si tous ses champs le sont.
- **Tags** : métadonnées lues par réflexion (`json`, `db`, `validate`, `yaml`). Options JSON : `omitempty` (omet les valeurs "vides"), `omitzero` (Go 1.24, omet selon la valeur zéro réelle, y compris pour `time.Time` ou une méthode `IsZero`), `-` (ignorer), `string`.
- **Struct anonyme** : `struct{ X, Y int }{1, 2}`, pratique dans les tests table-driven.
- **Struct vide** `struct{}` : occupe 0 octet ; sert pour les ensembles et les signaux sur channel (`chan struct{}`).
- **Alignement** : le compilateur ajoute du *padding* pour aligner les champs ; ordonner les champs du plus grand au plus petit peut réduire la taille (l'analyseur `fieldalignment` le signale). Utile pour des structs allouées par millions.
- **Embedding** (composition) : voir le chapitre Interfaces & composition.

**Valeurs vs références, la synthèse** :
| Type | Copie lors d'une affectation ou d'un passage | Modification visible par l'appelant ? |
|---|---|---|
| int, string, struct, tableau | toute la valeur | non |
| slice | l'en-tête (ptr, len, cap) | les éléments oui ; un append qui réalloue non |
| map | une référence | oui |
| channel | une référence | oui (même canal) |
| pointeur | l'adresse | oui (la valeur pointée) |
| interface | (type, valeur ou pointeur) | dépend de la valeur contenue |

### En situation

- **"Après un append sur un sous-slice, l'original a changé"** → capacité partagée → expression à trois indices ou copie.
- **"Une fonction ajoute des éléments à un slice mais l'appelant ne les voit pas"** → append a réalloué, l'en-tête n'a pas été renvoyé → retourner le slice ou passer un pointeur.
- **"fatal error: concurrent map writes"** → map partagée entre goroutines sans verrou.
- **"panic: assignment to entry in nil map"** → map déclarée avec var mais jamais initialisée par make.
- **"La longueur de la chaîne est fausse avec des accents"** → len compte les octets.
- **"Les résultats sortent dans un ordre différent à chaque exécution"** → itération sur une map.
- **"Le service garde beaucoup de mémoire après avoir traité un gros fichier"** → sous-slice qui retient le gros tableau.

### Questions techniques

**Q : Tableau vs slice ?** R : Le tableau a une taille fixe incluse dans son type et se copie en entier ; le slice est une vue (pointeur, len, cap) sur un tableau.

**Q : Que contient un en-tête de slice ?** R : Un pointeur vers le tableau sous-jacent, la longueur et la capacité.

**Q : len vs cap ?** R : Nombre d'éléments accessibles vs place disponible dans le tableau sous-jacent.

**Q : Pourquoi écrire `s = append(s, x)` ?** R : append peut renvoyer un en-tête vers un nouveau tableau ; sans réaffectation on perd le résultat.

**Q : Comment croît la capacité lors d'un append ?** R : Environ un doublement pour les petits slices, puis une croissance plus progressive au-delà de 256 éléments.

**Q : Deux slices peuvent-ils partager des données ?** R : Oui, un sous-slice partage le tableau sous-jacent ; modifier l'un modifie l'autre.

**Q : À quoi sert `s[a:b:c]` ?** R : Limiter la capacité du sous-slice pour qu'un append réalloue au lieu d'écraser les données suivantes.

**Q : nil slice vs slice vide ?** R : Tous deux de longueur 0 et utilisables ; le nil vaut nil et devient null en JSON, le vide devient [].

**Q : Peut-on comparer deux slices avec == ?** R : Non, seulement à nil ; utiliser slices.Equal.

**Q : Comment supprimer un élément d'un slice ?** R : `append(s[:i], s[i+1:]...)` ou slices.Delete pour garder l'ordre ; échange avec le dernier pour O(1) sans ordre.

**Q : Pourquoi un sous-slice peut-il provoquer une fuite mémoire ?** R : Il retient tout le tableau sous-jacent ; copier la partie utile.

**Q : Que renvoie la lecture d'une clé absente d'une map ?** R : La valeur zéro du type ; l'idiome `v, ok := m[k]` distingue absent et zéro.

**Q : Que se passe-t-il si on écrit dans une map nil ?** R : Un panic ; la lecture renvoie la valeur zéro.

**Q : Quels types peuvent être clés de map ?** R : Les types comparables ; pas les slices, maps ni fonctions.

**Q : L'ordre d'itération d'une map est-il garanti ?** R : Non, il est volontairement aléatoire.

**Q : Une map est-elle thread-safe ?** R : Non ; un accès concurrent avec écriture provoque une erreur fatale non récupérable.

**Q : Pourquoi `&m[k]` ne compile-t-il pas ?** R : Les éléments d'une map peuvent être déplacés lors de la croissance.

**Q : Une map rend-elle sa mémoire après des delete ?** R : Non, elle ne rétrécit pas ; il faut la recréer.

**Q : Comment implémenter un ensemble ?** R : `map[T]struct{}`, la struct vide ne coûtant rien.

**Q : Quelle implémentation pour les maps depuis Go 1.24 ?** R : Une implémentation de type Swiss Table, plus rapide.

**Q : Que renvoie len("héllo") ?** R : 6 : len compte les octets, et "é" en occupe 2.

**Q : s[i] sur une chaîne renvoie quoi ?** R : Un octet (byte), pas un caractère.

**Q : Comment parcourir les caractères d'une chaîne ?** R : `for i, r := range s`, qui décode l'UTF-8 en runes.

**Q : Une string est-elle mutable ?** R : Non ; on passe par []byte ou strings.Builder.

**Q : Que vaut string(rune(65)) ?** R : "A" ; pour "65" il faut strconv.Itoa.

**Q : Comment concaténer efficacement en boucle ?** R : strings.Builder.

**Q : Une struct est-elle comparable ?** R : Oui si tous ses champs sont comparables.

**Q : À quoi servent les tags de struct ?** R : À fournir des métadonnées lues par réflexion (json, db, validate).

**Q : omitempty vs omitzero ?** R : omitempty omet les valeurs "vides" selon des règles fixes ; omitzero (Go 1.24) omet selon la vraie valeur zéro, y compris time.Time et IsZero().

**Q : Pourquoi l'ordre des champs d'une struct peut-il compter ?** R : Le padding d'alignement change la taille ; trier par taille décroissante la réduit.

**Q : Taille d'une struct{} ?** R : Zéro octet.

### Questions de situation

**Q : "Une fonction modifie un slice passé en paramètre. Parfois l'appelant voit le changement, parfois non."**
R : "C'est le comportement normal des slices. La fonction reçoit une **copie de l'en-tête** qui pointe vers le même tableau. Si elle modifie des éléments existants (`s[0] = x`), l'appelant le voit. Si elle fait un `append` et que la capacité suffit, elle écrit dans le tableau partagé, mais l'appelant ne voit pas la nouvelle longueur ; si la capacité ne suffit pas, `append` alloue un nouveau tableau et l'appelant ne voit rien du tout.
Règle claire pour l'API : soit la fonction **retourne** le slice modifié (`s = ajouter(s, x)`, comme `append`), soit elle reçoit un pointeur `*[]T`, soit elle ne fait que lire. J'éviterais les fonctions qui modifient silencieusement un slice reçu."

**Q : "Un cache en mémoire est partagé entre plusieurs goroutines et le service plante avec 'concurrent map writes'."**
R : "Les maps Go ne sont pas protégées, et le runtime détecte certains accès concurrents et arrête le programme, ce qui n'est pas récupérable. Je protégerais la map par un `sync.RWMutex` encapsulé dans une struct avec des méthodes `Get`/`Set`, pour que personne ne puisse accéder à la map sans verrou. Le `RWMutex` laisse plusieurs lectures simultanées.
`sync.Map` est une alternative pour des cas précis : clés écrites une fois puis lues souvent, ou goroutines qui manipulent des ensembles de clés disjoints. Pour un cache avec expiration et taille maximale, j'utiliserais une bibliothèque éprouvée (ristretto, otter) plutôt que de la réinventer.
Et je lancerais les tests avec `-race` pour trouver les autres accès non protégés."

**Q : "Tu dois compter les mots d'un fichier et les afficher triés par fréquence."**
R :
```go
compte := make(map[string]int)
scanner := bufio.NewScanner(f)
scanner.Split(bufio.ScanWords)
for scanner.Scan() {
    compte[strings.ToLower(scanner.Text())]++
}
mots := slices.Collect(maps.Keys(compte))    // Go 1.23+
slices.SortFunc(mots, func(a, b string) int {
    return cmp.Compare(compte[b], compte[a])  // fréquence décroissante
})
```
"La map compte en O(n) ; comme son ordre est aléatoire, j'extrais les clés et je les trie avec un comparateur. `bufio.Scanner` lit en flux, donc la mémoire dépend du nombre de mots distincts, pas de la taille du fichier. Attention à la taille de ligne maximale par défaut du Scanner (64 Ko), réglable avec `Buffer`."

═══════════════════════════════════════════
# 3. POINTEURS, MÉTHODES & RECEVEURS
═══════════════════════════════════════════

### Vue d'ensemble

Go a des pointeurs, mais sans arithmétique de pointeurs (sauf via `unsafe`) et avec un ramasse-miettes : ils servent à **partager** une valeur et à la **modifier** sans la copier, pas à manipuler la mémoire. Comme tout est passé par valeur, le choix entre une valeur et un pointeur est une décision de conception quotidienne : pour un paramètre, pour un champ, et surtout pour le **receveur** d'une méthode.

Ce choix a des conséquences qui tombent souvent en entretien : quelle méthode fait partie de quel *method set*, pourquoi un type n'implémente pas une interface alors qu'il "a la méthode", et pourquoi une méthode à receveur valeur "ne modifie rien".

### Cours

**Pointeurs, les bases** :
```go
x := 10
p := &x        // p est un *int
*p = 20        // x vaut 20
var q *int     // nil
u := &User{}   // pointeur vers une struct allouée
n := new(int)  // pointeur vers un int initialisé à 0
```
- Déréférencer un pointeur `nil` provoque un panic (`invalid memory address or nil pointer dereference`).
- Pour les structs, `p.Champ` déréférence automatiquement (pas de `->`).
- On peut renvoyer l'adresse d'une variable locale : `return &User{}` est sûr, le compilateur place la variable dans le **heap** si elle s'échappe de la fonction (*escape analysis*).
- **`new(expr)`** (Go 1.26) : `new` accepte désormais une expression et crée une variable initialisée avec sa valeur, par exemple `Age: new(calculerAge(d))`. Très pratique pour les champs optionnels représentés par des pointeurs (JSON, Protobuf). Avant, il fallait une variable intermédiaire ou une fonction utilitaire `func ptr[T any](v T) *T { return &v }`.

**Quand utiliser un pointeur** :
- pour **modifier** la valeur de l'appelant ;
- pour éviter de copier une **grosse** struct (ordre de grandeur : au-delà de quelques centaines d'octets, à mesurer) ;
- pour représenter l'**absence** (`nil`) d'une valeur optionnelle ;
- quand la valeur **ne doit pas être copiée** (une struct contenant un `sync.Mutex`, un `bytes.Buffer`, ou qui représente une identité).

**Quand préférer une valeur** : petites structs immuables (un point, une date, un montant), types de base, et en général tant qu'il n'y a pas de raison d'utiliser un pointeur. Une valeur sur la pile ne coûte rien au GC ; un pointeur peut forcer une allocation dans le heap. "Un pointeur est plus rapide" est un mythe pour les petites structs.

**Méthodes** : une fonction avec un **receveur**.
```go
type Compte struct{ solde int }

func (c Compte) Solde() int       { return c.solde }   // receveur valeur
func (c *Compte) Deposer(n int)   { c.solde += n }     // receveur pointeur
```
- On peut définir des méthodes sur **tout type nommé défini dans le même package** (`type Celsius float64` peut avoir des méthodes), pas sur un type d'un autre package ni sur un type pointeur ou interface.
- **Receveur valeur** : la méthode reçoit une **copie** ; modifier `c` n'affecte pas l'original.
- **Receveur pointeur** : la méthode peut modifier l'original et évite la copie.
- **Appel pratique** : sur une variable **adressable**, Go insère automatiquement `&` ou `*` : `c.Deposer(5)` fonctionne même si `c` est une valeur, car Go écrit `(&c).Deposer(5)`. Cela ne marche pas sur une valeur non adressable, comme un élément de map ou le résultat d'une fonction.
- **Cohérence** : si une méthode du type a besoin d'un receveur pointeur, on met généralement **toutes** les méthodes en pointeur (recommandation officielle).
- Un receveur pointeur peut être `nil` et la méthode être appelée quand même : on peut gérer ce cas explicitement (`if c == nil { return 0 }`), technique utilisée pour des types dont la valeur nil a un sens.

**Method sets**, la règle qui détermine l'implémentation des interfaces :
| Type | Méthodes qui font partie de son method set |
|---|---|
| `T` | les méthodes à receveur `T` uniquement |
| `*T` | les méthodes à receveur `T` **et** `*T` |

Conséquence : si `Deposer` a un receveur `*Compte`, alors `Compte` (valeur) **n'implémente pas** une interface qui exige `Deposer` ; seul `*Compte` l'implémente.
```go
type Depositaire interface{ Deposer(int) }
var d Depositaire = Compte{}   // erreur : Deposer a un receveur pointeur
var d2 Depositaire = &Compte{} // OK
```
La raison : une valeur stockée dans une interface n'est pas adressable, Go ne peut donc pas prendre son adresse pour appeler une méthode à receveur pointeur.

**Valeurs de méthode et expressions de méthode** : `f := c.Deposer` crée une fonction liée à `c` (le receveur est capturé au moment de l'expression) ; `g := (*Compte).Deposer` est une fonction dont le premier paramètre est le receveur.

**Copier une struct qui contient un verrou** : copier un `sync.Mutex` copie son état, et les deux copies deviennent indépendantes, ce qui casse la synchronisation. `go vet` (analyseur `copylocks`) signale ces copies. D'où : receveur pointeur pour les types qui contiennent un `Mutex`, et ne jamais les passer par valeur.

**Escape analysis** : le compilateur décide si une variable peut vivre sur la **pile** (gratuite, libérée au retour de la fonction) ou doit aller dans le **heap** (gérée par le GC). Une variable s'échappe si son adresse survit à la fonction (retournée, stockée dans une structure globale, capturée par une goroutine), si sa taille n'est pas connue à la compilation, ou si elle est stockée dans une interface dans certains cas. Voir les décisions : `go build -gcflags=-m` (`moved to heap`, `escapes to heap`). Depuis Go 1.26, le compilateur peut placer sur la pile le tableau de davantage de slices de taille variable.

**unsafe** : `unsafe.Pointer` et `unsafe.Sizeof`, `unsafe.Slice`, `unsafe.String` permettent de contourner le système de types (interop, optimisations extrêmes). Hors bibliothèques très spécialisées, on ne s'en sert pas ; le code n'est plus garanti portable ni compatible.

**Un modèle simple** : pas de pointeurs vers des éléments de map, pas d'arithmétique, pas de références nulles "magiques". Un pointeur pointe toujours vers une variable valide ou vaut nil.

### En situation

- **"Ma méthode ne modifie pas la struct"** → receveur valeur, elle travaille sur une copie.
- **"Mon type n'implémente pas l'interface alors qu'il a la méthode"** → la méthode a un receveur pointeur et on utilise une valeur.
- **"panic: nil pointer dereference"** → un pointeur non initialisé, souvent un champ de struct ou une erreur ignorée qui laisse un résultat nil.
- **"Le verrou ne protège plus rien"** → struct contenant un Mutex copiée (receveur valeur, passage par valeur).
- **"Trop d'allocations dans un chemin critique"** → des pointeurs qui font échapper des valeurs → `-gcflags=-m`.

### Questions techniques

**Q : Go a-t-il de l'arithmétique de pointeurs ?** R : Non, sauf via le package unsafe.

**Q : Peut-on retourner l'adresse d'une variable locale ?** R : Oui, le compilateur la place dans le heap si elle s'échappe.

**Q : Que fait `new(T)` ?** R : Alloue une variable de type T initialisée à sa valeur zéro et renvoie son adresse.

**Q : Qu'apporte Go 1.26 à new ?** R : new accepte une expression : `new(f())` crée une variable initialisée avec cette valeur.

**Q : `new(T)` vs `&T{}` ?** R : Équivalents pour une struct ; &T{} permet d'initialiser des champs.

**Q : Que se passe-t-il en déréférençant un pointeur nil ?** R : Un panic à l'exécution.

**Q : Receveur valeur vs pointeur ?** R : Copie (pas de modification de l'original) vs accès à l'original (modification possible, pas de copie).

**Q : Quand choisir un receveur pointeur ?** R : Pour modifier le receveur, pour une grosse struct, pour un type non copiable (mutex), ou par cohérence si une autre méthode l'est.

**Q : Faut-il mélanger receveurs valeur et pointeur sur un même type ?** R : À éviter ; on garde la même sorte pour toutes les méthodes.

**Q : Quel est le method set de T et de *T ?** R : T : méthodes à receveur valeur ; *T : méthodes à receveur valeur et pointeur.

**Q : Pourquoi une valeur T ne satisfait-elle pas une interface dont une méthode a un receveur *T ?** R : Une valeur dans une interface n'est pas adressable, Go ne peut pas prendre son adresse.

**Q : Pourquoi `c.Deposer(5)` fonctionne-t-il sur une valeur ?** R : Go prend automatiquement l'adresse d'une variable adressable.

**Q : Peut-on appeler une méthode à receveur pointeur sur un élément de map ?** R : Non, un élément de map n'est pas adressable.

**Q : Peut-on définir une méthode sur un type d'un autre package ?** R : Non ; il faut définir un type nommé local basé dessus.

**Q : Peut-on appeler une méthode sur un pointeur nil ?** R : Oui pour une méthode à receveur pointeur, qui peut tester `c == nil`.

**Q : Pourquoi ne pas copier une struct contenant un sync.Mutex ?** R : La copie duplique l'état du verrou ; go vet (copylocks) le signale.

**Q : C'est quoi l'escape analysis ?** R : L'analyse du compilateur qui décide si une variable va sur la pile ou dans le heap.

**Q : Comment voir les décisions d'échappement ?** R : `go build -gcflags=-m`.

**Q : Un pointeur est-il toujours plus rapide qu'une valeur ?** R : Non : il peut forcer une allocation dans le heap et charger le GC ; pour une petite struct, la valeur est souvent meilleure.

**Q : Valeur de méthode vs expression de méthode ?** R : `x.M` lie le receveur ; `T.M` ou `(*T).M` est une fonction qui prend le receveur en premier paramètre.

### Questions de situation

**Q : "Tu conçois un type `Money` (montant + devise). Valeur ou pointeur ?"**
R : "Valeur. C'est une petite struct, conceptuellement **immuable**, comparable avec `==`, sans identité propre : deux montants de 10 EUR sont interchangeables. Les méthodes `Add`, `Sub` renvoient un nouveau `Money` plutôt que de modifier le receveur, ce qui supprime toute question de partage et de concurrence.
Pour le montant lui-même, un `int64` en centimes (ou une bibliothèque décimale) et jamais un `float64`. Un pointeur ne se justifierait que pour représenter un montant optionnel, et encore, un booléen explicite est souvent plus lisible."

**Q : "Le linter signale 'copylocks' sur ta struct de cache. Pourquoi est-ce grave ?"**
R : "Ma struct contient un `sync.Mutex` et je la passe ou la retourne par valeur, ou une de ses méthodes a un receveur valeur. Chaque copie a son propre état de verrou : deux goroutines peuvent alors 'verrouiller' deux copies différentes et accéder en même temps aux mêmes données sous-jacentes (la map, partagée par référence). La protection est perdue sans aucune erreur visible.
Correction : receveurs pointeurs pour toutes les méthodes, construction par une fonction `New...` qui renvoie un `*Cache`, et jamais de copie de la struct. On peut aussi y ajouter un champ `noCopy` (convention utilisée par la bibliothèque standard) pour que vet le détecte encore mieux."

═══════════════════════════════════════════
# 4. INTERFACES & COMPOSITION
═══════════════════════════════════════════

### Vue d'ensemble

Go n'a ni classes ni héritage. Sa réponse à la réutilisation et au polymorphisme tient en deux idées : la **composition** par *embedding* (intégrer un type dans un autre) et les **interfaces implicites** (un type implémente une interface dès qu'il possède ses méthodes, sans le déclarer). Cette implicité est le cœur du design Go : c'est le code **consommateur** qui définit l'interface dont il a besoin, et les interfaces restent **petites**, souvent une seule méthode (`io.Reader`, `io.Writer`, `fmt.Stringer`, `error`).

En entretien, trois sujets reviennent systématiquement : la différence avec les interfaces Java, le piège de l'**interface nil qui n'est pas nil**, et la maxime **"accept interfaces, return structs"**.

### Cours

**Déclaration et implémentation implicite** :
```go
type Notifier interface {
    Notify(ctx context.Context, msg string) error
}

type EmailNotifier struct{ /* ... */ }
func (e *EmailNotifier) Notify(ctx context.Context, msg string) error { /* ... */ return nil }

var n Notifier = &EmailNotifier{}   // aucun "implements"
```
Avantages : découplage total (le package qui fournit `EmailNotifier` ne connaît même pas l'interface), possibilité de définir une interface **après coup** pour un type tiers, mocks triviaux en test.

**Les interfaces de la bibliothèque standard à connaître** : `error` (`Error() string`), `fmt.Stringer` (`String() string`), `io.Reader` (`Read(p []byte) (n int, err error)`), `io.Writer`, `io.Closer`, `io.ReadWriter`, `io.ReadCloser` (composées par embedding d'interfaces), `sort.Interface` (`Len`, `Less`, `Swap`), `http.Handler` (`ServeHTTP`), `context.Context`, `json.Marshaler`/`Unmarshaler`, `encoding.TextMarshaler`.

**Composer des interfaces** :
```go
type ReadWriter interface {
    io.Reader
    io.Writer
}
```

**Interface vide et any** : `interface{}`, alias `any` depuis Go 1.18, est satisfaite par **tous** les types. À utiliser avec modération : elle supprime la vérification de types. Avec les generics, beaucoup d'usages d'`any` deviennent inutiles.

**Représentation interne** : une valeur d'interface est une paire **(type dynamique, valeur dynamique)** : en pratique deux mots, un pointeur vers les informations de type (et la table des méthodes, l'*itab*) et un pointeur vers les données. Un appel de méthode via une interface est un appel **indirect** (dispatch dynamique), un peu plus coûteux et qui empêche souvent l'inlining.

**Le piège de l'interface nil** : une interface vaut `nil` **seulement si son type ET sa valeur sont nil**.
```go
func trouver() error {
    var e *MonErreur = nil
    return e            // l'interface contient (type *MonErreur, valeur nil)
}
err := trouver()
fmt.Println(err == nil) // false !
```
L'interface a un type dynamique, donc elle n'est pas nil, même si le pointeur à l'intérieur l'est. Règle : dans une fonction qui retourne `error`, **retourner explicitement `nil`**, jamais une variable de type pointeur concret qui pourrait être nil.

**Type assertion** : récupérer le type concret.
```go
s, ok := v.(string)      // forme sûre : ok vaut false si ce n'est pas un string
s := v.(string)          // panic si ce n'est pas un string
w, ok := r.(io.WriterTo) // tester si la valeur implémente aussi une autre interface
```

**Type switch** :
```go
switch x := v.(type) {
case int:
    fmt.Println("entier", x+1)
case string, []byte:
    fmt.Println("texte", x)   // x est de type any ici (plusieurs types)
case nil:
    fmt.Println("nil")
default:
    fmt.Printf("autre %T\n", x)
}
```

**Interfaces optionnelles (mise à niveau de capacités)** : tester à l'exécution si une valeur offre une capacité supplémentaire, comme `io.Copy` qui vérifie si la source implémente `io.WriterTo` ou la destination `io.ReaderFrom` pour éviter une copie intermédiaire, ou `http.Flusher` sur un `ResponseWriter`.

**Embedding de structs (composition)** :
```go
type Logger struct{ prefix string }
func (l *Logger) Log(msg string) { fmt.Println(l.prefix, msg) }

type Service struct {
    *Logger            // champ embarqué
    nom string
}
s := Service{Logger: &Logger{prefix: "[svc]"}, nom: "api"}
s.Log("démarré")       // méthode "promue"
```
- Les champs et méthodes du type embarqué sont **promus** : accessibles directement sur le type englobant, et ils comptent pour l'implémentation des interfaces.
- Ce **n'est pas de l'héritage** : il n'y a pas de polymorphisme. Si `Logger.Log` appelle `l.Format()`, c'est toujours `Logger.Format` qui est appelée, même si `Service` définit sa propre `Format`. Le receveur de la méthode promue reste le `Logger` embarqué.
- Le type englobant peut **masquer** une méthode promue en définissant la sienne (*shadowing*).
- Conflits : deux types embarqués qui exposent le même nom au même niveau créent une ambiguïté, erreur seulement si on utilise ce nom.
- **Attention à l'API publique** : embarquer un type exporte toutes ses méthodes (embarquer un `sync.Mutex` dans une struct exportée expose `Lock`/`Unlock` aux utilisateurs). Préférer un champ nommé `mu sync.Mutex`.
- On peut aussi embarquer une **interface** dans une struct (utile pour les décorateurs ou pour les mocks qui n'implémentent qu'une partie des méthodes ; attention, appeler une méthode non implémentée provoque un panic sur nil).

**Principes de conception idiomatiques** :
- **"The bigger the interface, the weaker the abstraction"** (Rob Pike) : une interface d'une ou deux méthodes est facile à implémenter, à composer et à mocker.
- **Définir l'interface du côté du consommateur** : le package qui *utilise* une dépendance déclare l'interface minimale dont il a besoin ; le package qui *fournit* exporte un type concret.
- **"Accept interfaces, return structs"** : une fonction accepte l'interface la plus petite possible (flexible pour l'appelant) et renvoie un type concret (l'appelant accède à tout et peut définir sa propre interface). Exception : retourner une interface quand on veut cacher l'implémentation (`error`, `hash.Hash`) ou proposer plusieurs implémentations.
- **Ne pas créer d'interface avant d'en avoir besoin** : pas d'interface "au cas où" ; on l'extrait quand une deuxième implémentation ou un test l'exige.
- Pas de nommage `IUser` ou `UserInterface` : une interface d'une méthode se nomme en ajoutant *-er* (`Reader`, `Notifier`, `Validator`).

**Vérification à la compilation** :
```go
var _ Notifier = (*EmailNotifier)(nil)
```

**Comparer des interfaces** : `==` compare type et valeur dynamiques ; si le type dynamique n'est pas comparable (un slice), la comparaison **panique** à l'exécution.

**Interfaces et generics** : une interface peut servir de **contrainte** de type (voir le chapitre Generics). Une interface avec une liste de types (`int | float64`) ne peut être utilisée que comme contrainte, pas comme type de variable.

### En situation

- **"err != nil alors que la fonction renvoie un pointeur nil"** → interface non nil contenant un pointeur nil.
- **"Comment mocker cette dépendance ?"** → définir une petite interface côté consommateur et injecter une fausse implémentation.
- **"Ma méthode redéfinie dans la struct englobante n'est pas appelée par la méthode du type embarqué"** → l'embedding n'est pas de l'héritage.
- **"Les utilisateurs de mon package appellent Lock() sur ma struct"** → Mutex embarqué dans une struct exportée.
- **"panic: interface conversion"** → type assertion sans la forme `, ok`.

### Questions techniques

**Q : Comment un type implémente-t-il une interface en Go ?** R : Implicitement, en possédant toutes ses méthodes, sans mot-clé implements.

**Q : Avantages des interfaces implicites ?** R : Découplage, interfaces définies après coup ou par le consommateur, mocks simples.

**Q : Que représente `any` ?** R : Un alias de interface{}, satisfait par tous les types (Go 1.18).

**Q : Comment est représentée une valeur d'interface ?** R : Une paire (type dynamique, valeur dynamique), deux mots en mémoire.

**Q : Quand une interface vaut-elle nil ?** R : Seulement quand son type et sa valeur sont tous deux nil.

**Q : Pourquoi `err != nil` peut-il être vrai avec un pointeur nil dedans ?** R : L'interface porte un type dynamique non nil.

**Q : Comment éviter ce piège ?** R : Retourner explicitement nil dans les fonctions qui renvoient une interface.

**Q : Type assertion vs type switch ?** R : Vérifier un seul type (`v.(T)`) vs brancher sur plusieurs types.

**Q : Que se passe-t-il si une type assertion échoue ?** R : Panic avec la forme simple ; ok = false avec la forme `v, ok := x.(T)`.

**Q : C'est quoi une interface optionnelle ?** R : Une capacité testée à l'exécution par type assertion, comme io.WriterTo dans io.Copy.

**Q : L'embedding est-il de l'héritage ?** R : Non : les méthodes sont promues mais il n'y a pas de polymorphisme, le receveur reste le type embarqué.

**Q : Que sont les méthodes promues ?** R : Les méthodes du type embarqué accessibles directement sur le type englobant.

**Q : Pourquoi éviter d'embarquer un sync.Mutex dans une struct exportée ?** R : Lock et Unlock deviennent publics ; préférer un champ nommé.

**Q : Que signifie "accept interfaces, return structs" ?** R : Accepter l'interface minimale en paramètre, renvoyer un type concret.

**Q : Où définir une interface ?** R : Du côté du consommateur, là où elle est utilisée.

**Q : Pourquoi des interfaces petites ?** R : Plus faciles à implémenter, composer et mocker ; une grosse interface est une abstraction faible.

**Q : Comment nommer une interface d'une méthode ?** R : Avec le suffixe -er : Reader, Writer, Notifier.

**Q : Comment vérifier statiquement qu'un type implémente une interface ?** R : `var _ I = (*T)(nil)`.

**Q : Comparer deux interfaces peut-il paniquer ?** R : Oui si le type dynamique n'est pas comparable (slice, map).

**Q : Le dispatch par interface a-t-il un coût ?** R : Un appel indirect qui empêche souvent l'inlining ; négligeable hors boucles critiques.

**Q : Une interface avec union de types peut-elle typer une variable ?** R : Non, seulement servir de contrainte de generic.

**Q : Quelles interfaces composent io.ReadWriteCloser ?** R : io.Reader, io.Writer, io.Closer.

**Q : Quelle interface implémente un handler HTTP ?** R : http.Handler, avec ServeHTTP(ResponseWriter, *Request).

### Questions de situation

**Q : "Ton service appelle une API de paiement externe. Comment le rendre testable ?"**
R : "Dans le package métier qui en a besoin, je déclare une petite interface qui décrit uniquement ce que j'utilise :
```go
type Paiements interface {
    Debiter(ctx context.Context, compte string, montant int64) (string, error)
}
```
Le service reçoit cette interface par son constructeur. L'adaptateur réel (le client HTTP de l'API) vit dans un package d'infrastructure et la satisfait implicitement. En test, j'injecte une fausse implémentation écrite à la main, une simple struct avec des champs pour les réponses et les appels enregistrés, ou un mock généré si les interactions sont complexes.
C'est l'inverse du réflexe Java : je ne crée pas une interface dans le package du client pour 'exposer' l'API, c'est le consommateur qui la définit, à sa mesure."

**Q : "Une fonction de validation retourne `*ValidationError` quand il n'y a pas d'erreur, et les appelants voient une erreur."**
R : "La fonction a une signature du type `func valider() error` mais retourne une variable `var verr *ValidationError` restée nil. L'interface `error` retournée contient le type `*ValidationError` et une valeur nil : elle n'est donc pas nil, et `err != nil` est vrai.
Correction : retourner explicitement `nil` quand il n'y a pas d'erreur, ou déclarer la variable locale avec le type `error`. Je relirais aussi les autres fonctions du même style ; `staticcheck` et l'analyseur `nilness` aident à détecter ces cas."

**Q : "Comment tu réutilises du comportement entre plusieurs types sans héritage ?"**
R : "Plusieurs outils selon le besoin. La **composition explicite** : le type contient un champ du type réutilisé et délègue. L'**embedding** quand je veux réellement exposer toutes les méthodes du composant. Des **fonctions** libres, souvent génériques, qui travaillent sur une interface ou une contrainte plutôt que des méthodes dupliquées. Et le **pattern décorateur** : une struct qui implémente la même interface en enveloppant une autre implémentation (un `Notifier` qui ajoute des retries autour d'un autre `Notifier`).
Ce que je m'interdis, c'est de recréer une hiérarchie de 'classes de base' avec de l'embedding sur plusieurs niveaux : c'est difficile à lire et l'absence de polymorphisme surprend."

═══════════════════════════════════════════
# 5. GESTION DES ERREURS : ERROR, WRAPPING, PANIC & RECOVER
═══════════════════════════════════════════

### Vue d'ensemble

Go n'a pas d'exceptions. Une erreur est une **valeur** ordinaire, retournée comme dernier résultat d'une fonction, que l'appelant doit traiter explicitement. Le fameux `if err != nil { return err }` est souvent moqué pour sa verbosité, mais il a une vertu : chaque chemin d'échec est **visible dans le code**, à l'endroit où il se produit, sans saut invisible à travers la pile.

L'équipe Go a étudié pendant des années des propositions de syntaxe plus courte (`try`, `?`), puis a annoncé en 2025 qu'elle arrêtait de chercher un changement de syntaxe pour la gestion d'erreurs : le style actuel est là pour durer. Ce qui a évolué, c'est l'outillage : **wrapping** des erreurs avec `%w`, inspection avec `errors.Is` et `errors.As` (et `errors.AsType`, générique, en Go 1.26), et combinaison avec `errors.Join`.

`panic` existe, mais il est réservé aux **bugs** et aux situations irrécupérables, pas au contrôle de flux.

### Cours

**L'interface error** :
```go
type error interface {
    Error() string
}
```
Création : `errors.New("message")`, `fmt.Errorf("format %d", x)`.

**Conventions** :
- l'erreur est le **dernier** résultat ;
- on vérifie l'erreur **avant** d'utiliser les autres résultats (qui sont souvent des valeurs zéro ou invalides en cas d'erreur) ;
- messages en **minuscules**, sans ponctuation finale (ils seront concaténés : `"lecture config: ouverture fichier: permission denied"`) ;
- on **traite** une erreur une seule fois : soit on la gère (log, valeur par défaut, retry), soit on la **propage** avec du contexte, mais on ne fait pas les deux (logger puis retourner crée des doublons dans les logs).

**Wrapping** (Go 1.13) : ajouter du contexte en conservant l'erreur d'origine.
```go
if err != nil {
    return fmt.Errorf("charger utilisateur %d: %w", id, err)
}
```
`%w` enveloppe l'erreur (elle reste inspectable) ; `%v` l'intègre seulement au texte (la chaîne d'origine est perdue pour `errors.Is`). Depuis Go 1.20, plusieurs `%w` sont possibles dans un même `Errorf`. Depuis Go 1.27, `go vet` signale l'enveloppement d'un pointeur `*E` quand c'est le type `E` qui implémente `error`, source de faux négatifs avec `errors.Is`.

**Erreurs sentinelles** : des valeurs exportées comparables, pour des conditions attendues.
```go
var ErrNotFound = errors.New("not found")

if errors.Is(err, ErrNotFound) { /* 404 */ }
```
Exemples dans la bibliothèque standard : `io.EOF`, `sql.ErrNoRows`, `os.ErrNotExist`, `context.Canceled`, `context.DeadlineExceeded`, `http.ErrServerClosed`. **Ne pas comparer avec `==`** une erreur potentiellement enveloppée : `errors.Is` parcourt toute la chaîne.

**Types d'erreur personnalisés** : quand l'appelant a besoin de **données** sur l'erreur.
```go
type ValidationError struct {
    Champ  string
    Raison string
}
func (e *ValidationError) Error() string { return e.Champ + ": " + e.Raison }

var verr *ValidationError
if errors.As(err, &verr) {
    fmt.Println("champ invalide :", verr.Champ)
}
// Go 1.26 : version générique, plus sûre et plus rapide
if verr, ok := errors.AsType[*ValidationError](err); ok { /* ... */ }
```

**Personnaliser Is et As** : un type d'erreur peut implémenter `Is(target error) bool` (pour qu'une erreur corresponde à une sentinelle selon une règle propre), `As(any) bool`, et doit implémenter `Unwrap() error` (ou `Unwrap() []error` pour plusieurs causes) pour participer à la chaîne.

**errors.Join** (Go 1.20) : combiner plusieurs erreurs en une seule (par exemple les erreurs de validation de plusieurs champs, ou les erreurs de fermeture de plusieurs ressources). `errors.Is` et `errors.As` cherchent dans toutes les erreurs jointes.

**Erreurs opaques et comportement** : plutôt que d'exposer de nombreux types, on peut exposer un **comportement** testable via une interface, par exemple `interface{ Timeout() bool }` (implémentée par les erreurs réseau). Le client teste le comportement et non le type exact.

**Choisir entre les approches** :
| Besoin de l'appelant | Approche |
|---|---|
| savoir qu'une condition précise s'est produite | erreur sentinelle + `errors.Is` |
| récupérer des informations structurées | type d'erreur + `errors.As` / `errors.AsType` |
| seulement savoir que ça a échoué | erreur opaque enveloppée avec contexte |
| savoir si on peut réessayer | interface de comportement |

**Exposer ou non les erreurs d'une dépendance** : envelopper avec `%w` rend l'erreur de la dépendance (par exemple `sql.ErrNoRows`) partie de **votre API**. Si vous ne voulez pas que les appelants en dépendent, traduisez-la en votre propre sentinelle (`ErrNotFound`) ou utilisez `%v`.

**Erreurs ignorées** : ignorer une erreur doit être **explicite** (`_ = f.Close()`) et justifié. Le linter `errcheck` (inclus dans golangci-lint) signale les erreurs non vérifiées. Cas classique : `defer f.Close()` sur un fichier **écrit**, dont l'erreur de fermeture peut signaler une écriture perdue ; la récupérer via un retour nommé :
```go
func ecrire(path string) (err error) {
    f, err := os.Create(path)
    if err != nil { return err }
    defer func() {
        if cerr := f.Close(); cerr != nil && err == nil {
            err = cerr
        }
    }()
    // ... écritures
    return nil
}
```

**panic** : arrête le flux normal, exécute les `defer` de chaque fonction en remontant la pile, puis termine le programme avec une trace si personne ne le récupère. Déclenché par le runtime (index hors limites, nil pointer, division entière par zéro, map nil, type assertion simple échouée, channel fermé deux fois) ou par `panic(v)`.

Quand paniquer volontairement :
- une **erreur de programmation** (invariant violé, cas "impossible") ;
- une initialisation qui ne peut pas échouer en conditions normales : convention des fonctions `Must...` (`regexp.MustCompile`, `template.Must`) utilisées au démarrage.

Quand **ne pas** paniquer : toute erreur attendue (entrée invalide, réseau, fichier absent). Une bibliothèque ne doit pas paniquer à travers son API publique.

**recover** : récupère un panic, **uniquement** depuis une fonction différée, dans la **même goroutine**.
```go
func protege(f func()) (err error) {
    defer func() {
        if r := recover(); r != nil {
            err = fmt.Errorf("panic récupéré: %v", r)
        }
    }()
    f()
    return nil
}
```
- Un panic dans une **autre goroutine** ne peut pas être récupéré depuis la goroutine principale : il tue tout le programme. Chaque goroutine lancée dans un serveur doit avoir son propre `recover` si elle exécute du code potentiellement fautif.
- `net/http` récupère les panics de chaque handler (et logge), pour qu'un handler fautif ne tue pas le serveur ; les frameworks ajoutent un middleware de recovery.
- Certaines erreurs **fatales** ne sont pas récupérables : accès concurrent à une map, deadlock de toutes les goroutines (`all goroutines are asleep`), dépassement de pile, mémoire épuisée.
- Depuis Go 1.21, `panic(nil)` provoque un `*runtime.PanicNilError`, donc `recover()` ne renvoie plus nil dans ce cas.

**Stack traces** : les erreurs Go n'ont **pas** de stack trace par défaut. Le contexte vient du wrapping (`"charger commande: requête sql: ..."`) qui raconte le chemin. Des bibliothèques (`github.com/pkg/errors`, archivée, ou `cockroachdb/errors`) ajoutent des traces ; la plupart des projets modernes se contentent du wrapping et du tracing distribué.

**Erreurs et logs structurés** : avec `log/slog`, journaliser l'erreur comme attribut (`"err", err`) au point où elle est **traitée**, souvent dans le handler HTTP ou la boucle principale.

### En situation

- **"errors.Is renvoie false alors que c'est bien cette erreur"** → enveloppée avec `%v` au lieu de `%w`, ou comparée avec `==` après wrapping.
- **"La même erreur apparaît cinq fois dans les logs"** → chaque couche logge puis retourne.
- **"Le service redémarre à cause d'un panic dans une goroutine"** → pas de recover dans la goroutine.
- **"Des données manquent dans le fichier écrit, aucune erreur"** → erreur de Close ignorée.
- **"Comment renvoyer un 404 depuis la couche de données ?"** → sentinelle métier traduite par le handler.

### Questions techniques

**Q : Comment Go gère-t-il les erreurs ?** R : Par des valeurs de type error retournées en dernier résultat, vérifiées explicitement.

**Q : Que contient l'interface error ?** R : Une seule méthode, Error() string.

**Q : Pourquoi pas d'exceptions en Go ?** R : Pour rendre chaque chemin d'échec visible et explicite, sans saut caché dans la pile.

**Q : %w vs %v dans fmt.Errorf ?** R : %w enveloppe et garde l'erreur inspectable par errors.Is/As ; %v ne garde que le texte.

**Q : Peut-on utiliser plusieurs %w ?** R : Oui depuis Go 1.20.

**Q : C'est quoi une erreur sentinelle ?** R : Une variable d'erreur exportée et comparable, comme io.EOF ou sql.ErrNoRows.

**Q : Pourquoi errors.Is plutôt que == ?** R : errors.Is parcourt toute la chaîne d'erreurs enveloppées.

**Q : errors.Is vs errors.As ?** R : Tester l'identité à une valeur vs extraire une erreur d'un type donné.

**Q : Qu'apporte errors.AsType ?** R : Une version générique d'errors.As (Go 1.26), plus sûre et plus rapide.

**Q : À quoi sert errors.Join ?** R : Combiner plusieurs erreurs en une ; Is et As cherchent dans toutes.

**Q : Comment un type d'erreur participe-t-il à la chaîne ?** R : En implémentant Unwrap() error (ou Unwrap() []error).

**Q : Pourquoi les messages d'erreur sont-ils en minuscules ?** R : Ils sont concaténés avec d'autres contextes.

**Q : Faut-il logger une erreur et la retourner ?** R : Non, on la traite une fois : soit on la gère, soit on la propage.

**Q : Envelopper une erreur de dépendance avec %w a-t-il un inconvénient ?** R : Elle devient partie de votre API publique ; traduire en sentinelle propre si on veut l'isoler.

**Q : Pourquoi vérifier l'erreur de Close sur un fichier écrit ?** R : Elle peut signaler que des données n'ont pas été écrites.

**Q : Quand utiliser panic ?** R : Pour des bugs et invariants violés, ou des initialisations Must au démarrage ; jamais pour des erreurs attendues.

**Q : Que déclenche un panic au niveau du runtime ?** R : Index hors limites, nil pointer, division entière par zéro, écriture dans une map nil, assertion échouée, fermeture double d'un channel.

**Q : Où appeler recover ?** R : Dans une fonction différée de la goroutine qui panique.

**Q : Peut-on récupérer le panic d'une autre goroutine ?** R : Non ; il termine tout le programme.

**Q : Toutes les erreurs du runtime sont-elles récupérables ?** R : Non : accès concurrent aux maps, deadlock global, stack overflow et manque de mémoire sont fatals.

**Q : net/http récupère-t-il les panics ?** R : Oui, par handler, en loggant et en fermant la connexion.

**Q : Les erreurs Go ont-elles une stack trace ?** R : Non par défaut ; le contexte vient du wrapping.

**Q : C'est quoi la convention Must ?** R : Une fonction qui panique en cas d'erreur, pour les initialisations au démarrage (regexp.MustCompile).

**Q : Comment tester qu'une erreur est un timeout ?** R : Avec une interface de comportement (Timeout() bool) testée par errors.As, ou errors.Is(err, context.DeadlineExceeded).

**Q : Que fait panic(nil) depuis Go 1.21 ?** R : Il lève un *runtime.PanicNilError, recover ne renvoie plus nil.

### Questions de situation

**Q : "Comment tu structures la gestion d'erreurs entre la couche base de données, la couche métier et l'API HTTP ?"**
R : "Chaque couche parle son propre langage d'erreurs. La couche de **données** traduit les erreurs techniques en erreurs du domaine : `sql.ErrNoRows` devient `ErrNotFound` ; une violation de contrainte d'unicité devient `ErrDuplicate`. Le reste est enveloppé avec du contexte (`fmt.Errorf("insérer commande: %w", err)`).
La couche **métier** ajoute ses propres erreurs (`ErrStockInsuffisant`, ou un `*ValidationError` avec les champs) et propage les autres en ajoutant du contexte.
La couche **HTTP** est le seul endroit où l'on décide du code de statut et où l'on **logge** : un mapping avec `errors.Is` et `errors.As` (404, 409, 422), et tout le reste en 500 avec un message générique au client et l'erreur complète dans les logs avec l'identifiant de requête.
Ainsi les erreurs ne sont loggées qu'une fois, le client ne voit jamais de détail interne, et la couche métier ignore tout de HTTP."

**Q : "Un worker lancé avec `go traiter(job)` panique de temps en temps et fait tomber tout le service."**
R : "Un panic non récupéré dans n'importe quelle goroutine termine le processus, et le recover d'un middleware HTTP ne protège que la goroutine du handler. Je mettrais un `defer` avec `recover` au début de la fonction du worker, qui logge le panic avec la stack (`debug.Stack()`) et transforme l'échec en erreur traitée (job marqué en échec, métrique incrémentée).
Mais je n'en resterais pas là : un panic est un bug. Je corrigerais la cause (souvent un nil non vérifié ou un index hors limites), et je me demanderais si ce traitement doit vraiment s'exécuter dans des goroutines non supervisées ; un pool de workers ou `errgroup` donne un cadre plus propre."

**Q : "Les développeurs se plaignent de la verbosité de `if err != nil`. Qu'en penses-tu ?"**
R : "La verbosité est réelle, mais c'est un compromis assumé : chaque appel qui peut échouer est visible, et la revue de code voit immédiatement si une erreur est ignorée ou mal traitée. L'équipe Go a d'ailleurs renoncé en 2025 à changer la syntaxe après des années de propositions.
Pour réduire le bruit sans perdre en clarté : ajouter du **contexte utile** à chaque wrapping plutôt que propager nu, regrouper les opérations d'une même étape dans une fonction qui retourne une seule erreur, utiliser `errors.Join` pour les validations, et le pattern *sticky error* pour une suite d'écritures (un type qui mémorise la première erreur et ignore les opérations suivantes, comme `bufio.Writer`)."

═══════════════════════════════════════════
# 6. GENERICS (PARAMÈTRES DE TYPE)
═══════════════════════════════════════════

### Vue d'ensemble

Les generics sont arrivés en **Go 1.18** (mars 2022), après plus de dix ans de débats. L'équipe Go les a longtemps repoussés pour ne pas compliquer le langage ; le résultat est une version volontairement **plus simple** que celle de Java, C++ ou Rust. Ils servent avant tout à écrire des **fonctions utilitaires** et des **structures de données** réutilisables (une fonction `Map`, un ensemble, une file de priorité, un cache typé) sans passer par `interface{}` et des assertions de type.

Ils ont ensuite évolué régulièrement : packages `slices`, `maps` et `cmp` en 1.21, alias génériques en 1.24, contraintes autoréférentes en 1.26, et surtout **méthodes génériques en Go 1.27**, une limite historique enfin levée.

La règle d'usage reste la même : **n'utiliser les generics que quand on écrirait sinon le même code plusieurs fois pour des types différents**. Une interface classique reste préférable quand on veut un comportement polymorphe.

### Cours

**Fonction générique** :
```go
func Map[T, U any](xs []T, f func(T) U) []U {
    out := make([]U, 0, len(xs))
    for _, x := range xs {
        out = append(out, f(x))
    }
    return out
}

longueurs := Map([]string{"a", "bcd"}, func(s string) int { return len(s) }) // [1 3]
```
Les arguments de type sont en général **inférés** depuis les arguments ; on peut les donner explicitement : `Map[string, int](...)`.

**Contraintes** : une contrainte est une **interface**, qui peut contenir des méthodes et/ou un **ensemble de types**.
```go
type Number interface {
    ~int | ~int32 | ~int64 | ~float32 | ~float64
}

func Somme[T Number](xs []T) T {
    var total T
    for _, x := range xs {
        total += x
    }
    return total
}
```
- `any` : n'importe quel type (on ne peut alors utiliser que les opérations valables pour tous : affectation, passage en paramètre).
- `comparable` : les types utilisables avec `==` et `!=` (indispensable pour une clé de map). Depuis Go 1.20, `comparable` accepte aussi les interfaces (au risque d'un panic à l'exécution si la valeur dynamique n'est pas comparable).
- `~T` : **tous les types dont le type sous-jacent est T** (par exemple `type Celsius float64` satisfait `~float64`, mais pas `float64` tout seul).
- `cmp.Ordered` (Go 1.21) : les types ordonnés avec `<` (entiers, flottants, chaînes). Les anciennes contraintes de `golang.org/x/exp/constraints` sont remplacées.
- Une contrainte peut combiner méthodes et types : `interface{ ~string; String() string }`.
- Une interface qui contient une union de types **ne peut servir que de contrainte**, pas de type de variable.

**Types génériques** :
```go
type Pile[T any] struct {
    elements []T
}

func (p *Pile[T]) Empiler(v T) { p.elements = append(p.elements, v) }

func (p *Pile[T]) Depiler() (T, bool) {
    var zero T
    if len(p.elements) == 0 {
        return zero, false
    }
    v := p.elements[len(p.elements)-1]
    p.elements = p.elements[:len(p.elements)-1]
    return v, true
}

var p Pile[int]
```
`var zero T` est l'idiome pour obtenir la valeur zéro d'un paramètre de type (on voit aussi `*new(T)`, moins lisible).

**Méthodes génériques (Go 1.27)** : jusqu'à Go 1.26, une méthode pouvait utiliser les paramètres de type **de son type** (`Pile[T]`), mais ne pouvait pas déclarer **ses propres** paramètres de type. Il fallait écrire une fonction de package. Depuis Go 1.27, c'est possible :
```go
func (p *Pile[T]) Transformer[U any](f func(T) U) *Pile[U] { /* ... */ }
```
Limites qui demeurent : les méthodes d'**interface** ne peuvent pas déclarer de paramètres de type, et une méthode générique ne peut pas servir à implémenter une méthode d'interface. Exemple dans la bibliothèque standard : `math/rand/v2` ajoute la méthode générique `(*Rand) N`.

**Autres évolutions** :
- **Go 1.21** : packages `slices`, `maps`, `cmp` ; fonctions intégrées `min`, `max`, `clear` ; meilleure inférence.
- **Go 1.24** : **alias de types génériques** (`type Set[T comparable] = map[T]struct{}`).
- **Go 1.25** : la notion de *core type* disparaît de la spécification (règles simplifiées, sans changement visible pour la plupart du code).
- **Go 1.26** : un type générique peut se référer à lui-même dans sa contrainte (`type Adder[A Adder[A]] interface{ Add(A) A }`).
- **Go 1.27** : méthodes génériques ; dans un littéral de struct, une clé peut désormais être un sélecteur de champ imbriqué ; inférence étendue quand une fonction générique est affectée à une variable de type fonction.

**Ce qui n'existe pas (ou pas encore)** :
- pas de spécialisation (un comportement différent selon le type concret) : on peut faire un type switch sur `any(v)`, mais c'est un signe qu'une interface conviendrait mieux ;
- pas de paramètres de type variadiques ;
- pas d'opérateurs définis par l'utilisateur ;
- pas de covariance : `[]Chien` n'est pas un `[]Animal`, generics ou pas.

**Implémentation : GC shape stenciling** : Go ne génère pas une copie complète de la fonction pour chaque type (comme C++), ni n'utilise un seul code avec du boxing (comme Java). Il génère une version par **forme mémoire** (*GC shape*) : tous les types pointeurs partagent la même version, qui reçoit un **dictionnaire** décrivant le type réel. Conséquence pratique : une fonction générique appelée avec des pointeurs ou des interfaces peut être **un peu plus lente** que la version écrite à la main (appels indirects via le dictionnaire, moins d'inlining). Pour du code critique, mesurer.

**Generics vs interfaces, comment choisir** :
| Situation | Choix |
|---|---|
| même algorithme pour différents types de données (Map, Filter, Max, Set, cache) | generics |
| conteneurs typés (pile, file, arbre, LRU) | generics |
| comportements différents derrière un même contrat (Notifier email/SMS) | interface |
| une fonction qui appelle seulement des méthodes | interface (souvent suffisante et plus simple) |
| éviter des assertions de type sur `any` | generics |

Le guide officiel (*When To Use Generics*, Ian Lance Taylor) résume : écrire du code, pas des types ; ajouter des paramètres de type quand on se retrouve à écrire le même code plusieurs fois.

**Itérateurs génériques** : depuis Go 1.23, `iter.Seq[V]` et `iter.Seq2[K, V]` sont des types génériques de fonctions d'itération (voir le chapitre Go moderne).

### En situation

- **"J'ai écrit `ContainsInt`, `ContainsString`, `ContainsUserID`"** → une seule fonction générique, ou `slices.Contains`.
- **"Mon type Celsius ne satisfait pas la contrainte float64"** → utiliser `~float64`.
- **"Impossible d'utiliser ce type comme clé de map dans ma fonction générique"** → contrainte `comparable`.
- **"Ma fonction générique est plus lente que la version spécifique"** → GC shape stenciling et dictionnaires → mesurer, spécialiser si c'est critique.
- **"Je voudrais une méthode générique sur mon type"** → possible depuis Go 1.27, sinon fonction de package.

### Questions techniques

**Q : Depuis quelle version Go a-t-il des generics ?** R : Go 1.18 (mars 2022).

**Q : Qu'est-ce qu'une contrainte en Go ?** R : Une interface qui décrit les méthodes et/ou l'ensemble de types autorisés pour un paramètre de type.

**Q : any vs comparable ?** R : any accepte tout ; comparable n'accepte que les types utilisables avec == (nécessaire pour une clé de map).

**Q : Que signifie `~int` ?** R : Tous les types dont le type sous-jacent est int, y compris les types nommés comme `type ID int`.

**Q : Quelle contrainte pour les types ordonnés ?** R : cmp.Ordered (Go 1.21).

**Q : Une interface avec union de types peut-elle typer une variable ?** R : Non, uniquement servir de contrainte.

**Q : Comment obtenir la valeur zéro d'un paramètre de type ?** R : `var zero T`.

**Q : Les arguments de type sont-ils toujours explicites ?** R : Non, ils sont généralement inférés depuis les arguments de la fonction.

**Q : Qu'a changé Go 1.27 pour les méthodes ?** R : Une méthode peut déclarer ses propres paramètres de type (méthodes génériques).

**Q : Une méthode d'interface peut-elle être générique ?** R : Non, même en Go 1.27.

**Q : Qu'apporte Go 1.24 aux generics ?** R : Les alias de types génériques.

**Q : Qu'apporte Go 1.26 aux generics ?** R : Les contraintes autoréférentes (un type générique peut se citer dans sa liste de paramètres de type).

**Q : Comment Go compile-t-il les generics ?** R : Par GC shape stenciling : une version par forme mémoire, avec un dictionnaire de types.

**Q : Les generics sont-ils gratuits en performance ?** R : Pas toujours : avec des pointeurs ou des interfaces, les appels via dictionnaire peuvent être plus lents qu'une version spécifique.

**Q : Quand préférer une interface aux generics ?** R : Quand on veut un comportement différent derrière un même contrat, ou qu'on n'appelle que des méthodes.

**Q : Go a-t-il la spécialisation de generics ?** R : Non.

**Q : `[]Chien` est-il un `[]Animal` ?** R : Non, il n'y a pas de covariance.

**Q : Que contiennent les packages slices, maps et cmp ?** R : Des fonctions génériques sur les slices, les maps et la comparaison (Go 1.21).

**Q : Quelles fonctions intégrées génériques sont arrivées en 1.21 ?** R : min, max et clear.

### Questions de situation

**Q : "Écris une fonction générique qui regroupe des éléments par une clé."**
R :
```go
func GroupBy[T any, K comparable](xs []T, cle func(T) K) map[K][]T {
    out := make(map[K][]T)
    for _, x := range xs {
        k := cle(x)
        out[k] = append(out[k], x)
    }
    return out
}

parDept := GroupBy(employes, func(e Employe) string { return e.Departement })
```
"La clé doit être `comparable` puisqu'elle sert de clé de map ; les éléments peuvent être de n'importe quel type. L'`append` sur une entrée absente fonctionne car la valeur zéro d'un slice est nil et `append` l'accepte."

**Q : "Implémente un ensemble générique."**
R :
```go
type Set[T comparable] struct{ m map[T]struct{} }

func NewSet[T comparable](xs ...T) *Set[T] {
    s := &Set[T]{m: make(map[T]struct{}, len(xs))}
    for _, x := range xs {
        s.Add(x)
    }
    return s
}
func (s *Set[T]) Add(v T)      { s.m[v] = struct{}{} }
func (s *Set[T]) Has(v T) bool { _, ok := s.m[v]; return ok }
func (s *Set[T]) Len() int     { return len(s.m) }
func (s *Set[T]) Remove(v T)   { delete(s.m, v) }
```
"La struct vide comme valeur ne coûte rien. Le constructeur initialise la map, sinon `Add` paniquerait sur une map nil. Pour une version utilisable sans constructeur (valeur zéro utile), `Add` initialiserait la map paresseusement. Je pourrais ajouter un itérateur `All() iter.Seq[T]` pour la parcourir avec `range`. Et ce type n'est pas thread-safe : je le documenterais."

**Q : "Un collègue veut rendre générique toute la couche repository. Ton avis ?"**
R : "C'est tentant (un `Repository[T]` avec `Find`, `Save`, `Delete`), mais je serais prudent. Les requêtes réelles divergent vite d'une entité à l'autre (filtres, jointures, pagination spécifique), et un repository générique finit soit très limité, soit plein d'options et de réflexion.
Je préfère des repositories **concrets** par agrégat, avec des méthodes nommées selon le métier (`CommandesEnAttente(ctx, clientID)`), et éventuellement de petites **fonctions génériques** pour le code réellement répétitif (scanner des lignes, paginer). Les generics doivent supprimer de la duplication, pas créer un framework."

═══════════════════════════════════════════
# 7. CONCURRENCE : GOROUTINES, CHANNELS & SELECT
═══════════════════════════════════════════

### Vue d'ensemble

La concurrence est la raison d'être de Go. Le langage s'inspire du modèle **CSP** (*Communicating Sequential Processes*, Tony Hoare, 1978) : des processus indépendants qui communiquent par des canaux. Le proverbe qui résume l'approche : **"Don't communicate by sharing memory; share memory by communicating."** Plutôt que de protéger des données partagées par des verrous, on fait circuler les données entre goroutines par des channels, et une seule goroutine "possède" une donnée à un instant donné.

Une **goroutine** est une fonction qui s'exécute de façon concurrente, gérée par le runtime Go et non par l'OS. Elle démarre avec une pile de quelques kilo-octets (2 Ko) qui grandit au besoin ; on peut en lancer des centaines de milliers. C'est ce qui permet d'écrire un serveur qui traite chaque connexion dans sa propre goroutine, en code séquentiel lisible, sans callbacks ni réactif.

Concurrence n'est pas parallélisme (Rob Pike) : la **concurrence** est la structuration d'un programme en tâches indépendantes ; le **parallélisme** est leur exécution simultanée sur plusieurs cœurs. Go facilite la première, le runtime s'occupe de la seconde.

Les questions d'entretien portent sur la sémantique des channels (bloquants, bufferisés, fermés, nil), `select`, les **fuites de goroutines** et les patterns classiques (worker pool, pipeline, fan-out/fan-in).

### Cours

**Lancer une goroutine** : `go f(x)` ou `go func() { ... }()`. La fonction appelante ne l'attend pas ; quand `main` se termine, le programme s'arrête **sans attendre** les autres goroutines. Il faut donc toujours un mécanisme pour les attendre (`sync.WaitGroup`, channel, `errgroup`).

**Channels** :
```go
ch := make(chan int)      // non bufferisé
buf := make(chan int, 10) // bufferisé, capacité 10
ch <- 42                  // envoi
v := <-ch                 // réception
v, ok := <-ch             // ok == false si le channel est fermé et vide
close(ch)                 // fermeture
for v := range ch { }     // reçoit jusqu'à la fermeture
```
- **Non bufferisé** : l'envoi bloque jusqu'à ce qu'un récepteur prenne la valeur (et inversement). C'est un **rendez-vous** qui synchronise les deux goroutines.
- **Bufferisé** : l'envoi ne bloque que si le buffer est plein, la réception que s'il est vide. Il découple les débits, il ne garantit pas que la valeur a été traitée.
- **Directions** dans les signatures : `chan<- int` (envoi seulement), `<-chan int` (réception seulement). Le compilateur vérifie le bon usage ; c'est une documentation précieuse.

**Règles de comportement à connaître par cœur** :
| Opération | channel nil | channel ouvert | channel fermé |
|---|---|---|---|
| envoi `ch <- v` | bloque pour toujours | bloque ou réussit | **panic** |
| réception `<-ch` | bloque pour toujours | bloque ou réussit | valeurs restantes du buffer, puis valeur zéro immédiate (ok = false) |
| `close(ch)` | **panic** | réussit | **panic** |

Conséquences : **c'est l'émetteur qui ferme**, jamais le récepteur ; on ne ferme que pour signaler "plus de valeurs" (fermer n'est pas obligatoire, un channel non référencé est collecté par le GC) ; avec plusieurs émetteurs, on coordonne la fermeture (un `WaitGroup` puis une goroutine qui ferme après `Wait`).

**select** : attend sur plusieurs opérations de channel, exécute celle qui est prête (au hasard si plusieurs le sont).
```go
select {
case msg := <-entree:
    traiter(msg)
case sortie <- resultat:
    // envoyé
case <-ctx.Done():
    return ctx.Err()
case <-time.After(2 * time.Second):
    return errTimeout
default:
    // aucune n'est prête : select non bloquant
}
```
- Un `select {}` vide bloque pour toujours.
- **Désactiver un cas** : mettre le channel à `nil` (une opération sur nil bloque, donc le cas n'est jamais choisi). Utile quand on fusionne plusieurs sources et que l'une est terminée.
- `time.After` dans une boucle crée un timer à chaque tour ; depuis Go 1.23, les timers non référencés sont collectés immédiatement, mais un `time.NewTimer` réutilisé ou un `context.WithTimeout` reste plus propre.

**Timers et channels (Go 1.23 et 1.27)** : depuis Go 1.23, les channels des `time.Timer` et `time.Ticker` sont **non bufferisés** (synchrones) : après `Stop` ou `Reset`, aucune valeur périmée ne peut être reçue, ce qui supprime un piège historique. Le réglage `asynctimerchan` qui permettait de revenir à l'ancien comportement a été **supprimé en Go 1.27**.

**Patterns essentiels** :

*Attendre un groupe de goroutines* :
```go
var wg sync.WaitGroup
for _, url := range urls {
    wg.Go(func() {            // Go 1.25 : Add(1) + go + Done intégrés
        recuperer(url)
    })
}
wg.Wait()
```
Avant Go 1.25 : `wg.Add(1)` **avant** le `go`, et `defer wg.Done()` dans la goroutine.

*Worker pool* : borner la concurrence.
```go
func pool(ctx context.Context, jobs <-chan Job, n int) <-chan Resultat {
    out := make(chan Resultat)
    var wg sync.WaitGroup
    for range n {
        wg.Go(func() {
            for j := range jobs {
                select {
                case out <- traiter(j):
                case <-ctx.Done():
                    return
                }
            }
        })
    }
    go func() { wg.Wait(); close(out) }()   // un seul ferme, après tous les émetteurs
    return out
}
```

*Pipeline* : des étapes reliées par des channels, chacune une goroutine qui lit l'entrée, transforme et émet ; chaque étape **ferme sa sortie** quand son entrée est épuisée, et toutes écoutent `ctx.Done()` pour s'arrêter si l'aval abandonne.

*Fan-out / fan-in* : plusieurs workers lisent le même channel (fan-out) ; un `merge` fusionne plusieurs channels en un (fan-in).

*Sémaphore* : limiter le nombre d'opérations simultanées avec un channel bufferisé.
```go
sem := make(chan struct{}, 10)
for _, t := range taches {
    sem <- struct{}{}            // acquiert (bloque si 10 en cours)
    go func() {
        defer func() { <-sem }() // libère
        executer(t)
    }()
}
```
(ou `golang.org/x/sync/semaphore` pour un sémaphore pondéré avec contexte).

*errgroup* (`golang.org/x/sync/errgroup`) : lancer des goroutines, récupérer la **première erreur**, annuler les autres via le contexte, et limiter la concurrence.
```go
g, ctx := errgroup.WithContext(ctx)
g.SetLimit(8)
for _, id := range ids {
    g.Go(func() error {
        return charger(ctx, id)
    })
}
if err := g.Wait(); err != nil {
    return err
}
```
C'est le pattern le plus utilisé en production pour paralléliser des appels.

*Générateur*, *done channel*, *or-channel*, *tee* : variations des précédents ; aujourd'hui `context` remplace le *done channel* artisanal, et les itérateurs (Go 1.23) remplacent souvent les générateurs par channel.

*Résultat avec timeout* :
```go
ctx, cancel := context.WithTimeout(ctx, 500*time.Millisecond)
defer cancel()
res, err := appelLent(ctx)   // la fonction doit respecter ctx
```

**Fuites de goroutines** : une goroutine bloquée pour toujours (sur un channel que personne ne lira ou n'écrira, sur un verrou jamais libéré) ne sera **jamais collectée** et retient sa pile et tout ce qu'elle référence. Causes typiques :
- un émetteur sur un channel non bufferisé dont le récepteur est parti (retour anticipé après une erreur) ;
- un récepteur qui attend un channel jamais fermé ;
- une goroutine qui ne surveille pas `ctx.Done()`.

Prévention : chaque goroutine doit avoir une **condition de fin claire** (contexte, fermeture de channel) ; un buffer dimensionné au nombre d'émetteurs quand on peut abandonner la lecture (le pattern "premier résultat" utilise `make(chan T, len(sources))`) ; `errgroup`. Détection : le nombre de goroutines (`runtime.NumGoroutine`, métrique `go_goroutines`), le profil `goroutine` de pprof, la bibliothèque `goleak` (Uber) dans les tests, et le profil **`goroutineleak`** du runtime, expérimental en Go 1.26 et **disponible par défaut en Go 1.27** (`/debug/pprof/goroutineleak`), qui détecte via le GC les goroutines bloquées sur des primitives devenues inatteignables.

**Deadlock** : si **toutes** les goroutines sont bloquées, le runtime arrête le programme avec `fatal error: all goroutines are asleep - deadlock!`. Un deadlock partiel (quelques goroutines bloquées, le reste du programme vivant) n'est pas détecté : c'est une fuite.

**Channels vs mutex, comment choisir** : channels pour **transférer la propriété** d'une donnée, coordonner des étapes, signaler des événements, distribuer du travail ; mutex pour **protéger un état** partagé simple (un compteur, une map de cache). Un mutex est souvent plus simple et plus rapide pour un état partagé ; un channel utilisé comme verrou est un signe de complexité inutile.

**Bonnes pratiques** :
- celui qui **lance** une goroutine est responsable de sa **fin** ;
- éviter les goroutines "fire and forget" dans un serveur (pas d'erreurs remontées, pas d'arrêt propre) ;
- passer le `context.Context` à toute goroutine qui fait de l'I/O ;
- préférer des API **synchrones** et laisser l'appelant ajouter la concurrence (`go f()`), plutôt que des API qui lancent des goroutines en interne ;
- jamais de `time.Sleep` pour "attendre qu'une goroutine ait fini".

### En situation

- **"Le programme se termine avant que les goroutines aient fini"** → main n'attend pas → WaitGroup, errgroup.
- **"La mémoire et le nombre de goroutines augmentent sans cesse"** → fuite → profil goroutine, goroutineleak, goleak.
- **"panic: send on closed channel"** → plusieurs émetteurs, l'un ferme trop tôt.
- **"fatal error: all goroutines are asleep - deadlock!"** → envoi sur un channel non bufferisé sans récepteur, range sur un channel jamais fermé.
- **"Je dois appeler 50 API en parallèle, mais pas toutes en même temps"** → errgroup avec SetLimit, ou sémaphore.
- **"Le premier qui répond gagne"** → channel bufferisé + select + annulation des autres.

### Questions techniques

**Q : C'est quoi une goroutine ?** R : Une fonction exécutée de façon concurrente, gérée par le runtime Go, avec une petite pile extensible.

**Q : Taille initiale de la pile d'une goroutine ?** R : Environ 2 Ko, extensible dynamiquement.

**Q : Goroutine vs thread de l'OS ?** R : La goroutine est bien plus légère, ordonnancée par le runtime sur un petit nombre de threads.

**Q : Concurrence vs parallélisme ?** R : Structurer en tâches indépendantes vs les exécuter simultanément sur plusieurs cœurs.

**Q : Que se passe-t-il pour les goroutines quand main se termine ?** R : Le programme s'arrête sans les attendre.

**Q : Channel bufferisé vs non bufferisé ?** R : Le non bufferisé synchronise émetteur et récepteur (rendez-vous) ; le bufferisé ne bloque que plein ou vide.

**Q : Que se passe-t-il en envoyant sur un channel fermé ?** R : Un panic.

**Q : En recevant depuis un channel fermé ?** R : Les valeurs restantes, puis la valeur zéro immédiatement (ok = false).

**Q : Sur un channel nil ?** R : Envoi et réception bloquent pour toujours ; close panique.

**Q : Qui doit fermer un channel ?** R : L'émetteur, jamais le récepteur.

**Q : Faut-il toujours fermer un channel ?** R : Non, seulement pour signaler la fin des valeurs (range, récepteurs en attente).

**Q : Comment fermer un channel avec plusieurs émetteurs ?** R : Attendre tous les émetteurs (WaitGroup) puis fermer depuis une seule goroutine.

**Q : Que fait select avec plusieurs cas prêts ?** R : Il en choisit un au hasard.

**Q : À quoi sert default dans un select ?** R : Rendre le select non bloquant.

**Q : Comment désactiver un cas d'un select ?** R : Mettre son channel à nil.

**Q : À quoi servent les directions `chan<-` et `<-chan` ?** R : Restreindre un channel à l'envoi ou à la réception, vérifié par le compilateur.

**Q : Qu'a changé Go 1.23 pour les timers ?** R : Channels de timers non bufferisés (pas de valeur périmée après Stop/Reset) et timers non référencés collectés immédiatement.

**Q : Qu'apporte sync.WaitGroup.Go ?** R : Lancer une goroutine suivie par le WaitGroup en une seule ligne (Go 1.25).

**Q : Pourquoi appeler wg.Add avant go ?** R : Sinon Wait peut s'exécuter avant que l'Add n'ait eu lieu.

**Q : C'est quoi errgroup ?** R : Un groupe de goroutines qui renvoie la première erreur, annule le contexte et peut limiter la concurrence.

**Q : Comment limiter la concurrence ?** R : Worker pool, sémaphore (channel bufferisé), errgroup.SetLimit.

**Q : C'est quoi une fuite de goroutine ?** R : Une goroutine bloquée pour toujours, jamais collectée, qui retient sa mémoire.

**Q : Comment détecter une fuite de goroutines ?** R : Nombre de goroutines, profil goroutine de pprof, goleak en test, profil goroutineleak (Go 1.27).

**Q : Le runtime détecte-t-il les deadlocks ?** R : Seulement si toutes les goroutines sont bloquées.

**Q : Channel ou mutex ?** R : Channel pour transférer des données et coordonner ; mutex pour protéger un état partagé.

**Q : C'est quoi le modèle CSP ?** R : Des processus séquentiels indépendants qui communiquent par des canaux.

**Q : Que signifie "share memory by communicating" ?** R : Faire circuler les données par des channels plutôt que les partager sous verrou.

**Q : Une goroutine peut-elle être tuée de l'extérieur ?** R : Non, elle doit s'arrêter elle-même, en général en surveillant un contexte.

**Q : Pourquoi éviter time.Sleep pour synchroniser ?** R : C'est fragile et lent ; utiliser WaitGroup, channels ou contextes.

### Questions de situation

**Q : "Appelle trois services en parallèle et renvoie une erreur si l'un échoue, avec un timeout global de 2 secondes."**
R :
```go
func agreger(ctx context.Context) (Resultat, error) {
    ctx, cancel := context.WithTimeout(ctx, 2*time.Second)
    defer cancel()

    var r Resultat
    g, ctx := errgroup.WithContext(ctx)
    g.Go(func() (err error) { r.Profil, err = profil(ctx); return })
    g.Go(func() (err error) { r.Commandes, err = commandes(ctx); return })
    g.Go(func() (err error) { r.Reco, err = recommandations(ctx); return })
    if err := g.Wait(); err != nil {
        return Resultat{}, err
    }
    return r, nil
}
```
"Chaque goroutine écrit dans un champ distinct de `r`, donc pas de course de données, et `Wait` établit la synchronisation avant que je lise `r`. Au premier échec, `errgroup` annule le contexte, ce qui interrompt les autres appels s'ils respectent `ctx`. Si une réponse partielle est acceptable (les recommandations sont optionnelles), je ne renverrais pas son erreur et je la loggerais à la place."

**Q : "Le nombre de goroutines de ton service augmente de façon continue et la mémoire avec."**
R : "C'est une fuite de goroutines. Je récupérerais le profil `goroutine` (`/debug/pprof/goroutine?debug=2`) : il groupe les goroutines par pile, et des milliers bloquées sur la même ligne désignent le coupable. Sur Go 1.27, le profil `goroutineleak` identifie directement celles qui ne pourront jamais être débloquées.
Les causes habituelles : un envoi sur un channel non bufferisé après que le récepteur a abandonné (retour anticipé sur erreur ou timeout), une goroutine qui attend un channel jamais fermé, un appel réseau sans timeout, une goroutine qui ignore `ctx.Done()`.
Correction selon le cas : buffer dimensionné au nombre d'émetteurs, `select` avec `ctx.Done()` sur chaque envoi, timeouts sur les clients. Et j'ajouterais `goleak` aux tests du package pour que la régression ne revienne pas."

**Q : "Implémente une fonction qui renvoie le résultat du premier de plusieurs serveurs miroirs qui répond."**
R :
```go
func premier(ctx context.Context, serveurs []string) (string, error) {
    ctx, cancel := context.WithCancel(ctx)
    defer cancel()                               // annule les requêtes perdantes

    type rep struct { val string; err error }
    ch := make(chan rep, len(serveurs))          // buffer : aucun émetteur ne bloque
    for _, s := range serveurs {
        go func() {
            v, err := requete(ctx, s)
            ch <- rep{v, err}
        }()
    }
    var derniere error
    for range serveurs {
        r := <-ch
        if r.err == nil {
            return r.val, nil
        }
        derniere = r.err
    }
    return "", derniere
}
```
"Le buffer de la taille du nombre de serveurs est essentiel : quand je retourne après la première réponse, les goroutines restantes peuvent encore envoyer sans bloquer, donc pas de fuite. Le `cancel` différé interrompt les requêtes encore en cours."

═══════════════════════════════════════════
# 8. SYNCHRONISATION, ATOMIQUES & MODÈLE MÉMOIRE
═══════════════════════════════════════════

### Vue d'ensemble

Les channels ne remplacent pas tout. Pour protéger un état partagé (un compteur, un cache, une configuration rechargée à chaud), le package `sync` et le package `sync/atomic` sont souvent plus simples et plus rapides. Go assume ce pragmatisme : la bibliothèque standard elle-même utilise beaucoup plus de mutex que de channels.

Le **modèle mémoire** de Go (révisé en 2022, Go 1.19) définit quand une écriture faite par une goroutine est garantie visible par une autre. La règle pratique est simple : **sans synchronisation explicite, aucune garantie**. Une *data race* (deux accès concurrents dont au moins une écriture, sans synchronisation) est un bug, que l'outil le plus précieux de l'écosystème, le **détecteur de courses** (`-race`), sait trouver.

### Cours

**sync.Mutex** :
```go
type Compteur struct {
    mu sync.Mutex
    n  map[string]int
}

func (c *Compteur) Inc(cle string) {
    c.mu.Lock()
    defer c.mu.Unlock()
    c.n[cle]++
}
```
- La valeur zéro est un mutex déverrouillé, utilisable directement.
- **Non réentrant** : une goroutine qui reverrouille un mutex qu'elle détient se bloque elle-même (deadlock). Conséquence de conception : les méthodes publiques prennent le verrou, et appellent des méthodes privées qui supposent le verrou déjà pris.
- Ne **jamais copier** un mutex après usage (vet `copylocks`) : receveurs pointeurs.
- Placer le mutex juste au-dessus des champs qu'il protège, avec un commentaire.
- `TryLock` existe (Go 1.18) mais son usage est presque toujours un signe de mauvaise conception.
- Garder les sections critiques **courtes** : pas d'I/O ni d'appel réseau sous verrou.

**sync.RWMutex** : plusieurs lecteurs simultanés (`RLock`/`RUnlock`) ou un seul écrivain (`Lock`). Utile pour une donnée très lue et peu modifiée ; plus coûteux qu'un Mutex simple, donc pas systématique. Un écrivain en attente bloque les nouveaux lecteurs (pour éviter la famine de l'écrivain) ; prendre un `RLock` récursivement peut donc provoquer un deadlock.

**sync.WaitGroup** : `Add(n)`, `Done()`, `Wait()`, et `Go(func())` depuis Go 1.25 (le modernizer `waitgroupgo` de `go fix` convertit automatiquement l'ancien style). Ne pas copier un WaitGroup ; `Add` avant le lancement de la goroutine.

**sync.Once** et ses dérivés : exécuter une initialisation exactement une fois, même sous concurrence.
```go
var (
    once sync.Once
    cfg  *Config
)
func GetConfig() *Config {
    once.Do(func() { cfg = charger() })
    return cfg
}
// Go 1.21
var getCfg = sync.OnceValue(charger)          // func() *Config
var getCfg2 = sync.OnceValues(chargerAvecErr) // func() (*Config, error)
```
Si la fonction passée à `Do` panique, l'initialisation est considérée comme faite (elle ne sera pas retentée).

**sync.Cond** : attendre qu'une condition devienne vraie (`Wait` dans une boucle, `Signal`, `Broadcast`). Rarement nécessaire, les channels couvrent la plupart des cas.

**sync.Map** : map concurrente optimisée pour deux cas : clés écrites une fois puis lues souvent (caches qui ne font que grossir), ou goroutines qui travaillent sur des ensembles de clés disjoints. Dans le cas général, une `map` + `RWMutex` est plus simple, typée et souvent aussi rapide. Méthodes : `Load`, `Store`, `LoadOrStore`, `LoadAndDelete`, `Delete`, `Range`, `Swap`, `CompareAndSwap`, `Clear` (1.23). Non typée (valeurs `any`).

**sync.Pool** : cache d'objets temporaires réutilisables pour réduire les allocations (buffers d'encodage, par exemple). Les objets peuvent être **supprimés à tout moment** par le GC : ce n'est pas un pool de connexions. Toujours **réinitialiser** l'objet avant de le remettre (`buf.Reset()`) et ne pas y mettre des objets de taille très variable (un buffer énorme retenu indéfiniment).

**sync/atomic** : opérations atomiques sans verrou, pour des compteurs et des drapeaux.
```go
var requetes atomic.Int64      // types atomiques, Go 1.19
requetes.Add(1)
n := requetes.Load()

var pret atomic.Bool
pret.Store(true)

var cfg atomic.Pointer[Config] // pointeur typé, idéal pour une config rechargée
cfg.Store(nouvelle)
c := cfg.Load()
```
Préférer les **types** (`atomic.Int64`, `atomic.Bool`, `atomic.Pointer[T]`, `atomic.Value`) aux anciennes fonctions (`atomic.AddInt64(&x, 1)`), plus faciles à mal utiliser (accès non atomique accidentel, alignement 64 bits sur architectures 32 bits). Le modernizer `atomictypes` de `go fix` (Go 1.27) fait la conversion. `CompareAndSwap` pour les algorithmes lock-free, avec prudence.

**Le modèle mémoire, l'essentiel** : il garantit des relations *happens-before* :
- une opération de channel : l'envoi happens-before la réception correspondante ; la fermeture happens-before une réception qui observe la fermeture ; pour un channel non bufferisé, la réception happens-before la fin de l'envoi ;
- `Unlock` happens-before le `Lock` suivant ;
- `once.Do(f)` : la fin de f happens-before le retour de tout appel à `Do` ;
- `wg.Done()` happens-before le retour de `Wait` ;
- les opérations atomiques sont séquentiellement cohérentes entre elles (Go 1.19) ;
- l'instruction `go` happens-before le début de la goroutine lancée ; **la fin d'une goroutine n'est synchronisée avec rien** automatiquement.

Sans ces relations, un programme a une **data race**, et les valeurs lues peuvent être arbitraires (une struct multi-mots, comme une interface ou une string, peut même être lue à moitié mise à jour). Le conseil officiel : "If you must read the rest of this document to understand the behavior of your program, you are being too clever. Don't be clever."

**Data race vs race condition** : une *data race* est un accès concurrent non synchronisé (détectable par `-race`) ; une *race condition* est un défaut logique dépendant de l'ordre d'exécution (un *check-then-act* comme "si absent, insérer", même avec un mutex sur chaque opération séparée). On peut avoir une race condition sans data race.

**Le détecteur de courses** : `go test -race ./...`, `go run -race`, `go build -race`.
- Il instrumente les accès mémoire et signale chaque data race **effectivement exécutée** pendant le test, avec les deux piles en conflit.
- Coût : CPU ×2 à ×20, mémoire ×5 à ×10 : on l'utilise en tests et en CI, rarement en production (parfois sur une instance canary).
- Il ne trouve que ce qui est exécuté : des tests qui exercent réellement la concurrence sont nécessaires.
- Pas de faux positifs : toute course signalée est réelle.
- Requiert cgo sur la plupart des plateformes (support étendu au fil des versions, linux/riscv64 en 1.26).

**Deadlocks avec des verrous** : verrouiller plusieurs mutex dans un ordre différent selon les chemins, reverrouiller un mutex non réentrant, oublier un `Unlock` sur un chemin d'erreur (d'où `defer mu.Unlock()`), appeler un callback utilisateur sous verrou.

**Choisir le bon outil** :
| Besoin | Outil |
|---|---|
| compteur ou drapeau | `atomic.Int64`, `atomic.Bool` |
| configuration remplacée en bloc, lue partout | `atomic.Pointer[T]` (copie immuable) |
| état composite protégé | `sync.Mutex` (ou `RWMutex` si très majoritairement lu) |
| initialisation paresseuse unique | `sync.Once`, `sync.OnceValue` |
| attendre des goroutines | `sync.WaitGroup`, `errgroup` |
| transférer des données, pipeline | channels |
| réduire les allocations temporaires | `sync.Pool` |
| cache qui ne fait que grossir, très lu | `sync.Map` |

**golang.org/x/sync** : `errgroup`, `semaphore` (pondéré, avec contexte), `singleflight` (dédupliquer des appels concurrents identiques : si 100 requêtes demandent la même clé absente du cache, une seule exécute le chargement et les autres partagent le résultat, ce qui évite le *cache stampede*).

### En situation

- **"Le test passe mais -race signale une course"** → accès partagé non protégé → mutex, atomique ou channel.
- **"Le programme se bloque quand une méthode en appelle une autre"** → mutex non réentrant repris.
- **"Configuration rechargée à chaud lue par toutes les requêtes"** → atomic.Pointer vers une config immuable.
- **"100 requêtes simultanées rechargent la même donnée"** → singleflight.
- **"Les allocations de buffers dominent le profil"** → sync.Pool.
- **"Un compteur de métriques est un goulot de contention"** → atomique ou compteurs par shard.

### Questions techniques

**Q : sync.Mutex est-il réentrant ?** R : Non ; reverrouiller un mutex détenu bloque la goroutine.

**Q : Pourquoi `defer mu.Unlock()` ?** R : Pour garantir la libération sur tous les chemins de sortie, y compris les panics.

**Q : Mutex vs RWMutex ?** R : Accès exclusif vs plusieurs lecteurs simultanés ou un écrivain ; RWMutex seulement si les lectures dominent nettement.

**Q : Peut-on copier un Mutex ?** R : Non après usage : la copie duplique l'état ; go vet le signale.

**Q : Pourquoi ne pas faire d'I/O sous verrou ?** R : La section critique s'allonge et toutes les goroutines en attente sont bloquées.

**Q : Que garantit sync.Once ?** R : Que la fonction s'exécute exactement une fois, et que tous les appelants voient son effet.

**Q : Qu'apportent sync.OnceFunc, OnceValue, OnceValues ?** R : Des variantes de Once qui renvoient une fonction mémoïsée, avec valeur et éventuellement erreur (Go 1.21).

**Q : Quand utiliser sync.Map ?** R : Pour des clés écrites une fois et lues souvent, ou des ensembles de clés disjoints par goroutine ; sinon map + mutex.

**Q : sync.Pool est-il un pool de connexions ?** R : Non, un cache d'objets temporaires que le GC peut vider à tout moment.

**Q : Que faut-il faire avant de remettre un objet dans un sync.Pool ?** R : Le réinitialiser (Reset).

**Q : Pourquoi préférer atomic.Int64 à atomic.AddInt64 ?** R : Le type empêche les accès non atomiques accidentels et gère l'alignement.

**Q : Comment partager une configuration rechargée à chaud ?** R : atomic.Pointer[Config] vers une copie immuable remplacée en bloc.

**Q : C'est quoi une relation happens-before ?** R : La garantie qu'une opération est visible et ordonnée avant une autre (channels, verrous, Once, WaitGroup, atomiques).

**Q : La fin d'une goroutine est-elle synchronisée ?** R : Non, il faut un mécanisme explicite (WaitGroup, channel).

**Q : Data race vs race condition ?** R : Accès concurrent non synchronisé vs défaut logique dépendant de l'ordre d'exécution.

**Q : Comment détecter une data race ?** R : Avec le détecteur de courses : go test -race.

**Q : Le détecteur de courses a-t-il des faux positifs ?** R : Non, mais il ne détecte que les courses effectivement exécutées.

**Q : Pourquoi ne pas activer -race en production ?** R : Surcoût important en CPU et mémoire.

**Q : C'est quoi singleflight ?** R : Un mécanisme qui déduplique des appels concurrents pour une même clé : un seul s'exécute, les autres partagent le résultat.

**Q : Une interface ou une string peuvent-elles être lues à moitié écrites ?** R : Oui en cas de data race, ce sont des valeurs multi-mots.

**Q : Comment éviter un deadlock avec plusieurs mutex ?** R : Les acquérir toujours dans le même ordre, et ne jamais appeler de code externe sous verrou.

### Questions de situation

**Q : "Implémente un cache thread-safe avec expiration simple."**
R :
```go
type entree[V any] struct {
    val    V
    expire time.Time
}

type Cache[K comparable, V any] struct {
    mu   sync.RWMutex
    data map[K]entree[V]
    ttl  time.Duration
    now  func() time.Time      // injectable pour les tests
}

func NewCache[K comparable, V any](ttl time.Duration) *Cache[K, V] {
    return &Cache[K, V]{data: make(map[K]entree[V]), ttl: ttl, now: time.Now}
}

func (c *Cache[K, V]) Get(k K) (V, bool) {
    c.mu.RLock()
    e, ok := c.data[k]
    c.mu.RUnlock()
    if !ok || c.now().After(e.expire) {
        var zero V
        return zero, false
    }
    return e.val, true
}

func (c *Cache[K, V]) Set(k K, v V) {
    c.mu.Lock()
    c.data[k] = entree[V]{val: v, expire: c.now().Add(c.ttl)}
    c.mu.Unlock()
}
```
"RWMutex parce que les lectures dominent. L'horloge est injectable pour tester l'expiration sans attendre. Il manque une purge des entrées expirées (une goroutine périodique arrêtée par un contexte, ou une purge lors des écritures) et une taille maximale ; en production j'utiliserais une bibliothèque éprouvée. Et pour éviter que 100 requêtes rechargent la même clé expirée, j'ajouterais `singleflight` autour du chargement."

**Q : "Un collègue protège un compteur avec un channel bufferisé de taille 1 utilisé comme verrou."**
R : "Ça fonctionne, mais c'est une complexité sans bénéfice : c'est plus lent qu'un mutex, moins lisible, et un oubli de libération bloque tout. Pour un compteur, `atomic.Int64` suffit ; pour un état plus riche, un `sync.Mutex`. Les channels sont faits pour **communiquer** des données et coordonner des goroutines, pas pour protéger un état. Utiliser un mutex quand c'est la solution la plus simple n'a rien de honteux en Go."

**Q : "La CI passe, mais en production des valeurs incohérentes apparaissent sous charge."**
R : "Je soupçonnerais une **data race** que les tests ne déclenchent pas. Je lancerais la suite avec `-race`, en ajoutant des tests qui exercent vraiment la concurrence (plusieurs goroutines appelant la même méthode en boucle). Si les tests ne suffisent pas, je ferais tourner un build `-race` dans un environnement de charge ou sur une instance canary : le détecteur donnera les deux piles en conflit.
En relisant le code, je chercherais les suspects classiques : une map partagée, un slice auquel plusieurs goroutines ajoutent des éléments, un champ de struct modifié par un handler HTTP (les handlers s'exécutent en parallèle), une variable de boucle capturée sur une version de Go antérieure à 1.22."

═══════════════════════════════════════════
# 9. LE PACKAGE CONTEXT : ANNULATION, DÉLAIS & VALEURS
═══════════════════════════════════════════

### Vue d'ensemble

Dans un serveur, une requête déclenche souvent une cascade de travail : appels à une base, à d'autres services, goroutines en parallèle. Si le client se déconnecte ou si le délai est dépassé, tout ce travail doit s'arrêter pour ne pas gaspiller de ressources. Go a standardisé ce besoin avec `context.Context` (dans la bibliothèque standard depuis Go 1.7) : un objet qui transporte un **signal d'annulation**, une **échéance**, et quelques **valeurs de portée requête**, à travers toutes les couches d'un programme.

La convention est universelle : toute fonction qui fait de l'I/O ou qui peut durer prend un `ctx context.Context` en **premier paramètre**. Ne pas propager le contexte, c'est produire des requêtes "orphelines" qui continuent de tourner après que plus personne n'attend leur résultat, et des fuites de goroutines.

### Cours

**L'interface** :
```go
type Context interface {
    Deadline() (deadline time.Time, ok bool)
    Done() <-chan struct{}     // fermé quand le contexte est annulé ou expiré
    Err() error                // nil, context.Canceled ou context.DeadlineExceeded
    Value(key any) any
}
```

**Contextes racines** : `context.Background()` (racine du programme, de `main`, des tests, des jobs) et `context.TODO()` (marqueur temporaire quand on ne sait pas encore quel contexte passer). Dans un handler HTTP, le contexte de la requête : `r.Context()`, annulé quand le client se déconnecte ou que la requête se termine. Dans un test, `t.Context()` (Go 1.24), annulé à la fin du test.

**Dériver un contexte** (chaque dérivation renvoie un enfant ; annuler le parent annule tous ses enfants, jamais l'inverse) :
```go
ctx, cancel := context.WithCancel(parent)
ctx, cancel := context.WithTimeout(parent, 2*time.Second)
ctx, cancel := context.WithDeadline(parent, time.Now().Add(2*time.Second))
defer cancel()   // TOUJOURS, même si le délai expirera de lui-même
```
Ne pas appeler `cancel` laisse les ressources du contexte (timer, enregistrement auprès du parent) jusqu'à l'expiration du parent : `go vet` (analyseur `lostcancel`) le signale.

**Variantes ajoutées depuis** :
- `WithCancelCause` (Go 1.20) : annuler avec une **cause** précise, récupérable avec `context.Cause(ctx)` (plus informatif que `context.Canceled`).
- `WithTimeoutCause`, `WithDeadlineCause` (Go 1.21) : préciser la cause en cas d'expiration.
- `WithoutCancel` (Go 1.21) : un contexte qui conserve les **valeurs** du parent mais **pas son annulation** : pour une tâche qui doit se poursuivre après la fin de la requête (écrire un audit, envoyer une métrique).
- `AfterFunc` (Go 1.21) : exécuter une fonction quand le contexte est annulé (par exemple fermer une connexion pour débloquer une lecture qui ne connaît pas les contextes).
- `signal.NotifyContext` (package `os/signal`) : un contexte annulé à la réception de SIGINT/SIGTERM, idéal pour l'arrêt propre ; depuis Go 1.26, il est annulé avec une cause qui indique le signal reçu.

**Respecter l'annulation dans son code** :
```go
func traiterTout(ctx context.Context, items []Item) error {
    for _, it := range items {
        if err := ctx.Err(); err != nil {   // vérification peu coûteuse
            return err
        }
        if err := traiter(ctx, it); err != nil {
            return err
        }
    }
    return nil
}

select {
case res := <-resultats:
    return res, nil
case <-ctx.Done():
    return nil, context.Cause(ctx)
}
```
Les bibliothèques standard et la plupart des clients respectent le contexte : `http.NewRequestWithContext`, `db.QueryContext`, `ExecContext`, clients gRPC, Redis, Kafka. Un appel sans version `Context` (`db.Query` au lieu de `db.QueryContext`) n'est pas annulable.

**Valeurs de contexte** :
```go
type cleRequestID struct{}               // type non exporté : pas de collision

func AvecRequestID(ctx context.Context, id string) context.Context {
    return context.WithValue(ctx, cleRequestID{}, id)
}
func RequestID(ctx context.Context) (string, bool) {
    id, ok := ctx.Value(cleRequestID{}).(string)
    return id, ok
}
```
Règles :
- clé d'un **type non exporté** (jamais une `string`, sous peine de collision entre packages) ;
- accesseurs typés exposés par le package qui définit la clé ;
- réservé aux données de **portée requête** qui traversent les API : identifiant de requête, identité authentifiée, informations de tracing, locale ;
- **jamais** pour passer des paramètres optionnels ou des dépendances (logger, connexion de base) : ça cache les dépendances et casse la vérification de types ;
- la recherche est linéaire dans la chaîne de contextes : pas de grand nombre de valeurs.

**Règles d'usage officielles** :
- `ctx` est le **premier paramètre**, nommé `ctx` ;
- ne **pas stocker** un contexte dans une struct (sauf cas très particuliers documentés) : le passer explicitement à chaque appel ;
- ne jamais passer un contexte `nil` : utiliser `context.TODO()` ;
- un contexte est **immuable** et sûr en concurrence ;
- les fonctions qui reçoivent un contexte ne l'annulent pas elles-mêmes (elles dérivent un enfant si besoin).

**Délais en cascade** : un service qui reçoit une requête avec 2 s restantes ne doit pas lancer un appel aval avec un timeout de 5 s : le contexte hérite de l'échéance la plus proche, ce qui fait respecter naturellement le budget de temps de bout en bout. gRPC propage même les échéances entre services.

**Arrêt propre d'un serveur** :
```go
ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
defer stop()

srv := &http.Server{Addr: ":8080", Handler: mux}
go func() {
    if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
        log.Fatal(err)
    }
}()

<-ctx.Done()                                    // SIGTERM reçu
shutdownCtx, cancel := context.WithTimeout(context.Background(), 20*time.Second)
defer cancel()
srv.Shutdown(shutdownCtx)                        // termine les requêtes en cours
```
Le contexte de `Shutdown` est un **nouveau** contexte : le premier est déjà annulé.

### En situation

- **"Des requêtes en base continuent après que le client a abandonné"** → `Query` au lieu de `QueryContext`, contexte non propagé.
- **"Fuite de goroutines après des timeouts"** → goroutines qui n'écoutent pas `ctx.Done()`.
- **"vet signale lostcancel"** → `cancel` jamais appelé.
- **"Je veux écrire un audit après la réponse, mais le contexte est annulé"** → `context.WithoutCancel`.
- **"Comment passer le logger à toutes les fonctions ?"** → pas par le contexte : injection explicite ; seuls les attributs de requête peuvent y être.

### Questions techniques

**Q : À quoi sert context.Context ?** R : À propager l'annulation, l'échéance et des valeurs de portée requête à travers les appels.

**Q : Background vs TODO ?** R : Racine normale vs marqueur temporaire quand le bon contexte n'est pas encore connu.

**Q : Que renvoie Done() ?** R : Un channel fermé quand le contexte est annulé ou expiré.

**Q : Valeurs possibles de Err() ?** R : nil, context.Canceled ou context.DeadlineExceeded.

**Q : Pourquoi toujours appeler cancel ?** R : Pour libérer immédiatement les ressources du contexte ; vet signale l'oubli (lostcancel).

**Q : Annuler un enfant annule-t-il le parent ?** R : Non, seulement l'inverse.

**Q : WithTimeout vs WithDeadline ?** R : Durée relative vs instant absolu ; même mécanisme.

**Q : Qu'apporte WithCancelCause ?** R : Une cause d'annulation précise, récupérable par context.Cause (Go 1.20).

**Q : À quoi sert WithoutCancel ?** R : Garder les valeurs du parent sans hériter de son annulation (Go 1.21).

**Q : À quoi sert AfterFunc ?** R : Exécuter une fonction à l'annulation du contexte (Go 1.21).

**Q : Comment arrêter proprement sur SIGTERM ?** R : signal.NotifyContext, puis Shutdown du serveur avec un nouveau contexte à délai.

**Q : Quel type utiliser comme clé de valeur ?** R : Un type non exporté propre au package, jamais une string.

**Q : Que mettre dans les valeurs de contexte ?** R : Des données de portée requête : request ID, identité, tracing ; jamais des dépendances ni des paramètres.

**Q : Peut-on stocker un contexte dans une struct ?** R : C'est déconseillé : on le passe en premier paramètre à chaque appel.

**Q : Peut-on passer un contexte nil ?** R : Non, utiliser context.TODO().

**Q : Un contexte est-il thread-safe ?** R : Oui, il est immuable et utilisable par plusieurs goroutines.

**Q : Quel contexte utiliser dans un handler HTTP ?** R : r.Context(), annulé quand le client se déconnecte.

**Q : Comment un délai se propage-t-il aux appels avals ?** R : Un contexte dérivé ne peut pas dépasser l'échéance de son parent.

**Q : db.Query vs db.QueryContext ?** R : La seconde est annulable par le contexte.

### Questions de situation

**Q : "Un endpoint fait trois appels séquentiels à d'autres services et met parfois 30 secondes à échouer."**
R : "Il manque un **budget de temps**. Je dériverais en entrée de handler un contexte avec un timeout global aligné sur ce que le client accepte (par exemple 3 s), et je le passerais à chaque appel. Chaque appel hérite de l'échéance restante, donc le troisième ne peut pas démarrer avec un délai plus long que ce qui reste.
Je vérifierais que chaque client utilise vraiment le contexte (`NewRequestWithContext`, clients gRPC) et que le client HTTP a aussi ses propres timeouts de connexion. Enfin, si les appels sont indépendants, je les lancerais en parallèle avec `errgroup` pour diviser la latence."

**Q : "Tu dois écrire un log d'audit après chaque requête, même si le client se déconnecte."**
R : "Si j'utilise `r.Context()` directement, l'écriture sera annulée quand le client part. J'utiliserais `context.WithoutCancel(r.Context())` : on garde les valeurs utiles (identifiant de requête, utilisateur, trace) sans hériter de l'annulation, avec un timeout propre pour que l'écriture ne bloque pas indéfiniment.
Si l'audit est critique, je ne le ferais pas en ligne mais via une file (ou l'outbox en base dans la même transaction), pour qu'il survive aussi à un crash du processus."

═══════════════════════════════════════════
# 10. LE RUNTIME : SCHEDULER, MÉMOIRE & GARBAGE COLLECTOR
═══════════════════════════════════════════

### Vue d'ensemble

Chaque binaire Go embarque un **runtime** : un ordonnanceur de goroutines, un allocateur mémoire, un ramasse-miettes, la gestion des piles et les profils. Ce runtime est ce qui rend les goroutines si légères et la concurrence si simple, et il est remarquablement peu configurable : quelques variables d'environnement (`GOMAXPROCS`, `GOGC`, `GOMEMLIMIT`, `GODEBUG`) suffisent.

Pour un poste confirmé, on attend de savoir expliquer le modèle **G-M-P** du scheduler, la préemption, le fonctionnement du **GC concurrent** (et son évolution récente, *Green Tea*, défaut depuis Go 1.26), l'**escape analysis**, et comment régler `GOMEMLIMIT` et `GOMAXPROCS` en conteneur.

### Cours

**Le modèle G-M-P** :
- **G** (goroutine) : la pile, le compteur d'instructions, l'état.
- **M** (machine) : un thread de l'OS.
- **P** (processor) : un contexte d'exécution logique qui possède une **file locale** de goroutines prêtes. Il y a exactement `GOMAXPROCS` P.

Un M doit détenir un P pour exécuter du code Go. Le scheduler exécute les G de la file locale du P ; quand elle est vide, il prend dans la **file globale**, scrute le réseau (*netpoller*), puis **vole** la moitié de la file d'un autre P (*work stealing*). Une goroutine qui bloque sur un **appel système** libère son P, qu'un autre M peut prendre pour continuer à exécuter les autres goroutines. Les I/O réseau ne bloquent pas de thread : la goroutine est garée et le **netpoller** (epoll, kqueue, IOCP) la réveille quand la socket est prête. C'est ce qui permet du code réseau "bloquant" en apparence et scalable en réalité.

**Préemption** : depuis Go 1.14, la préemption est **asynchrone** (par signal) : une boucle serrée sans appel de fonction ne peut plus monopoliser un P et retarder le GC ou les autres goroutines. Avant, la préemption n'avait lieu qu'aux appels de fonctions.

**GOMAXPROCS** : nombre de P, donc nombre maximal de goroutines exécutant du code Go **en même temps**. Par défaut, le nombre de CPU logiques. Depuis **Go 1.25**, le runtime est **conscient des conteneurs** : sous Linux, il tient compte de la limite CPU du cgroup (par exemple une limite Kubernetes de 2 CPU donne GOMAXPROCS = 2 au lieu du nombre de cœurs de la machine) et la réajuste périodiquement si elle change. Avant 1.25, on utilisait la bibliothèque `go.uber.org/automaxprocs` pour éviter un GOMAXPROCS trop élevé, qui provoquait du *throttling* CPU et des latences. `runtime.GOMAXPROCS(n)` le fixe explicitement.

**Piles des goroutines** : environ 2 Ko au départ, **contiguës et extensibles** : quand une pile est pleine, le runtime alloue une pile deux fois plus grande et y **copie** l'ancienne (en ajustant les pointeurs). La pile maximale par défaut est de 1 Go sur 64 bits ; une récursion infinie se termine par `fatal error: stack overflow`.

**Allocation mémoire** : allocateur inspiré de TCMalloc, organisé en **classes de taille** ; chaque P a un cache local (*mcache*) qui permet d'allouer de petits objets sans verrou. Les petits objets (jusqu'à 32 Ko) passent par ces caches, les gros sont alloués directement. Go 1.27 ajoute des routines d'allocation spécialisées par taille, qui réduisent jusqu'à 30 % le coût de certaines petites allocations.

**Pile vs heap et escape analysis** : le compilateur place une variable sur la **pile** si elle ne survit pas à la fonction, ce qui ne coûte rien au GC. Elle **s'échappe** dans le heap si son adresse est retournée, stockée dans une variable globale ou un objet du heap, capturée par une goroutine ou une closure qui survit, si sa taille n'est connue qu'à l'exécution, ou dans certains cas de conversion vers une interface. Voir : `go build -gcflags=-m`. Réduire les allocations est le levier de performance numéro un en Go.

**Le garbage collector** :
- **Tri-color mark and sweep, concurrent, non générationnel, non compactant**. Les objets sont colorés blanc (non vus), gris (vus, à scanner), noir (scannés) ; à la fin du marquage, les blancs sont libérés.
- **Concurrent** : le marquage s'exécute en même temps que le programme, avec deux très courtes pauses *stop-the-world* (généralement bien en dessous de la milliseconde). Une **barrière d'écriture** maintient la cohérence pendant que le programme modifie des pointeurs.
- **Mark assist** : une goroutine qui alloue beaucoup pendant un cycle est mise à contribution pour aider au marquage, ce qui ralentit les gros allocateurs plutôt que de laisser le heap exploser. C'est souvent la vraie source de latence en Go : pas les pauses, mais le temps volé aux goroutines.
- **Non générationnel et non compactant** : choix assumé, compensé par l'escape analysis (beaucoup d'objets courts vivent sur la pile) et par un allocateur par classes de taille qui limite la fragmentation.
- **Green Tea** (expérimental en Go 1.25, **par défaut depuis Go 1.26**) : nouvel algorithme de marquage qui traite la mémoire par **pages** plutôt qu'objet par objet, avec une meilleure localité mémoire et une meilleure montée en charge sur de nombreux cœurs ; l'équipe Go annonce **10 à 40 % de surcoût GC en moins** sur des programmes qui sollicitent beaucoup le GC, et environ 10 % de mieux encore sur les processeurs récents qui supportent les instructions vectorielles utilisées pour le scan.

**Régler le GC** :
- **GOGC** (défaut 100) : un cycle démarre quand le heap a grossi de GOGC % depuis le heap vivant du dernier cycle. GOGC=200 : moins de cycles, plus de mémoire. `GOGC=off` désactive le déclenchement par croissance.
- **GOMEMLIMIT** (Go 1.19) : une **limite souple** de mémoire totale du runtime. Quand on s'en approche, le GC se déclenche plus souvent pour rester dessous. Indispensable en conteneur : on le fixe à environ 90 % de la limite mémoire du pod, ce qui évite les OOMKilled dus à un heap qui grossit entre deux cycles. Attention : si le heap vivant dépasse vraiment la limite, le programme passe son temps à collecter (*death spiral*), d'où le caractère souple et une protection interne qui plafonne le temps CPU consacré au GC.
- `debug.SetGCPercent`, `debug.SetMemoryLimit` pour le faire depuis le code.
- `runtime.GC()` force un cycle (rarement utile hors tests et benchmarks).

**Observer le GC** : `GODEBUG=gctrace=1` (une ligne par cycle : durée, heap avant/après, CPU), `runtime.ReadMemStats` (coûteux, provoque une pause) ou mieux `runtime/metrics`, les métriques Prometheus `go_gc_duration_seconds`, `go_memstats_*`, le **trace** d'exécution (`go tool trace`), les profils `heap` et `allocs` de pprof.

**Finaliseurs et nettoyage** : `runtime.SetFinalizer` (historique, nombreux pièges : ordre non garanti, objet ressuscité) est remplacé par **`runtime.AddCleanup`** (Go 1.24), plus sûr. Le package **`weak`** (Go 1.24) fournit des pointeurs faibles (`weak.Pointer[T]`) pour des caches qui ne doivent pas empêcher la collecte. Le package `unique` (Go 1.23) fait de l'*interning* (canonicaliser des valeurs égales pour économiser la mémoire et comparer par pointeur).

**Le profil de fuites de goroutines** (Go 1.27) : le GC sert aussi à détecter les goroutines bloquées pour toujours, quand la primitive sur laquelle elles attendent n'est plus atteignable par aucune goroutine vivante (voir le chapitre Concurrence).

**GODEBUG** : réglages de compatibilité et de diagnostic (`gctrace`, `schedtrace`, et les comportements historiques activables via la directive `godebug` du go.mod ou `//go:debug` dans le code). Les nouveaux comportements dépendent de la version `go` déclarée dans le go.mod.

**cgo** : appeler du C depuis Go. Coût d'un appel bien plus élevé qu'un appel Go (réduit d'environ 30 % en Go 1.26 mais toujours notable), le thread est bloqué pendant l'appel C, la compilation croisée se complique, et le binaire n'est plus statique. "Cgo is not Go" : on l'évite sauf nécessité (certains pilotes SQLite, bibliothèques natives).

### En situation

- **"Le pod est OOMKilled alors que l'usage moyen est bas"** → le heap grossit entre deux cycles → GOMEMLIMIT à ~90 % de la limite.
- **"Latences élevées et throttling CPU dans Kubernetes"** → GOMAXPROCS trop élevé sur un Go antérieur à 1.25 → mettre à jour, ou automaxprocs.
- **"Le GC consomme 30 % du CPU"** → trop d'allocations → profil allocs, escape analysis, sync.Pool, préallocation.
- **"Une boucle de calcul bloque tout le programme"** → plus vrai depuis la préemption asynchrone (1.14).
- **"Faut-il un GC générationnel ?"** → Go a choisi l'escape analysis et un GC concurrent ; Green Tea améliore le marquage.

### Questions techniques

**Q : Que contient le runtime Go ?** R : Le scheduler de goroutines, l'allocateur, le GC, la gestion des piles, le netpoller et les profils.

**Q : C'est quoi le modèle G-M-P ?** R : Goroutines (G) exécutées par des threads (M) qui détiennent un processeur logique (P) avec une file locale ; GOMAXPROCS P au total.

**Q : C'est quoi le work stealing ?** R : Un P inactif vole la moitié de la file d'un autre P.

**Q : Que se passe-t-il quand une goroutine fait un appel système bloquant ?** R : Son thread libère le P, qu'un autre thread reprend pour exécuter les autres goroutines.

**Q : Comment Go gère-t-il les I/O réseau sans bloquer de threads ?** R : Le netpoller (epoll/kqueue/IOCP) gare la goroutine et la réveille quand la socket est prête.

**Q : Depuis quand la préemption est-elle asynchrone ?** R : Go 1.14.

**Q : Que contrôle GOMAXPROCS ?** R : Le nombre de goroutines qui exécutent du code Go simultanément (nombre de P).

**Q : Qu'a changé Go 1.25 pour GOMAXPROCS ?** R : Il tient compte de la limite CPU du cgroup (conteneurs) et s'ajuste dynamiquement.

**Q : Comment grandit la pile d'une goroutine ?** R : Le runtime alloue une pile deux fois plus grande et copie l'ancienne.

**Q : Comment fonctionne le GC de Go ?** R : Mark and sweep tricolore, concurrent, non générationnel, non compactant, avec barrière d'écriture.

**Q : Le GC de Go provoque-t-il de longues pauses ?** R : Non, les pauses stop-the-world sont très courtes ; le coût principal est le CPU de marquage et le mark assist.

**Q : C'est quoi le mark assist ?** R : Une goroutine qui alloue pendant un cycle aide au marquage, ce qui la ralentit.

**Q : C'est quoi Green Tea ?** R : Le nouvel algorithme de marquage par pages, par défaut depuis Go 1.26, qui réduit le surcoût du GC.

**Q : Que règle GOGC ?** R : La croissance du heap (en %) qui déclenche un cycle ; 100 par défaut.

**Q : Que règle GOMEMLIMIT ?** R : Une limite souple de mémoire totale ; le GC se déclenche plus souvent à son approche (Go 1.19).

**Q : Quelle valeur de GOMEMLIMIT en conteneur ?** R : Environ 90 % de la limite mémoire du conteneur.

**Q : Pourquoi le GC de Go n'est-il pas générationnel ?** R : L'escape analysis place beaucoup d'objets courts sur la pile, et le GC concurrent garde des pauses faibles sans la complexité générationnelle.

**Q : Comment observer le GC ?** R : GODEBUG=gctrace=1, runtime/metrics, profils pprof, go tool trace.

**Q : SetFinalizer vs AddCleanup ?** R : AddCleanup (Go 1.24) remplace SetFinalizer avec moins de pièges.

**Q : À quoi sert le package weak ?** R : À créer des pointeurs faibles qui n'empêchent pas la collecte (Go 1.24).

**Q : Pourquoi éviter cgo ?** R : Appels coûteux, compilation croisée compliquée, binaire non statique, outillage dégradé.

**Q : C'est quoi GODEBUG ?** R : Des réglages de diagnostic et de compatibilité du runtime, contrôlables aussi par la version go du go.mod.

### Questions de situation

**Q : "Ton service Go dans Kubernetes a des pics de latence et des OOMKilled occasionnels."**
R : "Deux réglages à vérifier d'abord. **GOMAXPROCS** : sur un Go antérieur à 1.25, il vaut le nombre de cœurs du nœud, pas la limite du pod ; avec 2 CPU de limite sur un nœud de 32 cœurs, le runtime lance trop de travail en parallèle, le noyau *throttle* le conteneur et la latence explose. Passer à Go 1.25+ ou utiliser automaxprocs.
**GOMEMLIMIT** : sans lui, le GC se déclenche selon GOGC, et le heap peut doubler entre deux cycles et dépasser la limite du pod. Je le fixerais à environ 90 % de la limite mémoire (via une variable d'environnement, alimentée par la Downward API de Kubernetes).
Ensuite, je mesurerais : `gctrace`, profil d'allocations, et je chercherais les allocations évitables. Si le heap vivant est simplement trop grand, il faut augmenter la limite ou réduire la mémoire retenue (caches)."

**Q : "Explique pourquoi on peut avoir 100 000 goroutines mais pas 100 000 threads."**
R : "Un thread de l'OS réserve une pile de l'ordre du mégaoctet, demande au noyau de le créer et de le détruire, et son changement de contexte passe par le noyau. Une goroutine démarre avec une pile de 2 Ko qui grandit à la demande, et elle est ordonnancée en espace utilisateur par le runtime Go, qui la fait tourner sur un petit nombre de threads (GOMAXPROCS, plus ceux bloqués en appel système).
Surtout, une goroutine qui attend le réseau ne consomme aucun thread : le netpoller la gare et la réveille quand les données arrivent. 100 000 connexions inactives, c'est 100 000 petites piles en mémoire, pas 100 000 threads. Les virtual threads de Java 21 reprennent exactement cette idée."

═══════════════════════════════════════════
# 11. PERFORMANCE, BENCHMARKS & PROFILAGE
═══════════════════════════════════════════

### Vue d'ensemble

Go est rapide par défaut : compilé en natif, avec un GC à faibles pauses et une concurrence bon marché. Mais un programme Go lent l'est presque toujours pour les mêmes raisons : **trop d'allocations** (donc trop de travail pour le GC), de la **contention** sur des verrous, des I/O mal gérées (pas de buffering, pas de pool de connexions), ou un algorithme inadapté.

L'outillage est excellent et intégré : benchmarks dans le package `testing`, **pprof** pour les profils CPU, mémoire, blocage et contention, le **trace** d'exécution, et l'optimisation guidée par profil (**PGO**). La règle reste universelle : **mesurer avant d'optimiser**, puis re-mesurer.

### Cours

**Benchmarks** :
```go
func BenchmarkFormater(b *testing.B) {
    u := Utilisateur{ID: 42, Nom: "Ana"}
    b.ReportAllocs()
    for b.Loop() {           // Go 1.24 : remplace for i := 0; i < b.N; i++
        _ = formater(u)
    }
}
```
- `go test -bench=. -benchmem` : temps par opération (ns/op), octets alloués (B/op), allocations (allocs/op).
- **`b.Loop()`** (Go 1.24) : gère la chauffe et le chronométrage, exécute la configuration une seule fois, et **empêche le compilateur d'éliminer** le corps de la boucle (les arguments et résultats restent vivants). Depuis Go 1.26, il n'empêche plus l'inlining dans le corps, donc tous les benchmarks peuvent l'adopter.
- Ancien style `b.N` : penser à `b.ResetTimer()` après une configuration coûteuse, et à stocker le résultat dans une variable globale pour éviter l'élimination de code mort.
- Comparer proprement deux versions : `-count=10` puis **`benchstat`** (`golang.org/x/perf/cmd/benchstat`) qui calcule la différence et sa significativité statistique.
- `b.RunParallel` pour mesurer sous concurrence ; `-cpu=1,4,8` pour varier GOMAXPROCS.

**pprof** : profils échantillonnés, analysés avec `go tool pprof`.
| Profil | Ce qu'il montre |
|---|---|
| `cpu` (`profile`) | où le CPU est consommé (échantillon toutes les 10 ms) |
| `heap` | mémoire **vivante** (inuse) et allocations cumulées |
| `allocs` | toutes les allocations depuis le démarrage |
| `goroutine` | pile de chaque goroutine (fuites, blocages) |
| `block` | temps passé bloqué (channels, select, mutex) : à activer avec `runtime.SetBlockProfileRate` |
| `mutex` | contention sur les verrous : `runtime.SetMutexProfileFraction` |
| `threadcreate` | création de threads de l'OS |
| `goroutineleak` | goroutines bloquées pour toujours (Go 1.27) |

Obtenir les profils :
- en test : `go test -cpuprofile cpu.out -memprofile mem.out -bench .` ;
- en service : `import _ "net/http/pprof"` expose `/debug/pprof/` sur le `DefaultServeMux` ; **à servir sur un port d'administration interne, jamais publiquement** ;
- par code : `pprof.StartCPUProfile`, `pprof.WriteHeapProfile`.

Analyse : `go tool pprof -http=:8080 cpu.out` ouvre une interface web (vue **flame graph** par défaut depuis Go 1.26), ou en ligne de commande `top`, `list NomFonction`, `web`. Pour la mémoire : `-sample_index=alloc_space` (cumul alloué) vs `inuse_space` (vivant). Comparer deux profils : `-diff_base`.

**Profilage continu** en production : Pyroscope/Grafana, Parca, Datadog Continuous Profiler, Google Cloud Profiler ; surcoût faible, permet de comparer avant/après déploiement.

**Trace d'exécution** (`runtime/trace`, `go test -trace`, `go tool trace`) : chronologie fine des goroutines, du scheduler, des cycles GC, des appels système, des blocages réseau. Indispensable pour comprendre une **latence** (pourquoi une goroutine prête n'a pas tourné, combien de temps le GC a volé). Depuis Go 1.25, le **flight recorder** (`trace.FlightRecorder`) garde en mémoire les dernières secondes de trace et permet de les écrire au moment où un problème est détecté.

**PGO (Profile-Guided Optimization)**, stable depuis Go 1.21 : placer un profil CPU de production nommé `default.pgo` dans le package `main` ; `go build` l'utilise automatiquement pour inliner et dévirtualiser plus agressivement les chemins chauds. Gain typique annoncé : 2 à 14 % de CPU. Le profil peut être rafraîchi régulièrement depuis la production.

**Réduire les allocations, les techniques** :
- **préallouer** slices et maps quand la taille est connue (`make([]T, 0, n)`, `make(map[K]V, n)`) ;
- `strings.Builder` (avec `Grow`) ou `bytes.Buffer` au lieu de `+` en boucle ;
- `strconv` plutôt que `fmt.Sprintf` pour convertir des nombres (beaucoup moins d'allocations) ;
- réutiliser des buffers (`sync.Pool`, ou un buffer conservé dans une struct pour un traitement séquentiel) ;
- éviter les conversions inutiles `[]byte` ↔ `string` ;
- éviter de faire échapper des valeurs (retourner des valeurs plutôt que des pointeurs pour les petites structs, ne pas capturer inutilement dans des closures) ;
- passer `[]byte` en paramètre pour que l'appelant fournisse le buffer (style `AppendXxx`, comme `strconv.AppendInt`, `time.AppendFormat`) ;
- attention aux conversions implicites vers `interface{}` (`fmt.Println(x)`, logging non typé) qui allouent ;
- pour le JSON très sollicité : l'implémentation v2 de Go 1.27, nettement plus rapide en décodage, ou des générateurs de code.

**Autres leviers** :
- **I/O bufferisées** : `bufio.Reader`/`Writer` autour des fichiers et sockets ; oublier le `Flush` final d'un `bufio.Writer` est un bug classique.
- **Réutilisation des connexions HTTP** : un seul `http.Client` partagé, et **lire puis fermer** le corps des réponses pour que la connexion retourne au pool (depuis Go 1.27, la fermeture draine automatiquement une partie du corps restant).
- Pool de connexions de base de données bien dimensionné (`SetMaxOpenConns`, `SetMaxIdleConns`, `SetConnMaxLifetime`).
- **Contention** : profil `mutex` ; réduire la section critique, partitionner (*sharding*) une map protégée en N maps, utiliser des atomiques, `RWMutex` si lecture dominante.
- **Inlining** : le compilateur inline les petites fonctions (voir `-gcflags=-m`) ; les appels par interface ou par fonction pointeur l'empêchent souvent (PGO aide).
- **Vérification de bornes** : le compilateur élimine les *bounds checks* quand il peut prouver la validité ; une vérification explicite en amont (`_ = s[3]`) permet parfois d'en supprimer plusieurs.
- **Disposition mémoire** : structs compactes (ordre des champs), slices de structs plutôt que slices de pointeurs pour la localité (moins de pointeurs à scanner pour le GC).
- **SIMD** : package expérimental `simd/archsimd` (Go 1.26) et `simd` portable (Go 1.27), activables par `GOEXPERIMENT=simd`, pour des calculs vectoriels.

**Méthode** : symptôme (latence p99, CPU, mémoire) → métrique → profil adapté (CPU si le CPU est haut ; heap/allocs si le GC est lourd ; block/mutex/trace si le CPU est bas mais la latence haute) → hypothèse → changement → benchmark comparé avec benchstat → vérification en production.

### En situation

- **"Le CPU est élevé"** → profil CPU, flame graph, chercher les plateaux larges.
- **"Le GC prend beaucoup de CPU"** → profil allocs (`alloc_space`) → réduire les allocations.
- **"Latence élevée, CPU bas"** → attente : verrou (profil mutex/block), I/O, pool de connexions épuisé, trace d'exécution.
- **"Mon optimisation est 30 % plus rapide"** → benchstat sur `-count=10` avant de conclure.
- **"Connexions HTTP qui s'accumulent"** → corps de réponse non lu ou non fermé, nouveau client à chaque requête.
- **"Les endpoints pprof sont accessibles depuis internet"** → faille : port d'admin interne.

### Questions techniques

**Q : Comment écrire un benchmark ?** R : Une fonction BenchmarkXxx(b *testing.B) avec une boucle `for b.Loop()` (ou b.N), lancée par `go test -bench`.

**Q : Qu'apporte b.Loop() ?** R : Gestion automatique de la chauffe et du chronométrage, et protection contre l'élimination du code mesuré (Go 1.24).

**Q : Que signifie -benchmem ?** R : Afficher les octets et le nombre d'allocations par opération.

**Q : À quoi sert benchstat ?** R : Comparer statistiquement les résultats de plusieurs exécutions de benchmarks.

**Q : Quels profils fournit pprof ?** R : cpu, heap, allocs, goroutine, block, mutex, threadcreate, et goroutineleak en Go 1.27.

**Q : inuse_space vs alloc_space ?** R : Mémoire vivante actuellement vs total alloué depuis le démarrage.

**Q : Comment exposer pprof dans un service ?** R : Importer net/http/pprof, sur un port d'administration non public.

**Q : Pourquoi ne pas exposer pprof publiquement ?** R : Il révèle le code et l'état interne et peut être utilisé pour charger le service.

**Q : Profil CPU vs trace d'exécution ?** R : Où le CPU est consommé vs chronologie des goroutines, du scheduler et du GC pour comprendre une latence.

**Q : C'est quoi le flight recorder ?** R : Un enregistrement continu en mémoire des dernières secondes de trace, exportable à la demande (Go 1.25).

**Q : C'est quoi le PGO ?** R : L'optimisation guidée par un profil CPU de production (default.pgo), pour mieux inliner et dévirtualiser.

**Q : Comment réduire les allocations ?** R : Préallouer, réutiliser les buffers, strings.Builder, strconv plutôt que fmt, éviter les échappements.

**Q : Pourquoi fmt.Sprintf est-il coûteux ?** R : Il passe par la réflexion et convertit les arguments en interfaces, ce qui alloue.

**Q : Pourquoi un seul http.Client partagé ?** R : Pour réutiliser son pool de connexions ; en créer un par requête casse la réutilisation.

**Q : Que faut-il faire du corps d'une réponse HTTP ?** R : Le lire (ou le vider) et le fermer pour que la connexion soit réutilisée.

**Q : Comment réduire la contention sur une map partagée ?** R : La partitionner en plusieurs shards avec chacun son verrou, ou réduire la section critique.

**Q : Comment voir les décisions d'inlining ?** R : `go build -gcflags=-m`.

**Q : Slice de structs ou slice de pointeurs ?** R : De structs pour la localité mémoire et moins de travail pour le GC, sauf si les éléments sont gros ou partagés.

**Q : Quel profil pour une latence élevée avec un CPU bas ?** R : block, mutex et la trace d'exécution.

### Questions de situation

**Q : "Un endpoint JSON très sollicité consomme trop de CPU. Comment tu l'optimises ?"**
R : "D'abord un profil CPU en production ou en test de charge, pour vérifier que l'encodage est vraiment le coût principal. Si oui, plusieurs pistes par ordre d'effort. Encoder directement dans le `ResponseWriter` avec un `json.Encoder` plutôt que `Marshal` puis `Write`. Réduire la taille de la réponse (pagination, champs inutiles). Réutiliser les buffers avec un `sync.Pool`. Passer à Go 1.27, dont l'implémentation JSON repose sur la v2, nettement plus rapide en décodage. Si c'est encore insuffisant, un encodeur généré par code (sans réflexion) ou un format binaire (Protobuf) pour les clients internes.
Chaque étape validée par un benchmark comparé avec `benchstat`, puis par la métrique CPU en production."

**Q : "Le service a une latence p99 élevée alors que le CPU est à 20 %."**
R : "CPU bas et latence haute signifient que le programme **attend**. J'activerais les profils `block` et `mutex` pour voir si des goroutines attendent un verrou contendu ou un channel. Je prendrais un profil `goroutine` pour voir où elles sont bloquées en masse (souvent un pool de connexions de base épuisé, ou un appel réseau lent sans timeout). La trace d'exécution montrerait si le problème vient du GC (mark assist) ou du scheduler.
Les suspects habituels : `SetMaxOpenConns` trop bas, un mutex global autour d'une opération lente, des appels HTTP sans réutilisation de connexions, ou un service aval lent qu'on appelle en série au lieu de paralléliser."

═══════════════════════════════════════════
# 12. GO MODERNE : ITÉRATEURS, NOUVEAUTÉS & VERSIONS
═══════════════════════════════════════════

### Vue d'ensemble

Go évolue lentement et prudemment, mais il évolue. Depuis 2022, les changements ont été plus nombreux qu'en dix ans : generics (1.18), packages `slices`/`maps`/`cmp` et `log/slog` (1.21), sémantique des variables de boucle et `range` sur les entiers (1.22), **itérateurs** (`range` sur des fonctions, 1.23), Swiss Tables et `os.Root` (1.24), GOMAXPROCS conscient des conteneurs (1.25), GC Green Tea et `go fix` modernisé (1.26), méthodes génériques et `encoding/json/v2` (1.27).

En entretien, on attend de savoir situer ces évolutions, d'écrire un itérateur simple, et de connaître le **calendrier** : une version majeure tous les **six mois** (février et août), et seules les **deux dernières** sont supportées (correctifs de sécurité). En octobre 2026, ce sont **Go 1.26 et Go 1.27**.

### Cours

**Itérateurs (Go 1.23)** : `for range` accepte des **fonctions** de la forme :
```go
func(yield func() bool)
func(yield func(V) bool)       // iter.Seq[V]
func(yield func(K, V) bool)    // iter.Seq2[K, V]
```
L'itérateur appelle `yield` pour chaque élément ; si `yield` renvoie `false` (la boucle a fait `break` ou `return`), l'itérateur doit s'arrêter.
```go
func Pairs(n int) iter.Seq[int] {
    return func(yield func(int) bool) {
        for i := 0; i < n; i += 2 {
            if !yield(i) {
                return
            }
        }
    }
}

for v := range Pairs(10) {
    fmt.Println(v)        // 0 2 4 6 8
}
```
- `iter.Seq[V]` et `iter.Seq2[K, V]` sont les types standards à exposer dans les API.
- **Itérateurs "pull"** : `iter.Pull(seq)` renvoie des fonctions `next()` et `stop()` pour consommer un itérateur pas à pas (fusionner deux séquences, par exemple) ; toujours appeler `stop`.
- Fonctions de la bibliothèque standard : `slices.All`, `slices.Values`, `slices.Backward`, `slices.Collect`, `slices.Sorted`, `slices.Chunk`, `maps.Keys`, `maps.Values`, `maps.All`, `maps.Collect`, `strings.Lines`, `strings.SplitSeq`, `strings.FieldsSeq` (1.24), `bytes.Lines`, et les itérateurs de `reflect` (1.26 : `Type.Fields`, `Type.Methods`...).
- Exemple idiomatique : clés d'une map triées : `for _, k := range slices.Sorted(maps.Keys(m)) { ... }`.
- Avantages : parcourir une structure sans exposer son implémentation ni allouer un slice intermédiaire, itérer sur des sources paresseuses (lignes d'un fichier, résultats d'une base, pages d'une API), composer des transformations.
- Avant Go 1.23, on utilisait des méthodes `Next()`/`Value()` (style `bufio.Scanner`, `sql.Rows`), des callbacks, ou des channels (coûteux et sujets aux fuites).

**range sur un entier (Go 1.22)** : `for i := range 10 { }` parcourt 0 à 9 ; `for range n { }` répète n fois.

**Variables de boucle par itération (Go 1.22)** : pour les modules en `go 1.22`+, chaque itération de `for` a sa propre variable (fin du piège des closures et goroutines qui capturent la variable de boucle). `go fix` et le linter `copyloopvar` suppriment les anciennes copies `v := v` devenues inutiles.

**Fonctions intégrées récentes** : `min`, `max` (variadiques, sur types ordonnés) et `clear` (vide une map ou met à zéro un slice) en Go 1.21 ; `new(expr)` en Go 1.26.

**Packages récents à connaître** :
| Package | Version | Rôle |
|---|---|---|
| `slices`, `maps`, `cmp` | 1.21 | fonctions génériques sur slices, maps, comparaison |
| `log/slog` | 1.21 | logs structurés |
| `math/rand/v2` | 1.22 | nouvelle API d'aléatoire (premier package "v2" de la bibliothèque standard) |
| `iter` | 1.23 | types et fonctions d'itérateurs |
| `unique` | 1.23 | interning de valeurs |
| `structs` | 1.23 | marqueurs de disposition mémoire (`HostLayout`) |
| `weak` | 1.24 | pointeurs faibles |
| `crypto/mlkem`, `crypto/hkdf`, `crypto/pbkdf2`, `crypto/sha3` | 1.24 | cryptographie, dont post-quantique |
| `testing/synctest` | 1.25 (expérimental en 1.24) | tester du code concurrent avec une horloge virtuelle |
| `crypto/hpke` | 1.26 | chiffrement hybride à clé publique (RFC 9180) |
| `encoding/json/v2`, `encoding/json/jsontext` | 1.27 (expérimental en 1.25) | nouvelle API JSON, plus stricte et plus rapide |
| `uuid` | 1.27 | génération et analyse d'UUID |
| `crypto/mldsa` | 1.27 | signatures post-quantiques ML-DSA |
| `simd` | 1.27 (expérimental) | SIMD portable |

**Historique des versions (l'essentiel)** :
| Version | Date | Apports marquants |
|---|---|---|
| 1.0 | 2012 | promesse de compatibilité Go 1 |
| 1.5 | 2015 | compilateur et runtime écrits en Go, GC concurrent, GOMAXPROCS = nombre de CPU par défaut |
| 1.7 | 2016 | `context` dans la bibliothèque standard |
| 1.11 | 2018 | **modules** (expérimental), WebAssembly |
| 1.13 | 2019 | wrapping d'erreurs (`%w`, `errors.Is/As`), proxy de modules par défaut |
| 1.14 | 2020 | préemption asynchrone, defer quasi gratuit |
| 1.16 | 2021 | `embed`, `io/fs`, modules par défaut |
| 1.18 | 2022 | **generics**, fuzzing natif, workspaces (`go.work`) |
| 1.19 | 2022 | modèle mémoire révisé, types atomiques, **GOMEMLIMIT** |
| 1.20 | 2023 | `errors.Join` et plusieurs `%w`, PGO en preview, `context.WithCancelCause` |
| 1.21 | 2023 | `min`/`max`/`clear`, `slices`/`maps`/`cmp`, `log/slog`, **directive toolchain**, PGO stable, `sync.OnceValue` |
| 1.22 | 2024 | variables de boucle par itération, `range` sur entier, **routage HTTP amélioré** (méthodes et jokers dans `ServeMux`), `math/rand/v2` |
| 1.23 | 2024 | **itérateurs** (range sur fonctions), `iter`, `unique`, timers collectés immédiatement et channels de timers synchrones, télémétrie opt-in |
| 1.24 | 2025 | alias génériques, **maps Swiss Table**, directive `tool` dans go.mod, `testing.B.Loop`, `os.Root`, `runtime.AddCleanup`, `weak`, `omitzero` pour JSON, module cryptographique FIPS 140-3 |
| 1.25 | 2025 | **GOMAXPROCS conscient des conteneurs**, `sync.WaitGroup.Go`, `testing/synctest` stable, GC Green Tea et json/v2 expérimentaux, trace flight recorder, suppression des *core types* de la spécification |
| 1.26 | 2026 | `new(expr)`, contraintes génériques autoréférentes, **GC Green Tea par défaut**, `go fix` refondu avec les *modernizers*, `errors.AsType`, cgo ~30 % plus rapide, `slog.NewMultiHandler`, profil goroutineleak expérimental, échanges de clés TLS post-quantiques hybrides par défaut |
| 1.27 | 2026 | **méthodes génériques**, `encoding/json/v2` stable (et `encoding/json` reposant sur la v2), package `uuid`, profil **goroutineleak** stable, allocations de petits objets plus rapides, `crypto/mldsa`, `strings.CutLast`, `simd` expérimental |

**Politique de support et compatibilité** : chaque version majeure est maintenue jusqu'à la sortie de la deuxième version qui la suit (environ un an). La **directive `go`** du go.mod fixe la version minimale du langage et active les comportements de cette version (sémantique des boucles, réglages GODEBUG par défaut) ; la **directive `toolchain`** (1.21) indique la chaîne d'outils préférée, que la commande `go` peut télécharger automatiquement (`GOTOOLCHAIN`). Depuis Go 1.26, `go mod init` choisit par défaut une version `go` inférieure d'un cran à la chaîne d'outils, pour rester compatible avec les versions encore supportées.

**go fix et les modernizers (Go 1.26)** : `go fix ./...` applique des réécritures sûres vers les idiomes modernes : `min`/`max` au lieu de `if`, `slices.Contains` au lieu d'une boucle, `for range n`, `wg.Go`, suppression des copies de variable de boucle, types atomiques (1.27), `strings.CutPrefix`... Il s'appuie sur le même framework d'analyse que `go vet`, et la directive `//go:fix inline` permet à un auteur de bibliothèque d'automatiser la migration de ses utilisateurs vers une nouvelle API.

**Télémétrie** (1.23) : la chaîne d'outils peut collecter des statistiques d'usage anonymes, uniquement si l'utilisateur l'active (`go telemetry on`).

### En situation

- **"Je veux parcourir mon arbre avec range"** → méthode qui renvoie un `iter.Seq[T]`.
- **"Mon code a des `v := v` partout"** → inutiles depuis Go 1.22 → `go fix`.
- **"Quelle version de Go choisir pour un nouveau projet ?"** → la dernière stable, et une directive `go` compatible avec les versions supportées.
- **"Le CI utilise une autre version de Go que mon poste"** → directive toolchain et GOTOOLCHAIN.
- **"Comment moderniser une vieille base de code ?"** → montée de version progressive, `go fix`, linters.

### Questions techniques

**Q : À quel rythme sortent les versions de Go ?** R : Tous les six mois, en février et en août.

**Q : Quelles versions sont supportées ?** R : Les deux dernières versions majeures (1.26 et 1.27 en octobre 2026).

**Q : Quelle forme doit avoir une fonction pour être utilisée avec range (Go 1.23) ?** R : `func(yield func(...) bool)` avec zéro, un ou deux paramètres dans yield.

**Q : Que doit faire un itérateur quand yield renvoie false ?** R : S'arrêter immédiatement.

**Q : iter.Seq vs iter.Seq2 ?** R : Séquence de valeurs vs séquence de paires clé-valeur.

**Q : À quoi sert iter.Pull ?** R : Transformer un itérateur push en fonctions next/stop pour le consommer pas à pas.

**Q : Comment parcourir les clés d'une map dans l'ordre ?** R : `slices.Sorted(maps.Keys(m))`.

**Q : Que fait `for i := range 10` ?** R : Parcourt les entiers de 0 à 9 (Go 1.22).

**Q : Quelles fonctions intégrées sont arrivées en 1.21 ?** R : min, max et clear.

**Q : Quel est le premier package "v2" de la bibliothèque standard ?** R : math/rand/v2 (Go 1.22).

**Q : Qu'a apporté Go 1.22 au routage HTTP ?** R : Des motifs avec méthode et jokers dans ServeMux (`GET /users/{id}`).

**Q : Qu'a apporté Go 1.24 aux maps ?** R : Une implémentation de type Swiss Table, plus rapide.

**Q : Quelle est la grande nouveauté de Go 1.27 pour le langage ?** R : Les méthodes génériques.

**Q : Qu'est devenu encoding/json en Go 1.27 ?** R : Il repose sur l'implémentation v2, et encoding/json/v2 est disponible en version stable.

**Q : À quoi sert la directive go du go.mod ?** R : Fixer la version minimale du langage et activer les comportements correspondants.

**Q : Directive go vs directive toolchain ?** R : Version minimale du langage vs version de la chaîne d'outils préférée.

**Q : Que fait go fix depuis Go 1.26 ?** R : Il applique des modernizers qui réécrivent le code vers les idiomes et API récents.

**Q : Depuis quand les modules existent-ils ?** R : Go 1.11 (expérimental), par défaut depuis Go 1.16.

**Q : Depuis quand context est-il dans la bibliothèque standard ?** R : Go 1.7.

**Q : Depuis quand le wrapping d'erreurs existe-t-il ?** R : Go 1.13.

**Q : Que permet os.Root (Go 1.24) ?** R : Accéder à des fichiers en restant confiné dans un répertoire, contre le path traversal.

### Questions de situation

**Q : "Écris un itérateur sur les nœuds d'un arbre binaire, en ordre infixe."**
R :
```go
type Noeud[T any] struct {
    Val           T
    Gauche, Droit *Noeud[T]
}

func (n *Noeud[T]) Infixe() iter.Seq[T] {
    return func(yield func(T) bool) {
        n.parcourir(yield)
    }
}

func (n *Noeud[T]) parcourir(yield func(T) bool) bool {
    if n == nil {
        return true
    }
    return n.Gauche.parcourir(yield) && yield(n.Val) && n.Droit.parcourir(yield)
}

for v := range racine.Infixe() {
    if v > 100 { break }       // l'itérateur s'arrête proprement
}
```
"La fonction récursive renvoie `false` dès que `yield` renvoie `false`, ce qui propage l'arrêt dans toute la récursion : un `break` dans la boucle de l'appelant arrête vraiment le parcours. Avant Go 1.23, j'aurais renvoyé un slice (allocation de tous les éléments) ou utilisé un channel (coûteux et risque de fuite si l'appelant n'allait pas jusqu'au bout)."

**Q : "Tu reprends un projet en Go 1.19. Comment le mets-tu à jour ?"**
R : "Par étapes, en restant sur des versions supportées à chaque palier. Je monterais la directive `go` et la chaîne d'outils, je lancerais tous les tests avec `-race`, puis je regarderais les changements de comportement liés à la directive : le plus notable est la **sémantique des variables de boucle** de Go 1.22, qui corrige des bugs mais peut changer le comportement d'un code qui en dépendait (rare). Les réglages `GODEBUG` permettent de revenir temporairement à un ancien comportement si un test casse.
Ensuite `go fix ./...` pour appliquer les modernizers, la mise à jour des dépendances et de golangci-lint, et `govulncheck` pour les vulnérabilités. Les gains attendus : performances (Swiss Tables, Green Tea, PGO), GOMAXPROCS correct en conteneur, et des API plus simples (`slices`, `slog`, itérateurs)."

═══════════════════════════════════════════
# 13. BIBLIOTHÈQUE STANDARD ESSENTIELLE
═══════════════════════════════════════════

### Vue d'ensemble

La bibliothèque standard de Go est l'un de ses plus grands atouts : serveur et client HTTP de qualité production, JSON, cryptographie, compression, templates, tests, profilage, tout est inclus et maintenu par l'équipe Go avec la même promesse de compatibilité que le langage. Une culture forte de l'écosystème Go est de **commencer par la bibliothèque standard** et de n'ajouter des dépendances que quand elles apportent un vrai gain.

Ce chapitre couvre les packages qu'on utilise tous les jours et leurs pièges ; HTTP, bases de données, tests et logs ont leurs propres chapitres.

### Cours

**fmt, les verbes à connaître** : `%v` (valeur par défaut), `%+v` (struct avec noms de champs), `%#v` (syntaxe Go), `%T` (type), `%d`, `%x`, `%o`, `%b`, `%f`/`%.2f`, `%e`, `%g`, `%s`, `%q` (chaîne entre guillemets échappée), `%p` (pointeur), `%t` (booléen), `%w` (dans `Errorf` uniquement), `%%`. Largeur et alignement : `%5d`, `%-10s`, `%05d`. Un type qui implémente `String() string` (`fmt.Stringer`) contrôle son affichage avec `%v` et `%s` ; `GoString()` pour `%#v`, `Format` pour un contrôle total. Piège : une méthode `String()` qui fait `fmt.Sprintf("%v", t)` sur elle-même provoque une récursion infinie. `fmt.Sprint`, `Sprintf`, `Fprintf(w, ...)` (vers un `io.Writer`), `Errorf`, `Sscanf`. `go vet` vérifie la cohérence des verbes et arguments.

**strings** : `Contains`, `ContainsAny`, `HasPrefix`, `HasSuffix`, `Index`, `Split`, `SplitN`, `Fields` (découpe sur les espaces), `Join`, `Replace`/`ReplaceAll`, `ToLower`/`ToUpper`, `TrimSpace`, `Trim`, `TrimPrefix`, `TrimSuffix`, `Cut` (1.18, découpe autour du premier séparateur : `avant, apres, ok := strings.Cut(s, "=")`), `CutPrefix`/`CutSuffix` (1.20), `CutLast` (1.27), `EqualFold` (comparaison insensible à la casse), `Repeat`, `Count`, `Map`, `Title` (obsolète, utiliser `golang.org/x/text/cases`), `NewReader`, `NewReplacer` (remplacements multiples efficaces), `Builder`. Itérateurs `Lines`, `SplitSeq`, `FieldsSeq` (1.24).

**strconv** : `Atoi`, `Itoa`, `ParseInt(s, base, bits)`, `ParseFloat`, `ParseBool`, `FormatInt`, `FormatFloat`, `Quote`/`Unquote`, `AppendInt` (sans allocation). Les erreurs sont des `*strconv.NumError` (avec `ErrSyntax`, `ErrRange`).

**time** :
- `time.Time` : un instant, avec une localisation ; `time.Now()`, `t.Add(d)`, `t.Sub(u)` (renvoie une `Duration`), `t.Before/After/Equal`, `t.Truncate`, `t.UTC()`, `t.In(loc)`, `time.LoadLocation("Europe/Paris")`.
- **Comparer des instants avec `Equal`, pas `==`** : `==` compare aussi la localisation et la lecture d'horloge monotone.
- `time.Duration` : un `int64` en nanosecondes ; `2 * time.Second`, `d.Seconds()`, `time.ParseDuration("1h30m")`. Piège : `time.Sleep(5)` dort 5 nanosecondes.
- **Format par l'exemple** : la date de référence est `Mon Jan 2 15:04:05 MST 2006` (1 2 3 4 5 6 7). `t.Format("2006-01-02 15:04:05")`, `time.RFC3339`, `time.DateOnly` (1.20), `time.Parse(layout, s)`. Piège classique : écrire `"YYYY-MM-DD"` ne fonctionne pas.
- **Horloge monotone** : `time.Now()` contient une lecture monotone utilisée par `Sub` et `Since`, ce qui rend les mesures de durée insensibles aux ajustements de l'horloge système.
- `time.Timer`, `time.Ticker` (à arrêter avec `Stop`), `time.After`, `time.AfterFunc`.
- Testabilité : injecter une fonction `now func() time.Time` ou une interface d'horloge ; `testing/synctest` (1.25) offre une horloge virtuelle pour tester des timers sans attendre.

**io et bufio** :
- `io.Reader`, `io.Writer`, `io.Closer`, `io.ReaderAt`, `io.Seeker` ; utilitaires `io.Copy` (copie en flux, sans tout charger), `io.CopyN`, `io.ReadAll` (charge tout : attention aux gros contenus et aux entrées non bornées ; environ deux fois plus rapide depuis Go 1.26), `io.ReadFull`, `io.LimitReader` (borner une entrée non fiable), `io.TeeReader`, `io.MultiReader`, `io.MultiWriter`, `io.Pipe`, `io.Discard`, `io.NopCloser`.
- `io.EOF` est une erreur **attendue** qui signale la fin ; un `Read` peut renvoyer `n > 0` **et** `io.EOF` en même temps : traiter les octets lus avant l'erreur.
- `bufio.Reader` (`ReadString`, `ReadLine`), `bufio.Writer` (**appeler `Flush`**), `bufio.Scanner` (lignes, mots ; limite de 64 Ko par jeton, réglable avec `Buffer`).

**os** : `os.Open`, `os.Create`, `os.OpenFile(path, flags, perm)`, `os.ReadFile`/`WriteFile`, `os.MkdirAll`, `os.Remove`/`RemoveAll`, `os.Stat` + `errors.Is(err, fs.ErrNotExist)`, `os.Getenv` / `os.LookupEnv` (distinguer absent et vide), `os.Args`, `os.Exit` (**n'exécute pas les defer**). **`os.Root`** (Go 1.24) : ouvrir des fichiers en restant confiné sous un répertoire, protection native contre le *path traversal*. `os.CopyFS` (1.23). `path/filepath` pour les chemins du système (`Join`, `Abs`, `WalkDir`, `Clean`), `path` pour les chemins à barres obliques (URL). `io/fs` (1.16) : abstraction de systèmes de fichiers (`fs.FS`), utilisée par `embed` et `http.FS`.

**embed** (1.16) : inclure des fichiers dans le binaire.
```go
//go:embed templates/* static
var contenu embed.FS
```

**encoding/json** :
- `json.Marshal(v)`, `json.Unmarshal(data, &v)` ; en flux : `json.NewEncoder(w).Encode(v)`, `json.NewDecoder(r).Decode(&v)`.
- Seuls les champs **exportés** sont sérialisés ; tags `json:"nom,omitempty"`, `omitzero` (1.24), `-`, `string`.
- `Unmarshal` dans une `interface{}` produit `map[string]any`, `[]any`, `float64` pour **tous** les nombres (perte de précision au-delà de 2⁵³) ; `Decoder.UseNumber()` garde `json.Number`.
- `DisallowUnknownFields()` pour rejeter les champs inconnus (validation stricte d'une API).
- Interfaces `json.Marshaler`/`Unmarshaler` pour un format personnalisé ; `json.RawMessage` pour différer le décodage.
- Pas d'erreur sur les champs manquants : ils restent à leur valeur zéro ; distinguer absent et zéro avec un pointeur.
- **encoding/json/v2** (stable en 1.27) : API plus cohérente (`Marshal`, `MarshalWrite`, `Unmarshal`, `UnmarshalRead` avec options), défauts plus stricts (UTF-8 invalide et noms dupliqués rejetés), décodage nettement plus rapide ; `encoding/json` (v1) est désormais implémenté au-dessus de la v2 en conservant son comportement.
- Alternatives : `easyjson`, `sonic`, `goccy/go-json` pour des besoins de performance extrêmes.

**Autres encodages** : `encoding/xml`, `encoding/csv`, `encoding/base64`, `encoding/hex`, `encoding/binary`, `encoding/gob` (format binaire propre à Go).

**sort et slices** : `slices.Sort`, `slices.SortFunc(s, func(a, b T) int)`, `slices.SortStableFunc`, `slices.BinarySearch` ; `cmp.Compare`, `cmp.Or` (1.22, premier argument non nul, pratique pour les tris multicritères : `cmp.Or(cmp.Compare(a.Nom, b.Nom), cmp.Compare(a.Age, b.Age))`). L'ancien `sort.Slice` reste valide mais moins efficace (réflexion).

**regexp** : syntaxe RE2, **temps linéaire garanti** (pas de backtracking, donc pas de ReDoS ; en contrepartie, pas de références arrière ni de lookaround). `regexp.MustCompile` en variable de package, `MatchString`, `FindStringSubmatch`, `ReplaceAllString`. Une regex compilée est sûre en concurrence.

**math et nombres** : `math.MaxInt64`, `math.Inf`, `math.NaN`, `math.Floor`, `math.Round`, `math/big` (entiers et rationnels de précision arbitraire), `math/bits`, `math/rand/v2` (API moderne, générateurs ChaCha8/PCG, graine aléatoire automatique), `crypto/rand` pour tout ce qui est sécuritaire (`crypto/rand.Text()` en 1.24 pour un jeton).

**net/url, net/netip** : `url.Parse`, `url.Values` (`Encode`, construction sûre de query strings), `url.QueryEscape`, `url.PathEscape`. `net/netip` (1.18) : types d'adresses IP comparables et sans allocation (`netip.Addr`, `netip.Prefix`).

**flag** : parsing des arguments en ligne de commande (`flag.String`, `flag.Int`, `flag.Duration`, `flag.Parse`, `flag.NewFlagSet` pour des sous-commandes) ; pour des CLI riches, `cobra` ou `urfave/cli`.

**text/template et html/template** : templates avec données, `html/template` **échappe automatiquement** selon le contexte (HTML, attribut, JavaScript, URL), ce qui protège du XSS ; ne jamais générer du HTML avec `text/template`.

**log et log/slog** : `log` (historique, non structuré, `log.Fatal` appelle `os.Exit(1)`), `log/slog` (structuré, voir le chapitre Observabilité).

**Autres packages utiles** : `unicode/utf8`, `bytes`, `container/heap`, `container/list`, `container/ring`, `hash/crc32`, `hash/fnv`, `hash/maphash`, `crypto/*` (voir Sécurité), `compress/gzip`, `archive/zip`, `os/exec` (lancer des commandes, avec contexte : `exec.CommandContext`), `runtime/debug` (`ReadBuildInfo` pour la version du binaire, `SetMemoryLimit`), `expvar`, `testing/*`, `net/http/httptest`, `uuid` (1.27).

### En situation

- **"La date formatée affiche des chiffres absurdes"** → format par l'exemple avec 2006-01-02, pas YYYY-MM-DD.
- **"time.Sleep(5) ne dort pas"** → 5 nanosecondes, pas secondes.
- **"Deux instants égaux ne sont pas =="** → utiliser Equal.
- **"Les identifiants numériques perdent des chiffres après JSON"** → décodés en float64 → UseNumber ou chaîne.
- **"Le fichier écrit est vide"** → bufio.Writer sans Flush.
- **"Le programme se termine sans exécuter les defer"** → os.Exit ou log.Fatal.
- **"Un champ JSON n'apparaît pas"** → champ non exporté.

### Questions techniques

**Q : %v vs %+v vs %#v ?** R : Valeur par défaut / struct avec noms de champs / syntaxe Go.

**Q : Comment contrôler l'affichage d'un type avec fmt ?** R : En implémentant String() string (fmt.Stringer).

**Q : Pourquoi une méthode String peut-elle provoquer une récursion infinie ?** R : Si elle formate le receveur avec %v, qui rappelle String.

**Q : À quoi sert strings.Cut ?** R : Découper une chaîne autour de la première occurrence d'un séparateur, en renvoyant avant, après et un booléen.

**Q : Comment formater une date en Go ?** R : Par l'exemple, avec la date de référence `2006-01-02 15:04:05`.

**Q : Comment comparer deux time.Time ?** R : Avec Equal, Before, After, pas avec ==.

**Q : C'est quoi l'horloge monotone ?** R : Une lecture incluse dans time.Now, utilisée par Sub et Since, insensible aux ajustements de l'horloge système.

**Q : Que vaut time.Duration(5) ?** R : 5 nanosecondes.

**Q : Que signifie io.EOF ?** R : La fin normale des données ; ce n'est pas une erreur à remonter.

**Q : Un Read peut-il renvoyer des données et une erreur ?** R : Oui ; traiter les n octets lus avant l'erreur.

**Q : Pourquoi io.ReadAll peut-il être dangereux ?** R : Il charge tout en mémoire ; borner les entrées non fiables avec io.LimitReader ou http.MaxBytesReader.

**Q : Pourquoi appeler Flush sur un bufio.Writer ?** R : Sinon les données restées dans le buffer ne sont jamais écrites.

**Q : Quelle est la limite par défaut de bufio.Scanner ?** R : 64 Ko par jeton, réglable avec Buffer.

**Q : os.Exit exécute-t-il les defer ?** R : Non.

**Q : os.Getenv vs os.LookupEnv ?** R : LookupEnv distingue une variable absente d'une variable vide.

**Q : À quoi sert os.Root ?** R : Confiner l'accès aux fichiers sous un répertoire (Go 1.24), contre le path traversal.

**Q : À quoi sert embed ?** R : Inclure des fichiers dans le binaire à la compilation.

**Q : Quels champs encoding/json sérialise-t-il ?** R : Seulement les champs exportés.

**Q : En quoi sont décodés les nombres dans une interface{} ?** R : En float64, d'où une perte de précision possible ; UseNumber les garde en json.Number.

**Q : Comment rejeter les champs JSON inconnus ?** R : Decoder.DisallowUnknownFields().

**Q : Qu'apporte encoding/json/v2 ?** R : Une API plus cohérente, des défauts plus stricts et un décodage plus rapide (stable en Go 1.27).

**Q : Pourquoi les regex Go n'ont-elles pas de références arrière ?** R : Le moteur RE2 garantit un temps linéaire, sans backtracking.

**Q : math/rand vs crypto/rand ?** R : Aléatoire statistique vs aléatoire cryptographiquement sûr.

**Q : text/template vs html/template ?** R : html/template échappe automatiquement selon le contexte, contre le XSS.

**Q : Que fait log.Fatal ?** R : Il logge puis appelle os.Exit(1), sans exécuter les defer.

**Q : À quoi sert cmp.Or ?** R : Renvoyer le premier argument non nul, pratique pour enchaîner des critères de tri.

### Questions de situation

**Q : "Tu dois télécharger un fichier de plusieurs Go et le sauvegarder sur disque."**
R : "Jamais de `io.ReadAll` : je copierais en flux, du corps de la réponse vers le fichier, avec `io.Copy`, qui utilise un buffer de taille fixe :
```go
req, _ := http.NewRequestWithContext(ctx, http.MethodGet, url, nil)
resp, err := client.Do(req)
if err != nil { return err }
defer resp.Body.Close()
if resp.StatusCode != http.StatusOK { return fmt.Errorf("statut %d", resp.StatusCode) }

tmp, err := os.CreateTemp(dir, "dl-*")
if err != nil { return err }
defer os.Remove(tmp.Name())        // nettoyage si on échoue avant le rename
if _, err := io.Copy(tmp, resp.Body); err != nil { tmp.Close(); return err }
if err := tmp.Close(); err != nil { return err }
return os.Rename(tmp.Name(), destination)   // remplacement atomique
```
"La mémoire reste constante quelle que soit la taille. J'écris dans un fichier temporaire du même répertoire puis je le renomme, pour qu'un lecteur ne voie jamais un fichier partiel ; je vérifie l'erreur de `Close`. J'ajouterais une vérification de somme de contrôle si la source en fournit une, et une limite de taille si la source n'est pas fiable."

**Q : "Un client envoie des montants en JSON et certains arrivent arrondis."**
R : "Si le JSON est décodé dans une `map[string]any` ou un `float64`, les nombres passent par un flottant 64 bits : les grands entiers perdent des chiffres et les décimaux comme 0.1 ne sont pas exacts. Je décoderais dans une struct avec des types adaptés : un `int64` en centimes, ou un type décimal qui implémente `UnmarshalJSON`, ou `json.Number` avec `UseNumber()`. Le plus robuste pour une API financière est de transmettre les montants en centimes entiers, ou en chaîne décimale explicite."

═══════════════════════════════════════════
# 14. QUESTIONS PIÈGES : QU'AFFICHE CE CODE ?
═══════════════════════════════════════════

### Vue d'ensemble

Les entretiens Go contiennent presque toujours quelques extraits de code dont il faut prédire le comportement. Les pièges sont peu nombreux mais très précis : partage de mémoire des slices, interface nil, variables de boucle capturées (avant Go 1.22), `defer` et évaluation de ses arguments, channels fermés, maps nil, octets contre runes.

Méthode : identifier le **type exact** de chaque expression, se demander si une valeur est **copiée** ou **partagée**, et vérifier la **version de Go** (la sémantique des boucles a changé en 1.22). Toutes les sorties ci-dessous ont été vérifiées en compilant et exécutant le code (Go 1.22, et Go 1.21 pour l'ancien comportement des boucles).

### Cours

**Les familles de pièges** :
- **Slices** : partage du tableau sous-jacent, `append` qui écrase ou réalloue selon la capacité, expression à trois indices.
- **Interfaces** : une interface contenant un pointeur nil n'est pas nil.
- **Boucles** : capture de la variable de boucle (comportement différent avant et après Go 1.22).
- **defer** : ordre LIFO, arguments évalués immédiatement, modification des retours nommés.
- **Channels** : réception sur un channel fermé, double fermeture, select avec default.
- **Maps** : lecture d'une map nil (valeur zéro) vs écriture (panic).
- **Chaînes** : len en octets, indexation en octets, range en runes.
- **Constantes** : calculs en précision arbitraire à la compilation.
- **Embedding** : pas de polymorphisme.

### En situation

- **Test chronométré** → repérer d'abord la catégorie de piège.
- **Question "et si on passe à Go 1.21 ?"** → penser à la sémantique des variables de boucle.
- **En revue de code** → ces mêmes pièges sont les bugs à chercher.

### Questions techniques

**Q :**
```go
type MonErr struct{}
func (*MonErr) Error() string { return "x" }
func trouver() error { var e *MonErr; return e }

err := trouver()
fmt.Println(err == nil)
```
R : `false` : l'interface contient le type `*MonErr` avec une valeur nil, elle n'est donc pas nil.

**Q :**
```go
s := []int{1, 2, 3}
t := s[:2]
t = append(t, 99)
fmt.Println(s, t)
```
R : `[1 2 99] [1 2 99]` : `t` a une capacité de 3, `append` écrit dans le tableau partagé et écrase `s[2]`.

**Q :**
```go
a := make([]int, 3, 10)
b := append(a, 4)
c := append(a, 5)
fmt.Println(b[3], c[3])
```
R : `5 5` : `b` et `c` partagent le même tableau (capacité suffisante), le second `append` écrase la valeur écrite par le premier.

**Q :**
```go
sx := []int{1, 2, 3, 4, 5}
sy := sx[1:3]
fmt.Println(len(sy), cap(sy))
sz := sx[1:3:3]
sz = append(sz, 100)
fmt.Println(sx, sz)
```
R : `2 4`, puis `[1 2 3 4 5] [2 3 100]` : la capacité de `sz` est limitée à 2, donc `append` réalloue et `sx` n'est pas modifié.

**Q :**
```go
s := make([]int, 3)
s = append(s, 1)
fmt.Println(s, len(s))
```
R : `[0 0 0 1] 4` : `make([]int, 3)` crée trois zéros, `append` ajoute après (piège fréquent : on voulait `make([]int, 0, 3)`).

**Q :**
```go
var sl []int
fmt.Println(sl == nil, len(sl), cap(sl))
sl = append(sl, 1)
fmt.Println(sl)
```
R : `true 0 0` puis `[1]` : on peut faire `append` sur un slice nil.

**Q :**
```go
var m map[string]int
fmt.Println(m["x"], len(m))
m["x"] = 1
```
R : `0 0`, puis **panic** (`assignment to entry in nil map`) : lire une map nil est permis, y écrire non.

**Q :**
```go
m := map[string][]int{}
m["a"] = append(m["a"], 1)
fmt.Println(m)
```
R : `map[a:[1]]` : la valeur zéro d'une clé absente est un slice nil, accepté par `append`.

**Q : Peut-on écrire `m["a"].n = 5` avec `m := map[string]T{...}` ? Et avec `map[string]*T` ?** R : Non pour une map de valeurs (élément non adressable, erreur de compilation) ; oui pour une map de pointeurs.

**Q :**
```go
fmt.Println(len("héllo"), len([]rune("héllo")))
for i, r := range "hé!" {
    fmt.Print(i, ":", string(r), " ")
}
```
R : `6 5`, puis `0:h 1:é 3:! ` : len compte les octets ; range parcourt les runes et l'index saute de 1 à 3 car "é" occupe 2 octets.

**Q : `fmt.Println("abc"[1], string("abc"[1]))`** R : `98 b` : l'indexation renvoie un octet (98 est le code de 'b').

**Q : `str := "hello"; bs := []byte(str); bs[0] = 'H'; fmt.Println(str, string(bs))`** R : `hello Hello` : la conversion copie les octets, la chaîne est immuable.

**Q :**
```go
type Compte struct{ s int }
func (c Compte) DeposerV(n int)  { c.s += n }
func (c *Compte) DeposerP(n int) { c.s += n }

cc := Compte{}
cc.DeposerV(10); fmt.Println(cc.s)
cc.DeposerP(10); fmt.Println(cc.s)
```
R : `0` puis `10` : le receveur valeur modifie une copie ; le receveur pointeur modifie l'original (Go prend automatiquement `&cc`).

**Q :**
```go
x := 1
defer fmt.Println("defer x =", x)
x = 2
fmt.Println("x =", x)
```
R : `x = 2` puis `defer x = 1` : les arguments du defer sont évalués au moment de sa déclaration.

**Q :**
```go
func nomme() (n int) {
    defer func() { n *= 2 }()
    return 3
}
```
R : `6` : `return 3` affecte `n = 3`, puis le defer modifie le retour nommé.

**Q : `for i := 0; i < 3; i++ { defer fmt.Print(i, " ") }`** R : `2 1 0` : ordre LIFO.

**Q :**
```go
var funcs []func()
for i := 0; i < 3; i++ {
    funcs = append(funcs, func() { fmt.Print(i, " ") })
}
for _, f := range funcs { f() }
```
R : `0 1 2` avec un module en Go 1.22 ou plus (une variable par itération) ; `3 3 3` en Go 1.21 et avant (une seule variable partagée, qui vaut 3 à la fin).

**Q :**
```go
type Base struct{}
func (Base) Nom() string      { return "base" }
func (b Base) Salut() string  { return "je suis " + b.Nom() }
type Derive struct{ Base }
func (Derive) Nom() string    { return "derive" }

fmt.Println(Derive{}.Salut())
```
R : `je suis base` : l'embedding n'est pas de l'héritage ; `Salut` est appelée sur le `Base` embarqué, qui appelle `Base.Nom`.

**Q :**
```go
ch := make(chan int, 2)
ch <- 1; ch <- 2
close(ch)
for v := range ch { fmt.Print(v, " ") }
v, ok := <-ch
fmt.Println(v, ok)
```
R : `1 2 ` puis `0 false` : les valeurs restantes sont lues après la fermeture, puis la réception renvoie la valeur zéro et `ok = false`.

**Q : Que fait `close(ch)` sur un channel déjà fermé ?** R : Panic : `close of closed channel`. Envoyer sur un channel fermé panique aussi.

**Q :**
```go
ch := make(chan int)
select {
case v := <-ch:
    fmt.Println(v)
default:
    fmt.Println("default")
}
```
R : `default` : personne n'envoie, le select non bloquant prend le cas default.

**Q : Que se passe-t-il avec `ch := make(chan int); ch <- 1` dans main sans autre goroutine ?** R : `fatal error: all goroutines are asleep - deadlock!` : l'envoi sur un channel non bufferisé attend un récepteur qui n'existe pas.

**Q : `fmt.Println(0.1+0.2 == 0.3)`** R : `true` ! Ce sont des **constantes non typées**, calculées en précision arbitraire à la compilation.

**Q : `a, b := 0.1, 0.2; fmt.Println(a+b == 0.3, a+b)`** R : `false 0.30000000000000004` : ce sont maintenant des variables `float64`.

**Q : `fmt.Println(7/2, -7/2, 7%3, -7%3)`** R : `3 -3 1 -1` : division entière tronquée vers zéro, le signe du modulo suit le dividende.

**Q : `var i8 int8 = 127; i8++; fmt.Println(i8)`** R : `-128` : débordement silencieux.

**Q : `var x uint8 = 0; x--; fmt.Println(x)`** R : `255`.

**Q : `const big = 1 << 62; fmt.Println(big / (1 << 60))`** R : `4`.

**Q :**
```go
arr := [3]int{1, 2, 3}
arr2 := arr
arr2[0] = 9
fmt.Println(arr[0])
```
R : `1` : un tableau est une valeur, l'affectation le copie (avec un slice, ce serait 9).

**Q : `type P struct{ X int }; p1 := P{1}; p2 := p1; p2.X = 2; fmt.Println(p1.X, p1 == P{1})`** R : `1 true` : la struct est copiée et comparable.

**Q :**
```go
e1 := errors.New("a")
e2 := fmt.Errorf("ctx: %w", e1)
e3 := fmt.Errorf("ctx: %v", e1)
fmt.Println(errors.Is(e2, e1), errors.Is(e3, e1), e2 == e1)
```
R : `true false false` : seul `%w` conserve l'erreur dans la chaîne.

**Q :**
```go
var iface interface{}
fmt.Println(iface == nil)
var pp *P
iface = pp
fmt.Println(iface == nil)
```
R : `true` puis `false`.

**Q :**
```go
var x interface{} = 3
switch y := x.(type) {
case int, int64:
    fmt.Printf("%T\n", y)
}
```
R : `int` : avec plusieurs types dans un `case`, `y` a le type de `x` (interface), mais `%T` affiche le type dynamique.

**Q : `fmt.Println(strings.Split("a,b,", ","), len(strings.Split("a,b,", ",")), len(strings.Split("", ",")))`** R : `[a b ] 3 1` : la chaîne vide finale est conservée, et découper "" renvoie un slice d'un élément vide.

**Q : `fmt.Println(strings.Fields("  a  b "))`** R : `[a b]` : Fields découpe sur les espaces et ignore les vides.

**Q : `fmt.Println(time.Duration(1500) * time.Millisecond)`** R : `1.5s`.

**Q : `fmt.Printf("%v %+v %T\n", P{1}, P{1}, P{1})`** R : `{1} {X:1} main.P`.

**Q : `fmt.Println(fmt.Sprint(nil), fmt.Sprintf("%d", 3.5))`** R : `<nil> %!d(float64=3.5)` : fmt ne panique pas sur un mauvais verbe, il l'indique dans la sortie (et `go vet` le signale avant).

**Q : `fmt.Println(string(rune(65)), fmt.Sprint(65))`** R : `A 65` : convertir un entier en string donne le caractère correspondant, pas le texte du nombre.

**Q : `fmt.Println(copy(make([]int, 2), []int{1, 2, 3}))`** R : `2` : copy copie le minimum des deux longueurs.

**Q :**
```go
switch 2 {
case 1:
    fmt.Println("un")
case 2:
    fmt.Println("deux")
    fallthrough
case 3:
    fmt.Println("trois")
case 4:
    fmt.Println("quatre")
}
```
R : `deux` puis `trois` : pas de fall-through par défaut, mais `fallthrough` force l'exécution du cas suivant (sans tester sa condition), et un seul.

**Q :**
```go
var wg sync.WaitGroup
var mu sync.Mutex
total := 0
for i := 1; i <= 100; i++ {
    wg.Add(1)
    go func() { defer wg.Done(); mu.Lock(); total += i; mu.Unlock() }()
}
wg.Wait()
fmt.Println(total)
```
R : `5050` en Go 1.22+ (chaque goroutine capture sa propre variable `i`). En Go 1.21, le résultat serait imprévisible (les goroutines liraient une variable partagée qui change) et le détecteur de courses signalerait une data race.

**Q : Que se passe-t-il si une goroutine panique sans recover pendant que main attend ?** R : Tout le programme s'arrête, quel que soit le recover présent dans main.

**Q : Ce code compile-t-il ? `x := 1` puis plus rien n'utilise x.** R : Non : `declared and not used`.

### Questions de situation

**Q : "En entretien, on te montre un extrait avec des slices et tu hésites sur le résultat."**
R : "Je dessinerais le tableau sous-jacent et les en-têtes (pointeur, longueur, capacité) de chaque slice, puis j'appliquerais la règle d'`append` : si la capacité suffit, il écrit en place dans le tableau partagé ; sinon il alloue un nouveau tableau. Ce schéma rend presque tous ces pièges évidents.
En revue de code réelle, je chercherais justement ces situations : un `append` sur un sous-slice d'un slice qui continue d'être utilisé, et je proposerais `slices.Clone` ou l'expression à trois indices pour rendre l'intention explicite."

═══════════════════════════════════════════
# 15. GO IDIOMATIQUE : STYLE, NOMMAGE & CONCEPTION D'API
═══════════════════════════════════════════

### Vue d'ensemble

Écrire du Go qui compile est facile ; écrire du Go **idiomatique** demande de désapprendre des réflexes venus d'autres langages. Les références sont *Effective Go*, les *Go Code Review Comments* (wiki officiel), le *Google Go Style Guide* et les *Go Proverbs* de Rob Pike. Leur message commun : **clarté avant astuce**, **simplicité avant abstraction**, et une API qui se lit naturellement depuis le code appelant.

Les recruteurs Go y sont très attentifs, souvent plus qu'à la connaissance de frameworks : comment tu nommes, comment tu découpes en packages, comment tu conçois un constructeur et des options, comment tu gères les erreurs et la concurrence dans une API.

### Cours

**Go Proverbs (sélection à connaître)** :
- *Don't communicate by sharing memory, share memory by communicating.*
- *Concurrency is not parallelism.*
- *The bigger the interface, the weaker the abstraction.*
- *Make the zero value useful.*
- *interface{} says nothing.*
- *Gofmt's style is no one's favorite, yet gofmt is everyone's favorite.*
- *A little copying is better than a little dependency.*
- *Clear is better than clever.*
- *Errors are values.*
- *Don't just check errors, handle them gracefully.*
- *Don't panic.*
- *Reflection is never clear.*
- *Cgo is not Go.*

**Nommage** :
- **Court quand la portée est courte**, explicite quand elle est longue : `i`, `r`, `buf` dans une petite fonction ; `requetesEnAttente` pour une variable de package.
- **Pas de bégaiement** : dans le package `user`, le type s'appelle `user.Service` et pas `user.UserService` ; le constructeur `user.New`. On lit toujours le nom avec son package.
- **Packages** : un mot, minuscules, sans underscore ni majuscule, au singulier, qui décrit ce qu'il **fournit** (`http`, `json`, `auth`) ; éviter `util`, `common`, `helpers`, `models`, `base`, noms qui n'expriment rien et deviennent des fourre-tout.
- **Acronymes** en casse uniforme : `URL`, `HTTP`, `ID` (`userID`, `ServeHTTP`, `parseURL`).
- **Getters** sans `Get` : `Nom()`, et `SetNom()` pour le setter.
- **Interfaces** d'une méthode : suffixe *-er* (`Reader`, `Validator`).
- **Receveurs** : un nom court et cohérent (une ou deux lettres de l'initiale du type : `func (s *Server)`), jamais `this` ni `self`.
- **Erreurs** : variables `ErrXxx`, types `XxxError`.

**Constructeurs** : une fonction `NewXxx` (ou `New` si le package exporte un type principal) qui renvoie le type concret (souvent un pointeur) et éventuellement une erreur. Si la valeur zéro est utilisable, pas besoin de constructeur.

**Paramètres optionnels : le pattern functional options** (popularisé par Dave Cheney) :
```go
type Server struct {
    addr    string
    timeout time.Duration
    logger  *slog.Logger
}

type Option func(*Server)

func WithTimeout(d time.Duration) Option { return func(s *Server) { s.timeout = d } }
func WithLogger(l *slog.Logger) Option   { return func(s *Server) { s.logger = l } }

func NewServer(addr string, opts ...Option) *Server {
    s := &Server{addr: addr, timeout: 30 * time.Second, logger: slog.Default()}
    for _, opt := range opts {
        opt(s)
    }
    return s
}

srv := NewServer(":8080", WithTimeout(10*time.Second))
```
Avantages : valeurs par défaut sensées, API extensible sans casser les appelants, appels lisibles. Alternative plus simple et tout aussi idiomatique : une **struct de configuration** (`NewServer(Config{Addr: ":8080"})`), préférable quand il y a beaucoup d'options ou qu'elles viennent d'un fichier de configuration.

**Conception d'API** :
- **Accept interfaces, return structs.**
- Exposer le **minimum** : tout ce qui est exporté devient un engagement de compatibilité.
- Préférer des fonctions **synchrones** ; laisser l'appelant ajouter la concurrence.
- `ctx context.Context` en premier paramètre de toute opération potentiellement longue.
- Les erreurs en dernier résultat ; pas de panic à travers l'API.
- Éviter les paramètres booléens obscurs (`Fetch(true, false)`) : options nommées ou deux fonctions.
- Documenter chaque identifiant exporté par un commentaire qui commence par son nom (`// Parse analyse...`), lu par `go doc` et pkg.go.dev ; les exemples testables (`func ExampleParse()`) font partie de la documentation et sont exécutés par `go test`.
- Les fonctions `Must` uniquement pour l'initialisation.
- Les types sûrs en concurrence le documentent ; sinon on suppose qu'ils ne le sont pas.

**Organisation du code** :
- Un package regroupe ce qui **change ensemble** et forme un concept cohérent, pas une couche technique ("models", "controllers") à la Java.
- Commencer **plat** (un seul package, quelques fichiers), découper quand le besoin apparaît.
- Les **cycles d'import sont interdits** par le compilateur : c'est une contrainte qui force un graphe de dépendances propre (souvent résolue en déplaçant une interface vers le consommateur ou en extrayant un petit package).
- `internal/` : les packages sous un répertoire `internal` ne sont importables que par le code de l'arborescence parente, le moyen natif de cacher des détails d'implémentation.

**Lisibilité du flux** :
- **Chemin heureux aligné à gauche** : traiter les erreurs et cas particuliers tôt avec un `return`, éviter les `else` après un `return`.
- Fonctions courtes, mais une fonction de 50 lignes linéaire et claire vaut mieux que dix fonctions de 5 lignes qui obligent à sauter partout.
- Pas d'abstraction prématurée : dupliquer trois lignes vaut mieux qu'une dépendance ou une abstraction douteuse.

**SOLID en Go** :
- **S** : petits packages et types à responsabilité unique.
- **O** : extension par composition et interfaces (un nouveau type qui satisfait l'interface), sans modifier l'existant.
- **L** : toute implémentation d'une interface doit respecter son contrat documenté (un `io.Reader` qui renvoie `n > len(p)` viole le contrat).
- **I** : naturellement respecté par les petites interfaces définies côté consommateur.
- **D** : dépendre d'interfaces déclarées par le consommateur ; injection par constructeur, sans framework.

**Réflexion** : `reflect` est puissant (c'est ce qu'utilisent `encoding/json` et `fmt`) mais lent, non vérifié à la compilation et difficile à lire. Les generics et la génération de code (`go generate`) le remplacent dans beaucoup de cas.

**Génération de code** : `//go:generate stringer -type=Statut` puis `go generate ./...` ; outils courants : `stringer`, `mockgen`/`mockery`, `sqlc`, `protoc-gen-go`, `oapi-codegen`, `wire`. Le code généré est versionné et marqué par un commentaire `// Code generated ... DO NOT EDIT.`

### En situation

- **"Mon package s'appelle utils"** → découper selon ce qu'il fournit.
- **"Le constructeur a 9 paramètres"** → struct de config ou functional options.
- **"Un `user.UserService`"** → bégaiement → `user.Service`.
- **"Import cycle not allowed"** → l'interface au mauvais endroit, ou un package à extraire.
- **"Le code a quatre niveaux d'indentation"** → retours anticipés.
- **"On veut cacher un package d'implémentation"** → `internal/`.

### Questions techniques

**Q : Où trouver les conventions de style Go ?** R : Effective Go, Go Code Review Comments, le Google Go Style Guide, et gofmt.

**Q : Que signifie "make the zero value useful" ?** R : Concevoir les types pour qu'ils soient utilisables sans initialisation explicite.

**Q : Que signifie "a little copying is better than a little dependency" ?** R : Dupliquer quelques lignes plutôt qu'ajouter une dépendance pour un besoin minime.

**Q : C'est quoi le bégaiement dans le nommage ?** R : Répéter le nom du package dans l'identifiant (user.UserService au lieu de user.Service).

**Q : Pourquoi éviter un package utils ?** R : Le nom ne dit rien de ce qu'il fournit et il devient un fourre-tout couplé à tout.

**Q : Comment nommer un receveur ?** R : Une ou deux lettres liées au type, cohérentes ; jamais this ou self.

**Q : Comment écrire les acronymes ?** R : En casse uniforme : userID, ServeHTTP, parseURL.

**Q : C'est quoi le pattern functional options ?** R : Des fonctions `Option func(*T)` passées en variadique au constructeur pour configurer des paramètres optionnels.

**Q : Functional options vs struct de configuration ?** R : Options pour des API publiques extensibles avec peu d'options ; struct quand il y a beaucoup de champs ou une configuration chargée depuis un fichier.

**Q : Que signifie "accept interfaces, return structs" ?** R : Accepter des interfaces minimales, renvoyer des types concrets.

**Q : Comment documenter un identifiant exporté ?** R : Un commentaire juste au-dessus qui commence par son nom.

**Q : C'est quoi une fonction Example ?** R : Une fonction ExampleXxx dans un fichier de test, affichée dans la documentation et vérifiée par go test via son commentaire Output.

**Q : Pourquoi les cycles d'import sont-ils interdits ?** R : Pour garantir un graphe de dépendances acyclique, une compilation rapide et un découpage propre.

**Q : À quoi sert le répertoire internal ?** R : Ses packages ne sont importables que depuis l'arborescence parente.

**Q : Pourquoi aligner le chemin heureux à gauche ?** R : Traiter les erreurs tôt et retourner rend le flux principal lisible sans imbrication.

**Q : Comment s'applique le DIP en Go ?** R : Le consommateur déclare l'interface, la dépendance est injectée par le constructeur, sans framework.

**Q : Pourquoi limiter la réflexion ?** R : Lente, non vérifiée à la compilation et peu lisible ; generics et génération de code la remplacent souvent.

**Q : À quoi sert go generate ?** R : À exécuter les commandes de génération déclarées par des directives //go:generate.

### Questions de situation

**Q : "Tu conçois le client Go d'une API interne qui sera utilisé par dix équipes. Comment conçois-tu son API ?"**
R : "Un constructeur `New(baseURL string, opts ...Option) (*Client, error)` avec des valeurs par défaut sûres (timeouts, retries limités, un `http.Client` partagé), des options pour personnaliser (`WithHTTPClient`, `WithRetry`, `WithLogger`).
Des méthodes synchrones, typées et nommées selon le métier (`GetOrder(ctx, id) (*Order, error)`), avec `ctx` en premier, des erreurs sentinelles ou typées documentées (`ErrNotFound`, `*APIError` avec le code), et pas d'interface exportée : les équipes définiront leur propre petite interface pour leurs mocks.
Le client est sûr en concurrence et le documente. Je publierais des exemples testables, une politique de versioning sémantique stricte, et je garderais la surface exportée minimale, car chaque identifiant exporté est un engagement de compatibilité pour dix équipes."

**Q : "Le compilateur refuse un cycle d'import entre `order` et `customer`."**
R : "C'est souvent le signe qu'une dépendance est déclarée au mauvais endroit. Si `order` a seulement besoin de lire quelques informations client, je déclarerais dans `order` une petite interface (`ClientLookup`) implémentée par `customer`, et c'est le `main` qui branche les deux : `order` ne dépend plus de `customer`.
Si les deux partagent un concept (un type `Address`), je l'extrairais dans un petit package commun nommé selon ce concept. Et si les deux packages sont si imbriqués qu'on n'arrive pas à les séparer, c'est peut-être qu'ils forment un seul module métier qui devrait être un seul package."

═══════════════════════════════════════════
# 16. DESIGN PATTERNS EN GO
═══════════════════════════════════════════

### Vue d'ensemble

Les patterns du Gang of Four ont été pensés pour des langages à classes et héritage. En Go, beaucoup deviennent triviaux (Strategy n'est qu'une fonction ou une interface d'une méthode), d'autres changent de forme (le Builder cède souvent la place aux functional options), et certains sont inutiles (pas d'Abstract Factory élaborée sans hiérarchies de classes). En revanche, Go a ses **propres patterns**, surtout autour de la concurrence (pipelines, worker pools, fan-out/fan-in) et des middlewares.

En entretien, on attend de savoir reconnaître un pattern dans du code Go idiomatique et de l'implémenter **simplement**, sans reproduire la lourdeur Java.

### Cours

**Créationnels** :
- **Constructeur / Factory** : une fonction `NewXxx`. Une factory qui choisit l'implémentation selon un paramètre : `func NewStore(kind string) (Store, error)`, l'un des rares cas où l'on retourne une interface.
- **Functional options** : la forme Go du Builder pour les paramètres optionnels (voir le chapitre Go idiomatique). Un vrai Builder chaîné (`q := NewQuery().Where(...).Limit(10)`) existe aussi (constructeurs de requêtes SQL comme squirrel), avec la difficulté de remonter les erreurs (on les accumule et on les renvoie à `Build()`).
- **Singleton** : une variable de package initialisée avec `sync.Once` ou `sync.OnceValue`. À limiter : un état global complique les tests ; préférer l'injection d'une instance créée dans `main`.
- **Object pool** : `sync.Pool` pour les objets temporaires ; un channel bufferisé ou un pool dédié pour des ressources coûteuses (les connexions de base sont déjà gérées par `database/sql`).

**Structurels** :
- **Adapter** : un type qui implémente l'interface attendue en enveloppant un autre ; l'exemple canonique est `http.HandlerFunc`, qui adapte une fonction ordinaire à l'interface `http.Handler` :
```go
type HandlerFunc func(ResponseWriter, *Request)
func (f HandlerFunc) ServeHTTP(w ResponseWriter, r *Request) { f(w, r) }
```
Le même idiome (un type fonction qui implémente une interface) se réutilise partout.
- **Decorator / Middleware** : une fonction qui prend une implémentation et renvoie une implémentation enrichie de la même interface.
```go
func Logging(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        debut := time.Now()
        next.ServeHTTP(w, r)
        slog.Info("requete", "path", r.URL.Path, "duree", time.Since(debut))
    })
}
handler := Logging(Auth(Recover(mux)))
```
Les décorateurs s'appliquent aussi aux `io.Reader` (`bufio.NewReader`, `gzip.NewReader`, `io.LimitReader`) et à toute interface (un `Notifier` avec retry autour d'un autre `Notifier`).
- **Proxy** : `httputil.ReverseProxy`, ou un type qui ajoute cache, contrôle d'accès ou métriques devant un autre.
- **Facade** : un package qui offre une API simple au-dessus de sous-systèmes complexes.
- **Composite** : des nœuds qui implémentent la même interface que leurs enfants (arbre de validateurs, `io.MultiWriter`).

**Comportementaux** :
- **Strategy** : une interface d'une méthode ou simplement un **type fonction** (`type Tarif func(Commande) Montant`) ; `slices.SortFunc` prend sa stratégie de comparaison en paramètre.
- **Observer** : une liste de callbacks ou de channels abonnés ; attention à la concurrence et à la désinscription.
- **Chain of responsibility** : la chaîne de middlewares HTTP.
- **Template method** : sans héritage, on passe les étapes variables comme fonctions ou via une interface (`sort.Interface` fournit Len, Less, Swap à un algorithme fixe).
- **Command** : une fonction ou une struct décrivant une action (file de tâches, commandes d'une CLI cobra).
- **Iterator** : `iter.Seq` depuis Go 1.23 ; avant, des méthodes `Next`/`Value` (`sql.Rows`, `bufio.Scanner`).
- **State** : des fonctions d'état qui renvoient la fonction d'état suivante (`type etat func(*lexer) etat`), technique présentée par Rob Pike pour les lexers.
- **Visitor** : rare ; un type switch sur un ensemble fermé de types suffit souvent (c'est ce que fait `go/ast` avec `ast.Inspect`).

**Patterns de concurrence propres à Go** (détaillés dans le chapitre Concurrence) : *generator*, *pipeline*, *fan-out / fan-in*, *worker pool*, *semaphore*, *errgroup*, *rate limiter* (`golang.org/x/time/rate`, un *token bucket*), *pub/sub* par channels, *singleflight*, *context cancellation*, *bounded parallelism*, *heartbeat*, *circuit breaker* (bibliothèques comme sony/gobreaker).

**Patterns d'erreur** : *sentinel errors*, *error types*, *opaque errors* et *behavior interfaces* ; *sticky error* (un type qui mémorise la première erreur et ignore les opérations suivantes, utilisé par `bufio.Writer` et pratique pour enchaîner des écritures) :
```go
type ew struct{ w io.Writer; err error }
func (e *ew) write(b []byte) {
    if e.err != nil { return }
    _, e.err = e.w.Write(b)
}
```

**Patterns de test** : tests table-driven, *test doubles* écrits à la main, *golden files*, *functional options* pour configurer des fixtures, `httptest.Server` (voir le chapitre Tests).

**Injection de dépendances** : manuelle, dans `main` (*composition root*) ; outils de génération (Google **Wire**, à la compilation) ou conteneurs à l'exécution (Uber **Fx**/dig) pour les grosses applications. La plupart des projets Go se contentent de l'injection manuelle, plus lisible.

**Anti-patterns en Go** : interfaces créées avant d'en avoir besoin, getters/setters systématiques, packages `utils`/`common`, panique comme contrôle de flux, goroutines lancées sans condition de fin, `interface{}` partout, état global mutable, `init()` avec effets de bord lourds, hiérarchies d'embedding imitant l'héritage, abus de la réflexion, channels utilisés comme verrous.

### En situation

- **"Ajouter du logging, des métriques et de l'authentification à tous les handlers"** → middlewares chaînés.
- **"Une fonction doit accepter un handler sous forme de fonction ou de struct"** → adapter `HandlerFunc`.
- **"Plusieurs algorithmes de tarification interchangeables"** → Strategy en type fonction ou interface d'une méthode.
- **"Une suite d'écritures avec une vérification d'erreur à chaque ligne"** → sticky error.
- **"Un singleton de connexion à la base partout dans le code"** → instance créée dans main et injectée.

### Questions techniques

**Q : Comment implémenter un Singleton en Go ?** R : Une variable de package initialisée par sync.Once ou sync.OnceValue ; mieux vaut souvent injecter une instance.

**Q : Quel pattern remplace souvent le Builder en Go ?** R : Les functional options.

**Q : Quel pattern illustre http.HandlerFunc ?** R : Adapter : il adapte une fonction à l'interface http.Handler.

**Q : Comment s'écrit un middleware HTTP ?** R : Une fonction qui prend un http.Handler et renvoie un http.Handler enrichi (Decorator).

**Q : Comment s'écrit une Strategy en Go ?** R : Un type fonction ou une interface d'une méthode, passés en paramètre.

**Q : Quel pattern illustre sort.Interface ?** R : Template method : l'algorithme est fixe, les étapes Len/Less/Swap sont fournies.

**Q : Quel pattern illustrent bufio.NewReader et gzip.NewReader ?** R : Decorator autour d'un io.Reader.

**Q : C'est quoi le pattern des fonctions d'état ?** R : Chaque état est une fonction qui renvoie la fonction de l'état suivant.

**Q : C'est quoi le sticky error pattern ?** R : Mémoriser la première erreur et ignorer les opérations suivantes, pour vérifier l'erreur une seule fois à la fin.

**Q : Comment faire de l'injection de dépendances en Go ?** R : Manuellement dans main (composition root), ou avec Wire (compilation) ou Fx (exécution).

**Q : Quel pattern pour limiter un débit ?** R : Un rate limiter à jetons, par exemple golang.org/x/time/rate.

**Q : Quand retourner une interface depuis une factory ?** R : Quand l'implémentation est choisie à l'exécution ou doit rester cachée.

**Q : Citez des anti-patterns Go.** R : Interfaces prématurées, utils, panic comme contrôle de flux, goroutines sans fin, état global, interface{} partout.

### Questions de situation

**Q : "Écris un décorateur qui ajoute des retries à n'importe quel client qui implémente une interface `Fetcher`."**
R :
```go
type Fetcher interface {
    Fetch(ctx context.Context, id string) ([]byte, error)
}

type retryFetcher struct {
    next       Fetcher
    tentatives int
    attente    time.Duration
}

func WithRetry(next Fetcher, n int, attente time.Duration) Fetcher {
    return &retryFetcher{next: next, tentatives: n, attente: attente}
}

func (r *retryFetcher) Fetch(ctx context.Context, id string) ([]byte, error) {
    var err error
    for i := range r.tentatives {
        var b []byte
        if b, err = r.next.Fetch(ctx, id); err == nil {
            return b, nil
        }
        select {
        case <-time.After(r.attente << i):   // backoff exponentiel
        case <-ctx.Done():
            return nil, ctx.Err()
        }
    }
    return nil, fmt.Errorf("après %d tentatives: %w", r.tentatives, err)
}
```
"Le décorateur implémente la même interface, donc il se compose avec d'autres (cache, métriques) sans que l'appelant le sache. Il respecte le contexte pendant l'attente. En production j'ajouterais du *jitter* et je ne réessaierais que les erreurs temporaires, pas un 404."

═══════════════════════════════════════════
# 17. ARCHITECTURE & ORGANISATION D'UN PROJET GO
═══════════════════════════════════════════

### Vue d'ensemble

Go n'impose aucune structure de projet, et il n'existe pas de standard officiel au-delà de quelques conventions (`cmd/`, `internal/`). Le dépôt souvent cité "golang-standards/project-layout" **n'est pas** un standard de l'équipe Go, et beaucoup de développeurs Go mettent en garde contre son application systématique. L'équipe Go a publié un guide officiel, *Organizing a Go module*, qui recommande de **commencer simple** et de structurer selon le besoin.

En entretien, on attend une organisation **lisible** : où vit le code métier, comment les dépendances sont injectées, comment on isole l'infrastructure, et surtout de savoir **ne pas sur-architecturer** un petit service.

### Cours

**Structures recommandées selon la taille** (guide officiel *Organizing a Go module*) :

*Petit outil ou petit service* : tout dans le package `main` à la racine, plusieurs fichiers.
```
monoutil/
├── go.mod
├── main.go
├── config.go
└── handler.go
```

*Service de taille moyenne* :
```
monservice/
├── go.mod
├── cmd/
│   └── monservice/
│       └── main.go          # composition root : config, injection, démarrage
├── internal/
│   ├── order/               # package métier : types, logique, interfaces des dépendances
│   │   ├── order.go
│   │   ├── service.go
│   │   └── service_test.go
│   ├── postgres/            # adaptateur : implémente les interfaces de order
│   ├── httpapi/             # handlers HTTP, DTO, middlewares
│   └── config/
├── migrations/
├── api/                     # spécifications OpenAPI ou .proto
└── Makefile / Taskfile
```
- `cmd/<nom>/main.go` : un répertoire par binaire ; `main` reste **mince** (lire la configuration, construire les dépendances, lancer, gérer l'arrêt).
- `internal/` : non importable hors du module, ce qui laisse la liberté de refactorer.
- `pkg/` : convention (non officielle) pour du code **destiné** à être importé par d'autres modules ; inutile dans la plupart des services.
- Organisation **par domaine** (`order`, `customer`, `billing`) plutôt que par couche technique (`models`, `controllers`, `services`), qui crée des packages couplés à tout.

**Architecture hexagonale (ports & adapters) à la Go** :
- le package métier (`order`) définit ses types, sa logique et les **interfaces de ses dépendances** (`type Repository interface { ... }`, `type Payments interface { ... }`) : ce sont les *ports* ;
- les **adaptateurs** (`postgres`, `stripe`, `httpapi`, `kafka`) importent le package métier et implémentent ses interfaces ou l'appellent ;
- le métier n'importe **aucun** adaptateur ni framework ;
- `main` assemble le tout.
Ce découpage tombe naturellement en Go grâce aux interfaces implicites définies côté consommateur. Pour un petit CRUD, il est souvent excessif : un handler qui appelle un repository suffit.

**Composition root dans main** :
```go
func main() {
    if err := run(context.Background(), os.Getenv, os.Stdout); err != nil {
        fmt.Fprintln(os.Stderr, err)
        os.Exit(1)
    }
}

func run(ctx context.Context, getenv func(string) string, out io.Writer) error {
    ctx, stop := signal.NotifyContext(ctx, os.Interrupt, syscall.SIGTERM)
    defer stop()

    cfg, err := config.Load(getenv)
    if err != nil { return err }
    db, err := postgres.Open(ctx, cfg.DatabaseURL)
    if err != nil { return err }
    defer db.Close()

    orders := order.NewService(postgres.NewOrderRepo(db), stripe.New(cfg.StripeKey))
    srv := httpapi.NewServer(cfg.Addr, orders, slog.New(slog.NewJSONHandler(out, nil)))
    return srv.Run(ctx)       // retourne à l'arrêt propre
}
```
Le pattern `run` (popularisé par Mat Ryer) rend `main` testable : les dépendances au monde extérieur (variables d'environnement, sorties, contexte) sont passées en paramètres, et les `defer` s'exécutent normalement (contrairement à un `log.Fatal` au milieu de `main`).

**Modules et dépôts** :
- un **module** (`go.mod`) par dépôt dans la majorité des cas ;
- un **monorepo multi-modules** complique la gestion des versions ; les **workspaces** (`go.work`, Go 1.18) facilitent le développement local de plusieurs modules liés ;
- les microservices d'une même équipe peuvent partager un module, chacun ayant son répertoire `cmd/`.

**Monolithe modulaire vs microservices en Go** : Go se prête bien aux microservices (binaires légers, démarrage instantané), mais les mêmes compromis que dans tout écosystème s'appliquent. Un monolithe modulaire bien découpé en packages `internal/<domaine>` avec des interfaces entre domaines est souvent le meilleur point de départ ; un domaine peut ensuite être extrait en service.

**Couches et DTO** : séparer les types d'API (JSON, Protobuf) des types métier évite de lier le contrat public au modèle interne ; on accepte un peu de code de mapping (pas de MapStruct en Go : on écrit les conversions, ou on les génère).

**Configuration** : variables d'environnement (12-factor), parsées dans une struct typée au démarrage, validées, avec des valeurs par défaut ; bibliothèques possibles : `caarlos0/env`, `kelseyhightower/envconfig`, `koanf`, `viper` (plus lourd). Échouer **au démarrage** si la configuration est invalide.

**Arrêt propre** : contexte annulé sur SIGTERM, `http.Server.Shutdown` avec un délai, arrêt des consommateurs de files, attente des workers, fermeture des connexions de base, dans l'ordre inverse de la création.

**Frameworks** : Go a peu de frameworks "tout-en-un" ; on assemble des bibliothèques. Il existe des kits pour microservices (go-kit, historique ; Kratos ; go-micro ; Encore, plateforme avec génération d'infrastructure), mais beaucoup d'équipes préfèrent la bibliothèque standard + quelques bibliothèques ciblées (routeur, pilote de base, logs, métriques).

### En situation

- **"Un nouveau service, quelle structure ?"** → commencer simple, `cmd/` + `internal/<domaine>`, grossir au besoin.
- **"Le main fait 400 lignes"** → extraire `run()`, des constructeurs par composant.
- **"Le package métier importe le pilote PostgreSQL"** → interface définie côté métier, implémentation dans un adaptateur.
- **"Faut-il un dossier pkg/ ?"** → seulement pour du code destiné à être importé ailleurs.
- **"Comment tester main ?"** → pattern run avec dépendances injectées.

### Questions techniques

**Q : Existe-t-il une structure de projet officielle en Go ?** R : Non, mais un guide officiel (Organizing a Go module) recommande de commencer simple ; project-layout n'est pas un standard officiel.

**Q : À quoi sert cmd/ ?** R : À contenir un répertoire par binaire, avec un main mince.

**Q : À quoi sert internal/ ?** R : À rendre des packages importables uniquement depuis l'arborescence parente du module.

**Q : Faut-il toujours un dossier pkg/ ?** R : Non, seulement pour du code destiné à être importé par d'autres modules.

**Q : Organisation par couche ou par domaine ?** R : Par domaine (order, billing) plutôt que models/controllers, qui couplent tout.

**Q : Comment s'applique l'architecture hexagonale en Go ?** R : Le package métier définit les interfaces de ses dépendances ; les adaptateurs les implémentent ; main assemble.

**Q : C'est quoi le composition root ?** R : L'endroit unique (main) où toutes les dépendances sont construites et injectées.

**Q : C'est quoi le pattern run ?** R : Un main minimal qui appelle run(ctx, dépendances) et gère le code de sortie, ce qui rend le programme testable.

**Q : Pourquoi éviter log.Fatal dans main ?** R : Il appelle os.Exit et empêche l'exécution des defer (fermetures, flush).

**Q : C'est quoi un workspace Go ?** R : Un fichier go.work qui permet de développer plusieurs modules locaux ensemble (Go 1.18).

**Q : Où doit échouer une configuration invalide ?** R : Au démarrage, en validant la struct de configuration.

**Q : Quel ordre pour l'arrêt propre ?** R : Arrêter d'accepter du trafic, terminer les requêtes en cours, arrêter les workers, fermer les ressources dans l'ordre inverse de leur création.

**Q : Go a-t-il des frameworks comme Spring ?** R : Peu ; on assemble des bibliothèques, avec quelques kits (go-kit, Kratos, Encore).

### Questions de situation

**Q : "Présente l'architecture que tu choisirais pour un service de commandes en Go."**
R : "Un module avec `cmd/orders/main.go` et des packages `internal/` par responsabilité. Le cœur est `internal/order` : les types `Order`, `Line`, les règles (calcul du total, transitions de statut), un `Service` qui orchestre les cas d'usage, et les interfaces dont il a besoin (`Repository`, `Payments`, `EventPublisher`). Les adaptateurs vivent à côté : `internal/postgres` (avec sqlc pour les requêtes), `internal/httpapi` (handlers, DTO, validation, middlewares), `internal/kafka` (publication via un outbox).
`main` lit la configuration depuis l'environnement, construit les adaptateurs, les injecte dans le service, démarre le serveur HTTP et le relais d'outbox, et gère l'arrêt propre sur SIGTERM.
Les tests du package `order` utilisent des faux écrits à la main, ceux de `postgres` une vraie base dans testcontainers. Si le service reste petit, je fusionnerais certains packages : la structure doit suivre la complexité, pas la précéder."

═══════════════════════════════════════════
# 18. HTTP : SERVEURS, CLIENTS & API REST
═══════════════════════════════════════════

### Vue d'ensemble

Le package `net/http` est l'un des points forts de Go : un serveur HTTP/1.1 et HTTP/2 de qualité production, un client avec pool de connexions, TLS intégré, et un modèle simple, une interface `Handler` et une goroutine par requête. Beaucoup de services Go en production n'utilisent que la bibliothèque standard, surtout depuis que Go 1.22 a ajouté au `ServeMux` le routage par **méthode** et par **joker** (`GET /users/{id}`), qui couvre la majorité des besoins.

Les pièges sont connus et reviennent en entretien : **timeouts absents** (le serveur et le client par défaut n'en ont pas), corps de réponse non fermé, `http.DefaultClient` utilisé en production, handlers qui partagent un état sans synchronisation, et absence d'arrêt propre.

### Cours

**Le modèle** :
```go
type Handler interface {
    ServeHTTP(http.ResponseWriter, *http.Request)
}
```
Chaque requête s'exécute dans **sa propre goroutine** : les handlers doivent être sûrs en concurrence.

**Routage avec ServeMux (Go 1.22)** :
```go
mux := http.NewServeMux()
mux.HandleFunc("GET /users/{id}", getUser)
mux.HandleFunc("POST /users", createUser)
mux.HandleFunc("GET /files/{path...}", serveFile)   // joker "reste du chemin"
mux.HandleFunc("GET /{$}", home)                     // uniquement "/" exactement

func getUser(w http.ResponseWriter, r *http.Request) {
    id := r.PathValue("id")
    // ...
}
```
- Le motif le plus **spécifique** gagne ; deux motifs ambigus provoquent un panic à l'enregistrement.
- Un motif qui finit par `/` correspond à tout le sous-arbre.
- Méthode non autorisée : 405 automatique avec l'en-tête `Allow`.
- Depuis Go 1.26, la redirection automatique pour une barre oblique finale utilise un 307 au lieu d'un 301.
- Ne pas enregistrer sur le `http.DefaultServeMux` global (tout package importé peut y ajouter des routes, comme `net/http/pprof`).

**Routeurs et frameworks tiers** : **chi** (compatible `net/http`, middlewares, groupes de routes, très utilisé), **gorilla/mux** (historique), **Gin** (populaire, contexte propre, rapide), **Echo**, **Fiber** (basé sur fasthttp, **incompatible** avec `net/http`, donc avec son écosystème de middlewares). Depuis Go 1.22, beaucoup de nouveaux projets restent sur la bibliothèque standard ou chi.

**Écrire la réponse** :
```go
func writeJSON(w http.ResponseWriter, status int, v any) {
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(status)                 // après les en-têtes, avant le corps
    if err := json.NewEncoder(w).Encode(v); err != nil {
        slog.Error("encodage réponse", "err", err)
    }
}
```
Les en-têtes doivent être définis **avant** `WriteHeader` ; le premier `Write` envoie implicitement un 200 ; un second `WriteHeader` est ignoré avec un avertissement (`superfluous response.WriteHeader call`). Pour une erreur simple : `http.Error(w, msg, code)`.

**Lire la requête** :
```go
r.Body = http.MaxBytesReader(w, r.Body, 1<<20)   // limite à 1 Mo
dec := json.NewDecoder(r.Body)
dec.DisallowUnknownFields()
var in CreateUserRequest
if err := dec.Decode(&in); err != nil {
    http.Error(w, "corps invalide", http.StatusBadRequest)
    return
}
```
`r.URL.Query().Get("page")`, `r.Header.Get("Authorization")`, `r.PathValue("id")`, `r.FormValue`, `r.Context()`. Le serveur ferme lui-même le corps de la requête.

**Middlewares** : `func(http.Handler) http.Handler` chaînés (voir Design patterns) : recovery, logging, request ID, authentification, CORS, compression, limitation de débit, métriques, timeouts (`http.TimeoutHandler`). Passer des données de requête via le contexte (`r.WithContext`), avec des clés typées.

**Timeouts du serveur** (le `http.ListenAndServe` par défaut n'en a **aucun**, ce qui expose aux attaques *Slowloris*) :
```go
srv := &http.Server{
    Addr:              ":8080",
    Handler:           mux,
    ReadHeaderTimeout: 5 * time.Second,
    ReadTimeout:       10 * time.Second,
    WriteTimeout:      30 * time.Second,
    IdleTimeout:       120 * time.Second,
    MaxHeaderBytes:    1 << 20,
}
```
Pour un endpoint de streaming (SSE, gros téléchargement), `http.ResponseController` (Go 1.20) permet d'ajuster le délai d'écriture par requête et d'appeler `Flush`.

**Arrêt propre** : `srv.Shutdown(ctx)` arrête d'accepter de nouvelles connexions, attend la fin des requêtes en cours (jusqu'au délai), ferme les connexions inactives ; `ListenAndServe` renvoie alors `http.ErrServerClosed` (à ne pas traiter comme une erreur). `RegisterOnShutdown` pour notifier les connexions longues (WebSocket).

**Le client HTTP** :
```go
var client = &http.Client{
    Timeout: 10 * time.Second,            // délai global de la requête
    Transport: &http.Transport{
        MaxIdleConns:        100,
        MaxIdleConnsPerHost: 20,           // défaut : 2, souvent trop bas
        IdleConnTimeout:     90 * time.Second,
        TLSHandshakeTimeout: 5 * time.Second,
    },
}

req, err := http.NewRequestWithContext(ctx, http.MethodGet, url, nil)
resp, err := client.Do(req)
if err != nil { return err }
defer resp.Body.Close()
if resp.StatusCode != http.StatusOK {
    return fmt.Errorf("statut inattendu %d", resp.StatusCode)
}
```
Règles :
- **Un client partagé** et réutilisé (il est sûr en concurrence et porte le pool de connexions) ; ne jamais créer un `Transport` par requête.
- **Jamais `http.DefaultClient` ni `http.Get` en production** : aucun timeout.
- **Toujours fermer le corps** et le lire jusqu'au bout pour permettre la réutilisation de la connexion (Go 1.27 draine automatiquement une partie du reste à la fermeture).
- Une réponse 4xx/5xx **n'est pas une erreur** pour `client.Do` : vérifier `StatusCode`.
- `MaxIdleConnsPerHost` à 2 par défaut : sous charge vers un même hôte, des connexions sont ouvertes et fermées en permanence (ports en TIME_WAIT) ; l'augmenter.
- Les redirections sont suivies automatiquement (jusqu'à 10) ; `CheckRedirect` pour contrôler.
- Retries : non automatiques ; à implémenter (ou `hashicorp/go-retryablehttp`) seulement pour les requêtes idempotentes, avec backoff.

**API REST en Go, les bonnes pratiques** : codes HTTP corrects (201 + `Location` pour une création, 204 sans corps, 400 vs 422, 401 vs 403, 404, 409, 429 avec `Retry-After`), erreurs au format **Problem Details** (RFC 9457), validation des entrées (`go-playground/validator` ou validation manuelle), versioning (`/v1`), pagination par curseur, idempotence avec une clé `Idempotency-Key` pour les POST critiques, OpenAPI avec génération (`oapi-codegen`, `ogen`) en contract-first, ou génération de spec depuis le code (`swaggo`, `huma`).

**HTTP/2, HTTP/3 et TLS** : HTTP/2 est négocié automatiquement en TLS (ALPN) côté serveur et client ; h2c (HTTP/2 sans TLS) via le champ `Protocols` (Go 1.24). HTTP/3 (QUIC) n'est pas dans la bibliothèque standard : `quic-go`. TLS : `srv.ListenAndServeTLS(cert, key)`, ou `golang.org/x/crypto/acme/autocert` pour Let's Encrypt ; TLS 1.2 minimum par défaut.

**Autres** : `httputil.ReverseProxy` (avec `Rewrite`, `Director` étant déprécié depuis Go 1.26), `http.FileServer` et `http.FS` avec `embed`, `httptest` pour les tests (`NewRecorder`, `NewServer`, et `NewTestServer` en Go 1.27 pour synctest), WebSocket via `coder/websocket` ou `gorilla/websocket`, SSE avec `Flush`.

### En situation

- **"Le serveur accumule des connexions et finit par saturer"** → aucun timeout serveur (Slowloris).
- **"Une dépendance lente bloque toutes les goroutines"** → client sans timeout ou DefaultClient.
- **"Beaucoup de connexions en TIME_WAIT vers un même service"** → corps non lu/fermé, MaxIdleConnsPerHost trop bas, client recréé.
- **"superfluous WriteHeader call"** → WriteHeader appelé deux fois ou après Write.
- **"Les en-têtes que j'ajoute n'apparaissent pas"** → ajoutés après WriteHeader.
- **"Un corps de 2 Go envoyé par un client fait tomber le service"** → MaxBytesReader.
- **"Les requêtes en cours sont coupées au déploiement"** → pas de Shutdown propre.

### Questions techniques

**Q : Quelle interface implémente un handler ?** R : http.Handler, avec ServeHTTP(ResponseWriter, *Request).

**Q : Les handlers s'exécutent-ils en parallèle ?** R : Oui, une goroutine par requête : ils doivent être sûrs en concurrence.

**Q : Qu'apporte Go 1.22 au ServeMux ?** R : Des motifs avec méthode et jokers (`GET /users/{id}`), récupérés par r.PathValue.

**Q : Pourquoi éviter http.DefaultServeMux ?** R : C'est un global auquel n'importe quel package importé peut ajouter des routes.

**Q : chi vs Gin vs Fiber ?** R : chi reste compatible net/http ; Gin a son propre contexte ; Fiber repose sur fasthttp, incompatible avec net/http.

**Q : Dans quel ordre écrire en-têtes, statut et corps ?** R : En-têtes, puis WriteHeader, puis Write ; le premier Write envoie un 200 implicite.

**Q : Comment limiter la taille du corps d'une requête ?** R : http.MaxBytesReader.

**Q : Le serveur par défaut a-t-il des timeouts ?** R : Non ; il faut configurer ReadHeaderTimeout, ReadTimeout, WriteTimeout, IdleTimeout.

**Q : C'est quoi une attaque Slowloris ?** R : Ouvrir beaucoup de connexions et envoyer les en-têtes très lentement pour épuiser le serveur.

**Q : Comment arrêter proprement un serveur HTTP ?** R : srv.Shutdown(ctx), qui attend la fin des requêtes en cours ; ignorer http.ErrServerClosed.

**Q : Pourquoi ne pas utiliser http.Get en production ?** R : Il utilise DefaultClient, sans timeout.

**Q : Pourquoi partager un seul http.Client ?** R : Il est sûr en concurrence et porte le pool de connexions.

**Q : Pourquoi fermer et lire le corps de la réponse ?** R : Pour libérer et réutiliser la connexion.

**Q : client.Do renvoie-t-il une erreur sur un 500 ?** R : Non, il faut vérifier StatusCode.

**Q : Que vaut MaxIdleConnsPerHost par défaut ?** R : 2, souvent trop bas pour des appels intensifs vers un même hôte.

**Q : Comment faire du streaming de réponse ?** R : Écrire puis Flush, via http.ResponseController ou l'interface http.Flusher.

**Q : HTTP/2 est-il supporté nativement ?** R : Oui, négocié automatiquement en TLS ; h2c via Protocols (Go 1.24).

**Q : HTTP/3 est-il dans la bibliothèque standard ?** R : Non, on utilise quic-go.

**Q : Comment tester un handler sans démarrer de serveur ?** R : httptest.NewRecorder et httptest.NewRequest.

**Q : Quel format standard pour les erreurs d'API ?** R : Problem Details, RFC 9457.

### Questions de situation

**Q : "Écris un handler de création d'utilisateur robuste."**
R :
```go
func (h *Handler) createUser(w http.ResponseWriter, r *http.Request) {
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20)
    var in struct {
        Email string `json:"email"`
        Nom   string `json:"nom"`
    }
    dec := json.NewDecoder(r.Body)
    dec.DisallowUnknownFields()
    if err := dec.Decode(&in); err != nil {
        writeProblem(w, http.StatusBadRequest, "corps JSON invalide")
        return
    }
    if !strings.Contains(in.Email, "@") || in.Nom == "" {
        writeProblem(w, http.StatusUnprocessableEntity, "email ou nom invalide")
        return
    }
    u, err := h.users.Create(r.Context(), in.Email, in.Nom)
    switch {
    case errors.Is(err, user.ErrDuplicate):
        writeProblem(w, http.StatusConflict, "email déjà utilisé")
        return
    case err != nil:
        h.log.ErrorContext(r.Context(), "création utilisateur", "err", err)
        writeProblem(w, http.StatusInternalServerError, "erreur interne")
        return
    }
    w.Header().Set("Location", "/users/"+u.ID)
    writeJSON(w, http.StatusCreated, u)
}
```
"Taille limitée, champs inconnus rejetés, validation avant tout appel, erreurs métier traduites en codes précis, détails techniques uniquement dans les logs, `Location` et 201 pour la création, et le contexte de la requête propagé au service pour que tout s'annule si le client part. L'unicité de l'email reste garantie par une contrainte en base."

**Q : "Le service appelle une API externe et, quand elle devient lente, tout le service ralentit."**
R : "Sans timeout, chaque appel lent garde une goroutine et une connexion, et la file grossit jusqu'à épuiser les ressources. Je mettrais un **client dédié** avec un timeout global et des timeouts de connexion et de handshake, et je passerais le **contexte** de la requête entrante, éventuellement réduit à un budget précis.
Ensuite des protections : un **circuit breaker** pour arrêter d'appeler une dépendance en échec et renvoyer vite une erreur ou une valeur dégradée, une **limite de concurrence** vers cette dépendance (sémaphore), un cache si les données le permettent. Et des métriques de latence par dépendance pour voir le problème avant les utilisateurs."

═══════════════════════════════════════════
# 19. gRPC, PROTOBUF & COMMUNICATION ENTRE SERVICES
═══════════════════════════════════════════

### Vue d'ensemble

gRPC, créé par Google, est très présent dans l'écosystème Go : Kubernetes, etcd et la plupart des projets CNCF l'utilisent pour leurs communications internes. Il combine **Protocol Buffers** (un format binaire compact défini par un schéma `.proto`) et **HTTP/2** (multiplexage, streaming), avec du code client et serveur **généré** dans de nombreux langages. Le contrat est explicite et fortement typé, ce qui fait de gRPC un choix naturel pour la communication **service à service**.

En entretien Go, on attend de connaître le cycle de génération, les quatre modes d'appel, les **deadlines**, les codes de statut, les intercepteurs, et la compatibilité des messages Protobuf. Les alternatives récentes comme **Connect** (Buf) et l'outillage **buf** sont souvent appréciées.

### Cours

**Définir un service** :
```protobuf
syntax = "proto3";
package commandes.v1;
option go_package = "github.com/acme/commandes/gen/commandes/v1;commandesv1";

import "google/protobuf/timestamp.proto";

service CommandeService {
  rpc Obtenir(ObtenirRequest) returns (Commande);
  rpc Suivre(SuivreRequest) returns (stream Evenement);      // streaming serveur
}

message ObtenirRequest { string id = 1; }
message Commande {
  string id = 1;
  int64 montant_centimes = 2;
  Statut statut = 3;
  google.protobuf.Timestamp cree_le = 4;
}
enum Statut { STATUT_INCONNU = 0; STATUT_PAYEE = 1; STATUT_EXPEDIEE = 2; }
```

**Générer le code** : `protoc` avec les plugins `protoc-gen-go` (messages) et `protoc-gen-go-grpc` (client et serveur), ou plus simplement **buf** (`buf generate`, avec `buf.yaml` et `buf.gen.yaml`), qui ajoute aussi un linter de style et la **détection des changements incompatibles** (`buf breaking`).

**Implémenter le serveur** :
```go
type serveur struct {
    commandesv1.UnimplementedCommandeServiceServer   // compatibilité ascendante
    svc *commande.Service
}

func (s *serveur) Obtenir(ctx context.Context, req *commandesv1.ObtenirRequest) (*commandesv1.Commande, error) {
    c, err := s.svc.Obtenir(ctx, req.GetId())
    if errors.Is(err, commande.ErrNotFound) {
        return nil, status.Error(codes.NotFound, "commande introuvable")
    }
    if err != nil {
        return nil, status.Error(codes.Internal, "erreur interne")
    }
    return versProto(c), nil
}

grpcServer := grpc.NewServer(grpc.ChainUnaryInterceptor(logging, recovery, auth))
commandesv1.RegisterCommandeServiceServer(grpcServer, &serveur{svc: svc})
lis, _ := net.Listen("tcp", ":9090")
grpcServer.Serve(lis)
```
Embarquer `Unimplemented...Server` permet d'ajouter des méthodes au service sans casser la compilation des serveurs existants.

**Le client** : `conn, err := grpc.NewClient("dns:///commandes:9090", grpc.WithTransportCredentials(...))` (`grpc.Dial` est déprécié au profit de `NewClient`), puis `commandesv1.NewCommandeServiceClient(conn)`. Une connexion gRPC est **multiplexée** et doit être **partagée** et réutilisée, pas recréée à chaque appel.

**Les quatre modes d'appel** : **unaire** (une requête, une réponse), **streaming serveur** (le serveur envoie un flux : suivi en temps réel, export), **streaming client** (le client envoie un flux : téléversement), **bidirectionnel** (chat, synchronisation). En streaming, chaque côté appelle `Send`/`Recv` jusqu'à `io.EOF`.

**Deadlines et annulation** : le client fixe une échéance via le contexte (`context.WithTimeout`) ; elle est **transmise au serveur** dans les métadonnées et propagée aux appels avals qui utilisent le contexte. Sans deadline, un appel peut attendre indéfiniment : toujours en mettre une.

**Codes de statut** : `OK`, `InvalidArgument`, `NotFound`, `AlreadyExists`, `PermissionDenied`, `Unauthenticated`, `ResourceExhausted` (limitation), `FailedPrecondition`, `Aborted` (conflit), `Unavailable` (temporaire, réessayable), `DeadlineExceeded`, `Internal`, `Unimplemented`. `status.Error(codes.X, msg)` côté serveur, `status.Code(err)` / `status.FromError(err)` côté client. Des détails structurés peuvent être joints (`errdetails`, par exemple les violations de champs).

**Métadonnées** : l'équivalent des en-têtes (`metadata.NewOutgoingContext`, `metadata.FromIncomingContext`), pour l'authentification, le traçage, l'identifiant de requête.

**Intercepteurs** : les middlewares de gRPC, unaires et de streaming, côté serveur et côté client : logs, métriques, authentification, recovery, retry, tracing. La bibliothèque `go-grpc-middleware` (v2) en fournit beaucoup ; OpenTelemetry fournit l'instrumentation (`otelgrpc`).

**Autres fonctionnalités** : **health checking** standard (`grpc.health.v1`, utilisé par Kubernetes pour les probes gRPC), **reflection** (pour `grpcurl` et les outils de débogage), équilibrage de charge côté client (`round_robin`) et résolution DNS, keepalives, TLS et mTLS, compression, limites de taille de message (4 Mo par défaut en réception).

**Équilibrage et Kubernetes** : comme les connexions HTTP/2 sont longues et multiplexées, un Service Kubernetes classique (équilibrage par connexion) envoie tout le trafic d'un client vers un seul pod. Solutions : équilibrage côté client avec un Service *headless* et la politique `round_robin`, ou un service mesh / proxy L7 (Envoy, Linkerd, Istio) qui équilibre par requête.

**Évolution des messages Protobuf (compatibilité)** :
- ajouter un champ avec un **nouveau numéro** est compatible ;
- ne **jamais** changer le numéro ni le type d'un champ existant ;
- supprimer un champ : marquer son numéro et son nom `reserved` pour qu'ils ne soient jamais réutilisés ;
- en proto3, les champs scalaires ont une valeur par défaut et on ne distingue pas "absent" de "zéro", sauf avec le mot-clé `optional` ou les types *wrappers* ;
- versionner les packages (`commandes.v1`, `commandes.v2`) pour les changements cassants ;
- `buf breaking` en CI détecte automatiquement les incompatibilités.

**Protobuf en Go** : les messages générés **ne doivent pas être copiés** par valeur (ils contiennent un état interne : vet le signale), on utilise des pointeurs ; accesseurs `GetX()` sûrs sur nil ; `proto.Marshal`, `proto.Equal`, `protojson` pour le JSON canonique ; API "Opaque" récente qui cache les champs derrière des accesseurs.

**Connect (Buf)** : une implémentation compatible gRPC, gRPC-Web et d'un protocole HTTP simple (JSON ou binaire en POST), construite sur `net/http` : les mêmes handlers fonctionnent avec `curl`, les navigateurs et les clients gRPC, ce qui évite un proxy gRPC-Web.

**REST vs gRPC vs messagerie** : REST/JSON pour les API publiques et le navigateur ; gRPC pour le service à service performant et typé, le streaming ; messagerie (Kafka, NATS) pour le découplage temporel et les événements. **grpc-gateway** expose une API REST/JSON générée devant un service gRPC.

### En situation

- **"Un client gRPC attend indéfiniment"** → pas de deadline.
- **"Tout le trafic gRPC va sur un seul pod"** → connexions HTTP/2 longues → équilibrage côté client ou mesh.
- **"Après une modification du .proto, un ancien client décode des valeurs absurdes"** → numéro ou type de champ modifié → reserved, nouveaux numéros.
- **"Le navigateur doit appeler le service gRPC"** → gRPC-Web, Connect ou grpc-gateway.
- **"Comment renvoyer une erreur de validation détaillée ?"** → InvalidArgument + errdetails.BadRequest.

### Questions techniques

**Q : Sur quoi repose gRPC ?** R : Sur Protocol Buffers pour la sérialisation et HTTP/2 pour le transport.

**Q : Comment générer le code Go depuis un .proto ?** R : protoc avec protoc-gen-go et protoc-gen-go-grpc, ou buf generate.

**Q : À quoi sert UnimplementedXxxServer ?** R : À rester compatible quand de nouvelles méthodes sont ajoutées au service.

**Q : grpc.Dial ou grpc.NewClient ?** R : grpc.NewClient ; Dial est déprécié.

**Q : Faut-il créer une connexion par appel ?** R : Non, une connexion gRPC est multiplexée et doit être partagée.

**Q : Les quatre types d'appel gRPC ?** R : Unaire, streaming serveur, streaming client, bidirectionnel.

**Q : Comment fixer un timeout sur un appel gRPC ?** R : Par une deadline dans le contexte, propagée au serveur.

**Q : Comment renvoyer une erreur gRPC ?** R : status.Error(codes.X, message), lue côté client par status.Code.

**Q : Quel code pour une erreur temporaire réessayable ?** R : Unavailable.

**Q : C'est quoi un intercepteur ?** R : Un middleware gRPC (unaire ou streaming, serveur ou client).

**Q : À quoi servent les métadonnées ?** R : À transmettre des informations comme les en-têtes HTTP (auth, trace, request ID).

**Q : Pourquoi le trafic gRPC se concentre-t-il sur un pod derrière un Service Kubernetes ?** R : L'équilibrage se fait par connexion, et la connexion HTTP/2 est longue et multiplexée.

**Q : Comment faire évoluer un message Protobuf sans casse ?** R : Ajouter des champs avec de nouveaux numéros, ne jamais modifier les existants, réserver les numéros supprimés.

**Q : Comment distinguer absent et zéro en proto3 ?** R : Avec le mot-clé optional ou des types wrappers.

**Q : À quoi sert buf breaking ?** R : À détecter automatiquement les changements incompatibles d'un schéma Protobuf.

**Q : Peut-on copier un message Protobuf généré par valeur ?** R : Non, il contient un état interne ; on manipule des pointeurs.

**Q : C'est quoi Connect ?** R : Une implémentation compatible gRPC, gRPC-Web et HTTP/JSON simple, construite sur net/http.

**Q : À quoi sert grpc-gateway ?** R : Exposer une API REST/JSON générée devant un service gRPC.

**Q : Quelle est la taille maximale de message reçu par défaut ?** R : 4 Mo.

### Questions de situation

**Q : "Faut-il passer les communications internes de REST à gRPC ?"**
R : "Ça dépend de la douleur actuelle. gRPC apporte un **contrat strict** généré pour tous les langages, de meilleures performances (binaire, HTTP/2 multiplexé), le streaming et la propagation des deadlines. En contrepartie : un outillage de génération à maintenir, un débogage moins direct (pas de `curl`, il faut `grpcurl` et la reflection), un équilibrage de charge à revoir en Kubernetes, et des clients navigateur qui ont besoin de gRPC-Web ou de Connect.
Si les problèmes sont des contrats d'API qui dérivent entre équipes ou des latences inter-services, gRPC (ou Connect, qui garde une compatibilité HTTP simple) est un bon choix. Pour quelques appels peu fréquents, REST avec OpenAPI et génération de clients suffit."

═══════════════════════════════════════════
# 20. BASES DE DONNÉES EN GO
═══════════════════════════════════════════

### Vue d'ensemble

Go a une approche très différente de Java pour l'accès aux données : pas d'ORM dominant, et une culture qui privilégie le **SQL écrit à la main** et explicite. La base est `database/sql`, une abstraction de la bibliothèque standard avec **pool de connexions intégré**, au-dessus de laquelle on choisit un **pilote** (`pgx` pour PostgreSQL, `go-sql-driver/mysql`...). Autour gravitent des outils qui génèrent du code typé depuis le SQL (**sqlc**), des bibliothèques de confort (**sqlx**, **scany**), des constructeurs de requêtes (**squirrel**, **goqu**) et quelques ORM (**GORM**, **ent**, **Bun**).

Les questions d'entretien portent sur le réglage du pool, la gestion des transactions, la fermeture des `Rows`, la propagation du contexte, les valeurs NULL, l'injection SQL et le choix entre SQL brut, génération de code et ORM.

### Cours

**database/sql, les bases** :
```go
import _ "github.com/jackc/pgx/v5/stdlib"   // enregistre le pilote "pgx"

db, err := sql.Open("pgx", dsn)   // ne se connecte pas encore !
if err != nil { return err }
if err := db.PingContext(ctx); err != nil { return err }   // vérifier la connexion

db.SetMaxOpenConns(25)
db.SetMaxIdleConns(25)
db.SetConnMaxLifetime(30 * time.Minute)
db.SetConnMaxIdleTime(5 * time.Minute)
```
- `*sql.DB` n'est pas **une** connexion mais un **pool**, sûr en concurrence, à créer **une fois** et partager pendant toute la vie du programme.
- `sql.Open` valide seulement les arguments ; c'est `Ping` qui teste la connexion réelle.
- Par défaut, `MaxOpenConns` est **illimité** : sous charge, le service peut ouvrir des centaines de connexions et saturer la base. Toujours le borner, en tenant compte du nombre d'instances (10 pods × 25 connexions = 250 côté base).
- `ConnMaxLifetime` évite de garder des connexions vers un nœud de base qui a changé (bascule, proxy).
- `db.Stats()` expose l'usage du pool (connexions ouvertes, en attente, temps d'attente) : à exporter en métriques.

**Requêtes** :
```go
// une ligne
var u User
err := db.QueryRowContext(ctx,
    "SELECT id, email, nom FROM users WHERE id = $1", id).Scan(&u.ID, &u.Email, &u.Nom)
if errors.Is(err, sql.ErrNoRows) { return User{}, ErrNotFound }

// plusieurs lignes
rows, err := db.QueryContext(ctx, "SELECT id, nom FROM users WHERE actif = $1", true)
if err != nil { return nil, err }
defer rows.Close()                     // INDISPENSABLE : sinon la connexion reste prise
var res []User
for rows.Next() {
    var u User
    if err := rows.Scan(&u.ID, &u.Nom); err != nil { return nil, err }
    res = append(res, u)
}
if err := rows.Err(); err != nil { return nil, err }   // erreur survenue pendant l'itération
return res, nil

// écriture
res, err := db.ExecContext(ctx, "UPDATE users SET nom = $1 WHERE id = $2", nom, id)
n, _ := res.RowsAffected()
```
- **Paramètres liés** (`$1` en PostgreSQL, `?` en MySQL) : **jamais** de concaténation de valeurs (injection SQL). Un nom de table, de colonne ou un `ORDER BY` dynamique ne peut pas être paramétré : liste blanche.
- **Oublier `rows.Close()`** garde la connexion empruntée ; à force, le pool est épuisé et les requêtes attendent indéfiniment. Vérifier aussi `rows.Err()` après la boucle.
- `QueryRow` sans résultat renvoie `sql.ErrNoRows` au `Scan`.
- Toujours les versions **`Context`** (`QueryContext`, `ExecContext`), pour l'annulation et les timeouts.

**Valeurs NULL** : `Scan` dans un `string` d'une colonne NULL échoue. Options : `sql.NullString`, `sql.NullInt64`, `sql.NullTime`, le générique **`sql.Null[T]`** (Go 1.22), des pointeurs (`*string`), ou `COALESCE` dans la requête.

**Transactions** :
```go
func (r *Repo) Transferer(ctx context.Context, de, vers string, montant int64) (err error) {
    tx, err := r.db.BeginTx(ctx, &sql.TxOptions{Isolation: sql.LevelSerializable})
    if err != nil { return err }
    defer func() {
        if err != nil {
            _ = tx.Rollback()       // sans effet si déjà commitée
        }
    }()
    if _, err = tx.ExecContext(ctx, "UPDATE comptes SET solde = solde - $1 WHERE id = $2 AND solde >= $1", montant, de); err != nil {
        return err
    }
    if _, err = tx.ExecContext(ctx, "UPDATE comptes SET solde = solde + $1 WHERE id = $2", montant, vers); err != nil {
        return err
    }
    return tx.Commit()
}
```
- Une transaction **monopolise une connexion** jusqu'au `Commit` ou `Rollback` : pas d'appel réseau long à l'intérieur.
- Toutes les requêtes doivent passer par `tx`, pas par `db` (sinon elles s'exécutent hors transaction, sur une autre connexion).
- Idiome : `defer tx.Rollback()` juste après `BeginTx` (un Rollback après Commit renvoie simplement `sql.ErrTxDone`), ou une fonction utilitaire `withTx(ctx, db, func(tx *sql.Tx) error)` qui gère commit/rollback.
- Faire traverser la transaction aux repositories : passer une interface commune à `*sql.DB` et `*sql.Tx` (`QueryContext`, `ExecContext`...), ou une transaction placée dans un objet "unit of work".

**Requêtes préparées** : `db.PrepareContext` crée une requête réutilisable ; utile pour exécuter très souvent la même requête. Avec un pool, la requête est préparée par connexion. Penser à `stmt.Close()`. Les paramètres liés suffisent pour la sécurité, préparer n'est qu'une optimisation.

**pgx** : le pilote PostgreSQL de référence. Utilisable via `database/sql` (`pgx/stdlib`) ou via son **API native** (`pgxpool`), plus performante et plus riche : types PostgreSQL complets (tableaux, JSONB, intervalles), `COPY` pour l'import massif, `Batch` pour envoyer plusieurs requêtes en un aller-retour, `LISTEN/NOTIFY`, `pgx.CollectRows(rows, pgx.RowToStructByName[T])` pour mapper directement vers des structs.

**Les outils de l'écosystème** :
| Outil | Approche | Points forts / faibles |
|---|---|---|
| `database/sql` seul | SQL à la main | explicite, verbeux (Scan champ par champ) |
| **sqlc** | génère du Go typé depuis des fichiers `.sql` | SQL réel vérifié à la génération, aucune réflexion, très populaire ; requêtes dynamiques moins pratiques |
| **sqlx**, **scany** | extensions de `database/sql` | `Get`/`Select` vers des structs par tags `db`, requêtes nommées |
| **squirrel**, **goqu** | constructeurs de requêtes | requêtes dynamiques (filtres optionnels) sans concaténation |
| **GORM** | ORM complet | productif, associations, hooks ; réflexion, magie, requêtes cachées, N+1 possibles |
| **ent** (Meta) | ORM par génération de code depuis un schéma Go | typé, graphes de relations ; outillage plus lourd |
| **Bun** | ORM "SQL-first" léger | proche du SQL |

La tendance actuelle de la communauté penche vers **sqlc** (ou pgx natif) pour les services, et réserve les ORM aux applications CRUD riches où leur productivité l'emporte.

**Migrations** : **golang-migrate**, **goose**, **Atlas** (migrations déclaratives par différence de schéma), ou les migrations embarquées (fichiers SQL inclus avec `embed`). Les exécuter avant le déploiement de la nouvelle version (job dédié), pas en parallèle au démarrage de chaque réplica sans verrou, et toujours de façon **rétrocompatible** (ajouter avant de supprimer).

**Patterns d'accès** :
- un **repository** par agrégat, dont l'interface est déclarée côté métier ;
- traduire `sql.ErrNoRows` et les violations de contrainte (code PostgreSQL `23505` pour l'unicité, via `*pgconn.PgError`) en erreurs du domaine ;
- éviter le **N+1** (une requête par élément dans une boucle) : jointure, `WHERE id = ANY($1)` avec un tableau, ou chargement par lot ;
- pagination par **curseur** (`WHERE (cree_le, id) < ($1, $2) ORDER BY ... LIMIT`) plutôt que `OFFSET` sur les grosses tables ;
- import massif : `COPY` (pgx `CopyFrom`) ou insertions par lots, des ordres de grandeur plus rapides que ligne par ligne.

**NoSQL et autres** : MongoDB (`mongo-go-driver` officiel), Redis (`go-redis/redis` v9, `rueidis`), Cassandra (`gocql`), Elasticsearch (client officiel), DynamoDB (AWS SDK v2), bases embarquées (SQLite via `modernc.org/sqlite` en pur Go sans cgo, ou `mattn/go-sqlite3` avec cgo ; bbolt, Badger, Pebble pour du clé-valeur).

### En situation

- **"Les requêtes se bloquent après quelques heures"** → `rows.Close()` oublié ou transactions jamais terminées → pool épuisé.
- **"La base refuse des connexions (too many clients)"** → `MaxOpenConns` illimité × nombre de pods.
- **"Scan échoue sur une colonne NULL"** → `sql.Null[T]`, pointeur, COALESCE.
- **"Une requête dans la transaction ne voit pas les écritures précédentes"** → exécutée via `db` au lieu de `tx`.
- **"Les requêtes continuent après le timeout HTTP"** → versions sans Context.
- **"GORM ou sqlc ?"** → contrôle et performance du SQL vs productivité d'un ORM.

### Questions techniques

**Q : Qu'est-ce qu'un *sql.DB ?** R : Un pool de connexions sûr en concurrence, à créer une fois et partager.

**Q : sql.Open se connecte-t-il à la base ?** R : Non, il valide les arguments ; Ping teste la connexion.

**Q : Quelle est la valeur par défaut de MaxOpenConns ?** R : Illimitée ; il faut la borner.

**Q : À quoi sert SetConnMaxLifetime ?** R : À renouveler les connexions périodiquement (bascules, proxies, équilibrage).

**Q : Que se passe-t-il si on oublie rows.Close() ?** R : La connexion reste empruntée, et le pool finit par s'épuiser.

**Q : Pourquoi vérifier rows.Err() ?** R : Pour détecter une erreur survenue pendant l'itération, que Next masque en renvoyant false.

**Q : Que renvoie QueryRow sans résultat ?** R : sql.ErrNoRows au moment du Scan.

**Q : Comment gérer une colonne NULL ?** R : sql.NullString et équivalents, sql.Null[T] (Go 1.22), un pointeur, ou COALESCE.

**Q : Comment éviter l'injection SQL ?** R : Paramètres liés ($1, ?) ; liste blanche pour les identifiants dynamiques.

**Q : Pourquoi utiliser les méthodes Context ?** R : Pour annuler les requêtes sur timeout ou déconnexion du client.

**Q : Quel idiome pour garantir le rollback d'une transaction ?** R : defer tx.Rollback() juste après BeginTx (sans effet après Commit), ou rollback conditionnel sur erreur.

**Q : Pourquoi ne pas faire d'appel réseau dans une transaction ?** R : Elle monopolise une connexion et des verrous pendant tout ce temps.

**Q : Comment faire passer une transaction dans plusieurs repositories ?** R : Une interface commune à *sql.DB et *sql.Tx, ou un objet unit of work.

**Q : pgx natif vs database/sql ?** R : pgx natif est plus performant et expose les fonctionnalités PostgreSQL (COPY, Batch, types) ; database/sql est générique.

**Q : C'est quoi sqlc ?** R : Un générateur de code Go typé à partir de requêtes SQL écrites à la main.

**Q : Avantages et inconvénients de GORM ?** R : Productivité et associations vs réflexion, requêtes implicites, N+1 et performances moins prévisibles.

**Q : Comment détecter une violation d'unicité en PostgreSQL ?** R : errors.As vers *pgconn.PgError et code 23505.

**Q : Comment éviter un N+1 en Go ?** R : Jointure, ou chargement par lot avec `WHERE id = ANY($1)`.

**Q : Quels outils pour les migrations ?** R : golang-migrate, goose, Atlas.

**Q : Comment importer des millions de lignes rapidement en PostgreSQL ?** R : COPY (pgx CopyFrom) ou insertions par lots.

### Questions de situation

**Q : "Après un pic de trafic, toutes les requêtes en base attendent sans fin."**
R : "Symptôme typique d'un **pool épuisé**. Je regarderais `db.Stats()` (ou la métrique correspondante) : si `InUse` est au maximum et `WaitCount` explose, des connexions ne sont pas rendues. Les causes : un `rows.Close()` manquant sur un chemin d'erreur, une transaction jamais commitée ni annulée, ou un traitement long (appel HTTP) effectué pendant qu'on itère sur des `Rows` ou dans une transaction.
Les requêtes "attendent sans fin" aussi parce qu'il n'y a pas de contexte avec délai : avec `QueryContext` et un timeout, elles échoueraient vite au lieu de s'empiler. Je corrigerais les fuites, j'ajouterais `defer rows.Close()` systématique, et des timeouts sur chaque requête. L'analyseur `sqlclosecheck` (golangci-lint) détecte les `Rows` non fermés."

**Q : "Tu dois implémenter une recherche avec des filtres optionnels."**
R : "Concaténer des chaînes SQL est risqué et illisible. J'utiliserais un constructeur de requêtes comme squirrel, qui ajoute les conditions seulement si le filtre est présent et produit une requête paramétrée :
```go
q := sq.Select("id", "nom", "prix").From("produits").PlaceholderFormat(sq.Dollar)
if f.Categorie != "" { q = q.Where(sq.Eq{"categorie": f.Categorie}) }
if f.PrixMax > 0     { q = q.Where(sq.LtOrEq{"prix": f.PrixMax}) }
q = q.OrderBy("prix").Limit(uint64(min(f.Limite, 100)))
sqlStr, args, err := q.ToSql()
```
Le tri dynamique passe par une liste blanche de colonnes autorisées. Avec sqlc, l'alternative est une requête unique utilisant des paramètres optionnels (`WHERE ($1::text IS NULL OR categorie = $1)`), qui reste typée mais peut empêcher l'utilisation optimale des index : à vérifier avec `EXPLAIN`."

═══════════════════════════════════════════
# 21. TESTS EN GO
═══════════════════════════════════════════

### Vue d'ensemble

Le test fait partie du langage : le package `testing` et la commande `go test` suffisent pour les tests unitaires, les benchmarks, les exemples documentés, la couverture et le **fuzzing**. Pas de framework obligatoire. La culture Go privilégie les **tests table-driven**, les **faux écrits à la main** plutôt que des frameworks de mocks lourds, et les tests d'intégration contre de **vraies dépendances** (bases de données en conteneurs).

En entretien, on attend de savoir écrire un test table-driven avec sous-tests, tester un handler HTTP avec `httptest`, isoler une dépendance par une interface, tester du code concurrent (`-race`, `synctest`), et connaître le fuzzing.

### Cours

**Les bases** :
```go
// fichier montant_test.go, même package (ou package montant_test pour un test boîte noire)
func TestAddition(t *testing.T) {
    got := Add(2, 3)
    if got != 5 {
        t.Errorf("Add(2, 3) = %d, attendu 5", got)
    }
}
```
- Fichiers `*_test.go`, fonctions `TestXxx(t *testing.T)`, exclus du binaire de production.
- `t.Errorf` (signale et continue), `t.Fatalf` (signale et arrête **ce** test ; à appeler uniquement depuis la goroutine du test), `t.Log`, `t.Skip`, `t.Helper()` (les erreurs pointent vers l'appelant de la fonction utilitaire), `t.Cleanup(f)` (nettoyage à la fin du test), `t.TempDir()`, `t.Setenv()`, `t.Context()` (Go 1.24, annulé à la fin du test), `t.Chdir()` (1.24), `t.ArtifactDir()` (1.26, fichiers produits par le test).
- **Package de test externe** (`package foo_test`) : teste uniquement l'API exportée, comme un vrai utilisateur ; même package (`package foo`) : accès aux internes.
- Messages au format "got X, want Y" : on teste le comportement, avec des messages lisibles.
- `TestMain(m *testing.M)` pour une initialisation globale du package de test (démarrer une base partagée).

**Tests table-driven** (l'idiome Go par excellence) :
```go
func TestParseDuree(t *testing.T) {
    tests := []struct {
        nom     string
        entree  string
        want    time.Duration
        wantErr bool
    }{
        {"secondes", "30s", 30 * time.Second, false},
        {"minutes", "2m", 2 * time.Minute, false},
        {"vide", "", 0, true},
        {"invalide", "abc", 0, true},
    }
    for _, tt := range tests {
        t.Run(tt.nom, func(t *testing.T) {
            t.Parallel()
            got, err := ParseDuree(tt.entree)
            if (err != nil) != tt.wantErr {
                t.Fatalf("erreur = %v, wantErr %v", err, tt.wantErr)
            }
            if got != tt.want {
                t.Errorf("got %v, want %v", got, tt.want)
            }
        })
    }
}
```
- `t.Run` crée des **sous-tests** nommés, exécutables individuellement : `go test -run 'TestParseDuree/minutes'`.
- `t.Parallel()` exécute les sous-tests en parallèle (avant Go 1.22, il fallait copier `tt := tt` dans la boucle).
- Ajouter un cas = ajouter une ligne.

**Commandes utiles** : `go test ./...`, `-v`, `-run Regex`, `-count=1` (désactive le cache des résultats), `-race`, `-cover`, `-coverprofile=c.out` puis `go tool cover -html=c.out`, `-short` (sauter les tests longs avec `testing.Short()`), `-timeout`, `-failfast`, `-shuffle=on` (ordre aléatoire pour détecter les dépendances entre tests), `-json`. Les résultats de tests réussis sont **mis en cache** si rien n'a changé.

**Assertions** : la bibliothèque standard n'en fournit pas ; beaucoup d'équipes utilisent **testify** (`assert`, `require`), d'autres `go-cmp` (`cmp.Diff(want, got)` pour comparer des structures avec un diff lisible), ou restent en `if` purs. `reflect.DeepEqual` existe mais donne des messages pauvres.

**Doubles de test** :
- **Faux écrits à la main** (préférés) : une petite struct qui implémente l'interface dont le code dépend, avec des champs pour les réponses et l'enregistrement des appels.
```go
type fauxPaiements struct {
    err    error
    appels []int64
}
func (f *fauxPaiements) Debiter(ctx context.Context, compte string, m int64) (string, error) {
    f.appels = append(f.appels, m)
    return "tx-1", f.err
}
```
- **Mocks générés** : `go.uber.org/mock` (fork maintenu de gomock, avec `mockgen`), **mockery** (mocks testify), pour des interfaces larges ou des interactions complexes.
- Une interface petite et définie côté consommateur rend les faux triviaux : c'est l'un des bénéfices concrets des interfaces implicites.

**Tester HTTP** :
```go
func TestGetUser(t *testing.T) {
    h := NewHandler(&fauxRepo{users: map[string]User{"1": {ID: "1", Nom: "Ana"}}})
    req := httptest.NewRequest(http.MethodGet, "/users/1", nil)
    rec := httptest.NewRecorder()
    h.ServeHTTP(rec, req)
    if rec.Code != http.StatusOK {
        t.Fatalf("statut %d", rec.Code)
    }
}
```
`httptest.NewServer(handler)` démarre un vrai serveur local pour tester un **client** HTTP (y compris les erreurs et les timeouts simulés). `httptest.NewTLSServer` pour TLS.

**Tester du code concurrent et temporel** :
- `go test -race` en CI, systématiquement.
- **`testing/synctest`** (stable en Go 1.25) : `synctest.Test(t, func(t *testing.T) { ... })` exécute le code dans une **bulle** avec une **horloge virtuelle** : `time.Sleep(time.Hour)` y est instantané, et `synctest.Wait()` attend que toutes les goroutines de la bulle soient bloquées. Permet de tester des timeouts, des retries, des tickers et des caches à expiration de façon rapide et déterministe. Go 1.27 ajoute `synctest.Sleep` et `httptest.NewTestServer` (réseau en mémoire compatible avec les bulles).
- `goleak` (Uber) pour vérifier qu'aucune goroutine ne fuit à la fin des tests.
- Jamais de `time.Sleep` "pour attendre" dans un test : synchroniser sur un channel, un WaitGroup, ou utiliser synctest.

**Fuzzing** (Go 1.18) : générer des entrées aléatoires guidées par la couverture pour trouver des crashs et des cas limites.
```go
func FuzzParse(f *testing.F) {
    f.Add("30s")                      // corpus de départ
    f.Fuzz(func(t *testing.T, s string) {
        d, err := ParseDuree(s)
        if err == nil && d < 0 {
            t.Errorf("durée négative pour %q", s)
        }
    })
}
```
`go test -fuzz=FuzzParse -fuzztime=30s` ; les entrées qui font échouer le test sont enregistrées dans `testdata/fuzz/` et rejouées ensuite comme tests normaux. Idéal pour les parseurs, les décodeurs, les fonctions de validation et tout ce qui reçoit des données externes.

**Tests d'intégration** :
- **testcontainers-go** : démarrer PostgreSQL, Redis, Kafka dans des conteneurs depuis le test, avec des modules prêts à l'emploi.
- Séparer avec des **build tags** (`//go:build integration` puis `go test -tags=integration`) ou `testing.Short()`.
- Chaque test isole ses données (schéma ou base par test, transactions annulées, données préfixées).

**Autres techniques** :
- **Golden files** : comparer une sortie à un fichier de référence dans `testdata/`, régénéré avec un drapeau `-update`.
- **Exemples** : `func ExampleParseDuree() { fmt.Println(...) // Output: 30s }`, vérifiés par `go test` et affichés dans la documentation.
- `testdata/` : répertoire ignoré par la commande go, pour les fixtures.
- **testing/fstest** (`fstest.MapFS`) pour tester du code qui lit des fichiers via `fs.FS`.
- **testscript** pour tester des CLI par scripts.
- **Couverture** des tests d'intégration et des binaires (`go build -cover`, Go 1.20).
- Tests de mutation (outils tiers comme gremlins), tests par propriétés (`rapid`, `gopter`).

### En situation

- **"Comment tester ce service sans base de données ?"** → interface du repository côté métier + faux en mémoire.
- **"Le test qui vérifie un timeout de 30 s prend 30 s"** → testing/synctest et horloge virtuelle.
- **"Les tests passent seuls mais échouent ensemble"** → état partagé, `-shuffle=on`, `t.Parallel()` mal maîtrisé.
- **"Le test ne se relance pas"** → cache des résultats → `-count=1`.
- **"Valider un parseur contre des entrées malformées"** → fuzzing.
- **"Tester un client HTTP"** → httptest.NewServer qui simule l'API, y compris les erreurs.

### Questions techniques

**Q : Comment nomme-t-on un fichier et une fonction de test ?** R : Fichier *_test.go, fonction TestXxx(t *testing.T).

**Q : t.Error vs t.Fatal ?** R : Signaler et continuer vs signaler et arrêter le test courant.

**Q : Peut-on appeler t.Fatal depuis une autre goroutine ?** R : Non, uniquement depuis la goroutine du test.

**Q : À quoi sert t.Helper() ?** R : À faire pointer les messages d'erreur vers l'appelant de la fonction utilitaire.

**Q : C'est quoi un test table-driven ?** R : Un slice de cas (entrées, sorties attendues) parcouru dans une boucle, avec un sous-test par cas.

**Q : À quoi sert t.Run ?** R : Créer des sous-tests nommés, exécutables individuellement avec -run.

**Q : Que fait t.Parallel() ?** R : Exécuter le test ou sous-test en parallèle des autres tests parallèles.

**Q : package foo vs package foo_test ?** R : Accès aux internes vs test de l'API exportée uniquement.

**Q : Comment désactiver le cache des tests ?** R : -count=1.

**Q : Comment mesurer la couverture ?** R : -cover ou -coverprofile puis go tool cover -html.

**Q : Go a-t-il une bibliothèque d'assertions standard ?** R : Non ; testify ou go-cmp sont courants.

**Q : Faux manuel ou mock généré ?** R : Faux manuel pour de petites interfaces ; mock généré (mockgen, mockery) pour des interactions complexes.

**Q : Comment tester un handler HTTP ?** R : httptest.NewRequest et httptest.NewRecorder.

**Q : Comment tester un client HTTP ?** R : httptest.NewServer qui simule le serveur distant.

**Q : À quoi sert testing/synctest ?** R : Tester du code concurrent et temporel avec une horloge virtuelle, rapidement et de façon déterministe (Go 1.25).

**Q : C'est quoi le fuzzing en Go ?** R : Des tests FuzzXxx qui génèrent des entrées guidées par la couverture ; les échecs sont enregistrés dans testdata.

**Q : Comment séparer les tests d'intégration ?** R : Build tags (//go:build integration) ou testing.Short().

**Q : C'est quoi testcontainers-go ?** R : Une bibliothèque qui démarre des dépendances réelles en conteneurs depuis les tests.

**Q : À quoi sert TestMain ?** R : Exécuter une initialisation et un nettoyage globaux autour de tous les tests du package.

**Q : C'est quoi un golden file ?** R : Un fichier de sortie de référence comparé au résultat du test, régénéré à la demande.

**Q : Les exemples sont-ils exécutés ?** R : Oui, go test vérifie leur sortie via le commentaire Output.

**Q : Comment détecter les fuites de goroutines en test ?** R : goleak.

**Q : À quoi sert -shuffle=on ?** R : Exécuter les tests dans un ordre aléatoire pour révéler les dépendances cachées entre eux.

### Questions de situation

**Q : "Comment testes-tu une fonction qui réessaie un appel 3 fois avec un délai d'une seconde ?"**
R : "Sans précaution, le test durerait 3 secondes et serait sensible à la charge de la machine. Deux approches. **testing/synctest** : j'exécute le test dans une bulle, le `time.After` du retry utilise l'horloge virtuelle, donc les trois attentes sont instantanées, et je vérifie le nombre d'appels et la durée virtuelle écoulée.
Ou j'injecte le délai (et éventuellement une fonction d'attente) en paramètre, que je règle à zéro en test. Dans les deux cas, la dépendance appelée est un faux qui échoue deux fois puis réussit, et je teste aussi l'annulation par le contexte pendant une attente."

**Q : "Ta couverture est de 85 % mais des régressions passent quand même. Que fais-tu ?"**
R : "La couverture mesure ce qui est exécuté, pas ce qui est vérifié. Je regarderais d'abord les régressions échappées : sont-elles dans l'intégration (requêtes SQL, sérialisation), la concurrence, ou des cas limites ? Chaque catégorie appelle un type de test différent : tests d'intégration avec testcontainers pour le SQL, `-race` et synctest pour la concurrence, **fuzzing** pour les entrées malformées, et des tests table-driven enrichis des cas limites (vide, nil, bornes, unicode).
Je pourrais aussi lancer un outil de test de mutation sur les packages critiques pour repérer les assertions manquantes. Et chaque bug corrigé reçoit d'abord un test qui le reproduit."

═══════════════════════════════════════════
# 22. CONFIGURATION, LOGS & OBSERVABILITÉ
═══════════════════════════════════════════

### Vue d'ensemble

Un service Go en production doit être **observable** : des logs structurés pour comprendre un incident, des métriques pour détecter une dérive et alerter, et des traces distribuées pour suivre une requête à travers plusieurs services. Depuis Go 1.21, la bibliothèque standard fournit les **logs structurés** (`log/slog`) ; pour les métriques et les traces, l'écosystème s'est standardisé autour de **Prometheus** et **OpenTelemetry**.

Le runtime Go expose aussi beaucoup d'informations (goroutines, GC, mémoire) et des profils accessibles à chaud (pprof). En entretien, on attend de savoir configurer `slog`, instrumenter un service HTTP, choisir ce qu'on mesure, et propager le contexte de trace.

### Cours

**log/slog (Go 1.21)** :
```go
logger := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{
    Level:     slog.LevelInfo,
    AddSource: false,
}))
slog.SetDefault(logger)

logger.Info("commande créée", "commande_id", id, "montant", m, "client", cid)
logger.Error("paiement refusé", "err", err, slog.String("commande_id", id))

reqLogger := logger.With("request_id", rid)     // attributs communs
reqLogger.InfoContext(ctx, "traitement")         // le contexte permet d'ajouter la trace
```
- Niveaux : `Debug`, `Info`, `Warn`, `Error` ; niveau modifiable à chaud avec `slog.LevelVar`.
- Handlers : `TextHandler` (lisible, en local), `JSONHandler` (production, ingéré par Loki, Elasticsearch, Datadog...), `slog.NewMultiHandler` (Go 1.26) pour écrire vers plusieurs destinations ; handlers tiers (zap, zerolog via adaptateurs, coloration en local).
- Attributs typés (`slog.String`, `slog.Int`, `slog.Any`, `slog.Group`) plus efficaces que les paires clé-valeur libres ; l'analyseur `sloglint` vérifie la cohérence.
- Masquer les données sensibles en implémentant `slog.LogValuer` sur un type (`func (t Token) LogValue() slog.Value { return slog.StringValue("***") }`).
- Un handler personnalisé peut extraire du contexte le `trace_id` et le `request_id` pour les ajouter à chaque ligne : c'est ce qui permet de passer d'une trace aux logs.
- Avant slog, l'écosystème utilisait **zap** (Uber, très performant), **zerolog** (zéro allocation), **logrus** (historique, en maintenance) ; on les rencontre encore beaucoup.

**Bonnes pratiques de logs** : JSON sur la sortie standard (12-factor), un événement par ligne, des clés stables (`user_id` partout, pas tantôt `uid` tantôt `userId`), journaliser une erreur **une seule fois** là où elle est traitée, pas de données personnelles ni de secrets, niveau Debug désactivé en production (ou échantillonné), pas de log dans les boucles serrées.

**Métriques avec Prometheus** (`prometheus/client_golang`) :
```go
var (
    requetes = promauto.NewCounterVec(prometheus.CounterOpts{
        Name: "http_requests_total", Help: "Requêtes HTTP",
    }, []string{"route", "method", "status"})
    duree = promauto.NewHistogramVec(prometheus.HistogramOpts{
        Name: "http_request_duration_seconds", Buckets: prometheus.DefBuckets,
    }, []string{"route", "method"})
)
http.Handle("/metrics", promhttp.Handler())
```
- Types : **Counter** (ne fait qu'augmenter), **Gauge** (monte et descend), **Histogram** (distribution, base des percentiles côté serveur), **Summary** (quantiles calculés côté client, non agrégeables).
- Le collecteur Go par défaut expose `go_goroutines`, `go_gc_duration_seconds`, `go_memstats_*`, `process_*`.
- **Cardinalité** : un label par **route** (`/users/{id}`), jamais par URL complète ou identifiant utilisateur, sous peine de créer des millions de séries.
- Méthodes **RED** pour un service (Rate, Errors, Duration) et **USE** pour les ressources (Utilization, Saturation, Errors) ; quatre signaux dorés : latence, trafic, erreurs, saturation.

**OpenTelemetry Go** (`go.opentelemetry.io/otel`) : le standard pour les **traces** (et aussi métriques et logs). On configure un *TracerProvider* avec un exporteur **OTLP** vers un collecteur, on instrumente HTTP (`otelhttp.NewHandler`, `otelhttp.NewTransport`), gRPC (`otelgrpc`), les bases (`otelsql`, `otelpgx`), Kafka, et on crée des *spans* manuels pour les étapes métier :
```go
ctx, span := tracer.Start(ctx, "calculer-tarif")
defer span.End()
span.SetAttributes(attribute.String("client.segment", seg))
if err != nil {
    span.RecordError(err)
    span.SetStatus(codes.Error, err.Error())
}
```
- La propagation entre services passe par l'en-tête W3C `traceparent`, dans le **contexte** : sans `ctx` propagé, la trace se coupe.
- **Instrumentation automatique** : une instrumentation basée sur **eBPF** (projet OpenTelemetry Go Auto-Instrumentation) peut tracer un binaire Go sans modifier le code ; l'instrumentation par bibliothèques reste la plus complète.
- Échantillonnage (head-based au début de la trace, ou tail-based dans le collecteur pour garder toutes les traces en erreur ou lentes).

**Santé et probes** : endpoints `/healthz` (liveness : le processus répond, **sans** vérifier les dépendances, pour ne pas redémarrer en cascade quand la base tombe) et `/readyz` (readiness : prêt à recevoir du trafic, dépendances critiques joignables, faux pendant l'arrêt propre).

**Profilage en production** : `net/http/pprof` sur un port d'administration interne, profilage continu (Pyroscope, Parca, Datadog), runtime/metrics, trace flight recorder (Go 1.25).

**Informations de build** : `debug.ReadBuildInfo()` donne la version du module, le commit VCS et la date de build (inclus automatiquement depuis Go 1.18) ; à exposer dans un endpoint `/version` ou une métrique `build_info`. Injection classique d'une version à la compilation : `go build -ldflags "-X main.version=1.4.2"`.

**Configuration** : variables d'environnement lues dans une struct typée au démarrage (`caarlos0/env`, `envconfig`), fichiers YAML/TOML si nécessaire (`koanf`, `viper`), drapeaux (`flag`), secrets montés comme fichiers ou injectés depuis un gestionnaire de secrets. Validation au démarrage et échec immédiat si invalide ; rechargement à chaud rarement nécessaire (un redémarrage progressif suffit), sinon `atomic.Pointer[Config]`.

### En situation

- **"Impossible de retrouver les logs d'une requête précise"** → request_id et trace_id ajoutés à chaque log via le contexte.
- **"Prometheus sature"** → explosion de cardinalité (labels avec identifiants ou URL complètes).
- **"La trace s'arrête au service B"** → contexte non propagé ou client HTTP non instrumenté.
- **"Le pod redémarre en boucle quand la base est indisponible"** → liveness qui vérifie la base.
- **"Quelle version tourne en production ?"** → debug.ReadBuildInfo, ldflags, métrique build_info.
- **"Des mots de passe apparaissent dans les logs"** → LogValuer et revue des attributs loggés.

### Questions techniques

**Q : Qu'apporte log/slog ?** R : Des logs structurés (clé-valeur, JSON) dans la bibliothèque standard, avec niveaux et handlers (Go 1.21).

**Q : TextHandler vs JSONHandler ?** R : Lisible pour le développement vs structuré pour l'ingestion en production.

**Q : Comment changer le niveau de log à chaud ?** R : Avec un slog.LevelVar passé aux options du handler.

**Q : Comment masquer une donnée sensible dans slog ?** R : En implémentant slog.LogValuer sur son type.

**Q : Qu'apporte slog.NewMultiHandler ?** R : L'envoi des logs vers plusieurs handlers à la fois (Go 1.26).

**Q : Quelles bibliothèques de logs avant slog ?** R : zap, zerolog, logrus.

**Q : Les types de métriques Prometheus ?** R : Counter, Gauge, Histogram, Summary.

**Q : Histogram vs Summary ?** R : Buckets agrégeables entre instances (percentiles côté serveur) vs quantiles calculés par l'instance, non agrégeables.

**Q : Pourquoi éviter un label user_id ?** R : Explosion de la cardinalité : une série temporelle par utilisateur.

**Q : Quelles métriques le runtime Go expose-t-il ?** R : Goroutines, durées de GC, statistiques mémoire, threads, descripteurs (via le collecteur Go et process).

**Q : Comment se propage une trace entre services ?** R : Par l'en-tête W3C traceparent, porté par le contexte et injecté par les clients instrumentés.

**Q : Comment instrumenter un serveur HTTP avec OpenTelemetry ?** R : En enveloppant le handler avec otelhttp.NewHandler (et le client avec otelhttp.NewTransport).

**Q : Liveness vs readiness ?** R : Le processus est vivant (sans dépendances) vs prêt à recevoir du trafic.

**Q : Pourquoi une liveness ne doit-elle pas vérifier la base ?** R : Une panne de base redémarrerait tous les pods en cascade sans rien résoudre.

**Q : Comment connaître la version d'un binaire Go ?** R : debug.ReadBuildInfo (module, commit VCS) ou une variable injectée par -ldflags -X.

**Q : RED vs USE ?** R : Rate, Errors, Duration pour les services vs Utilization, Saturation, Errors pour les ressources.

**Q : Où doit échouer une configuration invalide ?** R : Au démarrage.

### Questions de situation

**Q : "Comment instrumentes-tu un nouveau service Go avant sa mise en production ?"**
R : "Logs : `slog` en JSON sur la sortie standard, un middleware qui ajoute `request_id` et `trace_id` au contexte, un logger dérivé par requête, et une ligne par requête avec route, statut et durée.
Métriques : histogramme de latence et compteur de requêtes par route, méthode et statut (labels à faible cardinalité), métriques du pool de base (`db.Stats`), et les métriques runtime par défaut ; un endpoint `/metrics`.
Traces : OpenTelemetry avec `otelhttp` côté serveur et client, l'instrumentation de la base et de Kafka, et quelques spans métier ; export OTLP vers un collecteur avec échantillonnage.
Opérations : `/healthz` et `/readyz` distincts, pprof sur un port interne, la version exposée en métrique `build_info`. Puis des tableaux de bord RED et des alertes sur les SLO, plutôt que sur des seuils de CPU."

═══════════════════════════════════════════
# 23. SÉCURITÉ EN GO
═══════════════════════════════════════════

### Vue d'ensemble

Go part avec de bons atouts : sûreté mémoire (pas de dépassement de tampon exploitable hors `unsafe` et cgo), une bibliothèque cryptographique **de haute qualité** maintenue par l'équipe Go (avec un module certifiable **FIPS 140-3** depuis Go 1.24 et de la cryptographie **post-quantique**), `html/template` qui échappe automatiquement, un moteur de regex sans backtracking (pas de ReDoS), et un outillage officiel de détection de vulnérabilités (**govulncheck**).

Mais les failles applicatives restent les mêmes que partout : injection SQL, contrôle d'accès défaillant, SSRF, path traversal, secrets dans le code, dépendances vulnérables, absence de timeouts (déni de service). Les questions d'entretien portent sur ces points, appliqués à l'écosystème Go.

### Cours

**Les risques classiques, version Go** :
- **Injection SQL** : toujours des paramètres liés (`$1`, `?`) ; jamais `fmt.Sprintf` dans une requête. Pour les identifiants dynamiques (colonne de tri), une liste blanche.
- **Injection de commandes** : `exec.Command("ls", userArg)` passe les arguments séparément, sans shell : sûr ; `exec.Command("sh", "-c", "ls "+userArg)` est vulnérable. Depuis Go 1.19, `exec.Command` ne résout plus un exécutable du répertoire courant via le PATH (protection contre un binaire malveillant local).
- **XSS** : `html/template` échappe selon le contexte ; ne pas convertir une entrée utilisateur en `template.HTML`. Ne jamais produire du HTML avec `text/template`.
- **Path traversal** : un nom de fichier `../../etc/passwd` ; `filepath.Clean` ne suffit pas seul. Utiliser **`os.Root`** (Go 1.24), qui confine toutes les opérations sous un répertoire (y compris face aux liens symboliques), ou `filepath.IsLocal` (Go 1.20) pour valider un chemin relatif. `http.FileServer` sur un `http.Dir` gère déjà ce cas.
- **SSRF** : télécharger une URL fournie par l'utilisateur ; valider le schéma, résoudre et refuser les adresses privées et de lien local (métadonnées cloud `169.254.169.254`), y compris après redirection, via un `net.Dialer` avec un contrôle (`Control`) qui vérifie l'adresse réellement connectée (contre le *DNS rebinding*).
- **Déni de service** : serveurs et clients HTTP **avec timeouts**, `http.MaxBytesReader` sur les corps, `io.LimitReader` sur les entrées non fiables, limites de taille de JSON, rate limiting (`golang.org/x/time/rate`), décompression bornée (bombes zip).
- **Contrôle d'accès** : vérifier côté serveur que la ressource appartient à l'utilisateur (IDOR/BOLA), dans chaque handler ou dans la requête SQL elle-même.
- **Mass assignment** : décoder le JSON dans un **DTO** d'entrée dédié, pas directement dans l'entité (qui pourrait avoir un champ `Role` ou `IsAdmin`).
- **Désérialisation** : `encoding/gob` et le décodage YAML de types arbitraires sur des données non fiables sont à éviter ; JSON vers des structs typées est sûr.
- **Fuites d'information** : pas de détails d'erreur internes dans les réponses, pas d'endpoints pprof ou expvar publics.

**Cryptographie dans la bibliothèque standard** :
- **Aléatoire** : `crypto/rand` pour tout jeton, identifiant de session, sel ou clé (`rand.Read`, `rand.Text()` en Go 1.24) ; **jamais** `math/rand` pour la sécurité. Depuis Go 1.24, `crypto/rand.Read` ne peut pas échouer en pratique.
- **Hachage** : `crypto/sha256`, `sha512`, `sha3` (1.24) pour l'intégrité.
- **Mots de passe** : **jamais** un hachage rapide ; `golang.org/x/crypto/bcrypt` ou `argon2` (argon2id), ou `crypto/pbkdf2` (stdlib 1.24) si une conformité l'impose.
- **HMAC** : `crypto/hmac` pour signer (webhooks, jetons), comparaison en **temps constant** avec `hmac.Equal` ou `subtle.ConstantTimeCompare`.
- **Chiffrement symétrique** : `crypto/aes` + `crypto/cipher` en mode **GCM** (chiffrement authentifié), nonce unique par message ; ou `golang.org/x/crypto/chacha20poly1305`.
- **Asymétrique et signatures** : `crypto/ed25519` (simple et moderne), `ecdsa`, `rsa` (le padding PKCS#1 v1.5 pour le chiffrement est déprécié en Go 1.26) ; `crypto/ecdh` pour l'échange de clés ; `crypto/hkdf` (1.24) pour dériver des clés.
- **Post-quantique** : `crypto/mlkem` (Go 1.24, échange de clés ML-KEM), échanges hybrides post-quantiques **activés par défaut** dans `crypto/tls` (X25519MLKEM768 dès 1.24, variantes SecP256r1MLKEM768 en 1.26), `crypto/mldsa` (Go 1.27, signatures ML-DSA), `crypto/hpke` (1.26).
- **FIPS 140-3** : depuis Go 1.24, un module cryptographique natif certifiable, activable avec `GODEBUG=fips140=on` (ou `only`) et `GOFIPS140` au build, sans recourir à BoringCrypto ni à cgo.
- Depuis Go 1.26, les paramètres `rand io.Reader` des fonctions de génération de clés sont ignorés au profit d'une source sûre ; en test, `testing/cryptotest.SetGlobalRandom` rend l'aléatoire déterministe.

**TLS** : `crypto/tls` avec des valeurs par défaut sûres (TLS 1.2 minimum, suites modernes, TLS 1.3). Ne **jamais** mettre `InsecureSkipVerify: true` en production. mTLS : `ClientAuth: tls.RequireAndVerifyClientCert` et un `ClientCAs`. Certificats automatiques : `autocert` (Let's Encrypt). Erreur `x509: certificate signed by unknown authority` : la chaîne n'est pas dans le pool de confiance (image `scratch` sans certificats CA : copier `/etc/ssl/certs/ca-certificates.crt` ou utiliser distroless).

**Authentification et autorisation** : JWT avec `golang-jwt/jwt` (v5) ou `lestrrat-go/jwx`, en **fixant l'algorithme attendu** (refuser `none` et les confusions HS/RS) et en validant `exp`, `iss`, `aud` ; OIDC avec `coreos/go-oidc` et `golang.org/x/oauth2` ; sessions avec cookies `HttpOnly`, `Secure`, `SameSite` ; protection CSRF (`http.CrossOriginProtection`, ajoutée en Go 1.25, qui s'appuie sur les en-têtes `Sec-Fetch-Site`/`Origin`, ou `gorilla/csrf`) ; autorisation avec des politiques (Casbin, OPA) ou des vérifications explicites.

**Chaîne d'approvisionnement** :
- **go.sum** : empreintes cryptographiques de chaque module, vérifiées à chaque téléchargement ;
- **checksum database** (`sum.golang.org`) : journal public transparent qui garantit que tout le monde reçoit le même code pour une version donnée (`GONOSUMDB`/`GOPRIVATE` pour les modules privés) ;
- **proxy de modules** (`proxy.golang.org`) : cache immuable des versions publiées ;
- **govulncheck** (`golang.org/x/vuln/cmd/govulncheck`) : détecte les vulnérabilités connues de la base officielle Go, et ne signale que celles dont le code vulnérable est **réellement appelé** par votre programme (analyse d'atteignabilité), ce qui réduit fortement le bruit ;
- **gosec** (inclus dans golangci-lint) : analyse statique de sécurité ;
- builds reproductibles (`-trimpath`), SBOM (les informations de dépendances sont incluses dans le binaire, lisibles par `go version -m`), signature des images ;
- minimiser les dépendances ("a little copying is better than a little dependency").

**Secrets** : jamais dans le code ni l'image ; variables d'environnement ou fichiers montés depuis un gestionnaire (Vault, Secrets Manager, Kubernetes Secrets chiffrés), rotation, pas de secrets dans les logs (`slog.LogValuer`). `runtime/secret` (expérimental 1.26) pour effacer les données sensibles en mémoire.

**Conteneurs** : image minimale (`scratch`, distroless), utilisateur non root, système de fichiers en lecture seule, binaire statique sans shell (réduit la surface d'attaque).

### En situation

- **"Un audit signale InsecureSkipVerify"** → certificats CA à installer, jamais désactiver la vérification.
- **"Des jetons de réinitialisation sont devinables"** → math/rand au lieu de crypto/rand.
- **"Un utilisateur télécharge des fichiers hors du répertoire autorisé"** → path traversal → os.Root.
- **"x509: certificate signed by unknown authority dans une image scratch"** → certificats CA absents.
- **"Une alerte de vulnérabilité sur une dépendance"** → govulncheck pour savoir si le code vulnérable est atteint.
- **"Le serveur tombe sous un afflux de connexions lentes"** → timeouts serveur.

### Questions techniques

**Q : Go est-il sûr en mémoire ?** R : Oui hors unsafe et cgo : pas de dépassement de tampon exploitable, GC, bornes vérifiées.

**Q : Comment éviter l'injection SQL en Go ?** R : Paramètres liés, jamais fmt.Sprintf dans une requête ; liste blanche pour les identifiants.

**Q : exec.Command est-il vulnérable à l'injection ?** R : Non tant qu'on passe les arguments séparément sans shell ; oui avec sh -c et une concaténation.

**Q : Comment se protéger du path traversal ?** R : os.Root (Go 1.24) ou filepath.IsLocal, plutôt qu'un simple Clean.

**Q : math/rand ou crypto/rand pour un jeton ?** R : crypto/rand, toujours.

**Q : Comment hacher un mot de passe en Go ?** R : bcrypt ou argon2id (golang.org/x/crypto), jamais SHA-256 seul.

**Q : Comment comparer deux secrets ?** R : En temps constant : hmac.Equal ou subtle.ConstantTimeCompare.

**Q : Quel mode pour le chiffrement AES ?** R : GCM (chiffrement authentifié), avec un nonce unique par message.

**Q : Que permet le module FIPS 140-3 de Go ?** R : Une cryptographie certifiable native (Go 1.24), activée par GODEBUG=fips140 et GOFIPS140.

**Q : Quelle cryptographie post-quantique dans Go ?** R : crypto/mlkem (1.24), échanges TLS hybrides par défaut, crypto/mldsa (1.27), crypto/hpke (1.26).

**Q : Pourquoi ne jamais utiliser InsecureSkipVerify ?** R : Il désactive la vérification du certificat et permet une attaque de l'homme du milieu.

**Q : Que garantit go.sum ?** R : Que le contenu des modules téléchargés correspond aux empreintes enregistrées.

**Q : À quoi sert sum.golang.org ?** R : C'est un journal public transparent des empreintes de modules, qui garantit que tous reçoivent le même code.

**Q : Qu'a de particulier govulncheck ?** R : Il ne signale que les vulnérabilités dont le code est réellement appelé par le programme.

**Q : À quoi sert gosec ?** R : Analyse statique de sécurité du code Go.

**Q : Comment valider un JWT correctement ?** R : Fixer l'algorithme attendu, vérifier la signature, exp, iss et aud.

**Q : Pourquoi décoder dans un DTO plutôt que dans l'entité ?** R : Pour empêcher le mass assignment de champs sensibles.

**Q : Qu'apporte http.CrossOriginProtection ?** R : Une protection CSRF basée sur les en-têtes Sec-Fetch-Site et Origin (Go 1.25).

**Q : Pourquoi une image scratch n'a-t-elle pas de certificats CA ?** R : Elle est vide ; il faut copier le fichier de certificats ou utiliser distroless.

### Questions de situation

**Q : "Implémente la vérification de signature d'un webhook entrant."**
R :
```go
func verifierSignature(secret []byte, corps []byte, signatureHex string) bool {
    mac := hmac.New(sha256.New, secret)
    mac.Write(corps)
    attendu := mac.Sum(nil)
    recu, err := hex.DecodeString(signatureHex)
    if err != nil {
        return false
    }
    return hmac.Equal(attendu, recu)      // comparaison en temps constant
}
```
"Je lis le corps brut avec une taille maximale (`http.MaxBytesReader`) avant de le décoder, car la signature porte sur les octets exacts. La comparaison en temps constant évite une attaque par mesure du temps. J'ajouterais la vérification d'un horodatage signé (refuser les messages trop anciens, contre le rejeu) et l'idempotence côté traitement, puisque l'émetteur peut renvoyer le même événement."

═══════════════════════════════════════════
# 24. MODULES, DÉPENDANCES & CHAÎNE D'OUTILS
═══════════════════════════════════════════

### Vue d'ensemble

Depuis Go 1.11 (et par défaut depuis 1.16), les dépendances sont gérées par les **modules** : un fichier `go.mod` décrit le module et ses dépendances, un fichier `go.sum` en garantit l'intégrité. Le système de Go a des choix originaux qui reviennent en entretien : la **sélection de version minimale** (MVS) au lieu de "la plus récente compatible", le **versionnement sémantique dans le chemin d'import** pour les versions majeures (`/v2`), un **proxy** et une **base de sommes de contrôle** publics, et une chaîne d'outils capable de **compiler pour n'importe quelle plateforme** depuis n'importe quelle machine.

### Cours

**go.mod** :
```
module github.com/acme/commandes

go 1.26                         // version minimale du langage

toolchain go1.27.2              // chaîne d'outils préférée (optionnel)

require (
    github.com/jackc/pgx/v5 v5.7.4
    golang.org/x/sync v0.12.0
)

require golang.org/x/text v0.22.0 // indirect

replace github.com/acme/lib => ../lib    // développement local

tool golang.org/x/tools/cmd/stringer     // Go 1.24 : outils du projet
```
- `go mod init <chemin>`, `go get pkg@version` (ajouter ou mettre à jour ; `@latest`, `@v1.2.3`, `@commit`, `@none` pour retirer), `go mod tidy` (ajoute les dépendances manquantes, retire les inutiles, met à jour go.sum ; depuis Go 1.27, fusionne aussi les blocs `require` dupliqués), `go mod download`, `go mod why pkg` (pourquoi une dépendance est là), `go mod graph`, `go list -m all`, `go mod verify`.
- **Dépendances `// indirect`** : requises par vos dépendances, pas importées directement.
- **`replace`** : remplacer un module par un chemin local ou un fork ; ne s'applique que dans le module principal (ignoré quand votre module est utilisé comme dépendance).
- **`exclude`** et **`retract`** (un auteur peut retirer une version publiée par erreur).
- **Directive `tool`** (Go 1.24) : suivre les outils du projet (générateurs, linters) dans go.mod et les lancer avec `go tool stringer`, à la place de l'ancien fichier `tools.go`.
- **Directive `ignore`** (Go 1.25) : exclure des répertoires du module.

**go.sum** : empreintes des modules (contenu et go.mod) ; à **versionner**. Toute différence avec le contenu téléchargé fait échouer le build. Vérifiées aussi contre la **checksum database** publique `sum.golang.org`.

**Minimal Version Selection (MVS)** : pour chaque module, Go choisit la **version la plus élevée parmi les minimums exigés** par l'ensemble des go.mod du graphe, et **pas** la dernière version publiée. Les builds sont donc **reproductibles** sans fichier de verrouillage séparé : une nouvelle version d'une dépendance n'est jamais prise sans action explicite (`go get -u`). C'est différent de npm ou Maven.

**Versionnement sémantique et versions majeures** :
- versions `vMAJEUR.MINEUR.CORRECTIF`, tags Git `v1.4.2` ;
- `v0.x` : pas de garantie de compatibilité ;
- à partir de **v2**, le chemin du module **change** : `module github.com/acme/lib/v2`, import `github.com/acme/lib/v2/pkg`. Deux versions majeures peuvent donc coexister dans un même build. C'est la *semantic import versioning* : "si l'ancienne et la nouvelle API ont le même chemin, la nouvelle doit être compatible".
- **pseudo-versions** pour un commit sans tag : `v0.0.0-20260112153045-abcdef123456`.

**Proxy, privé et environnement** :
- `GOPROXY` (défaut `https://proxy.golang.org,direct`) : cache public immuable des modules ; les entreprises utilisent souvent un proxy interne (Athens, Artifactory, Nexus).
- `GOPRIVATE=github.com/acme/*` : modules privés, téléchargés directement depuis le VCS sans passer par le proxy public ni la checksum database (`GONOSUMDB`, `GONOPROXY` pour un réglage fin) ; l'authentification Git passe par `.netrc` ou la configuration Git.
- `GOFLAGS`, `GOCACHE` (cache de compilation), `GOMODCACHE` (modules téléchargés), `GOPATH` (encore utilisé pour le cache et `go install`), `CGO_ENABLED`, `GOOS`, `GOARCH`, `GOTOOLCHAIN`, `GOEXPERIMENT`. `go env` les affiche, `go env -w` les fixe.

**Chaîne d'outils (toolchain)** : depuis Go 1.21, si un module exige une version de Go plus récente que celle installée, la commande `go` peut **télécharger automatiquement** la bonne chaîne d'outils (`GOTOOLCHAIN=auto`, défaut) ; `GOTOOLCHAIN=local` l'interdit (utile en CI pour maîtriser la version).

**Vendoring** : `go mod vendor` copie les dépendances dans `vendor/` (builds hors ligne, audit) ; utilisé automatiquement si le répertoire existe et que la directive `go` est ≥ 1.14.

**Workspaces** (Go 1.18) : `go work init ./app ./lib` crée un `go.work` qui fait utiliser les versions **locales** de plusieurs modules ensemble ; pratique en développement, généralement **non versionné**.

**Compilation croisée** : `GOOS=linux GOARCH=arm64 go build` produit un binaire Linux ARM64 depuis un Mac ou un Windows, sans rien installer, **tant que cgo n'est pas utilisé**. `go tool dist list` liste les plateformes (Linux, macOS, Windows, BSD, wasm, wasip1, et de nombreuses architectures).

**Options de build** :
- `-o` (nom du binaire), `-trimpath` (supprime les chemins locaux : builds reproductibles), `-ldflags "-s -w"` (retire table des symboles et infos DWARF : binaire plus petit), `-ldflags "-X main.version=1.4.2"` (injecter une valeur dans une variable string), `-tags` (build tags), `-race`, `-cover`, `-pgo`, `-gcflags` (options du compilateur, comme `-m`).
- **Build constraints** : `//go:build linux && amd64` en tête de fichier, ou suffixes `_linux.go`, `_test.go`, `_windows_amd64.go`.
- **CGO_ENABLED=0** : binaire **statique** pur Go (indispensable pour `scratch`), mais les packages qui dépendent du C (certains pilotes, résolution DNS système) basculent sur leur implémentation Go.
- Taille des binaires : quelques Mo à quelques dizaines de Mo (runtime inclus) ; `upx` compresse encore, au prix du temps de démarrage.

**go install** : `go install pkg@version` compile et installe un outil dans `$GOBIN` (ou `$GOPATH/bin`), sans modifier le go.mod courant.

**go generate** : exécute les commandes des directives `//go:generate` ; ne fait pas partie du build, le code généré est versionné.

**Publier un module** : un dépôt public, un `go.mod` au bon chemin, un tag `vX.Y.Z` ; pkg.go.dev indexe la documentation automatiquement. Ne jamais modifier un tag publié (le proxy et la checksum database en gardent l'empreinte) ; publier une nouvelle version, ou utiliser `retract`.

### En situation

- **"Une mise à jour de dépendance a cassé le build sans qu'on y touche"** → impossible avec MVS sauf go get -u ou go.sum ignoré : chercher qui a modifié go.mod.
- **"checksum mismatch"** → tag republié ou contenu altéré ; ne pas contourner sans comprendre.
- **"Impossible de récupérer un module privé"** → GOPRIVATE et authentification Git.
- **"Comment passer ma bibliothèque en v2 ?"** → nouveau chemin de module avec /v2.
- **"Le binaire ne démarre pas dans l'image scratch"** → binaire dynamique (cgo) → CGO_ENABLED=0.
- **"Je dois livrer pour Linux ARM depuis mon Mac"** → GOOS/GOARCH.

### Questions techniques

**Q : Que contient go.mod ?** R : Le chemin du module, la version de Go, les dépendances requises et d'éventuelles directives replace, exclude, retract, tool, toolchain.

**Q : À quoi sert go.sum ?** R : À vérifier l'intégrité des modules téléchargés par leurs empreintes ; il se versionne.

**Q : Que fait go mod tidy ?** R : Ajoute les dépendances manquantes, supprime les inutiles et met à jour go.sum.

**Q : C'est quoi la Minimal Version Selection ?** R : Go choisit, pour chaque module, la plus haute des versions minimales exigées dans le graphe, pas la dernière publiée.

**Q : Pourquoi Go n'a-t-il pas de fichier de verrouillage séparé ?** R : MVS rend la sélection déterministe à partir des go.mod ; go.sum garantit l'intégrité.

**Q : Comment publier une v2 d'un module ?** R : En changeant le chemin du module en .../v2 et en taguant v2.x.y.

**Q : C'est quoi une pseudo-version ?** R : Une version générée pour un commit sans tag (v0.0.0-date-hash).

**Q : À quoi sert GOPROXY ?** R : À télécharger les modules via un proxy (proxy.golang.org par défaut) qui les met en cache de façon immuable.

**Q : À quoi sert GOPRIVATE ?** R : À télécharger certains modules directement, sans proxy public ni checksum database.

**Q : Que fait replace ?** R : Remplace un module par un chemin local ou un fork ; seulement dans le module principal.

**Q : Qu'apporte la directive tool (Go 1.24) ?** R : Déclarer les outils du projet dans go.mod et les lancer avec go tool, à la place de tools.go.

**Q : Que fait GOTOOLCHAIN=auto ?** R : Télécharge automatiquement la chaîne d'outils exigée par le module si elle est plus récente.

**Q : À quoi sert go work ?** R : Développer plusieurs modules locaux ensemble sans replace.

**Q : Comment compiler pour une autre plateforme ?** R : GOOS et GOARCH, par exemple GOOS=linux GOARCH=arm64 go build.

**Q : Pourquoi CGO_ENABLED=0 ?** R : Pour un binaire statique sans dépendance à la libc, et une compilation croisée simple.

**Q : Comment injecter une version dans le binaire ?** R : -ldflags "-X main.version=..." sur une variable string.

**Q : Que font -ldflags "-s -w" ?** R : Retirent la table des symboles et les informations de débogage pour réduire la taille.

**Q : À quoi sert -trimpath ?** R : Supprimer les chemins locaux du binaire, pour des builds reproductibles.

**Q : Comment écrire une contrainte de build ?** R : `//go:build linux && amd64` en tête de fichier, ou un suffixe _linux.go.

**Q : go get vs go install ?** R : go get modifie les dépendances du module ; go install compile et installe un outil.

**Q : Peut-on modifier un tag déjà publié ?** R : Non, l'empreinte est conservée par le proxy et la checksum database ; il faut publier une nouvelle version ou retract.

### Questions de situation

**Q : "Une dépendance transitive a une vulnérabilité, mais la bibliothèque qui l'utilise n'a pas encore publié de correctif."**
R : "Avec MVS, je peux exiger directement une version minimale corrigée de la dépendance transitive dans mon go.mod : `go get vuln/module@v1.2.4`. Comme Go prend le maximum des minimums, c'est cette version qui sera utilisée par tout le graphe, à condition qu'elle reste compatible (même version majeure).
Je vérifierais avec `govulncheck` que le code vulnérable est effectivement atteint et que la correction le résout, je lancerais les tests, puis je retirerais l'exigence explicite quand la bibliothèque intermédiaire aura publié sa mise à jour."

═══════════════════════════════════════════
# 25. QUALITÉ DU CODE : FORMATAGE, ANALYSE STATIQUE & REVUE
═══════════════════════════════════════════

### Vue d'ensemble

L'écosystème Go a une culture d'outillage très forte : le formatage n'est jamais débattu (`gofmt`), l'analyse statique de base est intégrée (`go vet`, lancé automatiquement en partie par `go test`), et un agrégateur de linters (**golangci-lint**) est présent dans presque tous les projets. Depuis Go 1.26, `go fix` applique automatiquement des **modernisations** du code.

En entretien, on attend de connaître ces outils, quelques linters importants et ce qu'ils détectent, et une approche raisonnée de la revue de code en Go.

### Cours

**Formatage** :
- `gofmt -l -w .` (ou `go fmt ./...`) : formatage canonique, non configurable.
- `goimports` : gofmt + ajout et tri automatiques des imports.
- `gofumpt` : variante plus stricte, souvent utilisée en plus.
- Le formatage se vérifie en CI (`gofmt -l` doit renvoyer une liste vide).

**go vet** (analyses intégrées) : `printf` (verbes incohérents), `copylocks` (copie de mutex), `lostcancel` (cancel non appelé), `loopclosure` (ancien piège de boucle), `structtag` (tags mal formés), `unreachable`, `nilfunc`, `shift`, `stdmethods` (méthodes standard mal signées), `httpresponse` (utilisation de la réponse avant la vérification d'erreur), `tests` (Go 1.24, erreurs dans les noms et signatures de tests), `waitgroup` (Go 1.25, `wg.Add` mal placé), `hostport` (Go 1.25, construction d'adresse incompatible IPv6), `stdversion` (utilisation d'API trop récentes pour la version déclarée, lancé par défaut par `go test` depuis Go 1.27). `go test` exécute automatiquement un sous-ensemble de vet.

**staticcheck** (Dominik Honnef) : l'analyseur de référence de la communauté : bugs probables (SA), simplifications (S), style (ST), code mort (U1000), API dépréciées.

**golangci-lint** : un exécutable qui lance des dizaines de linters en parallèle, configuré par `.golangci.yml`. Linters courants :
| Linter | Détecte |
|---|---|
| `govet`, `staticcheck` | bugs probables |
| `errcheck` | erreurs non vérifiées |
| `ineffassign` | affectations inutiles |
| `unused` | code mort |
| `gosec` | problèmes de sécurité |
| `revive` | style (successeur de golint, désormais archivé) |
| `bodyclose` | corps de réponse HTTP non fermé |
| `sqlclosecheck`, `rowserrcheck` | `Rows` non fermés, `rows.Err()` oublié |
| `noctx` | requêtes HTTP sans contexte |
| `contextcheck` | contexte non propagé |
| `errorlint` | comparaisons d'erreurs sans `errors.Is/As`, `%w` manquant |
| `wrapcheck` | erreurs externes non enveloppées |
| `gocritic` | nombreuses vérifications diverses |
| `gocognit`, `gocyclo`, `funlen` | complexité |
| `dupl` | duplication |
| `exhaustive` | switch non exhaustif sur une énumération |
| `nilaway` (Uber) | déréférencements nil possibles (analyse interprocédurale) |
| `copyloopvar`, `intrange` | modernisations liées à Go 1.22 |
| `sloglint` | usage cohérent de slog |
| `paralleltest`, `tparallel` | tests parallèles mal configurés |

Bonne pratique : un ensemble raisonnable activé dès le début, bloquant en CI sur le **nouveau** code (option `new-from-rev`), plutôt que tout activer d'un coup sur une base existante.

**go fix et les modernizers** (Go 1.26) : `go fix ./...` réécrit le code vers les idiomes récents (`min`/`max`, `slices.Contains`, `for range n`, `strings.Cut`, `wg.Go`, types atomiques en 1.27...) en garantissant de ne pas changer le comportement ; la directive `//go:fix inline` permet à une bibliothèque de faire migrer automatiquement ses utilisateurs vers une nouvelle fonction. Les mêmes analyses existent dans `gopls` (l'éditeur les propose en correction rapide).

**gopls** : le serveur de langage officiel (VS Code, GoLand utilise son propre moteur, Neovim, Emacs...) : complétion, navigation, renommage sûr, diagnostics vet et staticcheck intégrés, refactorings.

**Autres outils** : `govulncheck` (vulnérabilités), `deadcode` (fonctions jamais atteintes depuis main), `go mod why`, `benchstat`, `gotestsum` (sortie de tests lisible en CI), `goreleaser` (publication), `mockgen`/`mockery`, `sqlc`, `buf`.

**Revue de code en Go, ce qu'on regarde** (inspiré des *Go Code Review Comments*) :
- les erreurs sont-elles toutes traitées, enveloppées avec du contexte, loggées une seule fois ?
- le contexte est-il propagé, les ressources fermées (`defer Close`), les timeouts présents ?
- chaque goroutine a-t-elle une condition de fin ? Les accès partagés sont-ils protégés ?
- les interfaces sont-elles petites et définies côté consommateur ?
- les noms sont-ils courts, sans bégaiement, sans `Get` ?
- l'API exportée est-elle minimale et documentée ?
- les tests couvrent-ils les cas limites, sont-ils table-driven quand c'est pertinent ?
- pas d'abstraction prématurée, pas de dépendance ajoutée pour trois lignes.

### En situation

- **"Débat sur le style dans les revues"** → gofmt/gofumpt et golangci-lint automatisés, la revue se concentre sur la conception.
- **"Le linter remonte 2 000 problèmes sur l'existant"** → bloquer seulement le nouveau code, corriger progressivement.
- **"Moderniser une base de code ancienne"** → go fix.
- **"Des réponses HTTP jamais fermées en production"** → bodyclose.
- **"Des NPE en production"** → nilaway, revue des retours `nil, nil`.

### Questions techniques

**Q : gofmt est-il configurable ?** R : Non, c'est volontaire : un seul style pour tout l'écosystème.

**Q : gofmt vs goimports ?** R : goimports formate aussi et gère automatiquement les imports.

**Q : Que vérifie go vet ?** R : Des erreurs probables qui compilent : Printf incohérent, copie de verrous, cancel oublié, tags mal formés, etc.

**Q : go test lance-t-il go vet ?** R : Oui, un sous-ensemble d'analyses rapides et fiables.

**Q : C'est quoi staticcheck ?** R : L'analyseur statique de référence de la communauté (bugs, simplifications, code mort, dépréciations).

**Q : C'est quoi golangci-lint ?** R : Un agrégateur qui lance de nombreux linters en parallèle avec une configuration commune.

**Q : Que détecte errcheck ?** R : Les erreurs retournées mais non vérifiées.

**Q : Que détecte bodyclose ?** R : Les corps de réponse HTTP non fermés.

**Q : Que détecte nilaway ?** R : Les déréférencements de nil possibles, par analyse interprocédurale.

**Q : Que fait go fix depuis Go 1.26 ?** R : Il applique des modernizers qui réécrivent le code vers les idiomes récents sans changer son comportement.

**Q : À quoi sert //go:fix inline ?** R : Permettre à go fix de remplacer automatiquement les appels à une fonction dépréciée par sa nouvelle version.

**Q : C'est quoi gopls ?** R : Le serveur de langage officiel de Go pour les éditeurs.

**Q : golint est-il encore utilisé ?** R : Non, il est archivé ; revive l'a remplacé.

**Q : Comment introduire un linter sur une grande base existante ?** R : Le rendre bloquant seulement sur le nouveau code (new-from-rev) et corriger l'existant progressivement.

### Questions de situation

**Q : "Quelle configuration de qualité mets-tu en place sur un nouveau projet Go ?"**
R : "En local : gopls dans l'éditeur, gofumpt et goimports à l'enregistrement. En CI, sur chaque pull request : vérification du formatage, `go vet`, golangci-lint avec un ensemble ciblé (staticcheck, errcheck, errorlint, gosec, bodyclose, sqlclosecheck, noctx, revive, exhaustive, et nilaway si l'équipe l'accepte), `go test -race -shuffle=on ./...` avec couverture, et `govulncheck`.
Je garderais la configuration versionnée et modérée : un linter qui produit trop de faux positifs finit désactivé ou contourné par des commentaires `//nolint` partout. Et périodiquement, `go fix` après une montée de version de Go."

═══════════════════════════════════════════
# 26. DOCKER, KUBERNETES & GO DANS LE CLOUD
═══════════════════════════════════════════

### Vue d'ensemble

Go et les conteneurs vont très bien ensemble : un service Go se compile en **un binaire statique** qui tient dans une image de quelques mégaoctets (`scratch` ou distroless), démarre en millisecondes et consomme peu de mémoire. Go est aussi **le langage de Kubernetes** : le plan de contrôle, `kubectl`, les opérateurs, Helm, Docker, containerd, etcd, Prometheus, Istio (en partie), Terraform et la plupart des outils cloud-native sont écrits en Go. Un développeur Go dans le cloud doit donc savoir conteneuriser correctement un service, régler le runtime pour Kubernetes, et souvent écrire des **contrôleurs** ou des outils qui parlent à l'API Kubernetes.

### Cours

**Dockerfile multi-stage typique** :
```dockerfile
# syntax=docker/dockerfile:1
FROM golang:1.27 AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN --mount=type=cache,target=/go/pkg/mod go mod download      # couche de dépendances
COPY . .
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    CGO_ENABLED=0 GOOS=linux go build -trimpath -ldflags="-s -w" -o /out/app ./cmd/app

FROM gcr.io/distroless/static-debian12:nonroot
COPY --from=build /out/app /app
USER nonroot:nonroot
EXPOSE 8080
ENTRYPOINT ["/app"]
```
- Copier `go.mod`/`go.sum` d'abord pour profiter du **cache de couches** quand seules les sources changent ; caches BuildKit pour les modules et le cache de compilation.
- `CGO_ENABLED=0` : binaire **statique** qui tourne dans `scratch` ou distroless/static.
- **scratch** : image vide ; il faut y ajouter les **certificats CA** (`/etc/ssl/certs/ca-certificates.crt`) pour les appels HTTPS, les fichiers de **fuseaux horaires** si on utilise `time.LoadLocation` (ou importer `time/tzdata` qui les embarque dans le binaire), et créer un utilisateur non root.
- **distroless/static** : contient déjà certificats, tzdata et un utilisateur `nonroot` : souvent le meilleur choix.
- Images obtenues : 5 à 30 Mo, sans shell (surface d'attaque minimale ; pour déboguer : `kubectl debug` avec un conteneur éphémère).
- **ko** : construit et publie une image d'un service Go **sans Dockerfile** (rapide, reproductible, multi-architecture), très utilisé dans l'écosystème Kubernetes.
- Multi-architecture (amd64 + arm64) : compilation croisée native de Go, `docker buildx` ou ko.
- `ENTRYPOINT` en forme exec : le binaire est PID 1 et reçoit SIGTERM (à gérer avec `signal.NotifyContext`).

**Régler le runtime pour Kubernetes** :
- **GOMAXPROCS** : conscient des limites CPU du cgroup depuis Go 1.25 ; avant, `go.uber.org/automaxprocs`. Sans cela, un GOMAXPROCS égal au nombre de cœurs du nœud provoque du throttling.
- **GOMEMLIMIT** : environ 90 % de la limite mémoire du conteneur, pour que le GC accélère avant l'OOMKill (via `resourceFieldRef` de la Downward API, ou une bibliothèque comme `automemlimit`).
- **requests/limits** : un service Go a souvent besoin de peu de mémoire ; limiter le CPU avec précaution (le throttling dégrade la latence ; certaines équipes ne fixent qu'une request CPU).
- **Probes** : liveness simple, readiness qui reflète la capacité à servir, startup probe si l'initialisation est longue.
- **Arrêt propre** : à la réception de SIGTERM, passer la readiness à faux, attendre quelques secondes que le pod soit retiré des endpoints (ou un `preStop`), puis `Shutdown` du serveur avant la fin du `terminationGracePeriodSeconds`.

**Go et l'API Kubernetes** :
- **client-go** : la bibliothèque officielle utilisée par `kubectl` et les contrôleurs. Clientset typé, client dynamique, **informers** (cache local des ressources alimenté par un *watch*, qui évite de solliciter l'API server) et **workqueues** (file avec limitation de débit et retry).
- **controller-runtime** : une couche plus haut niveau, base de **Kubebuilder** et de l'**Operator SDK**.
- **Opérateur** : un contrôleur qui gère une ressource personnalisée (**CRD**) avec une **boucle de réconciliation** idempotente : lire l'état désiré (spec), observer l'état réel, agir pour les rapprocher, mettre à jour le `status`, et requeue en cas d'erreur.
```go
func (r *BaseReconciler) Reconcile(ctx context.Context, req ctrl.Request) (ctrl.Result, error) {
    var db v1.Base
    if err := r.Get(ctx, req.NamespacedName, &db); err != nil {
        return ctrl.Result{}, client.IgnoreNotFound(err)
    }
    // créer ou mettre à jour le StatefulSet, le Service, le Secret attendus...
    // mettre à jour db.Status
    return ctrl.Result{RequeueAfter: 5 * time.Minute}, nil
}
```
- Principes : réconciliation **idempotente** et **déclarative** (pas de "j'ai reçu l'événement de création, donc je crée"), *owner references* pour que les ressources enfants soient supprimées avec le parent, *finalizers* pour nettoyer des ressources externes avant la suppression, élection de leader pour n'avoir qu'un contrôleur actif, tests avec `envtest` (vrai API server et etcd locaux).

**Serverless et autres plateformes** : AWS Lambda a un runtime Go natif (`aws-lambda-go`, déployé comme binaire `bootstrap` sur le runtime `provided.al2023`), avec des démarrages à froid très courts ; Google Cloud Run, Azure Container Apps, Fly.io ; WebAssembly (`GOOS=js`, `GOOS=wasip1`, et TinyGo pour l'embarqué et le Wasm compact).

**SDK cloud** : AWS SDK for Go v2, Google Cloud Go, Azure SDK for Go ; Terraform (écrit en Go) et ses providers, eux aussi écrits en Go avec le *Terraform Plugin Framework*.

### En situation

- **"L'image fait 900 Mo"** → image golang utilisée à l'exécution → multi-stage vers distroless/scratch.
- **"x509: certificate signed by unknown authority dans scratch"** → certificats CA manquants.
- **"unknown time zone Europe/Paris"** → tzdata absent → `import _ "time/tzdata"` ou distroless.
- **"Le binaire ne démarre pas : no such file or directory"** → binaire lié dynamiquement (cgo) dans scratch → CGO_ENABLED=0.
- **"Latence et throttling en pod"** → GOMAXPROCS (Go < 1.25), limites CPU.
- **"Écrire un opérateur"** → Kubebuilder, controller-runtime, réconciliation idempotente.

### Questions techniques

**Q : Pourquoi les images Go peuvent-elles être si petites ?** R : Le binaire statique contient tout, runtime compris ; il tourne dans scratch ou distroless.

**Q : Quel rôle joue CGO_ENABLED=0 pour les conteneurs ?** R : Il produit un binaire statique sans dépendance à la libc, exécutable dans scratch.

**Q : Que manque-t-il dans une image scratch ?** R : Certificats CA, données de fuseaux horaires, utilisateurs, shell.

**Q : Comment embarquer les fuseaux horaires dans le binaire ?** R : `import _ "time/tzdata"`.

**Q : scratch vs distroless/static ?** R : Image vide vs image minimale avec certificats, tzdata et utilisateur nonroot.

**Q : C'est quoi ko ?** R : Un outil qui construit et publie des images de services Go sans Dockerfile.

**Q : Pourquoi copier go.mod et go.sum avant les sources dans le Dockerfile ?** R : Pour mettre en cache la couche de téléchargement des dépendances.

**Q : Comment déboguer un conteneur sans shell ?** R : kubectl debug avec un conteneur éphémère.

**Q : Comment régler GOMEMLIMIT en Kubernetes ?** R : À environ 90 % de la limite mémoire, via la Downward API ou une bibliothèque.

**Q : Pourquoi GOMAXPROCS pose-t-il problème en conteneur avant Go 1.25 ?** R : Il vaut le nombre de cœurs du nœud et provoque du throttling CPU.

**Q : C'est quoi client-go ?** R : La bibliothèque Go officielle pour parler à l'API Kubernetes.

**Q : C'est quoi un informer ?** R : Un cache local de ressources Kubernetes alimenté par un watch, avec des notifications d'événements.

**Q : C'est quoi une boucle de réconciliation ?** R : Comparer l'état désiré et l'état réel et agir pour les rapprocher, de façon idempotente.

**Q : À quoi sert un finalizer ?** R : À exécuter un nettoyage (ressources externes) avant la suppression effective d'un objet.

**Q : Kubebuilder vs controller-runtime ?** R : controller-runtime est la bibliothèque ; Kubebuilder est l'outil qui génère le projet, les CRD et le code au-dessus.

**Q : Comment tester un opérateur ?** R : Avec envtest, qui démarre un vrai API server et etcd locaux.

**Q : Comment déployer du Go sur AWS Lambda ?** R : Un binaire nommé bootstrap sur le runtime provided.al2023, avec aws-lambda-go.

### Questions de situation

**Q : "Conteneurise un service Go pour la production."**
R : "Un Dockerfile multi-stage : une étape de build avec l'image `golang` officielle, `go mod download` en couche séparée avec cache BuildKit, compilation `CGO_ENABLED=0` avec `-trimpath` et `-ldflags "-s -w"` et la version injectée par `-X`. Une étape finale `distroless/static:nonroot` qui contient seulement le binaire, déjà dotée des certificats et de tzdata, exécutée en non-root, système de fichiers en lecture seule côté Kubernetes.
Image multi-architecture via buildx ou ko, scannée et signée en CI. Côté runtime : Go 1.25+ pour GOMAXPROCS, GOMEMLIMIT réglé sur la limite mémoire, arrêt propre sur SIGTERM avec passage de la readiness à faux avant le `Shutdown`."

═══════════════════════════════════════════
# 27. CI/CD, BUILD & PUBLICATION
═══════════════════════════════════════════

### Vue d'ensemble

Un pipeline Go est simple et rapide : la compilation est rapide, les tests aussi, et les outils sont standardisés. Le défi est plutôt de bien exploiter les **caches** (modules et compilation), d'exécuter systématiquement les bonnes vérifications (`-race`, lint, vulnérabilités), et de **publier** proprement des binaires multi-plateformes et des images.

### Cours

**Étapes d'un pipeline Go type** :
1. récupérer la bonne version de Go (`actions/setup-go` avec `go-version-file: go.mod`, cache intégré des modules et du cache de build) ;
2. `go mod download` puis `go mod verify` ; vérifier que `go mod tidy` ne produit aucun changement (dépendances cohérentes) ;
3. vérifier le formatage et le code généré à jour (`go generate ./...` puis `git diff --exit-code`) ;
4. `go vet ./...` et `golangci-lint run` ;
5. `go test -race -shuffle=on -coverprofile=cover.out ./...` (avec `gotestsum` pour un rapport JUnit lisible) ;
6. tests d'intégration (testcontainers ou services de la CI) ;
7. `govulncheck ./...` ;
8. build des binaires (matrice GOOS/GOARCH) et/ou de l'image ;
9. scan de l'image, SBOM, signature ;
10. publication et déploiement (GitOps).

**Exemple GitHub Actions** :
```yaml
jobs:
  ci:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-go@v5
        with:
          go-version-file: go.mod
      - run: go mod tidy && git diff --exit-code
      - run: go vet ./...
      - uses: golangci/golangci-lint-action@v8
      - run: go test -race -shuffle=on -coverprofile=cover.out ./...
      - run: go run golang.org/x/vuln/cmd/govulncheck@latest ./...
```
(Épingler les versions des actions et outils en vrai projet.)

**Caches** : le cache des modules (`GOMODCACHE`) et le **cache de compilation** (`GOCACHE`, adressé par contenu) rendent les builds incrémentaux très rapides ; les conserver entre exécutions de CI. Les résultats de tests réussis sont aussi mis en cache par `go test` (désactiver avec `-count=1` si les tests dépendent de l'extérieur).

**GoReleaser** : l'outil de référence pour publier un projet Go : à partir d'un tag Git, il compile pour toutes les plateformes, crée les archives et sommes de contrôle, génère le changelog, publie une release GitHub/GitLab, construit et pousse les images Docker, les paquets (deb, rpm, Homebrew, Scoop), signe les artefacts (cosign) et génère les SBOM.

**Versionnement** : tags SemVer (`v1.4.2`), version injectée par `-ldflags -X` ou lue via `debug.ReadBuildInfo()` (qui contient automatiquement les informations VCS depuis Go 1.18 : commit, date, état modifié).

**Reproductibilité** : `-trimpath`, version de Go épinglée (directive toolchain, `GOTOOLCHAIN=local` en CI), go.sum versionné, `GOFLAGS=-mod=readonly` (défaut) pour interdire au build de modifier go.mod. Go vise des builds **reproductibles** octet par octet pour un même code et une même chaîne d'outils.

**Monorepos et builds sélectifs** : avec plusieurs services dans un module, ne tester et construire que ce qui est affecté par le changement (`go list -deps`, outils comme Bazel avec `rules_go`, ou des filtres par chemin dans la CI).

**Déploiement** : image publiée avec un tag immuable, mise à jour du dépôt GitOps (ArgoCD, Flux), déploiement progressif (canary) piloté par les métriques. Les binaires Go démarrent vite, ce qui rend les déploiements progressifs et les rollbacks rapides.

### En situation

- **"La CI met 15 minutes à compiler"** → caches GOMODCACHE et GOCACHE non conservés.
- **"go.mod modifié en local mais pas commité"** → vérifier `go mod tidy` et `git diff` en CI.
- **"Le code généré n'est pas à jour"** → `go generate` puis diff en CI.
- **"Publier une CLI pour Linux, macOS et Windows"** → GoReleaser.
- **"Quelle version tourne ?"** → ldflags ou ReadBuildInfo, exposés par un endpoint ou une commande `version`.

### Questions techniques

**Q : Quelles étapes minimales dans une CI Go ?** R : mod tidy vérifié, vet, golangci-lint, tests avec -race, govulncheck, build.

**Q : Comment accélérer les builds en CI ?** R : Conserver GOMODCACHE et GOCACHE entre exécutions.

**Q : Pourquoi vérifier que go mod tidy ne change rien ?** R : Pour s'assurer que go.mod et go.sum commités sont cohérents avec le code.

**Q : Comment vérifier que le code généré est à jour ?** R : Lancer go generate en CI puis vérifier qu'aucun fichier n'a changé.

**Q : À quoi sert GoReleaser ?** R : Publier un projet Go : binaires multi-plateformes, archives, checksums, changelog, images, paquets, signatures.

**Q : Comment injecter la version dans le binaire ?** R : -ldflags "-X main.version=..." ou lire debug.ReadBuildInfo.

**Q : Que contient debug.ReadBuildInfo ?** R : Le module, les dépendances et versions, et les informations VCS (commit, date).

**Q : Pourquoi GOTOOLCHAIN=local en CI ?** R : Pour éviter un téléchargement automatique d'une autre version de Go et maîtriser la version utilisée.

**Q : Que fait -mod=readonly ?** R : Interdit à la commande go de modifier go.mod pendant le build (comportement par défaut).

**Q : Les résultats de go test sont-ils mis en cache ?** R : Oui pour les tests réussis dont les entrées n'ont pas changé ; -count=1 désactive ce cache.

### Questions de situation

**Q : "Conçois le pipeline de publication d'une CLI Go open source."**
R : "Sur chaque pull request : tidy vérifié, vet, golangci-lint, tests `-race` sur une matrice de versions de Go supportées et d'OS (Linux, macOS, Windows), govulncheck.
Sur un tag `v*` : GoReleaser compile pour toutes les plateformes cibles avec `-trimpath` et la version injectée, produit les archives et le fichier de sommes de contrôle, signe avec cosign, génère le SBOM, publie la release GitHub avec le changelog dérivé des Conventional Commits, et met à jour un tap Homebrew et une image Docker multi-architecture. La commande `version` de la CLI affiche la version, le commit et la date issus du build."

═══════════════════════════════════════════
# 28. ÉCRIRE DES OUTILS EN LIGNE DE COMMANDE
═══════════════════════════════════════════

### Vue d'ensemble

Go est devenu un langage de choix pour les **CLI** : un seul binaire sans dépendance, démarrage instantané, compilation croisée triviale pour toutes les plateformes. `kubectl`, `docker`, `gh` (GitHub CLI), `terraform`, `hugo`, `helm` et des milliers d'outils internes sont écrits en Go. En entretien pour un poste plateforme ou DevOps, on peut te demander comment structurer une CLI, gérer ses arguments, sa configuration et ses codes de sortie.

### Cours

**Bibliothèque standard** : `os.Args`, `flag` (drapeaux typés, `flag.NewFlagSet` par sous-commande), `os.Exit(code)` (0 succès, non nul échec ; attention, n'exécute pas les defer), `os.Stdin/Stdout/Stderr`, `bufio.Scanner` pour lire l'entrée, `os/signal` pour Ctrl+C, `os/exec` pour lancer d'autres programmes, `text/tabwriter` pour aligner des colonnes.

**Bibliothèques courantes** :
- **cobra** (spf13, utilisée par kubectl, gh, hugo, helm) : commandes et sous-commandes, drapeaux persistants, aide et complétion shell générées ; souvent associée à **viper** pour la configuration.
- **urfave/cli** : alternative plus légère.
- **kong** : CLI déclarées par des structs annotées.
- Interfaces terminal : **Bubble Tea** (Charm, architecture Elm pour des TUI), Lip Gloss (styles), `fatih/color`, barres de progression.

**Bonnes pratiques** :
- respecter les conventions Unix : sortie utile sur **stdout**, messages et erreurs sur **stderr**, codes de sortie significatifs, lecture de stdin si aucun fichier n'est donné, composable dans un pipe ;
- un mode de sortie machine (`--output json`) en plus de la sortie lisible ;
- configuration par ordre de priorité : drapeaux > variables d'environnement > fichier de configuration (dans `os.UserConfigDir()`) > valeurs par défaut ;
- gérer Ctrl+C avec `signal.NotifyContext` et propager le contexte ;
- logique dans des packages testables, `main` réduit à l'appel de `run(args, stdin, stdout, stderr) error` ;
- tester avec des tests table-driven sur `run`, des golden files pour les sorties, ou `testscript` pour des scénarios en ligne de commande ;
- commande `version`, complétion shell, aide claire ;
- distribution : GoReleaser, Homebrew, `go install module@latest`.

### En situation

- **"La CLI affiche ses erreurs dans le pipe de sortie"** → erreurs sur stderr.
- **"Les scripts ne savent pas si la commande a échoué"** → codes de sortie non nuls.
- **"Comment tester ma CLI ?"** → `run()` avec stdin/stdout injectés, golden files, testscript.
- **"Ctrl+C laisse des fichiers temporaires"** → contexte annulé sur signal et nettoyage.

### Questions techniques

**Q : Pourquoi Go est-il populaire pour les CLI ?** R : Binaire unique sans dépendance, démarrage instantané, compilation croisée simple.

**Q : flag vs cobra ?** R : Drapeaux simples de la bibliothèque standard vs framework de commandes et sous-commandes avec aide et complétion.

**Q : Où écrire les erreurs d'une CLI ?** R : Sur stderr, avec un code de sortie non nul.

**Q : os.Exit exécute-t-il les defer ?** R : Non ; d'où le pattern run() qui retourne une erreur et un main qui appelle os.Exit à la fin.

**Q : Comment gérer Ctrl+C proprement ?** R : signal.NotifyContext et propagation du contexte.

**Q : Quel ordre de priorité pour la configuration ?** R : Drapeaux, puis variables d'environnement, puis fichier, puis valeurs par défaut.

**Q : C'est quoi Bubble Tea ?** R : Une bibliothèque pour créer des interfaces terminal interactives selon l'architecture Elm.

**Q : Comment tester une CLI ?** R : Appeler run() avec des flux injectés, comparer à des golden files, ou utiliser testscript.

### Questions de situation

**Q : "Écris le squelette d'une CLI testable."**
R :
```go
func main() {
    ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt)
    defer stop()
    if err := run(ctx, os.Args[1:], os.Stdin, os.Stdout, os.Stderr); err != nil {
        fmt.Fprintln(os.Stderr, "erreur:", err)
        stop()
        os.Exit(1)
    }
}

func run(ctx context.Context, args []string, in io.Reader, out, errOut io.Writer) error {
    fs := flag.NewFlagSet("outil", flag.ContinueOnError)
    fs.SetOutput(errOut)
    format := fs.String("format", "text", "format de sortie (text|json)")
    if err := fs.Parse(args); err != nil {
        return err
    }
    // ... logique, en écrivant dans out
    _ = format
    return nil
}
```
"Toute la logique est dans `run`, qui reçoit ses entrées et sorties : en test, je passe un `strings.Reader` et des `bytes.Buffer` et je vérifie la sortie et l'erreur. `main` ne fait que brancher le vrai monde et convertir l'erreur en code de sortie, après avoir exécuté les defer."

═══════════════════════════════════════════
# 29. MICROSERVICES, MESSAGING & RÉSILIENCE EN GO
═══════════════════════════════════════════

### Vue d'ensemble

Go est massivement utilisé pour les microservices et les systèmes distribués : il est léger, démarre vite, gère très bien la concurrence réseau, et son écosystème couvre les besoins classiques (messagerie, cache, résilience, observabilité). Les principes d'architecture distribuée sont les mêmes que dans tout langage (sagas, outbox, idempotence, cohérence à terme) ; ce chapitre se concentre sur **leur mise en œuvre en Go** et sur les bibliothèques à connaître.

### Cours

**Kafka en Go** : trois clients principaux.
| Client | Caractéristiques |
|---|---|
| **franz-go** (`twmb/franz-go`) | pur Go, très complet (transactions, groupes coopératifs, toutes les fonctionnalités récentes), performant ; souvent recommandé aujourd'hui |
| **confluent-kafka-go** | binding de librdkafka (cgo) : éprouvé, mais build cgo |
| **segmentio/kafka-go** | API simple, pur Go, moins complet |
| **IBM/sarama** | historique (ex-Shopify), très répandu dans le code existant |

Pratiques : commit des offsets **après** traitement (at-least-once) et consommateurs **idempotents** ; un traitement par partition pour préserver l'ordre par clé (ou un worker pool avec routage par clé) ; arrêt propre (finir les messages en cours, committer, quitter le groupe) ; gestion des messages toxiques via un topic de retry et une DLQ ; producteur idempotent et `acks=all` pour la durabilité.

**Autres systèmes de messagerie** : **NATS** (écrit en Go : très léger, pub/sub et request/reply, **JetStream** pour la persistance et le rejeu), RabbitMQ (`rabbitmq/amqp091-go`), Redis Streams, Google Pub/Sub, AWS SQS/SNS (SDK v2). **Watermill** est une bibliothèque Go qui unifie plusieurs brokers derrière une même API et fournit des middlewares (retry, poison queue, CQRS).

**Orchestration de workflows** : **Temporal** (fork de Cadence, écrit en Go) : des workflows durables écrits en code Go ordinaire, avec reprise automatique après crash, retries, timers longs et compensations ; une solution solide pour les sagas orchestrées.

**Pattern outbox en Go** : dans la même transaction SQL, écrire la donnée métier et l'événement dans une table `outbox` ; une goroutine (ou un service dédié) relit l'outbox et publie, puis marque comme envoyé (ou on utilise Debezium en CDC). Plusieurs instances : `SELECT ... FOR UPDATE SKIP LOCKED` pour se répartir les lignes.

**Résilience** :
- **timeouts** partout (contextes, clients HTTP, requêtes SQL) ;
- **retries** avec backoff exponentiel et jitter (`cenkalti/backoff`, `avast/retry-go`, `hashicorp/go-retryablehttp`), uniquement pour les opérations idempotentes et les erreurs temporaires ;
- **circuit breaker** : `sony/gobreaker` ;
- **rate limiting** : `golang.org/x/time/rate` (token bucket, par client ou global), et côté distribué avec Redis ;
- **bulkhead** : limiter la concurrence par dépendance (sémaphore, `errgroup.SetLimit`) ;
- **load shedding** : rejeter tôt (429/503) quand une file interne dépasse un seuil ;
- **singleflight** contre les rafales de requêtes identiques ;
- **dégradation gracieuse** : valeur en cache ou réponse partielle quand une dépendance optionnelle échoue.

**Cache** : en mémoire avec **ristretto** (Dgraph), **otter**, `hashicorp/golang-lru` (v2 générique), **bigcache**/**freecache** (pour de très gros caches sans pression sur le GC) ; distribué avec **go-redis** (v9) ou rueidis (avec cache côté client). Protection contre le *stampede* par `singleflight`.

**Communication** : REST (net/http, chi), gRPC/Connect (voir les chapitres dédiés), GraphQL (**gqlgen**, génération de code depuis le schéma), WebSocket, événements.

**Découverte et configuration** : en Kubernetes, DNS et Services suffisent ; hors Kubernetes, Consul (écrit en Go) ou etcd (écrit en Go, client `go.etcd.io/etcd/client/v3`, utilisable aussi pour l'élection de leader et les verrous distribués).

**Kits de microservices** : go-kit (historique, très structurant), Kratos (Bilibili), go-micro, Dapr (runtime sidecar avec SDK Go), Encore (framework et plateforme avec infrastructure générée). Beaucoup d'équipes préfèrent assembler la bibliothèque standard et quelques bibliothèques ciblées.

**Arrêt propre d'un consommateur** :
```go
func (c *Consumer) Run(ctx context.Context) error {
    for {
        fetches := c.client.PollFetches(ctx)        // franz-go : s'interrompt quand ctx est annulé
        if ctx.Err() != nil {
            return nil                               // arrêt demandé
        }
        var traitErr error
        fetches.EachRecord(func(r *kgo.Record) {
            if traitErr == nil {
                traitErr = c.traiter(ctx, r)         // idempotent
            }
        })
        if traitErr != nil {
            return traitErr                          // ou envoi en retry/DLQ
        }
        if err := c.client.CommitUncommittedOffsets(ctx); err != nil {
            return err
        }
    }
}
```

### En situation

- **"Un même message Kafka est traité deux fois"** → at-least-once normal → idempotence (table des messages traités, upsert).
- **"Quel client Kafka choisir ?"** → franz-go pour du pur Go complet, confluent-kafka-go si on accepte cgo.
- **"Une saga sur plusieurs services"** → chorégraphie par événements ou orchestration avec Temporal.
- **"Les événements sont perdus quand le service plante entre la base et Kafka"** → outbox.
- **"Un service aval en panne fait tomber le nôtre"** → timeouts, circuit breaker, bulkhead.

### Questions techniques

**Q : Quels clients Kafka en Go ?** R : franz-go, confluent-kafka-go (librdkafka, cgo), segmentio/kafka-go, IBM/sarama.

**Q : Pourquoi éviter confluent-kafka-go dans certains projets ?** R : Il dépend de cgo (librdkafka), ce qui complique les builds statiques et la compilation croisée.

**Q : C'est quoi NATS JetStream ?** R : La couche de persistance de NATS : flux durables, rejeu, consommateurs avec acquittement.

**Q : À quoi sert Watermill ?** R : À unifier plusieurs brokers derrière une même API en Go, avec des middlewares.

**Q : Qu'apporte Temporal ?** R : Des workflows durables écrits en Go, avec reprise après crash, retries, timers et compensations.

**Q : Comment implémenter l'outbox en Go ?** R : Écrire l'événement en base dans la même transaction, puis un relais qui publie et marque, avec SKIP LOCKED en multi-instance.

**Q : Quelle bibliothèque de circuit breaker en Go ?** R : sony/gobreaker.

**Q : Quelle bibliothèque de rate limiting ?** R : golang.org/x/time/rate (token bucket).

**Q : Quels caches en mémoire en Go ?** R : ristretto, otter, golang-lru v2, bigcache, freecache.

**Q : Pourquoi bigcache ou freecache ?** R : Pour de très gros caches sans multiplier les pointeurs que le GC doit scanner.

**Q : Quelle bibliothèque GraphQL en Go ?** R : gqlgen, qui génère le code depuis le schéma.

**Q : Quels outils distribués célèbres sont écrits en Go ?** R : Kubernetes, etcd, Consul, NATS, Temporal, Prometheus, Docker.

### Questions de situation

**Q : "Conçois un consommateur Kafka robuste en Go."**
R : "Avec franz-go dans un groupe de consommateurs coopératif, et des offsets committés **après** traitement. Le traitement est idempotent : l'identifiant de l'événement est inséré dans une table avec contrainte unique dans la même transaction que l'effet métier. Pour la parallélisation, un worker par partition (ou un pool où les messages sont routés par clé), ce qui préserve l'ordre par clé.
En cas d'échec : quelques retries avec backoff pour les erreurs temporaires, puis publication vers un topic de retry différé ou une DLQ avec le contexte de l'erreur, pour ne pas bloquer la partition. Arrêt propre sur SIGTERM : on arrête de poller, on termine les messages en cours, on committe, on quitte le groupe. Métriques : lag par partition, taux d'erreurs, durée de traitement."

═══════════════════════════════════════════
# 30. ALGORITHMES & STRUCTURES DE DONNÉES EN GO
═══════════════════════════════════════════

### Vue d'ensemble

Les principes algorithmiques sont universels (voir n'importe quel cours de complexité), mais leur mise en œuvre en Go a ses spécificités : pas de collections riches comme en Java, on construit avec des **slices** et des **maps**, on utilise `container/heap` pour les tas, les packages `slices` et `sort` pour trier et chercher, et depuis les generics on peut écrire des structures réutilisables. En live coding Go, l'évaluateur regarde aussi l'idiomatique : gestion des cas limites, valeurs zéro, retours multiples au lieu d'exceptions.

### Cours

**Complexités des structures Go** :
| Opération | slice | map | `container/list` | tas (`container/heap`) |
|---|---|---|---|---|
| accès par index | O(1) | | O(n) | |
| recherche par clé | O(n) (O(log n) si trié) | O(1) moyen | O(n) | |
| ajout en fin | O(1) amorti (`append`) | O(1) | O(1) | O(log n) |
| insertion/suppression au milieu | O(n) | O(1) | O(1) avec l'élément | O(log n) |
| min/max | O(n) | | | O(1) / extraction O(log n) |

**Pile et file avec des slices** :
```go
pile = append(pile, x)                 // empiler
x, pile = pile[len(pile)-1], pile[:len(pile)-1]   // dépiler

file = append(file, x)                 // enfiler
x, file = file[0], file[1:]            // défiler (le tableau sous-jacent n'est libéré qu'à la réallocation)
```
Pour une file à fort débit, un buffer circulaire ou `container/list`.

**Tris** : `slices.Sort` (pdqsort, **non stable**, O(n log n)), `slices.SortFunc(s, func(a, b T) int)`, `slices.SortStableFunc` (stable), `slices.BinarySearch` / `BinarySearchFunc` (renvoie l'index et un booléen de présence), `sort.Search` (recherche du premier index satisfaisant un prédicat), `slices.IsSorted`. Comparateur multicritère : `cmp.Or(cmp.Compare(a.X, b.X), cmp.Compare(a.Y, b.Y))`.

**Tas avec container/heap** : implémenter `heap.Interface` (`Len`, `Less`, `Swap`, `Push`, `Pop`) sur un slice, puis utiliser `heap.Init`, `heap.Push`, `heap.Pop`, `heap.Fix`. Sert aux files de priorité, au top-K, à Dijkstra, à la fusion de K listes triées.

**Ensembles** : `map[T]struct{}` ; intersection et union en O(n + m).

**Graphes** : listes d'adjacence `map[string][]string` ou `[][]int` ; BFS avec une file (slice), DFS récursif ou avec pile ; Dijkstra avec `container/heap` ; tri topologique (algorithme de Kahn) pour ordonner des dépendances.

**Récursion** : les piles de goroutines grandissent dynamiquement (jusqu'à 1 Go par défaut), donc une récursion profonde est moins risquée qu'en Java, mais une récursion infinie finit en `fatal error: stack overflow` (non récupérable). Pas d'optimisation de la récursion terminale.

**Entiers et débordement** : `int` sur 64 bits en pratique, débordement silencieux ; `math.MaxInt`, `math/bits` (comptage de bits, multiplication avec retenue), `math/big` pour les grands nombres. Milieu de recherche dichotomique : `lo + (hi-lo)/2`.

**Chaînes** : travailler en `[]rune` si l'entrée peut contenir de l'Unicode, en `[]byte` si l'on sait qu'elle est ASCII (plus rapide) ; `strings.Builder` pour construire.

**Concurrence algorithmique** : paralléliser un calcul indépendant par morceaux avec des goroutines et un `WaitGroup`, en écrivant dans des indices distincts d'un slice de résultats (pas de verrou nécessaire), ou `errgroup` avec une limite.

### En situation

- **"Il me faut une file de priorité"** → container/heap.
- **"Trier par plusieurs critères"** → slices.SortFunc + cmp.Or.
- **"Le tri doit conserver l'ordre des égaux"** → SortStableFunc.
- **"Vérifier l'appartenance rapidement"** → map[T]struct{}.
- **"Récursion qui plante sur une entrée profonde"** → version itérative avec pile explicite.

### Questions techniques

**Q : Comment implémenter une pile en Go ?** R : Avec un slice : append pour empiler, re-slicing pour dépiler.

**Q : Comment implémenter une file de priorité ?** R : Implémenter heap.Interface sur un slice et utiliser container/heap.

**Q : Quelles méthodes faut-il pour heap.Interface ?** R : Len, Less, Swap (sort.Interface) plus Push et Pop.

**Q : slices.Sort est-il stable ?** R : Non (pdqsort) ; SortStableFunc l'est.

**Q : Que renvoie slices.BinarySearch ?** R : L'index où l'élément est ou serait inséré, et un booléen de présence.

**Q : Comment trier par plusieurs critères ?** R : SortFunc avec cmp.Or(cmp.Compare(...), cmp.Compare(...)).

**Q : Comment représenter un graphe en Go ?** R : Listes d'adjacence en map ou en slice de slices.

**Q : Une récursion profonde est-elle risquée en Go ?** R : Moins qu'en Java (piles extensibles jusqu'à 1 Go), mais un stack overflow est fatal et non récupérable.

**Q : Comment éviter le débordement dans le calcul du milieu ?** R : lo + (hi-lo)/2.

**Q : []rune ou []byte pour manipuler une chaîne ?** R : []rune si Unicode possible, []byte pour de l'ASCII connu.

**Q : Complexité de la recherche dans une map Go ?** R : O(1) en moyenne.

### Questions de situation

**Q : "Implémente Dijkstra en Go."**
R : "Je représente le graphe par des listes d'adjacence `map[string][]Arc` et j'utilise `container/heap` comme file de priorité sur la distance. On extrait le nœud de distance minimale, on ignore les entrées périmées (distance plus grande que celle déjà connue), on relâche ses arcs et on pousse les voisins améliorés. Complexité O((V + E) log V), valable pour des poids positifs. En Go, je ferais attention à initialiser les distances avec `math.MaxInt` et à renvoyer un booléen si la destination est inatteignable plutôt qu'une valeur magique."

═══════════════════════════════════════════
# 31. EXERCICES DE LIVE CODING CORRIGÉS EN GO
═══════════════════════════════════════════

### Vue d'ensemble

Les exercices de live coding d'un entretien Go reprennent les classiques (chaînes, tableaux, maps, listes chaînées) mais ajoutent presque toujours **une question de concurrence** : paralléliser des appels, écrire un worker pool, un pipeline, un compteur thread-safe, un cache. L'évaluateur regarde la justesse, la complexité, mais aussi l'**idiomatique** : retours multiples (`valeur, ok`), erreurs explicites, valeurs zéro, pas de fuite de goroutine.

Toutes les solutions ci-dessous ont été compilées et exécutées (avec `go vet` et le détecteur de courses `-race`). Elles utilisent des fonctionnalités de Go 1.21+ (`min`, `max`) et 1.22+ (`for range n`, variables de boucle par itération).

### Cours

**Réflexes** : HashMap → `map[K]V` ; ensemble → `map[T]struct{}` ou `map[T]bool` ; pile/file → slice ; tas → `container/heap` ; Unicode → `[]rune` ; concurrence → `sync.WaitGroup`, `errgroup`, channels avec `ctx.Done()`, mutex pour un état partagé ; toujours annoncer les cas limites (nil, vide, doublons, Unicode).

**1. FizzBuzz**
```go
func FizzBuzz(n int) []string {
    out := make([]string, 0, n)
    for i := 1; i <= n; i++ {
        switch {
        case i%15 == 0:
            out = append(out, "FizzBuzz")
        case i%3 == 0:
            out = append(out, "Fizz")
        case i%5 == 0:
            out = append(out, "Buzz")
        default:
            out = append(out, strconv.Itoa(i))
        }
    }
    return out
}
```
`switch` sans expression comme suite de conditions, slice préalloué, `strconv.Itoa` plutôt que `fmt.Sprint`.

**2. Inverser une chaîne (Unicode)**
```go
func Inverser(s string) string {
    r := []rune(s)
    for i, j := 0, len(r)-1; i < j; i, j = i+1, j-1 {
        r[i], r[j] = r[j], r[i]
    }
    return string(r)
}
```
Inverser les **octets** casserait les caractères multi-octets : `Inverser("héllo")` donne `olléh` grâce aux runes.

**3. Palindrome (lettres et chiffres, insensible à la casse)**
```go
func EstPalindrome(s string) bool {
    var r []rune
    for _, c := range s {
        if unicode.IsLetter(c) || unicode.IsDigit(c) {
            r = append(r, unicode.ToLower(c))
        }
    }
    for i, j := 0, len(r)-1; i < j; i, j = i+1, j-1 {
        if r[i] != r[j] {
            return false
        }
    }
    return true
}
```
`"Esope reste ici et se repose"` → true. Les accents ne sont pas normalisés (É ≠ e) : pour cela, `golang.org/x/text/unicode/norm` et suppression des diacritiques.

**4. Anagrammes**
```go
func SontAnagrammes(a, b string) bool {
    if len(a) != len(b) {
        return false
    }
    compte := make(map[rune]int)
    for _, c := range a {
        compte[c]++
    }
    for _, c := range b {
        compte[c]--
        if compte[c] < 0 {
            return false
        }
    }
    return true
}
```
La lecture d'une clé absente renvoie 0, ce qui simplifie le comptage.

**5. Premier caractère non répété**
```go
func PremierNonRepete(s string) (rune, bool) {
    compte := make(map[rune]int)
    for _, c := range s {
        compte[c]++
    }
    for _, c := range s {          // deuxième passe dans l'ordre de la chaîne
        if compte[c] == 1 {
            return c, true
        }
    }
    return 0, false
}
```
Retour `(valeur, ok)` idiomatique ; la map n'ayant pas d'ordre, on reparcourt la chaîne. `"aabbcdc"` → `d`.

**6. Compter les mots**
```go
func CompterMots(texte string) map[string]int {
    compte := make(map[string]int)
    mots := strings.FieldsFunc(strings.ToLower(texte), func(r rune) bool {
        return !unicode.IsLetter(r) && !unicode.IsDigit(r)
    })
    for _, m := range mots {
        compte[m]++
    }
    return compte
}
```
Pour l'afficher trié : `for _, k := range slices.Sorted(maps.Keys(compte))` (Go 1.23+).

**7. Fibonacci et factorielle**
```go
func Fibonacci(n int) int {
    a, b := 0, 1
    for range n {
        a, b = b, a+b              // affectation parallèle
    }
    return a
}

func Factorielle(n int64) *big.Int {
    r := big.NewInt(1)
    for i := int64(2); i <= n; i++ {
        r.Mul(r, big.NewInt(i))
    }
    return r
}
```
`Fibonacci(90)` tient encore dans un int64 ; au-delà, `math/big`. 25! dépasse largement int64.

**8. Crible d'Ératosthène**
```go
func Crible(max int) []int {
    compose := make([]bool, max+1)
    var premiers []int
    for i := 2; i <= max; i++ {
        if compose[i] {
            continue
        }
        premiers = append(premiers, i)
        for j := i * i; j <= max; j += i {
            compose[j] = true
        }
    }
    return premiers
}
```

**9. Two Sum**
```go
func TwoSum(t []int, cible int) (int, int, bool) {
    vus := make(map[int]int)
    for i, v := range t {
        if j, ok := vus[cible-v]; ok {
            return j, i, true
        }
        vus[v] = i
    }
    return 0, 0, false
}
```
O(n) en temps et en mémoire.

**10. Doublons (générique)**
```go
func Doublons[T comparable](xs []T) []T {
    vus := make(map[T]bool)        // false : vu une fois ; true : déjà signalé
    var out []T
    for _, x := range xs {
        if deja, ok := vus[x]; ok {
            if !deja {
                out = append(out, x)
                vus[x] = true
            }
            continue
        }
        vus[x] = false
    }
    return out
}
```
`[1 2 3 2 4 1 2]` → `[2 1]`, chaque doublon une seule fois, dans l'ordre de sa deuxième apparition.

**11. Recherche dichotomique**
```go
func RechercheDichotomique(t []int, x int) int {
    lo, hi := 0, len(t)-1
    for lo <= hi {
        mid := lo + (hi-lo)/2
        switch {
        case t[mid] == x:
            return mid
        case t[mid] < x:
            lo = mid + 1
        default:
            hi = mid - 1
        }
    }
    return -1
}
```
En production : `slices.BinarySearch(t, x)`.

**12. Parenthèses équilibrées**
```go
func BienParenthese(s string) bool {
    paires := map[rune]rune{')': '(', ']': '[', '}': '{'}
    var pile []rune
    for _, c := range s {
        switch c {
        case '(', '[', '{':
            pile = append(pile, c)
        case ')', ']', '}':
            if len(pile) == 0 || pile[len(pile)-1] != paires[c] {
                return false
            }
            pile = pile[:len(pile)-1]
        }
    }
    return len(pile) == 0
}
```
`"{[()()]}"` → true, `"([)]"` → false, `"(("` → false.

**13. Liste chaînée : inversion et détection de cycle**
```go
type Noeud struct {
    Val     int
    Suivant *Noeud
}

func InverserListe(tete *Noeud) *Noeud {
    var prec *Noeud
    for cour := tete; cour != nil; {
        suiv := cour.Suivant
        cour.Suivant = prec
        prec, cour = cour, suiv
    }
    return prec
}

func ACycle(tete *Noeud) bool {          // lièvre et tortue (Floyd)
    lent, rapide := tete, tete
    for rapide != nil && rapide.Suivant != nil {
        lent, rapide = lent.Suivant, rapide.Suivant.Suivant
        if lent == rapide {
            return true
        }
    }
    return false
}
```

**14. Fusionner deux slices triés**
```go
func Fusionner(a, b []int) []int {
    out := make([]int, 0, len(a)+len(b))
    i, j := 0, 0
    for i < len(a) && j < len(b) {
        if a[i] <= b[j] {
            out = append(out, a[i])
            i++
        } else {
            out = append(out, b[j])
            j++
        }
    }
    out = append(out, a[i:]...)
    return append(out, b[j:]...)
}
```

**15. Sous-tableau de somme maximale (Kadane)**
```go
func SommeMax(t []int) int {
    meilleur, courant := t[0], t[0]
    for _, v := range t[1:] {
        courant = max(v, courant+v)      // max intégré, Go 1.21
        meilleur = max(meilleur, courant)
    }
    return meilleur
}
```
`[-2 1 -3 4 -1 2 1 -5 4]` → 6. Préciser le cas du slice vide (ici panic : à documenter ou retourner `(int, bool)`).

**16. Plus longue sous-chaîne sans répétition (fenêtre glissante)**
```go
func PlusLongueSansRepetition(s string) int {
    derniere := make(map[rune]int)
    debut, best := 0, 0
    for i, c := range []rune(s) {
        if p, ok := derniere[c]; ok && p >= debut {
            debut = p + 1
        }
        derniere[c] = i
        best = max(best, i-debut+1)
    }
    return best
}
```
`"abcabcbb"` → 3, `"pwwkew"` → 3.

**17. Top K éléments les plus fréquents (container/heap)**
```go
type paire struct{ val, freq int }
type tasMin []paire

func (h tasMin) Len() int           { return len(h) }
func (h tasMin) Less(i, j int) bool { return h[i].freq < h[j].freq }
func (h tasMin) Swap(i, j int)      { h[i], h[j] = h[j], h[i] }
func (h *tasMin) Push(x any)        { *h = append(*h, x.(paire)) }
func (h *tasMin) Pop() any {
    old := *h
    x := old[len(old)-1]
    *h = old[:len(old)-1]
    return x
}

func TopK(t []int, k int) []int {
    freq := make(map[int]int)
    for _, v := range t {
        freq[v]++
    }
    h := &tasMin{}
    for v, f := range freq {
        heap.Push(h, paire{v, f})
        if h.Len() > k {
            heap.Pop(h)                  // retire le moins fréquent
        }
    }
    out := make([]int, h.Len())
    for i := len(out) - 1; i >= 0; i-- {
        out[i] = heap.Pop(h).(paire).val
    }
    return out
}
```
O(n log k). `[1 1 1 2 2 3]`, k=2 → `[1 2]`.

**18. Cache LRU générique (map + liste doublement chaînée)**
```go
type entree[K comparable, V any] struct {
    cle        K
    val        V
    prec, suiv *entree[K, V]
}

type Cache[K comparable, V any] struct {
    capacite    int
    index       map[K]*entree[K, V]
    tete, queue *entree[K, V]        // tête = plus récent
}

func NewCache[K comparable, V any](capacite int) *Cache[K, V] {
    return &Cache[K, V]{capacite: capacite, index: make(map[K]*entree[K, V])}
}

func (c *Cache[K, V]) detacher(e *entree[K, V]) {
    if e.prec != nil { e.prec.suiv = e.suiv } else { c.tete = e.suiv }
    if e.suiv != nil { e.suiv.prec = e.prec } else { c.queue = e.prec }
    e.prec, e.suiv = nil, nil
}

func (c *Cache[K, V]) enTete(e *entree[K, V]) {
    e.suiv = c.tete
    if c.tete != nil { c.tete.prec = e }
    c.tete = e
    if c.queue == nil { c.queue = e }
}

func (c *Cache[K, V]) Get(k K) (V, bool) {
    e, ok := c.index[k]
    if !ok {
        var zero V
        return zero, false
    }
    c.detacher(e)
    c.enTete(e)
    return e.val, true
}

func (c *Cache[K, V]) Put(k K, v V) {
    if e, ok := c.index[k]; ok {
        e.val = v
        c.detacher(e)
        c.enTete(e)
        return
    }
    if len(c.index) == c.capacite {
        vieux := c.queue
        c.detacher(vieux)
        delete(c.index, vieux.cle)
    }
    e := &entree[K, V]{cle: k, val: v}
    c.index[k] = e
    c.enTete(e)
}
```
Toutes les opérations en O(1). Non thread-safe : ajouter un `sync.Mutex` (et non un RWMutex, car `Get` modifie l'ordre). On peut aussi s'appuyer sur `container/list`.

**19. Traiter des éléments en parallèle avec une concurrence bornée**
```go
func TraiterEnParallele(ctx context.Context, ids []int, n int,
    f func(context.Context, int) (int, error)) ([]int, error) {

    res := make([]int, len(ids))
    sem := make(chan struct{}, n)
    var wg sync.WaitGroup
    var mu sync.Mutex
    var premiere error
    for i, id := range ids {
        wg.Add(1)
        go func() {
            defer wg.Done()
            select {
            case sem <- struct{}{}:           // acquiert une place
            case <-ctx.Done():
                return
            }
            defer func() { <-sem }()
            v, err := f(ctx, id)
            if err != nil {
                mu.Lock()
                if premiere == nil {
                    premiere = err
                }
                mu.Unlock()
                return
            }
            res[i] = v                         // indice distinct : pas de course
        }()
    }
    wg.Wait()
    return res, premiere
}
```
Version production en trois lignes avec `errgroup.WithContext` et `g.SetLimit(n)`, qui annule aussi les autres tâches au premier échec. À mentionner dans l'entretien.

**20. Pipeline générateur → carré, annulable**
```go
func Pipeline(ctx context.Context, nums []int) <-chan int {
    gen := func() <-chan int {
        out := make(chan int)
        go func() {
            defer close(out)
            for _, n := range nums {
                select {
                case out <- n:
                case <-ctx.Done():
                    return
                }
            }
        }()
        return out
    }
    carre := func(in <-chan int) <-chan int {
        out := make(chan int)
        go func() {
            defer close(out)
            for n := range in {
                select {
                case out <- n * n:
                case <-ctx.Done():
                    return
                }
            }
        }()
        return out
    }
    return carre(gen())
}
```
Chaque étape ferme sa sortie et écoute le contexte : si le consommateur abandonne et annule, aucune goroutine ne fuit. Somme de `[1 2 3]` au carré : 14.

**21. Compteur thread-safe dont la valeur zéro est utilisable**
```go
type Compteur struct {
    mu sync.Mutex
    m  map[string]int
}

func (c *Compteur) Inc(k string) {
    c.mu.Lock()
    defer c.mu.Unlock()
    if c.m == nil {
        c.m = make(map[string]int)     // initialisation paresseuse
    }
    c.m[k]++
}
```
`var cpt Compteur` fonctionne sans constructeur ; 100 goroutines → 100, sans data race signalée par `-race`.

**22. Regroupements et tris sur une liste de structs**
```go
type Employe struct {
    Nom, Dept string
    Salaire   float64
}

// regrouper par département
parDept := map[string][]Employe{}
for _, e := range emps {
    parDept[e.Dept] = append(parDept[e.Dept], e)
}

// trier par département puis salaire décroissant
slices.SortFunc(emps, func(a, b Employe) int {
    return cmp.Or(cmp.Compare(a.Dept, b.Dept), cmp.Compare(b.Salaire, a.Salaire))
})
```
Go n'a pas de "Streams" : on écrit des boucles explicites, ou des fonctions génériques (`GroupBy`, `Map`) quand elles sont réutilisées.

### En situation

- **"Écris-le en Go idiomatique"** → retours `(valeur, ok)` ou `(valeur, error)`, slices préalloués, pas d'exceptions.
- **"Et maintenant en parallèle"** → WaitGroup ou errgroup, résultats par indice, contexte, pas de fuite.
- **"Ton code est-il sûr en concurrence ?"** → `go test -race`, mutex sur l'état partagé.
- **"Que se passe-t-il avec une entrée vide ?"** → toujours annoncer le comportement.

### Questions techniques

**Q : Pourquoi convertir en []rune pour inverser une chaîne ?** R : Pour ne pas casser les caractères Unicode multi-octets.

**Q : Comment renvoyer "pas trouvé" en Go idiomatique ?** R : Un second résultat booléen (valeur, ok) ou une erreur sentinelle.

**Q : Pourquoi le LRU utilise-t-il un Mutex et pas un RWMutex ?** R : Get modifie l'ordre d'usage, c'est une écriture.

**Q : Comment éviter la course de données en collectant des résultats parallèles ?** R : Écrire chaque résultat à un indice distinct d'un slice préalloué, ou protéger par un mutex.

**Q : Comment s'assurer qu'un pipeline ne fuit pas ?** R : Chaque étape ferme sa sortie et sélectionne sur ctx.Done() à chaque envoi.

**Q : Comment paralléliser avec limite et arrêt au premier échec ?** R : errgroup.WithContext et SetLimit.

### Questions de situation

**Q : "On te demande d'écrire un rate limiter simple."**
R : "En production j'utiliserais `golang.org/x/time/rate` (token bucket) : `lim := rate.NewLimiter(rate.Limit(10), 20)` puis `lim.Wait(ctx)` ou `lim.Allow()`. Si on me demande de l'écrire, un token bucket avec une capacité, un débit de remplissage, un nombre de jetons et l'instant du dernier remplissage, le tout sous mutex : à chaque demande, je recalcule les jetons ajoutés depuis le dernier passage (bornés par la capacité), et j'accepte si au moins un jeton est disponible. L'horloge est injectée pour pouvoir le tester. Pour plusieurs instances, l'état passe dans Redis avec une opération atomique (script Lua)."

═══════════════════════════════════════════
# 32. GO FACE AUX AUTRES LANGAGES
═══════════════════════════════════════════

### Vue d'ensemble

Une question revient presque toujours dans un entretien Go : "Pourquoi Go ?" ou "Quelle différence avec Java / Python / Rust ?". Elle vérifie que tu comprends les **compromis** du langage et que tu sais choisir l'outil adapté, plutôt que de défendre Go par principe. Une bonne réponse cite des forces concrètes et des faiblesses réelles.

### Cours

**Go vs Java** :
| Aspect | Go | Java |
|---|---|---|
| Exécution | binaire natif statique | bytecode sur JVM (JIT), natif possible avec GraalVM |
| Démarrage, mémoire | millisecondes, faible empreinte | plus lent et plus gourmand (amélioré par AOT, CRaC, natif) |
| Performances de pointe | très bonnes | souvent supérieures sur des traitements longs grâce au JIT |
| Concurrence | goroutines et channels natifs | threads, virtual threads (Java 21), CompletableFuture, réactif |
| Langage | petit, explicite, peu d'abstractions | riche : héritage, annotations, generics complets |
| Erreurs | valeurs retournées | exceptions |
| Écosystème | infrastructure, cloud-native, réseau, CLI | entreprise, frameworks complets (Spring), big data |
| GC | concurrent, faible latence, peu réglable | plusieurs GC très réglables (G1, ZGC) |
| Build | `go build`, rapide, intégré | Maven/Gradle |

**Go vs Python** : Go est compilé, typé statiquement, bien plus rapide et sans GIL (vraie concurrence multi-cœur) ; Python est plus rapide à écrire pour des scripts, l'analyse de données et le machine learning, avec un écosystème scientifique sans équivalent. Usage fréquent : Python pour les données et le ML, Go pour les services et l'infrastructure qui les servent.

**Go vs Rust** : Rust offre la sûreté mémoire **sans GC** (ownership, borrow checker), des performances et une latence prévisibles, et un système de types très expressif, au prix d'une courbe d'apprentissage et d'une compilation plus lentes. Go est beaucoup plus simple et productif, avec un GC. Rust pour les systèmes très contraints (moteurs, embarqué, composants critiques en latence) ; Go pour les services réseau et les outils où la productivité de l'équipe prime.

**Go vs Node.js** : les deux excellent sur l'I/O concurrente ; Go utilise tous les cœurs nativement, est typé et compilé ; Node partage le langage avec le frontend et a un écosystème npm énorme.

**Go vs C/C++** : Go est sûr en mémoire, compile beaucoup plus vite, a une bibliothèque standard réseau complète ; C/C++ restent indispensables pour le très bas niveau et les contraintes temps réel strictes.

**Les forces de Go à citer** : simplicité et lisibilité (une équipe s'y forme vite), compilation rapide, binaire unique facile à déployer, concurrence intégrée, bibliothèque standard excellente pour le réseau, outillage unifié (fmt, vet, test, pprof, race), compatibilité ascendante garantie, très adapté aux conteneurs.

**Ses limites à reconnaître** : verbosité de la gestion d'erreurs, système de types moins expressif (pas de types somme ni d'énumérations natives, generics plus limités), pas de surcharge, écosystème plus mince pour l'applicatif d'entreprise lourd et le calcul scientifique, GC (même s'il est très efficace) qui exclut certains usages temps réel.

### En situation

- **"Pourquoi Go ?"** → forces concrètes liées au besoin (service réseau, conteneurs, équipe).
- **"Qu'est-ce qui te manque en Go ?"** → réponse honnête : types somme, verbosité des erreurs.
- **"Réécrire notre service Java en Go ?"** → seulement si un problème concret le justifie.

### Questions techniques

**Q : Go a-t-il des exceptions ?** R : Non, des valeurs d'erreur et, pour les bugs, panic/recover.

**Q : Go a-t-il de l'héritage ?** R : Non, de la composition par embedding et des interfaces implicites.

**Q : Go a-t-il un GC ?** R : Oui, concurrent et à faible latence.

**Q : Go vs Rust en une phrase ?** R : Go privilégie la simplicité avec un GC ; Rust la sûreté mémoire sans GC et le contrôle fin, au prix de la complexité.

**Q : Go a-t-il un GIL comme Python ?** R : Non, les goroutines s'exécutent en parallèle sur tous les cœurs.

**Q : Go a-t-il des énumérations natives ?** R : Non, on utilise un type nommé et iota.

**Q : Go a-t-il des types somme ?** R : Non ; on les simule avec des interfaces fermées (méthode non exportée) et un type switch.

### Questions de situation

**Q : "Ton entreprise envisage de réécrire un monolithe Java en Go. Ton avis ?"**
R : "Je demanderais d'abord quel problème on veut résoudre. Si ce sont la mémoire et le démarrage en conteneur, Java a aujourd'hui des réponses (AOT, natif, virtual threads) bien moins coûteuses qu'une réécriture. Si c'est la complexité du code, une réécriture dans un autre langage ne résout rien : c'est l'architecture qu'il faut revoir.
Go devient pertinent pour de **nouveaux** services aux besoins précis (proxy, passerelle, traitement réseau à forte concurrence, outils d'infrastructure, CLI) ou si l'équipe a déjà la compétence. Je proposerais d'extraire progressivement un ou deux services bien délimités en Go et de mesurer, plutôt qu'une réécriture complète, qui est l'un des projets les plus risqués qui soient."

═══════════════════════════════════════════
# 33. QUESTIONS COMPORTEMENTALES & PRÉSENTER SON EXPÉRIENCE
═══════════════════════════════════════════

### Vue d'ensemble

Un entretien technique n'est jamais purement technique. L'évaluateur cherche aussi à savoir comment tu travailles : comment tu réagis à un incident, à un désaccord, à un échec, comment tu apprends, si tu sais expliquer ce que tu as fait. Ces questions se préparent autant que les questions de Go, avec **tes propres exemples**.

Ce chapitre ne contient volontairement aucune expérience personnelle : il donne les trames de réponse. À toi de préparer, pour chaque trame, un ou deux exemples réels tirés de tes projets, stages, alternance ou projets personnels.

### Cours

**La méthode STAR** pour toute question qui commence par "parle-moi d'une fois où..." :
- **Situation** : le contexte en deux phrases (projet, équipe, enjeu).
- **Tâche** : ce qui t'incombait à toi.
- **Action** : ce que **tu** as fait, concrètement (le "je", pas le "on"), avec les choix et leurs raisons.
- **Résultat** : ce qui en est sorti, chiffré si possible, et ce que tu en as retenu.

**Se présenter en deux minutes** : présent (rôle, stack, ce que tu fais aujourd'hui) → passé (le parcours qui y mène) → futur (ce que tu cherches et pourquoi ce poste y correspond).

**Présenter un projet Go** : contexte métier en une phrase, architecture en une phrase, **ton périmètre exact**, la difficulté principale (souvent de la concurrence, de la performance ou de l'intégration), comment tu l'as résolue et mesurée, le résultat, une limite assumée. Prépare-toi aux relances : "pourquoi Go ici ?", "comment as-tu testé la concurrence ?", "comment as-tu mesuré ?", "qu'aurais-tu fait autrement ?".

**Les règles d'or** : précision sur ton périmètre, honnêteté (un résultat nuancé vaut mieux qu'un succès survendu), concision, jamais de critique d'un ancien employeur, et le "je ne sais pas" suivi de "voici comment je chercherais".

**Questions à préparer** :
| Question | Ce qui est évalué | Trame |
|---|---|---|
| Ta plus grande difficulté technique | démarche de résolution | STAR, insister sur le diagnostic (profils, traces, tests) |
| Un échec ou une erreur | lucidité, apprentissage | reconnaître sa part, la correction, ce qui a changé ensuite |
| Un désaccord technique | communication | écouter, argumenter sur des faits (benchmark, prototype), trouver une issue |
| Un désaccord avec ton lead | courage et loyauté | avis argumenté, puis application de la décision (*disagree and commit*) |
| Un incident en production | sang-froid, priorités | rétablir, communiquer, analyser, post-mortem sans blâme |
| Apprendre vite une techno | autonomie | documentation officielle, prototype, aide ciblée |
| Une tâche floue | proactivité | questions, reformulation écrite, première version rapide |
| Un retour négatif reçu | ouverture | l'accepter, l'appliquer, exemple de progression |
| Une réalisation dont tu es fier | motivation, impact | apport clair et mesurable |
| Travailler sur du legacy | pragmatisme | comprendre avant de changer, tests de caractérisation, petits pas |

**Questions de motivation** : pourquoi ce poste ou cette entreprise (un élément concret relié à ton projet), pourquoi Go (forces concrètes, pas une mode), pourquoi partir (vers l'avant, pas contre), où dans 3 à 5 ans (une direction crédible), qualités et défauts (une qualité prouvée, un vrai défaut maîtrisé), prétentions (fourchette du marché argumentée).

**Spécificités d'un entretien en ESN** : mobilité, adaptabilité à différents clients et stacks, autonomie chez le client, intercontrat ; souvent deux entretiens (ESN puis client final).

**Le test technique à la maison** : code propre, testé (`go test -race`), formaté et linté, avec un README qui explique comment lancer, les choix, les hypothèses et ce qui manque ; un `Makefile` ou des commandes simples ; pas de dépendances inutiles.

**Les questions à poser à la fin** : déroulement d'une journée type, revue de code et mise en production, plus grosse difficulté technique actuelle, gestion de la dette, montée en compétence des nouveaux, version de Go et pratiques (linters, observabilité), prochaines étapes.

### En situation

- **"Présente-toi"** → présent, passé, futur, deux minutes.
- **"Parle-moi d'une fois où..."** → STAR avec un exemple préparé.
- **"Pourquoi Go ?"** → besoins concrets auxquels Go répond.
- **"As-tu des questions ?"** → toujours oui.

### Questions techniques

**Q : C'est quoi la méthode STAR ?** R : Situation, Tâche, Action, Résultat.

**Q : Comment parler d'un échec ?** R : Le reconnaître, expliquer sa part, la correction et ce qui a changé ensuite.

**Q : C'est quoi "disagree and commit" ?** R : Exprimer un désaccord argumenté, puis s'engager pleinement dans la décision prise.

**Q : Que faire si on ne connaît pas une réponse ?** R : Le dire, puis expliquer comment on raisonnerait ou chercherait.

**Q : Que contient un bon rendu de test technique Go ?** R : Code formaté et linté, tests avec -race, README avec lancement, choix, hypothèses et limites.

### Questions de situation

**Q : "Parle-moi d'un problème de performance que tu as résolu."** *(trame)*
R : "[**Situation** : le service, le symptôme mesuré (latence p99, CPU, mémoire).] [**Tâche** : ce qu'on attendait de moi.] [**Action** : la démarche : métriques, profil pprof ou trace, hypothèse, vérification par benchmark comparé avec benchstat, correction (allocations réduites, verrou supprimé, pool dimensionné...).] [**Résultat** : le gain mesuré en production et ce que j'en ai retenu, par exemple un tableau de bord ou un test de non-régression ajouté.]"

**Q : "Comment réagis-tu face à une technologie inconnue avec une échéance courte ?"**
R : "Je cadre d'abord ce qui est réellement nécessaire pour l'échéance, puis documentation officielle et prototype minimal pour valider le cœur du besoin. J'identifie qui connaît la technologie pour lever vite les blocages. Et je signale tôt les risques, avec une proposition (réduire le périmètre, être épaulé), plutôt que de découvrir le retard à la fin."

═══════════════════════════════════════════
# GLOSSAIRE DES SIGLES ET TERMES
═══════════════════════════════════════════

Un index rapide pour le Ctrl+F : chaque terme renvoie à une définition courte ; le détail est dans le chapitre correspondant.

| Terme | Signification |
|---|---|
| **any** | Alias de `interface{}`, satisfait par tous les types (Go 1.18) |
| **append** | Fonction intégrée qui ajoute à un slice et peut réallouer |
| **cgo** | Mécanisme d'appel de code C depuis Go |
| **CGO_ENABLED** | Variable qui active ou désactive cgo ; 0 pour un binaire statique |
| **channel** | Canal typé de communication entre goroutines |
| **client-go** | Bibliothèque officielle pour l'API Kubernetes |
| **cmp** | Package de comparaison (Compare, Or, Ordered), Go 1.21 |
| **comparable** | Contrainte des types utilisables avec == |
| **Connect** | Protocole et bibliothèques compatibles gRPC sur net/http (Buf) |
| **context** | Package de propagation d'annulation, d'échéance et de valeurs |
| **controller-runtime** | Bibliothèque de contrôleurs Kubernetes, base de Kubebuilder |
| **CSP** | Communicating Sequential Processes, modèle de concurrence de Go |
| **data race** | Accès concurrent non synchronisé dont au moins une écriture |
| **defer** | Appel différé à la sortie de la fonction, en ordre LIFO |
| **distroless** | Image de conteneur minimale sans shell |
| **embed** | Package et directive pour inclure des fichiers dans le binaire |
| **embedding** | Composition par champ anonyme, avec méthodes promues |
| **errgroup** | Groupe de goroutines avec première erreur et annulation (x/sync) |
| **errors.Is / As / AsType / Join** | Inspection et combinaison des erreurs enveloppées |
| **escape analysis** | Décision du compilateur entre pile et heap |
| **FIPS 140-3** | Norme de modules cryptographiques ; module natif depuis Go 1.24 |
| **fuzzing** | Tests à entrées générées, guidés par la couverture (Go 1.18) |
| **G-M-P** | Modèle du scheduler : goroutine, thread OS, processeur logique |
| **GC** | Garbage collector concurrent, tricolore, non générationnel |
| **GODEBUG** | Réglages de comportement et de diagnostic du runtime |
| **GOGC** | Croissance du heap déclenchant un cycle de GC (défaut 100) |
| **GOMAXPROCS** | Nombre de P, conscient des conteneurs depuis Go 1.25 |
| **GOMEMLIMIT** | Limite mémoire souple du runtime (Go 1.19) |
| **GOOS / GOARCH** | Système et architecture cibles de la compilation |
| **GOPRIVATE / GOPROXY** | Modules privés / proxy de téléchargement des modules |
| **goroutine** | Fonction concurrente légère gérée par le runtime |
| **goroutineleak** | Profil des goroutines bloquées pour toujours (Go 1.27) |
| **GOTOOLCHAIN** | Sélection automatique de la chaîne d'outils (Go 1.21) |
| **govulncheck** | Détecteur officiel de vulnérabilités atteignables |
| **gopls** | Serveur de langage officiel |
| **Green Tea** | Algorithme de marquage du GC, par défaut depuis Go 1.26 |
| **gRPC** | RPC sur HTTP/2 et Protobuf |
| **golangci-lint** | Agrégateur de linters |
| **happens-before** | Relation d'ordre et de visibilité du modèle mémoire |
| **internal/** | Répertoire dont les packages ne sont importables que par leur parent |
| **iota** | Compteur de constantes pour les énumérations |
| **iter.Seq / Seq2** | Types d'itérateurs pour range sur fonctions (Go 1.23) |
| **ko** | Outil de construction d'images de services Go sans Dockerfile |
| **mark assist** | Contribution au marquage imposée aux goroutines qui allouent |
| **method set** | Ensemble des méthodes d'un type, détermine l'implémentation des interfaces |
| **modernizers** | Réécritures automatiques de go fix (Go 1.26) |
| **module** | Unité de versionnement décrite par go.mod |
| **MVS** | Minimal Version Selection, algorithme de choix des versions |
| **netpoller** | Composant du runtime qui gère les I/O réseau non bloquantes |
| **os.Root** | Accès fichiers confiné à un répertoire (Go 1.24) |
| **PGO** | Profile-Guided Optimization (default.pgo) |
| **pprof** | Outil et format de profils (CPU, heap, goroutines, blocages) |
| **receveur** | Paramètre spécial d'une méthode (valeur ou pointeur) |
| **rune** | Alias de int32 représentant un point de code Unicode |
| **select** | Attente sur plusieurs opérations de channel |
| **singleflight** | Déduplication d'appels concurrents identiques (x/sync) |
| **slice** | Vue (pointeur, longueur, capacité) sur un tableau |
| **slog** | Logs structurés de la bibliothèque standard (Go 1.21) |
| **sqlc** | Générateur de code Go typé depuis du SQL |
| **Swiss Table** | Implémentation des maps depuis Go 1.24 |
| **sync.Pool** | Cache d'objets temporaires réutilisables |
| **synctest** | Tests de concurrence avec horloge virtuelle (Go 1.25) |
| **tag de struct** | Métadonnées de champ lues par réflexion (json, db) |
| **toolchain** | Directive go.mod de chaîne d'outils préférée |
| **type assertion / type switch** | Récupérer le type concret d'une interface |
| **valeur zéro** | Valeur par défaut de toute variable non initialisée |
| **vet** | Analyseur statique intégré (go vet) |
| **workspace (go.work)** | Développement de plusieurs modules locaux ensemble |
| **%w** | Verbe de fmt.Errorf qui enveloppe une erreur |

═══════════════════════════════════════════
# RÉCAP FINAL
═══════════════════════════════════════════

### La méthode en une page

**Avant de répondre** : cadrer (reformuler, poser une question de contexte), puis structurer ("trois points"), donner un exemple concret, annoncer les compromis.

**Les dix réflexes Go qu'un évaluateur attend** :
1. Les erreurs sont des valeurs : vérifiées, enveloppées avec `%w`, loggées une seule fois.
2. Le `context.Context` est propagé en premier paramètre, avec des délais.
3. Chaque goroutine a une condition de fin ; pas de fuite.
4. Un état partagé est protégé (mutex, atomique) ou confié à une seule goroutine ; tests avec `-race`.
5. Les interfaces sont petites et définies côté consommateur ; on renvoie des structs.
6. Les slices partagent leur tableau : attention à `append` ; les maps ne sont pas thread-safe.
7. Une interface contenant un pointeur nil n'est pas nil.
8. Serveurs et clients HTTP ont des timeouts, les corps sont fermés, le client est partagé.
9. En conteneur : binaire statique, image minimale, GOMEMLIMIT, GOMAXPROCS correct, arrêt propre.
10. On mesure avant d'optimiser : benchmarks, benchstat, pprof, trace.

**Erreurs fréquentes en entretien** : écrire du "Java en Go" (interfaces partout, getters, hiérarchies), ignorer une erreur, lancer des goroutines sans les attendre, oublier le contexte, répondre par une définition sans exemple, prétendre connaître une fonctionnalité récente sans pouvoir l'expliquer.

**Préparation** : relire un chapitre par jour, refaire les questions pièges et les exercices de live coding sans regarder la solution, préparer deux ou trois exemples tirés de tes propres projets (une optimisation, un incident, un problème de concurrence) et les relier aux concepts de ce document.
