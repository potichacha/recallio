#include "employes.hpp"

#include <utility>

Employe::Employe(std::string nom, int salaire) : nom_{std::move(nom)}, salaire_{salaire} {
}

const std::string& Employe::nom() const {
    return nom_;
}

int Employe::salaire() const {
    return salaire_;
}

Manager::Manager(std::string nom, int salaire, int prime) : Employe{std::move(nom), salaire}, prime_{prime} {
}

int Manager::prime() const {
    return prime_;
}

int Manager::remuneration() const {
    return salaire() + prime_;
}
