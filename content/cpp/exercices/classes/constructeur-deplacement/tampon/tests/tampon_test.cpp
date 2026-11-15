#include "doctest.h"
#include "tampon.hpp"

#include <utility>

TEST_CASE("copie le contenu dans un tableau à part") {
    Tampon original{3};
    original.ecrire(0, 7);
    original.ecrire(2, 9);

    Tampon copie{original};

    REQUIRE(copie.taille() == 3);
    CHECK(copie.lire(0) == 7);
    CHECK(copie.lire(1) == 0);
    CHECK(copie.lire(2) == 9);
}

TEST_CASE("rend la copie indépendante de l'original") {
    Tampon original{2};
    Tampon copie{original};
    REQUIRE(copie.taille() == 2);

    copie.ecrire(0, 5);

    CHECK(original.lire(0) == 0);
    CHECK(copie.lire(0) == 5);
}

TEST_CASE("affecte par copie, et change de taille") {
    Tampon source{4};
    source.ecrire(3, 8);
    Tampon cible{1};

    cible = source;

    REQUIRE(cible.taille() == 4);
    CHECK(cible.lire(3) == 8);
    cible.ecrire(3, 1);
    CHECK(source.lire(3) == 8);
}

TEST_CASE("supporte l'affectation à soi-même") {
    Tampon tampon{2};
    tampon.ecrire(1, 6);
    Tampon& alias = tampon;

    tampon = alias;

    REQUIRE(tampon.taille() == 2);
    CHECK(tampon.lire(1) == 6);
}

TEST_CASE("prend le tableau de la source en la déplaçant") {
    Tampon source{3};
    source.ecrire(1, 4);

    Tampon cible{std::move(source)};

    REQUIRE(cible.taille() == 3);
    CHECK(cible.lire(1) == 4);
    CHECK(source.taille() == 0);
    CHECK(source.vide());
}

TEST_CASE("affecte par déplacement, et laisse la source vide") {
    Tampon source{2};
    source.ecrire(0, 3);
    Tampon cible{5};

    cible = std::move(source);

    REQUIRE(cible.taille() == 2);
    CHECK(cible.lire(0) == 3);
    CHECK(source.taille() == 0);
    CHECK(source.vide());
}

TEST_CASE("copie un tampon vide") {
    Tampon original{0};
    Tampon copie{original};
    CHECK(copie.taille() == 0);
    CHECK(copie.vide());
}
