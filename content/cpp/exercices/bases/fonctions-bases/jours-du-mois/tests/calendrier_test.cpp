#include "calendrier.hpp"
#include "doctest.h"

TEST_CASE("reconnaît une année divisible par 4") {
    CHECK(est_bissextile(2024));
    CHECK_FALSE(est_bissextile(2023));
}

TEST_CASE("exclut les années séculaires, sauf celles divisibles par 400") {
    CHECK_FALSE(est_bissextile(1900));
    CHECK(est_bissextile(2000));
}

TEST_CASE("donne 31 ou 30 jours selon le mois") {
    CHECK(jours_dans_mois(1, 2023) == 31);
    CHECK(jours_dans_mois(4, 2023) == 30);
    CHECK(jours_dans_mois(7, 2023) == 31);
    CHECK(jours_dans_mois(8, 2023) == 31);
    CHECK(jours_dans_mois(11, 2023) == 30);
    CHECK(jours_dans_mois(12, 2023) == 31);
}

TEST_CASE("donne 29 jours à février une année bissextile") {
    CHECK(jours_dans_mois(2, 2023) == 28);
    CHECK(jours_dans_mois(2, 2024) == 29);
    CHECK(jours_dans_mois(2, 1900) == 28);
}

TEST_CASE("renvoie zéro pour un mois qui n'existe pas") {
    CHECK(jours_dans_mois(0, 2024) == 0);
    CHECK(jours_dans_mois(13, 2024) == 0);
}
