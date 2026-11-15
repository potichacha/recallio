#include "doctest.h"
#include "notes.hpp"

#include <cstddef>
#include <optional>
#include <vector>

TEST_CASE("donne l'indice de la première occurrence") {
    CHECK(position({12, 15, 9, 15}, 15) == std::optional<std::size_t>{1});
    CHECK(position({12, 15, 9}, 12) == std::optional<std::size_t>{0});
    CHECK(position({12, 15, 9}, 9) == std::optional<std::size_t>{2});
}

TEST_CASE("renvoie nullopt pour une note absente") {
    CHECK(position({12, 15, 9}, 20) == std::nullopt);
    CHECK(position({}, 10) == std::nullopt);
}

TEST_CASE("compte les notes au moins égales à 10") {
    CHECK(nombre_admis({12, 9, 10, 4, 18}) == 3);
    CHECK(nombre_admis({3, 9}) == 0);
    CHECK(nombre_admis({}) == 0);
}

TEST_CASE("accepte des notes toutes comprises entre 0 et 20") {
    CHECK(tous_valides({0, 10, 20}));
}

TEST_CASE("refuse une note hors de l'intervalle") {
    CHECK_FALSE(tous_valides({12, 21}));
    CHECK_FALSE(tous_valides({-1, 12}));
}

TEST_CASE("tient un vecteur vide pour valide") {
    CHECK(tous_valides({}));
}

TEST_CASE("détecte une note sous la moyenne") {
    CHECK(contient_echec({12, 9, 15}));
    CHECK_FALSE(contient_echec({10, 12, 20}));
    CHECK_FALSE(contient_echec({}));
}
