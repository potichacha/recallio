#!/bin/sh
# Exécute une soumission Go (ADR 0033). L'entrée standard est une archive tar : le code dans src/, les tests dans tests/.
# La sortie standard commence par une ligne de résultat, que le runner lit :
#   @@recallio:invalid             l'archive est illisible
#   @@recallio:compilation-error   suivie des erreurs du compilateur
#   @@recallio:report              suivie du flux JSON de test2json, un événement par ligne
# Ce que le code écrit passe par test2json, qui l'enveloppe en événements : il ne peut pas se mêler à ce protocole.
set -u

cd /work || exit 1

if ! tar -x -f - 2>/dev/null; then
    echo "@@recallio:invalid"
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
