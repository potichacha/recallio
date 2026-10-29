# Recallio

[![CI](https://github.com/potichacha/recallio/actions/workflows/ci.yml/badge.svg)](https://github.com/potichacha/recallio/actions/workflows/ci.yml)

Application web (PWA) de révision des entretiens techniques par répétition espacée.

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

La page d'accueil affiche « API : UP » quand le backend répond.

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
puis construction et scan (Trivy) des deux images Docker. Dependabot propose chaque semaine les
mises à jour de dépendances.

## Documentation

- Cahier des charges : [CLAUDE.md](CLAUDE.md)
- Décisions d'architecture : [docs/adr/](docs/adr/)
