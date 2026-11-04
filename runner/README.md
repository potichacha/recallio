# Runner

Le service qui exécute le code des exercices (ADR 0030, 0033) : il reçoit du code et des tests, lance un conteneur
jetable de l'image de leur langage ([`sandbox/java`](../sandbox/java/) ou [`sandbox/go`](../sandbox/go/)), et renvoie
le résultat de chaque test. Seul le backend l'appelle.

## Lancer et tester

Docker est nécessaire : les tests construisent les images du bac à sable (la première fois, quelques minutes pour Go,
qui précompile sa bibliothèque standard ; le cache rend cela instantané ensuite), puis lancent de vrais conteneurs.

```bash
cd runner
./mvnw verify                        # isolation du bac à sable, API, et chaque exercice du contenu
./mvnw test -Dtest=ExercisesTest     # seulement les exercices de content/*/exercices
./mvnw quarkus:dev                   # http://localhost:8090, jeton de dev dans application.properties
```

## API

`POST /api/v1/runs`, avec `Authorization: Bearer <jeton>` :

```json
{
  "language": "JAVA",
  "code":  [{ "path": "Doublons.java", "content": "..." }],
  "tests": [{ "path": "DoublonsTest.java", "content": "..." }]
}
```

`language` vaut `JAVA` ou `GO`. Les fichiers Go sont à plat (`panier.go`), et seuls les tests s'appellent
`*_test.go` ; des fichiers qui ne sont pas du langage annoncé donnent une réponse 400.

La réponse donne `status` (`PASSED`, `FAILED`, `COMPILATION_ERROR`, `TIMEOUT`, `ERROR`), le résultat de chaque test
(`className`, `name`, `passed`, `message`) et `output` (erreurs de compilation). Si les exécutions simultanées sont
toutes prises, la réponse est 503. La santé est sur `/q/health`, sans jeton.

## Configuration

| Variable | Défaut | Rôle |
|---|---|---|
| `RECALLIO_RUNNER_TOKEN` | aucun en production | Le jeton que présente le backend, 32 caractères au moins (`openssl rand -hex 32`) |
| `RECALLIO_SANDBOX_JAVA_IMAGE` | `recallio-sandbox-java` | L'image du bac à sable Java |
| `RECALLIO_SANDBOX_GO_IMAGE` | `recallio-sandbox-go` | L'image du bac à sable Go |
| `RECALLIO_SANDBOX_TIMEOUT` | `20s` | Le délai d'une exécution, compilation comprise |
| `RECALLIO_SANDBOX_MAX_CONCURRENT_RUNS` | `4` | Les exécutions simultanées |
| `RECALLIO_SANDBOX_DOCKER_RUNTIME` | runc | `runsc` pour gVisor, s'il est installé sur l'hôte |

Le runner a besoin de la ligne de commande `docker` et du socket Docker, ce qui équivaut à des droits root sur l'hôte.
C'est pour cela qu'il est séparé du backend, et que son hôte ne doit porter rien d'autre de sensible.
