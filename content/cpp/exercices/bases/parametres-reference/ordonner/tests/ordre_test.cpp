#include "doctest.h"
#include "ordre.hpp"

TEST_CASE("échange les valeurs de deux variables") {
    int x = 1;
    int y = 2;
    echanger(x, y);
    CHECK(x == 2);
    CHECK(y == 1);
}

TEST_CASE("échanger deux fois redonne les valeurs de départ") {
    int x = 7;
    int y = -3;
    echanger(x, y);
    echanger(x, y);
    CHECK(x == 7);
    CHECK(y == -3);
}

TEST_CASE("range dans l'ordre croissant deux valeurs inversées") {
    int x = 9;
    int y = 4;
    ordonner(x, y);
    CHECK(x == 4);
    CHECK(y == 9);
}

TEST_CASE("laisse en place deux valeurs déjà dans l'ordre") {
    int x = 4;
    int y = 9;
    ordonner(x, y);
    CHECK(x == 4);
    CHECK(y == 9);
}

TEST_CASE("laisse en place deux valeurs égales") {
    int x = 5;
    int y = 5;
    ordonner(x, y);
    CHECK(x == 5);
    CHECK(y == 5);
}
