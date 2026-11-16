package boutique;

public class StockInsuffisantException extends RuntimeException {

    public StockInsuffisantException(String produit, int demande, int disponible) {
        super("Stock insuffisant pour " + produit + " : " + demande + " demandés, " + disponible + " disponibles");
    }
}
