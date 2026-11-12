# Rédiger un sujet pour Recallio

Ce dossier contient le contenu de Recallio : un **document de référence par sujet** (Java, Go, puis Spring Boot,
Kubernetes, Python...), dans `source/`. Chaque document est importé et découpé en cartes de révision.

**Ajouter un sujet ne demande aucun code** : un fichier Markdown, relu par pull request. Ce guide décrit le format
attendu, les règles d'une bonne question, et la procédure de publication.

- [1. Le fichier](#1-le-fichier)
- [2. L'en-tête](#2-len-tête)
- [3. Les chapitres](#3-les-chapitres)
- [4. Les questions](#4-les-questions)
- [5. Écrire une bonne question](#5-écrire-une-bonne-question)
- [6. Modifier un document existant sans perdre la progression](#6-modifier-un-document-existant-sans-perdre-la-progression)
- [7. Ajouter un sujet, étape par étape](#7-ajouter-un-sujet-étape-par-étape)
- [8. Liste de contrôle avant la pull request](#8-liste-de-contrôle-avant-la-pull-request)

## 1. Le fichier

`source/<slug>-entretien-reference.md`, par exemple `source/spring-boot-entretien-reference.md`.

Le nom du fichier doit correspondre au `subject` de l'en-tête : la CI le vérifie.

## 2. L'en-tête

Le document commence **obligatoirement** par un en-tête qui décrit le sujet (ADR 0013) :

```markdown
---
subject: spring-boot
title: Spring Boot
category: frameworks
position: 30
status: draft
---
```

| Clé | Valeur |
|---|---|
| `subject` | Le **slug** : minuscules, chiffres et tirets (`spring-boot`, `ia-llm`). **Définitif** : il entre dans l'identité de toutes les cartes du sujet, le changer ferait perdre leur progression à tous les utilisateurs. |
| `title` | Le nom affiché (`Spring Boot`). Modifiable librement. |
| `category` | `languages`, `frameworks`, `cloud-devops`, `data`, `security`, `fundamentals` ou `ai`. Une nouvelle catégorie passe par un ADR. |
| `position` | L'ordre d'affichage dans le catalogue. Java vaut 10 et Go 20 : laisser des trous permet d'en intercaler. |
| `status` | `draft` (importé, visible des seuls admins) ou `published` (visible de tous). |

Après l'en-tête viennent le titre du document (`# Spring Boot : le document de référence pour les entretiens
techniques`) et une introduction libre : l'import les ignore.

## 3. Les chapitres

Un chapitre devient un **thème** dans l'application. Son titre est numéroté et encadré de lignes `═` :

```markdown
═══════════════════════════════════════════
# 1. SPRING BOOT : FONDAMENTAUX
═══════════════════════════════════════════
```

Dans chaque chapitre, dans cet ordre :

| Section | Rôle | Importée ? |
|---|---|---|
| `### Vue d'ensemble` | Le pourquoi, en quelques lignes | non (lecture) |
| `### Cours` | Le détail des notions | non (lecture) |
| `### En situation` | Des symptômes vers les notions | non (lecture) |
| `### Questions techniques` | Réponses courtes | **oui** : cartes « Question » |
| `### Questions de situation` | Réponses rédigées | **oui** : cartes « Mise en situation » |
| `### QCM` (facultative) | Questions à choix multiples | **oui** : cartes « QCM », corrigées par l'application |

Deux chapitres particuliers :

- **Questions pièges** : un chapitre dont le titre contient « QU'AFFICHE CE CODE ? » (par exemple `# 14. QUESTIONS
  PIÈGES : QU'AFFICHE CE CODE ?`) produit des cartes « Qu'affiche ce code ? ». Il est reconnu à son titre, quel que soit
  son numéro.
- **Glossaire** : un titre `# GLOSSAIRE`, suivi d'un tableau `| Terme | Signification |`. Chaque ligne devient une carte
  « Définition ».

Tout ce qui n'est pas dans un chapitre numéroté ou dans le glossaire est ignoré : sommaire, méthode générale,
récapitulatif final.

Un sujet n'a pas besoin de tous les types de cartes : un chapitre de questions pièges n'a de sens que pour un langage,
et le glossaire est facultatif. Chaque chapitre doit en revanche contenir au moins une question.

## 4. Les questions

### Question technique : sur une ligne

```markdown
**Q : Que fait `defer` ?** R : Il diffère l'appel d'une fonction jusqu'au retour de la fonction englobante.
```

### Question avec du code : la réponse sur sa propre ligne

````markdown
**Q :**
```go
err := trouver()
fmt.Println(err == nil)
```
R : `false` : l'interface contient le type `*MonErr` avec une valeur nil, elle n'est donc pas nil.
````

### Question de situation : réponse rédigée sur plusieurs lignes

```markdown
**Q : "Pourquoi choisirais-tu Go plutôt que Java pour un nouveau service ?"**
R : "Go a des atouts nets pour certains services : un **binaire statique**...
Java garde l'avantage sur un écosystème d'entreprise plus riche...
Je choisirais Go pour un service réseau..."
```

La réponse s'étend jusqu'à la question suivante ou au prochain titre ; elle peut contenir des blocs de code.

### QCM : des choix en cases à cocher, puis l'explication

```markdown
**Q : Dans quel ordre s'exécutent plusieurs `defer` d'une même fonction ?**
- [ ] Dans l'ordre où ils sont écrits
- [x] Dans l'ordre inverse (dernier entré, premier sorti)
- [ ] Dans un ordre non garanti
R : Les appels différés sont empilés : au retour de la fonction, le dernier `defer` déclaré s'exécute en premier.
```

- De **2 à 8 choix**, dans une section `### QCM` du chapitre ; `- [x]` marque une bonne réponse, `- [ ]` une mauvaise.
- **Plusieurs bonnes réponses** sont permises : l'application affiche alors des cases à cocher et annonce « Plusieurs
  réponses possibles ». La réponse n'est juste que si toutes les bonnes sont cochées, et elles seules.
- La ligne `R :` est l'**explication**, montrée après la correction (sur plusieurs lignes si besoin). Elle dit pourquoi
  la bonne réponse l'est, et pourquoi le piège le plus tentant ne l'est pas.
- De bons distracteurs sont des erreurs que font vraiment les candidats, pas des réponses absurdes.
- Les choix sont affichés dans l'ordre du document : changer leur ordre, leur texte ou la bonne réponse crée une
  nouvelle version de la carte (la progression est conservée).

### Règles communes

- Le Markdown est rendu dans l'application : gras, `code`, listes, blocs de code.
- **Toujours indiquer le langage d'un bloc de code** (` ```go `, ` ```java `, ` ```yaml `, ` ```sql `...) : il sert à la
  coloration syntaxique. Un bloc sans langage s'affiche sans couleurs. La PWA ne colore que les langages de
  `frontend/src/components/highlightLanguages.ts`, nommés comme highlight.js (`bash`, `cpp`, `yaml`, et non `sh`,
  `c++`, `yml`) ; la CI échoue sur un autre (`CodeLanguagesTest`), et en ajouter un tient en deux lignes de ce fichier.
- Une annotation après la question est permise : `**Q : Que fait `defer` ?** *(très fréquent)*`.

## 5. Écrire une bonne question

Une carte est revue des dizaines de fois, sur un téléphone, en quelques secondes. Elle doit être :

- **Une seule idée.** « Différence entre `ArrayList` et `LinkedList` ? » plutôt que « Parlez-moi des collections ».
- **Autonome.** Lisible sans le cours autour : pas de « comme vu plus haut », pas de « cet exemple ».
- **Posée comme en entretien.** C'est la question que pose un recruteur technique, pas une question de quiz.
- **Une réponse courte pour les questions techniques** : une à trois phrases. Le détail appartient au cours ; une
  réponse longue est une question de situation.
- **Une réponse vérifiable.** Le code des questions pièges est compilé et exécuté avant d'être publié ; les versions
  citées sont datées (« Go 1.27, août 2026 »).
- **Sans doublon.** Deux questions qui ne diffèrent que par la ponctuation ou la casse sont un doublon : la CI le
  signale.
- **Une question de situation donne une démarche**, pas seulement un résultat : cadrer, diagnostiquer, proposer,
  arbitrer (voir la méthode universelle des documents existants).

## 6. Modifier un document existant sans perdre la progression

La progression d'un utilisateur est attachée à l'**identité** d'une carte : le sujet, le **numéro de chapitre** et la
**question normalisée** (sans casse, accents, ponctuation ni mise en forme). D'où ces règles :

| Modification | Effet |
|---|---|
| Corriger la réponse | Nouvelle version de la même carte : la progression est conservée. |
| Corriger une faute, la ponctuation ou le gras d'une question | Même carte : la progression est conservée. |
| Réécrire le sens d'une question | Nouvelle carte ; l'ancienne est retirée (sa progression n'est plus utilisée). |
| Supprimer une question | La carte est retirée ; elle revient avec sa progression si la question est rétablie. |
| Renommer un chapitre | Même thème (seule son adresse change) : la progression est conservée. |
| **Renuméroter un chapitre** | **Nouveau thème : toutes ses cartes perdent leur progression.** |
| **Changer le `subject`** | **Tout le sujet perd sa progression.** |

En pratique : **ajouter un nouveau chapitre à la fin**, avec le numéro suivant, plutôt que de l'intercaler.

Le rapport d'import (section 7) liste les cartes ajoutées, modifiées et retirées : un nombre de retraits inattendu
signale une renumérotation.

## 7. Ajouter un sujet, étape par étape

1. **Écrire le document** `source/<slug>-entretien-reference.md` en suivant ce guide, avec `status: draft`.
2. **Vérifier en local** : `cd backend && ./mvnw test -Dtest=ReferenceDocumentTest` contrôle tous les documents en
   quelques secondes. Puis `./mvnw quarkus:dev` importe le dossier au démarrage : le rapport d'import apparaît dans les
   logs.
3. **Ouvrir une pull request de contenu.** Le job CI `content` parse tous les documents et échoue sur une anomalie :
   question sans réponse, ligne non reconnue, doublon, en-tête invalide, nom de fichier qui ne correspond pas au sujet,
   chapitre vide, slug déjà utilisé.
4. **Merger, puis importer.** En local et dans le compose, l'import est automatique au démarrage. En production, un
   admin envoie le document :

   ```shell
   curl -X POST "$API/api/v1/admin/content-imports" \
     -H "Authorization: Bearer $ADMIN_TOKEN" \
     -H "Content-Type: text/markdown; charset=UTF-8" \
     --data-binary @content/source/spring-boot-entretien-reference.md
   ```

   La réponse est le rapport d'import (cartes ajoutées, modifiées, retirées, anomalies).
5. **Relire dans l'application.** Connecté avec un compte admin, le sujet apparaît dans le catalogue avec le badge
   **Brouillon** : on parcourt ses thèmes et ses cartes en conditions réelles (rendu du Markdown, coloration du code). Les
   autres utilisateurs le voient seulement annoncé, grisé, dans les pages Cours, Tour et Révisions (ADR 0052) : son
   contenu leur reste caché, et il n'entre dans aucune révision.
6. **Publier** : une seconde pull request passe l'en-tête en `status: published`, puis on réimporte. Les
   utilisateurs le voient dans le catalogue et peuvent le suivre depuis leur profil.

Pour **annoncer** un sujet avant de l'écrire (ADR 0052), un dossier `content/<slug>/` qui n'a que son `subject.yaml`, en
`status: draft`, suffit : les pages Cours, Tour et Révisions le montrent en « work in progress ». Le dossier se remplit
ensuite, comme celui de Java : **le référentiel d'abord**, dans sa propre pull request, pour faire relire le découpage
(domaines, chapitres, priorités, prérequis) avant d'écrire la moindre carte ; puis les notions, par priorité. Tant
qu'un brouillon n'a aucun fichier de notion, la CI n'exige pas de carte de lui. Un sujet sans référentiel ne peut pas
être publié (la CI le refuse). Son logo, s'il en a un, va dans `frontend/public/logos/` (voir le README de ce dossier).

Pour **retirer temporairement** un sujet publié, repasser en `status: draft` et réimporter : il disparaît du catalogue
et des révisions, sans perte de progression, et revient tel quel s'il est republié.

## 8. Le référentiel d'un sujet (refonte en cours, ADR 0025)

Le contenu évolue vers un **référentiel de notions** : `content/<sujet>/referentiel/*.yaml`, un fichier par domaine
(bases, objet, collections...), puis des chapitres et des notions. Il fait l'inventaire de **tout** le sujet ; le
contenu (leçons, cartes, exercices) viendra s'y rattacher notion par notion.

```yaml
domain: collections
title: Collections
position: 60
chapters:
  - id: maps
    title: Maps
    notions:
      - {id: hashmap-usage, title: "HashMap : put, get, getOrDefault", level: beginner, priority: 1, requires: [collections-panorama]}
```

| Champ | Valeurs | Rôle |
|---|---|---|
| `id` | minuscules, chiffres, tirets | **définitif** : la progression s'y rattachera |
| `level` | `beginner`, `intermediate`, `expert` | le public visé : apprendre, préparer un entretien, entretenir son niveau |
| `priority` | 1 essentiel, 2 courant, 3 avancé, 4 rare | l'ordre de production du contenu |
| `requires` | des `id` de notions | les prérequis, pour le parcours « apprendre » |
| `formats` | facultatif | les formats qui conviennent, quand tous ne conviennent pas (pas de bug à corriger sur l'histoire de Java) |
| `complete` | facultatif | `true` quand la notion est terminée : la CI vérifie alors qu'elle le reste (4 cartes par format, une leçon ; et un exercice dans son chapitre quand toutes les notions de priorité 1 du chapitre sont complètes et qu'au moins une de ses notions retient `exercise`) |

Formats possibles : `course`, `true-false`, `multiple-choice`, `fill-in`, `code-output`, `bug-fix`, `scenario`,
`definition`, `exercise`. La cible est de **4 à 5 cartes par format applicable** pour chaque notion.

La CI (`ReferentialTest`) refuse :
- un identifiant en double ou mal formé ;
- un prérequis inexistant ;
- un cycle de prérequis ;
- un prérequis plus avancé ou moins essentiel que la notion qui en dépend (un débutant ne doit pas buter sur une notion d'expert).

### Les fichiers de notion (ADR 0026)

Un sujet migré a son **dossier** : `content/<sujet>/subject.yaml` (les mêmes clés que l'en-tête d'un document), son
référentiel, et un fichier par notion, `notions/<domaine>/<notion>.yaml`. Un sujet a soit un document Markdown, soit un
dossier, jamais les deux.

```yaml
notion: string-pool                 # un identifiant du référentiel
lesson: |                           # facultative ; lue avant la 1re carte en parcours « apprendre »
  Le **pool de chaînes** déduplique les littéraux...
cards:
  - key: emplacement-du-pool        # définitive : la progression s'y rattache
    format: course
    question: Où se trouve le pool de chaînes ?
    choices:                        # ADR 0051 : chaque question se corrige sur ses choix
      - {text: "Dans le tas (heap)", correct: true}
      - {text: "Dans la pile du thread", correct: false}
      - {text: "Dans le Metaspace", correct: false}
    answer: Dans le tas (heap) depuis Java 7 ; avant, dans la PermGen.   # l'explication, après la correction
  - key: comparer-deux-chaines
    format: multiple-choice
    question: Comment tester que deux chaînes ont le même contenu ?
    choices:
      - {text: "`a.equals(b)`", correct: true}
      - {text: "`a == b`", correct: false}
    answer: "`equals` compare le contenu, `==` les références."
```

- La **clé** d'une carte (minuscules, chiffres, tirets) est unique dans sa notion. On peut corriger ou reformuler la
  question et la réponse librement : la carte et la progression restent. Changer la clé crée une autre carte.
- **Formats acceptés dans un fichier de notion** : `course`, `multiple-choice`, `code-output`, `scenario`, `definition`,
  `true-false`, `bug-fix`, `fill-in`. Les exercices de code (`exercise`) ont leur propre dossier (ci-dessous).
- **Des choix pour chaque question** (ADR 0051) : une carte `course`, `definition`, `code-output`, `bug-fix` ou
  `scenario` a des `choices`, écrits comme ceux d'un QCM. Le serveur la corrige : personne ne se note soi-même. Sa
  réponse (`answer`) devient l'explication, montrée après la correction. Le temps de la migration, une carte qui n'a pas
  encore ses choix s'auto-évalue. Pour de bons choix :
  - **De 2 à 8 choix**, le plus souvent 3 ou 4, jamais deux fois le même texte (la CI le refuse).
  - **Une seule réponse défendable**. Quand plusieurs sont justes, la question le dit (« Lesquelles... ? ») et chacune
    est marquée `correct: true`.
  - **Les mauvais choix sont des erreurs qu'on fait vraiment** : la confusion `print` / `println`, l'indice qui commence
    à 1, la concaténation prise pour une addition. Pas de choix absurde, ni « toutes les réponses ci-dessus ».
  - **Des choix de même forme et de longueur voisine** : le bon ne doit pas se repérer à ce qu'il est le plus long ou le
    seul précis.
  - **L'ordre n'a pas d'importance** : l'application mélange les choix à chaque affichage. Un vrai ou faux garde l'ordre
    Vrai, Faux.
  - **La question se comprend seule** et finit par une vraie question : « Pourquoi ? », « Que lui réponds-tu ? »,
    « Quelle correction est la bonne ? ». Elle tutoie, comme toute l'application (ADR 0050) : « Complète », « Ton
    collègue », jamais « Complétez » ni « vous ».
  - **L'explication dit pourquoi** le bon choix est juste et, si possible, pourquoi l'erreur la plus tentante est fausse.
  - Un choix sur plusieurs lignes (une sortie, un morceau de code) s'écrit en bloc :
    ```yaml
    choices:
      - text: |-
          ```
          AB
          C
          ```
        correct: true
    ```
- **Règles vérifiées par la CI** (`ContentQualityBaselineTest`, job `content`). Quatre défauts qu'une machine sait
  repérer font échouer la pull request :

  | Règle | Ce que la CI refuse |
  |---|---|
  | `longest-correct-choice` | Sur une carte à une seule bonne réponse, un bon choix plus long que le plus long des mauvais de plus de la moitié, et d'au moins 20 caractères. Les choix en bloc de code et les vrai ou faux ne comptent pas. |
  | `formal-address` | « vous », « votre », « vos » ou un verbe en « -ez » (« Complétez », « Indiquez ») dans la question, un choix ou l'explication, hors du code. |
  | `duplicate-question` | La même question, dans le même format, deux fois dans le sujet (à la casse, aux accents et aux espaces près). |
  | `multiple-answers` | Plus d'un QCM sur quatre à plusieurs bonnes réponses, dans le sujet. |

  Pour corriger un bon choix trop long, **allonge les mauvais** plutôt que de raccourcir le bon : une erreur plausible
  s'explique en autant de mots que la vérité. La précision qui ne sert qu'à justifier (« 1, le reste de la division »)
  va dans l'explication.

  Le contenu écrit avant ces règles les enfreint : `content/quality-baseline.yaml` note, par sujet et par règle, le
  nombre d'infractions tolérées. C'est un cliquet : quand tu en corriges, la CI te demande d'abaisser le plafond, et
  il ne se relève jamais. Un sujet absent du fichier n'en tolère aucune. En local, le test écrit la liste complète des
  infractions dans `backend/target/content-quality.md`.
- **Exercice de code** (`exercise`, ADR 0029) : un dossier `exercices/<domaine>/<notion>/<clé>/`, à côté de `notions/` :
  ```
  exercices/collections/choisir-collection/sans-doublons/
  ├── exercise.yaml        # statement: l'énoncé ; explanation: l'explication de la solution (Markdown)
  ├── starter/Doublons.java    # le code de départ, que le joueur modifie
  ├── solution/Doublons.java   # la solution : exactement les fichiers du code de départ
  └── tests/DoublonsTest.java  # des tests JUnit 5, qui jugent l'un et l'autre
  ```
  - La clé est le nom du dossier : définitive, et distincte des clés des cartes de la notion.
  - Des fichiers `.java` seulement, de 1 à 10 par partie ; des packages sont permis (`starter/fr/recallio/Panier.java`).
  - **En Go** (ADR 0033), la même forme avec des fichiers `.go` : `starter/` et `solution/` à plat, dans un seul
    package (pas de `package main`), et `tests/*_test.go` dans ce même package, avec le package `testing` seul (pas de
    bibliothèque d'assertions). Le bac à sable n'a pas de réseau et son cache de compilation est en lecture seule :
    les paquets standard précompilés dans `sandbox/go/warm/warm.go` sont rapides, un autre paquet standard se compile à
    chaque exécution (ADR 0037) ; s'il sert souvent, on l'ajoute à `warm.go`. Le code de départ compile (une fonction qui renvoie la valeur zéro, ou
    une version séquentielle) et échoue aux tests. Un test de concurrence ne repose pas sur une durée : il attend que
    les goroutines se rejoignent, avec un délai de garde court (quelques centaines de millisecondes) pour échouer
    vite.
  - **En C++** (ADR 0053), des fichiers `.cpp` et `.hpp` à plat, compilés par GCC 14 en C++23 : le code dans
    `starter/` et `solution/` (un en-tête et son implémentation, sans `main`), les tests dans `tests/*_test.cpp`, avec
    doctest. Un test inclut `"doctest.h"` et l'en-tête qu'il teste (`#include "panier.hpp"`), puis déclare ses cas
    (`TEST_CASE("retire les doublons en gardant l'ordre") { CHECK(...); }`) ; le `main` des tests est fourni. Seule la
    bibliothèque standard est disponible. Un test ne vérifie que du comportement défini : pas de lecture hors des
    bornes ni de variable non initialisée, qui passeraient ou échoueraient au hasard.
  - La solution doit passer tous les tests, et le code de départ compiler mais en échouer au moins un : le job CI
    `runner` joue chaque exercice dans le bac à sable (ADR 0030). En local : `cd runner && ./mvnw test
    -Dtest=ExercisesTest` (Docker requis).
  - Les tests nomment le comportement attendu (`retireLesDoublonsEnGardantLOrdre`) : ils servent aussi d'indice au
    joueur, qui les voit.
  - En révision, le joueur modifie le code de départ, lance les tests autant de fois qu'il veut, puis rend son code :
    ses tests le jugent (ADR 0030). La solution et l'explication n'apparaissent qu'avec la correction.
- **Vrai ou faux** (`true-false`, ADR 0027) : la question est une affirmation, `truth: true` ou `truth: false` dit si
  elle est vraie, la réponse explique pourquoi. Le serveur corrige. Une affirmation sans ambiguïté : pas de « toujours »
  ni de « jamais » qui souffrent d'exceptions, pas de double négation.
- **Bug à corriger** (`bug-fix`) : la question montre le code bogué (et le symptôme), et demande la cause (« Pourquoi ? »)
  ou la correction (« Quelle correction est la bonne ? ») ; ses choix sont des causes ou des corrections. La réponse
  donne le code corrigé puis l'explication.
- **Compléter le code** (`fill-in`) : chaque `___` de la question est un trou ; `blanks` liste, dans l'ordre, les
  réponses acceptées par chaque trou (`blanks: [[equals], [compareTo, compareToIgnoreCase]]`). Le serveur corrige, aux
  espaces près, en respectant la casse. Prévoir toutes les variantes justes ; un trou porte sur un mot-clé, une méthode
  ou une courte expression, pas sur une ligne entière.
- **Qu'affiche ce code ?** (`code-output`, ADR 0028) : la CI exécute le code avec JShell et compare le résultat à la
  vérification `check`, qui n'est jamais montrée à l'utilisateur :
  ```yaml
  check:
    code: |-                       # facultatif : par défaut, le premier bloc java de la question
      System.out.println(10 / 3 + " " + 10 % 3);
    output: "3 1"                  # la sortie exacte ; un bloc |- pour plusieurs lignes
    throws: NullPointerException   # facultatif : l'exception qui arrête le code
    # ou, seul : compiles: false
  ```
  - Le code a les imports par défaut de JShell (`java.util.*`, `java.util.stream.*`, `java.io.*`, `java.math.*`...), et
    peut déclarer classes et méthodes sans classe englobante.
  - Une question qui n'est pas exécutable telle quelle (une expression suivie de « ? », du code avec `...`) donne son
    `code`, avec un `println` qui affiche ce que la question demande.
  - Au premier niveau, JShell ignore `final` et `static` : une variable y est un champ. Une carte qui dépend d'une
    variable locale (une constante `final` calculée à la compilation) place son code dans une méthode :
    `void run() { ... }` puis `run();`.
  - Les autres packages s'importent dans le bloc de code de la question (`java.util.concurrent.atomic`,
    `java.util.concurrent.locks`, `java.lang.reflect`, `java.nio.charset`...). Un import ciblé évite les ambiguïtés
    (`import java.lang.reflect.Proxy;`, car `java.net.Proxy` est déjà importé).
  - `throws` attend le nom simple de l'exception (`NoSuchElementException`).
  - Le constructeur compact d'un record doit être `public`, et un type scellé se déclare dans une classe englobante.
  - JShell appelle `toString` sur les valeurs qu'il affiche, et son thread principal gère lui-même les interruptions :
    une carte sur l'interruption exécute son code dans un nouveau thread.
  - Une sortie non déterministe (threads, `hashCode` d'identité) remplace `check` par `unverifiable: <raison>`.
  - **En Go** (ADR 0033), `check.code` est un programme complet (`package main`, ses imports, `func main`), compilé et
    lancé par la chaîne d'outils `go` ; `panics` remplace `throws` et attend un extrait du message d'arrêt, qu'il
    s'agisse d'une panique (`index out of range`) ou d'une erreur fatale du runtime (`all goroutines are asleep`).
    La question montre seulement le fragment utile : les déclarations (types, fonctions) au-dessus, puis le corps de
    `main`. Une sortie concurrente reste déterministe (résultats rangés par indice, sommes, `WaitGroup`) ; l'ordre
    d'une `map` affichée par `fmt` est trié, mais pas celui d'un `range`.
  - **En C++** (ADR 0053), le code exécuté est un programme complet (ses `#include`, puis `int main()`), compilé par
    GCC 14 avec les options de `sandbox/cpp/flags` (C++23) ; c'est `check.code`, ou à défaut le bloc ```` ```cpp ````
    de la question. `throws` nomme le type de l'exception que personne ne rattrape, avec son espace de noms
    (`std::out_of_range`). La question peut ne montrer que le fragment utile, si `check.code` donne le programme
    complet. Une sortie qui dépend d'un comportement indéfini (débordement d'un entier signé, lecture hors des
    bornes) ou du compilateur (`sizeof`, l'ordre d'évaluation des arguments) remplace `check` par
    `unverifiable: <raison>` ; une carte peut justement demander « Que garantit le langage ? ». La CI installe GCC 14 ;
    en local, sans `g++` 14, ces cartes sont ignorées.
  - `./mvnw test -Dtest='CodeOutputTest'` exécute toutes ces cartes, comme la CI.
  - **Ses choix** (ADR 0051) : le bon est la sortie de `check`, écrite comme du code (`` `123` ``, ou un bloc pour
    plusieurs lignes), et aucun autre choix n'affiche la même chose. Pour une exception, le bon choix la nomme (« Une
    `NullPointerException` ») ; pour une panic Go, il dit « panic » ; pour un code qui ne compile pas, il le dit
    (« Ça ne compile pas ») ; pour un code qui n'affiche rien, il dit « Rien ». `./mvnw test
    -Dtest='CodeOutputChoicesTest'` le vérifie, sans rien exécuter.
- En YAML, une valeur qui contient « : » (la typographie française en met partout) doit être entre guillemets, ou
  écrite en bloc `|-`.
- Un format exclu par le référentiel pour la notion (`formats`) est refusé.
- `./mvnw test -Dtest='SubjectFolderTest'` vérifie chaque dossier, comme la CI.
- **Couverture** : le job CI `content` publie, dans le résumé de chaque PR, la couverture du référentiel par priorité,
  la matrice par format et la liste des notions essentielles sans carte. Une notion est *complète* quand chacun de ses
  formats a au moins 4 cartes. C'est une mesure, pas un blocage, jusqu'au pilote (étape 6 de la phase 3 bis). En local,
  `./mvnw test -Dtest='ContentCoverageReportTest'` l'écrit dans `backend/target/content-coverage.md`.

### Les pages du Tour (ADR 0037)

Une notion du Tour a, dans son fichier, un bloc `tour` entre sa leçon et ses cartes : une ou plusieurs pages, dans
l'ordre où on les lit. Le Tour d'un sujet suit l'ordre du référentiel.

```yaml
tour:
  - title: Le premier programme   # facultatif : le titre de la notion par défaut ; 80 caractères au plus
    text: |-
      L'explication, en Markdown, pour quelqu'un qui découvre le langage.
    example:
      files:
        main.go: |-                # Main.java en Java ; plusieurs fichiers sont permis
          package main
          ...
      output: |-
        La sortie exacte du programme.
    task:                          # facultatif : un « À toi »
      instructions: |-
        La consigne.
      starter:                     # compile et s'exécute, sans donner la sortie attendue
        main.go: |-
          ...
      solution:                    # les mêmes fichiers que le code de départ
        main.go: |-
          ...
      output: La sortie attendue.
```

- **Le public** : quelqu'un qui programme déjà dans un autre langage. Le texte n'est pas la leçon, qui est la fiche de
  révision : il explique en phrases courtes, et invite à essayer (« Essaie : remplace `Println` par `Print` »).
- **Un exemple par page** : un programme complet, qui s'exécute sans entrée et sort avec le code 0. En Go,
  `package main` et `func main` ; en Java, un `Main.java`, éventuellement en fichier source compact
  (`void main() { IO.println("Bonjour"); }`) ; en C++, un `main.cpp` avec `int main()`, et d'autres `.cpp` et `.hpp`
  à plat si la page parle de compilation séparée. Sa sortie est déterministe : ni heure, ni hasard, ni ordre d'une `map`
  parcourue par `range`, ni architecture de la machine (`runtime.GOARCH`).
- **Le code est formaté** comme le ferait l'outil du langage : des tabulations en Go (gofmt), quatre espaces en Java
  et en C++.
  En YAML, une tabulation est permise dans un bloc `|-`, après l'indentation.
- **Un « À toi »** demande une petite modification, vérifiable par la sortie seule. Le code de départ compile et
  s'exécute, car l'utilisateur le lance d'abord, mais il ne donne pas la sortie attendue.
- **Les prérequis** d'une notion qui sont dans le Tour ont leur page plus tôt, et toutes les pages d'un sujet sont dans
  un seul langage : le parseur le vérifie (`SubjectFolderTest`).
- **Les questions de validation** : à la fin de sa dernière page, une notion se valide par 2 ou 3 de ses cartes
  corrigées par le serveur (vrai ou faux, QCM, code à compléter), prises dans l'ordre du fichier, une par format
  d'abord. Une notion du Tour en a donc au moins deux, et le parseur le vérifie.
- **La CI joue chaque exemple et chaque « À toi »** dans le bac à sable (job `runner`) : `cd runner && ./mvnw test
  -Dtest=TourTest` (Docker requis). Le job `content` compte les pages du Tour dans son résumé.
- En Go, tout paquet standard est permis ; ceux de `sandbox/go/warm/warm.go` s'exécutent plus vite. En C++, toute la
  bibliothèque standard de GCC 14 (`<print>` compris), et rien d'autre.

## 9. Liste de contrôle avant la pull request

- [ ] Le fichier s'appelle `source/<subject>-entretien-reference.md`.
- [ ] L'en-tête est complet ; le `subject` est définitif ; `status: draft` pour un nouveau sujet.
- [ ] Les chapitres sont numérotés `# N. TITRE` ; aucun chapitre existant n'a été renuméroté.
- [ ] Chaque chapitre a ses sections, et au moins une question.
- [ ] Les questions techniques ont une réponse courte, les questions de situation une réponse rédigée.
- [ ] Chaque QCM a de 2 à 8 choix, au moins une bonne réponse et une explication.
- [ ] Dans un dossier de sujet, chaque carte (sauf vrai ou faux et code à trous) a ses choix, avec une seule réponse
      défendable, et sa question tutoie (ADR 0051).
- [ ] Les règles de rédaction passent (`./mvnw test -Dtest=ContentQualityBaselineTest`) : le bon choix ne se repère pas
      à sa longueur, personne n'est vouvoyé, aucune question en double, un QCM sur quatre au plus à plusieurs réponses.
- [ ] Chaque bloc de code indique son langage ; le code des questions pièges a été exécuté.
- [ ] Dans un dossier de sujet, chaque « qu'affiche ce code ? » a sa vérification `check` (ou `unverifiable`).
- [ ] Chaque page du Tour s'exécute dans le bac à sable avec la sortie annoncée (`TourTest`), et chaque « À toi » part
      d'un code qui s'exécute sans donner la sortie attendue ; chaque notion du Tour a au moins deux cartes corrigées par
      le serveur.
- [ ] `./mvnw test -Dtest=ReferenceDocumentTest` est vert.
- [ ] Le rapport d'import ne liste ni anomalie ni retrait inattendu.
