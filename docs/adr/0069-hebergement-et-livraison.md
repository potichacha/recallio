# 0069. L'hébergement et la livraison : deux VPS OVHcloud, Keycloak hébergé, des images publiées par la CI

- Statut : accepté
- Date : 2026-11-20
- Tranche les décisions ouvertes n° 5 (fournisseur OIDC de production), 6 (hébergeur) et 7 (nom du projet) de
  CLAUDE.md (section 10), prises par le propriétaire le 2026-11-05.
- Prolonge : ADR 0005 (le nom provisoire), ADR 0009 (l'import du contenu), ADR 0030 (le bac à sable), ADR 0054 (la
  Content-Security-Policy), ADR 0065 (le référencement)
- Origine : le déploiement (phase 1, étape 7), son étape 1, « des images prêtes à déployer »
- Complété par : ADR 0071 (le serveur de l'application : son compose, Caddy, Keycloak en production), ADR 0072 (le
  serveur du runner : son compose, le tunnel WireGuard, le pare-feu, les core dumps)

## Contexte

Recallio est prêt pour sa bêta : quelques testeurs qui révisent chaque jour sur mobile. Il faut décider où il tourne,
qui authentifie ses utilisateurs, et comment une version passe du dépôt aux serveurs.

Les contraintes :

- **le runner a besoin d'un démon Docker** : il lance un conteneur jetable par exécution (ADR 0030). Son accès au
  socket Docker vaut les droits root sur l'hôte, qui ne doit donc rien porter de sensible ;
- **un budget de bêta** : quelques euros par mois ;
- **les données en Europe** : les comptes et la progression des testeurs (RGPD) ;
- **une seule personne pour exploiter** : peu de pièces, et des outils déjà connus (Docker Compose, comme en local) ;
- **ce qui tourne en production est ce que la CI a vérifié** : une image n'est construite que par la CI, jamais sur le
  serveur. Or deux choses empêchaient de déployer les images telles quelles :
  - le compose montait `content/` dans le backend. En production, `POST /api/v1/admin/content-imports` n'importe qu'un
    document Markdown : les dossiers de sujet (ADR 0026), c'est-à-dire Java, Go et tous les sujets récents, ne
    s'importaient qu'au démarrage, depuis un dossier que le serveur n'avait pas ;
  - la PWA lisait l'adresse du fournisseur OIDC **à la construction** (`VITE_OIDC_AUTHORITY`) : l'image contenait
    `http://localhost:8180/realms/recallio`, et il aurait fallu une image du front par environnement.

## Options envisagées

**L'hébergement** :

| Option | Pour | Contre |
|---|---|---|
| Un PaaS (Clever Cloud) | Rien à administrer, PostgreSQL managé | Aucun démon Docker pour le runner : il faudrait un VPS en plus |
| Un seul VPS | Le moins cher, une seule machine | Le socket Docker du runner sur la machine de la base et de Keycloak : une évasion du bac à sable donnerait tout |
| Un Kubernetes managé | Déploiements déclaratifs, mise à l'échelle | Trop lourd pour une bêta, et un démon Docker à trouver quand même pour le runner |
| Hetzner | Longtemps le moins cher | Ses CX et CAX ne sont plus commandables depuis septembre 2026, et ses CPX ont fortement augmenté en juin 2026 |
| **Deux VPS OVHcloud, en Docker Compose** (retenu) | Le runner seul sur sa machine, en Europe, Compose comme en local | Deux machines à administrer : système, pare-feu, tunnel |

**Le fournisseur OIDC** :

- **Un fournisseur managé** (Zitadel Cloud, Clerk, Auth0) : rien à mettre à jour, mais un coût par utilisateur
  au-delà d'un palier gratuit, des comptes hors de notre serveur, et un fournisseur différent de celui du dev, des tests
  et de l'e2e.
- **Keycloak hébergé** (retenu) : c'est déjà le fournisseur du dev, des tests et de l'e2e, avec le même royaume ; les
  comptes restent sur notre serveur européen ; aucun coût par utilisateur. Le prix : ses mises à jour sont à notre
  charge.

**La livraison** :

- **Construire sur le serveur** (`docker compose up --build` dans une copie du dépôt) : rien à publier, mais le serveur
  compile (Maven, npm, les bacs à sable), et ce qui tourne n'est pas exactement ce que la CI a vérifié.
- **Des images publiées par la CI sur GHCR** (retenu) : le dépôt est public, les images peuvent l'être aussi, sans
  coût.
- **Un déploiement automatique** par la CI, en plus de la publication : il demanderait une clé SSH du serveur dans les
  secrets du dépôt. Écarté pour la bêta.

