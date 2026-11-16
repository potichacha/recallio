# Bac à sable d'exécution de Python (ADR 0062) : lance les tests pytest d'un exercice, ou un programme du Tour. Comme
# ceux des autres langages (ADR 0030, 0033, 0053), l'image ne fait confiance à rien de ce qu'elle reçoit : le runner
# l'isole au lancement (réseau coupé, système de fichiers en lecture seule, ressources plafonnées, utilisateur sans
# droits).
#
#   docker build -t recallio-sandbox-python sandbox/python

# Python 3.14, l'image officielle, légère. Le job content de la CI installe la même version pour les « qu'affiche ce
# code ? » : les deux exécutent le même Python.
FROM python:3.14-slim

# pytest et ses dépendances, aux versions et aux empreintes que fixe requirements.txt (pip-compile, depuis
# requirements.in) : pip refuse un paquet dont l'empreinte diffère. Dependabot les tient à jour.
COPY requirements.txt /tmp/requirements.txt
RUN pip install --no-cache-dir --no-deps --require-hashes --root-user-action=ignore --disable-pip-version-check \
        -r /tmp/requirements.txt \
    && rm /tmp/requirements.txt \
    && install -d -m 755 /opt/recallio

COPY --chmod=555 run.sh /opt/recallio/run.sh

# Comme dans un terminal de la CI : de l'UTF-8 quelle que soit la locale, et des sorties écrites sans attendre (sinon,
# ce qu'un programme affiche avant d'être tué resterait dans son tampon). Le code reçu n'écrit pas de .pyc.
ENV PYTHONUTF8=1 \
    PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

# Un utilisateur sans droits ni dossier personnel ; le runner l'impose aussi (--user).
USER 65534:65534
WORKDIR /work

ENTRYPOINT ["/opt/recallio/run.sh"]
