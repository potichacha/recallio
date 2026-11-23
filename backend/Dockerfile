# syntax=docker/dockerfile:1
# Contexte de build : la racine du dépôt, comme pour le front : le contenu (content/) voyage dans l'image (ADR 0069).
# Ce qui entre dans le contexte est filtré par backend/Dockerfile.dockerignore.
#
#   docker build -f backend/Dockerfile -t recallio-backend .

# --- Build : compile l'application avec le wrapper Maven -----------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# Sans unzip, le wrapper Maven télécharge le .tar.gz, dont l'empreinte ne correspond pas
# au distributionSha256Sum (calculé pour le .zip) : la vérification échouerait.
RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/*

# Dépendances d'abord : cette couche reste en cache tant que le pom ne change pas.
COPY backend/mvnw backend/pom.xml ./
COPY backend/.mvn/ .mvn/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:resolve dependency:resolve-plugins

COPY backend/src/main/ src/main/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests

# --- Exécution : image JRE minimale recommandée par Quarkus ---------------
FROM registry.access.redhat.com/ubi9/openjdk-25-runtime:1.24
ENV LANGUAGE='en_US:en'

COPY --from=build --chown=185 /build/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build --chown=185 /build/target/quarkus-app/*.jar /deployments/
COPY --from=build --chown=185 /build/target/quarkus-app/app/ /deployments/app/
COPY --from=build --chown=185 /build/target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8080
USER 185
ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

# Le contenu voyage dans l'image (ADR 0069) : le parseur et le contenu d'un même commit, vérifiés ensemble par la CI,
# partent ensemble. Tous les documents de référence et tous les dossiers de sujet sont importés à chaque démarrage
# (l'import est idempotent, ADR 0009). Copié en dernier : le contenu change plus souvent que le code, et les couches de
# l'application restent en cache. Il appartient à root : l'application le lit sans pouvoir le modifier.
ENV RECALLIO_CONTENT_IMPORT_ON_STARTUP=true \
    RECALLIO_CONTENT_SOURCE_DIRECTORY=/content/source \
    RECALLIO_CONTENT_SUBJECTS_DIRECTORY=/content
COPY content/ /content/

ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]
