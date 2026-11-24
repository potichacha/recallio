# 0073. Les sauvegardes : les deux bases, chiffrées pour une clé absente du serveur, verrouillées 30 jours hors d'OVHcloud

- Statut : accepté
- Date : 2026-11-24
- Prolonge : ADR 0069 (deux VPS, les secrets gardés à part), ADR 0071 (une instance PostgreSQL, deux bases, trois
  rôles ; le mot de passe SMTP dans la base de Keycloak), ADR 0072 (le serveur du runner), ADR 0070 (aucune donnée
  personnelle dans les logs), ADR 0056 (un compte supprimé reste dans les sauvegardes)
- Origine : le déploiement (phase 1, étape 7), son étape 5, « les sauvegardes » ; audit du 2026-11-10
  (`docs/audit-2026-11.md`), élément 46

## Contexte

Tout ce que Recallio ne sait pas refaire est dans une instance PostgreSQL, sur un seul serveur : la base `recallio`
(les comptes, la progression, l'XP) et la base `keycloak` (les identités, les mots de passe hachés, le royaume et ses
réglages, dont le mot de passe SMTP). Le reste se reconstruit : le contenu voyage dans l'image du backend, les images
sont sur GHCR, la configuration dans le dépôt, les certificats se redemandent.

L'offre VPS d'OVHcloud comprend une sauvegarde quotidienne du serveur (ADR 0069). Elle ne suffit pas : elle est chez le
même hébergeur, sous le même compte, et copie un disque sur lequel PostgreSQL écrit.

Les contraintes :

- **hors du serveur et hors d'OVHcloud** : un incident chez l'hébergeur, ou la perte du compte, ne doit pas emporter
  les données et leurs sauvegardes ;
- **un serveur pris ne doit ni lire ni détruire les sauvegardes** : c'est le premier endroit où un attaquant, ou une
  erreur, frappera ;
- **des données personnelles** : une sauvegarde contient des comptes. Elle ne se lit que pour restaurer, et ne se garde
  pas plus longtemps qu'annoncé (ADR 0056) ;
- **une seule personne exploite** : peu de pièces, une procédure à copier, et une restauration qui a été jouée ;
- **une sauvegarde qui s'arrête en silence** est le vrai risque : il faut l'apprendre sans aller voir.

## Options envisagées

**Ce qu'on sauvegarde** :

- **Le volume de PostgreSQL, ou une image du disque** : il faudrait arrêter la base, ou risquer une copie incohérente,
  et la restauration serait liée à la version majeure de PostgreSQL.
- **`pg_dumpall`** : un seul fichier, avec les rôles, mais en SQL seulement, sans compression ni restauration d'une
  seule base, et avec les empreintes des mots de passe des rôles.
- **Une archive en continu (WAL, pgBackRest)** : une perte de quelques minutes au lieu d'un jour, pour un outil et un
  dépôt de plus à exploiter. Trop pour une bêta.
- **`pg_dump` de chaque base, au format personnalisé** (retenu) : cohérent sans arrêt, compressé, restauré par
  `pg_restore`.

**Les rôles** : les sauvegarder (`pg_dumpall --roles-only`) ajouterait un troisième fichier, avec les empreintes des
mots de passe, pour deux rôles que le script d'initialisation recrée déjà sur un serveur neuf, avec les mots de passe
du `.env` (ADR 0071). Écarté, et vérifié par l'essai de restauration.

**Le chiffrement** :

- **Celui du fournisseur** (chiffrement côté serveur) : le fournisseur, et quiconque prend le compte, lit tout.
- **restic** (ou borg) : chiffre, déduplique et gère la rétention, mais le mot de passe du dépôt vit sur le serveur, qui
  doit lire le dépôt pour y écrire : un serveur pris lit toutes les sauvegardes passées. Et sa rétention suppose que le
  serveur puisse supprimer.
- **GPG** : fait la même chose qu'age, avec un trousseau, des réglages et des formats hérités.
- **age, pour une clé publique** (retenu) : le serveur ne peut que chiffrer. La clé privée naît sur le poste du
  propriétaire et n'est sur aucun serveur.

**La destination** :

