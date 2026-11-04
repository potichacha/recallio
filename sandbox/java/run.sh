#!/bin/sh
# Exécute une soumission (ADR 0030) ou un programme du Tour (ADR 0037), selon le premier argument : tests (par défaut)
# ou program, suivi du délai du programme en secondes.
# L'entrée standard est une archive tar : le code dans src/, et pour une soumission les tests dans tests/.
# La sortie standard commence par une ligne de résultat, que le runner lit :
#   @@recallio:invalid                         l'archive est illisible
#   @@recallio:compilation-error               suivie des erreurs de javac
#   @@recallio:report                          suivie du rapport XML de JUnit
#   @@recallio:program <code> <tronqué : 0|1>  suivie des sorties standard et d'erreur du programme, en base64, une
#                                              ligne chacune
# Le code exécuté écrit dans /dev/null, ou dans des fichiers pour un programme : il ne peut pas se mêler à ce protocole.
set -u

MODE=${1:-tests}
PROGRAM_SECONDS=${2:-10}
# Ce que le runner reçoit de chaque sortie d'un programme : 64 Kio.
MAX_OUTPUT=65536
JUNIT=/opt/recallio/junit.jar
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
    # Compilé depuis src/ : les erreurs nomment Main.java, comme le voit l'utilisateur.
    cd src || exit 1
    find . -name '*.java' -type f > /tmp/sources
    if ! javac -encoding UTF-8 -nowarn -d /tmp/classes @/tmp/sources > /tmp/javac.log 2>&1; then
        echo "@@recallio:compilation-error"
        sed 's|^\./||' /tmp/javac.log
        exit 0
    fi
    # Sans entrée standard ; au bout du délai, timeout l'arrête et sort avec le code 124.
    timeout -k 1 "$PROGRAM_SECONDS" java -Xmx256m -Xss1m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 \
        -cp /tmp/classes Main < /dev/null > /tmp/stdout 2> /tmp/stderr
    report_program $?
    exit 0
fi

find src tests -name '*.java' -type f > /tmp/sources 2>/dev/null
if ! javac -encoding UTF-8 -nowarn -d /tmp/classes -cp "$JUNIT" @/tmp/sources > /tmp/javac.log 2>&1; then
    echo "@@recallio:compilation-error"
    sed 's|^/work/||' /tmp/javac.log
    exit 0
fi

java -Xmx256m -Xss1m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -jar "$JUNIT" execute \
    --class-path /tmp/classes --scan-class-path --disable-banner --details=none \
    --reports-dir /tmp/reports > /dev/null 2>&1

echo "@@recallio:report"
cat /tmp/reports/TEST-junit-jupiter.xml 2>/dev/null
