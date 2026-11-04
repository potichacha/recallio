#!/bin/sh
# Exécute une soumission Go (ADR 0033) ou un programme du Tour (ADR 0037), selon le premier argument : tests (par
# défaut) ou program, suivi du délai du programme en secondes.
# L'entrée standard est une archive tar : le code dans src/, et pour une soumission les tests dans tests/.
# La sortie standard commence par une ligne de résultat, que le runner lit :
#   @@recallio:invalid                         l'archive est illisible
#   @@recallio:compilation-error               suivie des erreurs du compilateur
#   @@recallio:report                          suivie du flux JSON de test2json, un événement par ligne
#   @@recallio:program <code> <tronqué : 0|1>  suivie des sorties standard et d'erreur du programme, en base64, une
#                                              ligne chacune
# Ce que le code écrit passe par test2json, ou par des fichiers pour un programme : il ne peut pas se mêler à ce
# protocole.
set -u

MODE=${1:-tests}
PROGRAM_SECONDS=${2:-10}
# Ce que le runner reçoit de chaque sortie d'un programme : 64 Kio.
MAX_OUTPUT=65536
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
    # Un seul package main, dans un module sans dépendance.
    mkdir program && cp src/*.go program/ 2>/dev/null
    cd program || exit 1
    printf 'module programme\n\ngo 1.27\n' > go.mod
    if ! go build -o /tmp/programme . > /tmp/build.log 2>&1; then
        echo "@@recallio:compilation-error"
        grep -v '^#' /tmp/build.log | sed 's|^\./||'
        exit 0
    fi
    # Sans entrée standard ; au bout du délai, timeout l'arrête et sort avec le code 124.
    timeout -k 1 "$PROGRAM_SECONDS" /tmp/programme < /dev/null > /tmp/stdout 2> /tmp/stderr
    report_program $?
    exit 0
fi

# Un seul package : le code et ses tests dans le même dossier, avec un module sans dépendance.
mkdir package && cp src/*.go tests/*.go package/ 2>/dev/null
cd package || exit 1
printf 'module exercice\n\ngo 1.27\n' > go.mod

if ! go test -c -o /tmp/exercice.test . > /tmp/build.log 2>&1; then
    echo "@@recallio:compilation-error"
    # Les chemins sont relatifs au package (./panier.go:3:2) : on retire le « ./ » et la ligne d'en-tête de go test.
    grep -v '^#' /tmp/build.log | sed 's|^\./||'
    exit 0
fi

echo "@@recallio:report"
# -test.paniconexit0, que go test passe d'office : un os.Exit(0) en plein test devient une panic, pas une réussite.
/opt/recallio/test2json -t /tmp/exercice.test -test.v=test2json -test.paniconexit0 -test.timeout=15s 2>/dev/null
