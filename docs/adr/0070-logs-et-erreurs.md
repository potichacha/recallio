# 0070. Les logs et les erreurs : des logs JSON sans donnée personnelle, et Sentry allumé par son seul DSN

- Statut : accepté
- Date : 2026-11-23
- Prolonge : ADR 0030 (le runner), ADR 0054 (la Content-Security-Policy et la limite de débit), ADR 0069 (les balises
  d'`index.html` que nginx remplit au démarrage)
- Origine : le déploiement (phase 1, étape 7), son étape 2, « logs et erreurs » ; audit du 2026-11-10
  (`docs/audit-2026-11.md`), élément 33

## Contexte

Avant la bêta, il faut savoir ce qui se passe en production sans être devant la console d'un serveur :

- les logs du backend et du runner sont du texte, lisible par une personne, difficile à filtrer ; ceux de nginx portent
  l'adresse IP, le `User-Agent`, le `Referer` et la chaîne de requête de chaque visiteur ;
- une erreur du backend ou du runner ne se voit que dans les logs, et une erreur de la PWA nulle part : `PageError`
  l'écrit dans la console du navigateur de l'utilisateur, et rien n'est envoyé ailleurs.

Une règle gouverne tout : **aucune donnée personnelle ni secret ne quitte le serveur, et les logs n'en contiennent pas**
(CLAUDE.md, section 4.3 ; RGPD). Sont en jeu :

- l'adresse IP des visiteurs ;
- le jeton d'un lien d'invitation, dans l'adresse de la PWA (`/join/<jeton>`) et dans celle de l'API
  (`/api/v1/invitations/<jeton>`) : qui le connaît rejoint le groupe ;
- l'adresse d'un profil public (`/u/<adresse>`, `/api/v1/public-profiles/<adresse>`), souvent le nom de la personne ;
- le code et l'état du retour OIDC (`/auth/callback?code=…&state=…`), et toute chaîne de requête ;
- les valeurs que PostgreSQL recopie dans ses messages d'erreur (« Key (handle)=(alice) already exists »).

Les UUID des ressources (parties, duels, défis) ne sont pas en cause : ils ne disent rien de personne.

Une seconde contrainte vient de la CI : le 2026-11-19, Trivy l'a bloquée sur une faille critique de l'image de base Java
(CVE-2026-114938), déjà corrigée dans les dépôts de Red Hat, mais pas encore dans l'image
`ubi9/openjdk-25-runtime:1.24`. Le front, lui, appliquait déjà les correctifs d'Alpine (`apk upgrade`) ; mais avec le
cache de la CI, cette couche était réutilisée telle quelle d'une construction à l'autre.

## Options envisagées

**Où voir les erreurs** :

| Option | Pour | Contre |
|---|---|---|
| Les logs seulement | Rien à ajouter | Il faut les lire pour savoir qu'une erreur a eu lieu ; celles de la PWA restent invisibles |
| Sentry auto-hébergé | Les données chez nous | Des dizaines de conteneurs (Kafka, ClickHouse...) : bien trop lourd pour le VPS de l'application |
| GlitchTip auto-hébergé | Compatible avec les SDK de Sentry, plus léger | Un service de plus à administrer, mettre à jour et sauvegarder, pour une seule personne |
| **Sentry hébergé, en région UE** (retenu) | Rien à héberger, des alertes, les erreurs du front avec leur pile lisible | Les erreurs quittent le serveur : d'où leur nettoyage, ci-dessous |

**Comment l'allumer** :

- **Une variable `SENTRY_ENABLED` en plus du DSN** : deux variables à tenir accordées, et l'extension Quarkus refuse de
  démarrer si elle est allumée sans DSN.
- **Le DSN seul** (retenu) : renseigné, Sentry est allumé ; vide ou absent (en local, en CI, dans le compose), il est
  éteint et tout démarre normalement.

**Dans la PWA** :

- **`@sentry/react`** : son `ErrorBoundary` et ses intégrations du routeur doublent les `ErrorBoundary` que les routes
  ont déjà (`PageError`).
- **`@sentry/browser`, chargé avec l'application** : quelque 30 Ko de plus à l'ouverture, pour tout le monde, Sentry
  allumé ou non.
- **`@sentry/browser`, chargé à la demande** (retenu) : un `import()` dynamique, seulement si un DSN est donné, hors du
  budget de l'ouverture (ADR 0050).

**Les bloqueurs de publicité**, qui bloquent souvent les adresses de Sentry :

- **Un tunnel par nginx** (`/api/…` relayé vers Sentry) : les erreurs passeraient, mais c'est un relais vers un hôte
  extérieur à configurer et à borner (taille, débit), et un DSN à analyser dans nginx. Écarté pour la bêta, à revoir si
  trop d'erreurs du front se perdent.
- **Accepter la perte** (retenu) : les erreurs des navigateurs qui bloquent Sentry ne lui arrivent pas.

**Les source maps de la PWA** (pour lire la pile d'une erreur du front) :

- **Envoyées à Sentry par la CI** (`sentry-cli`) : la méthode la plus sûre, mais un jeton d'API de Sentry dans les
  secrets du dépôt, et une étape de plus à la publication.
- **Servies avec les fichiers** (retenu) : Sentry suit le `sourceMappingURL` de chaque fichier et télécharge la source
  map du site. Le dépôt est public : les servir ne révèle rien.

**Les correctifs de sécurité des images** :

- **Attendre une nouvelle image de base** : la CI reste bloquée, ou une faille connue part en production.
- **Mettre à jour les paquets à la construction** (retenu), sans cache pour cette étape.

## Décision

### 1. Les logs, en JSON et sans donnée personnelle

- **Le backend et le runner** : l'extension `quarkus-logging-json`, en JSON hors dev et test
  (`%dev,test.quarkus.log.console.json.enabled=false`) : en dev et en test, la console reste lisible ; le compose local,
  en profil `prod`, a des logs JSON. Chaque ligne porte un champ `service` (`backend`, `runner`).
- **nginx** : le journal d'accès en JSON (`log_format recallio_json`, `escape=json`) : l'heure, le service (`frontend`),
  la méthode, le chemin, le statut, les octets envoyés, la durée et celle du backend. **Sans adresse IP, sans
  `User-Agent` ni `Referer`, sans chaîne de requête** ; le chemin part de `$request_uri` (`$uri` devient `/index.html`
  après `try_files`), et des `map` masquent le jeton et l'adresse de profil : `/join/:token`,
  `/api/v1/invitations/:token`, `/u/:handle`, `/api/v1/public-profiles/:handle`.
- **Les refus de la limite de débit** (ADR 0054) ne s'écrivent plus dans le journal d'erreurs de nginx, qui recopie
  l'adresse IP et la requête : `limit_req_log_level info`, sous le niveau de ce journal dans l'image (`notice`). Les 429
  restent dans le journal d'accès, sans adresse IP.
- **PostgreSQL** : en production, la propriété `logServerErrorDetail=false` du pilote JDBC
  (`quarkus.datasource.jdbc.additional-jdbc-properties.logServerErrorDetail`) : ses exceptions et ses logs ne recopient
  plus les valeurs d'une erreur du serveur, seulement un message minimal.
- **Les logs existants**, relus : aucun ne porte de donnée personnelle (des identifiants techniques, des noms de
  fichier, des statuts). Le runner écrit en `WARN` la sortie d'erreur d'un programme qui échoue de façon inattendue
  (`DockerSandbox`) : elle reste dans les logs du serveur, et ne part jamais chez Sentry (point 2).
- **Les adresses masquées dans les logs du backend aussi** : le gestionnaire d'erreurs de Quarkus, qui n'est pas à nous,
  écrit l'adresse complète d'une requête qui échoue (une erreur 500), chaîne de requête comprise. Un filtre de logs,
  `SensitivePathsLogFilter` (`@LoggingFilter(name = "sensitive-paths")`, module `platform`), branché sur la console dans
  tous les profils (`quarkus.log.console.filter=sensitive-paths`), passe le message formaté de chaque ligne par
  `SensitivePaths`, la règle de Sentry (point 2). Il ne trie rien : il laisse passer toutes les lignes, mais remplace le
  message d'une ligne qui change par le texte masqué, sans paramètres à reformater ; c'est le seul point d'accroche
  avant la sortie. Une ligne sans rien à masquer reste intacte, et l'exception d'une ligne garde sa pile (son message,
  lui, n'est pas réécrit). Ainsi, « HTTP Request to /api/v1/invitations/:token/accept failed, error id: … » sort masqué
  dans les logs comme chez Sentry.

### 2. Sentry pour le backend et le runner

- **L'extension Quarkiverse `quarkus-logging-sentry`, en version 2.3.6** : la 4.0.0 suit Quarkus 4 ; la branche 2.3 est
  la dernière pour Quarkus 3. Dependabot ignore ses versions `>= 3`, pour le backend et le runner. Elle envoie à Sentry
  ce qui passe par les logs : aucun appel à Sentry dans le code.
- **Allumée par le seul DSN** : `%prod.quarkus.log.sentry.dsn=${SENTRY_DSN:}` ; une `ConfigSourceFactory` de quelques
  lignes, `SentryActivation`, allume l'interrupteur de l'extension (`quarkus.log.sentry.enabled`) quand un DSN est donné.
  Sa source passe après `application.properties` et les variables d'environnement : `QUARKUS_LOG_SENTRY_ENABLED=false`
  éteint Sentry malgré un DSN. Jamais en dev ni en test, où le DSN n'est pas lu.
- **Les événements** : les logs à partir d'`ERROR` ; les fils d'Ariane (les logs qui précèdent une erreur) à partir de
  `WARN` dans le backend, d'`ERROR` dans le runner, dont la sortie d'erreur des programmes (`WARN`) ne part donc jamais ;
  `in-app-packages` `com.recallio` ; **pas de traces de performance**.
- **Le backend nettoie chaque événement** avant l'envoi : `SentryEventScrubber`, un `BeforeSendCallback` (bean
  `@ApplicationScoped` et `@Unremovable`, que l'extension appelle) du module `platform`. Dans le message, ses paramètres,
  les messages des exceptions et ceux des fils d'Ariane, `SensitivePaths` masque le jeton après `/invitations/` et
  l'adresse après `/public-profiles/`, et retire toute chaîne de requête. Le gestionnaire d'erreurs de Quarkus écrit par
  exemple « HTTP Request to /api/v1/invitations/:token/accept failed, error id: … ».
- **Le runner** n'a ni jeton ni donnée personnelle dans ses adresses : pas de nettoyage. Ce que ses logs recopient d'un
  programme de joueur (sa sortie d'erreur, en `WARN`) reste sur son serveur.

### 3. Sentry pour la PWA, chargé à la demande

- **La configuration arrive au démarrage du conteneur, comme l'adresse du fournisseur OIDC** (ADR 0069) : trois balises
  d'`index.html` (`recallio-sentry-dsn`, `recallio-sentry-environment`, `recallio-sentry-release`), que nginx remplit
  (`sub_filter`, dans la seule `location` qui sert les fichiers) depuis les variables `SENTRY_DSN`,
  `SENTRY_ENVIRONMENT` et `SENTRY_RELEASE` du conteneur. `src/servedConfig.ts` lit ces balises, pour l'OIDC comme pour
  Sentry.
- **Rien n'est téléchargé ni envoyé sans DSN** : `src/monitoring/` démarre depuis `main.tsx` ; DSN vide ou marqueur
  intact (`npm run dev`), il ne fait rien. Sinon, il télécharge `@sentry/browser` par un `import()` dynamique, dans un
  morceau à part, ni compté dans le budget de l'ouverture ni mis en cache par le service worker. Les erreurs signalées
  avant la fin du téléchargement attendent, puis partent ; les erreurs globales d'avant peuvent être perdues : c'est
  accepté.
- **`PageError`** signale l'erreur d'une page une fois (`useEffect`), et non plus à chaque rendu.
- **Sans donnée personnelle** :
  - ni utilisateur, ni adresse IP déduite par Sentry : la version 11 du SDK a remplacé `sendDefaultPii` par
    `dataCollection`, et `userInfo: false` y fait dire à Sentry de ne jamais déduire l'adresse (`infer_ip: never`) ; ni
    cookies, ni chaîne de requête, ni corps de requête, ni variables locales ;
  - ni Session Replay, ni traces de performance, ni fils d'Ariane de la console, ni session à chaque visite
    (`BrowserSession`, qui joindrait Sentry sans erreur), ni compte des événements écartés ;
  - un `beforeSend` et un `beforeBreadcrumb` passent les adresses (celle de la page, le `Referer`, celles des fils
    d'Ariane de navigation et de `fetch`) et les messages par une fonction pure, `src/monitoring/scrub.ts` : sans chaîne
    de requête ni fragment, le jeton de `/join/` et `/invitations/` et l'adresse de `/u/` et `/public-profiles/`
    masqués.
- **Les source maps** : `build.sourcemap: true` ; Sentry les télécharge du site, à côté de chaque fichier (« source
  fetching »). Les fichiers ont une empreinte dans leur nom : la source map téléchargée est toujours celle du fichier.
- **La Content-Security-Policy** (ADR 0054) admet une seconde origine tierce dans `connect-src` : `SENTRY_ORIGIN`,
  l'origine du DSN, vide par défaut, à accorder au DSN comme `OIDC_ORIGIN` à `OIDC_AUTHORITY`.

### 4. Les correctifs de sécurité des images, appliqués à chaque construction

- Dans l'étape d'exécution du backend et du runner, en `USER root` :
  `microdnf upgrade -y --nodocs --setopt=install_weak_deps=0 && microdnf clean all` (non interactif, sans
  documentation ni dépendances faibles) ; le backend repasse ensuite en `USER 185`. Le front garde son
  `apk upgrade --no-cache`.
- Cette étape s'appelle `runtime` dans les trois `Dockerfile`, et la CI ne la prend jamais du cache
  (`no-cache-filters: runtime` de `docker/build-push-action`, dans les jobs `docker` et `publish`) : sinon, la couche de
  mise à jour serait réutilisée indéfiniment, et ne prendrait plus aucun correctif. L'étape de construction (Maven, npm)
  reste en cache.

### Ce qui part chez Sentry, et ce qui n'y part jamais

| Service | Ce qui part | Ce qui ne part jamais |
|---|---|---|
| Backend | Les logs `ERROR` (gestionnaire d'erreurs de Quarkus, tâches en échec...), leur pile, et les logs `WARN` et `ERROR` qui les précèdent, nettoyés | Le jeton d'une invitation, l'adresse d'un profil, les chaînes de requête, les valeurs de PostgreSQL, les logs sous `WARN` |
| Runner | Ses logs `ERROR` (docker introuvable, sortie illisible...) | La sortie d'erreur des programmes des joueurs, leur code |
| PWA | Les erreurs que rien n'attrape et celles des pages, avec l'adresse de la page et le `Referer` nettoyés, le `User-Agent`, les fils d'Ariane de navigation, de `fetch` et de clic, nettoyés | L'utilisateur, l'adresse IP, les cookies, les chaînes de requête et les fragments, le jeton d'une invitation, l'adresse d'un profil, la console, les sessions, Session Replay, les traces |

Sentry voit l'adresse IP d'où part un envoi, comme tout serveur : le projet lui interdit de la garder (ci-dessous).

### Les variables

| Variable | Services | Défaut | Rôle |
|---|---|---|---|
| `SENTRY_DSN` | backend, runner, frontend | vide | Le DSN du projet Sentry du service : renseigné, Sentry est allumé ; vide, rien n'est envoyé |
| `SENTRY_ENVIRONMENT` | backend, runner, frontend | `production` | L'environnement des erreurs |
| `SENTRY_RELEASE` | backend, runner, frontend | vide | La version des erreurs : le compose de production y mettra le tag de l'image (étape 7.3) |
| `SENTRY_ORIGIN` | frontend | vide | L'origine du DSN du front (`https://o….ingest.de.sentry.io`), admise par la Content-Security-Policy |

### Les réglages de Sentry, à la mise en service

1. Un compte dans la **région UE** de Sentry : la région se choisit à la création de l'organisation, et ne change plus.
2. **Trois projets**, un DSN chacun : `recallio-backend` et `recallio-runner` (Java), `recallio-frontend` (JavaScript
   pour le navigateur).
3. Dans les réglages de chaque projet (« Security & Privacy ») : **« Prevent Storing of IP Addresses »**, et le
   nettoyage des données côté serveur (« Data Scrubber », « Use Default Scrubbers ») activés.
4. Pour le front, le téléchargement des sources (« Enable JavaScript source fetching ») laissé actif : c'est lui qui lit
   les source maps.
5. **Des alertes par e-mail** : à chaque nouveau problème, et quand un problème revient après avoir été résolu.
6. Les DSN dans les variables des serveurs, avec l'origine du DSN du front dans `SENTRY_ORIGIN`.

## Conséquences

- ➕ Une erreur se voit sans lire les logs, celles de la PWA comprises, avec une pile lisible ; une seule variable par
  service l'allume.
- ➕ Les logs se filtrent par service et par champ ; ni le journal d'accès de nginx ni les lignes du backend ne portent
  de jeton d'invitation, d'adresse de profil ou de chaîne de requête, et nginx n'écrit plus l'adresse IP.
- ➕ Chaque image prend les correctifs de sécurité publiés depuis son image de base, sans attendre une nouvelle image.
- ➖ **Les autres lignes du journal d'erreurs de nginx**, rares (un backend injoignable, une requête malformée...),
  gardent l'adresse IP du client et la requête, et restent du texte : nginx ne permet pas de choisir leur format.
- ➖ Les deux premières lignes du backend et du runner, écrites par le script de lancement de l'image (`run-java.sh`)
  avant la JVM, restent du texte.
- ➖ Les erreurs des navigateurs qui bloquent Sentry sont perdues, comme les erreurs globales d'avant le téléchargement
  de Sentry.
- ➖ Sentry peut ne pas joindre le site, ou tarder, pour lire une source map : une erreur du front peut alors arriver
  avec une pile illisible.
- ➖ **Le masquage est une liste** : une nouvelle adresse qui porterait un secret ou une donnée personnelle se masque en
  trois endroits : `SensitivePaths` (backend, qui sert aux logs comme à Sentry), `src/monitoring/scrub.ts` (PWA) et les
  `map` de `frontend/nginx/default.conf.template`.
- ➖ L'étape `runtime` se reconstruit à chaque passage de la CI : quelques dizaines de secondes par image, et deux
  constructions du même commit peuvent différer par leurs paquets système. C'est le but.
- 🔁 Le tunnel par nginx, si trop d'erreurs du front se perdent ; l'envoi des source maps par la CI, si le
  téléchargement par Sentry ne suffit pas ; `quarkus-logging-sentry` 4 avec Quarkus 4 ; OpenTelemetry plus tard
  (CLAUDE.md, section 3).
