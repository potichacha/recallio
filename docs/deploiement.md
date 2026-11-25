# Déployer et exploiter Recallio

> Pour le propriétaire du projet, qui monte et fait vivre les serveurs de la bêta. Chaque étape donne les commandes à
> copier et ce qu'on doit voir. Les décisions sont dans les ADR [0069](adr/0069-hebergement-et-livraison.md) (deux
> serveurs, des images publiées par la CI), [0070](adr/0070-logs-et-erreurs.md) (logs et Sentry),
> [0071](adr/0071-le-serveur-de-l-application.md) (le serveur de l'application),
> [0072](adr/0072-le-serveur-du-runner.md) (le serveur du runner), [0073](adr/0073-les-sauvegardes.md) (les
> sauvegardes), [0074](adr/0074-les-pages-legales.md) (les pages légales) et
> [0075](adr/0075-le-theme-de-connexion.md) (le thème des pages de connexion).
>
> État : le serveur de l'application (étape 7.3), le serveur du runner (7.4, section 11), les sauvegardes (7.5,
> section 12) et les pages légales (7.6 : leur configuration en section 5, leur relecture en section 13). La mise en
> service (7.7) se suit à la section 13 : l'ordre des étapes, la surveillance de la disponibilité, les premiers
> testeurs.

Conventions :

- `<domaine>` est le domaine du site (par exemple `recallio.fr`), `<ip>` l'adresse IPv4 du serveur de l'application,
  `<ip-runner>` celle du serveur du runner (section 11), `recallio` l'utilisateur que l'on crée sur chaque serveur
  (section 3) ;
