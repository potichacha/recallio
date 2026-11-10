# 0053. C++ dans le bac à sable

- Statut : accepté
- Date : 2026-11-10
- Prolonge : ADR 0028 (les « qu'affiche ce code ? » vérifiés), ADR 0029 (le format des exercices), ADR 0030 (le bac à
  sable), ADR 0033 (Go, et un runner multi-langage), ADR 0037 (les programmes du Tour)

## Contexte

Le propriétaire veut C++ aussi complet que Java et Go : des exercices jugés par leurs tests, des « qu'affiche ce code ? »
vérifiés en CI, et un Tour. Le runner sait exécuter Java et Go. L'ADR 0033 annonçait qu'un troisième langage suivrait le
même chemin : une image, un lecteur de rapport, une extension, une coloration.

Une mesure guide la conception. Dans un conteneur limité à un processeur, une soumission de trois fichiers (un en-tête,
son code, ses tests) prend 3,5 secondes si doctest se compile avec elle. Un conteneur qui ne fait rien en prend déjà
0,5.

## Options envisagées

**Le compilateur** :

- **Clang** : des messages d'erreur souvent plus clairs.
- **GCC** (retenu) : le compilateur de référence sous Linux. Ubuntu le fournit en paquet, pour l'image comme pour la
  CI.

**L'image de base** :

- **`gcc:14`**, l'image officielle : 2 Go, car elle embarque tout un environnement de construction (git, bibliothèques
  de développement).
- **`ubuntu:24.04` et son paquet `g++-14`** (retenu) : 490 Mo. C'est aussi le compilateur qu'installe la CI, au
  correctif près.

**Sa version** :

- **GCC 15** : plus de C++23 et de C++26 (`std::flat_map`, `import std`), mais la CI devrait l'installer par une action
  tierce.
- **GCC 14** (retenu) : la CI en dispose sans rien d'autre, et il couvre le C++23 courant (`std::print`,
  `std::expected`, `std::ranges::to`, `this` déduit).

**Le framework des tests d'exercice** :

- **GoogleTest** : une bibliothèque à compiler, d'ordinaire avec CMake.
- **Catch2** : depuis sa version 3, une bibliothèque compilée elle aussi.
- **doctest** (retenu) : un seul en-tête, rapide à compiler, et un rapport au format JUnit, que le runner sait déjà lire.

**La vitesse d'une soumission** :

- **Compiler doctest à chaque fois**.
- **Précompiler, dans l'image, le `main` de doctest et son en-tête** (retenu). GCC n'utilise un en-tête précompilé que
  si les options sont les mêmes : un fichier, `sandbox/cpp/flags`, les fixe pour l'image comme pour la CI.

## Décision

**L'image `sandbox/cpp/`** :

- elle part d'`ubuntu:24.04` et de son paquet `g++-14`, et compile en C++23 (`-std=c++23 -O0`, dans `flags`) ;
- doctest 2.4.12 y entre vérifié par son empreinte, avec son `main` et son en-tête précompilés ;
- l'isolation est celle de l'ADR 0030, inchangée.

Son script, `run.sh`, suit le protocole des autres :

1. il lit une archive tar sur son entrée standard (`src/` et `tests/`) ;
2. il réunit le code et les tests à plat dans un même dossier ;
3. il compile : en cas d'échec, `@@recallio:compilation-error`, suivi des erreurs de GCC ;
4. sinon, il lance les tests, qui écrivent leur rapport JUnit dans un fichier, puis `@@recallio:report` suivi de ce
   rapport.

Un programme du Tour se compile depuis `src/` et se lance avec son délai, comme en Java et en Go. Sa sortie standard
part **ligne par ligne**, comme dans un terminal (`stdbuf -oL`). Sinon, écrite dans un fichier, elle resterait dans le
tampon du programme : une exception que personne ne rattrape l'arrête par `abort`, et tout ce qu'il avait affiché
serait perdu. Les « qu'affiche ce code ? » s'exécutent de la même façon.

Un plantage n'écrit rien de lui-même. Comme un terminal, `run.sh` nomme alors le signal qui a arrêté le programme
(`Segmentation fault`, `Aborted`, `Floating point exception`), et écarte les messages de `timeout`.

**Le runner** :

- la requête peut porter `language: CPP` ; l'image est `RECALLIO_SANDBOX_CPP_IMAGE` ;
- les fichiers sont des `.cpp` et des `.hpp` à plat ; les tests, et eux seuls, s'appellent `*_test.cpp` ;
- le rapport est lu par le lecteur JUnit. Le message d'un test raté est le texte que doctest écrit (le fichier, la ligne,
  l'assertion et ses valeurs), plutôt que son attribut `message`, qui ne donne que la décomposition (`0 == 1`) ;
- un plantage ou un `exit` en plein test laisse le rapport vide : le runner dit que les tests ne sont pas allés au bout.

**Les exercices C++**, au format de l'ADR 0029 :

- le code de départ et la solution sont des `.cpp` et des `.hpp` à plat ;
- les tests (`*_test.cpp`) incluent `doctest.h` et l'en-tête qu'ils testent (`#include "panier.hpp"`) ;
- seule la bibliothèque standard est disponible.

**Les « qu'affiche ce code ? » C++** :

- le code exécuté est un programme complet, avec ses `#include` et `main` : celui de `check.code`, ou à défaut le
  bloc ```` ```cpp ```` de la question ;
- `throws` nomme le type de l'exception que personne ne rattrape (`std::out_of_range`) ;
- une sortie qui dépend d'un comportement indéfini, ou du compilateur, se déclare `unverifiable` ;
- la CI (job `content`) installe GCC 14, et `CppSnippetRunner` lit les options dans `sandbox/cpp/flags`. En local, sans
  GCC 14, ces cartes sont ignorées ; en CI, elles échouent.

**Le backend et la PWA** : `CodeLanguage.CPP` accepte les `.cpp` et les `.hpp`. L'éditeur colore le C++ avec
`@codemirror/lang-cpp`, dans le même chunk chargé à la demande que Java et Go.

## Conséquences

- ➕ Un troisième langage sans refonte : une image, une extension, une coloration. Le lecteur de rapport JUnit sert à
  nouveau.
- ➕ Une soumission prend 1,2 seconde au lieu de 3,5, dont 0,5 pour démarrer le conteneur. L'image pèse 490 Mo, moins
  que celle de Java.
- ➖ **Le compilateur est choisi à deux endroits** : l'image (`ubuntu:24.04`) et le job `content` (`ubuntu-24.04`).
  Une montée de version les change ensemble ; GCC 15 demandera une autre version d'Ubuntu.
- ➖ Le C++ a des comportements indéfinis. Une carte dont la sortie en dépend est `unverifiable`, et un exercice ne
  teste que du code au comportement défini.
- ➖ Les messages de GCC changent avec la locale : en UTF-8, comme dans la CI, il cite le code entre guillemets
  typographiques, et entre apostrophes dans le bac à sable. Le contenu ne compare jamais le texte d'une erreur, il
  vérifie seulement qu'il y en a une (`compiles: false`).
- ➖ Un programme C++ plante souvent, et le noyau peut confier son image mémoire (core dump) à un programme de l'hôte,
  quand `kernel.core_pattern` commence par `|` (apport, systemd-coredump) : la limite du conteneur ne l'en empêche
  pas. Au déploiement, le serveur du runner les désactive.
- 🔁 La triche reste assumée (ADR 0030) : le code d'un exercice pourrait écrire un faux rapport, puis s'arrêter. En
  révision personnelle, cela ne nuit qu'au tricheur.
