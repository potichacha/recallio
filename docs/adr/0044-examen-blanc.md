# 0044. L'examen blanc

- Statut : accepté
- Date : 2026-11-05

## Contexte

L'indice de préparation (ADR 0041) dit si l'on sait les notions essentielles d'un sujet, d'après la mémoire de la
révision. CLAUDE.md (phase 3 ter, étape 8) prévoit aussi une épreuve : « une simulation d'entretien chronométrée, sur des
formats corrigés par le serveur, avec une note et le corrigé à la fin ».

Deux règles du produit s'appliquent : FSRS ne doit voir que les vraies révisions, espacées (comme pour le Tour, ADR
0037, et l'entraînement, ADR 0038) ; en révision personnelle, aucune mesure anti-triche (CLAUDE.md, 2.4).

## Options envisagées

- **Une session de révision chronométrée** : les réponses fausseraient FSRS (des cartes revues hors de leur échéance), et
  le journal de révision mélangerait épreuve et mémoire.
- **Une épreuve entièrement dans le navigateur** : pas de note fiable à garder, et les réponses justes seraient envoyées
  avec les questions.
- **Un examen tenu par le serveur** (retenu) : il tire les questions, garde le temps imparti, corrige et garde la note.
  Ni le journal, ni FSRS, ni l'XP ne voient les réponses.

## Décision

- **Les questions** : jusqu'à 20 cartes corrigées par le serveur (QCM, vrai ou faux, code à compléter) du sujet. Les
  exercices de code en sont exclus, trop longs pour une épreuve. Le tirage prend une question par notion à chaque tour,
  les notions essentielles d'abord, au hasard à priorité égale (`MockExamDraw`, graine tirée de l'identifiant de
  l'examen). Une carte sans notion (document Markdown) compte comme sa propre notion, de priorité courante. Il faut au
  moins 5 questions possibles (422 sinon).
- **Le temps** : une minute par question, mesurée par le serveur ; la PWA décompte le temps restant qu'il lui donne,
  jamais à partir de l'horloge du téléphone. Le temps écoulé, la PWA rend la copie telle quelle.
- **La copie** : toutes les réponses d'un coup. Une question sans réponse est fausse. Une copie arrivée après le temps
  imparti (30 secondes de marge pour le réseau) est notée, mais marquée « hors délai » : en révision personnelle, on ne
  punit pas, on informe. Une copie ne se rend qu'une fois (409).
- **Ce qui est gardé** : l'examen, ses questions (`mock_exam`, `mock_exam_question`) et sa note ; pas les réponses.
- **L'API** : `POST /api/v1/me/mock-exams` (commencer), `GET …/{id}` (reprendre après un rechargement),
  `POST …/{id}/submission` (rendre la copie : la note et le corrigé), `GET …?subject=` (les 10 dernières notes).
- **La PWA** : la page `/subjects/:slug/exam`, depuis la jauge « Prêt pour l'entretien » du tableau de bord. L'examen en
  cours est dans l'adresse (`?exam=`), pour le reprendre.

## Conséquences

- La note d'un examen est un résultat vérifié par le serveur : le profil public (ADR 0043) pourra montrer la meilleure,
  dans le vérifié.
- Un examen commencé et jamais rendu reste en base sans note ; il n'apparaît pas dans l'historique. Pas de nettoyage
  pour l'instant : une ligne par examen, et quelques-unes par question.
- Pas de quête ni de succès pour l'examen à ce stade : il mesure, il ne doit pas devenir une façon de gagner de l'XP.
- Les questions sont les mêmes cartes que la révision : un examen peut montrer une carte déjà vue le jour même. C'est
  voulu : l'entretien ne prévient pas.
