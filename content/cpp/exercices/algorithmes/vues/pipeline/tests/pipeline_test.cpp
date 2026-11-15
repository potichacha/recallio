#include "doctest.h"
#include "pipeline.hpp"

#include <string>
#include <vector>

TEST_CASE("donne le carré des nombres pairs") {
    CHECK(carres_des_pairs({1, 2, 3, 4, 5, 6}) == std::vector<int>{4, 16, 36});
    CHECK(carres_des_pairs({-2, 0, 7}) == std::vector<int>{4, 0});
}

TEST_CASE("donne un vecteur vide sans nombre pair") {
    CHECK(carres_des_pairs({1, 3, 5}).empty());
    CHECK(carres_des_pairs({}).empty());
}

TEST_CASE("donne les premiers mots assez longs") {
    const std::vector<std::string> mots{"kiwi", "fraise", "noix", "abricot", "cassis"};
    CHECK(premiers_longs(mots, 6, 2) == std::vector<std::string>{"fraise", "abricot"});
    CHECK(premiers_longs(mots, 4, 3) == std::vector<std::string>{"kiwi", "fraise", "noix"});
}

TEST_CASE("donne tous les mots assez longs s'il y en a moins de n") {
    const std::vector<std::string> mots{"kiwi", "fraise", "noix"};
    CHECK(premiers_longs(mots, 5, 4) == std::vector<std::string>{"fraise"});
    CHECK(premiers_longs(mots, 10, 2).empty());
}

TEST_CASE("donne les premiers multiples") {
    CHECK(multiples(3, 4) == std::vector<int>{3, 6, 9, 12});
    CHECK(multiples(1, 3) == std::vector<int>{1, 2, 3});
}

TEST_CASE("donne un vecteur vide pour zéro multiple") {
    CHECK(multiples(7, 0).empty());
}
