# 0062. Python dans le bac à sable

- Statut : accepté
- Date : 2026-11-17
- Prolonge : ADR 0028 (les « qu'affiche ce code ? » vérifiés), ADR 0029 (le format des exercices), ADR 0030 (le bac à
  sable), ADR 0033 (un runner multi-langage), ADR 0037 (les programmes du Tour), ADR 0053 (C++, le modèle suivi)

## Contexte

L'étape 7.3 de la feuille de route demande un sujet Python aussi complet que Java, Go et C++. Il lui faut des
exercices jugés par leurs tests, des « qu'affiche ce code ? » vérifiés en CI, et un Tour. Le runner sait exécuter
Java, Go et C++. L'ADR 0053 a montré le chemin d'un nouveau langage : une image, un lecteur de rapport, une extension
et une coloration.

Python diffère des trois autres sur un point : il n'a pas de compilation à part. Une faute de syntaxe ne se voit qu'au
chargement du module, et une erreur d'import qu'au moment où les tests le chargent.

## Options envisagées

**L'image de base** :

- **`ubuntu:24.04`**, comme pour C++ : la version de Python d'Ubuntu 24.04 est la 3.12. Une plus récente demanderait
  un dépôt tiers (deadsnakes).
- **`python:3.14-slim`**, l'image officielle (retenue) : 181 Mo, sur Debian 13. Au contraire de `gcc:14` (2 Go), elle
  est légère. La CI installe la même version avec `actions/setup-python`, une action officielle de GitHub.

**La version de Python** :

- **3.15**, sortie en octobre 2026.
- **3.14** (retenue) : un an de correctifs. C'est aussi la version que les rédacteurs du contenu connaissent le mieux,
  ce qui compte pour un contenu écrit en partie de mémoire et relu ensuite.

**Le framework des tests d'exercice** :

- **`unittest`**, dans la bibliothèque standard : rien à installer, mais des classes et des `self.assertEqual`, loin de
  ce qu'on écrit aujourd'hui.
- **pytest** (retenu) : le standard de fait, celui qu'on interroge en entretien. Ses tests sont de simples fonctions
  avec `assert`, et il écrit un rapport au format JUnit, que le runner sait déjà lire.

## Décision

**L'image `sandbox/python/`** :

- elle part de `python:3.14-slim` et installe pytest avec ses dépendances, aux versions et aux empreintes de
  `requirements.txt` (écrit par `pip-compile` depuis `requirements.in`). pip refuse un paquet dont l'empreinte diffère,
  et Dependabot tient le fichier à jour ;
- le code s'exécute en UTF-8 quelle que soit la locale (`PYTHONUTF8`), avec des sorties sans tampon
  (`PYTHONUNBUFFERED`), comme dans un terminal, et sans `.pyc` écrits ;
- l'isolation est celle de l'ADR 0030, inchangée.

Son script, `run.sh`, suit le protocole des autres langages :

1. il lit une archive tar sur son entrée standard (`src/` et `tests/`) ;
2. il réunit le code et les tests à plat dans un même dossier ;
3. **il vérifie la syntaxe de chaque fichier**, comme le ferait un compilateur. En cas d'erreur, il écrit
   `@@recallio:compilation-error`, suivi du message de Python (le fichier, la ligne, le caret), sans pile ;
4. il lance pytest, sans son cache, avec des piles courtes et un rapport JUnit. **Quand pytest ne peut pas charger un
   fichier de tests** (un nom introuvable à l'import, une exception au chargement du module), il n'a rien exécuté.
   `run.sh` rend alors la section des erreurs de pytest comme une erreur de compilation, sans les cadres de la
   bibliothèque standard ;
5. sinon, il écrit `@@recallio:report`, suivi du rapport.

Un programme du Tour est un `main.py`, avec d'autres modules à plat qu'il importe. Sa syntaxe est vérifiée de la même
façon, puis il est lancé avec son délai, sans entrée standard. Une exception que personne ne rattrape laisse sa pile
dans la sortie d'erreur, avec le code de sortie 1, comme dans un terminal.

**Le runner** :

- la requête peut porter `language: PYTHON` ; l'image est `RECALLIO_SANDBOX_PYTHON_IMAGE` ;
- les fichiers sont des modules `.py` à plat, au nom d'identifiant Python (on les importe par leur nom). Les tests, et
  eux seuls, s'appellent `*_test.py`, un nom que pytest découvre de lui-même ;
- un programme a un `main.py` ;
- le rapport est lu par le lecteur JUnit. Le message d'un test raté est le texte de pytest : la ligne du test,
  l'assertion et ses valeurs (`E   assert -1 == 5`) ;
- un `os._exit` en plein test laisse le rapport vide : le runner dit que les tests ne sont pas allés au bout.

**Les « qu'affiche ce code ? » Python** :

- le code exécuté est un programme complet : `check.code`, ou à défaut le bloc ```` ```python ```` de la question ;
- `throws` nomme le type de l'exception que personne ne rattrape, lu à la dernière ligne de la pile (`KeyError`) ;
  une erreur de syntaxe répond à `compiles: false` ;
- une sortie qui dépend de l'ordre d'un `set` de chaînes, d'une adresse ou de l'heure se déclare `unverifiable` ;
- la CI (job `content`) installe Python 3.14, et `PythonSnippetRunner` l'exige : les messages d'erreur changent d'une
  version de Python à l'autre. En local, sans Python 3.14, ces cartes sont ignorées ; en CI, elles échouent.

**Le backend et la PWA** : `CodeLanguage.PYTHON` accepte les `.py`, et une page du Tour Python a un `main.py`.
L'éditeur colore Python avec `@codemirror/lang-python`, dans le même chunk chargé à la demande que les autres langages.

## Conséquences

- ➕ Un quatrième langage sans refonte : une image, une extension, une coloration. Le lecteur de rapport JUnit sert une
  troisième fois.
- ➕ Une soumission se juge en 1 à 1,5 seconde, conteneur compris, et un programme du Tour en une demi-seconde. L'image
  pèse 214 Mo, la plus légère des quatre.
- ➕ Le joueur lit pourquoi rien n'a tourné : une erreur de syntaxe, ou un nom que ses tests ne trouvent pas, au lieu
  d'un échec sans message.
- ➖ **La version de Python est choisie à deux endroits** : l'image (`python:3.14-slim`) et le job `content`
  (`setup-python`). Dependabot ne propose pas de nouvelle version mineure : on les monte ensemble, à la main.
- ➖ L'ordre d'un `set` de chaînes change d'une exécution à l'autre (le hachage des chaînes est aléatoire). Le bac à
  sable ne le fige pas, puisque le joueur doit voir Python tel qu'il est : le contenu évite ces sorties, ou les déclare
  `unverifiable`.
- ➖ pip reste dans l'image. Sans réseau, il ne peut rien installer.
- 🔁 La triche reste assumée (ADR 0030) : le code d'un exercice pourrait écrire un faux rapport, puis s'arrêter. En
  révision personnelle, cela ne nuit qu'au tricheur.
