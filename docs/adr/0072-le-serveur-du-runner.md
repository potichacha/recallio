# 0072. Le serveur du runner : un VPS seul, joint par WireGuard, sans core dumps

- Statut : accepté
- Date : 2026-11-23
- Prolonge : ADR 0069 (deux VPS reliés par WireGuard, les images publiées par la CI), ADR 0071 (le serveur de
  l'application, dont ce serveur reprend les conventions), ADR 0030 (le bac à sable, gVisor), ADR 0053 (les core dumps),
  ADR 0070 (les logs et Sentry)
- Origine : le déploiement (phase 1, étape 7), son étape 4, « le serveur du runner » ; audit du 2026-11-10
  (`docs/audit-2026-11.md`), élément 33

## Contexte

L'ADR 0069 a placé le runner sur son propre serveur, un VPS-1 d'OVHcloud (2 vCores, 4 Go, Debian 13 ou Ubuntu 24.04),
parce que son accès au socket Docker vaut les droits root sur l'hôte (ADR 0030). Le backend le joint par un tunnel
WireGuard, avec le jeton partagé : le `.env.example` de l'application a déjà
`RECALLIO_RUNNER_URL=http://10.13.13.2:8090`. Il manquait tout ce qui fait tourner ce serveur : son compose, le tunnel,
le durcissement de l'hôte et la procédure.

Les contraintes :

- **le port du runner n'est jamais sur Internet**, et Docker contourne le pare-feu `ufw` pour les ports qu'il publie
  (ADR 0071) ;
- **un programme qui plante ne laisse rien sur l'hôte** (ADR 0053). Quand `kernel.core_pattern` commence par `|`
  (apport sur Ubuntu, systemd-coredump), le noyau confie l'image mémoire du programme à un programme de l'hôte, lancé en
  root, quelle que soit la limite du conteneur. Constaté sous Docker Desktop, dont le motif est
  `|/wsl-capture-crash %t %E %p %s` : chaque programme C++ qui plante dans le bac à sable, avec une limite douce de 0
  dans le conteneur, a laissé un fichier de 556 Ko sur le poste Windows ;
- **le serveur ne construit rien** : les images du runner et du bac à sable sont celles de la CI, au tag `sha-…` du
  backend (ADR 0069) ;
- **une seule personne exploite** : les conventions du serveur de l'application (ADR 0071), des commandes à copier.

## Options envisagées

**Le lien entre le backend et le runner** :

| Option | Pour | Contre |
|---|---|---|
| Un port public, en TLS, avec le jeton | Rien à installer | Le runner sur Internet, un certificat à tenir, une faille avant le jeton ouverte à tous |
| Un tunnel SSH permanent (`autossh`) | SSH est déjà là | Un service de plus à maintenir en vie, du TCP dans du TCP, une clé SSH du runner sur l'application |
| Un réseau privé géré (Tailscale) | Rien à configurer à la main | Un compte et un serveur de coordination tiers pour deux machines |
| **WireGuard** (retenu, ADR 0069) | Dans le noyau, sans service à surveiller ; un seul port UDP, ouvert à la seule adresse de l'application | Des clés à générer et à recopier une fois |

**La clé partagée (`PresharedKey`)** : WireGuard la dit facultative (« Optional, and may be omitted ») et la décrit
comme une couche de cryptographie symétrique ajoutée à la cryptographie à clé publique, « for post-quantum resistance »
(`wg(8)`).

- **Sans** : un secret de moins à recopier.
- **Avec** (retenu) : un secret tiré une fois, recopié comme le jeton du runner. Le trafic enregistré aujourd'hui (le
  jeton, le code des joueurs) ne se déchiffrera pas avec un futur ordinateur quantique, et la clé privée volée d'un
  serveur ne suffit plus à se faire passer pour lui. Elle ne coûte rien à l'exécution.

**Où publier le port du runner** :

