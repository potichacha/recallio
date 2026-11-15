#include "doctest.h"
#include "recursif.hpp"

TEST_CASE("élève un nombre à une puissance") {
    CHECK(puissance(2, 10) == 1024);
    CHECK(puissance(3, 4) == 81);
    CHECK(puissance(7, 1) == 7);
}

TEST_CASE("donne 1 pour l'exposant zéro") {
    CHECK(puissance(5, 0) == 1);
    CHECK(puissance(0, 0) == 1);
}

TEST_CASE("élève une base négative ou nulle") {
    CHECK(puissance(-2, 3) == -8);
    CHECK(puissance(0, 5) == 0);
}

TEST_CASE("additionne les chiffres d'un nombre") {
    CHECK(somme_chiffres(1234) == 10);
    CHECK(somme_chiffres(909) == 18);
}

TEST_CASE("renvoie un nombre à un seul chiffre tel quel") {
    CHECK(somme_chiffres(0) == 0);
    CHECK(somme_chiffres(7) == 7);
}

TEST_CASE("calcule le plus grand commun diviseur") {
    CHECK(pgcd(48, 18) == 6);
    CHECK(pgcd(18, 48) == 6);
    CHECK(pgcd(17, 5) == 1);
}

TEST_CASE("donne l'autre nombre quand l'un des deux est nul") {
    CHECK(pgcd(9, 0) == 9);
    CHECK(pgcd(0, 9) == 9);
}
