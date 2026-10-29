# Recallio

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

## Documentation

- Cahier des charges : [CLAUDE.md](CLAUDE.md)
- Décisions d'architecture : [docs/adr/](docs/adr/)