- **Sur toutes les adresses** (`8090:8090`) : sur Internet, quoi que dise `ufw`. Exclu.
- **Le runner sur le réseau de l'hôte**, Quarkus écoutant sur l'adresse du tunnel, filtré par `ufw` : le pare-feu filtre
  de nouveau, mais c'est une configuration à part, loin du compose local et de celui de l'application (une adresse
  d'écoute à donner à Quarkus, une règle `ufw` par interface), et le runner partage la pile réseau de l'hôte.
- **Sur l'adresse WireGuard du serveur seulement** (retenu) : `10.13.13.2:8090:8090`, comme la console de Keycloak sur
  `127.0.0.1` (ADR 0071).

**L'ordre entre Docker et WireGuard au démarrage du serveur**. Docker ne peut lier un port qu'à une adresse qui existe ;
`10.13.13.2` n'existe qu'une fois `wg0` monté. Essayé sur un serveur simulé (Docker 29.10 dans un conteneur, son propre
réseau) :

- **Sans rien** : Docker démarre le conteneur, la liaison échoue (« failed to bind host port 10.13.13.2:8090/tcp: cannot
  assign requested address »), le conteneur reste `exited`, et **Docker ne réessaie jamais**, même une fois `wg0` monté
  (attendu 45 secondes). C'est documenté : une politique de redémarrage ne s'applique qu'à un conteneur qui a démarré.
  Après un redémarrage du serveur, le runner ne repartirait pas.
- **`net.ipv4.ip_nonlocal_bind=1`** : le conteneur démarre sans `wg0`, et répond une fois le tunnel monté (essayé).
  Mais le réglage vaut pour tous les programmes de l'hôte (n'importe lequel peut écouter sur une adresse qu'il n'a pas),
  et il cache un tunnel absent : le runner serait `healthy` et injoignable.
- **Un « drop-in » systemd de `docker.service`** (retenu) : `After=` et `Wants=wg-quick@wg0.service`. Docker ne démarre
  qu'une fois `wg-quick` terminé, c'est-à-dire l'adresse posée, que le pair réponde ou non (`wg-quick@.service` est un
  service `oneshot`). `Wants` démarre WireGuard avec Docker même si on a oublié de l'activer ; ce n'est pas `Requires` :
  si WireGuard échoue, Docker démarre quand même, et le runner reste `exited` avec un message explicite. Essayé : Docker
  démarré après `wg0`, le conteneur démarre et répond par le tunnel.

**Un filtrage en plus dans la chaîne `DOCKER-USER`**. L'adresse de liaison tient le port hors d'Internet : un paquet
pour `10.13.13.2` n'y est pas routé. Mais une machine du même segment réseau que le serveur peut l'atteindre en routant
`10.13.13.2` vers l'adresse publique du serveur : le noyau accepte un paquet pour l'une de ses adresses sur n'importe
quelle interface, la traduction d'adresse de Docker ne regarde pas l'interface d'arrivée, et `ufw` ne voit rien. Docker
le documente (« the published port can be accessed via other host interfaces using direct routing to the host
address », page *Port publishing*, *Gateway modes*), et c'est vérifié : sur le serveur simulé, `ufw` actif, une machine
voisine atteint le port par cette route, et pas par l'adresse publique.

- **Une règle `DOCKER-USER`** (`! -i wg0`, rejet) fermerait ce chemin, mais elle doit survivre aux rechargements
  d'`ufw` et aux redémarrages de Docker : une chaîne déclarée dans `/etc/ufw/after.rules`, une configuration de plus
  entre deux pare-feux, pour un attaquant qui devrait encore présenter le jeton.
- **L'adresse de liaison et le jeton** (retenu) : l'adresse écarte Internet, ses robots et ses balayages ; le jeton
  (64 caractères hexadécimaux, 256 bits, comparé en temps constant, ADR 0030) arrête le reste. Seule la santé répond
  sans lui, et ne dit que `UP` ou `DOWN` ; rien d'autre sous `/q` ne répond en production (essayé : 404). À revoir si
  le runner gagne une route sans jeton.

**Les core dumps** :

