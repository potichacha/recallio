#include "compte.hpp"
#include "doctest.h"

TEST_CASE("ouvre le compte avec le dépôt initial") {
    CompteBancaire compte{100};
    CHECK(compte.solde() == 100);
}

TEST_CASE("démarre à zéro pour un dépôt initial négatif") {
    CompteBancaire compte{-50};
    CHECK(compte.solde() == 0);
}

TEST_CASE("ajoute un dépôt strictement positif") {
    CompteBancaire compte{100};
    compte.deposer(40);
    CHECK(compte.solde() == 140);
}

TEST_CASE("ignore un dépôt nul ou négatif") {
    CompteBancaire compte{100};
    compte.deposer(0);
    compte.deposer(-30);
    CHECK(compte.solde() == 100);
}

TEST_CASE("retire un montant que le solde couvre") {
    CompteBancaire compte{100};
    CHECK(compte.retirer(30));
    CHECK(compte.solde() == 70);
    CHECK(compte.retirer(70));
    CHECK(compte.solde() == 0);
}

TEST_CASE("refuse un retrait qui dépasse le solde") {
    CompteBancaire compte{100};
    CHECK_FALSE(compte.retirer(101));
    CHECK(compte.solde() == 100);
}

TEST_CASE("refuse un retrait nul ou négatif") {
    CompteBancaire compte{100};
    CHECK_FALSE(compte.retirer(0));
    CHECK_FALSE(compte.retirer(-20));
    CHECK(compte.solde() == 100);
}

TEST_CASE("lit le solde d'un compte constant") {
    const CompteBancaire compte{25};
    CHECK(compte.solde() == 25);
}
