# 0071. Le serveur de l'application : Caddy, Keycloak en production et une seule instance PostgreSQL, en Docker Compose

- Statut : accepté
- Date : 2026-11-23
- Prolonge : ADR 0069 (deux VPS, Keycloak hébergé, les images publiées par la CI), ADR 0070 (des logs sans donnée
  personnelle), ADR 0054 (la limite de débit et `TRUSTED_PROXY`), ADR 0056 (la suppression du compte chez Keycloak),
  ADR 0047 (les clés VAPID), ADR 0010 (le client OIDC de la PWA)
- Origine : le déploiement (phase 1, étape 7), son étape 3, « le serveur de l'application » ; audit du 2026-11-10
  (`docs/audit-2026-11.md`), élément 33 (HSTS)
- Complété par : ADR 0072 (le tunnel WireGuard, côté application, et le runner mis à jour avant l'application),
  ADR 0073 (les sauvegardes des deux bases, et leur restauration)

## Contexte

L'ADR 0069 a choisi deux VPS OVHcloud en Docker Compose. Le serveur de l'application (VPS-2 : 4 vCores, 8 Go, Debian ou
Ubuntu LTS) porte Caddy, le front, le backend, Keycloak sous `auth.<domaine>` et PostgreSQL ; le runner viendra sur son
propre serveur (étape 7.4). Il manquait tout ce qui le fait tourner : le compose de production, le proxy TLS, un
Keycloak en mode production avec son royaume, la base, et la procédure, écrite pour un développeur qui n'a jamais
administré de serveur.

Les contraintes :

- **le serveur ne construit rien** : les images du backend et du front sont celles de la CI, au tag `sha-…` épinglé ;
  les autres sont des images officielles (ADR 0069) ;
- **aucune donnée personnelle dans les logs** (ADR 0070), y compris ceux qu'écrivent Caddy, Keycloak et PostgreSQL ;
- **la console d'administration de Keycloak n'est pas sur Internet** : c'est la clé de tous les comptes ;
- **une seule personne exploite** : peu de pièces, des secrets en un seul fichier, des commandes à copier ;
- **les décisions de Keycloak se vérifient** : le royaume de production ne doit pas être un royaume faux qui démarre.

## Options envisagées

**Le proxy TLS** :

| Option | Pour | Contre |
|---|---|---|
| nginx et certbot | nginx est déjà celui du front | Deux pièces, un renouvellement par cron, HSTS et HTTP/3 à écrire |
| Traefik | Certificats automatiques | Une configuration par labels, plus de concepts que nécessaire |
| Le nginx du front, en TLS | Un conteneur de moins | Des certificats dans l'image ou un volume de plus, et Keycloak derrière le front |
| **Caddy** (retenu) | HTTPS automatique (Let's Encrypt, renouvellement, redirection), HTTP/3, un Caddyfile de quelques lignes | Un composant de plus à suivre |

**La console d'administration de Keycloak** :

- **Un Keycloak managé** : déjà écarté par l'ADR 0069 (coût par utilisateur, comptes hors du serveur, un fournisseur
  différent du dev et de l'e2e).
- **La console ouverte avec une liste d'adresses IP** : l'adresse du propriétaire change (box, 4G, déplacements), et la
  console resterait une cible sur Internet ; la documentation de Keycloak conseille de ne l'exposer qu'en interne.
- **Un tunnel SSH** (retenu) : la console n'écoute que sur la boucle locale du serveur ; SSH, déjà ouvert et par clé
  seulement, en est la seule porte.

**L'image de Keycloak** :

- **Une image construite avec `--optimized`** : le démarrage gagne la dizaine de secondes de la construction, mais il
  faudrait la construire et la publier, alors que le serveur ne construit rien et que la CI ne publie que nos images.
- **L'image officielle, construite au démarrage** (retenu) : quelques secondes de plus à chaque démarrage, et une ligne
  de moins à maintenir.

**Le royaume de production** :

- **Le royaume local sans ses utilisateurs** : ses adresses sont celles de `localhost`, il est en anglais, sans
  vérification de l'e-mail ni protection contre la force brute ; les Dev Services, le compose et l'e2e en dépendent, et
  toute retouche pour la production les toucherait.
- **Un royaume de production à part** (retenu) : `infra/production/app/keycloak/recallio-realm.json`.

**Le mot de passe SMTP** :

- **Le coffre `file` de Keycloak** : le secret reste dans un fichier du serveur, jamais dans la base ; plus sûr, mais un
  dossier monté de plus, un fichier par secret au nom imposé, et un réglage de plus pour un seul secret.
- **Un placeholder du royaume** (retenu) : la valeur vient de la variable d'environnement à l'import, puis vit dans la
  base de Keycloak, comme le reste du royaume.

**La base** :

- **Deux instances PostgreSQL** : deux fois la mémoire, deux sauvegardes, deux mises à jour.
- **Une base, deux schémas** : les deux applications partageraient un propriétaire, ou des droits fins à écrire.
- **Une instance, deux bases, trois rôles** (retenu) : chaque application est propriétaire de sa base, et d'elle seule.

**Le pilote des logs de Docker** :

- **`json-file`**, le défaut : des fichiers JSON que d'autres outils savent lire, mais sans rotation si l'on oublie ses
  options.
- **`local`** (retenu) : le pilote que Docker conseille pour éviter un disque plein, qui compresse les fichiers
  tournés ; `docker compose logs` les lit comme les autres. Si un collecteur de logs lisant les fichiers arrive un jour,
  on reviendra à `json-file`.

## Décision

### 1. Les fichiers : `infra/production/app/`

| Fichier | Rôle |
|---|---|
| `compose.yaml` | Les cinq services, leurs réseaux, leurs volumes ; le projet s'appelle `recallio-app`, distinct du compose local (`recallio`) |
| `Caddyfile` | Les trois sites, HSTS, le tri des chemins de Keycloak, les logs de Caddy |
| `.env.example` | Toutes les variables, commentées, avec la commande qui génère chaque secret ; copié en `.env` (`chmod 600`), jamais versionné |
| `keycloak/recallio-realm.json` | Le royaume de production, importé au premier démarrage |
| `postgres/init-databases.sql` | Les deux bases et les deux rôles, à la première initialisation |

L'étape 7.4 ajoutera `infra/production/runner/`. La procédure est dans `docs/deploiement.md`.

### 2. Le compose de production

- **Les images** : `ghcr.io/potichacha/recallio-backend` et `-frontend` au tag `${RECALLIO_TAG}`, obligatoire, sans
  défaut ; `caddy:2.11` (la dernière mineure, 2.11.9 au 2026-11-23), `quay.io/keycloak/keycloak:26.7` et `postgres:18`,
  comme en local. Une version mineure épinglée prend ses correctifs à chaque `docker compose pull`.
- **Chaque service** : `restart: unless-stopped`, un `healthcheck`, des `depends_on` à condition (PostgreSQL, puis
  Keycloak, puis le backend, puis le front, puis Caddy), et la même rotation des logs (`x-logging` : pilote `local`,
  5 fichiers de 10 Mo).
- **Les healthchecks** :
  - le backend et Keycloak ne sont prêts que sur un **statut 200** de leur `health/ready` : la réponse 503 d'un service
    qui démarre contient déjà des `"UP"` (les vérifications qui passent), et un simple `grep UP` le croyait prêt. Le
    backend démarrait alors pendant l'amorçage de Keycloak, et échouait sa découverte OIDC ;
  - Caddy et le front, par une **connexion TCP** (`nc -z`) : une requête HTTP toutes les 30 secondes remplirait le
    journal d'accès de nginx, et l'API d'administration de Caddy journalise chaque appel.
- **Le front redémarre avec le backend** (`depends_on.backend.restart: true`) : nginx résout l'adresse du backend à son
  démarrage, et un backend recréé peut changer d'adresse. Vérifié : recréer le backend relance le front, et l'API
  répond.
- **Deux réseaux** :
  - `edge` (Caddy, front, Keycloak), au sous-réseau fixe `10.80.0.0/24`, hors des plages que Docker attribue lui-même :
    Caddy y a l'adresse fixe `10.80.0.10`, écrite une fois (ancre YAML `x-caddy-address`) et reprise par le
    `TRUSTED_PROXY` du front et le `KC_PROXY_TRUSTED_ADDRESSES` de Keycloak ;
  - `internal` (front, backend, Keycloak, PostgreSQL), qui n'est pas un réseau `internal: true` : le backend en sort
    pour Web Push, Sentry et le runner (WireGuard), Keycloak pour le relais SMTP. PostgreSQL n'est que sur lui.
- **Deux ports publiés, pas un de plus** : Caddy (80, 443, et 443/udp pour HTTP/3) ; Keycloak sur `127.0.0.1:8280`, pour
  le tunnel SSH. Docker contourne le pare-feu `ufw` pour les ports qu'il publie : un port publié sans `127.0.0.1:`
  serait sur Internet, quoi que dise `ufw`.
- **Les variables dérivées du domaine**, par l'interpolation de Compose, pour ne jamais les accorder à la main :
  `PUBLIC_ORIGIN=https://${DOMAIN}` (backend, front, et l'import du royaume), `OIDC_AUTHORITY` et `OIDC_ORIGIN` (front),
  `KC_HOSTNAME=https://auth.${DOMAIN}`, l'expéditeur `no-reply@${DOMAIN}`, et `SENTRY_RELEASE=${RECALLIO_TAG}`.
- **Les secrets sont obligatoires** (`${VAR:?message}`) : un secret oublié fait refuser le démarrage, avec le nom de la
  variable, plutôt que de lancer un service mal configuré.
- **PostgreSQL** écrit ses erreurs en `log_error_verbosity=terse` : sans cela, son journal recopie les valeurs d'une
  erreur (« DETAIL: Key (handle)=(alice) already exists »), ce que l'ADR 0070 avait retiré du seul pilote JDBC.
  Vérifié : le `DETAIL` disparaît.
- **La mémoire des deux JVM est plafonnée** : sans limite, la JVM dimensionne son tas sur la mémoire de l'hôte (80 %
  pour le backend, par le `run-java.sh` de son image ; 70 % pour Keycloak). Mesuré le 2026-11-23 sur la pile de
  vérification :

  | Service | Limite | Tas maximal | Au repos | Après un parcours complet |
  |---|---|---|---|---|
  | Backend | 1,5 Go | 1,2 Go (80 %) | 440 à 490 Mo | 515 Mo |
  | Keycloak | 1,5 Go | 1 075 Mo (70 %) | 600 à 835 Mo | 860 Mo |
  | PostgreSQL | aucune | | 100 à 110 Mo | 115 à 150 Mo |
  | Caddy, front | aucune | | 15 Mo chacun | 15 à 22 Mo |

  La limite doit couvrir le tas maximal **et** le reste de la JVM (métaspace, code compilé, fils) : à 1 Go, il ne
  restait que 205 Mo au backend et 307 Mo à Keycloak, que son guide de dimensionnement compte à 300 Mo. À 1,5 Go, le
  reste dispose de 300 et 460 Mo, et la limite de Keycloak couvre les 1 360 Mo de l'exemple de ce guide (10 000
  sessions). Les deux JVM prennent ainsi au plus 3 Go des 8 Go du serveur. Une JVM utilise davantage quand sa limite
  grandit (son tas initial en dépend) : les mesures à 1 Go étaient de 400 à 450 Mo et de 570 à 700 Mo. Le parcours
  mesuré : trois inscriptions avec leur e-mail, des appels à l'API, une suppression de compte, des échecs de connexion ;
  la charge de la bêta reste à observer (`docker stats`).

### 3. Caddy

- **Trois sites** : `{$DOMAIN}` relaie vers le front ; `www.{$DOMAIN}` redirige (301) vers `{$DOMAIN}`, chemin compris ;
  `auth.{$DOMAIN}` relaie vers Keycloak. HTTP redirige vers HTTPS (308). `DOMAIN` et `ACME_EMAIL` (le contact de
  Let's Encrypt) viennent du `.env`.
- **HSTS sur toutes les origines** : `max-age=31536000; includeSubDomains`, sans `preload`, qu'on ne peut pas défaire.
  L'en-tête remplace celui que Keycloak envoie lui-même : un seul par réponse.
- **Sous `auth.`, seulement le royaume `recallio`** (guide « Configuring a reverse proxy » de Keycloak 26) :

  | Chemin | Exposé | Pourquoi |
  |---|---|---|
  | `/realms/recallio/` | oui | Les pages de connexion et les points d'accès OIDC du royaume |
  | `/resources/` | oui | Les fichiers des thèmes |
  | `/admin/`, `/realms/master/` | non | La console et le royaume d'administration : par le tunnel SSH seulement |
  | `/health`, `/metrics` | non | Sur le port de gestion (9000), jamais relayé |
  | `/.well-known/` (RFC 8414) | non | Aucun client de Recallio ne s'en sert : la PWA et le backend lisent `/realms/recallio/.well-known/` |
  | `/robots.txt` | non | Keycloak 26.7 ne le sert pas (404, vérifié), et le tableau actuel du guide ne le cite plus |
  | tout le reste | non | 404, répondu par Caddy |

  Caddy nettoie le chemin avant de le comparer : `/realms/recallio/../master/`, `%2e%2e` ou `%2f..%2f` répondent 404, et
  `..;` est refusé par Keycloak lui-même (400). Vérifié un à un.
- **`X-Forwarded-For` est remplacé, jamais complété** : sans `trusted_proxies`, Caddy ignore la valeur envoyée par le
  client et y met l'adresse qu'il voit. nginx (`set_real_ip_from`) et Keycloak, qui ne croient que l'adresse de Caddy,
  reçoivent la vraie adresse du visiteur. Vérifié : 25 requêtes vers l'essai, chacune avec une fausse adresse
  différente, reçoivent quand même des 429 après la rafale permise.
- **Aucun journal d'accès** : celui de nginx suffit, sans adresse IP (ADR 0070). Caddy écrit quand même, en JSON :
  - son démarrage, sa configuration, ses arrêts ;
  - ses certificats : obtention, renouvellement, nettoyage du stockage, avec les noms de domaine seulement ;
  - les avertissements et erreurs de relais (un service injoignable, une réponse interrompue). Ces lignes recopieraient
    la requête : un filtre du journal par défaut en retire l'adresse et le port du client, ses en-têtes et l'adresse
    demandée. Il reste la méthode, le site, le protocole, le statut et l'erreur.

  Les erreurs de négociation TLS (un client qui ne parle pas TLS, un nom inconnu) ne s'écrivent qu'au niveau `debug` :
  rien n'apparaît au niveau par défaut. Vérifié.
- **Les certificats et le compte ACME** sont dans le volume `caddy-data` : le perdre ferait tout redemander, dans la
  limite de Let's Encrypt (5 certificats identiques par semaine).

### 4. PostgreSQL : une instance, deux bases, trois rôles

- Le superutilisateur `postgres` (`POSTGRES_PASSWORD`) n'administre que l'instance.
- `recallio`, propriétaire de la base `recallio` (le backend, ses migrations Flyway) ; `keycloak`, propriétaire de la
  base `keycloak`. Ni l'un ni l'autre n'est superutilisateur, ni ne crée de base ou de rôle ; propriétaire de sa base,
  chacun l'est aussi de son schéma `public` (`pg_database_owner`), où il crée ses tables.
- `CONNECT` et `TEMPORARY` sont retirés à `PUBLIC` sur les deux bases : chacun n'ouvre que la sienne. Vérifié :
  `recallio` est refusé sur la base `keycloak`, et l'inverse.
- `postgres/init-databases.sql` les crée, avec les mots de passe des variables du conteneur, lus par psql (`\getenv`) :
  ni le script ni la ligne de commande ne les portent. Il ne s'exécute **qu'à la première initialisation** du volume ;
  ensuite, changer un mot de passe dans le `.env` ne change pas celui de la base (`ALTER ROLE`, dans la procédure).

### 5. Keycloak en production

- **`start --import-realm`**, configuré par ses variables, vérifiées dans les guides de Keycloak 26.7 (« All
  configuration », « Configuring the hostname (v2) », « Configuring a reverse proxy ») :

  | Variable | Valeur | Pourquoi |
  |---|---|---|
  | `KC_DB`, `KC_DB_URL`, `KC_DB_USERNAME`, `KC_DB_PASSWORD` | `postgres`, la base `keycloak` | Sa base, dans l'instance commune |
  | `KC_HTTP_ENABLED` | `true` | Le TLS s'arrête chez Caddy |
  | `KC_PROXY_HEADERS`, `KC_PROXY_TRUSTED_ADDRESSES` | `xforwarded`, l'adresse de Caddy | Le schéma et l'adresse du client, crus de Caddy seulement |
  | `KC_HOSTNAME` | `https://auth.${DOMAIN}` | L'émetteur des jetons |
  | `KC_HOSTNAME_BACKCHANNEL_DYNAMIC` | `true` | Le backend l'appelle en interne (`http://keycloak:8080`), comme en local |
  | `KC_HOSTNAME_ADMIN` | `http://localhost:8280` | La console, par le tunnel |
  | `KC_HEALTH_ENABLED` | `true` | Le healthcheck, sur le port de gestion |
  | `KC_LOG_CONSOLE_OUTPUT` | `json` | Les logs en JSON |
  | `KC_BOOTSTRAP_ADMIN_PASSWORD` | du `.env` | Le compte temporaire `temp-admin` |

- **Le compte d'administration temporaire** n'est créé qu'au tout premier démarrage (une base vide) ; ensuite, Keycloak
  ignore la variable. Seul son mot de passe est passé : le nom reste celui par défaut, `temp-admin`, car Keycloak refuse
  de démarrer avec un nom vide et sans mot de passe. Une fois un administrateur permanent créé (avec OTP), `temp-admin`
  est supprimé et la variable vidée.
- **La console par le tunnel SSH** (`ssh -L 8280:127.0.0.1:8280 <serveur>`, puis `http://localhost:8280/admin/`) ; le
  port 8280 évite celui du Keycloak de développement (8180). **`KC_HOSTNAME_ADMIN` ne suffit pas** : il sert les
  fichiers de la console et son API d'administration, mais la connexion au royaume `master` passe par l'adresse publique
  du royaume, `https://auth.<domaine>/realms/master/`, que Caddy refuse. La façon documentée est l'**adresse publique du
  royaume `master`** (« Frontend URL », une des sources d'adresse du guide « Configuring the hostname (v2) ») :
  `http://localhost:8280`, réglée une fois après le premier démarrage, par `kcadm.sh` dans le conteneur. Vérifié : sans
  elle, la console reste bloquée ; avec elle, la connexion au royaume `master` aboutit et la console s'ouvre, tandis que
  `https://auth.<domaine>/realms/master/` répond toujours 404.
- **L'import ne se fait qu'au premier démarrage** : un royaume qui existe est ignoré (« Realm 'recallio' already exists.
  Import skipped »). Ensuite, le royaume se modifie dans la console ; une modification du fichier n'a plus d'effet sur
  le serveur, et doit être reportée à la main dans la console.

### 6. Le royaume de production

Il reprend du royaume local le client `recallio-frontend` (public, PKCE S256, le mapper d'audience `recallio-api`), le
rôle `admin` et ses actions requises, dont `delete_account`. Il en diffère ainsi :

- **aucun utilisateur** ;
- **les adresses du client** (`redirectUris`, `webOrigins`, `post.logout.redirect.uris`, `baseUrl`) viennent de
  `${PUBLIC_ORIGIN}` : Keycloak remplace un `${VARIABLE}` du fichier par la variable d'environnement à l'import (guide
  « Importing and exporting realms »). Essayé sur Keycloak 26.7.5 : `${VAR}` est remplacé, `${VAR:défaut}` prend le
  défaut, `${env.VAR}` ne l'est pas, et une variable absente **reste telle quelle**, sans erreur (une adresse ainsi
  restée fait seulement échouer la validation du client). D'où la règle : chaque variable du royaume est obligatoire
  dans le compose (`:?`), qui refuse de démarrer plutôt que d'importer un royaume faux ;
- **le rôle par défaut** (`default-roles-recallio`) contient `account` / `delete-account`, avec `view-profile`,
  `manage-account`, `offline_access` et `uma_authorization`, les rôles par défaut de Keycloak : tout nouvel inscrit
  peut supprimer son compte (ADR 0056) ;
- **inscription ouverte, connexion par e-mail, mot de passe oublié, vérification de l'e-mail**. Avec la vérification,
  Keycloak 26.7 ne demande plus le mot de passe dans le formulaire d'inscription : il le fait choisir après le lien de
  l'e-mail. Aucun compte n'a donc de mot de passe avant que son adresse soit prouvée. Les actions requises ont pour cela
  les priorités de Keycloak 26.7 (la vérification, 50, avant le mot de passe, 57) : celles du royaume local, plus
  anciennes, faisaient choisir le mot de passe d'abord. `VERIFY_PROFILE` et `delete_credential`, actives par défaut dans
  Keycloak, s'y ajoutent ;
- **la protection contre la force brute** : 5 échecs, puis une attente qui croît de 60 secondes jusqu'à 15 minutes ;
  des échecs à moins d'une seconde d'écart bloquent 60 secondes. Le blocage est temporaire : un blocage définitif
  permettrait de fermer le compte de n'importe qui en connaissant son nom ;
- **la politique de mot de passe** `length(10) and notUsername and notEmail`. Le NIST (SP 800-63B-4) demande 15
  caractères pour un mot de passe seul, la CNIL (délibération 2022-100) 8 caractères de trois types avec un blocage
  après échecs : 10 caractères sans règle de composition, avec la force brute bloquée, est un compromis pour une bêta, à
  revoir avant l'ouverture à tous ;
- **le français**, langue par défaut et seule langue (`internationalizationEnabled`, `supportedLocales`,
  `defaultLocale`), pour les pages et les e-mails ;
- **« Se souvenir de moi »**, avec des sessions longues pour qui le coche : 30 jours d'inactivité, 90 jours au plus. Les
  durées par défaut (30 minutes, 10 heures) restent pour les autres. La PWA garde ses jetons en `sessionStorage` : c'est
  la session de Keycloak qui évite de retaper son mot de passe chaque jour. Les sessions sont en base (sessions
  persistantes, le défaut de Keycloak 26) : vérifié, une session « se souvenir de moi » survit à la recréation de
  Keycloak et à `docker compose restart`, et reconnecte sans formulaire ;
- **`eventsListeners: []`** : le listener `jboss-logging` écrit sinon, à chaque échec de connexion, l'adresse IP et
  l'identifiant dans les logs. La protection contre la force brute n'en dépend pas : vérifié, après six échecs, le bon
  mot de passe est refusé, et Keycloak n'a écrit **aucune ligne** pendant tout le parcours (inscriptions, suppression,
  échecs) ;
- **les e-mails par Brevo** (`smtpServer`) : relais `smtp-relay.brevo.com`, port 587 en STARTTLS, avec
  authentification ; l'identifiant et la clé SMTP de Brevo, l'expéditeur `no-reply@<domaine>` au nom « Recallio », une
  adresse de réponse facultative. Tout passe par des placeholders, l'hôte et le port compris : la vérification locale
  pointe un faux serveur SMTP. Sans adresse de réponse, Keycloak répond à l'expéditeur.

Le mot de passe SMTP est ainsi **stocké dans la base de Keycloak** (masqué dans la console et dans l'API), comme le
reste du royaume ; le coffre `file` a été écarté (ci-dessus). Changer de clé SMTP, c'est la changer dans la console.

### 7. Le DNS : IPv4 seulement pour la bêta

Trois enregistrements `A` : `<domaine>`, `www` et `auth`. **Pas d'`AAAA`** : Docker publie aussi les ports en IPv6, mais
par son proxy en espace utilisateur ; un client IPv6 arriverait chez Caddy avec l'adresse de la passerelle de Docker, et
la limite de débit de nginx (ADR 0054) compterait tous les visiteurs IPv6 comme un seul. En IPv4, Docker garde l'adresse
du client.

### 8. La CI et Dependabot

- **Le job `production`** de la CI, quand `infra/production/` change (une nouvelle sortie du job `changes`), et toujours
  sur `main` : `docker compose config --quiet` avec les variables de `.env.example` (ses secrets vides remplis d'une
  valeur factice) ; `caddy fmt` et `caddy validate` avec l'image de Caddy que le compose épingle (lue par
  `docker compose config --images`) ; la validité JSON du royaume. Il entre dans les `needs` du job `publish` : une
  image publiée a passé toute la CI. Le job `e2e` ne se lance plus pour un changement de la seule configuration de
  production : il ne suit que le compose local et son royaume.
- **Dependabot** suit les images du compose de production (écosystème `docker-compose`, `/infra/production/app`), sauf
  les versions majeures de PostgreSQL, qui demandent une migration des données. Les images au tag `${RECALLIO_TAG}` ne
  sont pas suivies.

## Conséquences

- ➕ Le serveur se monte en suivant `docs/deploiement.md`, sans rien construire ; un seul fichier, le `.env`, porte tous
  ses secrets, et un secret oublié empêche de démarrer.
- ➕ La console de Keycloak, le royaume `master`, la base et le port de gestion ne sont pas sur Internet ; seul SSH y
  mène.
- ➕ Ni Caddy, ni Keycloak, ni PostgreSQL n'écrivent d'adresse IP, d'identifiant ou de valeur de la base dans leurs
  logs ; tous écrivent en JSON, avec une rotation.
- ➕ Vérifié sur une pile locale identique (domaine `recallio.localhost`, certificats de l'autorité interne de Caddy,
  faux serveur SMTP) : tous les services sains en moins d'une minute sur une base neuve, une inscription complète en
  français avec l'e-mail de vérification, le retour connecté dans la PWA, un appel à l'API, la suppression du compte
  (ADR 0056), la force brute, et le redémarrage.
- ➖ **Le royaume ne s'importe qu'une fois** : le fichier décrit le premier état ; ensuite, la console fait foi. Un
  changement voulu pour la production se fait dans la console, et se reporte dans le fichier pour un serveur neuf.
- ➖ **Le mot de passe SMTP est dans la base de Keycloak** : une sauvegarde de la base le contient (étape 7.5).
- ➖ **Keycloak garde l'adresse IP du dernier échec de connexion** d'un compte, pour sa protection contre la force brute
  (dans son cache, pas dans ses logs), et celle de chaque session de connexion, le temps de la session : dit dans la
  politique de confidentialité (ADR 0074).
- ➖ Quand tout redémarre ensemble (un redémarrage de l'hôte, ou `docker compose restart`), Docker n'attend pas
  Keycloak : le backend écrit une erreur (« Failed to create OidcProviderClientImpl »), envoyée à Sentry, puis se
  connecte à la première requête. Vérifié : l'application fonctionne ensuite normalement.
  `quarkus.oidc.connection-delay` n'y change rien (il ne réessaie pas une réponse 503). `docker compose down` puis
  `up -d` respectent l'ordre.
- ➖ Les premières lignes de Keycloak à chaque démarrage, celles de la construction de sa configuration, restent du
  texte, comme les deux premières du backend (ADR 0070).
- ➖ Une montée de Keycloak proposée par Dependabot se reporte à la main dans le compose local, qui doit garder la même
  version que la production.
- 🔁 Le coffre `file` de Keycloak, si d'autres secrets s'ajoutent au royaume ; une image de Keycloak `--optimized`
  publiée par la CI, si le temps de démarrage compte ; l'IPv6, quand Docker transmettra l'adresse des clients IPv6 ou
  quand Caddy écoutera directement sur l'hôte ; une politique de mot de passe plus longue avant l'ouverture à tous.
