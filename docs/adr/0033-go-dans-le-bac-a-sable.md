# 0033. Go dans le bac à sable : un runner multi-langage

- Statut : accepté
- Date : 2026-11-04
- Prolonge l'ADR 0028 (exécution des « qu'affiche ce code ? »), l'ADR 0029 (format des exercices) et l'ADR 0030 (bac à
  sable).

## Contexte

Go doit devenir aussi complet que Java (CLAUDE.md, section 5.6) : un dossier de notions, la matrice de cartes, et des
exercices de code jugés par leurs tests. Aujourd'hui, tout ce qui exécute du code suppose Java :
- le runner n'a qu'une image (`sandbox/java`), lit un rapport JUnit et n'accepte que des fichiers `.java` ;
- le backend et la PWA n'acceptent, eux aussi, que des fichiers `.java`, et l'éditeur colore du Java ;
- la CI exécute les « qu'affiche ce code ? » avec JShell.

L'ADR 0030 l'avait prévu : « un autre langage demandera sa propre image et son protocole, par un nouvel ADR ».

Une mesure guide la conception. Dans un conteneur limité à un processeur, un `go test` à froid prend **27 secondes** : il
recompile la bibliothèque standard, qui n'est plus livrée précompilée depuis Go 1.20. C'est au-delà du délai de 20
secondes du bac à sable.

## Options envisagées

**Le langage d'une exécution** :
- le déduire, dans le runner, de l'extension des fichiers : implicite, et le runner devinerait ;
- **un champ `language` explicite dans la requête** (retenu), contrôlé contre les extensions des fichiers.

**Le cache de compilation de Go** :
- aucun cache : 27 secondes par exécution ;
- un cache copié dans `/tmp` à chaque exécution : rapide, mais 60 Mo de `tmpfs` pris sur la mémoire du conteneur ;
- **un cache précompilé dans l'image, lu en lecture seule** (retenu) : 1,5 seconde par exécution, rien à copier. Go
  tolère un cache où il ne peut pas écrire : le code du joueur est recompilé à chaque fois, et c'est le seul.

**Le rapport des tests** :
- `go test -json` en une étape : les erreurs de compilation se mêlent alors au rapport ;
- **compiler d'abord (`go test -c`), puis lancer le binaire à travers `test2json`** (retenu). Une erreur de compilation a
  son marqueur, comme en Java, et le rapport est un flux JSON, une ligne par événement.

**Les « qu'affiche ce code ? » de Go** :
- les exécuter dans le bac à sable : il faudrait un conteneur par carte, ou un mode de lot que le runner n'a pas ;
- **la chaîne d'outils `go` de la machine** (retenu), comme JShell est celle du JDK. Notre contenu est relu par pull
  request et n'a pas besoin d'isolation (ADR 0028).

## Décision

**L'image `sandbox/go/`** :
- elle part de `golang:1.27-bookworm`, sans réseau à l'exécution (`GOPROXY=off`, `GOTOOLCHAIN=local`, `CGO_ENABLED=0`) ;
- un cache précompile les paquets standard courants (`fmt`, `strings`, `sort`, `slices`, `maps`, `sync`, `context`,
  `errors`, `time`, `testing`...) ; il est lu en lecture seule ;
- elle compile `test2json`, que l'image officielle ne fournit plus.

Son script, `run.sh`, suit le même protocole que celui de Java :
1. il lit une archive tar sur son entrée standard (`src/` et `tests/`) ;
2. il réunit les fichiers dans un même dossier, avec un `go.mod` (`module exercice`, `go 1.27`) ;
3. il compile : en cas d'échec, `@@recallio:compilation-error`, suivi des erreurs du compilateur ;
4. sinon, il lance les tests : `@@recallio:report`, suivi du flux JSON de `test2json`.

L'isolation est celle de l'ADR 0030, inchangée (réseau, système de fichiers, mémoire, processus, utilisateur, délai).

**Le runner** :
- la requête porte `language` (`JAVA` ou `GO`) ;
- chaque langage a son image (`RECALLIO_SANDBOX_JAVA_IMAGE`, `RECALLIO_SANDBOX_GO_IMAGE`), ses règles de chemin et son
  lecteur de rapport (`JunitReport`, `GoTestReport`) ;
- un test Go est un test de plus haut niveau ou un sous-test sans sous-test (`TestSomme/vide`) : un parent n'est pas
  compté deux fois ;
- le message d'un test en échec est ce que le test a écrit (`t.Errorf`, la `panic`), tronqué comme en Java.

**Les exercices Go** (format de l'ADR 0029, inchangé ailleurs) :
- des fichiers `.go` à plat, sans sous-dossier : un seul package par exercice ;
- les tests s'appellent `*_test.go`, le code de départ et la solution non ;
- tous les fichiers déclarent le même package, celui qu'on veut (`package panier`) ;
- seule la bibliothèque standard est disponible.

**Le langage d'un exercice** se déduit de l'extension de ses fichiers (`ExerciseFiles.language()`) ; un exercice qui
mêle les deux est refusé à l'import. Le backend transmet ce langage au runner.

**La PWA** colore chaque fichier selon son extension : `@codemirror/lang-go` rejoint `@codemirror/lang-java`, dans le
même chunk chargé à la demande (quelques kilo-octets compressés).

**Les « qu'affiche ce code ? » de Go** :
- `check` garde sa forme (ADR 0028) ;
- `panics` remplace `throws` : un extrait du message d'arrêt attendu (`index out of range`, `all goroutines are asleep`) ;
- le code exécuté est un programme complet (`package main` et `func main`). Go refuse les imports inutilisés : un
  fragment ne s'envelopperait pas de façon fiable ;
- la CI (job `content`) installe Go 1.27, comme l'image. En local, sans `go`, ces cartes sont ignorées, sauf en CI où
  elles échouent ;
- une sortie non déterministe (ordre d'une `map`, ordonnancement des goroutines) se déclare `unverifiable`.

## Conséquences

- Un exercice Go coûte 1,5 seconde, autant qu'un exercice Java ; l'image pèse environ 1,3 Go.
- **La version de Go est écrite à deux endroits**, l'image du bac à sable et le job `content` : une montée de version
  les change ensemble.
- **La triche reste assumée** (ADR 0030) : un test Go qui imprime `--- PASS:` peut tromper `test2json`. En révision
  personnelle, cela ne nuit qu'au tricheur.
- **Un troisième langage** (Python, Rust...) suivra le même chemin : une image, un lecteur de rapport, une extension, une
  coloration. Ce sera un ajout, plus une refonte.
