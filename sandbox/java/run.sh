#!/bin/sh
# Exécute une soumission (ADR 0030). L'entrée standard est une archive tar : le code dans src/, les tests dans tests/.
# La sortie standard commence par une ligne de résultat, que le runner lit :
#   @@recallio:invalid             l'archive est illisible
#   @@recallio:compilation-error   suivie des erreurs de javac
#   @@recallio:report              suivie du rapport XML de JUnit
# Le code exécuté écrit dans /dev/null : il ne peut pas se mêler à ce protocole.
set -u

JUNIT=/opt/recallio/junit.jar
cd /work || exit 1

if ! tar -x -f - 2>/dev/null; then
    echo "@@recallio:invalid"
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
