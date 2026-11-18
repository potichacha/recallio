# 0063. La fin de l'auto-évaluation : le serveur corrige chaque carte qu'on révise

- Statut : accepté
- Date : 2026-11-17
- Révise : ADR 0048 (le mode « questions corrigées », la note d'auto-évaluation), ADR 0051 (son point 7, la fin de la
  migration)

## Contexte

L'ADR 0051 a donné des choix à chaque format : une question de cours, un code, un bug, une situation ou une définition
se corrigent comme un QCM. Il prévoyait, à la fin de la migration, que l'auto-évaluation disparaisse et que les parties
réservées aux QCM s'ouvrent aux autres formats (son point 7).

La migration est finie. Toutes les cartes des dossiers de sujet ont leurs choix : Java, Go, C++, Python, Spring Boot,
Quarkus, Programmer de zéro. Seuls les documents Markdown de `content/source/` produisent encore des cartes sans choix.
Ce sont des sujets en brouillon, visibles des seuls admins, qui attendent leur réécriture en dossier (ADR 0050).

Jusqu'ici, deux critères différents décidaient de ce qu'une partie admettait :

- **le format** : `CardType.GRADED_BY_CHOICES` pour les défis et le live, `CardDifficulty.RATED_FORMATS` pour les
  duels, `EXAM_FORMATS`, `TrialService.FORMATS`, `TourQuestions.FORMATS` ;
- **la carte** : `CardSnapshot.isGradedByChoices()` pour la correction d'une réponse (ADR 0051).

Une question de cours à choix se corrigeait donc en révision, mais n'entrait ni dans un défi, ni dans un duel.

## Options envisagées

Pour les cartes Markdown sans choix :

- **Les garder en auto-évaluation**, seules de leur espèce : deux façons de répondre resteraient dans la PWA et dans
  l'API pour des sujets que personne d'autre qu'un admin ne voit.
- **Leur donner des choix dans le format Markdown** : un chantier de contenu sur des documents voués à être réécrits.
- **Ne plus les réviser** (retenu) : elles restent dans le catalogue, lisibles, et se révisent quand leur sujet est
  réécrit en dossier.

Pour le critère des parties :

- **Ajouter les formats un à un** aux listes existantes : une carte Markdown de ces formats, sans choix, y entrerait.
- **Un critère porté par la carte** (retenu) : ce que le serveur sait corriger d'elle, d'après sa version courante.

Pour le profil public, le vérifié comptait une réussite selon le format de la carte :

- **Compter toute réussite d'une carte qui a maintenant des choix** : un ancien « Je savais » deviendrait vérifié.
- **Regarder la version révisée** (retenu) : elle avait des choix, des trous ou des tests, ou non.

## Décision

1. **Un dossier de sujet n'a plus de carte sans choix.** Une carte `course`, `definition`, `code-output`, `bug-fix` ou
   `scenario` sans choix est une erreur d'import, signalée par la CI comme les autres. Un vrai ou faux a ses deux choix
   fixes, un code à trous ses trous. Les documents Markdown ne changent pas : leurs choix restent facultatifs.
2. **Le critère porte sur la carte.** `CardFilter` reçoit un critère de correction, `Grading`, lu sur la version
   courante :
   - `ANY` : toutes les cartes (le défaut) ;
   - `CHOICES` : des choix, pour répondre d'un geste sous un chrono court (défis de groupe, parties live) ;
   - `CHOICES_OR_BLANKS` : des choix ou un code à trous, sans exécuter de code (duels, examen blanc, essai, questions
     du Tour) ;
   - `SERVER` : des choix, un code à trous ou un exercice jugé par ses tests (la révision, l'entraînement, le carnet
     d'erreurs).

   `CardSnapshot` dit la même chose d'une carte chargée : `isGradedByChoices()`, `isGradedWithoutRunningCode()`,
   `isGradedByServer()`. Les listes de formats disparaissent.
3. **La révision ne sert que les cartes que le serveur corrige**, nouvelles comme dues : les cartes dues sont tirées des
   mêmes cartes éligibles que les nouvelles. Une carte Markdown sans choix n'est plus proposée.
4. **L'API n'a plus de note d'auto-évaluation.** `RecordReviewRequest` perd son champ `rating` ; la note se déduit de la
   correction (« Bien » si juste, « À revoir » sinon). Une réponse à une carte que le serveur ne sait pas corriger reçoit
   l'erreur 422 `review.answer-mismatch`. La file et l'entraînement n'envoient plus le verso avant la réponse : le champ
   `answer` disparaît de `QueuedCard` et de `PracticeCard`, l'explication arrive avec la correction. Le paramètre `type`
   de la file reste, pour les exercices et les groupes ; le mode « questions corrigées » de la PWA (ADR 0048) n'a plus
   de raison d'être.
5. **La cote de difficulté** (ADR 0046) apprend de chaque réponse corrigée par le serveur, sauf d'un exercice, qui ne
   passe pas en duel.
6. **Le vérifié du profil public** (ADR 0043) compte une carte réussie dans une version que le serveur corrigeait. Le
   module review lit ses versions réussies dans son journal ; le module content dit lesquelles se corrigent
   (`CardDirectory.cardsOfVersionsGradedByServer`), sans que l'un lise les tables de l'autre (ADR 0001). La maîtrise et
   le déblocage des notions (ADR 0032, 0034) gardent les réussites d'avant : elles relèvent du déclaré.

## Conséquences

- ➕ Personne ne se note soi-même : chaque révision, chaque réussite et chaque point d'XP viennent d'une correction.
- ➕ Les défis, le live, les duels, l'examen blanc, l'essai et les questions du Tour s'ouvrent à tous les formats : une
  question de cours, un « qu'affiche ce code ? » ou une mise en situation s'y posent comme un QCM.
- ➕ Un format ajouté demain n'a aucune liste à rejoindre : il suffit que ses cartes aient des choix.
- ➖ Les sujets Markdown en brouillon ne se révisent plus, sauf leurs QCM ; un admin qui les relit le fait dans le
  catalogue. Ils retrouvent la révision une fois réécrits en dossier (ADR 0050).
- ➖ Le tableau de bord compte encore les cartes de ces sujets ; il n'en montre aucune, puisqu'ils ne sont pas publiés.
- ➖ La PWA doit retirer l'auto-évaluation, la zone de texte et le mode « questions corrigées » (étape suivante, côté
  front et e2e).
- 🔁 `CardReviewed.gradedByServer` vaut toujours vrai ; il reste pour que chaque observateur dise ce qu'il exige.