- **Le stockage objet d'OVHcloud** : hors du serveur, mais chez le même hébergeur et sous le même compte.
- **Un autre serveur, par SSH (rsync, borg)** : un serveur de plus à payer, à mettre à jour et à protéger.
- **Un stockage objet compatible S3, chez un autre hébergeur** (retenu) : Backblaze B2, région UE. Une clé limitée à un
  bucket, des fichiers verrouillés (Object Lock), une rétention appliquée par le bucket, 10 Go gratuits.

**L'envoi** : en flux (`pg_dump | age | rclone rcat`), rien ne se pose sur le disque, mais un `pg_dump` qui échoue en
route laisse dans le bucket un fichier tronqué au nom d'une sauvegarde, que le serveur ne peut pas supprimer. Par un
**fichier temporaire chiffré** (retenu) : rien n'est envoyé tant que les deux bases ne sont pas sauvegardées en entier,
et le fichier part avec sa taille et son empreinte.

**Où ça tourne** : un conteneur de plus dans le compose (une image à construire ou à choisir, le socket Docker ou le
mot de passe de la base à lui confier, un cron dans un conteneur), ou **un script et un timer systemd sur l'hôte**
(retenu) : `journalctl` pour le journal, `systemctl` pour l'état, et un rattrapage après un arrêt du serveur.

**La restauration** : `pg_restore --clean` dans la base en place ne supprime que les objets que la sauvegarde connaît ;
une table créée depuis par une migration resterait, et Flyway, revenu à l'historique de la sauvegarde, échouerait en la
recréant. **`pg_restore --clean --create`** (retenu) supprime la base et la recrée.

## Décision

### 1. Les fichiers : `infra/production/app/backup/`

| Fichier | Rôle |
|---|---|
| `backup.sh` | Sauvegarde, chiffre, envoie, vérifie, puis appelle le battement de cœur |
| `recallio-backup.service` | Lance le script, en root, avec la configuration de `/etc/recallio-backup/` |
| `recallio-backup.timer` | Chaque nuit à 02:15 UTC, jusqu'à 15 minutes plus tard, avec rattrapage (`Persistent=true`) |
| `backup.env.example` | La clé publique d'age, la destination, l'adresse du battement ; copié en `/etc/recallio-backup/backup.env` |
| `rclone.conf.example` | Le remote `recallio-backup`, sans identifiant ; copié en `/etc/recallio-backup/rclone.conf` |

La configuration réelle vit hors du dépôt, dans `/etc/recallio-backup/` (`chmod 700`, fichiers en `600`, à root). La
procédure est dans `docs/deploiement.md`, section 12.

### 2. Ce qui est sauvegardé

- **Les deux bases**, `recallio` puis `keycloak`, chacune par `pg_dump --format custom`, lancé dans le conteneur
  `postgres` (`docker compose exec -T`), par le superutilisateur, sans mot de passe (la connexion locale du conteneur).
  Les deux sauvegardes se suivent à quelques secondes : un compte créé entre les deux a son identité sans son profil,
  que le backend crée à la connexion suivante.
- **Pas les rôles** : `postgres/init-databases.sql` les recrée, avec les mots de passe du `.env`.
- **Pas le `.env`, ni les clés WireGuard, ni les certificats de Caddy** : le propriétaire garde les deux premiers dans
  son gestionnaire de mots de passe (ADR 0069, 0072) ; Caddy redemande les troisièmes.
- **Rien sur le serveur du runner** : il ne garde aucune donnée. Il se remonte par sa procédure.
- Les fichiers portent l'heure UTC du lancement : `recallio-2026-11-24T0215Z.dump.age` et
  `keycloak-2026-11-24T0215Z.dump.age`.

### 3. Le chiffrement : age, pour une clé publique

- Le serveur n'a que la **clé publique** (`BACKUP_AGE_RECIPIENT`). `pg_dump` écrit dans `age`, qui écrit le fichier
  chiffré : **aucun dump en clair ne se pose sur le disque**. `set -o pipefail` fait échouer la ligne quand `pg_dump`
  échoue, même si `age` a chiffré ce qu'il a reçu.
- Le dossier de travail est celui que systemd donne au service (`StateDirectory`, `/var/lib/recallio-backup`, en
  `700`), sur le disque plutôt que dans `/tmp`, qui peut être en mémoire. Il est effacé à la sortie, réussie ou non.
- **La clé privée** est générée sur le poste du propriétaire (`age-keygen`), gardée dans son gestionnaire de mots de
  passe avec une seconde copie ailleurs, et n'est jamais sur un serveur : la restauration déchiffre sur le poste.
  **La perdre, c'est perdre toutes les sauvegardes.**
