package banque;

public class SoldeInsuffisantException extends RuntimeException {

    public SoldeInsuffisantException(String numero) {
        super("Solde insuffisant sur le compte " + numero);
    }
}
