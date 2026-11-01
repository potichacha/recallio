# 0019. Cartes QCM : un format dans les documents, la correction par le serveur

- Statut : accepté
- Date : 2026-11-01

## Contexte

Les QCM (`MULTIPLE_CHOICE`, CLAUDE.md, section 5.3) sont le premier format à **correction automatique**. Ils serviront
aussi aux défis de groupe (étape suivante) et au mode live (phase 3), seuls contextes compétitifs (section 2.4). Il faut
décider comment les écrire dans les documents, comment les stocker, et qui les corrige.

## Options envisagées

Pour le format dans les documents :

1. **Une section `### QCM`** (retenu), avec des choix en cases à cocher Markdown (`- [ ]`, `- [x]`) puis
   `R : explication`. Le document reste lisible tel quel sur GitHub pendant la relecture, et la section est facultative :
   un document existant reste valide.
2. Un bloc YAML par question : plus structuré, mais étranger au reste du document et plus pénible à relire.

Pour la correction :

1. **Par le client** : la file envoie les bonnes réponses, la PWA corrige et envoie une note. Simple, mais la bonne
   réponse est visible dans la réponse HTTP, et un défi ou une partie live ne pourrait pas s'y fier.
2. **Par le serveur** (retenu) : la file envoie les choix **sans** dire lesquels sont justes ni l'explication ; la PWA
   envoie les choix cochés ; le serveur corrige, en déduit la note et renvoie la correction.

## Décision

- Un QCM a **de 2 à 8 choix**, dont au moins un juste ; **plusieurs bonnes réponses** sont permises (la PWA affiche alors
  des cases à cocher). Une réponse est juste si elle coche **toutes les bonnes réponses et elles seules** : pas de demi-
  point, qui compliquerait la note FSRS.
- Une anomalie (moins de deux choix, aucune bonne réponse, explication manquante) est signalée par le parseur, donc par
  la CI de contenu.
- Les choix sont stockés avec la version de la carte (`card_version.choices`, `jsonb`), et ils entrent dans son
  empreinte : modifier un choix crée une nouvelle version, sans perdre la progression. Les cartes des autres formats
  gardent leur empreinte.
- Les colonnes JSON utilisent le format propre à Hibernate (`quarkus.hibernate-orm.mapping.format.global=ignore`),
  indépendant du JSON de l'API REST.
- **Note** : juste → `GOOD`, fausse → `AGAIN` (`Rating.ofGradedAnswer`). Ni « Difficile » ni « Facile » : le serveur ne
  sait pas avec quel effort la réponse a été trouvée. `POST /api/v1/reviews` reçoit `selectedChoices` **à la place** de
  `rating` ; envoyer une note pour un QCM, ou des choix pour une autre carte, répond **422** `review.answer-mismatch`.
- La réponse contient la **correction** (positions des bonnes réponses, explication), aussi quand la requête est rejouée
  (idempotence, CLAUDE.md, section 4.4).
- Le catalogue, qui montre déjà les réponses, montre les choix et marque les bons une fois la réponse révélée.
- Six premiers QCM sont rédigés : trois au chapitre 1 de Java, trois au chapitre 1 de Go.

## Conséquences

- Le nombre de bonnes réponses se devine (bouton radio ou cases à cocher) : c'est l'usage des QCM d'entretien, et cela
  évite de faire perdre un candidat sur la forme.
- L'ordre des choix est celui du document. Les mélanger à l'affichage (pour un défi ou une partie live) se fera côté
  serveur, sans changer le format.
- Les QCM s'ajoutent aux documents au fil des pull requests de contenu ; la CI les vérifie comme le reste.
