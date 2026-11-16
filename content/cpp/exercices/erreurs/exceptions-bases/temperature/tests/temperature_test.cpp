#include "doctest.h"
#include "temperature.hpp"

#include <stdexcept>
#include <vector>

TEST_CASE("convertit des degrés Celsius en kelvins") {
    CHECK(convertir(0.0) == doctest::Approx(273.15));
    CHECK(convertir(26.85) == doctest::Approx(300.0));
}

TEST_CASE("accepte le zéro absolu") {
    CHECK(convertir(-273.15) == doctest::Approx(0.0));
}

TEST_CASE("lance invalid_argument sous le zéro absolu") {
    CHECK_THROWS_AS(convertir(-300.0), std::invalid_argument);
    CHECK_THROWS_AS(convertir(-273.16), std::invalid_argument);
}

TEST_CASE("calcule la moyenne des températures") {
    CHECK(moyenne({10.0, 20.0, 30.0}) == doctest::Approx(20.0));
    CHECK(moyenne({-5.0}) == doctest::Approx(-5.0));
}

TEST_CASE("lance invalid_argument pour un vecteur vide") {
    CHECK_THROWS_AS(moyenne({}), std::invalid_argument);
}

TEST_CASE("renvoie la conversion quand elle réussit") {
    CHECK(convertir_ou(0.0, -1.0) == doctest::Approx(273.15));
}

TEST_CASE("renvoie la valeur par défaut quand la conversion échoue") {
    CHECK(convertir_ou(-500.0, -1.0) == doctest::Approx(-1.0));
    CHECK_NOTHROW(convertir_ou(-500.0, 0.0));
}
