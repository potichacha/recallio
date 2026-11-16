#pragma once

class CompteBancaire {
public:
    // Ouvre le compte avec ce dépôt ; un dépôt négatif donne un solde nul.
    explicit CompteBancaire(int depot);

    // Le solde, jamais négatif.
    int solde() const;

    // Ajoute le montant au solde, s'il est strictement positif.
    void deposer(int montant);

    // Retire le montant s'il est strictement positif et ne dépasse pas le solde ; dit si le retrait a eu lieu.
    bool retirer(int montant);

private:
    int solde_;
};
