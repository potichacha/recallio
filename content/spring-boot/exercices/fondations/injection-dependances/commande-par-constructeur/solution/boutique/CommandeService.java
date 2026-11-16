package boutique;

import org.springframework.stereotype.Service;

@Service
public class CommandeService {

    private final Stock stock;
    private final Journal journal;

    public CommandeService(Stock stock, Journal journal) {
        this.stock = stock;
        this.journal = journal;
    }

    public void commander(String produit, int quantite) {
        if (stock.quantite(produit) < quantite) {
            throw new IllegalArgumentException("Stock insuffisant pour " + produit);
        }
        stock.retirer(produit, quantite);
        journal.noter("commande de " + quantite + " " + produit);
    }
}
