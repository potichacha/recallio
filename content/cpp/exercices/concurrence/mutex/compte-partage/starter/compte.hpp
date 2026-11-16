#pragma once

#include <mutex>

class Compte {
public:
    // Ajoute le montant au solde.
    void deposer(int montant);

    // Retire le montant et renvoie true si le solde le permet ; sinon ne change rien et renvoie false.
    bool retirer(int montant);

    // Le solde.
    int solde() const;

private:
    mutable std::mutex verrou_;
    int solde_ = 0;
};
