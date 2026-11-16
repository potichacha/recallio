package banque;

public class CompteIntrouvableException extends RuntimeException {

    public CompteIntrouvableException(String numero) {
        super("Compte " + numero + " introuvable");
    }
}
