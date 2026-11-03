# 0032. Les parcours : apprendre, préparer un entretien, entretenir son niveau

- Statut : accepté
- Date : 2026-11-03

## Contexte

La refonte du contenu (ADR 0025) veut servir trois publics avec le même référentiel : **apprendre** un sujet qu'on ne
connaît pas, **préparer** un entretien, **entretenir** un niveau d'expert. Le référentiel donne à chaque notion un
niveau (débutant, intermédiaire, expert), une priorité (1 essentiel à 4 rare) et ses prérequis. Jusqu'ici, la file de
révision prenait les nouvelles cartes dans l'ordre du programme, le même pour tous.

## Options envisagées

- **Un parcours global par utilisateur** : simple, mais on ne peut plus apprendre Go tout en entretenant Java.
- **Un parcours par sujet suivi** (retenu) : le choix vit à côté du suivi, dans le module `user`.
- Pour **acquérir une notion**, ce qui la fait compter comme prérequis satisfait, trois options :
  - **toutes ses cartes vues** : avec une trentaine de cartes par notion, il faudrait des semaines pour la débloquer ;
  - **une maîtrise mesurée par FSRS** : c'est la maîtrise par notion de la gamification v2 (phase 3 ter), et ce serait
    l'anticiper ;
  - **un seuil de cartes réussies** (retenu).

## Décision

**Trois parcours** (`LearningTrack`), choisis pour chaque sujet suivi (`followed_subject.track`, migration V18,
`PUT /api/v1/me/followed-subjects/{slug}/track`). « Préparer un entretien » est le parcours par défaut, ce qui ne change
rien pour un sujet déjà suivi. Un sujet non suivi, ou révisé avec un groupe, suit lui aussi ce parcours par défaut.

| Parcours | Ordre des nouvelles cartes | Filtre |
|---|---|---|
| Apprendre | Du débutant vers l'expert, puis l'ordre du programme | Seulement les notions débloquées |
| Préparer un entretien | Par priorité, puis l'ordre du programme | Aucun |
| Entretenir son niveau | Intermédiaires et expertes d'abord, par priorité ; les débutantes en dernier | Aucun |

- **Les cartes dues ne changent pas** : elles passent toujours en premier, triées par retard. Le parcours ne range que
  les nouvelles cartes.
- **Une notion est acquise** quand 5 de ses cartes ont été réussies au moins une fois (notées Bien ou Facile), ou
  toutes si elle en a moins. Une notion sans carte compte comme acquise, sinon elle bloquerait tout ce qui en dépend.
- **Une notion est débloquée** quand tous ses prérequis sont acquis. Un prérequis retiré ou inconnu ne bloque rien.
- **Une carte déjà vue reste révisable** même si sa notion redevient verrouillée : le verrou ne touche que les
  nouvelles cartes.
- **Les cartes sans notion** (documents Markdown) gardent l'ordre du programme, quel que soit le parcours.
- **Le code** : les règles sont dans le domaine du module `review`, en Java pur. `LearningPath` dit ce qui est acquis
  et débloqué, `NewCardOrder` range les cartes. Elles s'appuient sur l'API publique de `content` (`NotionInfo` :
  niveau, priorité, prérequis, nombre de cartes) et sur celle de `user` (`LearningTrack`).

**Les leçons et la carte du parcours :**
- **La leçon d'une notion** s'affiche avant sa toute première carte, en parcours « apprendre » seulement. Elle ne
  compte pas comme une révision, et la file ne propose qu'une carte par notion (ADR 0031) : la leçon n'apparaît qu'une
  fois. Les autres parcours vont droit aux cartes.
- **La carte du parcours** (`GET /api/v1/learning-paths/{sujet}`, page « Mon parcours » de la PWA) donne chaque notion
  par chapitre avec son état :
  - *à venir* : pas encore de carte ;
  - *verrouillée* : un prérequis non acquis, en parcours « apprendre » seulement ;
  - *disponible* ;
  - *en cours* : des cartes vues ;
  - *acquise*.

## Conséquences

- Le même référentiel sert trois publics, sans dupliquer le contenu.
- Le seuil de 5 cartes est une première mesure, simple et explicable. La maîtrise par notion (phase 3 ter) pourra le
  remplacer, en tenant compte de la mémoire FSRS et de la diversité des formats.
- Un débutant en parcours « Apprendre » ne voit, au départ, que les notions sans prérequis : la qualité des prérequis
  du référentiel devient importante (la CI vérifie déjà qu'ils existent et ne forment pas de cycle).
- Les leçons des notions restent à rédiger : c'est le pilote (phase 3 bis, étape 6). Une notion sans leçon passe
  directement à ses cartes.
