# Bac à sable d'exécution des exercices Java (ADR 0030) : compile le code reçu avec ses tests JUnit et les lance.
# L'image ne fait confiance à rien de ce qu'elle reçoit. C'est le runner qui l'isole au lancement : réseau coupé,
# système de fichiers en lecture seule, mémoire, processeur et nombre de processus plafonnés, utilisateur sans droits.
#
#   docker build -t recallio-sandbox-java sandbox/java

# Les bibliothèques de Spring Boot (ADR 0060), téléchargées pendant la construction : le conteneur, lui, n'a pas de
# réseau. Maven vérifie l'empreinte de chaque fichier (-C) ; JUnit et ce qu'il embarque restent ceux du lanceur.
FROM maven:3.9-eclipse-temurin-26 AS libraries
WORKDIR /build
COPY libraries/pom.xml .
RUN mvn -B -C -q dependency:copy-dependencies -DoutputDirectory=/lib \
        -DexcludeGroupIds=junit,org.junit,org.junit.jupiter,org.junit.platform,org.opentest4j,org.apiguardian

FROM eclipse-temurin:25-jdk-noble

RUN install -d -m 755 /opt/recallio

# Le lanceur JUnit autonome, vérifié par son empreinte : l'image ne dépend pas d'un téléchargement modifié.
ADD --checksum=sha256:e62b96ac475dbcde8599ea905d088f65d90778f86e259b856a49fa5c4ea256ec --chmod=444 \
    https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/6.1.3/junit-platform-console-standalone-6.1.3.jar \
    /opt/recallio/junit.jar

# Lisibles par tous, modifiables par root seul : l'utilisateur du conteneur ne peut pas les remplacer.
COPY --from=libraries /lib/ /opt/recallio/lib/
COPY --chmod=555 run.sh /opt/recallio/run.sh

# Un utilisateur sans droits ni dossier personnel ; le runner l'impose aussi (--user).
USER 65534:65534
WORKDIR /work

ENTRYPOINT ["/opt/recallio/run.sh"]
