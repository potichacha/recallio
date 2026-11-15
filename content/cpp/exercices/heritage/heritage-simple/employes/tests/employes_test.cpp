#include "doctest.h"
#include "employes.hpp"

#include <string>

namespace {
std::string fiche(const Employe& employe) {
    return employe.nom() + " : " + std::to_string(employe.salaire());
}
}  // namespace

TEST_CASE("garde le nom et le salaire d'un employé") {
    Employe ada{"Ada", 3000};
    CHECK(ada.nom() == "Ada");
    CHECK(ada.salaire() == 3000);
}

TEST_CASE("passe le nom et le salaire d'un manager à la classe de base") {
    Manager grace{"Grace", 4000, 500};
    CHECK(grace.nom() == "Grace");
    CHECK(grace.salaire() == 4000);
}

TEST_CASE("garde la prime du manager") {
    Manager grace{"Grace", 4000, 500};
    CHECK(grace.prime() == 500);
}

TEST_CASE("additionne le salaire et la prime") {
    Manager grace{"Grace", 4000, 500};
    CHECK(grace.remuneration() == 4500);
    Manager alan{"Alan", 3500, 0};
    CHECK(alan.remuneration() == 3500);
}

TEST_CASE("traite un manager comme un employé") {
    Manager grace{"Grace", 4000, 500};
    CHECK(fiche(grace) == "Grace : 4000");
}
