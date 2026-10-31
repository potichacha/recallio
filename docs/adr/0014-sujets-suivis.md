# 0014. Sujets suivis : choix de l'utilisateur, quota de nouvelles cartes à parts égales

- Statut : accepté
- Date : 2026-10-31

## Contexte

Avec plusieurs sujets au catalogue (ADR 0013), chaque utilisateur choisit les sujets qu'il révise (CLAUDE.md,
sections 2.5 et 6.1) : Java, Go, puis les suivants à mesure qu'on les ajoute. Il faut décider :

1. ce que voit un utilisateur qui n'a encore rien choisi ;
2. où vivent les sujets suivis, et comment la révision les lit sans enfreindre les frontières des modules (ADR 0001) ;
3. comment le quota quotidien de nouvelles cartes, **global**, se répartit entre les sujets suivis (décision ouverte
   n° 9 de CLAUDE.md).

## Options envisagées

Pour un utilisateur sans sujet :

1. **Suivre tous les sujets par défaut** : simple, mais un nouveau sujet publié arriverait sans prévenir dans les
   révisions de tout le monde.
2. **Un écran de choix au premier login** (retenu) : l'utilisateur voit tous les sujets publiés et coche ceux qu'il
   veut. Le choix reste modifiable dans le profil.

Pour la répartition du quota :

1. **Parts égales, en alternant les sujets** (retenu) : aucun réglage, comportement prévisible.
2. **Pondération choisie par l'utilisateur** (Java ×2, Go ×1) : plus souple, mais un écran de réglage, une colonne
   de poids et davantage de cas à tester, sans besoin exprimé aujourd'hui.
3. **Proportionnel à la taille des sujets** : un gros sujet (Java, 1 311 cartes) noierait les petits, ce que la
   section 6.1 veut précisément éviter.

## Décision

- **Premier login** : tant qu'un utilisateur ne suit aucun sujet, l'accueil et la révision le conduisent à l'écran
  « Quels sujets voulez-vous réviser ? ». On ne peut suivre qu'un sujet publié, et au moins un.
- **Stockage** : table `followed_subject (user_id, subject_slug, followed_at)` dans le module `user` (une préférence).
  Le sujet est désigné par son **slug**, définitif par construction (ADR 0013) : pas de clé étrangère vers la table
  `subject` du module `content`. Un sujet suivi qui repasse en brouillon disparaît simplement des sujets suivis, et
  revient s'il est republié.
- **Frontières** : `content.SubjectDirectory` (API publique) donne les sujets publiés au module `user`, qui valide le
  choix ; `user.FollowedSubjects` (API publique) donne les sujets suivis au module `review`.
- **File de révision** :
  - sans filtre, elle puise dans les sujets suivis ; le tableau de bord compte les mêmes cartes ;
  - un sujet (`?subject=go`) ou un thème demandé explicitement se révise même s'il n'est pas suivi : c'est un choix
    explicite de l'utilisateur, depuis le catalogue.
- **Quota** : les cartes dues passent toujours en premier, tous sujets confondus. Les nouvelles cartes sont prises
  **à tour de rôle dans chaque sujet** (Java, Go, Java, Go...), dans l'ordre du catalogue ; quand un sujet n'a plus de
  carte nouvelle, sa part revient aux autres. Le quota reste global : des cartes introduites plus tôt dans la journée,
  dans un seul sujet, comptent pour tous.
- **Arrêter de suivre** un sujet supprime seulement la ligne de `followed_subject` : la progression (`review_state`)
  est conservée et revient si on le suit de nouveau.

## Conséquences

- Un nouveau sujet publié n'apparaît dans les révisions de personne tant qu'on ne le choisit pas : il faudra le faire
  savoir (catalogue, plus tard une notification).
- L'alternance des sujets mélange les domaines dans une même session, ce qui est aussi bénéfique à la mémorisation
  (pratique entrelacée).
- Une pondération par sujet reste possible plus tard sans changer de modèle : une colonne `weight` dans
  `followed_subject` et un tirage pondéré dans `ReviewQueuePlanner`.
