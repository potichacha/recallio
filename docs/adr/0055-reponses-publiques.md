# 0055. Les bonnes réponses restent lisibles sans compte

- Statut : accepté
- Date : 2026-11-11
- Prolonge : ADR 0046 (le classé), ADR 0051 (des choix pour toutes les questions), CLAUDE.md section 2.4
  (philosophie anti-triche)
- Origine : audit du 2026-11-10 (`docs/audit-2026-11.md`), élément 16

## Contexte

Le catalogue est public : `GET /api/v1/themes/{slug}/cards` renvoie chaque carte avec sa réponse et ses choix, le bon
compris, sans compte. C'est voulu depuis la phase 1 : on feuillette les cartes avant de s'inscrire.

Depuis, le classé, les duels, le mode live et les défis posent ces mêmes cartes dans un contexte compétitif. L'audit le
note : qui prépare un duel peut lire toutes les réponses du sujet. Aucune décision ne l'avait assumé.

## Options envisagées

- **Réserver les réponses aux utilisateurs connectés** : un compte est gratuit et se crée en une minute. Le tricheur
  les lirait quand même ; seul le visiteur honnête perdrait le catalogue.
- **Retirer les réponses du catalogue pour tous**, et ne les donner qu'après une réponse : le catalogue ne servirait
  plus à relire un thème. Les réponses resteraient de toute façon lisibles en révisant.
- **Un vivier de cartes réservé au classé**, absent du catalogue : c'est doubler le contenu à rédiger, alors que le
  contenu courant n'est pas fini (ADR 0051).
- **Les garder publiques et l'assumer** (retenu).

## Décision

Les réponses restent lisibles sans compte. Le classé ne repose pas sur leur secret, mais sur ce que le serveur mesure :

- **le temps** : 30 secondes par question en duel, mesurées par le serveur (ADR 0046) ; chercher la réponse coûte plus
  que la savoir ;
- **le nombre** : plusieurs milliers de cartes par sujet, tirées près du niveau du joueur ; les apprendre par cœur,
  c'est réviser ;
- **l'enjeu** : des rangs et des récompenses cosmétiques, rien qui vaille de tricher.

C'est la section 2.4 : on ne peut pas empêcher l'usage d'une IA ou d'internet dans un navigateur, et un classement
valorise la régularité plus que le score brut.

## Conséquences

- Le profil public distingue déjà le vérifié du déclaré (ADR 0043). « Vérifié » veut dire « corrigé par le serveur,
  dans le temps imparti », pas « sans aide ».
- Le catalogue reste soumis à la limite de débit des requêtes sans compte (ADR 0054) : le lire est permis, l'aspirer en
  boucle ne l'est pas.
- **Ce qui ferait revenir sur cette décision** : un enjeu réel (un classement utilisé par une entreprise pour évaluer,
  phase 4), ou une triche constatée pendant la bêta. La réponse serait alors un vivier réservé, pas un catalogue fermé.
