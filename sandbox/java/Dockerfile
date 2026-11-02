# Bac à sable d'exécution des exercices Java (ADR 0030) : compile le code reçu avec ses tests JUnit et les lance.
# L'image ne fait confiance à rien de ce qu'elle reçoit. C'est le runner qui l'isole au lancement : réseau coupé,
# système de fichiers en lecture seule, mémoire, processeur et nombre de processus plafonnés, utilisateur sans droits.
#
#   docker build -t recallio-sandbox-java sandbox/java

FROM eclipse-temurin:25-jdk-noble

RUN install -d -m 755 /opt/recallio

# Le lanceur JUnit autonome, vérifié par son empreinte : l'image ne dépend pas d'un téléchargement modifié.
ADD --checksum=sha256:e62b96ac475dbcde8599ea905d088f65d90778f86e259b856a49fa5c4ea256ec --chmod=444 \
    https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/6.1.3/junit-platform-console-standalone-6.1.3.jar \
    /opt/recallio/junit.jar

COPY --chmod=555 run.sh /opt/recallio/run.sh

# Un utilisateur sans droits ni dossier personnel ; le runner l'impose aussi (--user).
USER 65534:65534
WORKDIR /work

ENTRYPOINT ["/opt/recallio/run.sh"]
