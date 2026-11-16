#include "doctest.h"
#include "outils.hpp"

#include <string>
#include <vector>

TEST_CASE("compte les entiers pairs") {
    CHECK(compter_pairs({1, 2, 3, 4, 6}) == 3);
    CHECK(compter_pairs({1, 3, 5}) == 0);
}

TEST_CASE("compte zéro et les négatifs pairs") {
    CHECK(compter_pairs({0, -2, -3}) == 2);
}

TEST_CASE("renvoie zéro pour un vecteur vide") {
    CHECK(compter_pairs({}) == 0);
}

TEST_CASE("trie les mots du plus court au plus long") {
    std::vector<std::string> mots{"fraise", "kiwi", "pomme"};
    trier_par_longueur(mots);
    CHECK(mots == std::vector<std::string>{"kiwi", "pomme", "fraise"});
}

TEST_CASE("départage les mots de même longueur par ordre alphabétique") {
    std::vector<std::string> mots{"noix", "kiwi", "figue", "coing", "ail"};
    trier_par_longueur(mots);
    CHECK(mots == std::vector<std::string>{"ail", "kiwi", "noix", "coing", "figue"});
}

TEST_CASE("double chaque élément en place") {
    std::vector<int> valeurs{1, -2, 0, 5};
    doubler(valeurs);
    CHECK(valeurs == std::vector<int>{2, -4, 0, 10});
}

TEST_CASE("laisse un vecteur vide tel quel") {
    std::vector<int> valeurs;
    doubler(valeurs);
    CHECK(valeurs.empty());
}
