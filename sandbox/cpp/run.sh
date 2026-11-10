#!/bin/sh
# Exécute une soumission C++ (ADR 0053) ou un programme du Tour (ADR 0037), selon le premier argument : tests (par
# défaut) ou program, suivi du délai du programme en secondes.
# L'entrée standard est une archive tar : le code dans src/, et pour une soumission les tests dans tests/.
# La sortie standard commence par une ligne de résultat, que le runner lit :
#   @@recallio:invalid                         l'archive est illisible
#   @@recallio:compilation-error               suivie des erreurs du compilateur
#   @@recallio:report                          suivie du rapport XML de doctest, au format JUnit
#   @@recallio:program <code> <tronqué : 0|1>  suivie des sorties standard et d'erreur du programme, en base64, une
#                                              ligne chacune
# Les tests écrivent leur rapport dans un fichier, et un programme ses sorties : ce que le code écrit ne peut pas se
# mêler à ce protocole.
set -u

MODE=${1:-tests}
PROGRAM_SECONDS=${2:-10}
# Ce que le runner reçoit de chaque sortie d'un programme : 64 Kio.
MAX_OUTPUT=65536
# Les options de compilation, celles de l'en-tête précompilé de doctest : sans elles, GCC l'ignorerait.
FLAGS=$(cat /opt/recallio/flags)
# Les tests s'arrêtent avant le délai du conteneur, pour laisser le temps de rendre leur rapport.
TESTS_SECONDS=15
cd /work || exit 1

# Le résultat d'un programme : son code de sortie, si une sortie a été tronquée, puis ses deux sorties en base64.
report_program() {
    truncated=0
    for output in /tmp/stdout /tmp/stderr; do
        if [ "$(wc -c < "$output")" -gt "$MAX_OUTPUT" ]; then
            truncated=1
        fi
    done
    echo "@@recallio:program $1 $truncated"
    head -c "$MAX_OUTPUT" /tmp/stdout | base64 -w 0
    echo
    head -c "$MAX_OUTPUT" /tmp/stderr | base64 -w 0
    echo
}

if ! tar -x -f - 2>/dev/null; then
    echo "@@recallio:invalid"
    exit 0
fi

if [ "$MODE" = program ]; then
    # Compilé depuis src/ : les erreurs nomment main.cpp, comme le voit l'utilisateur.
    cd src || exit 1
    # shellcheck disable=SC2086
    if ! g++ $FLAGS -o /tmp/programme ./*.cpp > /tmp/build.log 2>&1; then
        echo "@@recallio:compilation-error"
        sed 's|^\./||' /tmp/build.log
        exit 0
    fi
    # Sans entrée standard ; au bout du délai, timeout l'arrête et sort avec le code 124. La sortie standard part ligne
    # par ligne, comme dans un terminal (stdbuf -oL) : sinon, ce qu'un programme affiche avant de planter resterait
    # dans son tampon, perdu. Seul le programme écrit dans /tmp/stderr : les messages de timeout sont écartés.
    : > /tmp/stderr
    timeout -k 1 "$PROGRAM_SECONDS" sh -c 'exec stdbuf -oL /tmp/programme 2> /tmp/stderr' \
        < /dev/null > /tmp/stdout 2> /dev/null
    code=$?
    # Un plantage n'écrit rien de lui-même : comme un terminal, on nomme le signal qui a arrêté le programme.
    case $code in
        134) echo "Aborted" ;;
        136) echo "Floating point exception" ;;
        139) echo "Segmentation fault" ;;
    esac >> /tmp/stderr
    report_program $code
    exit 0
fi

# Le code et ses tests côte à côte, à plat : un test inclut l'en-tête qu'il teste par son nom (#include "panier.hpp").
mkdir exercice && cp src/* tests/* exercice/ 2>/dev/null
cd exercice || exit 1

# shellcheck disable=SC2086
if ! g++ $FLAGS -I/opt/recallio/include -o /tmp/exercice ./*.cpp /opt/recallio/doctest_main.o > /tmp/build.log 2>&1; then
    echo "@@recallio:compilation-error"
    sed 's|^\./||' /tmp/build.log
    exit 0
fi

# Un plantage (segmentation, exit en plein test) laisse le rapport vide : le runner le dit.
timeout -k 1 "$TESTS_SECONDS" /tmp/exercice --reporters=junit --out=/tmp/report.xml > /dev/null 2>&1
echo "@@recallio:report"
# Les fichiers ont été compilés sous leur nom précédé de ./ (un nom qui commence par - n'est pas une option) : le
# rapport les nomme comme l'utilisateur les voit.
sed 's|^\./||; s|classname="\./|classname="|' /tmp/report.xml 2>/dev/null
