#include "doctest.h"
#include "multiples.hpp"

TEST_CASE("additionne 3, 5, 6 et 9 en dessous de 10") {
    CHECK(somme_des_multiples(10) == 23);
}

TEST_CASE("exclut la limite elle-même") {
    CHECK(somme_des_multiples(5) == 3);
    CHECK(somme_des_multiples(6) == 8);
}

TEST_CASE("ne compte qu'une fois un multiple de 3 et de 5") {
    CHECK(somme_des_multiples(16) == 60);
}

TEST_CASE("renvoie zéro quand il n'y a aucun multiple") {
    CHECK(somme_des_multiples(3) == 0);
    CHECK(somme_des_multiples(0) == 0);
    CHECK(somme_des_multiples(-5) == 0);
}

TEST_CASE("tient jusqu'à mille") {
    CHECK(somme_des_multiples(1000) == 233168);
}
