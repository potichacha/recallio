# 0018. Classement hebdomadaire : calculé par gamification, la régularité avant les points

- Statut : accepté (les points sont devenus l'XP : voir ADR 0035)
- Date : 2026-11-01

## Contexte

Chaque groupe a un classement de la semaine (CLAUDE.md, section 6.4), qui valorise la **régularité** plus que le score
brut (section 2.4). Le créateur en choisit la visibilité et l'objectif quotidien (ADR 0017). Les données utiles (cartes
révisées par jour local, points) appartiennent au module gamification ; les membres et les réglages, au module group.

## Options envisagées

Où calculer le classement :

1. **Dans group**, en lisant l'activité et les points par une API de gamification : impossible sans cycle, car
   gamification dépend déjà de review (événement `CardReviewed`), qui dépend de group (révision avec le groupe,
   ADR 0017).
2. **Dans gamification** (retenu), en lisant les membres et les réglages par une API publique de group
   (`group.GroupLeaderboards`) : les dépendances restent `gamification` → `group`, `review` → `group`, sans cycle.
   CLAUDE.md (section 4.1) range les classements dans group ; c'est l'écart que consigne cet ADR. L'adresse,
   `/api/v1/groups/{id}/leaderboard`, reste sous le groupe.

Comment classer :

1. **Lexicographique** (retenu) : jours réguliers, puis points, puis cartes révisées. Un membre présent cinq jours passe
   devant un membre qui a tout révisé en une soirée, quels que soient leurs points.
2. Un score pondéré (par exemple 100 points par jour régulier) : plus souple, mais les poids seraient arbitraires et
   difficiles à expliquer.

Pour les données : **SQL sur les tables existantes** (retenu), comme prévu à la phase 2 ; Valkey seulement si un besoin
réel apparaît (taille des groupes bornée à 100 membres).

## Décision

- Un **jour régulier** : un jour local où le membre a révisé au moins l'objectif quotidien **du groupe** (toutes ses
  révisions comptent, quel que soit le sujet : c'est la régularité qui est valorisée, pas un sujet en particulier).
- La **semaine** va du lundi au dimanche (ISO 8601), dans le fuseau de celui qui consulte. Les jours sont les jours
  locaux de chaque membre (`daily_activity`) ; les points sont ceux gagnés entre le lundi 0 h et le lundi suivant 0 h de
  ce fuseau.
- Rang : jours réguliers, puis points, puis cartes. Les ex æquo sur les trois critères partagent leur rang (1, 1, 3).
- Une seule requête pour tout le groupe (`JpaWeeklyActivity`) : chaque membre y figure, même inactif.
- Visibilité : **nominatif**, chacun voit tous les noms ; **anonymisé**, chacun voit son nom et les scores des autres,
  sans leurs noms ; **désactivé**, aucune ligne.
- L'énumération `LeaderboardVisibility` passe dans l'API publique du module group, que lit gamification.

## Conséquences

- Pour un membre d'un autre fuseau, les points du début et de la fin de semaine peuvent se décaler de quelques heures :
  acceptable pour un classement amical, et sans effet sur les jours réguliers.
- Changer l'objectif du groupe recalcule la semaine en cours (les compteurs quotidiens sont conservés, l'objectif est
  appliqué à la lecture).
- Les défis de groupe (étape 6) pourront réutiliser `WeeklyActivity` ou un port voisin, sans toucher au module group.
