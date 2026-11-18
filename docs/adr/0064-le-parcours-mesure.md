# 0064. Le parcours mesuré : visite, essai, compte, première révision, sans donnée personnelle

- Statut : accepté
- Date : 2026-11-18
- Prolonge : ADR 0050 (essai sans compte, rétention), ADR 0054 (limite de débit), ADR 0056 (données personnelles)
- Origine : audit du 2026-11-10 (`docs/audit-2026-11.md`), élément 44

## Contexte

La rétention (ADR 0050) dit combien de joueurs reviennent. Elle ne dit rien de ceux qui ne commencent jamais : un
visiteur qui repart sans essayer, un essai qui ne donne pas de compte, un compte qui ne révise pas. Avant la bêta, il
faut savoir où l'on perd les visiteurs, pour savoir quoi améliorer d'abord : la page publique, l'essai, l'inscription
ou l'accueil d'un nouveau venu.

L'audit nommait quatre étapes : visite, essai, compte créé, première session terminée. Trois contraintes :

- **le RGPD et la section 4.5 de `CLAUDE.md`** : une mesure ne doit pas devenir un suivi des personnes ;
- **la frontière des modules** (ADR 0001) : l'essai est dans `review`, le compte dans `user`, l'activité dans
  `gamification` ;
- **la page publique pose déjà la première question de l'essai** (ADR 0058) : elle charge les questions pour chaque
  visiteur, qu'il réponde ou non.

## Options envisagées

**L'outil** :

- **Un outil tiers** (Plausible, Matomo) : des tableaux tout faits, mais une dépendance et un sous-traitant de plus, un
  script de plus dans la Content-Security-Policy (ADR 0054), et, selon l'outil et ses réglages, un bandeau de
  consentement. Il voit les pages, pas les étapes qui se jouent sur le serveur (un compte, une révision).
- **Des compteurs agrégés par jour** (une ligne par étape et par jour, incrémentée) : le plus sobre en stockage, mais
  le découpage est figé à l'écriture (le jour, et le fuseau choisi ce jour-là), et chaque écriture se dispute la même
  ligne.
- **Des événements sans identifiant** (retenu) : une ligne par étape franchie, avec son instant et rien d'autre. Les
  semaines se calculent à la lecture, et une autre période ne demande aucune migration.

**La quatrième étape** :

- **La première session terminée** : la fin d'une session n'existe que dans le navigateur (la durée choisie écoulée,
  ou la file vide, ADR 0048). Il faudrait que la PWA l'annonce, et un onglet fermé avant la fin ne l'annoncerait pas.
- **La première révision** (retenu) : le serveur l'observe déjà, sur `CardReviewed`. C'est le geste qui compte :
  quelqu'un qui a révisé une carte a franchi l'inscription et l'accueil.

**Le moment de l'essai** :

- **Le chargement des questions** : la page publique les charge pour poser la première, à chaque visite. Visites et
  essais se confondraient.
- **La réponse à la première question** (retenu) : le visiteur a lu une question et a répondu. Le serveur reconnaît la
  première question de l'essai d'un sujet, qu'elle soit posée sur la page publique ou sur la page de l'essai.

## Décision

1. **Une table `funnel_event`** (migration V30) : un identifiant UUID v7, l'étape (`VISIT`, `TRIAL`, `ACCOUNT`,
   `FIRST_REVIEW`) et l'instant (`timestamptz`). Ni identifiant d'utilisateur, ni adresse IP, ni cookie, ni sujet.
2. **Le module `gamification`** l'écrit et la lit, comme la rétention. Les autres modules lui parlent par des événements
   CDI publics, jamais par ses classes ni ses tables :
   - **`VISIT`** : la page publique appelle `POST /api/v1/funnel/visits` (public, sans corps, 204) une fois par session
     de navigateur, d'après un drapeau en `sessionStorage`. Sans stockage, la visite compte quand même ;
   - **`TRIAL`** : `review.TrialStarted`, publié par `TrialService` quand une réponse porte sur la première question de
     l'essai d'un sujet. L'essai n'enregistre rien d'autre, et publie hors de toute transaction : l'observateur a la
     sienne ;
   - **`ACCOUNT`** : `user.AccountCreated`, publié par `UserProfileService` quand il crée le profil, dans la même
     transaction. De deux premières connexions simultanées, seule celle dont le profil a été inséré le publie ;
   - **`FIRST_REVIEW`** : `FunnelRecorder` observe `CardReviewed` après `ActivityRecorder` (une priorité plus basse,
     comme `AchievementTracker`). La révision est la première si l'activité de l'utilisateur ne compte qu'une carte en
     tout : rien avant celle-ci.
3. **Le rapport** : `GET /api/v1/admin/funnel`, rôle admin, à côté de la rétention. Les huit dernières semaines ISO, du
   lundi au dimanche **en UTC** (un événement n'a pas d'utilisateur, donc pas de fuseau), la semaine en cours d'abord.
   Pour chaque semaine, le compte de chaque étape et le taux de passage depuis la précédente, en pourcentage arrondi
   vers le bas ; aucun taux pour la première étape, ni quand personne n'a franchi la précédente (pas de division par
   zéro). La page « Rétention et parcours » de la PWA le montre dans un tableau.
4. **Rien à exporter ni à supprimer avec un compte** (ADR 0056) : aucune ligne ne désigne une personne. `funnel_event`
   n'a pas de colonne `user_id`, et le module ne déclare rien de plus dans `PersonalData`.
5. **La limite de débit** : `/api/v1/funnel/visits` est sous la `location /api/` de nginx, dont la zone `public` compte
   les requêtes sans jeton par adresse IP (ADR 0054). Aucune règle de plus.

## Conséquences

- ➕ On voit, semaine par semaine, l'étape qui perd le plus de visiteurs, sans outil tiers, sans bandeau de
  consentement, et sans rien savoir de qui ils sont.
- ➕ Chaque étape est enregistrée là où elle se produit, côté serveur, sauf la visite : la seule que le serveur ne voit
  pas, la page publique étant un fichier statique servi par nginx.
- ➕ Une autre période (le jour, le mois) ne demande qu'une requête de plus, pas une migration.
- ➖ **Un taux n'est pas une conversion** : sans identifiant, il compare deux comptes de la même semaine, pas le
  devenir des mêmes visiteurs. Il peut dépasser 100 % : un compte créé le lundi par un visiteur du dimanche, ou par
  quelqu'un qui s'est connecté sans passer par la page publique.
- ➖ La visite est déclarée par le navigateur : un robot qui exécute le JavaScript est compté, et un visiteur dont le
  navigateur refuse `sessionStorage` est compté à chaque retour sur la page publique. Ce sont des marges, pas des
  tendances.
- ➖ Un essai recommencé (la page publique rouverte dans une autre session) compte de nouveau : `TRIAL` compte des
  essais commencés, pas des personnes.
- ➖ Reconnaître la première question relit les notions et les cartes du sujet à chaque réponse de l'essai, comme le
  fait le chargement des questions. C'est acceptable pour cinq réponses par essai ; un cache viendrait si la mesure le
  demande.
- 🔁 Les semaines sont en UTC : une visite le lundi à 0 h 30 à Paris est encore le dimanche en UTC, et compte dans la
  semaine précédente. Sur huit semaines, la tendance n'en dépend pas.
