#include "doctest.h"
#include "texte.hpp"

#include <string>
#include <vector>

TEST_CASE("donne l'extension d'un nom de fichier") {
    CHECK(extension("photo.png") == "png");
    CHECK(extension("archive.tar.gz") == "gz");
}

TEST_CASE("donne une chaîne vide sans point, ou après un point final") {
    CHECK(extension("LISEZMOI") == "");
    CHECK(extension("fin.") == "");
}

TEST_CASE("remplace chaque occurrence") {
    CHECK(remplacer_tout("le chat et le chien", "le", "un") == "un chat et un chien");
    CHECK(remplacer_tout("a-b-c", "-", "+") == "a+b+c");
}

TEST_CASE("laisse le texte tel quel quand rien n'est trouvé") {
    CHECK(remplacer_tout("bonjour", "xyz", "abc") == "bonjour");
    CHECK(remplacer_tout("", "a", "b") == "");
}

TEST_CASE("ne réexamine pas le texte déjà remplacé") {
    CHECK(remplacer_tout("aa", "a", "aa") == "aaaa");
    CHECK(remplacer_tout("abab", "ab", "") == "");
}

TEST_CASE("découpe autour du séparateur") {
    CHECK(decouper("a,b,c", ',') == std::vector<std::string>{"a", "b", "c"});
    CHECK(decouper("un seul", ',') == std::vector<std::string>{"un seul"});
}

TEST_CASE("garde les morceaux vides") {
    CHECK(decouper("a,,b", ',') == std::vector<std::string>{"a", "", "b"});
    CHECK(decouper("a,", ',') == std::vector<std::string>{"a", ""});
    CHECK(decouper("", ',') == std::vector<std::string>{""});
}
