# 0020. Défis de groupe : une série de QCM commune, un délai par carte mesuré par le serveur

- Statut : accepté
- Date : 2026-11-01

## Contexte

Un défi de groupe propose « une série de cartes commune, avec un délai de réponse par carte » (CLAUDE.md, section 6.4).
C'est le premier contexte compétitif du produit, avant le mode live de la phase 3 (section 2.4) : la comparaison doit
être équitable, sans surveillance intrusive.

## Options envisagées

Pour les cartes du défi :

1. **Des QCM seulement** (retenu) : la correction est automatique (ADR 0019), donc la même pour tous ; un délai a un
   sens. C'est aussi ce que prévoit le mode live (section 6.5).
2. Tous les formats : une carte en auto-évaluation chronométrée se note soi-même, ce qui rend la comparaison vide.

Pour le délai :

1. **Mesuré par le serveur** (retenu) : l'heure de présentation de chaque carte est enregistrée ; recharger la page ne
   relance pas le chrono, et une réponse hors délai compte comme fausse.
2. Mesuré par le navigateur : simple, mais il suffit de recharger la page pour gagner du temps.

## Décision

- Le créateur lance un défi : un titre, **3 à 20 QCM**, **10 à 120 secondes** par carte. La série est **tirée au
  hasard** à la création parmi les QCM des sujets et thèmes du groupe (son réglage de formats est ignoré : un défi ne
  contient que des QCM) ; elle est la même pour tous, dans le même ordre. Pas assez de QCM : **422**
  `group.not-enough-cards`.
- Un défi est **ouvert sept jours**. Chaque membre le joue **une fois**, carte après carte :
  - `POST /api/v1/challenges/{id}/next` rend la carte en cours si son délai court encore, sinon présente la suivante
    (son chrono démarre) ; une carte présentée et restée sans réponse au-delà du délai compte comme fausse ;
  - `POST /api/v1/challenges/{id}/answers` corrige la carte présentée ; la renvoyer rend la correction déjà
    enregistrée (idempotence, section 4.4).
- Une **tolérance réseau de 2 secondes** s'ajoute au délai : sur mobile, le trajet de la réponse ne doit pas faire perdre
  un point.
- Le client affiche un compte à rebours à partir du **temps restant** donné par le serveur, jamais à partir de son
  horloge : un écart d'horloge entre le téléphone et le serveur ne change rien.
- **Classement du défi** : les parties terminées, par score puis par temps total ; les ex æquo partagent leur rang. Il
  suit la visibilité choisie pour le groupe (ADR 0018) : nominatif, anonymisé, ou seulement sa propre partie si le
  classement est désactivé.
- Les défis **ne touchent pas à la révision personnelle** : ni note FSRS, ni points. Un défi mesure, il ne planifie pas.
- Tables `challenge`, `challenge_card` (la série) et `challenge_answer` (une ligne par carte présentée à un membre), dans
  le module group ; supprimer le groupe supprime ses défis.

## Conséquences

- Ouvrir le défi dans deux onglets ne donne pas deux chances : la carte présentée est la même, et sa première réponse
  fait foi.
- Les choix restent dans l'ordre du document ; les mélanger par membre se fera côté serveur si le besoin apparaît.
- Le mode live (phase 3) réutilisera la correction et la mesure du délai par le serveur, en synchronisant les membres.
