# 0027. Formats texte : vrai ou faux, bug à corriger, compléter le code

- Statut : accepté
- Date : 2026-11-02

## Contexte

La refonte du contenu (ADR 0025) demande plusieurs formats par notion. L'application sait présenter cinq formats :
questions de cours, « qu'affiche ce code », mises en situation et définitions (auto-évaluées), QCM (corrigé par le
serveur, ADR 0019). L'étape 2 de la phase 3 bis ajoute trois formats texte : vrai ou faux, bug à corriger, compléter le
code. Pour chacun, il faut décider qui corrige.

## Options envisagées

Pour le **vrai ou faux** : l'auto-évaluation (rapide, mais rien n'empêche de se mentir) ou **la correction par le serveur**
(retenue) : une affirmation n'a qu'une bonne réponse.

Pour le **bug à corriger** : comparer automatiquement le code saisi au code corrigé (fragile : plusieurs corrections sont
justes), l'exécuter (c'est le bac à sable de l'étape 4), ou **l'auto-évaluation** (retenue pour l'instant) : on écrit sa
correction, puis on la compare à la correction modèle.

Pour **compléter le code** : l'auto-évaluation, ou **la correction par le serveur** (retenue) : chaque trou accepte une
liste de réponses, comparées sans tenir compte des espaces.

## Décision

**Vrai ou faux** (`TRUE_FALSE`) : un QCM à deux choix fixes, « Vrai » et « Faux ». Il réutilise tout ce que le QCM a
déjà : le stockage des choix (`card_version.choices`), la correction en révision (`selectedChoices`), le composant de la
PWA. Comme le QCM, il est admis dans les défis de groupe et les parties live (`CardType.GRADED_BY_CHOICES`).

```yaml
  - key: new-string-dans-le-pool
    format: true-false
    question: '`new String("a") == "a"` vaut `true`.'
    truth: false
    answer: "`new` crée toujours un nouvel objet, hors du pool."
```

**Bug à corriger** (`BUG_FIX`) : un code bogué en question, le code corrigé et l'explication en réponse. On peut écrire sa
correction avant de révéler la réponse, puis on s'auto-évalue, comme pour une mise en situation. La vérification par
exécution viendra avec le bac à sable (étape 4).

**Compléter le code** (`FILL_IN`) : chaque `___` de la question est un trou ; `blanks` donne, dans l'ordre, les réponses
acceptées par chaque trou. Le serveur corrige :

- une réponse est juste si elle est égale à l'une des réponses acceptées **aux espaces près** (`Objects.hash(x,y)` vaut
  `Objects.hash(x, y)`), la casse comptant comme dans du code ;
- la carte est réussie si tous les trous sont justes ; la correction montre, trou par trou, la réponse attendue ;
- les trous sont stockés avec la version (`card_version.blanks`, JSON) ; la file de révision ne donne que leur nombre
  (`blankCount`), jamais les réponses ; la réponse se fait par `filledBlanks`, une chaîne par trou.

```yaml
  - key: completer-hashcode-objects-hash
    format: fill-in
    question: |-
      ```java
      public int ___() { return Objects.___(x, y); }
      ```
    blanks: [[hashCode], [hash]]
    answer: "On redéfinit `hashCode` sur les mêmes champs qu'`equals`."
```

Un code à trous n'est pas encore proposé dans les défis ni en live : la saisie est plus lente qu'un choix, et le chrono
de ces modes est calibré pour des QCM.

## Conséquences

- Le vrai ou faux compte dans la diversité des formats corrigés par le serveur, qui servira au profil « vérifié » et au
  classé (ADR 0025).
- Un bug à corriger auto-évalué compte comme « déclaré », pas comme « vérifié », tant qu'il n'est pas exécuté.
- Un code à trous est corrigé par le serveur : il compte comme « vérifié ». Rédiger ses trous demande de prévoir les
  variantes justes (`compareTo` ou `compareToIgnoreCase`) : une réponse juste refusée décourage.
- Rédiger un vrai ou faux demande une affirmation sans ambiguïté : « toujours », « jamais » et les doubles négations
  sont à éviter (content/README.md).