- Les paquets `age` de Debian 13 (1.2.1) et d'Ubuntu 24.04 (1.1.1) suffisent.

### 4. La destination : Backblaze B2, par son API S3, avec rclone

- **Le script ne connaît pas le fournisseur** : seulement `BACKUP_DESTINATION`, un remote rclone et un chemin
  (`recallio-backup:<bucket>/postgres`). Changer de fournisseur, c'est changer `rclone.conf`.
- **La clé d'application du serveur** est limitée au bucket, avec quatre droits : `listBuckets`, `listAllBucketNames`,
  `listFiles`, `writeFiles`. D'après la documentation de B2 (« Application Key Capabilities », « S3-Compatible App
  Keys ») :

  | Droit | Ce qu'il permet | Donné |
  |---|---|---|
  | `writeFiles` | Déposer un fichier, en un ou plusieurs morceaux ; **masquer un fichier** (`b2_hide_file`, ou un `DeleteObject` sans version) | oui |
  | `listFiles` | Lister les fichiers et leurs versions : la vérification du script | oui |
  | `listBuckets`, `listAllBucketNames` | Trouver le bucket ; B2 demande le second aux clés limitées à un bucket, pour les clients S3 | oui |
  | `readFiles` | Télécharger un fichier, et lire ses métadonnées (`HeadObject`) | non |
  | `deleteFiles` | Supprimer une version | non |
  | `bypassGovernance` | Supprimer une version verrouillée en mode gouvernance | non |

  La console de B2 ne propose que trois profils (lecture et écriture, lecture seule, écriture seule) : la clé se crée
  avec l'outil en ligne de commande (`b2 key create`), et la console affiche ensuite ses droits. La clé maîtresse du
  compte ne va jamais sur le serveur, et l'API S3 de B2 la refuse.
