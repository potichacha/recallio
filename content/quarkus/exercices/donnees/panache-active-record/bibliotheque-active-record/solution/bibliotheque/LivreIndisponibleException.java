package bibliotheque;

public class LivreIndisponibleException extends RuntimeException {

    public LivreIndisponibleException(String titre) {
        super("« " + titre + " » est déjà emprunté");
    }
}
