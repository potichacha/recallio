# 0038. S'entraîner sur une notion : un entraînement libre, non enregistré

- Statut : accepté
- Date : 2026-11-05

## Contexte

Le Tour (ADR 0037) prévoit, depuis chaque page, un lien « S'entraîner sur cette notion » qui ouvre ses cartes. La
feuille de route le décrivait comme « un filtre par notion dans la file de révision ».

Une notion compte une trentaine de cartes (4 à 5 par format, ADR 0025). Or la file de révision n'en propose qu'une par
jour (ADR 0031) : les cartes sœurs, révisées coup sur coup, reçoivent des notes flatteuses, donc des échéances trop
lointaines. C'est aussi pour cela que les questions qui valident une notion du Tour ne sont pas enregistrées.

## Options envisagées

- **Un filtre par notion dans la file de révision, avec une exception à l'ADR 0031** :
  - la séance compte pour l'XP et la série ;
  - mais elle enchaîne trente cartes sœurs, que FSRS espace trop ;
  - ces cartes reviennent ensuite dues en même temps, et la règle d'une carte par jour les étale sur des semaines.
- **Un filtre par notion qui respecte l'ADR 0031** : cohérent, mais il propose une carte par jour, et le bouton ne sert
  presque à rien.
- **Un entraînement libre, non enregistré** (retenu) : toutes les cartes de la notion, tout de suite. Ces réponses ne
  sont pas des révisions, comme les questions de validation du Tour.

## Décision

**L'entraînement** (`GET /api/v1/me/practice/{sujet}/{notion}`) :
- donne toutes les cartes actives de la notion, quel que soit leur état FSRS ;
- les formats alternent dans l'ordre du fichier : une carte de chaque format à tour de rôle, plutôt que quatre vrai ou
  faux d'affilée ;
- les exercices de code, plus longs, viennent en dernier.

**La correction** :
- **vrai ou faux, QCM et code à trous** : le serveur les corrige (`POST …/checks`). La réponse n'est connue qu'après
  l'essai ;
- **exercice de code** : il s'essaie contre ses tests, avec la même exécution qu'en révision (`POST /api/v1/exercise-runs`,
  ADR 0030) ;
- **autres formats** : ils se retournent.

**Rien n'est enregistré** : ni le journal de révision, ni FSRS, ni l'XP, ni la série. Les cartes reviennent dans les
révisions espacées comme les autres, une par notion et par jour.

**L'accès** :
- il faut être connecté, comme pour toute action du Tour ;
- un lien y mène depuis chaque page du Tour et depuis chaque notion de la carte des compétences.

## Conséquences

- L'entraînement ne fait pas progresser : un utilisateur qui ne s'entraîne qu'ainsi n'a ni XP ni série. Les quêtes de
  la phase 3 ter pourront le récompenser, sans toucher à FSRS.
- La correction des questions du Tour et celle de l'entraînement partagent le même code (`AnswerCorrection`). La
  correction des révisions reste à part, car elle déduit une note FSRS.
- L'API publique du contenu sait chercher une seule notion (`CardDirectory.notion`), sans lire toutes celles du sujet.
  Le Tour s'en sert aussi, à chaque page ouverte.
- La solution d'un exercice n'est pas montrée : elle n'apparaît, comme aujourd'hui, qu'en révision, après avoir rendu
  son code.
