#include "doctest.h"
#include "notes.hpp"

TEST_CASE("calcule la moyenne de plusieurs notes") {
    CHECK(moyenne({12, 15, 9}) == doctest::Approx(12.0));
    CHECK(moyenne({20}) == doctest::Approx(20.0));
}

TEST_CASE("garde la partie décimale de la moyenne") {
    CHECK(moyenne({15, 16}) == doctest::Approx(15.5));
    CHECK(moyenne({12, 15, 10}) == doctest::Approx(12.3333).epsilon(0.001));
}

TEST_CASE("renvoie zéro pour un relevé vide") {
    CHECK(moyenne({}) == doctest::Approx(0.0));
}

TEST_CASE("garde les notes d'au moins 10, dans le même ordre") {
    CHECK(garder_reussies({12, 7, 15, 9}) == std::vector<int>{12, 15});
    CHECK(garder_reussies({10, 9, 10}) == std::vector<int>{10, 10});
}

TEST_CASE("renvoie un vecteur vide quand aucune note ne passe") {
    CHECK(garder_reussies({3, 8}).empty());
    CHECK(garder_reussies({}).empty());
}

TEST_CASE("ne modifie pas le relevé reçu") {
    std::vector<int> notes{12, 7};
    garder_reussies(notes);
    CHECK(notes == std::vector<int>{12, 7});
}
