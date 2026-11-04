public class Compte {

    private double solde;

    public Compte(double solde) {
        this.solde = solde;
    }

    public void retirer(double montant) {
        if (montant > solde) {
            throw new SoldeInsuffisant(solde, montant);
        }
        solde -= montant;
    }

    public double solde() {
        return solde;
    }
}
