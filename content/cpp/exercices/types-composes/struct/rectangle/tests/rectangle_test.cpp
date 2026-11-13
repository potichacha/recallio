#include "doctest.h"
#include "rectangle.hpp"

TEST_CASE("calcule l'aire d'un rectangle") {
    CHECK(aire(Rectangle{3, 4}) == 12);
    CHECK(aire(Rectangle{5, 5}) == 25);
}

TEST_CASE("calcule le périmètre d'un rectangle") {
    CHECK(perimetre(Rectangle{3, 4}) == 14);
    CHECK(perimetre(Rectangle{1, 10}) == 22);
}

TEST_CASE("donne une aire nulle à un rectangle sans largeur") {
    CHECK(aire(Rectangle{0, 7}) == 0);
    CHECK(perimetre(Rectangle{0, 7}) == 14);
}

TEST_CASE("agrandit les deux dimensions") {
    Rectangle grand = agrandir(Rectangle{3, 4}, 2);
    CHECK(grand.largeur == 6);
    CHECK(grand.hauteur == 8);
}

TEST_CASE("ne modifie pas le rectangle reçu") {
    Rectangle petit{3, 4};
    agrandir(petit, 5);
    CHECK(petit.largeur == 3);
    CHECK(petit.hauteur == 4);
}

TEST_CASE("multiplie l'aire par le carré du facteur") {
    CHECK(aire(agrandir(Rectangle{2, 3}, 3)) == 54);
}
