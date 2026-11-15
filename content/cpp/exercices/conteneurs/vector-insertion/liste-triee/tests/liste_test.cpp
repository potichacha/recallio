#include "doctest.h"
#include "liste.hpp"

#include <optional>
#include <vector>

TEST_CASE("insère au milieu d'un vecteur trié") {
    std::vector<int> valeurs{1, 3, 7};
    inserer_trie(valeurs, 5);
    CHECK(valeurs == std::vector<int>{1, 3, 5, 7});
}

TEST_CASE("insère en tête et à la fin") {
    std::vector<int> valeurs{4, 6};
    inserer_trie(valeurs, 2);
    inserer_trie(valeurs, 9);
    CHECK(valeurs == std::vector<int>{2, 4, 6, 9});
}

TEST_CASE("insère dans un vecteur vide") {
    std::vector<int> valeurs;
    inserer_trie(valeurs, 8);
    CHECK(valeurs == std::vector<int>{8});
}

TEST_CASE("insère aussi une valeur déjà présente") {
    std::vector<int> valeurs{1, 5, 9};
    inserer_trie(valeurs, 5);
    CHECK(valeurs == std::vector<int>{1, 5, 5, 9});
}

TEST_CASE("retire les négatifs et les compte") {
    std::vector<int> valeurs{3, -1, 4, -5, 0};
    CHECK(retirer_negatifs(valeurs) == 2);
    CHECK(valeurs == std::vector<int>{3, 4, 0});
}

TEST_CASE("retire des négatifs voisins") {
    std::vector<int> valeurs{-1, -2, -3, 6, -4, -5};
    CHECK(retirer_negatifs(valeurs) == 5);
    CHECK(valeurs == std::vector<int>{6});
}

TEST_CASE("ne touche pas à un vecteur sans négatif") {
    std::vector<int> valeurs{0, 1, 2};
    CHECK(retirer_negatifs(valeurs) == 0);
    CHECK(valeurs == std::vector<int>{0, 1, 2});
}

TEST_CASE("retire le dernier élément et le renvoie") {
    std::vector<int> valeurs{1, 2, 3};
    CHECK(retirer_dernier(valeurs) == std::optional<int>{3});
    CHECK(valeurs == std::vector<int>{1, 2});
}

TEST_CASE("renvoie nullopt pour un vecteur vide") {
    std::vector<int> valeurs;
    CHECK(retirer_dernier(valeurs) == std::nullopt);
    CHECK(valeurs.empty());
}
