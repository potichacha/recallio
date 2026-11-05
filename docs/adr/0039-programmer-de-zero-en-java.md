# 0039. Programmer de zéro, en Java 25

- Statut : accepté
- Date : 2026-11-05

## Contexte

Le Tour (ADR 0037) sert deux publics : quelqu'un qui programme déjà suit le Tour d'un langage, et quelqu'un qui n'a
jamais programmé suit un Tour à part, « Programmer de zéro ». CLAUDE.md laissait ouverte la question du langage de ce
dernier (décision 12) : « Python est le choix classique, mais il demande son contenu et son image de bac à sable ; Go ou
Java sont déjà prêts. »

## Options envisagées

- **Python** : c'est le premier langage de nombreux cours, et sa syntaxe est légère. Mais tout serait à construire :
  - une image `sandbox/python` et le mode « programme » du runner pour elle ;
  - la vérification des « qu'affiche ce code ? » en CI ;
  - un sujet Python, encore « à rédiger ».
- **Go** : il est prêt et petit (25 mots-clés). Mais ses idiomes (pointeurs, erreurs en valeur de retour) se prêtent
  moins à une toute première approche, et l'application est d'abord tournée vers Java.
- **Java 25** (retenu) :
  - les **fichiers source compacts** (`void main() { IO.println("Bonjour"); }`) ont été conçus pour débuter : ni
    classe, ni `static`, ni `String[] args` à expliquer le premier jour ;
  - le bac à sable Java existe, et la CI y joue déjà chaque page ;
  - le Tour de Java prend naturellement la suite.

## Décision

**Le sujet** :
- `programmer-de-zero`, de catégorie Fondamentaux, est placé en tête de sa catégorie (position 150) ;
- son référentiel compte un domaine, « Apprendre à programmer », et sept chapitres : premiers pas, valeurs et
  variables, décisions, répétitions, fonctions, listes, aller plus loin ;
- ses 23 notions sont toutes débutantes et essentielles.

**Chaque notion** a :
- une courte leçon ;
- cinq cartes : une question de cours, deux vrai ou faux, deux QCM. Les quatre dernières, corrigées par le serveur,
  valident sa page (ADR 0037) ;
- une page du Tour, écrite pour quelqu'un qui découvre : une idée par page, un programme court, un « À toi » sur la
  plupart.

**Les programmes** sont des fichiers source compacts Java 25, avec `IO.println` ; les classes n'apparaissent pas, sauf
un `record` vers la fin.

**Le sujet entre en brouillon**, comme tout nouveau sujet (CLAUDE.md, 5.4) : on le relit dans l'application, puis une
seconde PR le publie.

## Conséquences

- Aucune nouvelle image ni nouvelle dépendance : `TourTest` joue ces pages comme celles du Tour de Java.
- Le contenu reste à enrichir pour atteindre la matrice de l'ADR 0025 (4 à 5 cartes par format) : la couverture du
  sujet apparaît dans le résumé du job `content`.
- Un Tour Python reste possible plus tard, comme sujet à part, quand le sujet Python sera rédigé et son bac à sable
  construit.
