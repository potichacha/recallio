#include "doctest.h"
#include "outils.hpp"

#include <string>

TEST_CASE("trouve le plus petit et le plus grand de deux entiers") {
    CHECK(minimum(3, 7) == 3);
    CHECK(minimum(7, 3) == 3);
    CHECK(maximum(3, 7) == 7);
    CHECK(maximum(7, 3) == 7);
}

TEST_CASE("fonctionne avec des nombres à virgule") {
    CHECK(minimum(2.5, 1.5) == doctest::Approx(1.5));
    CHECK(maximum(2.5, 1.5) == doctest::Approx(2.5));
}

TEST_CASE("fonctionne avec des chaînes") {
    CHECK(minimum(std::string{"poire"}, std::string{"pomme"}) == "poire");
    CHECK(maximum(std::string{"poire"}, std::string{"pomme"}) == "pomme");
}

TEST_CASE("renvoie la valeur commune quand les deux sont égales") {
    CHECK(minimum(4, 4) == 4);
    CHECK(maximum(4, 4) == 4);
}

TEST_CASE("ramène une valeur trop grande ou trop petite dans l'intervalle") {
    CHECK(borner(15, 0, 10) == 10);
    CHECK(borner(-3, 0, 10) == 0);
}

TEST_CASE("laisse une valeur déjà dans l'intervalle, bornes comprises") {
    CHECK(borner(5, 0, 10) == 5);
    CHECK(borner(0, 0, 10) == 0);
    CHECK(borner(10, 0, 10) == 10);
}

TEST_CASE("borne aussi des nombres à virgule") {
    CHECK(borner(1.5, 0.0, 1.0) == doctest::Approx(1.0));
}
