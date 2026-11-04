# 0035. L'XP par sujet

- Statut : accepté
- Date : 2026-11-04

## Contexte

Depuis la phase 1, chaque révision rapporte 10 points, quels que soient la note et le format. Atteindre l'objectif du
jour en rapporte 50. Les points sont des événements d'un registre append-only (`points_ledger`), et le classement
hebdomadaire des groupes les additionne après la régularité (ADR 0018).

La gamification v2 (ADR 0025) demande une **XP par sujet** qui rapporte « plus pour les formats exigeants, jamais pour
la vitesse ». Le propriétaire du projet a tranché deux questions au moment du plan de la phase 3 ter :
- **une seule monnaie** : l'XP remplace les points, et le classement de groupe additionne l'XP ;
- **une XP de base à chaque révision**, quelle que soit la note, comme les points aujourd'hui, et un **bonus pour une
  réussite corrigée par le serveur**.

## Options envisagées

- **Deux monnaies** (les points pour les groupes, l'XP pour la progression) : deux nombres à expliquer, qui montent
  ensemble presque toujours.
- **De l'XP seulement pour une réussite**, auto-évaluée comprise : elle pousserait à noter « Bien » une carte mal sue.
  Le barème actuel évite déjà ce piège en ne regardant pas la note.
- **Une XP de base selon l'effort du format, et un bonus quand le serveur corrige une réussite** (retenu) :
  l'auto-évaluation ne change rien à l'XP, et la réussite rapporte davantage quand elle est vérifiée.

## Décision

**L'XP remplace les points.** C'est le même registre append-only, et la table garde son nom (`points_ledger`), mais le
code et l'API parlent d'XP (`XpLedger`, `totalXp`). La PWA affiche « XP » partout, et le classement de groupe
additionne l'XP de la semaine.

**Le barème**, à partir d'une unité configurable (`recallio.gamification.xp-per-review`, 10 par défaut) :

| Formats | Effort | XP de base | Bonus d'une réussite corrigée par le serveur |
|---|---|---|---|
| Question de cours, définition, vrai ou faux, QCM | court | 10 | + 5 pour un vrai ou faux ou un QCM juste |
| « Qu'affiche ce code ? », code à compléter | code | 15 | + 8 pour un code à compléter juste |
| Mise en situation, bug à corriger | rédaction | 20 | aucun : ces formats s'auto-évaluent |
| Exercice de code | exercice | 40 | + 20 pour un exercice qui passe ses tests |

- L'XP de base vaut l'unité multipliée par l'effort du format (1 ; 1,5 ; 2 ; 4), arrondie. Le bonus vaut la moitié de
  l'XP de base, seulement pour un format corrigé par le serveur (QCM, vrai ou faux, code à compléter, exercice de code)
  et seulement quand la réponse est juste.
- **Jamais lié au temps** : le temps de réflexion est enregistré, mais ne rapporte rien.
- **L'objectif du jour** rapporte toujours un bonus (50 XP), qui n'appartient à aucun sujet.
- **L'XP d'un sujet** est la somme des entrées de ce sujet : chaque entrée garde le sujet de la carte révisée (colonne
  `subject_slug`, migration V19). Le total de l'utilisateur compte toutes les entrées, bonus du jour compris.
- **Le niveau d'un sujet** : passer du niveau n au niveau n + 1 demande 100 × n XP. On atteint ainsi le niveau 2 à
  100 XP, le niveau 3 à 300, le niveau 5 à 1 000, le niveau 10 à 4 500 et le niveau 20 à 19 000. Avec une vingtaine de
  cartes par jour, on passe le niveau 10 en trois semaines environ, et le niveau 20 en deux à trois mois.
- **L'événement `CardReviewed`** porte le sujet de la carte, son format et si elle est réussie (notée au moins
  « Bien », ou juste). Le module `gamification` en déduit le bonus, d'après le format (`CardType.isGradedByServer`).
- Les règles sont dans le domaine du module `gamification`, en Java pur : `XpRules` pour le barème, `SubjectLevel` pour
  le niveau.

## Conséquences

- Une seule monnaie. Le mode live garde ses « points » : ce sont le score d'une partie (ADR 0023), pas le registre.
- Les formats exigeants rapportent davantage : un exercice de code vaut quatre questions de cours. C'est voulu (ADR 0025 :
  donner envie de faire les exercices). Le classement de groupe reste d'abord une affaire de régularité.
- Changer l'unité ajuste tout le barème ; changer les rapports d'effort demande de modifier cet ADR.
- Les entrées enregistrées avant la migration n'ont pas de sujet : elles comptent dans le total, pas dans l'XP d'un
  sujet. Comme l'application n'est pas encore déployée, aucun utilisateur réel n'est concerné.
