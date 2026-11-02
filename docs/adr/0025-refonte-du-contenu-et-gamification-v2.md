# 0025. Refonte du contenu (référentiel, notions, niveaux), gamification v2 et classé

- Statut : accepté
- Date : 2026-11-02

## Contexte

Le contenu vient de deux documents d'entretien (Java, Go) : environ 2 000 cartes, presque toutes des questions de cours
et des mises en situation, 6 QCM, aucun exercice de code. Le propriétaire du projet veut que l'application serve trois
publics avec le même contenu :

- quelqu'un qui **ne connaît pas** le langage et veut l'apprendre ;
- quelqu'un qui le connaît et **prépare un entretien** ;
- un **expert** qui veut **entretenir son niveau**.

Il veut couvrir **tout** le langage, l'essentiel d'abord, chaque notion sous plusieurs formes de questions et avec
plusieurs cartes par forme ; des cartes où l'on **code**, parfois dans plusieurs fichiers ; une gamification qui donne
envie de faire les exercices et de partager ses résultats ; un système classé avec des rangs.

## Options envisagées

Pour l'unité de contenu :

1. Continuer d'enrichir les documents d'entretien : rien ne garantit la couverture, et le format ne sert qu'un public.
2. **Un référentiel de notions par sujet** (retenu) : l'inventaire complet d'abord, le contenu ensuite, la couverture
   mesurée. C'est l'évolution prévue « à terme » par CLAUDE.md (section 5.2) ; le parseur est déjà isolé derrière une
   interface.

Pour la rédaction à l'échelle (10 000 à 15 000 cartes pour « tout Java ») :

1. À la main : des mois de travail.
2. **Brouillons rédigés par Claude, relus par le propriétaire** (retenu), avec des contrôles automatiques en CI (forme,
   couverture, exécution du code). C'est l'aide à la rédaction prévue en phase 5, avancée.

Pour le classé : un rang global, ou **un rang par sujet** (retenu : on peut être Or en Java et Argent en Go).

## Décision

**Contenu (phase 3 bis).**

- Un **référentiel** par sujet, `content/<sujet>/referentiel/*.yaml` : domaines → chapitres → notions. Chaque notion a un
  identifiant définitif, un **niveau** (`beginner`, `intermediate`, `expert` : un par public) et une **priorité** :
  - 1, essentiel ;
  - 2, courant ;
  - 3, avancé ;
  - 4, rare.

  Elle a aussi ses **prérequis** et, si elle n'admet pas tous les formats, la liste de ceux qui lui conviennent. La CI le
  valide.
- Un **sujet est un langage ou un outil**. Le référentiel Java couvre le langage, la JVM, la bibliothèque standard et
  l'outillage Java (tests, build, modules). Les chapitres du document Java consacrés à Spring, Quarkus, Jakarta EE, aux
  API REST, aux bases de données, à Docker, Kubernetes, la CI/CD, l'observabilité, la sécurité, le réseau, le cloud,
  Kafka, le cache, les algorithmes, la conception (SOLID, patterns, architecture) et aux questions comportementales
  deviennent des **sujets propres** du catalogue lors de la migration.
- Une notion porte une courte **leçon** et des cartes de **formats variés** : cours, vrai ou faux, QCM, compléter le
  code, qu'affiche ce code, bug à corriger, mise en situation, définition, exercice de code.
- **Matrice** : **4 à 5 cartes par format applicable** à chaque notion, et au moins un exercice de code par chapitre.
- **Production par priorité** : les notions de priorité 1 de tous les chapitres d'abord.
- **Exercices de code** : un dossier (énoncé, fichiers de départ, solution modèle, tests), un ou plusieurs fichiers. La
  CI vérifie que la solution passe les tests et que le départ y échoue ; elle exécute aussi chaque « qu'affiche ce
  code ». Le code des joueurs est compilé et testé côté serveur, dans un **bac à sable isolé**, avancé depuis la phase 5
  (son choix technique fera l'objet d'un ADR).
- **Parcours** : apprendre (prérequis, leçons, déblocage), préparer un entretien, entretenir son niveau. Deux cartes d'une
  même notion ne tombent pas le même jour.
- **Rédaction** : Claude rédige les brouillons ; **chaque PR de contenu est relue par le propriétaire** avant le merge.

**Gamification v2, partage, classé (phase 3 ter).**

- Carte des compétences, **maîtrise par notion** (mémoire FSRS et diversité des formats réussis), XP par sujet (plus pour
  les formats exigeants, jamais pour la vitesse), quêtes qui font varier les formats, succès, gel de streak.
- Cartes de partage ; **profil public de compétences, privé par défaut** (RGPD, protection des données dès la conception),
  que chacun rend public ou privé à tout moment. Le profil distingue le **vérifié** (classé, live, défis, exercices jugés
  par le serveur) du **déclaré** (auto-évaluation).
- **Classé, un rang par sujet** : Bronze → Argent → Or → Platine → Diamant → Maître, en divisions ; cote Glicko-2
  cachée, matchs de placement, saisons aux récompenses cosmétiques. Duels asynchrones d'abord (la même série qu'un
  adversaire de cote proche, joué plus tôt), classé en direct ensuite. Uniquement des formats corrigés par le serveur,
  chrono mesuré par le serveur, questions adaptées par une cote de difficulté des cartes.

**Ordre** : refonte du contenu, puis gamification v2, partage et classé, puis le déploiement.

## Conséquences

- CLAUDE.md change : le classé devient un second contexte compétitif à côté du live (section 2.4) ; le modèle de
  contenu (section 5.5), la gamification (section 6.3) et la feuille de route (section 9) sont mis à jour.
- La révision personnelle reste sans prime à la vitesse (section 6.3) : seuls le live et le classé la comptent.
- La migration vers les notions change l'identité des cartes. Aucun utilisateur réel n'a encore de progression, puisque
  le déploiement vient à la fin : c'est le moment de le faire.
- Le volume est le risque principal : la production se fait priorité par priorité, et la CI mesure l'avancement.
