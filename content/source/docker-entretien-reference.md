---
subject: docker
title: Docker
category: cloud-devops
position: 100
status: published
---

# Docker : le document de référence pour les entretiens techniques

> Pour préparer un entretien : conteneurs et Docker.
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (les ponts entre symptômes et notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ces chapitres viennent du document Java, dont ils ont été sortis pour devenir un sujet à part entière (ADR 0025).

═══════════════════════════════════════════
# 1. DOCKER
═══════════════════════════════════════════

### Vue d'ensemble

Docker répond à un problème vieux comme le déploiement : *"ça marche sur ma machine"*. Il empaquette l'application **avec tout son environnement d'exécution** (librairies système, dépendances, configuration) dans une image immuable qui se comporte identiquement partout.

Techniquement, un conteneur n'est pas une machine virtuelle : il n'y a pas d'OS invité. C'est un **processus isolé** sur le noyau de l'hôte, grâce à deux mécanismes Linux : les **namespaces** (isolation de la vue : processus, réseau, système de fichiers) et les **cgroups** (limitation des ressources : CPU, mémoire). D'où sa légèreté et son démarrage instantané comparé à une VM.

L'autre apport majeur est l'**immuabilité** : on ne modifie pas un conteneur en production, on en déploie un nouveau. Cela rend les déploiements reproductibles et les rollbacks triviaux.

### Cours

**Image vs conteneur** : l'image est un template en lecture seule, le conteneur une instance en exécution avec une couche inscriptible par-dessus.

**Layers** : chaque instruction du Dockerfile crée une couche, mise en cache et partagée entre images. D'où deux règles : ordonner **du moins au plus changeant** (dépendances avant code source) pour maximiser le cache, et savoir que supprimer un fichier dans une couche ultérieure ne réduit pas la taille, il reste dans la couche précédente.

**Multi-stage build** : la pratique la plus impactante :
```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline        # couche cachée tant que le pom ne change pas
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/app.jar app.jar
RUN addgroup -S app && adduser -S app -G app
USER app
ENTRYPOINT ["java","-jar","app.jar"]
```
Le stage de build contient le JDK et Maven ; l'image finale seulement un JRE et le jar. On passe couramment de 1,5 Go à 200 Mo.

**Bonnes pratiques** : image de base minimale (alpine, distroless) pour réduire taille et surface d'attaque ; utilisateur **non-root** ; `.dockerignore` pour exclure `target/`, `.git/` ; un processus par conteneur ; ne jamais mettre de secret dans une image (il reste dans les couches).

**CMD vs ENTRYPOINT** : ENTRYPOINT définit la commande principale, CMD ses arguments par défaut, remplaçables au `docker run`.

**Volumes** : le conteneur est éphémère ; les données persistantes vont dans un volume.

**Réseau** : chaque conteneur a sa pile réseau. Dans un réseau Docker, les conteneurs se joignent par leur **nom de service** : `localhost` dans un conteneur désigne le conteneur lui-même, erreur très fréquente.

**Docker Compose** : orchestrer plusieurs conteneurs localement via un YAML (application + base + cache).

**Sécurité** : scanner les images (Trivy, Snyk), rebâtir régulièrement pour récupérer les correctifs de la base, épingler les versions d'images plutôt que d'utiliser `latest`.

**Instructions du Dockerfile, les nuances** :
- `COPY` vs `ADD` : ADD sait aussi extraire des archives locales et télécharger des URL ; on préfère COPY, plus prévisible.
- `ARG` (variable disponible au **build** uniquement) vs `ENV` (variable présente aussi à l'**exécution**). Un secret passé en ARG reste visible dans l'historique de l'image : utiliser les secrets de BuildKit (`RUN --mount=type=secret`).
- `RUN` exécute au build ; `CMD`/`ENTRYPOINT` à l'exécution.
- **Forme exec vs forme shell** : `ENTRYPOINT ["java", "-jar", "app.jar"]` (exec) lance java comme **PID 1**, qui reçoit directement le SIGTERM ; `ENTRYPOINT java -jar app.jar` (shell) lance un `sh -c`, qui ne transmet pas toujours les signaux : l'application est tuée brutalement après le délai de grâce. Si un script de démarrage est nécessaire, finir par `exec java ...`.
- `HEALTHCHECK` (Docker seul, ignoré par Kubernetes qui a ses probes), `EXPOSE` (documentaire), `WORKDIR`, `USER`, `LABEL`.
- **BuildKit** : cache de dépendances avec `RUN --mount=type=cache,target=/root/.m2 mvn package`, builds parallèles, multi-architecture avec `docker buildx` (images amd64 et arm64).

**Construire une image Java sans Dockerfile** : **Jib** (plugin Maven/Gradle, sans démon Docker, couches séparées dépendances/ressources/classes), **Buildpacks** (`mvn spring-boot:build-image`, image optimisée automatiquement). Les **jars en couches** de Spring Boot (`java -Djarmode=tools -jar app.jar extract --layers`) permettent de séparer dépendances et code pour un meilleur cache.

**Images de base** : `eclipse-temurin:<version>-jre`, variantes `-alpine` (musl, très légère, parfois des différences de comportement natif), images **distroless** (sans shell ni gestionnaire de paquets, surface d'attaque minimale, mais débogage plus difficile), images `ubi` (Red Hat, pour OpenShift). Épingler par **digest** (`@sha256:...`) pour une reproductibilité totale.

**Stockage** : *volume* nommé géré par Docker (persistance), *bind mount* (un répertoire de l'hôte monté, pratique en développement), *tmpfs* (en mémoire).

**Réseau** : `bridge` (défaut, réseau isolé avec NAT ; sur un réseau utilisateur, résolution DNS par nom de conteneur), `host` (partage la pile réseau de l'hôte), `none`. Publier un port : `-p 8080:8080` (hôte:conteneur).

**Commandes essentielles** : `docker build -t app:1.0 .`, `docker run -d --name app -p 8080:8080 -e VAR=x app:1.0`, `docker ps -a`, `docker logs -f app`, `docker exec -it app sh`, `docker inspect`, `docker stats`, `docker images`, `docker rmi`, `docker system prune`, `docker history image` (voir les couches et leur taille), `docker compose up -d` / `down` / `logs`.

**Docker Compose** : `depends_on` n'attend par défaut que le **démarrage** du conteneur, pas que le service soit prêt ; avec `condition: service_healthy` et un healthcheck, on attend vraiment. Fichiers d'override pour varier par environnement, profils.

**Écosystème** : standard **OCI** (format d'image et runtime, indépendant de Docker), runtimes **containerd** et CRI-O (utilisés par Kubernetes, qui ne passe plus par le démon Docker depuis la version 1.24), **Podman** (sans démon, conteneurs rootless), registries (Docker Hub, GHCR, Harbor, ECR, ACR).

**JVM en conteneur, la checklist** : JDK récent (détection des limites cgroups v2), `-XX:MaxRAMPercentage=70` à `75`, `-XX:ActiveProcessorCount` si la limite CPU est fractionnaire, `ExitOnOutOfMemoryError`, logs sur stdout, utilisateur non root, arrêt propre (exec form), éventuellement CDS/AOT cache pour le démarrage.

### En situation

- **"Notre image fait 1,5 Go"** → multi-stage, base minimale, layers.
- **"Le build Docker est lent"** → ordre des instructions, cache.
- **"Ça marche en local mais pas en conteneur"** → config, réseau, chemins, ressources.
- **"Comment sécuriser une image ?"** → non-root, base minimale, scan, pas de secret.

### Questions techniques

**Q : Conteneur vs VM ?** R : Le conteneur est un processus isolé sur le noyau hôte (namespaces + cgroups) ; la VM virtualise un OS complet.

**Q : Quels mécanismes Linux sous-tendent les conteneurs ?** R : Les namespaces (isolation) et les cgroups (limitation de ressources).

**Q : Image vs conteneur ?** R : Template en lecture seule vs instance en exécution avec couche inscriptible.

**Q : Pourquoi le multi-stage ?** R : Ne garder que l'artefact final dans l'image, sans les outils de build, taille et surface d'attaque réduites.

**Q : Pourquoi ordonner les instructions ?** R : Pour maximiser le cache des couches.

**Q : Supprimer un fichier réduit-il l'image ?** R : Non, s'il a été ajouté dans une couche précédente.

**Q : CMD vs ENTRYPOINT ?** R : Commande principale vs arguments par défaut remplaçables.

**Q : Pourquoi non-root ?** R : Limiter les dégâts en cas de compromission du conteneur.

**Q : Peut-on mettre un secret dans une image ?** R : Non, il reste consultable dans les couches.

**Q : COPY vs ADD ?** R : COPY copie ; ADD sait aussi extraire des archives et télécharger, donc on préfère COPY.

**Q : ARG vs ENV ?** R : Variable de build uniquement vs variable présente aussi à l'exécution.

**Q : Forme exec vs forme shell de l'ENTRYPOINT ?** R : Le processus est PID 1 et reçoit les signaux vs il est lancé par un shell qui peut ne pas transmettre le SIGTERM.

**Q : Comment ne pas laisser de secret dans une image ?** R : Secrets BuildKit (RUN --mount=type=secret), jamais ARG/ENV/COPY d'un secret.

**Q : C'est quoi Jib ?** R : Un outil qui construit des images Java depuis Maven/Gradle sans Dockerfile ni démon Docker.

**Q : Alpine vs distroless ?** R : Image minimale avec shell (libc musl) vs image sans shell ni gestionnaire de paquets, encore plus réduite.

**Q : Volume vs bind mount ?** R : Stockage géré par Docker vs répertoire de l'hôte monté.

**Q : depends_on garantit-il que la base est prête ?** R : Non, seulement démarrée ; il faut condition: service_healthy avec un healthcheck.

**Q : Kubernetes utilise-t-il encore Docker ?** R : Non depuis 1.24 : il utilise containerd ou CRI-O, et exécute les mêmes images OCI.

**Q : Podman vs Docker ?** R : Podman fonctionne sans démon et en rootless, avec une CLI compatible.

**Q : Comment voir le poids de chaque couche ?** R : docker history.

**Q : Pourquoi épingler une image par digest ?** R : Un tag peut être déplacé ; le digest garantit exactement le même contenu.

### Questions de situation

**Q : "Votre image fait 1,5 Go, réduis-la."**
R : "Du plus au moins efficace.
Le **multi-stage build** d'abord : si l'image contient le JDK, Maven et les sources, c'est le gros du poids. Un stage compile, le stage final ne garde que le jar sur un JRE. On passe typiquement à quelques centaines de mégas.
L'**image de base** ensuite : alpine ou distroless réduit encore et diminue la surface d'attaque, avec moins de paquets à patcher.
Les **couches** : regrouper installation et nettoyage dans une même instruction, puisque supprimer plus tard ne réduit rien.
Et un **`.dockerignore`** correct, pour ne pas envoyer `target/` ou `.git/` dans le contexte de build.
Bénéfices concrets : déploiements plus rapides, moins de bande passante, moins de vulnérabilités à suivre."

**Q : "L'application marche en local mais pas dans le conteneur."**
R : "Je chercherais les différences d'environnement, dans cet ordre.
La **configuration** : un fichier de propriétés ou une variable présente en local et absente du conteneur. Je vérifierais avec `docker exec env`.
Le **réseau**, cause très fréquente : `localhost` dans un conteneur désigne le conteneur lui-même, pas l'hôte. Une base joignable en local sur `localhost:5432` doit être référencée par son nom de service dans le réseau Docker.
Les **chemins et permissions** : un chemin absolu qui n'existe pas dans l'image, ou un conteneur non-root sans droit d'écriture là où il essaie.
Les **ressources** : si la JVM ne respecte pas la limite mémoire du conteneur, celui-ci est tué, OOMKilled.
En pratique je lance un shell avec `docker exec -it` et je lis `docker logs`. L'erreur est souvent explicite, mais personne n'est allé la lire."

**Q : "Ton application dans un conteneur met 30 secondes à s'arrêter et perd des requêtes."**
R : "Trente secondes, c'est le délai de grâce par défaut : le processus n'a pas réagi au SIGTERM et a été tué par SIGKILL. Cause probable : l'ENTRYPOINT en forme **shell**, donc java n'est pas PID 1 et ne reçoit pas le signal, ou un script qui lance java sans `exec`.
Correction : forme exec, ou `exec java ...` dans le script, voire un mini-init comme `tini`. Ensuite, côté application : activer l'arrêt gracieux (`server.shutdown=graceful` en Spring Boot) pour finir les requêtes en cours, et en Kubernetes un `preStop` court pour laisser le temps au retrait du pod des endpoints."
