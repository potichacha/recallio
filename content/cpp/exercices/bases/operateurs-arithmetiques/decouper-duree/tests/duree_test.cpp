#include "doctest.h"
#include "duree.hpp"

TEST_CASE("découpe 3725 secondes en 1 h 2 min 5 s") {
    CHECK(heures(3725) == 1);
    CHECK(minutes(3725) == 2);
    CHECK(secondes_restantes(3725) == 5);
}

TEST_CASE("ne compte aucune heure en dessous de 3600 secondes") {
    CHECK(heures(3599) == 0);
    CHECK(minutes(3599) == 59);
    CHECK(secondes_restantes(3599) == 59);
}

TEST_CASE("tombe juste sur une heure pleine") {
    CHECK(heures(7200) == 2);
    CHECK(minutes(7200) == 0);
    CHECK(secondes_restantes(7200) == 0);
}

TEST_CASE("garde les minutes entre 0 et 59 au-delà d'une heure") {
    CHECK(minutes(86399) == 59);
    CHECK(heures(86399) == 23);
}

TEST_CASE("donne zéro partout pour une durée nulle") {
    CHECK(heures(0) == 0);
    CHECK(minutes(0) == 0);
    CHECK(secondes_restantes(0) == 0);
}
