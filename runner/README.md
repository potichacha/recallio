# Runner

Le service qui exécute du code dans un conteneur jetable de l'image de son langage ([`sandbox/java`](../sandbox/java/)
ou [`sandbox/go`](../sandbox/go/)). Il juge un exercice par ses tests et renvoie le résultat de chacun (ADR 0030,
0033), ou lance un programme du Tour et renvoie ce qu'il a écrit (ADR 0037). Seul le backend l'appelle.

## Lancer et tester

Docker est nécessaire : les tests construisent les images du bac à sable (la première fois, quelques minutes pour Go,
qui précompile sa bibliothèque standard ; le cache rend cela instantané ensuite), puis lancent de vrais conteneurs.

```bash
cd runner
./mvnw verify                        # isolation du bac à sable, API, programmes, et chaque exercice du contenu
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

`POST /api/v1/programs`, avec le même jeton, lance un programme sans tests (ADR 0037) :

```json
{
  "language": "JAVA",
  "files": [{ "path": "Main.java", "content": "void main() { IO.println(\"Bonjour\"); }" }]
}
```

Un programme Java a un `Main.java` à la racine (les fichiers source compacts de Java 25 sont permis) ; un programme Go,
un `package main`. Il est compilé, puis lancé sans entrée standard. La réponse donne `status` (`COMPLETED`,
`COMPILATION_ERROR`, `TIMEOUT`, `ERROR`), `exitCode`, `stdout` et `stderr` (64 Kio au plus chacune, `truncated` dit si
l'une a été coupée) et `message` (erreurs de compilation, ou ce qui a arrêté le programme). Les soumissions et les
programmes partagent les exécutions simultanées.

## Configuration

| Variable | Défaut | Rôle |
|---|---|---|
| `RECALLIO_RUNNER_TOKEN` | aucun en production | Le jeton que présente le backend, 32 caractères au moins (`openssl rand -hex 32`) |
| `RECALLIO_SANDBOX_JAVA_IMAGE` | `recallio-sandbox-java` | L'image du bac à sable Java |
| `RECALLIO_SANDBOX_GO_IMAGE` | `recallio-sandbox-go` | L'image du bac à sable Go |
| `RECALLIO_SANDBOX_TIMEOUT` | `20s` | Le délai d'une exécution, compilation comprise |
| `RECALLIO_SANDBOX_PROGRAM_TIMEOUT` | `10s` | Le délai d'un programme une fois compilé, en secondes entières, plus court que le précédent |
| `RECALLIO_SANDBOX_MAX_CONCURRENT_RUNS` | `4` | Les exécutions simultanées, soumissions et programmes confondus |
| `RECALLIO_SANDBOX_DOCKER_RUNTIME` | runc | `runsc` pour gVisor, s'il est installé sur l'hôte |

Le runner a besoin de la ligne de commande `docker` et du socket Docker, ce qui équivaut à des droits root sur l'hôte.
C'est pour cela qu'il est séparé du backend, et que son hôte ne doit porter rien d'autre de sensible.
