# 0059. Qui apprend découvre une notion dans le Tour avant de la réviser

- Statut : accepté
- Date : 2026-11-12
- Prolonge : ADR 0032 (les parcours), ADR 0037 (le Tour), ADR 0052 (un premier login guidé par le but)

## Contexte

Le parcours « apprendre » (ADR 0032) range les nouvelles cartes du débutant vers l'expert et ne propose une notion que
lorsque ses prérequis sont acquis. Le Tour (ADR 0037) fait découvrir une notion : un texte, un programme à exécuter,
puis deux ou trois questions qui la valident.

Les deux ne se connaissaient pas. Quelqu'un qui apprend Go pouvait recevoir en révision des cartes sur les channels sans
avoir ouvert leur page du Tour : la révision espacée lui posait des questions sur ce qu'il n'avait jamais vu. Le
propriétaire a demandé, le 2026-11-06, que la révision suive le Tour pour qui apprend.

## Options envisagées

**Ce qui ouvre une notion à la révision** :

- **Avoir ouvert sa page du Tour** : on n'enregistre que la dernière page ouverte par sujet, pas chacune ; et ouvrir une
  page n'est pas l'avoir lue.
- **L'avoir validée dans le Tour** (retenu) : c'est la définition que le Tour donne lui-même d'une notion faite
  (CLAUDE.md, 6.6), et la donnée existe déjà (`tour_progress`).

**À qui la règle s'applique** :

- **À tout le monde** : qui prépare un entretien connaît déjà le langage, et n'a que faire de 81 pages de Tour avant sa
  première question sur les goroutines.
- **Au seul parcours « apprendre »** (retenu) : c'est lui qui s'adresse à qui découvre.

**Les notions déjà commencées** :

- **Les fermer aussi**, jusqu'à leur validation : quelqu'un qui révisait avant la règle perdrait ses nouvelles cartes
  du jour au lendemain.
- **Les laisser ouvertes** (retenu) : la règle ne ferme qu'une notion dont aucune carte n'a encore été vue.

## Décision

En parcours « apprendre », une notion **qui a des pages dans le Tour** n'entre en révision qu'une fois **validée dans
le Tour**.

- La règle est dans le domaine (`LearningPath.awaitsTour`), à côté de celle des prérequis. Elle s'y ajoute : une notion
  dont un prérequis manque reste verrouillée, validée ou non.
- Elle ne touche que les **nouvelles** cartes. Une carte due revient à son heure, comme toujours (ADR 0032).
- Une notion **sans page dans le Tour** (la plupart des notions intermédiaires et expertes) n'attend rien : ses
  prérequis suffisent.
- Une notion **déjà commencée** continue.
- La carte du parcours dit l'état de la notion : `TOUR_FIRST`, « À lire dans le Tour », avec un lien vers sa page.
- Quand une session n'a plus rien à proposer, le bilan renvoie au Tour : « Valide la notion suivante pour débloquer de
  nouvelles cartes. »

Aucune table, aucune route nouvelle : `NotionStatus` gagne une valeur, que la spec OpenAPI publie.

## Conséquences

- Pour qui apprend, le Tour devient la porte d'entrée de chaque notion, et la révision ce qui fait retenir ce qu'on
  vient d'y voir. L'accueil met déjà le Tour avant la révision pour ce parcours (ADR 0052).
- Un débutant qui ne valide rien dans le Tour n'a que les cartes des notions sans page. C'est voulu ; le bilan de la
  session et la carte du parcours le lui disent.
- Changer de parcours lève la règle : passer à « préparer un entretien » ouvre toutes les notions.
- Une notion validée puis sortie du Tour (ses pages retirées du contenu) n'a plus de page : elle n'attend plus rien.
- La file de révision lit les notions validées à chaque appel, en une requête de plus pour le parcours « apprendre »
  seulement.
- Publier « Programmer de zéro » s'appuiera sur cette règle : son public ne connaît aucune notion avant de l'avoir lue.
