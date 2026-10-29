# Recallio

Application web (PWA) de révision des entretiens techniques par répétition espacée.

## Prérequis

- JDK 25 (Temurin)
- Node.js 22+ et npm
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

## Documentation

- Cahier des charges : [CLAUDE.md](CLAUDE.md)
- Décisions d'architecture : [docs/adr/](docs/adr/)
