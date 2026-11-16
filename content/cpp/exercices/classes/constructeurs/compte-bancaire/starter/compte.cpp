#include "compte.hpp"

CompteBancaire::CompteBancaire(int depot) : solde_{0} {
    // À toi : le dépôt initial, s'il n'est pas négatif.
}

int CompteBancaire::solde() const {
    return solde_;
}

void CompteBancaire::deposer(int montant) {
    // À toi : ajoute le montant, s'il est strictement positif.
}

bool CompteBancaire::retirer(int montant) {
    // À toi : retire le montant s'il est permis, et dis si le retrait a eu lieu.
    return false;
}
