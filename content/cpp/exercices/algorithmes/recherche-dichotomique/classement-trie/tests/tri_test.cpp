#include "doctest.h"
#include "tri.hpp"

#include <vector>

TEST_CASE("trouve une valeur présente") {
    CHECK(contient({1, 3, 5, 7}, 5));
    CHECK(contient({1, 3, 5, 7}, 1));
    CHECK(contient({1, 3, 5, 7}, 7));
}

TEST_CASE("ne trouve pas une valeur absente") {
    CHECK_FALSE(contient({1, 3, 5, 7}, 4));
    CHECK_FALSE(contient({}, 4));
}

TEST_CASE("compte les occurrences") {
    CHECK(occurrences({1, 2, 2, 2, 5}, 2) == 3);
    CHECK(occurrences({1, 2, 2, 2, 5}, 5) == 1);
    CHECK(occurrences({1, 2, 2, 2, 5}, 3) == 0);
}

TEST_CASE("insère au milieu, en tête et à la fin") {
    std::vector<int> valeurs{2, 4, 8};
    inserer(valeurs, 6);
    inserer(valeurs, 1);
    inserer(valeurs, 9);
    CHECK(valeurs == std::vector<int>{1, 2, 4, 6, 8, 9});
}

TEST_CASE("insère dans un vecteur vide, et une valeur déjà présente") {
    std::vector<int> valeurs;
    inserer(valeurs, 3);
    inserer(valeurs, 3);
    inserer(valeurs, 1);
    CHECK(valeurs == std::vector<int>{1, 3, 3});
}

TEST_CASE("compte les éléments strictement inférieurs") {
    CHECK(nombre_inferieurs({1, 3, 3, 7}, 3) == 1);
    CHECK(nombre_inferieurs({1, 3, 3, 7}, 4) == 3);
    CHECK(nombre_inferieurs({1, 3, 3, 7}, 0) == 0);
    CHECK(nombre_inferieurs({1, 3, 3, 7}, 100) == 4);
}
