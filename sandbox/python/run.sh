#!/bin/sh
# Exécute une soumission Python (ADR 0062) ou un programme du Tour (ADR 0037), selon le premier argument : tests (par
# défaut) ou program, suivi du délai du programme en secondes.
# L'entrée standard est une archive tar : le code dans src/, et pour une soumission les tests dans tests/.
# La sortie standard commence par une ligne de résultat, que le runner lit :
#   @@recallio:invalid                         l'archive est illisible
#   @@recallio:compilation-error               suivie de l'erreur de syntaxe, ou de ce qui empêche de charger le code
#   @@recallio:report                          suivie du rapport XML de pytest, au format JUnit
#   @@recallio:program <code> <tronqué : 0|1>  suivie des sorties standard et d'erreur du programme, en base64, une
#                                              ligne chacune
# Les tests écrivent leur rapport dans un fichier, et un programme ses sorties : ce que le code écrit ne peut pas se
# mêler à ce protocole.
set -u

MODE=${1:-tests}
PROGRAM_SECONDS=${2:-10}
# Ce que le runner reçoit de chaque sortie d'un programme : 64 Kio.
MAX_OUTPUT=65536
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

# Python n'a pas de compilation à part : on vérifie la syntaxe de chaque fichier avant de rien exécuter, comme le ferait
# un compilateur. Une erreur s'écrit comme Python l'écrit (le fichier, la ligne, le caret, le message), sans pile.
check_syntax() {
    python - "$@" > /tmp/syntax.log 2>&1 << 'EOF'
import sys
import traceback

valid = True
for path in sys.argv[1:]:
    with open(path, encoding="utf-8") as source:
        try:
            compile(source.read(), path, "exec")
        except SyntaxError as error:
            valid = False
            print("".join(traceback.format_exception_only(error)), end="")
sys.exit(0 if valid else 1)
EOF
}

if ! tar -x -f - 2>/dev/null; then
    echo "@@recallio:invalid"
    exit 0
fi

if [ "$MODE" = program ]; then
    # Lancé depuis src/ : les erreurs nomment main.py, comme le voit l'utilisateur.
    cd src || exit 1
    if ! check_syntax ./*.py; then
        echo "@@recallio:compilation-error"
        sed 's|"\./|"|' /tmp/syntax.log
        exit 0
    fi
    # Sans entrée standard ; au bout du délai, timeout l'arrête et sort avec le code 124. Seul le programme écrit dans
    # /tmp/stderr : les messages de timeout sont écartés. Une exception que personne ne rattrape y laisse sa pile, et
    # le programme sort avec le code 1, comme dans un terminal.
    : > /tmp/stderr
    timeout -k 1 "$PROGRAM_SECONDS" sh -c 'exec python main.py 2> /tmp/stderr' \
        < /dev/null > /tmp/stdout 2> /dev/null
    code=$?
    # Python nomme le fichier par son chemin complet : on le rend tel que l'utilisateur le voit.
    sed -i 's|"/work/src/|"|' /tmp/stderr
    report_program $code
    exit 0
fi

# Le code et ses tests côte à côte, à plat : un test importe le module qu'il teste par son nom (from panier import ...).
mkdir exercice && cp src/* tests/* exercice/ 2>/dev/null
cd exercice || exit 1

if ! check_syntax ./*.py; then
    echo "@@recallio:compilation-error"
    sed 's|"\./|"|' /tmp/syntax.log
    exit 0
fi

# pytest sans son cache (rien à garder d'une exécution à l'autre), avec des piles courtes et un rapport JUnit. Il sort
# avec le code 2 quand il n'a pas pu charger un fichier de tests : un import qui échoue, une exception au chargement du
# module. Rien ne s'est exécuté, et le joueur lit pourquoi, comme une erreur de compilation : la section des erreurs de
# pytest, sans les cadres de la bibliothèque standard ni le conseil sur le nom des modules, qui ne le concerne pas.
timeout -k 1 "$TESTS_SECONDS" python -m pytest -q --tb=short -p no:cacheprovider --junitxml=/tmp/report.xml \
    > /tmp/pytest.log 2>&1
if [ $? -eq 2 ]; then
    echo "@@recallio:compilation-error"
    sed -n '/^=* ERRORS =*$/,/^=* short test summary info =*$/p' /tmp/pytest.log | sed '1d;$d' \
        | sed 's|/work/exercice/||g' \
        | awk '/^\/usr\/local\/lib\// { skip = 1; next }
               skip && /^[ \t]/ { next }
               /^Hint: / { next }
               { skip = 0; print }'
    exit 0
fi
echo "@@recallio:report"
cat /tmp/report.xml 2>/dev/null
