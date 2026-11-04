public class SoldeInsuffisant extends RuntimeException {

    private final double solde;
    private final double montant;

    public SoldeInsuffisant(double solde, double montant) {
        super("solde " + solde + " insuffisant pour un retrait de " + montant);
        this.solde = solde;
        this.montant = montant;
    }

    public double solde() {
        return solde;
    }

    public double montant() {
        return montant;
    }

    public double manque() {
        return montant - solde;
    }
}
