# Déployer et exploiter Recallio

> Pour le propriétaire du projet, qui monte et fait vivre les serveurs de la bêta. Chaque étape donne les commandes à
> copier et ce qu'on doit voir. Les décisions sont dans les ADR [0069](adr/0069-hebergement-et-livraison.md) (deux
> serveurs, des images publiées par la CI), [0070](adr/0070-logs-et-erreurs.md) (logs et Sentry) et
> [0071](adr/0071-le-serveur-de-l-application.md) (le serveur de l'application).
>
> État : le serveur de l'application (étape 7.3). Le serveur du runner (7.4), les sauvegardes (7.5) et la mise en
> service (7.7) compléteront ce document, à leurs sections annoncées en fin de page.

Conventions :

- `<domaine>` est le domaine du site (par exemple `recallio.fr`), `<ip>` l'adresse IPv4 du serveur de l'application,
  `recallio` l'utilisateur que l'on crée sur le serveur (section 3) ;
- une commande précédée de « Sur ton poste » se tape sur ta machine, dans Git Bash sous Windows (dans PowerShell,
  `curl` n'est pas le vrai curl, et `grep` manque), sauf mention de PowerShell ; les autres, sur le serveur, connecté
  en SSH ;
- toutes les commandes `docker compose` se lancent depuis `/opt/recallio/infra/production/app`.

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
    runner["Runner<br/>(VPS-1, étape 7.4)"]
    user -- "https://&lt;domaine&gt;" --> caddy
    user -- "https://auth.&lt;domaine&gt;" --> caddy
    caddy --> front --> back
    caddy --> kc
    back --> db
    kc --> db
    back -. WireGuard .-> runner
```

| Service | Ce qu'il fait | Joignable depuis Internet |
|---|---|---|
| Caddy | HTTPS (certificats Let's Encrypt), HSTS ; relaie `<domaine>` vers le front et `auth.<domaine>` vers Keycloak | oui : 80 et 443 |
| Front | nginx : la PWA, et `/api` relayé vers le backend | non : par Caddy |
| Backend | L'API ; importe le contenu à chaque démarrage | non : par le front |
| Keycloak | Les comptes, la connexion, les e-mails (Brevo) ; sa console d'administration | ses pages de connexion, par Caddy ; sa console, par un tunnel SSH seulement |
| PostgreSQL | Deux bases : `recallio` et `keycloak` | non |

**Ce qui est secret** : tout est dans un seul fichier, `/opt/recallio/infra/production/app/.env`, lisible par toi seul
(`chmod 600`), jamais versionné. Il porte les mots de passe de PostgreSQL, celui du compte Keycloak temporaire, la clé
SMTP de Brevo, le jeton du runner et les clés VAPID. **Garde-en une copie hors du serveur** (un gestionnaire de mots de
passe) : sans lui, une restauration ne peut pas se reconnecter à la base, et de nouvelles clés VAPID désabonnent tout le
monde des rappels. Le reste du dossier vient du dépôt public et ne contient aucun secret.

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

Attendu : `.env.example  Caddyfile  compose.yaml  keycloak  postgres`.

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

Le jeton du runner devra être le même dans le `.env` du serveur du runner (étape 7.4).

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
- `RECALLIO_RUNNER_URL` : l'adresse WireGuard du runner (étape 7.4). D'ici là, la valeur d'exemple suffit : rendre un
  exercice répond 503, le reste fonctionne ;
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

1. Sur `https://<domaine>`, *Se connecter*, puis *Enregistrement* : inscris-toi comme n'importe qui, suis le lien de
   l'e-mail, choisis ton mot de passe. Tu reviens connecté dans la PWA.
2. Dans la console, royaume `recallio`, *Users*, ton compte, *Role mapping*, *Assign role*, *Filter by realm roles*,
   `admin`, *Assign*.
3. Dans la PWA, déconnecte-toi et reconnecte-toi : ton nouveau jeton porte le rôle `admin` (les sujets en brouillon et
   les pages des admins apparaissent).

### Tester l'envoi d'un e-mail

Dans la console, royaume `recallio`, *Realm settings*, onglet *Email*, *Test connection* : Keycloak envoie un e-mail de
test à l'adresse de ton administrateur `master`. Attendu : un message de réussite, et l'e-mail reçu (regarde aussi les
indésirables). Les réglages SMTP du `.env` n'ont servi qu'à l'import : désormais, ils se changent sur cet onglet.

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

```shell
cd /opt/recallio/infra/production/app
git pull                                      # seulement si infra/production/app a changé
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
- Note le tag précédent (en commentaire dans le `.env`, par exemple) : c'est le chemin du retour.

**Revenir en arrière**, c'est remettre le tag précédent, puis `docker compose pull` et `up -d`. Les migrations Flyway ne
se défont pas : l'ancien code tourne sur le nouveau schéma, ce que la règle des migrations rétrocompatibles permet
(CLAUDE.md, section 7).

### Caddy, Keycloak et PostgreSQL

Leurs images sont épinglées à une version mineure (`caddy:2.11`, `keycloak:26.8`, `postgres:18`) :

- **les correctifs** de ces versions arrivent avec `docker compose pull` puis `up -d`. Fais-le une fois par mois, avec
  la mise à jour de Docker (section 3) ;
- **une nouvelle version mineure** arrive par une pull request de Dependabot (`caddy:2.12`, `keycloak:26.9`…) : après le
  merge, `git pull`, `docker compose pull`, `up -d`. Pour **Keycloak**, lis d'abord ses notes de mise à jour
  (*Upgrading Guide*) : il migre sa base au démarrage, sans retour possible autrement que par une sauvegarde
  (section 12) ; et reporte la version dans le compose local (`infra/compose.yaml`) ;
- **une version majeure de PostgreSQL** (18 vers 19) demande une vraie migration : la nouvelle version ne lit pas les
  fichiers de l'ancienne. Dependabot ne la propose pas. Le principe : une sauvegarde complète (`pg_dumpall`), puis la
  nouvelle image sur un volume neuf, puis la restauration, en gardant l'ancien volume jusqu'à ce que tout soit vérifié.
  La procédure exacte s'écrira et se répétera sur une copie avant le jour venu, avec celle des sauvegardes (section 12).

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
| La connexion échoue : « Invalid parameter: redirect_uri » | Le domaine a changé après l'import du royaume | Dans la console, royaume `recallio`, *Clients*, `recallio-frontend` : corriger les adresses |
| La console, par le tunnel, reste blanche ou redirige vers `https://auth.<domaine>/realms/master/…` | L'adresse du royaume `master` n'est pas réglée | La commande `kcadm.sh` de la section 7 |
| Plus aucun administrateur Keycloak (OTP perdu) | | Le guide de Keycloak « Bootstrapping and recovering an admin account » (commande `bootstrap-admin`) |
| Une erreur « Failed to create OidcProviderClientImpl » dans Sentry, après un redémarrage du serveur | Le backend a démarré avant Keycloak | Rien : il se connecte à la première requête |

## 11. Le serveur du runner (étape 7.4, à venir)

## 12. Les sauvegardes (étape 7.5, à venir)

## 13. La mise en service (étape 7.7, à venir)
