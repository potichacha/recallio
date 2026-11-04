# 0037. Le Tour : apprendre un langage pas à pas

- Statut : accepté
- Date : 2026-11-04

## Contexte

L'application sert trois publics (CLAUDE.md, section 5.5) : apprendre un langage, préparer un entretien, entretenir son
niveau. Pour le premier, le parcours « Apprendre » (ADR 0032) range les notions de la débutante à l'experte, et la leçon
d'une notion s'affiche dans la session de révision, avant sa première carte. Cette leçon est une fiche : courte, dense,
tournée vers l'entretien. Elle rappelle bien une notion qu'on a déjà rencontrée, mais elle n'apprend pas un langage à
quelqu'un qui le découvre.

Deux modèles montrent ce qui manque :
- **le Go Tour** : une suite de pages courtes, chacune avec son explication et un programme qu'on modifie et qu'on
  exécute dans la page, et des exercices en fin de chapitre ;
- **Ornikar** : chaque leçon se termine par quelques questions, et on sait où l'on en est.

Le Tour vise deux publics :
- quelqu'un qui programme déjà dans un autre langage : chaque langage a son Tour, au niveau du Go Tour ;
- quelqu'un qui n'a jamais programmé : un Tour « Programmer de zéro », à part, qui explique ce qu'est une variable ou une
  boucle.

## Options envisagées

**Où vivent les pages du Tour ?**
- Un arbre de pages à part (`content/<sujet>/tour/...`), relié aux notions : le récit est libre, mais il faut maintenir
  deux structures et vérifier qu'elles restent d'accord.
- **Un bloc `tour` dans le fichier de la notion** (retenu). Un fichier par notion reste la seule source (ADR 0026). L'ordre
  du Tour est celui du référentiel, qui est déjà un ordre d'apprentissage. Les cartes de la notion fournissent les
  questions de vérification et l'entraînement.

**Comment exécuter le code d'une page ?**
- Le mode actuel du runner, qui juge un exercice par ses tests : trop lourd pour un exemple, qui n'a pas de tests.
- **Un mode « programme » dans le runner** (retenu). Il compile, lance `main` et renvoie la sortie standard, la sortie
  d'erreur et le code de sortie, dans le même conteneur jetable et avec la même isolation (ADR 0030).

**Les questions de vérification comptent-elles comme des révisions ?**
- Oui, enregistrées dans le journal et dans FSRS. Mais une carte réussie juste après la leçon n'est pas retrouvée de
  mémoire : FSRS l'espacerait trop vite. Trois cartes d'une même notion le même jour contredisent aussi l'ADR 0031.
- **Non : corrigées par le serveur, sans être enregistrées** (retenu). Elles valident la page ; les cartes entrent
  ensuite dans les révisions comme les autres, espacées.

**Où vérifier les exemples ?**
- Avec JShell et la chaîne d'outils `go` de la machine, comme les « qu'affiche ce code ? » (ADR 0028) : c'est rapide,
  mais ce n'est pas l'environnement des utilisateurs (paquets précompilés de l'image Go, fichiers source compacts de
  Java 25).
- **Dans le bac à sable lui-même** (retenu), comme les exercices (`ExercisesTest`) : un exemple qui passe la CI
  s'exécute à l'identique pour les utilisateurs.

## Décision

**Une page** appartient à une notion. Son bloc `tour` contient une ou plusieurs pages, et chacune a :
- un titre court, facultatif (le titre de la notion par défaut) ;
- un texte écrit pour quelqu'un qui découvre le langage. Ce n'est pas la leçon, qui reste la fiche de révision ;
- un exemple : un programme complet et la sortie qu'il donne. On le modifie et on l'exécute dans la page ;
- facultativement, un « À toi » : une consigne, du code de départ, une solution et la sortie attendue. Le serveur
  exécute le programme de l'utilisateur et compare sa sortie à celle attendue, qui ne quitte pas le serveur.

Une notion sans bloc `tour` n'est pas dans le Tour.

**L'ordre** est celui du référentiel : domaines, chapitres, puis notions. La CI vérifie que les prérequis d'une notion
qui sont dans le Tour ont leur page plus tôt.

**Valider une page** :
- on répond à 2 ou 3 questions de la notion, corrigées par le serveur (vrai ou faux, QCM, code à compléter) ;
- elles sont prises dans l'ordre du fichier, une par format quand c'est possible ;
- elles ne sont pas enregistrées comme des révisions, et on peut réessayer ;
- la progression garde, par utilisateur, les pages validées et la dernière page ouverte.

**À la fin d'un chapitre**, on trouve ses exercices (ADR 0029), jugés par leurs tests. **Depuis chaque page**,
« S'entraîner » ouvre les cartes de la notion.

**Le mode « programme » du runner** (`POST /api/v1/programs`, réservé au backend comme le reste de son API) :
- il reçoit les fichiers d'un langage :
  - en Java, un `Main.java` à la racine (les fichiers source compacts de Java 25 sont permis) ;
  - en Go, un `package main` ;
- il compile, puis lance le programme, sans entrée standard, pendant 10 secondes au plus ;
- il renvoie :
  - le statut (terminé, erreur de compilation, délai dépassé, erreur du runner) ;
  - le code de sortie ;
  - les deux sorties, chacune tronquée à 64 Kio ;
- l'isolation et le délai du conteneur sont ceux des exercices, et les deux modes partagent la limite d'exécutions
  simultanées.

**Côté backend**, `POST /api/v1/playground-runs` est réservé aux utilisateurs connectés et transmet au runner. La limite
par utilisateur est celle des exercices : une exécution à la fois, exercices et programmes confondus (429 sinon). Un
utilisateur n'occupe donc jamais plus d'une des places du runner. La lecture du Tour (sommaire et pages) est publique,
comme le catalogue.

**La CI** joue dans le bac à sable :
- chaque exemple, dont la sortie doit être celle annoncée ;
- la solution de chaque « À toi », qui doit donner la sortie attendue ;
- le code de départ de chaque « À toi », qui ne doit pas la donner.

**Chaque langage** a son Tour, fait de contenu seulement. Ajouter un langage demande son contenu et son image de bac à
sable, comme Go avec l'ADR 0033. **Programmer de zéro** est un sujet à part (catégorie Fondamentaux), avec son
référentiel et son Tour, dans un langage choisi à son étape.

## Conséquences

- Le contenu grossit : environ une page par notion débutante (une centaine pour Go, un peu plus pour Java). Claude les
  rédige, le propriétaire les relit (ADR 0025), la CI les exécute.
- Une notion a deux textes, qui n'ont pas le même rôle : la leçon pour réviser, le texte du Tour pour découvrir.
- Tout utilisateur connecté peut faire exécuter son code, comme il le peut déjà pour les exercices. L'isolation du
  conteneur et la limite par utilisateur sont la protection. Chaque exécution coûte un conteneur (1 à 3 secondes).
- Les questions de vérification ne changent ni FSRS ni l'XP. Les quêtes et les succès (phase 3 ter) récompenseront le
  Tour.
- En Go, tout paquet standard est permis. L'image du bac à sable désactive l'index des modules (`GODEBUG=goindex=0`) :
  rangé dans le cache en lecture seule, il faisait échouer tout paquet absent de `sandbox/go/warm/warm.go` (« is not in
  std », rétro de la production Go). Un tel paquet se compile maintenant à chaque exécution, et toutes les exécutions
  coûtent environ 0,1 seconde de plus. Un paquet qui sert souvent s'ajoute à `warm.go`.
- Un programme qui sort lui-même avec le code 124 passe pour un délai dépassé : c'est le code que renvoie `timeout`.
