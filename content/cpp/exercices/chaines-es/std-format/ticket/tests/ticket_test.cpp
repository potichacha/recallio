#include "doctest.h"
#include "ticket.hpp"

#include <string>

TEST_CASE("affiche un prix avec deux décimales") {
    CHECK(prix(12.5) == "12.50 EUR");
    CHECK(prix(3.0) == "3.00 EUR");
}

TEST_CASE("arrondit un prix à deux décimales") {
    CHECK(prix(0.126) == "0.13 EUR");
    CHECK(prix(1234.561) == "1234.56 EUR");
}

TEST_CASE("complète le numéro par des zéros") {
    CHECK(numero(42) == "#000042");
    CHECK(numero(7) == "#000007");
}

TEST_CASE("garde un numéro de six chiffres ou plus tel quel") {
    CHECK(numero(123456) == "#123456");
    CHECK(numero(1234567) == "#1234567");
}

TEST_CASE("aligne les trois champs d'une ligne") {
    CHECK(ligne("Cahier", 3, 7.5) == "Cahier       x  3      7.50");
    CHECK(ligne("Stylo", 12, 14.4) == "Stylo        x 12     14.40");
}

TEST_CASE("produit une ligne de 27 caractères") {
    CHECK(ligne("Gomme", 1, 0.8).size() == 27);
    CHECK(ligne("Classeur A4", 100, 1250.0) == "Classeur A4  x100   1250.00");
}
