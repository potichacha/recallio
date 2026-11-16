package banque;

public class SoldeInsuffisantException extends RuntimeException {

    public SoldeInsuffisantException(String titulaire, int solde, int montant) {
        super(titulaire + " a " + solde + " et ne peut pas virer " + montant);
    }
}
