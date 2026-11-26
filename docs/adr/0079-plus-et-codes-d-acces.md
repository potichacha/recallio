# 0079. Le droit « Plus » et les codes d'accès : un droit daté, des codes dont seule l'empreinte est gardée, et la limite de la révision intensive

- Statut : accepté
- Date : 2026-11-26
- Prolonge : ADR 0077 (la refonte « trois portes », étape 9 : « Plus » avant tout paiement), ADR 0078 (la révision
  intensive, dont il pose la limite et rend la correction idempotente), ADR 0001 (monolithe modulaire), ADR 0008
  (UUID v7), ADR 0016 (un secret à usage borné), ADR 0054 (la limite de débit), ADR 0056 (`PersonalData`), ADR 0070
  (rien de secret dans les logs), ADR 0074 (la politique de confidentialité dit ce que le code garde), ADR 0076
  (`UsageStatistics`)
- Origine : la refonte « trois portes », étape 9 ; cet ADR couvre le serveur, les écrans viennent dans la PR suivante

## Contexte

L'ADR 0077 a choisi l'offre payante sans le paiement : « Plus » est un droit attaché au compte, avec une date de fin, et
pendant la bêta seuls des codes d'accès l'ouvrent. Le propriétaire les a demandés ainsi : « la possibilité de rentrer un
code long qui permet de ne pas payer, ou d'avoir des réductions ». Sans paiement, une réduction n'a pas de sens
aujourd'hui : un code donne des jours de Plus.

Plus ne lève pour l'instant qu'une limite, celle de la révision intensive (ADR 0078), qui a laissé deux choses à cette
étape : la limite elle-même, et l'idempotence de la correction, qu'une limite rend nécessaire (une requête rejouée après
une coupure ne doit pas coûter deux réponses).

Les maquettes (`Plus.dc.html`, `Code.dc.html`, `Intensif.dc.html`) disent ce que les écrans demanderont : le droit et sa
fin, un code qu'on colle « sans que les tirets et les minuscules comptent », ce que le code donnerait avant de
l'activer (« 3 mois de Plus offerts », puis « Activer mes 3 mois »), et « 14 questions gratuites restantes
aujourd'hui ».

Quatre contraintes :

- un code est un **secret** : qui le connaît obtient ce qu'il donne. Il ne doit se lire ni dans la base, ni dans une
  sauvegarde, ni dans un log ;
- il doit se **recopier et se dicter** : un testeur le reçoit par message, parfois de vive voix ;
- **pas de cycle entre modules** (ADR 0001) : `review` doit savoir si un compte a Plus, sans que le module du droit
  dépende de lui ;
- la politique de confidentialité (ADR 0074) dit ce que le code garde : un droit, des utilisations de codes et des
  essais ratés sont trois données de plus.

## Options envisagées

**Où ranger le droit** :

- **Dans `user`**, une colonne `plus_ends_at` sur le profil : le plus court. Mais les codes, leurs lots et leurs essais
  ratés n'ont rien d'un profil, et le paiement viendra s'y ajouter.
- **Dans `gamification`** : Plus n'est ni de l'XP ni une série.
- **Un module `entitlement`** (retenu) : le droit, ce qui l'ouvre aujourd'hui (les codes) et ce qui l'ouvrira demain (le
  paiement). Les autres modules n'en voient qu'une question : « ce compte a-t-il Plus ? ».

**Ce que la base garde d'un code** :

- **Le code en clair**, comme le jeton d'une invitation (ADR 0016) : un admin pourrait le relire. Mais l'ADR 0016 le
  justifiait par ce que le jeton ouvre (un groupe de révision) et annonçait de revoir ce choix si un secret donnait
  plus de droits. Un code donne ce qui se vendra : une fuite de la base, ou d'une sauvegarde, donnerait tous les codes
  encore libres.
- **Une empreinte lente et salée** (bcrypt, Argon2), comme un mot de passe : elle protège un secret que son
  propriétaire a choisi, donc devinable. Elle demanderait une dépendance, et interdirait de retrouver un code par son
  empreinte (il faudrait comparer chaque code du catalogue).
- **Son empreinte SHA-256** (retenu) : le code est tiré au hasard sur 125 bits, il n'y a rien à deviner. Une empreinte
  rapide et sans sel suffit, et elle sert d'index.

**Comment ne jamais dépasser le nombre d'utilisations** :

