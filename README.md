# Recallio

[![CI](https://github.com/potichacha/recallio/actions/workflows/ci.yml/badge.svg)](https://github.com/potichacha/recallio/actions/workflows/ci.yml)

Application web (PWA) de révision des entretiens techniques par répétition espacée.

## Ce que fait l'application

- **Réviser** un catalogue de sujets (Java, Go, frameworks, cloud, données, fondamentaux) en cartes de neuf formats,
  planifiées par FSRS : un sujet par session, pour la durée choisie. Les QCM, vrais ou faux, codes à compléter et
  exercices de code sont corrigés par le serveur ; les autres cartes se retournent, et l'on dit si l'on savait.
- **Apprendre** un langage dans le Tour, page par page, en exécutant chaque exemple dans un bac à sable ; « Programmer
  de zéro » pour qui n'a jamais programmé.
- **Se préparer** : la maîtrise notion par notion, l'indice de préparation, l'examen blanc chronométré.
- **Garder l'habitude** : XP par sujet, série et gels de série, quêtes, succès, rappels par notification.
- **Jouer à plusieurs** : groupes, classement de la semaine, défis, mode live, et le classé par sujet (rangs, duels,
  saisons).
- **Partager** : un profil public de compétences, privé par défaut, et des cartes de partage.

## Structure du dépôt

| Dossier | Contenu |
|---|---|
| [`backend/`](backend/) | API Quarkus (Java 25), monolithe modulaire |
| [`frontend/`](frontend/) | PWA React + TypeScript + Vite |
| [`runner/`](runner/) | Service Quarkus qui exécute le code des exercices dans des conteneurs jetables (ADR 0030) |
| [`sandbox/java/`](sandbox/java/), [`sandbox/go/`](sandbox/go/) | Images du bac à sable, une par langage (JDK 25 et JUnit ; Go 1.27), lancées par le runner |
| [`content/`](content/) | Contenu : documents Markdown (`source/`) et dossiers de sujet (`java/` : référentiel, notions, exercices) |
| [`infra/`](infra/) | `compose.yaml` et royaume Keycloak pour lancer l'ensemble avec Docker |
| [`e2e/`](e2e/) | Parcours de bout en bout (Playwright) contre la pile Docker |
| [`docs/`](docs/) | Architecture, ADR, rétrospectives |

## Prérequis

- JDK 25 (Temurin)
- Node.js 24 LTS et npm (voir `frontend/.nvmrc`)
- Docker (les Dev Services de Quarkus lancent PostgreSQL et Keycloak)

## Lancer en local

Backend (http://localhost:8080) :

```shell
cd backend
./mvnw quarkus:dev
```

Le premier démarrage télécharge les images PostgreSQL et Keycloak (Dev Services) : compter
une à deux minutes. Docker Desktop doit être lancé.

Vérification : `curl http://localhost:8080/api/v1/health`

Frontend (http://localhost:5173), dans un second terminal :

```shell
cd frontend
npm install
npm run dev
```

La page d'accueil affiche « API : UP » quand le backend répond ; une fois connecté, elle devient le tableau de
bord (cartes dues, série, progression par thème) et le menu « Réviser » lance une session.

Exercices de code : ils s'exécutent dans le runner (ADR 0030), à lancer dans un troisième terminal si l'on veut les
essayer en dev (sans lui, les essayer ou les rendre répond 503) :

```shell
docker build -t recallio-sandbox-java sandbox/java   # les images du bac à sable, une fois
docker build -t recallio-sandbox-go sandbox/go
cd runner
./mvnw quarkus:dev
```

Comptes de test (royaume Keycloak `recallio`, en dev comme dans le compose) : `alice` / `alice` (rôle `admin`),
`bob` / `bob` et `carol` / `carol`. En dev, Keycloak tourne sur http://localhost:8180 (console d'administration : `admin` / `admin`).

## Lancer avec Docker (comme en production)

Construit les images et lance PostgreSQL, Keycloak, le runner (et l'image du bac à sable), le backend et le front :

```shell
docker compose -f infra/compose.yaml up --build
```

Front : http://localhost:3000 · Keycloak : http://localhost:8180 (admin / admin, utilisateurs
`alice` / `alice`, `bob` / `bob` et `carol` / `carol`).

## Intégration continue

Chaque pull request lance [`.github/workflows/ci.yml`](.github/workflows/ci.yml) : build et tests
du backend (avec vérification que la spec OpenAPI est à jour), lint, tests et build du front,
tests du runner (isolation du bac à sable, et chaque exercice de code joué : la solution passe, le code de départ
échoue), puis construction et scan (Trivy) des deux images Docker, et enfin les parcours de bout en bout Playwright
contre la pile complète (voir [`e2e/README.md`](e2e/README.md)). Dependabot propose chaque semaine les
mises à jour de dépendances.

## Contribuer

- Une branche courte par étape, créée depuis `main` à jour ; pull request obligatoire.
- Messages en [Conventional Commits](https://www.conventionalcommits.org/fr/) (`feat:`, `fix:`, `docs:`...).
- Merge en *squash* une fois les contrôles de la CI verts (`backend`, `frontend`, `docker`, `e2e`).
- Avant de pousser : `./mvnw spotless:apply` (backend) et `npm run format` (frontend).

## Dépannage

| Symptôme | Cause et solution |
|---|---|
| `Port 8080 seems to be in use` | Une autre application occupe le port : l'arrêter, ou lancer `./mvnw quarkus:dev -Dquarkus.http.port=8090` puis le front avec la variable `BACKEND_URL=http://localhost:8090`. |
| Dev Services ne démarre pas | Docker Desktop n'est pas lancé. |
| `Port 8180` déjà utilisé au démarrage du backend | Le Keycloak du compose tourne encore : `docker compose -f infra/compose.yaml down`. |
| Connexion refusée par l'API (401) avec un jeton valide | Le jeton doit avoir l'audience `recallio-api` : vérifier le mapper du client dans le royaume. |
| `npm test` plante (`undici`, `zlib`) ou avertissements `EBADENGINE` | Version de Node non LTS : installer Node 24 (`frontend/.nvmrc`). |
| `docker compose up --wait` : conteneur `unhealthy` | Voir `docker compose -p recallio-e2e logs <service>` ; les ports 3000 et 8180 doivent être libres. |
| La CI échoue sur `openapi.yaml` | L'API a changé : lancer `./mvnw package` dans `backend/` et committer la spec. |

## Documentation

- Cahier des charges : [CLAUDE.md](CLAUDE.md)
- Architecture : [docs/architecture.md](docs/architecture.md)
- Décisions d'architecture : [docs/adr/](docs/adr/)
- Rétrospectives : [docs/retro/](docs/retro/)