- une commande précédée de « Sur ton poste » se tape sur ta machine, dans Git Bash sous Windows (dans PowerShell,
  `curl` n'est pas le vrai curl, et `grep` manque), sauf mention de PowerShell ; les autres, sur le serveur, connecté
  en SSH ;
- toutes les commandes `docker compose` se lancent depuis `/opt/recallio/infra/production/app`, sauf celles du serveur
  du runner, depuis `/opt/recallio/infra/production/runner` (section 11).

## 1. Vue d'ensemble

```mermaid
flowchart LR
    user([Navigateur ou PWA])
    subgraph app ["Serveur de l'application (VPS-2)"]
        caddy["Caddy<br/>:80 :443"]
        front["Front (nginx)"]
        back["Backend"]
        kc["Keycloak<br/>127.0.0.1:8280"]
        db[("PostgreSQL<br/>bases recallio et keycloak")]
    end
    subgraph run ["Serveur du runner (VPS-1)"]
        runner["Runner<br/>10.13.13.2:8090"]
        sandbox["Bac à sable<br/>un conteneur par exécution"]
    end
    store[("Stockage objet<br/>hors d'OVHcloud")]
    user -- "https://&lt;domaine&gt;" --> caddy
    user -- "https://auth.&lt;domaine&gt;" --> caddy
    caddy --> front --> back
    caddy --> kc
    back --> db
    kc --> db
    back -. "WireGuard (UDP 51820), jeton" .-> runner
    runner --> sandbox
    db -. "chaque nuit : les deux bases, chiffrées" .-> store
```

| Service | Ce qu'il fait | Joignable depuis Internet |
|---|---|---|
| Caddy | HTTPS (certificats Let's Encrypt), HSTS ; relaie `<domaine>` vers le front et `auth.<domaine>` vers Keycloak | oui : 80 et 443 |
| Front | nginx : la PWA, et `/api` relayé vers le backend | non : par Caddy |
| Backend | L'API ; importe le contenu à chaque démarrage | non : par le front |
| Keycloak | Les comptes, la connexion, les e-mails (Brevo) ; sa console d'administration | ses pages de connexion, par Caddy ; sa console, par un tunnel SSH seulement |
| PostgreSQL | Deux bases : `recallio` et `keycloak` | non |
| Runner, sur son serveur | Exécute le code des joueurs (exercices, Tour) dans un conteneur jetable par exécution | non : le backend seul, par le tunnel WireGuard, avec le jeton |
| La sauvegarde, sur le serveur de l'application | Un script lancé chaque nuit par systemd : les deux bases, chiffrées, envoyées dans un stockage objet hors d'OVHcloud (section 12) | non : elle n'écoute rien, elle envoie |

**Ce qui est secret** : sur le serveur de l'application, tout est dans un seul fichier,
`/opt/recallio/infra/production/app/.env`, lisible par toi seul (`chmod 600`), jamais versionné. Il porte les mots de
passe de PostgreSQL, celui du compte Keycloak temporaire, la clé SMTP de Brevo, le jeton du runner et les clés VAPID.
**Garde-en une copie hors du serveur** (un gestionnaire de mots de passe) : sans lui, une restauration ne peut pas se
reconnecter à la base, et de nouvelles clés VAPID désabonnent tout le monde des rappels. Le reste du dossier vient du
dépôt public et ne contient aucun secret. Le serveur du runner a son propre `.env` (le même jeton), et chaque serveur
ses clés WireGuard dans `/etc/wireguard/` ; la clé partagée du tunnel se garde avec le jeton (section 11). La sauvegarde
a sa configuration dans `/etc/recallio-backup/`, lisible par root seul : la clé du stockage, qui ne peut ni lire ni
supprimer. **La clé privée qui déchiffre les sauvegardes n'est sur aucun serveur** : elle reste chez toi, dans ton
gestionnaire de mots de passe (section 12).

## 2. Les prérequis

### Le domaine

Acheté chez le registraire de ton choix, une fois vérifié que « Recallio » n'est pas une marque déposée dans ta classe
(INPI, EUIPO ; ADR 0069).

### Les enregistrements DNS

Chez le registraire, trois enregistrements `A`, vers l'adresse IPv4 du serveur de l'application :

| Nom | Type | Valeur |
|---|---|---|
| `<domaine>` (souvent noté `@`) | `A` | `<ip>` |
| `www` | `A` | `<ip>` |
| `auth` | `A` | `<ip>` |

**Pas d'enregistrement `AAAA` (IPv6) pour la bêta**, et supprime ceux que le registraire aurait créés : derrière Docker,
un visiteur IPv6 arriverait avec l'adresse de la passerelle de Docker, et la limite de débit les compterait tous comme
un seul visiteur (ADR 0071).

Vérifier (sur ton poste, quelques minutes à quelques heures après) :

```shell
nslookup <domaine>
nslookup www.<domaine>
nslookup auth.<domaine>
```

Attendu : chacun répond `<ip>`, et aucun ne donne d'adresse IPv6.

### Brevo, pour les e-mails de Keycloak

Keycloak envoie la vérification de l'adresse et le « mot de passe oublié » par le relais SMTP de Brevo.

1. Crée un compte Brevo ; l'offre gratuite suffit pour une bêta.
2. **Authentifie le domaine expéditeur** : dans les réglages de Brevo, la page des domaines expéditeurs, ajoute
   `<domaine>`. Brevo affiche les enregistrements à créer chez le registraire ; les valeurs affichées font foi :
   - le **code Brevo**, un enregistrement `TXT` qui prouve que le domaine est à toi ;
   - **DKIM**, en général deux `CNAME` (`brevo1._domainkey`, `brevo2._domainkey`), qui signent les e-mails ;
   - **DMARC**, un `TXT` sur `_dmarc`, par exemple `v=DMARC1; p=none; rua=mailto:<ton adresse>` pour commencer, à durcir
     (`p=quarantine`) une fois que tout passe ;
   - **SPF** : Brevo n'en demande pas, il envoie avec sa propre adresse de retour et s'aligne sur DMARC par DKIM.
3. Attends que Brevo marque le domaine **authentifié** (jusqu'à 48 heures).
4. Crée une **clé SMTP** : dans *SMTP & API*, onglet *SMTP*, génère une nouvelle clé. Note l'identifiant (*Login*)
   affiché sur cette page et la clé : ce sont `SMTP_USER` et `SMTP_PASSWORD` (section 5). Une clé SMTP, pas une clé
   d'API.

### Sentry, facultatif

Si tu veux les erreurs dans Sentry (ADR 0070) : un compte en région UE, deux projets pour ce serveur
(`recallio-backend`, `recallio-frontend`) et leurs DSN, réglés comme le dit l'ADR 0070. Sans DSN, rien n'est envoyé.

### Backblaze B2, pour les sauvegardes

Les sauvegardes partent chaque nuit dans un stockage objet, chez un autre hébergeur qu'OVHcloud : un incident chez
OVHcloud ne doit pas emporter le serveur et ses sauvegardes (ADR 0073). Crée un compte Backblaze B2 en choisissant la
région **EU Central** à l'inscription : elle ne se change plus ensuite. Les 10 premiers gigaoctets sont gratuits ; une
sauvegarde pèse quelques mégaoctets. Active la double authentification du compte. Le bucket et sa clé se créent à la
section 12.

### Le serveur

Un VPS-2 d'OVHcloud, en **Debian 13** ou **Ubuntu 24.04 LTS**, avec ta clé SSH publique ajoutée à la commande. OVHcloud
t'envoie son adresse IPv4 et le nom de l'utilisateur créé (`debian` ou `ubuntu`).

## 3. Préparer le serveur

### Un utilisateur à toi, SSH par clé seulement

Sur ton poste, connecte-toi avec l'utilisateur d'OVHcloud :

```shell
ssh debian@<ip>      # ou ubuntu@<ip>
```

Crée ton utilisateur, donne-lui `sudo` et ta clé :

```shell
sudo adduser recallio                 # choisis un mot de passe : il servira à sudo
sudo usermod -aG sudo recallio
sudo install -d -m 700 -o recallio -g recallio /home/recallio/.ssh
sudo install -m 600 -o recallio -g recallio ~/.ssh/authorized_keys /home/recallio/.ssh/authorized_keys
```

Dans un **second terminal**, sans fermer le premier, vérifie que tu entres avec ta clé :

```shell
ssh recallio@<ip>
sudo whoami
```

Attendu : `root`. Puis interdis les mots de passe et `root` en SSH. Le fichier commence par `00-` pour passer avant
ceux de l'image (le premier réglage lu gagne) :

```shell
printf 'PasswordAuthentication no\nKbdInteractiveAuthentication no\nPermitRootLogin no\n' \
  | sudo tee /etc/ssh/sshd_config.d/00-recallio.conf
sudo sshd -t && sudo systemctl restart ssh
sudo sshd -T | grep -E '^(passwordauthentication|kbdinteractiveauthentication|permitrootlogin) '
```

Attendu : les trois lignes valent `no`. Vérifie encore, dans un nouveau terminal, que `ssh recallio@<ip>` passe ;
ensuite seulement, ferme le premier. Désormais, connecte-toi toujours en `recallio`.

### Le pare-feu

```shell
sudo apt-get update && sudo apt-get install -y ufw
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 443/udp
sudo ufw enable
sudo ufw status verbose
```

Attendu : `Status: active`, et des règles `ALLOW IN` pour `22/tcp`, `80/tcp`, `443/tcp` et `443/udp` (en IPv4 et en
IPv6).

**Attention : Docker contourne `ufw` pour les ports qu'il publie.** Il écrit ses propres règles de pare-feu, lues avant
celles d'`ufw` : un port publié par un conteneur est joignable depuis Internet même si `ufw` ne l'autorise pas. C'est
pourquoi le compose ne publie que les ports de Caddy, et celui de la console de Keycloak sur `127.0.0.1` seulement. Ne
publie jamais un autre port sans `127.0.0.1:` devant.

### Les mises à jour de sécurité automatiques

```shell
sudo apt-get update && sudo apt-get full-upgrade -y
sudo apt-get install -y unattended-upgrades
sudo dpkg-reconfigure --priority=low unattended-upgrades     # réponds « Oui »
cat /etc/apt/apt.conf.d/20auto-upgrades
```

Attendu : `APT::Periodic::Update-Package-Lists "1";` et `APT::Periodic::Unattended-Upgrade "1";`. Les correctifs de
sécurité du système s'installent chaque nuit. Un nouveau noyau demande un redémarrage : quand le fichier
`/var/run/reboot-required` existe, redémarre (`sudo reboot`) à une heure creuse ; les conteneurs repartent seuls.

### Docker Engine et le plugin Compose

Depuis le dépôt officiel de Docker, pas le paquet de la distribution (la page
[docs.docker.com/engine/install/debian](https://docs.docker.com/engine/install/debian/), ou `/ubuntu/`, fait foi si elle
a changé) :

```shell
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

Sur Ubuntu, remplace `debian` par `ubuntu` dans les deux adresses. Puis donne Docker à ton utilisateur (le groupe
`docker` vaut les droits root : ne l'accorde à personne d'autre), et reconnecte-toi :

```shell
sudo usermod -aG docker recallio
exit
```

```shell
ssh recallio@<ip>
docker run --rm hello-world
docker compose version
```

Attendu : « Hello from Docker! », puis `Docker Compose version v2.…`. Docker ne se met pas à jour avec les correctifs
automatiques : une fois par mois, `sudo apt-get update && sudo apt-get upgrade` (les conteneurs redémarrent, voir
section 10).

### L'heure en UTC

```shell
sudo timedatectl set-timezone Etc/UTC
timedatectl
```

Attendu : `Time zone: Etc/UTC (UTC, +0000)` et `System clock synchronized: yes`. Les logs et la base sont en UTC ; le
fuseau des utilisateurs vient de leur navigateur.

## 4. Récupérer la configuration

Le dépôt est public : on le clone à un emplacement fixe, sans identifiant. On ne se sert que de
`infra/production/app/` ; rien n'est construit sur le serveur.

```shell
sudo apt-get install -y git openssl
sudo install -d -o recallio -g recallio /opt/recallio
git clone https://github.com/potichacha/recallio.git /opt/recallio
cd /opt/recallio/infra/production/app
ls -A
```

Attendu : `.env.example  Caddyfile  backup  compose.yaml  keycloak  postgres  wireguard`. Le thème des pages de
connexion est ailleurs dans le dépôt, dans `/opt/recallio/infra/keycloak/themes/recallio` : le compose le monte de là
(section 7, « Le thème des pages de connexion »).

## 5. Configurer

### Le fichier `.env`

```shell
cd /opt/recallio/infra/production/app
cp .env.example .env
chmod 600 .env
ls -l .env
```

Attendu : `-rw------- 1 recallio recallio … .env`. Chaque variable y est commentée.

### Les secrets

Chaque secret est tiré au hasard et écrit directement dans le `.env`, sans passer par l'écran :

```shell
for v in POSTGRES_PASSWORD RECALLIO_DB_PASSWORD KEYCLOAK_DB_PASSWORD RECALLIO_RUNNER_TOKEN; do
  sed -i "s/^$v=$/$v=$(openssl rand -hex 32)/" .env
done
sed -i "s/^KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD=$/KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD=$(openssl rand -hex 16)/" .env
```

Le jeton du runner devra être le même dans le `.env` du serveur du runner (section 11).

### Les clés VAPID, une fois pour toutes

Les rappels par notification (ADR 0047) signent leurs envois avec une paire de clés P-256. Le backend les attend brutes,
en base64url : la clé publique (le point non compressé, 65 octets) et la clé privée (32 octets). **Ne les change plus
ensuite** : de nouvelles clés désabonnent tous les appareils.

```shell
openssl ecparam -name prime256v1 -genkey -noout -out /tmp/vapid.pem
pub=$(openssl ec -in /tmp/vapid.pem -pubout -outform DER 2>/dev/null | tail -c 65 | openssl base64 -A | tr '+/' '-_' | tr -d '=')
priv=$(openssl ec -in /tmp/vapid.pem -outform DER 2>/dev/null | head -c 39 | tail -c 32 | openssl base64 -A | tr '+/' '-_' | tr -d '=')
sed -i "s/^RECALLIO_PUSH_VAPID_PUBLIC_KEY=.*/RECALLIO_PUSH_VAPID_PUBLIC_KEY=$pub/; s/^RECALLIO_PUSH_VAPID_PRIVATE_KEY=.*/RECALLIO_PUSH_VAPID_PRIVATE_KEY=$priv/" .env
shred -u /tmp/vapid.pem
unset pub priv
grep VAPID .env | awk -F= '{ print $1, length($2) }'
```

Attendu : `RECALLIO_PUSH_VAPID_PUBLIC_KEY 87` et `RECALLIO_PUSH_VAPID_PRIVATE_KEY 43`.

### Le reste, à la main

```shell
nano .env
```

- `DOMAIN` : `<domaine>`, sans `https://` ; `ACME_EMAIL` : ton adresse, pour Let's Encrypt ;
- `SMTP_USER` et `SMTP_PASSWORD` : l'identifiant et la clé SMTP de Brevo (section 2) ; `SMTP_REPLY_TO`, si tu veux qu'on
  puisse répondre aux e-mails ;
- `RECALLIO_PUSH_SUBJECT` : `mailto:` suivi de ton adresse ;
- `LEGAL_CONTACT_EMAIL` : l'adresse de contact des pages légales (ADR 0074), obligatoire. Elle est publiée sur le site
  et lisible dans le code de chaque page : prends une adresse dédiée, que tu lis vraiment (`contact@<domaine>`), pas ton
  adresse personnelle. C'est là qu'arrivent les demandes sur les données, auxquelles tu dois répondre sous un mois ;
- `LEGAL_PUBLISHER` : ton nom, si tu veux qu'il figure dans les mentions légales, entre guillemets s'il contient un
  espace (`LEGAL_PUBLISHER="Prénom Nom"`). Vide, les pages disent que le site est édité par un particulier, à titre non
  professionnel, connu de l'hébergeur : la loi le permet à un éditeur non professionnel, à condition que ton compte
  OVHcloud porte bien ton identité ;
- `RECALLIO_RUNNER_URL` : garde la valeur d'exemple, `http://10.13.13.2:8090`, l'adresse du runner dans le tunnel
  WireGuard (section 11). Tant que le serveur du runner n'est pas monté, rendre un exercice répond 503 ; le reste
  fonctionne ;
- `SENTRY_DSN_BACKEND`, `SENTRY_DSN_FRONTEND`, `SENTRY_ORIGIN_FRONTEND` : facultatifs ;
- `RECALLIO_TAG` : ci-dessous.

### Le tag à déployer

Le serveur épingle les images d'un commit de `main` dont le **job `publish` est entièrement vert** (les sept images
publiées) : sur GitHub, *Actions*, *CI*, branche `main`, un run vert jusqu'aux sept `publish (…)`. Le tag est `sha-`
suivi des 12 premiers caractères du commit. Sur ton poste, avec la CLI de GitHub :

```shell
gh run list --repo potichacha/recallio --workflow ci.yml --branch main --status success --limit 5 --json headSha,displayTitle
```

Puis, sur le serveur, `RECALLIO_TAG=sha-<12 caractères>` dans le `.env`. Les paquets GHCR doivent être publics
(ADR 0069) : le serveur les tire sans identifiant.

### Vérifier la configuration

```shell
docker compose config --quiet && echo OK
```

Attendu : `OK`. Sinon, Compose nomme la variable qui manque (« required variable SMTP_USER is missing a value »).

## 6. Premier démarrage

```shell
cd /opt/recallio/infra/production/app
docker compose pull
docker compose up -d
docker compose logs -f
```

`Ctrl+C` quitte les logs sans rien arrêter. Dans l'ordre, tu dois voir :

1. **PostgreSQL** initialise sa base, puis lance le script des bases et des rôles :
   `running /docker-entrypoint-initdb.d/init-databases.sql`. Ce script ne s'exécute **qu'une fois**, sur un volume
   vide : changer ensuite un mot de passe dans le `.env` ne change pas celui de la base (section 10).
2. **Keycloak** prépare sa configuration (quelques lignes de texte, normal), crée ses tables, importe le royaume
   (`Realm 'recallio' imported`) et crée le compte temporaire (`Created temporary admin user with username temp-admin`).
3. **Le backend** attend Keycloak, applique les migrations Flyway, importe le contenu (une dizaine de secondes sur une
   base neuve), puis `started in …`.
4. **Le front**, puis **Caddy**, qui obtient les trois certificats : `certificate obtained successfully` pour
   `<domaine>`, `www.<domaine>` et `auth.<domaine>`.

Compte une à deux minutes. Puis :

```shell
docker compose ps
```

Attendu : les cinq services `Up … (healthy)`.

## 7. Keycloak

### Ouvrir la console d'administration

La console n'est pas sur Internet : `https://auth.<domaine>/admin/` répond 404. Elle écoute sur `127.0.0.1:8280` du
serveur, que l'on joint par un tunnel SSH.

**Une fois, au premier démarrage**, sur le serveur : donne au royaume `master` l'adresse du tunnel. Sans cela, la
console chargerait, mais sa connexion passerait par `https://auth.<domaine>/realms/master/`, que Caddy refuse
(ADR 0071). La commande utilise le compte temporaire, dont le mot de passe est déjà dans le conteneur :

```shell
docker compose exec keycloak sh -c '/opt/keycloak/bin/kcadm.sh update realms/master --no-config --server http://localhost:8080 --realm master --user temp-admin --password "$KC_BOOTSTRAP_ADMIN_PASSWORD" -s attributes.frontendUrl=http://localhost:8280'
```

Attendu : `Logging into http://localhost:8080 as user temp-admin of realm master`, et rien d'autre.

Puis, **sur ton poste**, à chaque fois que tu as besoin de la console :

```shell
ssh -N -L 8280:127.0.0.1:8280 recallio@<ip>
```

La commande ne rend pas la main tant que le tunnel est ouvert (`Ctrl+C` le ferme). Ouvre
[http://localhost:8280/admin/](http://localhost:8280/admin/) : la page de connexion du royaume `master`. Le port 8280
évite celui du Keycloak de développement (8180).

### Un administrateur permanent, avec OTP

1. Connecte-toi en `temp-admin`, avec le mot de passe du `.env` (`grep KEYCLOAK_BOOTSTRAP .env`, sur le serveur).
2. Royaume `master` (en haut à gauche), *Users*, *Create new user* : un nom, ton adresse, *Email verified* activé ; dans
   *Required user actions*, choisis *Configure OTP*. *Create*.
3. Onglet *Credentials*, *Set password* : un mot de passe long, *Temporary* désactivé.
4. Onglet *Role mapping*, *Assign role*, filtre *Filter by realm roles*, coche `admin`, *Assign*.
5. Déconnecte-toi (en haut à droite), reconnecte-toi avec ce compte : Keycloak te fait enregistrer l'OTP dans une
   application d'authentification. Il le demandera à chaque connexion.
6. *Users*, `temp-admin`, *Delete*.
7. Sur le serveur, vide le mot de passe temporaire (Keycloak ne s'en sert plus, il n'a rien à faire sur le serveur) :

   ```shell
   sed -i 's/^KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD=.*/KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD=/' .env
   docker compose up -d keycloak
   ```

   Keycloak redémarre (une trentaine de secondes sans connexion possible ; les sessions sont gardées).

### Ton compte dans Recallio, avec le rôle `admin`

1. Sur `https://<domaine>`, *Se connecter*, puis *Créer un compte* : inscris-toi comme n'importe qui, suis le lien de
   l'e-mail, choisis ton mot de passe. Tu reviens connecté dans la PWA.
2. Dans la console, royaume `recallio`, *Users*, ton compte, *Role mapping*, *Assign role*, *Filter by realm roles*,
   `admin`, *Assign*.
3. Dans la PWA, déconnecte-toi et reconnecte-toi : ton nouveau jeton porte le rôle `admin` (les sujets en brouillon et
   les pages des admins apparaissent).

### Tester l'envoi d'un e-mail

Dans la console, royaume `recallio`, *Realm settings*, onglet *Email*, *Test connection* : Keycloak envoie un e-mail de
test à l'adresse de ton administrateur `master`. Attendu : un message de réussite, et l'e-mail reçu (regarde aussi les
indésirables). Les réglages SMTP du `.env` n'ont servi qu'à l'import : désormais, ils se changent sur cet onglet.

### Le thème des pages de connexion

Se connecter, s'inscrire, retrouver son mot de passe : ces pages sont servies par Keycloak, avec le thème `recallio`
(ADR 0075). Le thème est un dossier du dépôt, `/opt/recallio/infra/keycloak/themes/recallio`, que le compose monte en
lecture seule dans Keycloak ; aucune image n'est à construire.

- **Au premier démarrage**, rien à faire : le royaume importé nomme déjà ce thème (`loginTheme`).
- **Un royaume déjà importé ne relit pas son fichier** : si le royaume existait avant le thème, ou si la page de
  connexion a l'allure grise de Keycloak, choisis-le à la main. Dans la console, royaume `recallio`, *Realm settings*,
  onglet *Themes*, *Login theme* : `recallio`, *Save*. L'effet est immédiat.
- **Après une mise à jour du thème** (un `git pull` qui change `infra/keycloak/themes/`) : `docker compose restart
  keycloak`. En production, Keycloak garde les thèmes en cache jusqu'à son redémarrage ; compte une trentaine de
  secondes sans connexion possible, les sessions sont gardées.
- **Les liens vers les pages légales** (au pied de chaque page, et dans une phrase sur la page d'inscription) pointent
  vers `https://<domaine>` : le thème lit `PUBLIC_ORIGIN`, que le compose déduit de `DOMAIN`, à chaque démarrage de
  Keycloak.
- **Si le dossier manque** (un dépôt incomplet, un chemin changé), Keycloak ne s'arrête pas : il sert son thème par
  défaut. Pour le voir : `docker compose exec keycloak ls /opt/keycloak/themes/recallio/login`.
- Les e-mails de Keycloak et sa console de compte gardent leur allure par défaut (ADR 0075).

## 8. Vérifier

Sur ton poste :

```shell
curl -sI https://<domaine>/ | grep -iE '^(HTTP|strict-transport-security)'
curl -sI https://auth.<domaine>/realms/recallio/account/ | grep -iE '^(HTTP|strict-transport-security)'
curl -sI https://www.<domaine>/ | grep -iE '^(HTTP|location)'
curl -s -o /dev/null -w '%{http_code}\n' https://auth.<domaine>/admin/
curl -s -o /dev/null -w '%{http_code}\n' https://auth.<domaine>/realms/master/.well-known/openid-configuration
curl -s https://auth.<domaine>/realms/recallio/.well-known/openid-configuration | grep -o '"issuer":"[^"]*"'
curl -s https://<domaine>/api/v1/health
```

Attendu, dans l'ordre :

- `HTTP/2 200` (ou `HTTP/1.1 200`, selon ton curl) et `strict-transport-security: max-age=31536000; includeSubDomains` ;
- la même chose pour Keycloak ;
- `HTTP/2 301` et `location: https://<domaine>/` ;
- `404` (la console n'est pas sur Internet) ;
- `404` (le royaume `master` non plus) ;
- `"issuer":"https://auth.<domaine>/realms/recallio"` ;
- `{ "status": "UP", … }`.

Puis, dans un navigateur :

- `https://<domaine>` : la page d'accueil ; l'essai sans compte fonctionne ;
- `https://<domaine>/mentions-legales` et `https://<domaine>/confidentialite`, sans te connecter (les deux liens du
  pied de page) : elles affichent ton adresse de contact, en lien, et l'éditeur attendu (ton nom, ou « édité par un
  particulier » si `LEGAL_PUBLISHER` est vide). Si l'adresse manque ou reste l'ancienne : `docker compose up -d` après
  avoir corrigé le `.env`, puis efface les données du site dans ton navigateur : l'application y garde sa page
  d'entrée jusqu'à la version suivante (ADR 0074) ;
- **la page de connexion** (*Se connecter*) : Rico et « Recallio » au-dessus du formulaire, le violet et la police de
  l'application, et au pied de la carte « Mentions légales » et « Confidentialité », qui ouvrent les deux pages de
  `https://<domaine>`. Si elle est grise et carrée : section 7, « Le thème des pages de connexion » ;
- **une inscription complète**, avec une autre adresse que la tienne : la page de Keycloak est en français, l'e-mail
  de vérification arrive de « Recallio <no-reply@<domaine>> », son lien fait choisir le mot de passe (10 caractères au
  moins) puis ramène connecté dans la PWA ;
- la connexion avec « Se souvenir de moi » : le lendemain, *Se connecter* reconnecte sans formulaire.

Enfin, que rien d'autre n'est ouvert (sur ton poste, dans PowerShell) :

```powershell
Test-NetConnection <ip> -Port 8280 | Select-Object TcpTestSucceeded
Test-NetConnection <ip> -Port 5432 | Select-Object TcpTestSucceeded
```

Attendu : `False` deux fois (après quelques secondes d'attente chacun).

## 9. Mettre à jour, revenir en arrière

### Une nouvelle version de Recallio

Les deux serveurs portent le même tag. **Le serveur du runner d'abord** (section 11, « Mettre à jour ») : un nouveau
backend ne parle ainsi jamais à un ancien runner. Puis, sur le serveur de l'application :

```shell
cd /opt/recallio/infra/production/app
git pull                                      # seulement si infra/ a changé
diff <(grep -oE '^[A-Z_]+=' .env.example) <(grep -oE '^[A-Z_]+=' .env)
nano .env                                     # RECALLIO_TAG=sha-<nouveau commit>
docker compose pull
docker compose up -d
docker compose ps
```

- Le `diff` ne doit rien afficher : une ligne `<` est une variable apparue dans `.env.example`, à ajouter au `.env`.
- Compose ne recrée que le backend et le front, dont l'image a changé ; Keycloak, PostgreSQL et Caddy ne bougent pas, et
  les sessions restent ouvertes.
- **L'indisponibilité attendue** : l'API répond 502 d'environ **20 secondes**, le temps que le nouveau backend démarre,
  importe le contenu (quelques secondes quand il n'a pas changé, ADR 0069) et soit déclaré prêt, puis que le front
  redémarre après lui.
- Si le `git pull` a changé `infra/keycloak/themes/` (le thème des pages de connexion) : `docker compose restart
  keycloak`, qui ne relit ses thèmes qu'à son démarrage (section 7).
- Note le tag précédent (en commentaire dans le `.env`, par exemple) : c'est le chemin du retour.

**Revenir en arrière**, c'est remettre le tag précédent, puis `docker compose pull` et `up -d` : ici d'abord, puis sur
le serveur du runner, dans l'ordre inverse de la mise à jour. Les migrations Flyway ne se défont pas : l'ancien code
tourne sur le nouveau schéma, ce que la règle des migrations rétrocompatibles permet (CLAUDE.md, section 7).

### Caddy, Keycloak et PostgreSQL

Leurs images sont épinglées à une version mineure (`caddy:2.11`, `keycloak:26.8`, `postgres:18`) :

- **les correctifs** de ces versions arrivent avec `docker compose pull` puis `up -d`. Fais-le une fois par mois, avec
  la mise à jour de Docker (section 3) ;
- **une nouvelle version mineure** arrive par une pull request de Dependabot (`caddy:2.12`, `keycloak:26.9`…) : après le
  merge, `git pull`, `docker compose pull`, `up -d`. Pour **Keycloak**, lis d'abord ses notes de mise à jour
  (*Upgrading Guide*) : il migre sa base au démarrage, sans retour possible autrement que par une sauvegarde. Lance-en
  donc une juste avant (`sudo systemctl start recallio-backup.service`, section 12) ; et reporte la version dans le
  compose local (`infra/compose.yaml`) ;
- **une version majeure de PostgreSQL** (18 vers 19) demande une vraie migration : la nouvelle version ne lit pas les
  fichiers de l'ancienne. Dependabot ne la propose pas. Le principe est celui du « serveur perdu » de la section 12 :
  une sauvegarde, puis la nouvelle image sur un volume neuf, puis la restauration, en gardant l'ancien volume jusqu'à
  ce que tout soit vérifié. À répéter d'abord sur ton poste, par l'essai de restauration, avec l'image de la nouvelle
  version.

## 10. Exploiter

### Lire les logs

Chaque service écrit en JSON, une ligne par événement, sans adresse IP ni donnée personnelle (ADR 0070, 0071). Docker
en garde 5 fichiers de 10 Mo par conteneur.

```shell
docker compose logs --tail 100 backend
docker compose logs -f --since 10m frontend
docker compose ps
```

Pour filtrer (`sudo apt-get install -y jq`), par exemple les erreurs du backend, ou les réponses 5xx du front :

```shell
docker compose logs --no-log-prefix backend | jq -R 'fromjson? | select(.level == "ERROR") | .message'
docker compose logs --no-log-prefix frontend | jq -R -c 'fromjson? | select(.status >= 500)'
```

### L'espace disque

```shell
df -h /
docker system df
docker image prune
```

`docker image prune` supprime les images qui ne servent plus à rien (sans tag). Les anciennes versions de Recallio
restent jusqu'à `docker image prune -a`, qui supprime toute image qu'aucun conteneur n'utilise : sans danger, pour
revenir en arrière, `docker compose pull` les tire de nouveau de GHCR.

### Redémarrer

```shell
docker compose restart keycloak            # un service
docker compose restart backend frontend    # le backend, toujours avec le front, qui le retrouve ainsi
docker compose down && docker compose up -d   # tout, dans l'ordre (les volumes restent)
```

Préfère `down` puis `up -d` pour tout redémarrer : `docker compose restart`, comme un redémarrage du serveur, relance
tout en même temps ; le backend démarre alors avant Keycloak, écrit une erreur (envoyée à Sentry), et se connecte à la
première requête.

### Changer un mot de passe de la base

Le script d'initialisation ne s'exécute plus : on change le mot de passe dans PostgreSQL, puis dans le `.env`.

```shell
docker compose exec postgres psql -U postgres
```

Dans psql, `\password recallio` (ou `\password keycloak`), puis `\q`. Reporte-le dans le `.env`
(`RECALLIO_DB_PASSWORD` ou `KEYCLOAK_DB_PASSWORD`), puis `docker compose up -d`.

### Dépannage

| Symptôme | Cause probable | Que faire |
|---|---|---|
| `docker compose up` refuse : « required variable … is missing a value » | Une variable du `.env` est vide | La remplir (section 5) |
| Le site répond par une erreur de certificat | Caddy n'a pas obtenu ses certificats | `docker compose logs caddy` : le DNS ne pointe pas vers `<ip>`, ou le port 80 est fermé (`sudo ufw status`). Ne supprime jamais le volume `caddy-data` : Let's Encrypt limite les demandes (5 certificats identiques par semaine) |
| Le backend reste `unhealthy` | La base ou Keycloak | `docker compose logs backend` : « password authentication failed », le mot de passe du `.env` n'est plus celui de la base (section 10, « Changer un mot de passe ») ; une erreur OIDC, Keycloak n'est pas prêt (`docker compose ps`) |
| `https://<domaine>/api/…` répond 502 | Le backend démarre, ou le front ne l'a pas retrouvé après sa recréation | Attendre 30 secondes ; sinon `docker compose restart frontend` |
| L'e-mail de vérification n'arrive pas | Brevo, le domaine ou les indésirables | Les indésirables ; *Transactional*, *Logs* dans Brevo ; *Test connection* dans la console (section 7) ; un changement du `.env` sur SMTP n'a pas d'effet après le premier démarrage : il se fait dans la console |
| La page de connexion a l'allure par défaut de Keycloak (grise, carrée) | Le royaume ne nomme pas le thème, ou son dossier n'est pas monté | Section 7, « Le thème des pages de connexion » |
| La connexion échoue : « Un paramètre n'est pas valide : redirect_uri » | Le domaine a changé après l'import du royaume | Dans la console, royaume `recallio`, *Clients*, `recallio-frontend` : corriger les adresses |
| La console, par le tunnel, reste blanche ou redirige vers `https://auth.<domaine>/realms/master/…` | L'adresse du royaume `master` n'est pas réglée | La commande `kcadm.sh` de la section 7 |
| Plus aucun administrateur Keycloak (OTP perdu) | | Le guide de Keycloak « Bootstrapping and recovering an admin account » (commande `bootstrap-admin`) |
| Une erreur « Failed to create OidcProviderClientImpl » dans Sentry, après un redémarrage du serveur | Le backend a démarré avant Keycloak | Rien : il se connecte à la première requête |

## 11. Le serveur du runner

Ce serveur ne porte que le runner et les images du bac à sable : l'accès du runner au socket Docker vaut les droits root
sur l'hôte (ADR 0030). Le backend le joint par un tunnel WireGuard, de `10.13.13.1` (l'application) à `10.13.13.2`
(le runner), avec le jeton partagé. Les décisions sont dans l'ADR [0072](adr/0072-le-serveur-du-runner.md).

Dans cette section, `<ip-runner>` est l'adresse IPv4 publique du serveur du runner, et `<ip>` reste celle du serveur de
l'application. Chaque bloc dit sur quel serveur il se tape ; les commandes `docker compose` du runner se lancent depuis
`/opt/recallio/infra/production/runner`.

### Le serveur

Un VPS-1 d'OVHcloud (2 vCores, 4 Go), en **Debian 13** ou **Ubuntu 24.04 LTS** (la même que l'application, pour n'avoir
qu'une façon de faire), avec ta clé SSH publique ajoutée à la commande.

### Préparer le serveur

Sur le runner, suis la section 3, avec `<ip-runner>` à la place de `<ip>` : un utilisateur à toi et SSH par clé
seulement, les mises à jour de sécurité automatiques, Docker Engine et le plugin Compose, l'heure en UTC. **Sauf le
pare-feu**, qui diffère : ci-dessous, à la place de celui de la section 3. **Puis les core dumps**, une fois Docker
installé.

#### Le pare-feu

Sur le runner, à la place de « Le pare-feu » de la section 3 :

```shell
sudo apt-get update && sudo apt-get install -y ufw
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp
sudo ufw allow from <ip> to any port 51820 proto udp
sudo ufw enable
sudo ufw status verbose
```

Attendu : `Status: active`, `22/tcp ALLOW IN Anywhere` (en IPv4 et en IPv6) et `51820/udp ALLOW IN <ip>` : WireGuard,
depuis le seul serveur de l'application. Ni 80, ni 443, ni 8090 : Docker publie le port du runner lui-même, sur
l'adresse du tunnel seulement.

Le serveur de l'application, lui, n'ouvre **aucun port de plus** : c'est lui qui appelle, et `ufw` laisse revenir les
réponses d'une connexion qu'il a ouverte ; l'envoi d'un paquet toutes les 25 secondes (`PersistentKeepalive`) la garde
ouverte.

#### Les core dumps

Une fois Docker installé. Un programme C++ qui plante dans le bac à sable ne doit rien laisser sur l'hôte (ADR 0053).
Sur Ubuntu, apport (ou systemd-coredump, s'il est installé) reçoit l'image mémoire de tout programme qui plante,
conteneurs compris, malgré leurs limites : le noyau la lui confie quand le motif `kernel.core_pattern` commence par `|`.
Sur le runner :

```shell
printf 'kernel.core_pattern=core\nfs.suid_dumpable=0\n' | sudo tee /etc/sysctl.d/90-recallio-core-dumps.conf
sudo systemctl disable --now apport.service && sudo systemctl mask apport.service    # sur Ubuntu seulement
sudo sysctl --system > /dev/null
sysctl kernel.core_pattern fs.suid_dumpable
```

Attendu : `kernel.core_pattern = core` et `fs.suid_dumpable = 0`. Sur Debian, apport n'existe pas : saute la deuxième
ligne. Ne désinstalle pas apport sur Ubuntu : le méta-paquet `ubuntu-server` partirait avec lui.

Puis une limite de 0 pour l'image mémoire de tout conteneur ; sans elle, le noyau l'écrirait dans le conteneur :

```shell
printf '{\n  "default-ulimits": {\n    "core": { "Name": "core", "Hard": 0, "Soft": 0 }\n  }\n}\n' | sudo tee /etc/docker/daemon.json
sudo systemctl restart docker
```

Sur un Docker neuf, `/etc/docker/daemon.json` n'existe pas ; s'il existe déjà, ajoute-y la clé `default-ulimits` à la
main plutôt que de l'écraser. La vérification vient après le premier démarrage (« Vérifier »).

### Récupérer la configuration

Le même dépôt, au même emplacement ; on ne se sert que de `infra/production/runner/`. Sur le runner :

```shell
sudo apt-get install -y git jq
sudo install -d -o recallio -g recallio /opt/recallio
git clone https://github.com/potichacha/recallio.git /opt/recallio
cd /opt/recallio/infra/production/runner
ls -A
```

Attendu : `.env.example  compose.yaml  wireguard`. Sur le serveur de l'application, `git pull` dans `/opt/recallio`,
pour avoir `infra/production/app/wireguard/`.

### WireGuard, sur les deux serveurs

**Les clés.** Sur chacun des deux serveurs :

```shell
sudo apt-get install -y wireguard-tools iputils-ping
sudo sh -c 'cd /etc/wireguard && umask 077 && wg genkey > private.key && wg pubkey < private.key > public.key'
sudo cat /etc/wireguard/public.key
```

Attendu : 44 caractères qui finissent par `=`, la clé publique du serveur. Note les deux : elles ne sont pas secrètes.
La clé privée, elle, ne quitte jamais son serveur.

**La clé partagée**, tirée une fois, sur le runner :

```shell
sudo sh -c 'umask 077 && wg genpsk > /etc/wireguard/preshared.key'
sudo cat /etc/wireguard/preshared.key
```

Copie-la, puis recopie-la sur le serveur de l'application (la commande attend que tu la colles, sans l'afficher) :

```shell
read -rsp 'Clé partagée : ' psk && echo
printf '%s\n' "$psk" | sudo sh -c 'umask 077 && cat > /etc/wireguard/preshared.key' && unset psk
sudo wc -c /etc/wireguard/preshared.key
```

Attendu : `45 /etc/wireguard/preshared.key`. Comme le jeton du runner, elle est secrète : garde-la dans ton gestionnaire
de mots de passe, jamais dans un fichier de ton poste.

**Le tunnel, côté runner.** Sur le runner, remplace `COLLE-ICI…` par la clé publique du serveur de l'application :

```shell
cd /opt/recallio/infra/production/runner
sudo install -m 600 wireguard/wg0.conf.example /etc/wireguard/wg0.conf
sudo sh -c 'sed -i "s|<RUNNER_PRIVATE_KEY>|$(cat /etc/wireguard/private.key)|; s|<PRESHARED_KEY>|$(cat /etc/wireguard/preshared.key)|" /etc/wireguard/wg0.conf'
app_public_key=COLLE-ICI-LA-CLE-PUBLIQUE-DE-L-APPLICATION
sudo sed -i "s|<APP_PUBLIC_KEY>|$app_public_key|" /etc/wireguard/wg0.conf
sudo grep -c '<[A-Z_]*>' /etc/wireguard/wg0.conf
sudo ls -l /etc/wireguard/wg0.conf
```

Attendu : `0` (plus aucun marqueur), et `-rw------- 1 root root`.

**Le tunnel, côté application.** Sur le serveur de l'application, avec la clé publique du runner et `<ip-runner>` :

```shell
cd /opt/recallio/infra/production/app
sudo install -m 600 wireguard/wg0.conf.example /etc/wireguard/wg0.conf
sudo sh -c 'sed -i "s|<APP_PRIVATE_KEY>|$(cat /etc/wireguard/private.key)|; s|<PRESHARED_KEY>|$(cat /etc/wireguard/preshared.key)|" /etc/wireguard/wg0.conf'
runner_public_key=COLLE-ICI-LA-CLE-PUBLIQUE-DU-RUNNER
sudo sed -i "s|<RUNNER_PUBLIC_KEY>|$runner_public_key|; s|<RUNNER_PUBLIC_IP>|<ip-runner>|" /etc/wireguard/wg0.conf
sudo grep -c '<[A-Z_]*>' /etc/wireguard/wg0.conf
```

Attendu : `0`.

**Démarrer**, sur le runner d'abord, puis sur l'application :

```shell
sudo systemctl enable --now wg-quick@wg0
```

Puis, sur l'application :

```shell
ping -c 3 10.13.13.2
sudo wg show
```

Attendu : trois réponses ; puis `latest handshake: … seconds ago`, `transfer: … received, … sent` et
`persistent keepalive: every 25 seconds`. Sur le runner :

```shell
ping -c 3 10.13.13.1
sudo wg show
```

Attendu : trois réponses, et pour le pair `endpoint: <ip>:<port>` et une poignée de main récente. Le tunnel redémarre
seul avec chaque serveur (`enable`).

### Docker après WireGuard

Le runner publie son port sur `10.13.13.2`, qui n'existe qu'une fois `wg0` monté. Si Docker démarre avant, la liaison
échoue, le runner reste arrêté, et **Docker ne réessaie jamais** (ADR 0072). Un « drop-in » systemd fait attendre
Docker. Sur le runner :

```shell
sudo mkdir -p /etc/systemd/system/docker.service.d
printf '[Unit]\nAfter=wg-quick@wg0.service\nWants=wg-quick@wg0.service\n' | sudo tee /etc/systemd/system/docker.service.d/10-after-wireguard.conf
sudo systemctl daemon-reload
systemctl show docker.service -p After -p Wants | grep -o 'wg-quick@wg0.service'
```

Attendu : `wg-quick@wg0.service` deux fois. Le redémarrage du serveur qui le prouve vient à la fin de « Vérifier ».

### Configurer et démarrer

Sur le runner :

```shell
cd /opt/recallio/infra/production/runner
cp .env.example .env
chmod 600 .env
```

**Le jeton**, le même que celui de l'application. Sur le serveur de l'application, affiche-le et copie-le :

```shell
grep '^RECALLIO_RUNNER_TOKEN=' /opt/recallio/infra/production/app/.env | cut -d= -f2
```

Puis, sur le runner, colle-le (il ne s'affiche pas) :

```shell
read -rsp 'Jeton du runner : ' token && echo
sed -i "s/^RECALLIO_RUNNER_TOKEN=.*/RECALLIO_RUNNER_TOKEN=$token/" .env && unset token
grep '^RECALLIO_RUNNER_TOKEN=' .env | awk -F= '{ print length($2) }'
```

Attendu : `64`. **Le reste**, avec `nano .env` :

- `RECALLIO_TAG` : **le même tag que sur le serveur de l'application** (section 5) ;
- `SENTRY_DSN_RUNNER` : le DSN du projet `recallio-runner`, facultatif (ADR 0070) ;
- `WIREGUARD_ADDRESS` reste `10.13.13.2`, et `RECALLIO_SANDBOX_DOCKER_RUNTIME` reste vide (« Essayer gVisor »).

```shell
docker compose config --quiet && echo OK
```

Attendu : `OK`. **Le premier démarrage** :

```shell
docker compose pull
docker compose up -d
docker compose ps -a
```

Les images du bac à sable sont lourdes : environ **1 Go à télécharger** (le runner 190 Mo, Java 384 Mo, Go 331 Mo, C++
133 Mo, Python 52 Mo) et **5 Go sur le disque**. Compte quelques minutes pour le `pull`, selon le débit du serveur ;
ensuite, `up -d` prend une dizaine de secondes. Attendu : les quatre services `sandbox-*` en `Exited (0)` (ils ne font
que s'assurer que leur image est là), et `runner` en `Up … (healthy)`, avec `10.13.13.2:8090->8090/tcp`.

### Vérifier

**La santé du runner, par le tunnel.** Sur le serveur de l'application :

```shell
curl -s http://10.13.13.2:8090/q/health/ready
cd /opt/recallio/infra/production/app
docker compose exec backend sh -c 'exec 3<>/dev/tcp/10.13.13.2/8090 && printf "GET /q/health/ready HTTP/1.0\r\n\r\n" >&3 && head -n1 <&3'
```

Attendu : `"status": "UP"`, puis `HTTP/1.0 200 OK` : le backend, depuis son conteneur, joint le runner par le tunnel.

**Le parcours réel**, dans la PWA, connecté : rends un exercice de code (une carte d'exercice, *Rendre*) ; le résultat
de chaque test s'affiche. Puis ouvre une page du Tour et *Exécute* son exemple ; sa sortie s'affiche. Aucun message
« L'exécution du code est indisponible ».

**Chaque langage, et ses durées.** Sur le runner, ces fonctions lancent un programme ou rendent la solution d'un
exercice du dépôt, et affichent la durée de la requête :

```shell
cd /opt/recallio/infra/production/runner
token=$(grep '^RECALLIO_RUNNER_TOKEN=' .env | cut -d= -f2)
runner=http://10.13.13.2:8090
program() {
  local out; out=$(mktemp)
  jq -n --arg l "$1" --arg p "$2" --arg c "$3" '{language: $l, files: [{path: $p, content: $c}]}' \
    | curl -s -o "$out" -w "$1 : HTTP %{http_code}, %{time_total} s, " -H "Authorization: Bearer $token" \
        -H 'Content-Type: application/json' --data-binary @- "$runner/api/v1/programs"
  if [ -s "$out" ]; then jq -r '.status + " " + (.stdout | rtrimstr("\n"))' "$out"; else echo "-"; fi; rm -f "$out"
}
files() {
  (cd "$1" && find . -type f | sed 's|^\./||' | sort | while read -r f; do
    jq -n --arg p "$f" --rawfile c "$f" '{path: $p, content: $c}'
  done) | jq -s .
}
exercise() {
  local out; out=$(mktemp)
  jq -n --arg l "$1" --argjson code "$(files "$2/solution")" --argjson tests "$(files "$2/tests")" \
      '{language: $l, code: $code, tests: $tests}' \
    | curl -s -o "$out" -w "${2##*/} : HTTP %{http_code}, %{time_total} s, " -H "Authorization: Bearer $token" \
        -H 'Content-Type: application/json' --data-binary @- "$runner/api/v1/runs"
  if [ -s "$out" ]; then jq -r .status "$out"; else echo "-"; fi; rm -f "$out"
}
```

Puis :

```shell
program JAVA Main.java 'void main() { IO.println("Bonjour"); }'
program GO main.go 'package main
import "fmt"
func main() { fmt.Println("Bonjour") }'
program CPP main.cpp '#include <print>
int main() { std::println("Bonjour"); }'
program PYTHON main.py 'print("Bonjour")'
cd /opt/recallio/content
exercise JAVA java/exercices/bases/boucle-for/fizz-buzz
exercise GO go/exercices/bases/boucle-for/etapes-collatz
exercise CPP cpp/exercices/bases/boucle-for/somme-multiples
exercise PYTHON python/exercices/bases/boucle-for-range/fizzbuzz
exercise JAVA spring-boot/exercices/web/rest-controller/catalogue-livres
exercise JAVA quarkus/exercices/rest/quarkus-rest/catalogue-produits
```

Attendu : `HTTP 200` partout, `COMPLETED Bonjour` pour chaque programme, `PASSED` pour chaque exercice. **Note les
durées** : sur un poste de développement, Python et C++ prennent moins d'une seconde à 2 secondes, Go et Java 1 à 3,
l'exercice Spring Boot 5 à 6, et l'exercice Quarkus 13 à 20, pour un délai de 30 (ADR 0072). Un exercice Quarkus
au-delà de 25 secondes sur le serveur serait trop près du délai : à régler avant d'ouvrir la bêta.

**Deux exécutions à la fois, pas trois** : lance trois fois l'exercice Quarkus ensemble.

```shell
for i in 1 2 3; do exercise JAVA quarkus/exercices/rest/quarkus-rest/catalogue-produits & sleep 0.3; done; wait
```

Attendu : un `HTTP 503` aussitôt (les deux places du runner étaient prises ; le backend le traduit en « réessaie dans un
moment »), puis deux `HTTP 200` et `PASSED`.

**Le port fermé depuis Internet.** Sur ton poste, dans PowerShell :

```powershell
Test-NetConnection <ip-runner> -Port 8090 | Select-Object TcpTestSucceeded
```

Attendu : `False` (après quelques secondes d'attente).

**Les core dumps désactivés.** Sur le runner :

```shell
sysctl kernel.core_pattern fs.suid_dumpable
cd /opt/recallio/infra/production/runner
docker compose exec runner sh -c 'echo "limite : $(ulimit -c) / $(ulimit -Hc)"'
```

Attendu : `kernel.core_pattern = core`, `fs.suid_dumpable = 0`, puis `limite : 0 / 0` (la limite de `daemon.json`, que
chaque conteneur reçoit). Puis un vrai programme C++ qui plante, dans le bac à sable, par le runner :

```shell
jq -n --arg c '#include <print>
int main() { std::println("avant"); volatile int *p = nullptr; return *p; }' \
    '{language: "CPP", files: [{path: "main.cpp", content: $c}]}' \
  | curl -s -H "Authorization: Bearer $token" -H 'Content-Type: application/json' --data-binary @- \
      "$runner/api/v1/programs"
```

Attendu : `{"status":"COMPLETED","exitCode":139,"stdout":"avant\n","stderr":"Segmentation fault\n",…}`. Et le même
plantage dans l'image du bac à sable, avec ses options, tel qu'un terminal l'afficherait :

```shell
image=$(docker compose config --images | grep sandbox-cpp)
docker run --rm --network none --read-only --tmpfs /tmp:rw,exec,size=96m --user 65534:65534 --entrypoint bash "$image" \
  -c 'cd /tmp && echo "int main() { volatile int *p = nullptr; return *p; }" > crash.cpp && g++ -o crash crash.cpp && ./crash; ls /tmp'
ls /var/crash 2>/dev/null; coredumpctl list 2>&1 | tail -1
```

Attendu : une ligne `… Segmentation fault ./crash`, **sans** `(core dumped)`, puis `crash  crash.cpp` (aucun fichier
`core`) ; enfin, rien dans `/var/crash`, et `coredumpctl` absent (« command not found ») ou sans entrée (« No coredumps
found »). Un `(core dumped)` voudrait dire qu'un réglage manque : `sysctl`, `daemon.json`, ou apport encore actif.

**Après un redémarrage du serveur.** Sur le runner :

```shell
sudo reboot
```

Une minute après, reconnecte-toi :

```shell
cd /opt/recallio/infra/production/runner
docker compose ps
sysctl kernel.core_pattern
sudo journalctl -b -u wg-quick@wg0 -u docker -o short-monotonic | grep -E 'Finished|Started'
```

Attendu : `runner` en `Up … (healthy)`, `kernel.core_pattern = core`, et la ligne `Finished … wg0` avant
`Started docker.service` : Docker a attendu le tunnel. Depuis l'application,
`curl -s http://10.13.13.2:8090/q/health/ready` répond de nouveau `UP` (le tunnel se rétablit en moins de 30 secondes).

### Mettre à jour

Les deux serveurs portent **le même tag**, et **le runner se met à jour d'abord** : un nouveau backend ne parle ainsi
jamais à un ancien runner (ADR 0072). Sur le runner :

```shell
cd /opt/recallio/infra/production/runner
git pull                                      # seulement si infra/production/runner a changé
diff <(grep -oE '^[A-Z_]+=' .env.example) <(grep -oE '^[A-Z_]+=' .env)
nano .env                                     # RECALLIO_TAG=sha-<nouveau commit>
docker compose pull
docker compose up -d
docker compose ps -a
```

- Le `diff` ne doit rien afficher (une ligne `<` est une variable à ajouter au `.env`).
- Compose recrée les services du bac à sable avec leur nouvelle image, puis le runner : pendant ses quelques secondes de
  redémarrage, rendre un exercice répond « réessaie dans un moment ».
- Attendu : les `sandbox-*` en `Exited (0)`, le `runner` en `Up … (healthy)`.

Puis le serveur de l'application, au même tag (section 9). **Revenir en arrière** : l'application d'abord, au tag
précédent, puis le runner.

### Exploiter

**Les logs** du runner sont en JSON (ADR 0070), sans le code des joueurs ; la sortie d'erreur d'un programme qui échoue
de façon inattendue y est écrite en `WARN`.

```shell
docker compose logs --tail 100 runner
docker compose logs --no-log-prefix runner | jq -R 'fromjson? | select(.level == "ERROR" or .level == "WARN") | .message'
```

**L'espace disque.** Chaque version apporte ses images du bac à sable (jusqu'à 5 Go, moins quand elles n'ont pas
changé : les couches identiques ne se retéléchargent pas).

```shell
df -h /
docker system df
docker image prune -a
```

`docker image prune -a` supprime les images des versions précédentes et **garde celles de la version en place** : les
conteneurs arrêtés des services `sandbox-*` les référencent. **N'utilise jamais `docker system prune` ni
`docker container prune`** : ils suppriment ces conteneurs, et le `prune -a` suivant supprimerait les images du bac à
sable. Le runner tirerait alors l'image à la première exécution, au-delà de son délai : l'exercice compterait comme
raté. Si c'est arrivé : `docker compose pull && docker compose up -d`.

**Dépannage** :

| Symptôme | Cause probable | Que faire |
|---|---|---|
| `sudo wg show` : pas de `latest handshake` | Le pare-feu du runner, une clé ou l'adresse du runner | Sur le runner, `sudo ufw status` : `51820/udp` depuis `<ip>` ; des deux côtés, la clé publique du pair (`PublicKey`) doit être le `public key` affiché de l'autre côté ; sur l'application, `Endpoint` doit être `<ip-runner>:51820` ; puis `sudo systemctl restart wg-quick@wg0` |
| Le runner reste `unhealthy` ou redémarre | Le jeton, ou le socket Docker | `docker compose logs runner` : « doit faire au moins 32 caractères », le jeton du `.env` est vide ou coupé (section « Configurer et démarrer ») |
| La PWA dit « L'exécution du code est indisponible » (503) | Le tunnel, le runner, ou le jeton | Sur l'application, `docker compose logs backend \| grep -i runner` : « Le runner est injoignable », le tunnel ou le runner (`curl` de la santé, ci-dessus) ; « Le runner a répondu 401 », les deux jetons diffèrent ; « 503 », deux exécutions étaient en cours, c'est normal |
| Après un redémarrage du serveur, le runner est `Exited` : « failed to bind host port 10.13.13.2:8090/tcp: cannot assign requested address » (`docker inspect -f '{{.State.Error}}' recallio-runner-runner-1`) | Docker a démarré avant `wg0` | Vérifie le drop-in (« Docker après WireGuard ») et que `wg-quick@wg0` démarre (`systemctl status wg-quick@wg0`), puis `docker compose up -d` |
| Un exercice dépasse son délai juste après un nettoyage du disque | L'image du bac à sable a été supprimée (`docker system prune`) | `docker compose pull && docker compose up -d` |

### Essayer gVisor

Facultatif, et **pas activé pour la bêta** (ADR 0072) : gVisor (`runsc`) intercale un noyau en espace utilisateur entre
le code des joueurs et celui de l'hôte, ce qui réduit fortement le risque d'évasion, mais ralentit la compilation et le
démarrage des JVM. À essayer avant l'ouverture à tous : on l'active si l'exercice Quarkus reste sous les 20 secondes.

**Mesurer d'abord sans lui** : les fonctions et les commandes de « Chaque langage, et ses durées », ci-dessus ; note les
durées.

**L'installer**, depuis le dépôt officiel (la page
[gvisor.dev/docs/user_guide/install](https://gvisor.dev/docs/user_guide/install/) fait foi si elle a changé). Sur le
runner :

```shell
sudo apt-get install -y ca-certificates curl gnupg
curl -fsSL https://gvisor.dev/archive.key | sudo gpg --dearmor -o /usr/share/keyrings/gvisor-archive-keyring.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/gvisor-archive-keyring.gpg] https://storage.googleapis.com/gvisor/releases release main" \
  | sudo tee /etc/apt/sources.list.d/gvisor.list > /dev/null
sudo apt-get update && sudo apt-get install -y runsc
docker info --format '{{json .Runtimes}}' | grep -o '"runsc"'
sudo cat /etc/docker/daemon.json
```

Attendu : `"runsc"` (le paquet déclare le runtime à Docker), et `daemon.json` qui porte à la fois `default-ulimits` et
`runtimes`. Si `"runsc"` manque : `sudo runsc install && sudo systemctl reload docker`. Puis :

```shell
cd /opt/recallio/infra/production/runner
docker run --rm --runtime=runsc --entrypoint uname "$(docker compose config --images | grep sandbox-python)" -a
```

Attendu : une ligne `Linux …` dont la version n'est pas celle de l'hôte (`uname -r`) : c'est le noyau de gVisor qui
répond.

**L'activer pour le runner** : dans le `.env`, `RECALLIO_SANDBOX_DOCKER_RUNTIME=runsc`, puis :

```shell
docker compose up -d
docker compose ps
```

Attendu : le runner recréé, `Up … (healthy)`. Relance les mêmes mesures, et compare : chaque programme et chaque
exercice doit encore réussir, et l'exercice Quarkus rester sous les 20 secondes. Essaie aussi un exercice dans la PWA.

**Revenir en arrière** : vide la variable (`RECALLIO_SANDBOX_DOCKER_RUNTIME=`) dans le `.env`, puis
`docker compose up -d`. Le runtime reste installé sans servir. Pour le retirer aussi : `sudo apt-get purge -y runsc`,
retire l'entrée `runsc` de `runtimes` dans `/etc/docker/daemon.json` (en gardant `default-ulimits`), puis
`sudo systemctl restart docker`.

## 12. Les sauvegardes

Chaque nuit, vers 02:15 UTC, le serveur de l'application sauvegarde ses deux bases (`recallio` et `keycloak`), les
chiffre, et les envoie dans un bucket Backblaze B2, hors d'OVHcloud. Chaque sauvegarde y reste 30 jours. Les décisions
sont dans l'ADR [0073](adr/0073-les-sauvegardes.md).

| Question | Réponse |
|---|---|
| Qu'est-ce qui est sauvegardé ? | Les deux bases, en entier : les comptes et la progression (`recallio`), les identités, les mots de passe hachés et les réglages du royaume (`keycloak`) |
| Qu'est-ce qui ne l'est pas ? | Le `.env`, les clés WireGuard et la clé privée des sauvegardes : ils sont dans ton gestionnaire de mots de passe (sections 1 et 11). Les certificats de Caddy : il les redemande. Le contenu : il est dans l'image du backend |
| Et le serveur du runner ? | Rien à sauvegarder : il ne garde aucune donnée. Il se remonte en suivant la section 11 |
| Qui peut lire une sauvegarde ? | Toi seul, avec la clé privée d'age, qui n'est sur aucun serveur. Ni Backblaze, ni quelqu'un qui aurait pris le serveur |
| Qui peut en supprimer une ? | Personne avant 30 jours : le bucket les verrouille (Object Lock). La clé du serveur ne peut qu'écrire et lister |

Trois endroits, dans cette section : **la console de Backblaze**, dans ton navigateur ; **ton poste**, dans Git Bash sauf
mention de PowerShell ; **le serveur de l'application**. Chaque bloc dit où il se tape.

### Le bucket

Dans la console de Backblaze (*B2 Cloud Storage*, *Buckets*, *Create a Bucket*) ; ses libellés font foi s'ils ont
changé :

1. **Le nom** : unique chez Backblaze, par exemple `recallio-backups-` suivi de quelques caractères au hasard. Note-le :
   c'est `<bucket>` dans la suite.
2. **Files in Bucket are** : *Private*.
3. **Default Encryption** : *Disable*. Les sauvegardes arrivent déjà chiffrées, avec une clé que Backblaze n'a pas.
4. **Object Lock** : *Enable*. C'est lui qui empêche de supprimer une sauvegarde, et il ne s'active bien qu'à la
   création.

Puis, sur la ligne du bucket :

5. **Object Lock**, la règle par défaut (*Default Retention Policy*) : mode *Governance*, **30 jours**. Chaque fichier
   déposé est alors verrouillé 30 jours : pendant ce temps, ni la clé du serveur ni une règle du bucket ne peuvent le
   supprimer ; toi seul le peux, depuis ton compte.
6. **Lifecycle Settings**, *Use custom lifecycle rules*, une règle :

   | Champ | Nom dans l'API | Valeur |
   |---|---|---|
   | Le préfixe des fichiers | `fileNamePrefix` | vide (tout le bucket) |
   | Les jours avant de masquer | `daysFromUploadingToHiding` | `30` |
   | Les jours avant de supprimer | `daysFromHidingToDeleting` | `1` |

   Une sauvegarde est ainsi masquée 30 jours après son envoi, puis supprimée le lendemain. Backblaze applique ces
   règles une fois par jour : compte **32 jours au plus** entre l'envoi et la suppression.
7. Note l'**Endpoint** affiché sur la ligne du bucket, `s3.eu-central-003.backblazeb2.com` par exemple : sa partie du
   milieu est la région, `<région>` dans la suite.

### La clé du serveur

Le serveur reçoit une clé qui ne peut **que lister et écrire** dans ce bucket : ni lire, ni supprimer. La console ne
propose que trois profils tout faits, sans cette combinaison : la clé se crée avec l'outil en ligne de commande de
Backblaze.

1. Dans la console, *Application Keys* : génère la clé maîtresse (*Generate New Master Application Key*) et garde la
   page ouverte. Elle peut tout : elle ne va jamais sur le serveur.
2. Sur ton poste, télécharge `b2-windows.exe` depuis les
   [versions de l'outil](https://github.com/Backblaze/B2_Command_Line_Tool/releases/latest), puis, dans PowerShell, dans
   le dossier du téléchargement :

   ```powershell
   .\b2-windows.exe account authorize
   .\b2-windows.exe key create --bucket <bucket> recallio-backup-server listBuckets,listAllBucketNames,listFiles,writeFiles
   .\b2-windows.exe account clear
   ```

   La première commande demande le `keyID` et la clé maîtresse. La deuxième affiche deux valeurs, **une seule fois** :
   le `keyID` de la nouvelle clé, puis son `applicationKey`. Garde-les dans ton gestionnaire de mots de passe. La
   troisième efface la clé maîtresse du poste.
3. Dans la console, *Application Keys* : la clé `recallio-backup-server` doit afficher le bucket `<bucket>` et les
   quatre droits `listBuckets, listAllBucketNames, listFiles, writeFiles`. **Ni `readFiles`, ni `deleteFiles`, ni
   `bypassGovernance`** : sinon, supprime-la et recommence.

Avec `writeFiles`, la clé peut encore masquer un fichier ou déposer un fichier du même nom : c'est pourquoi le bucket
verrouille chaque fichier 30 jours (ADR 0073).

### La paire de clés qui chiffre

Les sauvegardes sont chiffrées avec [age](https://age-encryption.org) : le serveur n'a que la **clé publique**, qui
chiffre ; la **clé privée**, qui déchiffre, naît sur ton poste et n'en sort que vers ton gestionnaire de mots de passe.

Sur ton poste, dans PowerShell, une fois :

```powershell
winget install --id FiloSottile.age -e
```

Sans `winget`, l'archive Windows des [versions d'age](https://github.com/FiloSottile/age/releases/latest) contient
`age.exe` et `age-keygen.exe`. Puis, dans un **nouveau** Git Bash :

```shell
age-keygen -o cle-sauvegardes-recallio.txt
cat cle-sauvegardes-recallio.txt
```

Attendu : `Public key: age1…`, puis les trois lignes du fichier : une date, la clé publique en commentaire, et la clé
privée, `AGE-SECRET-KEY-1…`.

1. **Copie les trois lignes dans ton gestionnaire de mots de passe**, et une seconde copie ailleurs (imprimée, dans un
   lieu sûr). **Sans cette clé, aucune sauvegarde ne se lit, et personne ne peut la retrouver.**
2. Note la clé publique (`age1…`, 62 caractères) : elle n'est pas secrète.
3. Supprime le fichier : `rm cle-sauvegardes-recallio.txt`. Tu le recréeras depuis le gestionnaire pour restaurer.

### Installer les outils sur le serveur

Sur le serveur de l'application :

```shell
sudo apt-get update && sudo apt-get install -y age rclone curl
age --version && rclone version | head -1
cd /opt/recallio && git pull
ls infra/production/app/backup
```

Attendu : age 1.1 ou plus, rclone 1.60 ou plus (les paquets de Debian 13 et d'Ubuntu 24.04 suffisent), puis
`backup.env.example  backup.sh  rclone.conf.example  recallio-backup.service  recallio-backup.timer`.

### Configurer

La configuration vit hors du dépôt, dans `/etc/recallio-backup/`, lisible par root seul. Sur le serveur :

```shell
cd /opt/recallio/infra/production/app
sudo install -d -m 700 /etc/recallio-backup
sudo install -m 600 backup/backup.env.example /etc/recallio-backup/backup.env
sudo install -m 600 backup/rclone.conf.example /etc/recallio-backup/rclone.conf
```

**La clé publique et le bucket**, en remplaçant les deux `COLLE-ICI…` :

```shell
age_public_key=COLLE-ICI-LA-CLE-PUBLIQUE-AGE
bucket=COLLE-ICI-LE-NOM-DU-BUCKET
sudo sed -i "s|^BACKUP_AGE_RECIPIENT=.*|BACKUP_AGE_RECIPIENT=$age_public_key|; s|<BUCKET>|$bucket|" /etc/recallio-backup/backup.env
```

**La clé du serveur**, en remplaçant le `keyID` et la région ; l'`applicationKey` se colle sans s'afficher :

```shell
key_id=COLLE-ICI-LE-KEYID
region=eu-central-003
read -rsp 'applicationKey : ' key && echo
sudo sed -i "s|<B2_KEY_ID>|$key_id|; s|<B2_APPLICATION_KEY>|$key|; s|<B2_REGION>|$region|g" /etc/recallio-backup/rclone.conf && unset key
sudo grep -c '<[A-Z0-9_]*>' /etc/recallio-backup/backup.env /etc/recallio-backup/rclone.conf
sudo ls -l /etc/recallio-backup
```

Attendu : `0` pour chaque fichier (plus aucun marqueur), et deux fichiers `-rw------- 1 root root`. Puis vérifie que la
clé entre dans le bucket :

```shell
sudo sh -c '. /etc/recallio-backup/backup.env && rclone --config /etc/recallio-backup/rclone.conf ls "$BACKUP_DESTINATION"' && echo OK
```

Attendu : `OK`, sans rien d'autre (le dossier est encore vide). Une erreur `403` ou `SignatureDoesNotMatch` : le
`keyID`, l'`applicationKey` ou la région (« Dépannage »).

### La première sauvegarde, à la main

Sur le serveur :

```shell
cd /opt/recallio/infra/production/app
sudo install -m 644 backup/recallio-backup.service backup/recallio-backup.timer /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl start recallio-backup.service && echo OK
journalctl -u recallio-backup.service -o cat --since -5min | grep '^sauvegarde'
```

La commande `start` attend la fin de la sauvegarde (quelques secondes). Attendu : `OK`, puis :

```text
sauvegarde : début, vers recallio-backup:<bucket>/postgres
sauvegarde : base recallio sauvegardée et chiffrée (8456591 octets)
sauvegarde : base keycloak sauvegardée et chiffrée (254320 octets)
sauvegarde : recallio-2026-11-24T0858Z.dump.age envoyé et retrouvé dans la destination (8456591 octets)
sauvegarde : keycloak-2026-11-24T0858Z.dump.age envoyé et retrouvé dans la destination (254320 octets)
sauvegarde : terminé : 2 bases sauvegardées
```

Une base neuve pèse environ 8 Mo, presque tout en contenu ; le nom de chaque fichier porte l'heure UTC du lancement.
Vérifie ensuite :

- dans la console de Backblaze, *Browse Files*, `<bucket>`, `postgres` : les deux fichiers, et, dans le détail de
  chacun, un verrou (*Object Lock*) jusqu'à dans 30 jours ;
- sur le serveur, que la clé ne peut pas relire ce qu'elle a déposé, et que rien n'est resté sur le disque :

  ```shell
  sudo sh -c '. /etc/recallio-backup/backup.env && rclone --config /etc/recallio-backup/rclone.conf cat "$BACKUP_DESTINATION/$(rclone --config /etc/recallio-backup/rclone.conf lsf "$BACKUP_DESTINATION" | tail -1)"' | head -c 21
  sudo ls -A /var/lib/recallio-backup
  ```

  Attendu : rien, ou une erreur `403`, puis rien : la clé ne lit pas, et le dossier de travail est vide. Si la
  première commande affiche `age-encryption.org/v1`, la clé a le droit `readFiles` : refais-la.

**Joue maintenant l'« Essai de restauration »**, plus bas : une sauvegarde qu'on n'a jamais restaurée n'en est pas
encore une.

### Chaque nuit

Sur le serveur :

```shell
sudo systemctl enable --now recallio-backup.timer
systemctl list-timers recallio-backup.timer
```

Attendu : une ligne dont `NEXT` est la nuit prochaine, entre 02:15 et 02:30 UTC, et `ACTIVATES` vaut
`recallio-backup.service`. Si le serveur était éteint à cette heure, la sauvegarde part à son démarrage.

### Savoir qu'une sauvegarde a manqué

Une sauvegarde qui s'arrête en silence ne se remarque que le jour où l'on en a besoin. Deux moyens.

**Un battement de cœur, facultatif et conseillé.** Un service comme [Healthchecks.io](https://healthchecks.io) (offre
gratuite) attend un appel par jour et t'écrit quand il ne vient pas. Crée un compte, puis une vérification : une
période d'**un jour**, un délai de grâce de **2 heures**. Copie son adresse d'appel (`https://hc-ping.com/…`), puis, sur
le serveur :

```shell
read -rsp 'Adresse du battement : ' url && echo
sudo sed -i "s|^BACKUP_HEARTBEAT_URL=.*|BACKUP_HEARTBEAT_URL=$url|" /etc/recallio-backup/backup.env && unset url
sudo systemctl start recallio-backup.service
journalctl -u recallio-backup.service -o cat --since -5min | grep '^sauvegarde' | tail -2
```

Attendu : `sauvegarde : battement de cœur envoyé`, puis `terminé`, et la vérification passe au vert chez Healthchecks.
Le script n'appelle cette adresse qu'après avoir retrouvé les deux fichiers dans le bucket, et n'y envoie aucune
donnée. Une sauvegarde qui échoue n'appelle rien : c'est le silence qui alerte.

**À la main**, à tout moment, sur le serveur :

```shell
systemctl list-timers recallio-backup.timer
systemctl is-failed recallio-backup.service
journalctl -u recallio-backup.service --since -2d -o cat | grep '^sauvegarde'
sudo sh -c '. /etc/recallio-backup/backup.env && rclone --config /etc/recallio-backup/rclone.conf ls "$BACKUP_DESTINATION"' | sort -t- -k2 | tail -4
```

Attendu : `LAST` date de la nuit dernière ; `inactive` (un `failed` veut dire que la dernière sauvegarde a échoué : lis
le journal) ; les lignes de la dernière sauvegarde, jusqu'à `terminé` ; et les fichiers des deux dernières nuits, avec
leur taille.

### Restaurer

Restaurer **remplace** les deux bases par celles de la sauvegarde : tout ce qui a été fait depuis est perdu. Les deux
fichiers d'une même sauvegarde portent la même heure ; restaure toujours les deux ensemble.

**1. Télécharger**, sur ton poste : dans la console de Backblaze, *Browse Files*, `<bucket>`, `postgres`, télécharge
`recallio-<date>.dump.age` et `keycloak-<date>.dump.age` de la sauvegarde choisie, dans un dossier vide.

**2. Déchiffrer**, sur ton poste, dans ce dossier. Recrée d'abord le fichier de la clé depuis ton gestionnaire de mots
de passe (`notepad cle-sauvegardes-recallio.txt`, colle les trois lignes, enregistre) :

```shell
age --decrypt --identity cle-sauvegardes-recallio.txt --output recallio.dump recallio-<date>.dump.age
age --decrypt --identity cle-sauvegardes-recallio.txt --output keycloak.dump keycloak-<date>.dump.age
ls -l recallio.dump keycloak.dump
```

Attendu : aucun message, et deux fichiers de quelques mégaoctets. age refuse un fichier abîmé ou tronqué : si une
commande échoue, n'utilise pas son résultat, et prends la sauvegarde précédente.

**3. Arrêter ce qui écrit dans les bases**, sur le serveur. Le site répond 502 jusqu'à l'étape 5 :

```shell
cd /opt/recallio/infra/production/app
docker compose stop frontend backend keycloak
```

**4. Restaurer chaque base**, depuis ton poste : le dump traverse SSH et entre dans `pg_restore`, dans le conteneur,
sans se poser sur le disque du serveur.

```shell
ssh recallio@<ip> 'cd /opt/recallio/infra/production/app && docker compose exec -T postgres pg_restore --username postgres --dbname postgres --clean --if-exists --create --exit-on-error' < recallio.dump
ssh recallio@<ip> 'cd /opt/recallio/infra/production/app && docker compose exec -T postgres pg_restore --username postgres --dbname postgres --clean --if-exists --create --exit-on-error' < keycloak.dump
```

Attendu : aucun message, pour chacune. `pg_restore` supprime la base, la recrée avec son propriétaire et ses droits,
puis la remplit. S'il répond `database "recallio" is being accessed by other users`, l'étape 3 a été oubliée : il n'a
rien touché.

**5. Redémarrer et vérifier**, sur le serveur :

```shell
docker compose up -d
docker compose ps
docker compose exec -T postgres psql -U postgres -d recallio -Atc "SELECT 'comptes', count(*) FROM app_user UNION ALL SELECT 'révisions', count(*) FROM review_log"
docker compose exec -T postgres psql -U postgres -d recallio -Atc "SELECT 'dernière révision : ' || max(reviewed_at) FROM review_log"
```

Attendu : les cinq services `Up … (healthy)` après une minute ; un nombre de comptes plausible ; une dernière révision
juste avant l'heure de la sauvegarde. Puis connecte-toi dans la PWA avec ton compte : ta progression est là.

**6. Effacer les copies en clair**, sur ton poste :

```shell
rm recallio.dump keycloak.dump cle-sauvegardes-recallio.txt
```

**7. Rejouer les suppressions de compte.** Un compte supprimé après l'heure de la sauvegarde est revenu avec elle : voir
« Les comptes supprimés », plus bas.

**Quelle version pour quelle sauvegarde.** Restaure avec le tag qui tournait le jour de la sauvegarde, ou un plus
récent, jamais un plus ancien :

- une sauvegarde **plus ancienne que l'image** : rien à faire. Au démarrage, le backend applique les migrations Flyway
  qui manquent, et Keycloak migre sa base ;
- une sauvegarde **plus récente que l'image** (tu es revenu à un ancien tag depuis) : remets d'abord le tag d'alors
  (section 9). Keycloak ne sait pas revenir en arrière, et le backend ne le peut qu'à une version près.

#### Le serveur est perdu

Sur un serveur neuf :

1. les sections 2 (le DNS, vers la nouvelle adresse), 3 et 4 ;
2. la section 5, **avec le `.env` gardé dans ton gestionnaire de mots de passe**, et non un `.env` neuf : les mots de
   passe des bases et les clés VAPID doivent être ceux d'avant ;
3. la base seule, que son script d'initialisation crée vide, avec ses deux rôles :

   ```shell
   cd /opt/recallio/infra/production/app
   docker compose pull
   docker compose up -d postgres
   docker compose ps postgres
   ```

   Attendu : `Up … (healthy)` ;
4. les étapes 1, 2 et 4 de « Restaurer », avec la dernière sauvegarde ;
5. le reste : `docker compose up -d`, puis les étapes 5 à 7 de « Restaurer », et la section 8. La section 7 est
   inutile : l'administrateur de Keycloak, le royaume et ses réglages sont dans la base restaurée ;
6. le tunnel WireGuard, avec de nouvelles clés (section 11), et les sauvegardes de ce nouveau serveur (cette section,
   avec le même bucket, la même clé publique, et une nouvelle clé de serveur à la place de l'ancienne, à supprimer dans
   la console).

### Essai de restauration

À jouer **à la mise en service**, puis **une fois par trimestre** (note la date) : il prouve que la sauvegarde de la
nuit se déchiffre, se restaure, et que tu as toujours la clé. Tout se passe sur ton poste, avec Docker Desktop, dans un
PostgreSQL jetable : rien ne touche le serveur.

1. Télécharge et déchiffre la dernière sauvegarde : les étapes 1 et 2 de « Restaurer ».
2. Restaure-la dans un conteneur à part :

   ```shell
   docker run -d --name recallio-restore-check -e POSTGRES_PASSWORD=essai postgres:18
   until docker exec recallio-restore-check pg_isready -h 127.0.0.1 -q; do sleep 1; done
   docker exec recallio-restore-check psql -U postgres -c 'CREATE ROLE recallio' -c 'CREATE ROLE keycloak'
   docker exec -i recallio-restore-check pg_restore --username postgres --dbname postgres --create --exit-on-error < recallio.dump
   docker exec -i recallio-restore-check pg_restore --username postgres --dbname postgres --create --exit-on-error < keycloak.dump
   ```

   Attendu : `CREATE ROLE` deux fois, puis aucun message. Le conteneur ne publie aucun port.
3. Compte ce qu'elle contient :

   ```shell
   docker exec recallio-restore-check psql -U postgres -d recallio -Atc "SELECT 'comptes', count(*) FROM app_user UNION ALL SELECT 'révisions', count(*) FROM review_log UNION ALL SELECT 'cartes', count(*) FROM card UNION ALL SELECT 'migrations', count(*) FROM flyway_schema_history"
   docker exec recallio-restore-check psql -U postgres -d recallio -Atc "SELECT 'dernière révision : ' || max(reviewed_at) FROM review_log"
   docker exec recallio-restore-check psql -U postgres -d keycloak -Atc "SELECT r.name, count(u.id) FROM realm r LEFT JOIN user_entity u ON u.realm_id = r.id GROUP BY r.name ORDER BY r.name"
   ```

   Attendu : les comptes, les révisions, les cartes et les migrations de la base ; une dernière révision de la veille au
   soir ; puis les utilisateurs de Keycloak par royaume (`master`, tes administrateurs ; `recallio`, les inscrits).
   Compare avec le serveur, où les mêmes requêtes se lancent par
   `docker compose exec -T postgres psql -U postgres -d recallio -Atc "…"` : les nombres du serveur sont égaux, ou un
   peu plus grands (la journée en cours).
4. Efface tout :

   ```shell
   docker rm -f -v recallio-restore-check
   rm recallio.dump keycloak.dump cle-sauvegardes-recallio.txt
   ```

### Les comptes supprimés

Une sauvegarde contient les comptes du jour où elle a été faite. Deux conséquences (ADR 0056, 0073) :

- **un compte supprimé reste dans les sauvegardes** jusqu'à leur suppression, 32 jours au plus. Personne ne les lit
  entre-temps : elles sont chiffrées, et ne servent qu'à une restauration ;
- **une restauration fait revenir** les comptes supprimés depuis l'heure de la sauvegarde. Il faut les supprimer de
  nouveau.

Après une restauration :

1. compte les suppressions faites dans l'application depuis l'heure de la sauvegarde, si le serveur a gardé ses logs
   (le backend les note sans dire de qui) :

   ```shell
   docker compose logs --no-log-prefix --since 2026-11-24T02:15:00Z backend | grep -c 'Un compte a été supprimé'
   ```

   Avec l'heure UTC de la sauvegarde, celle du nom de ses fichiers. `0` : il n'y a rien à rejouer ;
2. sinon, ou si les logs sont perdus avec le serveur : écris aux inscrits que le service a été restauré à son état du
   `<date>`, et que celui qui a supprimé son compte depuis doit le supprimer de nouveau (profil, « Mes données ») ou te
   l'écrire ;
3. traite de nouveau les demandes de suppression reçues par e-mail depuis cette date. Garde donc la date de chaque
   demande reçue par e-mail pendant 32 jours, le temps que la dernière sauvegarde qui contient le compte disparaisse.

### Dépannage

| Symptôme | Cause probable | Que faire |
|---|---|---|
| `systemctl start` échoue aussitôt : « Failed to load environment files » | `/etc/recallio-backup/backup.env` manque | « Configurer » |
| Le journal dit « la clé publique age du destinataire (age1...) manque » ou « malformed recipient » | `BACKUP_AGE_RECIPIENT` est vide ou mal collée | `sudo nano /etc/recallio-backup/backup.env` : la ligne `age1…` entière, 62 caractères |
| `service "postgres" is not running`, puis « ÉCHEC » | PostgreSQL est arrêté | `docker compose ps`, puis `docker compose up -d` |
| « required variable … is missing a value » | Le `.env` du compose est incomplet | Section 5 |
| `SignatureDoesNotMatch`, `InvalidAccessKeyId` ou `403` à l'envoi | Le `keyID`, l'`applicationKey`, la région, ou une clé supprimée | `sudo nano /etc/recallio-backup/rclone.conf` ; la clé existe-t-elle encore dans la console ? |
| `403` juste après « sauvegardée et chiffrée », alors que la clé et la région sont bonnes | La clé n'a pas `writeFiles` ou `listFiles`, ou n'est pas celle de ce bucket | Regarde ses droits dans la console ; refais-la avec ses quatre droits |
| « ATTENTION : le battement de cœur n'a pas pu être envoyé » | L'adresse du battement, ou le service de surveillance | La sauvegarde est faite. Vérifie `BACKUP_HEARTBEAT_URL` |
| Healthchecks alerte, et `systemctl is-failed` répond `inactive` | Le serveur était éteint, ou le timer n'est pas activé | `systemctl list-timers recallio-backup.timer`, puis « Chaque nuit » |
| `pg_restore` : `role "recallio" does not exist` | La base n'a pas été initialisée par son script (un volume créé sans lui) | Sur un serveur neuf seulement : `docker compose down -v`, puis `docker compose up -d postgres`, et restaure de nouveau |
| `age: error: no identity matched any of the recipients` | Ce n'est pas la clé privée de la clé publique du serveur | Compare la clé publique du fichier de la clé avec `BACKUP_AGE_RECIPIENT` |
| La clé privée est perdue | | Les sauvegardes existantes sont illisibles. Génère une nouvelle paire tout de suite (« La paire de clés qui chiffre »), remplace `BACKUP_AGE_RECIPIENT`, et lance une sauvegarde |
| La clé du serveur a fui, ou le serveur a été pris | | Dans la console de Backblaze, supprime la clé et crée-en une autre (« La clé du serveur ») : les sauvegardes déjà faites sont verrouillées et illisibles sans la clé privée |

## 13. La mise en service (étape 7.7)

Les sections 2 à 12 disent **comment** faire chaque chose ; celle-ci dit **dans quel ordre**, ce qui se prépare à
l'avance, et ce qui doit être vrai avant d'inviter quelqu'un. Coche au fur et à mesure.

### L'ordre

Ce qui attend quelqu'un d'autre que toi se lance en premier : la propagation du DNS (jusqu'à quelques heures) et
l'authentification du domaine par Brevo (jusqu'à 48 heures).

**Jour 1 : ce qui a un délai**

- [ ] Vérifier que « Recallio » n'est pas une marque déposée dans ta classe (INPI, EUIPO), puis acheter le domaine
      (section 2).
- [ ] Commander les deux serveurs chez OVHcloud, dans un centre de données de l'Union européenne, avec ta clé SSH : le
      VPS-2 de l'application (section 2) et le VPS-1 du runner (section 11). Ton compte OVHcloud est à ton nom, avec
      ton adresse et ton téléphone à jour.
- [ ] Créer les trois enregistrements `A`, sans `AAAA` (section 2).
- [ ] Ouvrir le compte Brevo et lancer l'authentification du domaine (section 2).
- [ ] Ouvrir le compte Backblaze B2 en région **EU Central** (section 2), et Sentry en région UE si tu le veux.
- [ ] Ouvrir un gestionnaire de mots de passe si tu n'en as pas : il recevra le `.env`, la clé partagée de WireGuard
      et la clé privée des sauvegardes.

**Jour 2 : le serveur de l'application**

- [ ] Préparer le serveur : utilisateur, SSH par clé, pare-feu, mises à jour, Docker, UTC (section 3).
- [ ] Récupérer la configuration et remplir le `.env`, dont `LEGAL_CONTACT_EMAIL` et le tag à déployer (sections 4
      et 5). Le tag est le numéro du dernier commit de `main` dont la CI est verte.
- [ ] Copier le `.env` dans le gestionnaire de mots de passe.
- [ ] Premier démarrage : les cinq services `healthy` (section 6).
- [ ] Keycloak : l'administrateur permanent avec OTP, ton compte avec le rôle `admin`, l'e-mail de test (section 7).
      L'e-mail attend que Brevo ait authentifié le domaine.
- [ ] Toutes les vérifications de la section 8, dont une inscription complète avec une autre adresse que la tienne.

**Jour 3 : le runner et les sauvegardes**

- [ ] Le serveur du runner : pare-feu, core dumps, WireGuard sur les deux serveurs, Docker **après** WireGuard, la
      configuration et le démarrage (section 11).
- [ ] Ses vérifications (section 11, « Vérifier »), puis dans l'application : exécuter une page du Tour de Go et de
      Java, et rendre un exercice de code.
- [ ] Les sauvegardes : le bucket, la clé du serveur, la paire de clés qui chiffre, la première sauvegarde à la main,
      le timer de chaque nuit, le battement de cœur (section 12).
- [ ] La clé privée des sauvegardes est dans le gestionnaire de mots de passe, et **sur aucun serveur**.
- [ ] **L'essai de restauration**, le lendemain de la première sauvegarde de nuit (section 12). Note sa date : le
      prochain est dans trois mois.

**Avant le premier testeur**

- [ ] Relire les deux pages légales, avec la liste en fin de section.
- [ ] Brancher la surveillance de la disponibilité (ci-dessous).
- [ ] Faire toi-même, sur ton téléphone, le parcours d'un nouveau venu : l'essai sans compte, l'inscription, le premier
      login, une session de révision, une page du Tour, l'installation de la PWA, un rappel accepté.
- [ ] Ouvrir `/admin/beta`, `/admin/retention` et `/admin/reports` avec ton compte : les trois s'affichent.

### Surveiller la disponibilité

La sauvegarde a son battement de cœur (section 12) ; le site, lui, se surveille de l'extérieur. Un service de
surveillance (UptimeRobot ou un équivalent, l'offre gratuite suffit) interroge une adresse toutes les cinq minutes et
t'écrit quand elle ne répond plus. Il ne reçoit aucune donnée d'un utilisateur : seulement l'adresse à interroger, et
la tienne pour l'alerte. Tant qu'il ne fait que cela, il n'a pas à figurer dans la politique de confidentialité.

Deux vérifications suffisent :

| Adresse | Ce qu'elle prouve | Attendu |
|---|---|---|
| `https://<domaine>/api/v1/health` | Caddy, le front, le backend et sa base répondent | code 200, et le texte `UP` |
| `https://auth.<domaine>/realms/recallio/.well-known/openid-configuration` | Keycloak répond : on peut se connecter | code 200 |

- Règle l'alerte après **deux échecs de suite** : une mise à jour rend l'API indisponible une vingtaine de secondes
  (section 9), et ce n'est pas une panne.
- Si le service sait prévenir d'un certificat qui expire, active-le : c'est le premier signe d'un Caddy qui n'arrive
  plus à le renouveler.
- **Le runner n'est pas vu par ces vérifications** : il n'est pas joignable depuis Internet. S'il tombe, le reste du
  site marche, et seuls les exercices et « Exécuter » échouent. Sa santé se lit par le tunnel (section 11,
  « Vérifier ») ; pendant la bêta, exécute une page du Tour de temps en temps, et regarde Sentry s'il est activé.

Quand une alerte arrive : `docker compose ps` sur le serveur de l'application, puis les logs du service qui n'est pas
`healthy` (section 10).

### Les premiers testeurs

- **Qui** : quelques personnes que tu connais, cinq à dix pour commencer. L'inscription est ouverte à qui a l'adresse :
  il n'y a pas de liste d'invités. Le site est public et les moteurs de recherche peuvent le trouver (ADR 0065) ; ne
  le partage simplement pas ailleurs tant que la bêta n'est pas ouverte à tous.
- **Quoi leur dire** : c'est une bêta, le contenu a été rédigé avec une IA et contient des erreurs, et le bouton
  « Signaler » de chaque carte et de chaque page du Tour est fait pour cela. Donne-leur ton adresse de contact.
- **Ce qu'ils peuvent utiliser** : Java et Go sont publiés. Les autres sujets apparaissent grisés, « work in
  progress », jusqu'à leur relecture et leur publication (une pull request de contenu chacune).
- **Les séances de test** : le protocole, les neuf tâches et le questionnaire sont dans
  [`tests-utilisateurs.md`](tests-utilisateurs.md).

Ce que tu regardes, et quand :

| Quand | Où | Quoi |
|---|---|---|
| Chaque jour | Ta boîte aux lettres | Les alertes de disponibilité et de sauvegarde |
| Chaque jour | `/admin/reports` | Les signalements : chacun est une erreur de contenu à corriger par une pull request |
| Chaque semaine | `/admin/beta` | Qui révise, les cartes les plus ratées (souvent une question mal posée), où l'on en est dans le Tour, les fonctionnalités que personne n'ouvre (ADR 0076) |
| Chaque semaine | `/admin/retention` | Qui revient après 1, 7 et 30 jours, et où l'on perd les visiteurs |
| Chaque semaine | Sentry, s'il est activé | Les erreurs nouvelles |
| Chaque semaine | Le serveur | L'espace disque (section 10) |

La phase 1 se termine quand quelques testeurs révisent chaque jour sur mobile et que tu sais combien reviennent après
sept jours : c'est ce que dit `/admin/retention`.

### Avant l'ouverture à tous

Rien de ceci ne bloque la bêta ; tout attend ses retours :

- faire relire les pages légales par un professionnel (ci-dessous) ;
- essayer gVisor sur le serveur du runner (section 11, « Essayer gVisor ») ;
- durcir DMARC (`p=quarantine`) une fois que les e-mails passent (section 2) ;
- relire et publier les sujets en brouillon ;
- décider si l'inscription reste ouverte sans limite, au vu de la charge du runner (deux exécutions à la fois, ADR
  0072).

### À relire avant la mise en service : les pages légales

Les deux pages disent ce que le code fait (ADR 0074, qui donne la preuve de chaque affirmation). Elles ne sont pas
validées en droit : relis-les toi-même avant d'ouvrir la bêta, et fais-les relire par un professionnel avant
l'ouverture à tous. Ce que toi seul peux vérifier :

- **Ton statut** : les mentions légales disent que tu édites le site à titre non professionnel. Sans `LEGAL_PUBLISHER`,
  elles disent aussi que tu as communiqué ton identité à l'hébergeur : ton compte OVHcloud doit être à ton nom, avec
  ton adresse et ton téléphone à jour.
- **L'adresse de contact** : elle reçoit vraiment les messages, tu la lis, et tu réponds sous un mois à une demande sur
  des données. Garde la date de chaque demande de suppression 32 jours (section 12, « Les comptes supprimés »).
- **L'hébergeur** : OVH SAS, 2 rue Kellermann, 59100 Roubaix. Compare avec les mentions légales de ton contrat
  OVHcloud. La politique ne dit pas dans quel pays sont les serveurs : choisis un centre de données de l'Union
  européenne.
- **Les prestataires** de la politique sont ceux que tu as vraiment ouverts : Backblaze en région UE (section 12),
  Brevo, et Sentry en région UE s'il est activé. Un prestataire de plus (un service de surveillance qui recevrait
  autre chose qu'un battement de cœur, un outil de mesure) s'ajoute à la page **avant** d'être branché.
- **Sentry**, s'il est activé : « Prevent Storing of IP Addresses » est coché dans chaque projet (ADR 0070). La
  politique dit que Recallio lui demande de ne pas garder l'adresse IP.
- **Keycloak** : les durées de session du royaume sont celles de la politique (30 minutes et 10 heures ; 30 et 90 jours
  avec « Se souvenir de moi »), et l'inscription demande bien un nom d'utilisateur, une adresse e-mail, un prénom et un
  nom. Si tu changes l'un de ces réglages dans la console, change la page.
- **Les sauvegardes** : la règle de cycle de vie du bucket supprime bien un fichier 31 à 32 jours après son envoi
  (section 12). La politique annonce 30 jours, 32 au plus.
- **La propriété intellectuelle** : les mentions légales constatent que le dépôt n'a pas de licence. Si tu en ajoutes
  une, corrige la phrase.
- **L'âge** : la politique dit que Recallio ne s'adresse pas aux moins de 15 ans, sans le vérifier.
- **La date** en tête de chaque page (`UPDATED_ON`, dans `frontend/src/legal/`) est celle de ta dernière relecture.
