# 0057. Apprendre de ses erreurs : le carnet, la leçon, et pas d'annulation

- Statut : accepté
- Date : 2026-11-12
- Prolonge : ADR 0031 (une carte par notion et par jour), ADR 0038 (s'entraîner sans rien enregistrer), ADR 0051 (des
  choix pour toutes les questions)
- Origine : audit du 2026-11-10 (`docs/audit-2026-11.md`), éléments 27, 35 et 38

## Contexte

Une réponse fausse n'apprenait rien de plus que sa correction : la carte repartait dans la file, et revenait quand FSRS
le décidait. L'audit proposait trois choses.

- **Un carnet d'erreurs** (27) : refaire les cartes ratées de la semaine.
- **« Revoir la leçon »** (35) sous une réponse fausse.
- **Annuler la dernière réponse** (38), pour un clic de trop.

## Options envisagées

**Le carnet enregistre-t-il les réponses ?**

- **Oui, comme une révision** : FSRS verrait des révisions rapprochées de quelques minutes, qu'il prend pour un oubli
  suivi d'un réapprentissage. Il contournerait aussi l'ADR 0031, et rapporterait de l'XP pour des cartes revues exprès.
- **Non, comme l'entraînement d'une notion** (retenu, ADR 0038).

**D'où vient la liste des cartes ratées ?**

- **Une table dédiée**, tenue à jour à chaque réponse : une projection de plus à maintenir et à effacer (ADR 0056).
- **Le journal de révision** (retenu) : il a déjà chaque réponse et sa note.

**Annuler la dernière réponse** :

- **La construire** : une révision met à jour la mémoire FSRS, le journal, l'XP, l'activité du jour, les quêtes, les
  succès et la cote de la carte, dont deux registres en ajout seul. Il faudrait défaire les sept.
- **Y renoncer** (retenu).

## Décision

**Le carnet d'erreurs**, dans le module `review` (`MistakesService`) :

- une carte y figure si sa **dernière** révision est notée « À revoir » et date de **sept jours** au plus (jours
  glissants, comptés avec le `Clock`) ; ratée puis réussie, elle en sort ;
- par sujet, la plus récemment ratée d'abord, **20 cartes au plus** ; une carte retirée du contenu n'y est plus ;
- `GET /api/v1/me/mistakes/{subject}` rend les cartes, sans leurs bonnes réponses ;
  `POST /api/v1/me/mistakes/{subject}/checks` corrige une réponse. Ces routes ont la forme de celles de l'entraînement ;
- la correction n'est donnée que pour une carte **du carnet de l'utilisateur** : la route ne sert pas à demander la
  réponse de n'importe quelle carte ;
- **rien n'est enregistré** : ni journal, ni FSRS, ni XP, ni série. Bien répondre dans le carnet ne retire pas la carte :
  seule une vraie révision le fait.

Dans la PWA : `/subjects/{sujet}/mistakes`, proposé sur l'écran de lancement d'une session (« Revoir mes erreurs de la
semaine (3) ») et au bilan d'une session où l'on s'est trompé. Le paquet de cartes est celui de l'entraînement
(`PracticeDeck`).

**« Revoir la leçon »** : une carte de révision ou d'entraînement porte le slug de sa notion. Sous une réponse fausse,
un lien ouvre la leçon dans un nouvel onglet, pour ne pas perdre la session. Une carte d'un sujet encore en document n'a
pas de notion : pas de lien.

**Pas d'annulation de la dernière réponse** :

- annuler une réponse corrigée par le serveur, c'est voir la bonne réponse puis rejouer la carte : cela contredit
  l'ADR 0051. L'annulation n'a de sens que pour l'auto-évaluation, qui disparaît avec la conversion de Java et de Go ;
- le clic de trop se traite avant l'enregistrement : choisir, puis « Valider » (déjà en place pour les cartes à choix,
  repris par la refonte, étape 8.9.4) ;
- son coût est sans rapport avec ce qu'elle apporte.

## Conséquences

- Le carnet lit le journal par la dernière révision de chaque carte de l'utilisateur. Avec des dizaines de milliers de
  révisions, il faudra un index sur `(user_id, card_id, reviewed_at)` ; aucun besoin mesuré aujourd'hui.
- Effacer le journal (ADR 0056) vide le carnet : rien d'autre à effacer.
- Le carnet ne compte pas dans l'objectif du jour. C'est voulu : il s'ajoute aux révisions, il ne les remplace pas.
- L'élément 38 de l'audit est clos sans code. Si la bêta montre des erreurs de saisie malgré « Valider », la réponse
  sera un délai de quelques secondes avant l'enregistrement, pas une annulation après coup.
