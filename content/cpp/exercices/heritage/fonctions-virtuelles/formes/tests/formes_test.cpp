#include "doctest.h"
#include "formes.hpp"

TEST_CASE("calcule l'aire d'un rectangle et d'un carré") {
    CHECK(Rectangle{2, 5}.aire() == 10);
    CHECK(Carre{3}.aire() == 9);
}

TEST_CASE("nomme chaque forme") {
    CHECK(Rectangle{2, 5}.nom() == "rectangle");
    CHECK(Carre{3}.nom() == "carré");
}

TEST_CASE("appelle la bonne version à travers l'interface") {
    Carre carre{4};
    const Forme& forme = carre;
    CHECK(forme.aire() == 16);
    CHECK(forme.nom() == "carré");
}

TEST_CASE("additionne les aires de formes différentes") {
    std::vector<std::unique_ptr<Forme>> formes;
    formes.push_back(std::make_unique<Carre>(3));
    formes.push_back(std::make_unique<Rectangle>(2, 5));
    formes.push_back(std::make_unique<Carre>(1));
    CHECK(aire_totale(formes) == 20);
}

TEST_CASE("donne une aire totale nulle pour un vecteur vide") {
    std::vector<std::unique_ptr<Forme>> formes;
    CHECK(aire_totale(formes) == 0);
}

TEST_CASE("trouve la forme la plus grande") {
    std::vector<std::unique_ptr<Forme>> formes;
    formes.push_back(std::make_unique<Carre>(3));
    formes.push_back(std::make_unique<Rectangle>(2, 5));
    CHECK(plus_grande(formes) == "rectangle");
}

TEST_CASE("garde la première forme en cas d'égalité") {
    std::vector<std::unique_ptr<Forme>> formes;
    formes.push_back(std::make_unique<Carre>(2));
    formes.push_back(std::make_unique<Rectangle>(1, 4));
    CHECK(plus_grande(formes) == "carré");
}

TEST_CASE("renvoie une chaîne vide pour un vecteur vide") {
    std::vector<std::unique_ptr<Forme>> formes;
    CHECK(plus_grande(formes).empty());
}
