package bibliotheque;

public class LivreIndisponibleException extends RuntimeException {

    public LivreIndisponibleException(String isbn) {
        super("Le livre " + isbn + " est déjà emprunté");
    }
}
