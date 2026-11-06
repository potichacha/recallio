# 0051. Des choix pour toutes les questions : le serveur corrige, plus d'auto-évaluation

- Statut : accepté
- Date : 2026-11-06
- Remplace en partie : ADR 0048 (l'auto-évaluation « Je savais / Je ne savais pas »)
- Complète : ADR 0019 (les QCM), ADR 0027 (les formats texte), ADR 0028 (le code vérifié en CI)

## Contexte

En testant l'application en local, le propriétaire a relevé deux défauts.

- **Une question de cours ne corrigeait rien**. On pouvait écrire sa réponse, que personne ne lisait, puis la carte
  demandait « Je savais » ou « Je ne savais pas ». Chacun se notait lui-même.
- **L'auto-évaluation concernait plus de la moitié du contenu** : 6 481 cartes sur 12 041.

  | Format | Cartes |
  |---|---|
  | Questions de cours | 2 481 |
  | Bugs à corriger | 1 568 |
  | « Qu'affiche ce code ? » | 1 414 |
  | Mises en situation | 850 |
  | Définitions | 168 |

En creusant, un troisième défaut est apparu, dans les cartes déjà corrigées.

- **Les QCM se trichaient sans le vouloir**. Les choix s'affichaient dans l'ordre du fichier, et le bon était le premier
  dans 2 277 QCM sur 2 280. Cocher le premier choix suffisait, y compris en duel et en live.

## Options envisagées

- **Garder l'auto-évaluation** (l'existant, comme Anki).
  - ➕ Retrouver une réponse de mémoire ancre mieux que la reconnaître.
  - ➖ Rien ne garantit ni l'honnêteté ni la justesse du jugement.
  - ➖ Le classé et le profil vérifié ne peuvent pas s'y fier.
- **Corriger les réponses écrites avec une IA** : c'est la phase 5. Chaque réponse aurait un coût, et la correction
  pourrait se tromper.
- **Retirer ces cartes de la révision** : rapide, mais on perd plus de la moitié du contenu.
- **Convertir ces cartes en QCM** (`multiple-choice`) : simple, mais elles perdent leur format.
  - La maîtrise, qui demande trois formats (ADR 0034), l'XP par format (ADR 0035), les quêtes et la matrice du contenu
    ne verraient plus qu'un format.
- **Donner des choix à chaque carte, qui garde son format** (retenu).

## Décision

1. **Chaque format reçoit des choix**. Une carte de cours, de définition, de code, de bug ou de situation a des choix
   (`choices`), écrits comme ceux d'un QCM.
   - Elle garde son format, et le serveur la corrige sur ses choix, comme un QCM.
   - Juste, la carte est retrouvée ; fausse, elle est à revoir.
   - Sa réponse devient l'explication, montrée après la correction.
2. **La règle est portée par la carte** : une carte se corrige sur ses choix quand elle en a
   (`CardSnapshot.isGradedByChoices()`).
   - Un QCM et un vrai ou faux en ont toujours.
   - Le serveur refuse l'auto-évaluation d'une carte qui a des choix.
3. **Les choix sont mélangés à chaque affichage** : en révision, à l'entraînement, à l'essai, à l'examen blanc, dans
   les défis et en live.
   - Un vrai ou faux garde son ordre : Vrai, puis Faux.
   - Le serveur corrige toujours d'après la position des choix tels qu'il les a envoyés.
4. **Les règles de rédaction** (`content/README.md`) :
   - de 2 à 8 choix, le plus souvent 3 ou 4, sans doublon ;
   - une seule réponse défendable, ou « Lesquelles » quand plusieurs sont justes ;
   - des mauvais choix qui sont des erreurs qu'on fait vraiment ;
   - une question qui se comprend seule, et qui tutoie (ADR 0050).
5. **Le code est vérifié**. Pour un « qu'affiche ce code ? », la CI vérifie que le bon choix est la sortie donnée par
   `check`, déjà confrontée à l'exécution (ADR 0028), et qu'aucun autre choix ne l'affiche.
   - Une exception s'y nomme, une panic Go s'y dit « panic ».
   - Un code qui ne compile pas le dit (« compil »), et un code qui n'affiche rien aussi (« rien »).
6. **La migration se fait par étapes** : un pilote (« Programmer de zéro » et le chapitre Java « Premiers pas »), puis
   Java, puis Go. En attendant, une carte qui n'a pas encore ses choix s'auto-évalue comme avant.
7. **Ce qui change à la fin de la migration** :
   - les choix deviennent obligatoires pour ces formats dans un dossier de sujet ;
   - l'auto-évaluation, la zone de texte et le mode « questions corrigées » disparaissent de la PWA ;
   - les défis, le live, les duels, l'examen blanc, l'essai et les questions du Tour s'ouvrent à ces formats ;
   - le profil public compte leurs réussites comme vérifiées.

   D'ici là, ces parties restent limitées aux QCM, aux vrai ou faux et aux codes à trous, dont toutes les cartes sont
   corrigées.

## Conséquences

- ➕ Personne ne se note soi-même : chaque réussite est vérifiée, et rapporte le bonus d'XP (ADR 0035).
- ➕ On ne réussit plus un QCM en cochant toujours le premier choix.
- ➕ Sur un téléphone, on répond d'un geste, sans clavier.
- ➖ Reconnaître une réponse est plus facile que la retrouver, et en entretien personne ne propose quatre choix.
  - Les mauvais choix doivent donc être des erreurs plausibles.
  - Les codes à trous et les exercices restent pour produire la réponse soi-même.
- ➖ Il faut rédiger les choix d'environ 6 400 cartes, que le propriétaire relit (ADR 0025).
- ➖ Pendant la migration, deux façons de répondre cohabitent, et les cartes converties n'entrent pas encore dans les
  défis, le live, les duels ni l'examen blanc.
- 🔁 L'XP de base de chaque format ne change pas : lire une situation et quatre réponses demande plus qu'un vrai ou faux.
