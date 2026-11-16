package banque;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Compte {

    private int solde = 100;

    @Audite
    public int deposer(int montant) {
        solde += montant;
        return solde;
    }

    @Audite
    public int retirer(int montant) {
        if (montant > solde) {
            throw new SoldeInsuffisantException(solde, montant);
        }
        solde -= montant;
        return solde;
    }

    public int solde() {
        return solde;
    }
}
