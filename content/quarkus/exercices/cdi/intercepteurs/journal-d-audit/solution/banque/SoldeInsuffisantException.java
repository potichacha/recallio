package banque;

public class SoldeInsuffisantException extends RuntimeException {

    public SoldeInsuffisantException(int solde, int montant) {
        super("Solde insuffisant : " + solde + " pour un retrait de " + montant);
    }
}
