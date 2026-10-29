# syntax=docker/dockerfile:1

# --- Build : compile l'application avec le wrapper Maven -----------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# Sans unzip, le wrapper Maven télécharge le .tar.gz, dont l'empreinte ne correspond pas
# au distributionSha256Sum (calculé pour le .zip) : la vérification échouerait.
RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/*

# Dépendances d'abord : cette couche reste en cache tant que le pom ne change pas.
COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:resolve dependency:resolve-plugins

COPY src/main/ src/main/
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

ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]
