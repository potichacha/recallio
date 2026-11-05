# 0045. Les cartes de partage

- Statut : accepté
- Date : 2026-11-05

## Contexte

CLAUDE.md (6.3, phase 3 ter, étape 12) prévoit des **cartes de partage** : chapitre maîtrisé, podium, promotion. Une
image qu'on publie sur un réseau ou qu'on envoie à un ami fait connaître l'application, et récompense un vrai progrès.

Le profil public (ADR 0043) est privé par défaut : une carte de partage ne doit rien publier que l'utilisateur n'ait
choisi d'envoyer.

## Options envisagées

- **Des images générées par le serveur**, à une adresse publique (une balise Open Graph) : il faudrait une bibliothèque
  de rendu côté serveur, et une adresse publique par exploit, donc des exploits publics par défaut.
- **Un service tiers de génération d'images** : il recevrait les données de l'utilisateur.
- **Une image dessinée dans le navigateur** (retenu), puis confiée au partage du téléphone (Web Share API) ; sinon
  téléchargée ; sinon le texte copié. Rien ne quitte l'appareil sans un geste de l'utilisateur.

## Décision

- **L'image** (`shareCard.ts`) : 1200 × 630, le format des aperçus de liens ; le nom de l'application, d'où vient
  l'exploit, l'exploit en gros, son détail et l'adresse de l'application. Dessinée sur un `canvas`, sans dépendance.
- **Le partage** (`ShareButton`) : avec l'image quand le navigateur sait partager un fichier ; sinon l'image est
  téléchargée ; sans image, le texte est partagé ou copié. Fermer la fenêtre de partage n'est pas une erreur.
- **Les exploits** :
  - un chapitre dont toutes les notions sont maîtrisées, sur la carte des compétences ;
  - une place sur le podium d'une partie live ;
  - le profil public, depuis sa page ;
  - la promotion d'un rang en classé, avec le classé (étapes 13 à 17).

## Conséquences

- Aucun changement côté serveur, aucune nouvelle dépendance.
- Le rendu varie un peu selon les polices du système : la carte n'utilise que `system-ui`, pour rester lisible partout.
- Les navigateurs de bureau sans Web Share téléchargent l'image : l'utilisateur la publie lui-même.
- Le texte partagé ne contient que l'exploit et l'adresse de l'application, jamais celle d'un profil privé.
