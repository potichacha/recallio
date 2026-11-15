#include "adaptateurs.hpp"
#include "doctest.h"

#include <vector>

TEST_CASE("accepte des symboles bien appariés") {
    CHECK(bien_parenthese("()"));
    CHECK(bien_parenthese("([]{})"));
    CHECK(bien_parenthese("{[()()]}"));
}

TEST_CASE("ignore les autres caractères") {
    CHECK(bien_parenthese("f(a[0], {1, 2})"));
    CHECK(bien_parenthese("bonjour"));
    CHECK(bien_parenthese(""));
}

TEST_CASE("refuse un fermant qui ne correspond pas") {
    CHECK_FALSE(bien_parenthese("(]"));
    CHECK_FALSE(bien_parenthese("([)]"));
}

TEST_CASE("refuse un fermant sans ouvrant") {
    CHECK_FALSE(bien_parenthese(")"));
    CHECK_FALSE(bien_parenthese("())"));
}

TEST_CASE("refuse un ouvrant jamais fermé") {
    CHECK_FALSE(bien_parenthese("("));
    CHECK_FALSE(bien_parenthese("{[]"));
}

TEST_CASE("donne les k plus grandes valeurs, dans l'ordre décroissant") {
    CHECK(plus_grands({3, 9, 1, 5, 7}, 3) == std::vector<int>{9, 7, 5});
    CHECK(plus_grands({4, 4, 2}, 2) == std::vector<int>{4, 4});
}

TEST_CASE("donne toutes les valeurs s'il y en a moins de k") {
    CHECK(plus_grands({2, 8}, 5) == std::vector<int>{8, 2});
    CHECK(plus_grands({}, 3).empty());
}

TEST_CASE("donne un vecteur vide pour k nul") {
    CHECK(plus_grands({1, 2, 3}, 0).empty());
}