- **`ulimit -c 0` dans le conteneur** : ignoré quand le motif est un pipe (`man 5 core` : « RLIMIT_CORE will be ignored
  if the system is configured to pipe core dumps to a program »). Constaté : limite douce de 0, et pourtant
  « (core dumped) » et un fichier sur l'hôte.
- **`kernel.core_pattern=|/bin/false`** : rien n'est écrit, mais le noyau lance quand même un programme de l'hôte, en
  root, à chaque plantage, et ignore toujours la limite.
- **Un motif sans pipe, une limite de 0 pour tous les conteneurs, et apport masqué** (retenu), ci-dessous.

**gVisor** (`runsc`, ADR 0030), un noyau en espace utilisateur entre le code des joueurs et celui de l'hôte :

- **Activé dès la bêta** : le risque d'évasion baisse fortement. Mais la compilation et le démarrage des JVM
  ralentissent, alors qu'un exercice Quarkus prend déjà 13 à 20 secondes sur un poste de développement, pour un délai
  de 30 ; et gVisor ne s'essaie pas sous Docker Desktop : on ignore combien d'exercices dépasseraient le délai.
- **Prêt à essayer, pas activé pour la bêta** (retenu) : le runner accepte déjà `RECALLIO_SANDBOX_DOCKER_RUNTIME=runsc`,
  et la procédure dit comment l'installer, mesurer chaque langage et revenir en arrière.

## Décision

### 1. Les fichiers

| Fichier | Rôle |
|---|---|
| `infra/production/runner/compose.yaml` | Le runner et les quatre images du bac à sable ; le projet s'appelle `recallio-runner` |
| `infra/production/runner/.env.example` | Toutes les variables, commentées ; copié en `.env` (`chmod 600`), jamais versionné |
| `infra/production/runner/wireguard/wg0.conf.example` | Le tunnel, côté runner : il écoute |
| `infra/production/app/wireguard/wg0.conf.example` | Le tunnel, côté application : il appelle |

La procédure est la section 11 de `docs/deploiement.md`.

### 2. Le compose du runner

- **Les images** : `ghcr.io/potichacha/recallio-runner` et les quatre `recallio-sandbox-{java,go,cpp,python}`, **au même
  tag**, `${RECALLIO_TAG}`, obligatoire. Le nom de chaque image du bac à sable est écrit une fois (une ancre YAML), puis
  repris par son service et par la variable qui la donne au runner (`RECALLIO_SANDBOX_*_IMAGE`, ADR 0069) : le runner
  lance exactement les images que `docker compose pull` a tirées.
- **Un service par image du bac à sable**, comme dans le compose local : `docker compose pull` la tire et `up -d` l'a
  avant le runner, qui en dépend (`service_completed_successfully`). Chacun lance `true` (`entrypoint`), sans réseau
  (`network_mode: none`) ni redémarrage : `true` dit ce qu'il fait, sans dépendre de la réaction du script du bac à
  sable à une entrée vide. Vérifié : `true` existe dans les quatre images et sort avec 0 ; un second `up -d` relance ces
  services sans toucher au runner.
- **`docker image prune -a` ne les supprime pas** : leurs conteneurs arrêtés les référencent. Vérifié, comme le fait que
  `docker container prune` et `docker system prune` suppriment ces conteneurs, après quoi `prune -a` supprimerait les
  images : la procédure l'interdit. Sans son image, le runner la tirerait à la première exécution, et dépasserait son
  délai, compté au joueur comme un échec.
