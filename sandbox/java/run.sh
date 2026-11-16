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
# Spring Boot et ses dépendances (ADR 0060) ; l'étoile est développée par javac et java, pas par le shell. javac
# compile avec -parameters, comme les plugins Maven et Gradle de Spring Boot : Spring lit le nom des paramètres.
LIBRARIES='/opt/recallio/lib/*'
# Une JVM qui démarre vite et tient dans la mémoire du conteneur, même pour un contexte Spring.
JVM_OPTIONS="-Xmx256m -Xss1m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
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

# Vrai si le code importe autre chose que le JDK et JUnit : Spring Boot, AssertJ, Mockito... Sans ces bibliothèques,
# javac et la JVM démarrent une demi-seconde plus tôt : le Java ordinaire ne les charge pas.
imports_libraries() {
    grep -rhsE '^[[:space:]]*import[[:space:]]' "$@" \
        | grep -vqE '^[[:space:]]*import[[:space:]]+(static[[:space:]]+|module[[:space:]]+)?(java|javax|org\.junit)\.'
}

if ! tar -x -f - 2>/dev/null; then
    echo "@@recallio:invalid"
    exit 0
fi

EXTRA=""
if imports_libraries src tests; then
    EXTRA=":$LIBRARIES"
fi

if [ "$MODE" = program ]; then
    # Compilé depuis src/ : les erreurs nomment Main.java, comme le voit l'utilisateur.
    cd src || exit 1
    find . -name '*.java' -type f > /tmp/sources
    if ! javac -encoding UTF-8 -nowarn -parameters -d /tmp/classes -cp "/tmp/classes$EXTRA" @/tmp/sources \
        > /tmp/javac.log 2>&1; then
        echo "@@recallio:compilation-error"
        sed 's|^\./||' /tmp/javac.log
        exit 0
    fi
    # Sans entrée standard ; au bout du délai, timeout l'arrête et sort avec le code 124.
    # shellcheck disable=SC2086
    timeout -k 1 "$PROGRAM_SECONDS" java $JVM_OPTIONS \
        -cp "/tmp/classes$EXTRA" Main < /dev/null > /tmp/stdout 2> /tmp/stderr
    report_program $?
    exit 0
fi

find src tests -name '*.java' -type f > /tmp/sources 2>/dev/null
if ! javac -encoding UTF-8 -nowarn -parameters -d /tmp/classes -cp "$JUNIT$EXTRA" @/tmp/sources > /tmp/javac.log 2>&1; then
    echo "@@recallio:compilation-error"
    sed 's|^/work/||' /tmp/javac.log
    exit 0
fi

# Les bibliothèques sur le classpath de la JVM, mais seules les classes de l'exercice sont parcourues pour y trouver
# les tests.
# shellcheck disable=SC2086
java $JVM_OPTIONS -cp "$JUNIT$EXTRA:/tmp/classes" org.junit.platform.console.ConsoleLauncher execute \
    --scan-class-path /tmp/classes --disable-banner --details=none \
    --reports-dir /tmp/reports > /dev/null 2>&1

echo "@@recallio:report"
cat /tmp/reports/TEST-junit-jupiter.xml 2>/dev/null
