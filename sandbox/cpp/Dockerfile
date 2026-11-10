# Bac à sable d'exécution du C++ (ADR 0053) : compile le code reçu avec ses tests doctest et les lance, ou lance un
# programme du Tour. Comme ceux de Java et de Go (ADR 0030, 0033), l'image ne fait confiance à rien de ce qu'elle reçoit :
# le runner l'isole au lancement (réseau coupé, système de fichiers en lecture seule, ressources plafonnées, utilisateur
# sans droits).
#
#   docker build -t recallio-sandbox-cpp sandbox/cpp

# GCC 14, en C++23 : le paquet g++-14 d'Ubuntu 24.04, celui qu'installe la CI pour les « qu'affiche ce code ? » (job
# content). Les deux compilent donc avec le même compilateur.
FROM ubuntu:24.04

RUN apt-get update \
    && apt-get install -y --no-install-recommends g++-14 \
    && rm -rf /var/lib/apt/lists/* \
    && ln -s /usr/bin/g++-14 /usr/local/bin/g++ \
    && install -d -m 755 /opt/recallio/include

# doctest, le framework des tests d'exercice : un seul en-tête, vérifié par son empreinte.
ADD --checksum=sha256:94029a7d32da24a56249658147dbd2b33ff0b9ed665295cbbaf19aafff5b0ced --chmod=444 \
    https://raw.githubusercontent.com/doctest/doctest/v2.4.12/doctest/doctest.h \
    /opt/recallio/include/doctest.h

COPY --chmod=444 flags /opt/recallio/flags

# Le main de doctest, compilé une fois : une soumission ne compile que son code et ses tests. L'en-tête précompilé
# (doctest.h.gch, à côté de doctest.h) épargne à chaque fichier de tests l'analyse de doctest ; GCC ne le prend que si
# les options de compilation sont les mêmes, d'où le fichier flags partagé avec run.sh.
RUN cd /opt/recallio \
    && printf '#define DOCTEST_CONFIG_IMPLEMENT_WITH_MAIN\n#include "doctest.h"\n' > /tmp/doctest_main.cpp \
    && g++ $(cat flags) -Iinclude -c /tmp/doctest_main.cpp -o doctest_main.o \
    && g++ $(cat flags) -x c++-header include/doctest.h -o include/doctest.h.gch \
    && rm /tmp/doctest_main.cpp \
    && chmod -R a+rX /opt/recallio

COPY --chmod=555 run.sh /opt/recallio/run.sh

# Un utilisateur sans droits ni dossier personnel ; le runner l'impose aussi (--user).
USER 65534:65534
WORKDIR /work

ENTRYPOINT ["/opt/recallio/run.sh"]
