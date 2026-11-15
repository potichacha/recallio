#include "compte.hpp"

void Compte::deposer(int montant) {
    std::lock_guard garde{verrou_};
    solde_ += montant;
}

bool Compte::retirer(int montant) {
    std::lock_guard garde{verrou_};
    if (montant > solde_) {
        return false;
    }
    solde_ -= montant;
    return true;
}

int Compte::solde() const {
    std::lock_guard garde{verrou_};
    return solde_;
}
