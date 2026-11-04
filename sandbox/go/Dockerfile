# Bac à sable d'exécution des exercices Go (ADR 0033) : compile le code reçu avec ses tests et les lance.
# Comme celui de Java (ADR 0030), l'image ne fait confiance à rien de ce qu'elle reçoit : le runner l'isole au lancement
# (réseau coupé, système de fichiers en lecture seule, ressources plafonnées, utilisateur sans droits).
#
#   docker build -t recallio-sandbox-go sandbox/go

FROM golang:1.27-bookworm

# Rien ne se télécharge à l'exécution : ni module, ni autre chaîne d'outils. Le cache de compilation est celui de
# l'image, lu en lecture seule ; GOPATH et les fichiers temporaires vont dans le tmpfs /tmp.
ENV GOCACHE=/opt/recallio/gocache \
    GOPATH=/tmp/gopath \
    GOTMPDIR=/tmp \
    GOPROXY=off \
    GOTOOLCHAIN=local \
    GOTELEMETRY=off \
    CGO_ENABLED=0

# Précompile la bibliothèque standard dont se servent les exercices : sans ce cache, chaque exécution la recompilerait
# (27 secondes sur un processeur, au-delà du délai du runner). test2json n'est plus fourni compilé : on le compile.
COPY warm/ /opt/recallio/warm/
RUN cd /opt/recallio/warm \
    && go test -count=1 . > /dev/null \
    && go build -o /opt/recallio/test2json cmd/test2json \
    && rm -rf /opt/recallio/warm \
    && chmod -R a+rX /opt/recallio

COPY --chmod=555 run.sh /opt/recallio/run.sh

# Un utilisateur sans droits ni dossier personnel ; le runner l'impose aussi (--user).
USER 65534:65534
WORKDIR /work

ENTRYPOINT ["/opt/recallio/run.sh"]
