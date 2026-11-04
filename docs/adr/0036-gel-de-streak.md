# 0036. Le gel de streak

- Statut : accepté
- Date : 2026-11-04

## Contexte

Le streak compte les jours consécutifs où l'objectif quotidien est atteint, dans le fuseau de l'utilisateur
(CLAUDE.md, section 6.3). Un seul jour manqué le remet à zéro : une journée de maladie ou de voyage efface des semaines
de régularité, et c'est souvent à ce moment-là qu'on abandonne. La gamification v2 (ADR 0025) prévoit donc un **gel de
streak**, « gagné avec le streak, consommé automatiquement un jour manqué ».

Le streak n'est stocké nulle part. C'est une projection des jours où l'objectif a été atteint (`daily_activity`, dont
la colonne `goal_reached` est figée au moment de la révision).

## Options envisagées

- **Acheter un gel avec de l'XP**, comme les gemmes de Duolingo : l'XP deviendrait une monnaie qu'on dépense, et le
  niveau d'un sujet baisserait. Or l'XP mesure la progression (ADR 0035), elle ne se dépense pas.
- **Un registre des gels gagnés et consommés** : il faudrait écrire chaque consommation au bon moment, soit par une
  tâche planifiée à minuit de chaque fuseau, soit à la révision suivante. En attendant, une lecture montrerait un état
  faux, ou devrait faire le calcul quand même.
- **Une projection des jours où l'objectif est atteint** (retenu) : comme le streak lui-même, rien à stocker ni à
  planifier, et la consommation est automatique.

## Décision

- **Gagner un gel** : chaque fois que le streak atteint un multiple de 7 jours (7, 14, 21…), un gel s'ajoute à la
  réserve. La réserve en garde 2 au plus : au-delà, le gel n'est pas gagné.
- **Consommer un gel** : pendant un streak, un jour passé sans l'objectif atteint consomme un gel. Le streak continue,
  sans compter ce jour. Sans gel en réserve, il retombe à zéro. Plusieurs jours manqués d'affilée consomment autant de
  gels.
- **Aujourd'hui** ne consomme rien tant que la journée n'est pas finie : on peut encore atteindre l'objectif.
- **Le calcul** reprend l'historique depuis le premier jour où l'objectif a été atteint. Il est fait dans le domaine du
  module `gamification`, en Java pur (`Streak`), à chaque lecture de la progression.
- **L'API** :
  - la progression donne les gels en réserve, les jours d'objectif atteint avant le prochain gel (absents quand la
    réserve est pleine) et si l'objectif atteint aujourd'hui vient de rapporter un gel ;
  - l'activité des derniers jours marque les jours couverts par un gel.
- **La PWA** :
  - les gels sont dans l'en-tête et sur la tuile du streak ;
  - un jour couvert porte un flocon dans les derniers jours ;
  - l'objectif atteint annonce le gel qu'il rapporte.

## Conséquences

- Aucune migration, aucune tâche planifiée. Le calcul parcourt quelques centaines de jours par année d'historique : il
  reste négligeable devant les lectures en base.
- Les règles (7 jours, 2 gels) s'appliquent à tout l'historique : les changer réécrirait les streaks passés. Elles sont
  donc fixées dans cet ADR et dans le domaine, pas dans la configuration.
- Le classement de groupe ne change pas : il compte les jours réguliers réels (ADR 0018), et un jour couvert par un gel
  n'en est pas un. La régularité se mesure, elle ne se protège pas.
- Le gel ne rapporte pas d'XP, et n'en coûte pas.