**Le contenu en production** :

- **Monté depuis un dossier du serveur**, comme le faisait le compose : `content/` à copier à chaque version, à côté de
  l'image, et le parseur d'un commit pourrait lire le contenu d'un autre.
- **Importé par l'endpoint d'administration** : il n'importe qu'un document Markdown ; les dossiers de sujet
  demanderaient un nouvel endpoint, qui reçoive des centaines de fichiers.
- **Copié dans l'image du backend, et importé à chaque démarrage** (retenu) : le parseur et le contenu d'un même commit,
  vérifiés ensemble par la CI, partent ensemble.

**L'adresse du fournisseur OIDC dans la PWA** :

- **Une image du front par environnement**, construite avec son `VITE_OIDC_AUTHORITY` : l'image vérifiée par la CI ne
  serait pas celle déployée.
- **Un fichier de configuration lu par la PWA** (`/config.json`) : une requête de plus avant toute connexion, et un
  fichier de plus à tenir hors du cache du service worker.
- **Une balise d'`index.html` remplie par nginx au démarrage du conteneur** (retenu), sur le modèle de `PUBLIC_ORIGIN`
  (ADR 0065).

## Décision

1. **Deux VPS OVHcloud, en Docker Compose, reliés par WireGuard** (décision ouverte n° 6) :
   - **le serveur de l'application** porte Caddy (HTTPS), le front, le backend, Keycloak et PostgreSQL ;
   - **le serveur du runner** ne porte que le runner et les images du bac à sable : son accès au socket Docker vaut les
     droits root sur l'hôte (ADR 0030). Le backend le joint par le tunnel WireGuard, avec le jeton partagé.
   - Le chiffrage, sur la gamme VPS d'OVHcloud (prix « à partir de », hors taxes, relevés en juin 2026, IPv4 et
     sauvegarde quotidienne du VPS comprises) :

     | Serveur | Offre | Ressources | Prix par mois |
     |---|---|---|---|
     | L'application | VPS-2 | 4 vCores, 8 Go | 7,21 € HT |
     | Le runner | VPS-1 | 2 vCores, 4 Go : deux exécutions simultanées | 3,81 € HT |

     Soit environ 13,20 € TTC par mois, plus le domaine (10 à 20 € par an). Le prix sans engagement reste à vérifier
     dans le panier.
2. **Keycloak hébergé** sur le serveur de l'application (décision ouverte n° 5), sous son propre sous-domaine,
   `auth.<domaine>` : ses pages et ses cookies restent hors de l'origine de l'application.
3. **Le nom : Recallio est gardé** (décision ouverte n° 7, ADR 0005), sous réserve des marques déposées (INPI, EUIPO),
   vérifiées avant d'acheter le domaine.
