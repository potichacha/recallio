# syntax=docker/dockerfile:1

# --- Build : compile le runner avec le wrapper Maven ---------------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# Sans unzip, le wrapper Maven télécharge le .tar.gz, dont l'empreinte ne correspond pas
# au distributionSha256Sum (calculé pour le .zip) : la vérification échouerait.
RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/*

COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:resolve dependency:resolve-plugins

COPY src/main/ src/main/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests

# --- Exécution ----------------------------------------------------------
FROM registry.access.redhat.com/ubi9/openjdk-25-runtime:1.24
ENV LANGUAGE='en_US:en'

# La ligne de commande docker : le runner lance un conteneur de bac à sable par exécution (ADR 0030).
COPY --from=docker:29-cli /usr/local/bin/docker /usr/local/bin/docker

COPY --from=build --chown=185 /build/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build --chown=185 /build/target/quarkus-app/*.jar /deployments/
COPY --from=build --chown=185 /build/target/quarkus-app/app/ /deployments/app/
COPY --from=build --chown=185 /build/target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8090
# Le socket Docker, monté depuis l'hôte, n'est accessible qu'à root. Lui donner accès, c'est donner les droits root
# sur l'hôte quel que soit l'utilisateur du conteneur : c'est pourquoi le runner est isolé du reste (ADR 0030).
USER root
ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]
