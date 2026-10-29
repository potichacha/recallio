# Recallio

[![CI](https://github.com/potichacha/recallio/actions/workflows/ci.yml/badge.svg)](https://github.com/potichacha/recallio/actions/workflows/ci.yml)

Application web (PWA) de révision des entretiens techniques par répétition espacée.

## Structure du dépôt

| Dossier | Contenu |
|---|---|
| [`backend/`](backend/) | API Quarkus (Java 25), monolithe modulaire |
| [`frontend/`](frontend/) | PWA React + TypeScript + Vite |
| [`content/source/`](content/source/) | Contenu source des cartes (Markdown) |
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

Comptes de test (royaume Keycloak `recallio`, en dev comme dans le compose) : `alice` / `alice` (rôle `admin`)
et `bob` / `bob`. En dev, Keycloak tourne sur http://localhost:8180 (console d'administration : `admin` / `admin`).

## Lancer avec Docker (comme en production)

Construit les images et lance PostgreSQL, Keycloak, le backend et le front :

```shell
docker compose -f infra/compose.yaml up --build
```

Front : http://localhost:3000 · Keycloak : http://localhost:8180 (admin / admin, utilisateurs
`alice` / `alice` et `bob` / `bob`).

## Intégration continue

Chaque pull request lance [`.github/workflows/ci.yml`](.github/workflows/ci.yml) : build et tests
du backend (avec vérification que la spec OpenAPI est à jour), lint, tests et build du front,
puis construction et scan (Trivy) des deux images Docker, et enfin les parcours de bout en bout Playwright
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