4. **La CI publie les images sur GHCR**, job `publish` de `.github/workflows/ci.yml` :
   - sept images, `ghcr.io/<propriétaire>/recallio-<image>` : `backend`, `frontend`, `runner`, `sandbox-java`,
     `sandbox-go`, `sandbox-cpp` et `sandbox-python`, pour `linux/amd64` seulement (les VPS sont en x86-64) ;
   - seulement sur un push sur `main`, après tous les jobs de vérification : sur `main`, ils tournent tous (job
     `changes`), et une image publiée a donc passé toute la CI. Sur `main`, un run n'est plus annulé par le suivant,
     qui attend son tour, pour ne pas couper une publication en cours. Une image peut quand même échouer seule (le
     registre, le réseau) : le serveur n'épingle que le commit d'un job `publish` entièrement vert ;
   - deux tags : `sha-<12 premiers caractères du commit>`, immuable, celui que le serveur épingle, et `main`, le
     dernier publié. **Revenir en arrière, c'est épingler le tag précédent** ;
   - les labels OCI `org.opencontainers.image.source` (l'adresse du dépôt, qui y relie le paquet) et
     `org.opencontainers.image.revision` (le commit) ;
   - le runner reçoit le nom des images du bac à sable par ses variables (`RECALLIO_SANDBOX_JAVA_IMAGE`...), au même
     tag que lui.
5. **Le déploiement se fait à la main** pour la bêta : changer le tag sur le serveur, tirer les images, relancer. Un
   déploiement automatique demanderait une clé SSH du serveur dans la CI : on le fera si le besoin vient.
6. **Le contenu voyage dans l'image du backend** :
   - `backend/Dockerfile` se construit depuis la racine du dépôt, comme celui du front ;
     `backend/Dockerfile.dockerignore` ne laisse passer que les sources du backend et `content/` ;
   - `content/` est copié dans `/content` **en dernier** : le contenu change plus souvent que le code, et les couches
     de l'application restent en cache ;
   - l'image active l'import au démarrage et donne ses deux dossiers (`RECALLIO_CONTENT_IMPORT_ON_STARTUP`,
     `RECALLIO_CONTENT_SOURCE_DIRECTORY`, `RECALLIO_CONTENT_SUBJECTS_DIRECTORY`). L'import est idempotent (ADR 0009) :
     un démarrage sans changement de contenu n'écrit rien ;
   - le compose local fait de même : plus de volume `content/`, et un changement du contenu demande `--build` ;
   - **l'import au démarrage dure environ 2 secondes** quand le contenu n'a pas changé, et 6 secondes sur une base
     neuve : le backend répond alors 5 secondes après son lancement, 10 sur une base neuve. Mesuré le 2026-11-20 sur la
     pile du compose, sur un poste de développement : 17 documents et 12 dossiers de sujet, quelque 28 000 cartes. C'est
     le temps pendant lequel le backend n'est pas prêt, à chaque déploiement ; un VPS peut être un peu plus lent.
7. **Le front reçoit l'adresse du fournisseur OIDC à son démarrage** : une seule image pour tous les environnements.
   - `index.html` porte `<meta name="recallio-oidc-authority" content="__OIDC_AUTHORITY__">`, que nginx remplit avec la
     variable `OIDC_AUTHORITY` du conteneur (`sub_filter`), comme `__PUBLIC_ORIGIN__` : dans la seule `location` qui
     sert les fichiers, jamais sur les réponses de l'API ;
   - la PWA lit la balise (`src/auth/oidcAuthority.ts`, une fonction pure et testée). Si elle manque ou garde son
     marqueur, parce que `npm run dev` sert `index.html` sans nginx, la PWA prend `VITE_OIDC_AUTHORITY`, puis le
     Keycloak local. Le client (`recallio-frontend`) ne change pas ;
   - `OIDC_ORIGIN`, la seule origine tierce de la Content-Security-Policy (ADR 0054), reste une variable à part : nginx
     ne sait pas la déduire d'une adresse. Les deux sont à accorder ;
   - le service worker met en cache `index.html` tel que nginx l'a servi, donc rempli : il le télécharge du serveur à
     son installation.
8. **L'ouverture** : une bêta pour quelques testeurs d'abord ; l'ouverture à tous attend leurs retours.

Les étapes suivantes du déploiement décideront du reste : les logs et Sentry (7.2), les e-mails de Keycloak par Brevo
(7.3), les sauvegardes hors du serveur (7.5), les pages légales (7.6).

## Conséquences

- ➕ Ce qui tourne en production est ce que la CI a vérifié : les mêmes images, le même contenu, le même parseur.
  Revenir en arrière ne demande aucune construction.
- ➕ Une évasion du bac à sable ne donne que le serveur du runner : ni la base, ni Keycloak, ni les secrets de
  l'application.
- ➕ Keycloak est le même partout : le royaume, les parcours de connexion et leurs tests de bout en bout valent pour la
  production.
- ➖ **Chaque déploiement réimporte le contenu** : le backend n'est pas prêt pendant l'import, quelques secondes,
  acceptable pour une bêta. Si cette durée grandit avec le catalogue, l'import pourra sauter les sujets dont les
  fichiers n'ont pas changé.
- ➖ Un document importé par l'endpoint d'administration est remplacé, au démarrage suivant, par celui de l'image s'il
  porte un sujet qu'elle contient : le contenu de l'image fait foi. Corriger le contenu, c'est une pull request, puis
  un déploiement.
- ➖ Le service worker garde `index.html` tel qu'il l'a reçu jusqu'à l'image suivante : changer `OIDC_AUTHORITY` ou
  `PUBLIC_ORIGIN` sans changer d'image ne touche pas les visiteurs déjà venus. Ces adresses se fixent une fois par
  environnement.
- ➖ Deux serveurs à administrer (système, pare-feu, WireGuard), et les mises à jour de Keycloak à suivre.
- ➖ Les secrets (base, Keycloak, jeton du runner, clés VAPID) sont des variables d'environnement des serveurs, jamais
  dans le dépôt : ils se sauvegardent à part.
- 🔁 Les paquets GHCR doivent être **publics**, pour que les serveurs les tirent sans identifiant : à vérifier après la
  première publication, dans les réglages de chaque paquet.
- 🔁 Un déploiement automatique, quand les déploiements deviendront fréquents ; un hébergement plus grand, quand la
  bêta s'ouvrira à tous.