- **Une mise à jour conditionnelle** (`update … where redemptions < max`) : une seule instruction, mais la règle (lot
  révoqué, expiré, code épuisé, déjà utilisé par ce compte) se retrouve en SQL, où elle ne se teste pas seule.
- **Verrouiller la ligne du code** (retenu) : `select … for update`, puis les règles dans le domaine, en Java pur.
  Deux comptes qui utilisent le même code passent l'un après l'autre.

**Ce que devient une utilisation à la suppression du compte** :

- **La garder, sans le compte** : il faudrait un identifiant qui ne mène plus à personne, pour une ligne qui ne sert
  plus à rien.
- **L'effacer, et rouvrir le code** : un compte recréé réutiliserait son code sans fin.
- **L'effacer, le compteur restant sur le code** (retenu) : le nombre d'utilisations est une colonne du code, pas un
  compte des lignes. Tout ce qui désigne l'utilisateur part ; le code reste utilisé.

**Le statut HTTP de la limite quotidienne** :

- **429** : c'est bien un quota. Mais dans l'application, un 429 veut dire « réessaie dans un instant » (la limite de
  débit de nginx, une exécution en cours, trop d'essais de codes), et réessayer n'y change rien avant le lendemain.
- **402** : réservé par la norme, et il n'y a rien à payer.
- **403** (retenu) : le compte voit la ressource, il lui manque un droit. C'est la définition du `ForbiddenException`
  de la plateforme, et le code stable dit lequel.

**Où garder de quoi rendre la correction idempotente** :

- **Une ligne par réponse** (`request_id` unique, comme `review_log`) : l'ADR 0078 a justement refusé de garder une
  ligne par réponse, et il faudrait la purger chaque nuit.
- **Les identifiants de requête du jour, dans le compteur** : sans table de plus. Mais un même identifiant renvoyé pour
  toutes les réponses ne serait compté qu'une fois : la limite se contournerait d'une ligne de script.
- **Une empreinte de la réponse, dans le compteur** (retenu) : l'identifiant de requête, la carte et les choix, hachés
  ensemble.

## Décision

### Le module `entitlement`

Un module simple : des records dans le domaine, des requêtes natives dans l'infrastructure, pas d'entité JPA
(`CLAUDE.md`, section 4.3). Son API publique tient en une méthode, `PlusAccess.hasPlus(userId)`, d'après l'horloge
injectée. Il dépend de `user` et de `platform` ; `review` dépend de lui.

### Le droit

`plus_entitlement (user_id, ends_at, updated_at)` : une ligne par compte qui a eu Plus un jour. Le droit est actif tant
que `ends_at` est dans le futur ; ensuite la ligne reste, et dit quand il a pris fin. Rien ne tourne la nuit : c'est la
date, lue à chaque question, qui décide.

`GET /api/v1/me/plus` rend `active`, `endsAt` (absente si le compte n'a jamais eu Plus) et `daysLeft` (un jour entamé
compte pour un), de quoi tenir l'écran « Plus » et la carte du menu.

### Un code

- **25 caractères tirés par `SecureRandom` parmi 32**, soit **125 bits**, écrits en cinq groupes :
  `K7QW2-M9XDF-4TLPA-8RBNC-H3VYE`. L'alphabet (`23456789ABCDEFGHJKLMNPQRSTUVWXYZ`) n'a ni 0, ni O, ni 1, ni I. Avec 32
  caractères, chacun porte 5 bits et le tirage n'a pas de biais.
- **La saisie est normalisée** : la casse, les espaces et les tirets de toute sorte ne comptent pas. Une saisie qui n'a
  pas la forme d'un code ne touche pas la table des codes.
- **Seule l'empreinte SHA-256 est gardée** (`access_code.code_hash`, unique), avec les **quatre derniers caractères**
  (`suffix`), pour reconnaître un code dans une liste. Ces 20 bits connus en laissent 105 à qui lirait la base.
- **Les codes ne sont rendus qu'une fois**, dans la réponse qui crée leur lot. Aucune autre route ne les connaît.
- **Jamais dans un log** : le code voyage dans le corps de la requête, jamais dans l'adresse ; `AccessCode`, le DTO de
  la requête et celui de la création masquent leur `toString()` ; les logs nomment le lot, jamais le code ni le compte.

### Un lot

`access_code_batch (id, label, plus_days, max_redemptions_per_code, expires_at, created_at, revoked_at)` et
`access_code (id, batch_id, code_hash, suffix, redemptions)`. Routes d'admin, sous `/api/v1/admin/access-code-batches` :

| Route | Ce qu'elle fait |
|---|---|
| `POST` | crée un lot : un libellé, de 1 à 200 codes, de 1 à 3 650 jours de Plus, de 1 à 10 000 utilisations par code (1 par défaut), une date d'expiration facultative ; rend le lot et ses codes |
| `GET ?page&size` | les lots, le plus récent d'abord, par pages de 100 au plus : créés, utilisés, utilisations, expiré, révoqué |
| `GET /{id}` | un lot et ses codes, chacun par ses derniers caractères et son nombre d'utilisations |
| `PUT /{id}/revocation` | révoque le lot ; sans effet s'il l'est déjà |

Révoquer un lot ferme ses codes encore libres. Les droits déjà ouverts restent : le droit ne garde pas de lien vers le
code qui l'a ouvert.

**Le modèle reste ouvert à un autre effet** sans le construire : c'est le lot qui porte ce que ses codes donnent. Le
jour où un code vaudra une réduction, le lot recevra une colonne de plus (un type d'effet, dont les lots existants
prendront la valeur par défaut) ; les codes, leurs empreintes et leurs utilisations ne changeront pas.

### Utiliser un code

`POST /api/v1/me/plus/redemptions`, avec le code : le droit du compte finit `plus_days` jours après
**max(maintenant, sa fin actuelle)**. Un code utilisé pendant un droit en cours ne fait rien perdre ; après un droit
échu, les jours partent de maintenant.

`POST /api/v1/me/plus/access-code-checks` dit ce que le code donnerait, sans l'utiliser : la maquette l'annonce avant
le bouton « Activer ». Cette route n'était pas demandée ; elle suit les mêmes règles que l'utilisation, essais ratés
compris, sinon elle servirait à deviner des codes.

| Cas | Réponse |
|---|---|
| le code marche | 201 (utilisation) ou 200 (vérification) |
| inconnu, mal formé, épuisé, expiré, révoqué | **422** `entitlement.access-code-invalid`, le même corps dans tous les cas |
| déjà utilisé par ce compte | **409** `entitlement.access-code-already-redeemed` |
| trop d'essais ratés | **429** `entitlement.too-many-code-attempts` |

Le 409 ne révèle rien : seul le compte qui a utilisé le code le reçoit. Il est vérifié avant le reste, puisqu'un code à
usage unique que ce compte a utilisé est aussi un code épuisé. Sur mobile, une requête renvoyée après une coupure
reçoit ce 409, et l'écran relit le droit : le code n'est pas utilisé deux fois (clé primaire
`access_code_redemption (code_id, user_id)`).

**La concurrence** : la ligne du code est verrouillée (`for update`) le temps de l'utilisation, puis celle du droit.
Dix comptes sur un code à trois utilisations : trois y arrivent. Un compte qui envoie deux fois le même code : une
utilisation. Deux codes différents en même temps : leurs jours se cumulent. Les verrous sont toujours pris dans le même
ordre, le code puis le droit.

### Contre l'essai en force

`access_code_attempt (user_id, window_started_at, failures)`, une ligne par compte : **10 essais ratés par heure**,
d'après l'horloge. La fenêtre s'ouvre au premier essai raté ; au-delà de dix, toute tentative est refusée (429) jusqu'à
sa fin, **même avec un code valide**, sinon la limite n'arrêterait rien. Le code essayé n'est pas gardé.

L'essai raté s'écrit dans la transaction que l'exception termine (`dontRollbackOn`) : rien d'autre n'y a été écrit.

**La limite de débit de nginx (ADR 0054) ne couvre pas cette route**, et n'a pas à le faire : elle compte les requêtes
sans compte, par adresse, et celle-ci demande un jeton. La limite par compte fait le travail, en base, donc commune à
plusieurs instances. Créer des comptes à la chaîne ne mène nulle part : à dix essais par heure et par compte, 125 bits
ne se devinent pas. `nginx` n'est pas modifié.

### La limite quotidienne de la révision intensive

- Un compte sans Plus fait corriger **20 réponses par jour**, son jour, dans son fuseau
  (`recallio.review.drill-free-answers-per-day`). Avec Plus, aucune limite.
- `POST /api/v1/me/drill/checks` refuse la réponse de trop par un **403** `review.drill-daily-limit-reached`, sans la
  corriger ni la compter.
- **`GET /api/v1/me/drill/questions` reste ouvert.** Une question sans sa correction ne donne rien de ce que Plus
  vend ; le tirage reste sans état (ADR 0078) ; et une page chargée juste avant la limite ne devient pas une erreur.
  C'est l'écran qui lit ce qu'il reste et montre le mur.
- `GET /api/v1/me/drill/usage` rend en plus `dailyLimit` et `remaining`, absents avec Plus ; chaque correction rend
  `remainingToday`.
- La limite et le compteur tiennent dans **une seule instruction** (`insert … on conflict do update … where answers <
  limite`) : deux réponses simultanées ne passent pas à 21.
- Un droit qui prend fin en cours de journée rend la limite aussitôt : les réponses du jour comptent, il ne reste rien.

### L'idempotence de la correction

`POST /api/v1/me/drill/checks` reçoit un `requestId` obligatoire, tiré par le client pour chaque réponse et renvoyé tel
quel si la requête est rejouée, comme les routes qui enregistrent une révision.

Le compteur du jour garde, dans `drill_activity.recent_checks`, **les empreintes des 32 dernières réponses comptées** :
un UUID dérivé de l'identifiant de requête, de la carte et des choix (`DrillCheckId`). Une réponse dont l'empreinte y
est déjà n'est pas recomptée, et reçoit la même correction, même à la limite. La correction se recalcule : elle ne
dépend que de la carte et des choix.

- **La carte et les choix entrent dans l'empreinte** : le même identifiant renvoyé pour une autre carte, ou d'autres
  choix, est une autre réponse, et elle est comptée. Il n'y a donc pas de 409 « identifiant déjà utilisé », et pas de
  contournement de la limite.
- **Ni la carte ni la réponse ne se lisent** dans une empreinte : la promesse de l'ADR 0078 tient.
- **Pas de table, rien à purger** : les empreintes partent avec la ligne du jour, exportée et effacée avec le compte.
  Une requête rejouée après 32 autres réponses serait recomptée : un renvoi suit sa requête de près.

### Données personnelles, statistiques

- `EntitlementPersonalData` exporte le droit, les utilisations (avec les derniers caractères du code, jamais son
  empreinte) et les essais ratés, et **efface les trois** avec le compte. Le compteur du code reste.
- `UsageFeature.PLUS` (les comptes qui ont Plus à ce jour) et `UsageFeature.ACCESS_CODE` (ceux qui ont utilisé un code
  sur la période) sur la page « Bêta » (ADR 0076).
- La politique de confidentialité gagne une ligne, « Recallio Plus » (ADR 0074).

## Conséquences

- **Un code perdu ne se retrouve pas.** L'admin note les codes à la création ; sinon il révoque le lot et en crée un
  autre. La liste ne montre que les derniers caractères.
- **Révoquer un lot ne reprend rien** : un compte qui a utilisé un code garde ses jours. Reprendre un droit demanderait
  de savoir quel code l'a ouvert, ce que le droit ne garde pas.
- **Dix essais ratés bloquent aussi le bon code**, une heure au plus. Quelqu'un qui se trompe dix fois en recopiant
  attend ; le message le lui dit.
- **Sous une forte concurrence, un compte peut dépasser dix essais ratés de quelques unités** : le blocage est lu avant
  l'essai, pas sous verrou. Sans conséquence sur 125 bits.
- **Un code à plusieurs utilisations circule** : c'est voulu (un code pour une promotion d'école), et borné par
  `max_redemptions_per_code`, la date d'expiration et la révocation.
- **`review` dépend d'`entitlement`** : une requête de plus par correction, sur une clé primaire. Si un autre module
  demande Plus, il passe par la même interface.
- **La spec change sans compatibilité pour un appel** : `requestId` est obligatoire dans `DrillAnswer`. Aucun écran ne
  l'appelait encore dans `main`.
- **Une donnée de plus par compte et par jour** : trente-deux empreintes au plus dans `drill_activity`. Elles sortent
  dans l'export, telles qu'elles sont en base.
- **Ce qui attend** : les écrans (« Plus », « J'ai un code », le mur de la révision intensive, la page des lots pour un
  admin) ; le paiement, qui ouvrira le même droit ; ce que Plus débloque d'autre (ADR 0077) ; la table des modules de
  `CLAUDE.md`.
