# 0012. Gamification : registre de points, streak et événements entre modules

- Statut : accepté
- Date : 2026-10-29

## Contexte

Chaque révision doit rapporter des points et faire avancer l'objectif du jour, base du streak (CLAUDE.md,
section 6.3). Le module `gamification` doit donc savoir qu'une carte a été révisée. Le module `review` ne doit pas
dépendre de `gamification` pour autant : la révision a du sens sans points, et les groupes (phase 2) réagiront
aussi aux révisions. Deux autres contraintes s'ajoutent :
- une réponse rejouée (idempotence, ADR 0011) ne doit rien rapporter une seconde fois ;
- « le jour » est celui du fuseau de l'utilisateur.

## Options envisagées

1. **`review` appelle `gamification`** directement
   - ✅ Simple et explicite
   - ❌ `review` dépend d'un module qui dépend de lui pour la lecture : risque de cycle, et chaque nouveau consommateur
     modifie `review`
2. **Événement CDI synchrone**, publié par `review` et observé par `gamification` dans la même transaction
   - ✅ `review` ignore ses consommateurs ; l'événement est un type public de son package racine
   - ✅ Atomique : la révision et ses points sont enregistrés ensemble, ou pas du tout
   - ❌ Une erreur dans un observateur annule la révision ; les observateurs ralentissent la requête
3. **Outbox et traitement asynchrone**
   - ✅ Découplage complet, tolérance aux pannes
   - ❌ Infrastructure en plus (table outbox, traitement en tâche de fond, cohérence à terme) : prématuré pour un
     monolithe

## Décision

On choisit l'option 2.

- **Événement** : `com.recallio.review.CardReviewed` (utilisateur, identifiant de la révision, carte, instant). Il est
  publié une seule fois par réponse, uniquement quand elle est enregistrée pour la première fois.
- **Points** : registre `points_ledger` append-only (un déclencheur interdit les `UPDATE`) ; le total est une somme.
  - Barème : 10 points par carte, **quelle que soit la note** (récompenser « Facile » pousserait à surévaluer ses
    réponses), et rien pour la vitesse.
  - Bonus de 50 points quand l'objectif du jour est atteint.
  - L'unicité `(user_id, reason, source_id)` empêche de payer deux fois la même révision.
- **Activité quotidienne** : `daily_activity`, une ligne par utilisateur et par jour local. Le compteur est incrémenté
  en une seule instruction (`INSERT … ON CONFLICT DO UPDATE … RETURNING`). La journée réussie est figée avec
  l'objectif en vigueur, pour que changer l'objectif ne réécrive pas le passé.
- **Streak** : jours consécutifs avec l'objectif atteint, jusqu'à aujourd'hui. Le streak d'hier reste valable tant que
  la journée n'est pas finie ; un jour manqué le remet à zéro. Calcul en Java pur (`Streaks`), testé sur les
  changements de mois et d'heure.
- **Réglages** : `recallio.gamification.points-per-review`, `daily-goal` (10 cartes) et `daily-goal-bonus`.

## Conséquences

- ➕ `review` ne connaît pas `gamification` ; un futur module (groupes) pourra observer le même événement.
- ➕ Chaque point est explicable (une ligne du registre) et le total peut être recalculé à tout moment.
- ➖ Les observateurs sont dans la transaction de la révision : ils doivent rester rapides et ne pas échouer pour des
  raisons externes.
- ➖ Le jour d'une révision dépend du fuseau au moment de la révision : changer de fuseau peut créer ou sauter un jour.
  Cas rare, accepté.
- 🔁 Si un observateur devient lent ou dépend d'un service externe (notifications), passer à l'option 3 pour lui.
- 🔁 Le « gel de streak » (CLAUDE.md, 6.3) reste à faire : un jour gelé sera une ligne de plus, sans changer le calcul.
