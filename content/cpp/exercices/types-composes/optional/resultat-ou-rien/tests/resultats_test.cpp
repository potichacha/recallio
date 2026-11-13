#include "doctest.h"
#include "resultats.hpp"

TEST_CASE("divise deux entiers") {
    CHECK(quotient(7, 2) == std::optional<int>{3});
    CHECK(quotient(-9, 3) == std::optional<int>{-3});
}

TEST_CASE("renvoie un quotient nul comme une valeur") {
    CHECK(quotient(0, 5) == std::optional<int>{0});
}

TEST_CASE("ne renvoie rien pour une division par zéro") {
    CHECK_FALSE(quotient(7, 0).has_value());
    CHECK_FALSE(quotient(0, 0).has_value());
}

TEST_CASE("trouve le premier nombre pair") {
    CHECK(premier_pair({1, 3, 4, 6}) == std::optional<int>{4});
    CHECK(premier_pair({8}) == std::optional<int>{8});
}

TEST_CASE("compte zéro et les négatifs parmi les nombres pairs") {
    CHECK(premier_pair({1, 0, 2}) == std::optional<int>{0});
    CHECK(premier_pair({-3, -4}) == std::optional<int>{-4});
}

TEST_CASE("ne renvoie rien sans nombre pair") {
    CHECK_FALSE(premier_pair({1, 3, 5}).has_value());
    CHECK_FALSE(premier_pair({}).has_value());
}
