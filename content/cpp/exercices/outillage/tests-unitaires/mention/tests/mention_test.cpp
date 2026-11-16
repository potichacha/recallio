#include "doctest.h"
#include "mention.hpp"

#include <stdexcept>

TEST_CASE("refuse une note sous la moyenne") {
    CHECK(mention(0.0) == "Refusé");
    CHECK(mention(5.5) == "Refusé");
    CHECK(mention(9.99) == "Refusé");
}

TEST_CASE("donne passable de 10 à moins de 12") {
    CHECK(mention(10.0) == "Passable");
    CHECK(mention(11.99) == "Passable");
}

TEST_CASE("donne assez bien de 12 à moins de 14") {
    CHECK(mention(12.0) == "Assez bien");
    CHECK(mention(13.99) == "Assez bien");
}

TEST_CASE("donne bien de 14 à moins de 16") {
    CHECK(mention(14.0) == "Bien");
    CHECK(mention(15.99) == "Bien");
}

TEST_CASE("donne très bien de 16 à 20") {
    CHECK(mention(16.0) == "Très bien");
    CHECK(mention(20.0) == "Très bien");
}

TEST_CASE("lance invalid_argument pour une note négative") {
    CHECK_THROWS_AS(mention(-0.01), std::invalid_argument);
    CHECK_THROWS_AS(mention(-5.0), std::invalid_argument);
}

TEST_CASE("lance invalid_argument pour une note supérieure à 20") {
    CHECK_THROWS_AS(mention(20.01), std::invalid_argument);
    CHECK_THROWS_AS(mention(100.0), std::invalid_argument);
}