- **Les variables du runner** : `RECALLIO_RUNNER_TOKEN` (obligatoire, le même que dans le `.env` de l'application),
  `RECALLIO_SANDBOX_MAX_CONCURRENT_RUNS=2` (chaque exécution a `--cpus 1`, le serveur a deux vCores),
  `RECALLIO_SANDBOX_DOCKER_RUNTIME` (vide, runc ; vérifié : une valeur vide est acceptée), `SENTRY_DSN` (le DSN du
  projet `recallio-runner`, depuis `SENTRY_DSN_RUNNER`, facultatif) et `SENTRY_RELEASE=${RECALLIO_TAG}`.
- **Le socket Docker est monté**, comme en local.
- **Le seul port publié** : `${WIREGUARD_ADDRESS:-10.13.13.2}:8090:8090`. La variable n'existe que pour la vérification
  locale, sur `127.0.0.1`.
- **Comme sur le serveur de l'application** : `restart: unless-stopped`, un healthcheck sur le **statut 200** de
  `/q/health/ready`, la même rotation des logs (`x-logging`, pilote `local`, 5 fichiers de 10 Mo).
- **La mémoire du runner est plafonnée à 512 Mo** (`mem_limit`) : sans limite, le tas de sa JVM monterait à 80 % des
  4 Go de l'hôte (`run-java.sh`). La limite compte aussi les commandes `docker` que le runner lance, une par exécution. Mesuré le
  2026-11-23 sur la pile de vérification, avec les images de la CI :

  | Mesure | Valeur |
  |---|---|
  | Tas maximal, à 512 Mo | 410 Mo (80 %) |
  | Le runner au repos | 190 Mo (dont 66 Mo de cache de fichiers) |
  | Pendant deux exécutions Quarkus simultanées | 210 Mo, dont 27 à 28 Mo par commande `docker` |
  | Le plus haut du conteneur, toutes vérifications faites | 214 Mo |
  | Chaque conteneur du bac à sable, exercice Quarkus | 320 Mo au plus, pour sa limite de 512 Mo (ADR 0030) |

  512 Mo laissent deux fois et demie le plus haut mesuré.

### 3. Quatre gigaoctets suffisent

| Poste | Au plus |
|---|---|
| Le système (Debian ou Ubuntu minimal, SSH, journald, mises à jour), estimé | 300 à 400 Mo |
| Docker et containerd, et un processus de suivi de 10 Mo par conteneur (mesuré : 100 Mo pour `dockerd`), estimé | 200 Mo |
| Le runner | 512 Mo (sa limite) |
| Deux exécutions, `tmpfs` compris | 2 × 512 Mo (leur limite) |
| **Total** | **2,1 Go**, sur 4 |

Le reste sert de cache aux fichiers des images (le JDK, la chaîne Go, GCC), que chaque exécution relit : c'est ce qui
garde les exécutions rapides.

### 4. WireGuard

- **Le runner écoute** : `Address 10.13.13.2/24`, `ListenPort 51820`, un seul pair, l'application
  (`AllowedIPs 10.13.13.1/32` : les seuls paquets acceptés de ce pair viennent de cette adresse).
- **L'application appelle** : `Address 10.13.13.1/24`, `Endpoint <ip du runner>:51820`, `AllowedIPs 10.13.13.2/32`
  (seul le trafic vers le runner passe par le tunnel) et `PersistentKeepalive 25`.
- **Les clés** : `wg genkey` et `wg pubkey` sur chaque serveur, dans `/etc/wireguard/` (`umask 077`) ; une clé privée ne
  quitte jamais son serveur. La clé partagée, `wg genpsk`, est tirée sur le runner et recopiée sur l'application. Les
  modèles ne contiennent que des marqueurs (`<RUNNER_PRIVATE_KEY>`…), remplacés par `sed` depuis ces fichiers ;
  `wg0.conf` est en `chmod 600`.
- **Le serveur de l'application n'ouvre aucun port pour WireGuard** : il appelle, et `ufw` suit les connexions ; les
  réponses du runner reviennent par l'entrée ouverte à l'aller, que `PersistentKeepalive` garde ouverte (« for the
  purpose of keeping a stateful firewall or NAT mapping valid persistently », `wg(8)`). Vérifié avec `ufw` actif,
  entrées refusées par défaut, sur les deux serveurs simulés.
- **Un conteneur de l'application passe par `wg0`** malgré la politique `FORWARD` d'`ufw` (`DROP`) : Docker place ses
  chaînes (`DOCKER-USER`, `DOCKER-FORWARD`) avant celles d'`ufw`, accepte le trafic sortant de ses réseaux, et le masque
  derrière l'adresse de l'interface de sortie (« outgoing packets from containers are masqueraded », *Port publishing*).
  Le runner voit donc `10.13.13.1`, la seule adresse que WireGuard accepte de ce pair. Vérifié : depuis un conteneur
  du réseau par défaut et depuis un réseau défini par l'utilisateur, comme `internal`, le serveur voit `10.13.13.1`.
- Vérifié de bout en bout avec les deux modèles remplis de clés tirées pour l'essai : `wg-quick strip` les lit,
  `wg show` montre la poignée de main, le ping passe dans les deux sens, et le vrai runner répond par le tunnel (santé,
  un programme, 401 sans jeton).

### 5. Le pare-feu du runner

`ufw` : `22/tcp`, et `51820/udp` depuis la seule adresse IPv4 du serveur de l'application ; rien d'autre. Le port du
runner n'y figure pas : Docker le publie lui-même, sur l'adresse du tunnel.

### 6. Docker après WireGuard

`/etc/systemd/system/docker.service.d/10-after-wireguard.conf` : `After=wg-quick@wg0.service` et
`Wants=wg-quick@wg0.service`. Redémarrer WireGuard seul ne demande pas de redémarrer Docker : essayé, le conteneur reste
en marche et le port répond de nouveau après la poignée de main suivante (17 secondes).

### 7. Les core dumps désactivés

- **`/etc/sysctl.d/90-recallio-core-dumps.conf`** : `kernel.core_pattern=core` et `fs.suid_dumpable=0`. Un motif sans
  `|` est un fichier, écrit par le noyau dans le contexte du programme qui plante (« Paths are interpreted according to
  the settings that are active for the crashing process », `man 5 core`) : dans le conteneur, dont seuls les `tmpfs`
  sont inscriptibles, et qui disparaît avec lui. Jamais un programme de l'hôte. `fs.suid_dumpable=0` interdit l'image
  d'un programme qui a changé d'identité.
- **Une limite de 0 pour tous les conteneurs** : `default-ulimits` (`core`, `0:0`) dans `/etc/docker/daemon.json`. Sans
  elle, les conteneurs héritent de containerd, dont le service a `LimitCORE=infinity`, et le noyau écrirait une image
  dans le conteneur. La limite dure ne se relève pas sans capacité (le bac à sable n'en a aucune).
- **apport masqué** sur Ubuntu (`systemctl mask apport.service`) : son service réécrit le motif à chaque démarrage. Le
  désinstaller emporterait le méta-paquet `ubuntu-server`. systemd-coredump n'est installé par défaut ni sur Debian ni
  sur Ubuntu ; s'il l'est, le fichier `90-` passe après son `50-coredump.conf`.
- **Vérifié** sous Docker Desktop, avec les options du bac à sable, en changeant le motif le temps de l'essai (remis à
  sa valeur d'origine, comparée octet par octet) :

  | Motif | Limite dans le conteneur | Ce qu'on voit |
  |---|---|---|
  | `\|/wsl-capture-crash …` (un pipe) | douce 0 | « (core dumped) », et un fichier sur l'hôte |
  | `core` | illimitée (Docker Engine) | « (core dumped) », et un fichier `core` de 304 Ko dans le `/tmp` du conteneur |
  | `core` | `0:0` (`default-ulimits`) | « Segmentation fault » seul, aucun fichier |

  Le runner, lui, répond la même chose dans tous les cas : `exitCode` 139, « Segmentation fault » sur la sortie
  d'erreur. Vérifié aussi sur un serveur simulé : avec `default-ulimits`, un conteneur aux options du bac à sable a une
  limite `0:0` et ne peut pas la relever.

### 8. gVisor, à essayer avant l'ouverture à tous

Il n'est pas activé pour la bêta, dont les testeurs sont connus. On l'active **avant l'ouverture à tous**, quand du code
d'inconnus s'exécutera, si les durées mesurées sous gVisor (section « Essayer gVisor » de la procédure) laissent de la
marge sous le délai de 30 secondes : l'exercice Quarkus le plus long sous les 20 secondes. Sinon, on revoit le délai ou
le bac à sable Java avant d'ouvrir.

### 9. Une mise à jour : le runner d'abord

Les deux serveurs portent le même tag. Le runner se met à jour **avant** l'application : un nouveau backend ne parle
jamais à un ancien runner (une requête qu'il ne connaîtrait pas, un langage qu'il n'aurait pas). L'inverse dure quelques
minutes, un ancien backend face au nouveau runner : l'API du runner n'a fait jusqu'ici qu'ajouter (des langages, les
programmes), et doit continuer ainsi. Pendant le redémarrage du runner (moins de 10 secondes, mesuré), une exécution
répond 503, et la PWA dit de réessayer.

### 10. La CI et Dependabot

- **Le job `production`** vérifie aussi `infra/production/runner/compose.yaml` avec son `.env.example` (`docker compose
  config --quiet`, les valeurs vides remplies d'une valeur factice, comme pour l'application), et que les deux modèles
  WireGuard ne contiennent aucune clé : aucune chaîne qui a la forme d'une clé (44 caractères base64), et un marqueur
  `<…>` sur chaque ligne de clé. Son nom ne change pas ; le motif `^infra/production/` le déclenche déjà.
- **Pas de Dependabot pour ce compose** : il n'a que nos images, au tag d'un commit, qui changent avec lui. Leurs images
  de base sont suivies dans leurs `Dockerfile` (écosystème `docker` de Dependabot ; `sandbox/cpp` n'y figure pas), et les
  correctifs de sécurité du runner entrent à chaque construction (ADR 0070).

## Conséquences

- ➕ Le port du runner n'est que sur le tunnel ; le pare-feu n'ouvre que SSH et WireGuard, pour la seule adresse de
  l'application ; le serveur de l'application n'ouvre rien de plus.
- ➕ Un programme qui plante ne laisse rien, ni sur l'hôte, ni dans le conteneur.
- ➕ Après un redémarrage du serveur, le runner repart seul : Docker attend `wg0`.
- ➕ Un seul `.env`, qui ne porte que le tag, le jeton et le DSN ; les clés WireGuard sont dans `/etc/wireguard`, sur
  chaque serveur.
- ➖ **Une machine du même segment réseau peut atteindre le port** en routant l'adresse du tunnel vers le serveur : le
  jeton est alors la seule barrière, comme il l'était dans le compose local.
- ➖ **Deux configurations WireGuard à tenir accordées** : changer l'adresse publique du runner, c'est changer
  l'`Endpoint` de l'application, et son adresse IPv4 dans `ufw` sur le runner si l'application change d'adresse.
- ➖ **Chaque version apporte ses images du bac à sable** : environ 1 Go à télécharger et 5 Go sur le disque pour une
  première version, moins ensuite (les couches inchangées ne se téléchargent pas) ; `docker image prune -a` libère les
  anciennes, et `docker system prune` est à proscrire.
- ➖ Les durées mesurées ici le sont sur un poste de développement (un processeur par exécution), programme du Tour ou
  exercice, requête comprise : Python 0,3 à 1,3 s, C++ 0,4 à 2,0 s, Go 1,0 à 2,7 s, Java 1,9 à 3,3 s, Spring Boot 5,1
  à 5,6 s, Quarkus 13 à 20 s seul, 21 s quand deux tournent ensemble. Celles du VPS restent à mesurer à la mise en
  service (la procédure le fait) ; un exercice Quarkus y sera le plus près du délai de 30 secondes.
- 🔁 gVisor avant l'ouverture à tous (point 8) ; une règle `DOCKER-USER` si le runner gagne une route sans jeton ;
  `--ulimit core=0` dans la ligne de commande du bac à sable (`DockerSandbox.command`), pour ne plus dépendre du réglage
  de l'hôte.
