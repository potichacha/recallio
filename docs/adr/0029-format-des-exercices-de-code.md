# 0029. Format des exercices de code

- Statut : accepté
- Date : 2026-11-02

## Contexte

La matrice de contenu (ADR 0025) vise au moins un exercice de code par chapitre : un énoncé, du code de départ en un ou
plusieurs fichiers, une solution modèle, et des tests qui jugent le code. L'étape 4 de la phase 3 bis les apporte en
quatre temps : le format et l'import (cette décision), le bac à sable d'exécution (ADR à venir), la correction en
révision, puis l'éditeur dans la PWA.

Un exercice ne tient pas dans une carte YAML : ses fichiers sont du vrai code, que l'auteur doit pouvoir compiler et
tester avec ses outils habituels, et dont la relecture en pull request doit montrer un diff lisible.

## Options envisagées

- **Le code dans le fichier YAML de la notion**, en blocs `|` : un seul fichier par notion, mais du Java indenté dans du
  YAML, impossible à ouvrir dans un IDE, et un diff noyé dans le reste des cartes.
- **Un dossier par exercice, dans le dossier de la notion** (`notions/<domaine>/<notion>/exercises/<clé>/`) : proche des
  cartes, mais `notions/` ne contiendrait plus seulement des fichiers YAML de notion.
- **Un dossier par exercice, dans un dossier `exercices/` à côté de `notions/`** (retenu) :
  `exercices/<domaine>/<notion>/<clé>/`, avec les mêmes domaines et notions que `notions/`.

## Décision

```
content/java/exercices/collections/choisir-collection/sans-doublons/
├── exercise.yaml        # statement (l'énoncé) et explanation (l'explication de la solution), en Markdown
├── starter/Doublons.java
├── solution/Doublons.java
└── tests/DoublonsTest.java
```

- Un exercice est une carte de type `CODING_EXERCISE`, rattachée à sa notion. Son identifiant dérive du sujet, de la
  notion et du nom de son dossier, comme une carte à clé (ADR 0026). La clé ne doit donc pas être déjà celle d'une carte
  de la notion.
- Le référentiel doit retenir le format `exercise` pour la notion. Une notion sans liste `formats` les retient tous.
- **Les fichiers** :
  - `starter/` est le code qu'on modifie ;
  - `solution/` a exactement les mêmes fichiers, résolus ;
  - `tests/` contient des tests JUnit 5, qui ne remplacent aucun fichier du code de départ.
  - Chaque partie compte de 1 à 10 fichiers `.java` de 20 000 caractères au plus. Le chemin peut contenir des packages
    (`fr/recallio/Panier.java`), jamais de `..`.
- **Le stockage** : les trois parties sont rangées en JSON dans `card_version.exercise` (migration V17). Elles font
  partie de l'empreinte de la version : modifier un test crée une nouvelle version.
- **L'exposition** : le catalogue les expose (`Card.exercise`), et la file de révision aussi (`QueuedCard.exercise`).
- **En attendant le bac à sable**, l'exercice est **auto-évalué**, comme le prévoit CLAUDE.md (section 5.3) :
  - on lit l'énoncé, le code de départ et les tests, puis on révèle la solution et on se note ;
  - la file de révision envoie donc la solution, comme la réponse de toute carte auto-évaluée, et la PWA ne la montre
    qu'une fois révélée.
  - *Mise à jour* : depuis l'arrivée du runner, l'exercice est **jugé par ses tests** et la solution n'est envoyée
    qu'avec la correction (ADR 0030, « Correction en révision »).
- **Les exclusions** : comme le code à trous, l'exercice est exclu des défis et des parties live.
- **La couverture** : le rapport de couverture compte les exercices **par chapitre**, pas dans la matrice par notion.

## Conséquences

- Un exercice se rédige comme du code ordinaire : on peut ouvrir son dossier dans un IDE, compiler et lancer ses tests.
  La CI vérifiera, une fois le bac à sable en place, que la solution passe les tests et que le code de départ y échoue.
- Seul Java est pris en charge (fichiers `.java`, tests JUnit 5). Un autre langage demandera d'élargir le format, par un
  nouvel ADR.
- Quand le serveur jugera les exercices, la file de révision cessera d'envoyer la solution avant la soumission : son
  DTO (`ExerciseToSolve`) est déjà distinct de celui du catalogue pour cette raison.
