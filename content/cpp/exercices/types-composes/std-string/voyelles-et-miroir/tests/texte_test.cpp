#include "doctest.h"
#include "texte.hpp"

TEST_CASE("compte les voyelles d'un mot") {
    CHECK(compter_voyelles("Bonjour") == 3);
    CHECK(compter_voyelles("rythme") == 2);
}

TEST_CASE("compte les voyelles en majuscules") {
    CHECK(compter_voyelles("AEIOUY") == 6);
}

TEST_CASE("ne compte rien sans voyelle") {
    CHECK(compter_voyelles("C++ 23") == 0);
    CHECK(compter_voyelles("") == 0);
}

TEST_CASE("inverse un mot") {
    CHECK(inverser("abc") == "cba");
    CHECK(inverser("Recallio") == "oillaceR");
}

TEST_CASE("inverse une chaîne d'un caractère ou vide") {
    CHECK(inverser("x") == "x");
    CHECK(inverser("") == "");
}

TEST_CASE("inverser deux fois redonne le texte") {
    CHECK(inverser(inverser("bonjour le monde")) == "bonjour le monde");
}
