# recallio-backend

Backend Quarkus de Recallio. Voir le [README racine](../README.md) et les [ADR](../docs/adr/).

```shell
./mvnw quarkus:dev   # mode dev : live reload, Dev UI sur http://localhost:8080/q/dev-ui
./mvnw verify        # build complet : tests, ArchUnit, Error Prone/NullAway, Spotless
./mvnw spotless:apply  # reformate le code (palantir-java-format)
```
