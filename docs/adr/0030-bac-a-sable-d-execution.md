# 0030. Bac à sable d'exécution : un runner maison à conteneurs jetables

- Statut : accepté
- Date : 2026-11-02
- Tranche la décision ouverte n° 10 de CLAUDE.md (section 10).

## Contexte

Les exercices de code (ADR 0029) sont jugés par leurs tests. Pour cela, il faut exécuter du code écrit par les joueurs,
donc du code **non fiable** : il peut boucler sans fin, épuiser la mémoire, lancer des processus en rafale, ouvrir des
connexions, lire ou écrire des fichiers. C'est la première fois que Recallio exécute du code qui ne vient pas de son
dépôt. La CI doit aussi vérifier chaque exercice du contenu, dans le même environnement que celui où il sera joué.

## Options envisagées

| Option | Pour | Contre |
|---|---|---|
| Judge0 auto-hébergé | Éprouvé, multi-langages | Lourd (PostgreSQL, Redis, workers), conteneur privilégié, failles d'évasion publiées en 2024, multi-fichiers par archive zip |
| Piston auto-hébergé | Plus léger, multi-fichiers | Conteneur privilégié, pas de JUnit : il faudrait un lanceur de tests à nous |
| Service tiers (Judge0 via API) | Rien à héberger | Code des joueurs envoyé hors d'Europe, coût à l'appel, dépendance externe |
| **Runner maison à conteneurs jetables** (retenu) | Maîtrisé de bout en bout, même image en CI et en production, isolation renforçable (gVisor) | Le durcissement est notre responsabilité ; demande un hôte qui accepte Docker |

## Décision

**Deux pièces, dans le monorepo :**
- **`sandbox/java/`, l'image d'exécution.** Elle contient le JDK 25, le lanceur JUnit autonome (6.1.3, vérifié par son
  empreinte SHA-256) et un script, `run.sh`. Le script lit sur son entrée standard une archive tar (`src/` et `tests/`),
  compile, lance les tests, puis écrit sur la sortie standard un marqueur suivi des erreurs de compilation ou du rapport
  XML de JUnit. Le code exécuté écrit dans `/dev/null` : il ne peut pas se mêler à ce protocole.
- **`runner/`, un petit service Quarkus séparé du backend.** Il expose `POST /api/v1/runs` (code et tests en entrée,
  résultat de chaque test en sortie) et lance un conteneur neuf par exécution avec la ligne de commande `docker`.
  - **Pourquoi un service séparé :** le socket Docker donne les droits root sur la machine, et seul le runner y a accès,
    jamais le backend. Le runner est joint par le seul backend, sur le réseau interne, avec un jeton partagé
    (`Authorization: Bearer`, 32 caractères au moins, comparé en temps constant).
  - **Pourquoi Quarkus :** la même pile, les mêmes contrôles de qualité, la santé (`/q/health`), la validation des
    entrées. La seule dépendance ajoutée est `commons-compress`, pour écrire l'archive tar.

**L'isolation, à chaque exécution** (`DockerSandbox.command`) :
- `--network none` : aucun réseau ;
- `--read-only`, avec deux `tmpfs` (`/work` et `/tmp`), bornés en taille : rien d'autre n'est inscriptible ;
- `--memory 512m --memory-swap 512m`, `--cpus 1`, `--pids-limit 128`, `--ulimit nofile=1024` ;
- `--cap-drop ALL`, `--security-opt no-new-privileges`, `--user 65534:65534` (nobody) ;
- un délai de 20 secondes, compilation comprise, au-delà duquel le conteneur est tué (`docker kill`) ;
- une sortie tronquée à 1 Mio, et des messages d'échec tronqués à 2 000 caractères ;
- un rapport XML lu par un parseur qui refuse toute déclaration de type de document (pas de XXE) ;
- un runtime plus isolant que runc, gVisor (`runsc`), activable par configuration (`RECALLIO_SANDBOX_DOCKER_RUNTIME`)
  là où il est installé.

**Les limites de charge :** 4 exécutions simultanées au plus (configurable). Au-delà, le runner répond 503 plutôt que
d'épuiser la machine. La limite par utilisateur viendra avec la correction en révision, côté backend.

**En CI**, le job `runner` construit l'image et vérifie l'isolation : pas de réseau, pas d'écriture hors des `tmpfs`,
utilisateur sans droits, boucle infinie tuée, mémoire bornée. Il joue aussi **chaque exercice du contenu** :
- la solution passe tous ses tests ;
- le code de départ compile et en échoue au moins un.

## Conséquences

- **Le code des joueurs ne touche jamais le backend ni sa base.** Une évasion du conteneur reste le risque principal ;
  gVisor le réduit fortement, et l'hôte du runner ne doit porter rien d'autre de sensible.
- **L'hébergement** (décision ouverte n° 6) devra accepter Docker pour le runner : un VPS, ou un PaaS pour le reste et
  un VPS pour le runner.
- **La triche reste possible et assumée** (CLAUDE.md, section 2.4) : le code du joueur s'exécute dans la même JVM que
  ses tests, et pourrait fausser le rapport. En révision personnelle, cela ne nuit qu'au tricheur. Le mode classé
  (phase 3 ter), s'il accepte un jour les exercices, devra isoler les tests du code jugé.
- **Un autre langage** demandera sa propre image (`sandbox/<langage>/`) et son protocole, par un nouvel ADR.
- **Une exécution coûte environ 2 secondes**, surtout le démarrage de la JVM. C'est acceptable pour une soumission. Un
  pool de conteneurs préchauffés pourra venir si le besoin apparaît.