- **Une clé sans `deleteFiles` ne suffit pas.** `writeFiles` permet de masquer un fichier, ou d'en déposer un autre au
  même nom ; l'ancienne version devient « masquée », et une règle de cycle de vie la supprimerait ensuite. D'où
  l'**Object Lock**, activé à la création du bucket, avec une règle par défaut : **mode gouvernance, 30 jours**. Tout
  fichier déposé est verrouillé 30 jours ; ni la clé du serveur ni une règle de cycle de vie ne peuvent supprimer une
  version verrouillée (« Lifecycle rules do not override Object Lock »).
  - **Le mode gouvernance plutôt que conformité** : en mode conformité, personne ne peut supprimer avant terme, pas
    même le propriétaire, sinon en fermant son compte. Le mode gouvernance arrête le serveur, qui n'a pas
    `bypassGovernance`, et laisse au propriétaire le moyen de retirer une sauvegarde (une erreur de réglage, une
    demande d'effacement qui ne peut pas attendre). Il ne protège pas d'un compte Backblaze pris : la double
    authentification du compte y répond.
- **La rétention est celle du bucket**, puisque le serveur ne peut rien supprimer : une règle de cycle de vie, sur tout
  le bucket, `daysFromUploadingToHiding = 30` et `daysFromHidingToDeleting = 1`. La première masque chaque fichier
  30 jours après son envoi, version courante comprise ; la seconde supprime une version masquée un jour après. B2
  applique les règles une fois par jour, au premier passage après le délai : **une sauvegarde est supprimée entre 31 et
  32 jours après son envoi**, quand son verrou de 30 jours est échu.
- **rclone**, par le paquet de Debian 13 et d'Ubuntu 24.04 (1.60.1), suffit. Trois réglages sont nécessaires à une clé
  qui ne lit pas, chacun vérifié en le retirant (l'envoi échoue alors par un 403) :

  | Réglage | Où | Pourquoi |
  |---|---|---|
  | `no_check_bucket = true` | `rclone.conf` | rclone ne tente pas de créer ou de vérifier le bucket |
  | `no_head = true` | `rclone.conf` | Pas de `HeadObject` après l'envoi, que `readFiles` seul permet |
  | `--no-check-dest` | le script | Pas de `HeadObject` avant l'envoi, pour savoir si le fichier existe déjà |

  Sans relecture, l'intégrité repose sur deux choses : l'envoi porte l'empreinte MD5 du fichier, que le stockage
  vérifie à la réception (un seul morceau jusqu'à 200 Mo ; au-delà, une empreinte par morceau) ; puis le script
  **retrouve le fichier dans la liste du dossier, à sa taille**, avant de l'annoncer.
- **Rien n'est envoyé tant que les deux bases ne sont pas sauvegardées** : une sauvegarde interrompue ne laisse aucun
  fichier dans le bucket.

### 5. Un script et un timer systemd, sur l'hôte

- `backup.sh` : `set -euo pipefail`, une ligne par étape dans le journal (`journalctl -u recallio-backup.service`),
  aucun secret écrit, et un code de sortie non nul au premier échec. Il passe `shellcheck`.
- Le script ne lit que son environnement. `recallio-backup.service` le remplit : `EnvironmentFile` pour `backup.env`,
  `RCLONE_CONFIG` pour `rclone.conf`. **La sauvegarde à la main est la même que celle de la nuit** :
  `systemctl start recallio-backup.service`.
- Le service tourne en **root** : il lui faut le socket Docker (qui vaut déjà root), le `.env` du compose et la
  configuration. Il est lancé par `bash`, pour ne pas dépendre du bit d'exécution du fichier. `NoNewPrivileges`,
  `PrivateTmp`, `ProtectSystem=full`, `UMask=0077`, un délai d'une heure.
- Le timer : `OnCalendar=*-*-* 02:15:00 UTC` (l'heure creuse des utilisateurs européens), `RandomizedDelaySec=15min`,
  `Persistent=true` (une sauvegarde manquée serveur éteint part à son démarrage).

### 6. Savoir qu'une sauvegarde a manqué

- **Un battement de cœur, facultatif** : si `BACKUP_HEARTBEAT_URL` est posée, le script l'appelle (`curl`, une requête
  sans contenu) **après** avoir retrouvé les deux fichiers dans le bucket. Un service de surveillance du type
  Healthchecks.io alerte quand l'appel ne vient pas : c'est le silence qui prévient, que le script ait échoué, que le
  timer soit arrêté ou que le serveur soit éteint. Vide, rien n'est appelé.
- L'adresse vaut identifiant : elle n'est jamais écrite dans le journal. Un battement qui échoue n'est qu'un
  avertissement : la sauvegarde est faite, et le service de surveillance alertera de lui-même.
- La procédure donne les commandes qui montrent la dernière sauvegarde (`systemctl list-timers`,
  `systemctl is-failed`, `rclone ls`).

### 7. La restauration

- **Sur le poste du propriétaire** : il télécharge les deux fichiers d'une même sauvegarde depuis la console de B2 et
  les déchiffre avec la clé privée. age refuse un fichier tronqué ou modifié : on ne restaure qu'un fichier déchiffré
  en entier.
- **Vers le serveur, par SSH** : le dump entre dans `pg_restore`, dans le conteneur, sans se poser sur le disque du
  serveur. Ni la clé privée ni une clé de lecture du stockage ne viennent sur le serveur.
- **`pg_restore --clean --if-exists --create --exit-on-error`**, connecté à la base `postgres`, après l'arrêt du front,
  du backend et de Keycloak : la base est supprimée, recréée avec son propriétaire et ses droits (`REVOKE CONNECT ...
  FROM PUBLIC`, que seul `--create` restaure), puis remplie. Tant qu'une session est ouverte, PostgreSQL refuse de
  supprimer la base, et rien n'est touché.
- **Le serveur perdu** : un serveur neuf, le `.env` d'avant, PostgreSQL seul (son script d'initialisation crée les
  rôles et les bases vides), la restauration, puis le reste. Keycloak retrouve son royaume et son administrateur dans
  la base : il n'importe rien (« Import skipped »).
- **Les versions** : on restaure avec le tag du jour de la sauvegarde, ou un plus récent. Le backend applique au
  démarrage les migrations Flyway qui manquent (`migrate-at-start`), et Keycloak migre sa base ; aucun des deux ne
  revient en arrière.
- **L'essai de restauration**, à la mise en service puis une fois par trimestre : la dernière sauvegarde, restaurée
  sur le poste dans un PostgreSQL jetable (`postgres:18`), et quelques tables comptées. Il prouve aussi que le
  propriétaire a toujours la clé privée.

### 8. Les données personnelles

- Une sauvegarde contient tous les comptes : identités, adresses e-mail, mots de passe hachés, progression. Elle est
  chiffrée avant de quitter le serveur ; Backblaze, sous-traitant, ne stocke que des fichiers qu'il ne peut pas lire,
  dans l'Union européenne.
- **Un compte supprimé (ADR 0056) reste dans les sauvegardes jusqu'à leur suppression, 32 jours au plus.** Elles ne sont
  lues que pour une restauration.
- **Une restauration fait revenir les comptes supprimés depuis l'heure de la sauvegarde** : il faut rejouer ces
  suppressions. Le backend note chaque suppression dans ses logs, sans dire de qui (ADR 0056) : la procédure les compte
  depuis l'heure de la sauvegarde. Zéro, il n'y a rien à faire ; sinon, ou si les logs sont perdus avec le serveur, le
  propriétaire écrit aux inscrits que le service a été restauré à telle date, et que celui qui a supprimé son compte
  depuis doit le refaire. Les demandes reçues par e-mail se rejouent d'après leur date, gardée 32 jours.

### 9. La CI

Le job `production` vérifie en plus : `shellcheck` sur le script ; `systemd-analyze verify` sur les deux unités, où
tout message est un échec ; et l'absence de clé dans les deux modèles (ni clé d'age, publique ou privée, ni adresse de
battement, ni identifiant du stockage). `.gitignore` écarte `backup.env` et `rclone.conf`.

## Conséquences

- ➕ Un serveur pris ne lit aucune sauvegarde (il n'a ni la clé privée, ni le droit de télécharger) et n'en supprime
  aucune avant 30 jours.
- ➕ Vérifié le 2026-11-24, avec le vrai script lancé par systemd dans un conteneur Debian 13 (age 1.2.1, rclone
  1.60.1), la pile du compose de production (images du commit `647c225`), et MinIO à la place de B2, avec une clé qui
  ne peut que lister et écrire, dans un bucket verrouillé :
  - les deux fichiers arrivent chiffrés (`age-encryption.org/v1`), et le dossier de travail est vide ensuite ;
  - un mauvais secret du stockage, PostgreSQL arrêté, une clé publique invalide : le service échoue, et le battement
    n'est pas appelé ; il l'est après un succès ;
  - la clé du serveur ne lit ni ne supprime ; l'administrateur du stockage lui-même ne supprime pas une version
    verrouillée sans contourner la gouvernance ;
  - la restauration en place, puis sur un PostgreSQL neuf : les comptes de lignes de sept tables de `recallio` et de
    cinq de `keycloak` sont ceux de l'original, un compte créé après la sauvegarde a disparu, les propriétaires et les
    droits des bases sont revenus, le backend valide ses 33 migrations, et deux comptes d'avant la sauvegarde
    obtiennent un jeton et relisent leur progression ;
  - l'essai de restauration, joué tel qu'il est écrit.
- ➖ **On perd au plus un jour** : ce qui a été fait depuis la dernière nuit. Acceptable pour une bêta.
- ➖ **La clé privée est un point de défaillance** : perdue, toutes les sauvegardes sont illisibles. Deux copies, et un
  essai par trimestre qui la fait servir.
- ➖ **Le mot de passe SMTP et les mots de passe hachés** sont dans la sauvegarde de `keycloak` (ADR 0071) : chiffrés
  avec le reste.
- ➖ Deux comptes de plus à tenir (Backblaze, et le service de surveillance), et un outil sur le poste pour créer la
  clé du serveur.
- ➖ **Les réglages de B2 n'ont pas pu être essayés** (aucun compte n'a été créé pour cette étape) : les droits de la
  clé, l'Object Lock et la règle de cycle de vie viennent de sa documentation, et MinIO les a simulés. La première
  sauvegarde et le premier essai de restauration, à la mise en service, les confirment.
- ➖ Rejouer les suppressions de compte après une restauration repose sur un décompte dans les logs, puis sur un
  message aux inscrits : le backend ne garde nulle part, hors de la base, la liste des comptes supprimés.
- 🔁 Le mode conformité de l'Object Lock, à l'ouverture à tous ; une archive en continu (WAL), si un jour de perte
  devient trop ; une trace des suppressions de compte qui survive à une restauration (une empreinte de l'identifiant,
  hors de la base), si les restaurations ne restent pas exceptionnelles ; la migration vers une version majeure de
  PostgreSQL, qui se répétera par l'essai de restauration.
