#include "employes.hpp"

// À toi : range le nom et le salaire.
Employe::Employe(std::string nom, int salaire) : nom_{}, salaire_{0} {
}

const std::string& Employe::nom() const {
    return nom_;
}

int Employe::salaire() const {
    return salaire_;
}

// À toi : passe le nom et le salaire au constructeur d'Employe, et garde la prime.
Manager::Manager(std::string nom, int salaire, int prime) : Employe{"", 0}, prime_{0} {
}

int Manager::prime() const {
    return prime_;
}

int Manager::remuneration() const {
    // À toi : le salaire plus la prime.
    return 0;
}
