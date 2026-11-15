#include "compte.hpp"

CompteBancaire::CompteBancaire(int depot) : solde_{depot > 0 ? depot : 0} {
}

int CompteBancaire::solde() const {
    return solde_;
}

void CompteBancaire::deposer(int montant) {
    if (montant > 0) {
        solde_ += montant;
    }
}

bool CompteBancaire::retirer(int montant) {
    if (montant <= 0 || montant > solde_) {
        return false;
    }
    solde_ -= montant;
    return true;
}
