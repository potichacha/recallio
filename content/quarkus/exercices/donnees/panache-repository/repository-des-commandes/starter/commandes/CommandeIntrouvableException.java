package commandes;

public class CommandeIntrouvableException extends RuntimeException {

    public CommandeIntrouvableException(Long id) {
        super("Aucune commande " + id);
    }
}
