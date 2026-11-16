package bibliotheque;

public class LivreIntrouvableException extends RuntimeException {

    public LivreIntrouvableException(Long id) {
        super("Aucun livre d'identifiant " + id);
    }
}
