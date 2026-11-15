#include "bits.hpp"
#include "doctest.h"

TEST_CASE("active un bit à zéro") {
    CHECK(activer(0u, 0) == 1u);
    CHECK(activer(0u, 3) == 8u);
    CHECK(activer(0b0101u, 1) == 0b0111u);
}

TEST_CASE("laisse inchangé un bit déjà actif") {
    CHECK(activer(0b0110u, 2) == 0b0110u);
}

TEST_CASE("active le bit de rang 31") {
    CHECK(activer(0u, 31) == 0x80000000u);
}

TEST_CASE("désactive un bit à un") {
    CHECK(desactiver(0b0111u, 1) == 0b0101u);
    CHECK(desactiver(1u, 0) == 0u);
}

TEST_CASE("laisse inchangé un bit déjà à zéro") {
    CHECK(desactiver(0b0101u, 1) == 0b0101u);
}

TEST_CASE("dit si un bit est actif") {
    CHECK(est_actif(0b0100u, 2));
    CHECK_FALSE(est_actif(0b0100u, 1));
    CHECK(est_actif(0x80000000u, 31));
}

TEST_CASE("compte les bits à un") {
    CHECK(compter(0u) == 0);
    CHECK(compter(0b1011u) == 3);
    CHECK(compter(0xFFFFFFFFu) == 32);
}
