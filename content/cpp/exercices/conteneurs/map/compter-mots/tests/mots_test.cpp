#include "doctest.h"
#include "mots.hpp"

#include <map>
#include <string>
#include <vector>

TEST_CASE("compte chaque mot") {
    const auto compte = compter({"le", "chat", "et", "le", "chien", "et", "le", "rat"});
    CHECK(compte.size() == 5);
    CHECK(compte.at("le") == 3);
    CHECK(compte.at("et") == 2);
    CHECK(compte.at("chat") == 1);
}

TEST_CASE("renvoie une map vide pour un texte vide") {
    CHECK(compter({}).empty());
}

TEST_CASE("donne les occurrences d'un mot présent") {
    const std::map<std::string, int> compte{{"chat", 2}, {"chien", 1}};
    CHECK(occurrences(compte, "chat") == 2);
    CHECK(occurrences(compte, "chien") == 1);
}

TEST_CASE("donne zéro pour un mot absent, sans l'ajouter") {
    const std::map<std::string, int> compte{{"chat", 2}};
    CHECK(occurrences(compte, "poisson") == 0);
    CHECK(compte.size() == 1);
}

TEST_CASE("trouve le mot le plus fréquent") {
    const std::map<std::string, int> compte{{"chat", 2}, {"chien", 5}, {"rat", 1}};
    CHECK(plus_frequent(compte) == "chien");
}

TEST_CASE("départage une égalité par l'ordre alphabétique") {
    const std::map<std::string, int> compte{{"rat", 3}, {"chat", 3}, {"chien", 1}};
    CHECK(plus_frequent(compte) == "chat");
}

TEST_CASE("renvoie une chaîne vide pour une map vide") {
    CHECK(plus_frequent({}) == "");
}

TEST_CASE("enchaîne le comptage et la recherche du plus fréquent") {
    const auto compte = compter({"b", "a", "b", "a", "c"});
    CHECK(plus_frequent(compte) == "a");
    CHECK(occurrences(compte, "c") == 1);
}
