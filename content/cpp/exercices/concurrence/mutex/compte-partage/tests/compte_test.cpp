#include "compte.hpp"
#include "doctest.h"

#include <atomic>
#include <thread>
#include <vector>

TEST_CASE("démarre avec un solde nul") {
    const Compte compte;
    CHECK(compte.solde() == 0);
}

TEST_CASE("ajoute les dépôts au solde") {
    Compte compte;
    compte.deposer(30);
    compte.deposer(12);
    CHECK(compte.solde() == 42);
}

TEST_CASE("retire quand le solde le permet") {
    Compte compte;
    compte.deposer(50);
    CHECK(compte.retirer(20));
    CHECK(compte.solde() == 30);
    CHECK(compte.retirer(30));
    CHECK(compte.solde() == 0);
}

TEST_CASE("refuse un retrait supérieur au solde, sans rien changer") {
    Compte compte;
    compte.deposer(10);
    CHECK_FALSE(compte.retirer(11));
    CHECK(compte.solde() == 10);
}

TEST_CASE("ne perd aucun dépôt fait par plusieurs threads") {
    Compte compte;
    std::vector<std::thread> threads;
    for (int t = 0; t < 4; ++t) {
        threads.emplace_back([&compte] {
            for (int i = 0; i < 5000; ++i) {
                compte.deposer(1);
            }
        });
    }
    for (auto& thread : threads) {
        thread.join();
    }
    CHECK(compte.solde() == 20000);
}

TEST_CASE("ne laisse jamais le solde devenir négatif") {
    Compte compte;
    compte.deposer(1000);
    std::atomic<int> retraits{0};
    std::vector<std::thread> threads;
    for (int t = 0; t < 4; ++t) {
        threads.emplace_back([&compte, &retraits] {
            for (int i = 0; i < 1000; ++i) {
                if (compte.retirer(1)) {
                    ++retraits;
                }
            }
        });
    }
    for (auto& thread : threads) {
        thread.join();
    }
    CHECK(retraits.load() == 1000);
    CHECK(compte.solde() == 0);
}
