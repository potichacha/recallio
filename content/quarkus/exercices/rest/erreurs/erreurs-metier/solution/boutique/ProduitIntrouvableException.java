package boutique;

public class ProduitIntrouvableException extends RuntimeException {

    public ProduitIntrouvableException(long produitId) {
        super("Produit " + produitId + " introuvable");
    }
}
