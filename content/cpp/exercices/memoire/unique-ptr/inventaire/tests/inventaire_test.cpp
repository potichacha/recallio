#include "doctest.h"
#include "inventaire.hpp"

#include <utility>

TEST_CASE("démarre vide") {
    Inventaire inventaire;
    CHECK(inventaire.nombre() == 0);
    CHECK(inventaire.valeur_totale() == 0);
}

TEST_CASE("prend possession des articles ajoutés") {
    Inventaire inventaire;
    auto stylo = std::make_unique<Article>("stylo", 2);
    inventaire.ajouter(std::move(stylo));
    inventaire.ajouter(std::make_unique<Article>("cahier", 5));
    CHECK(stylo == nullptr);
    CHECK(inventaire.nombre() == 2);
}

TEST_CASE("additionne les prix") {
    Inventaire inventaire;
    inventaire.ajouter(std::make_unique<Article>("stylo", 2));
    inventaire.ajouter(std::make_unique<Article>("cahier", 5));
    inventaire.ajouter(std::make_unique<Article>("cartable", 30));
    CHECK(inventaire.valeur_totale() == 37);
}

TEST_CASE("ignore un pointeur nul") {
    Inventaire inventaire;
    inventaire.ajouter(nullptr);
    CHECK(inventaire.nombre() == 0);
}

TEST_CASE("rend le dernier article à l'appelant") {
    Inventaire inventaire;
    inventaire.ajouter(std::make_unique<Article>("stylo", 2));
    inventaire.ajouter(std::make_unique<Article>("cahier", 5));

    std::unique_ptr<Article> rendu = inventaire.retirer_dernier();

    REQUIRE(rendu != nullptr);
    CHECK(rendu->nom == "cahier");
    CHECK(rendu->prix == 5);
    CHECK(inventaire.nombre() == 1);
    CHECK(inventaire.valeur_totale() == 2);
}

TEST_CASE("rend un pointeur nul quand l'inventaire est vide") {
    Inventaire inventaire;
    CHECK(inventaire.retirer_dernier() == nullptr);
    CHECK(inventaire.nombre() == 0);
}
