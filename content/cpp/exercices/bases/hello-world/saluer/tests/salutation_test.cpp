#include "doctest.h"
#include "salutation.hpp"

TEST_CASE("salue par le prénom") {
    CHECK(saluer("Ada") == "Bonjour, Ada !");
}

TEST_CASE("garde le prénom tel qu'il est écrit") {
    CHECK(saluer("Jean-Luc") == "Bonjour, Jean-Luc !");
}

TEST_CASE("salue sans prénom quand il est vide") {
    CHECK(saluer("") == "Bonjour !");
}
