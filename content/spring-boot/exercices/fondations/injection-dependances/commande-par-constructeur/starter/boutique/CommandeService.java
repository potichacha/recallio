package boutique;

import org.springframework.stereotype.Service;

@Service
public class CommandeService {

    // Chaque service fabrique ses propres copies : les beans du conteneur ne voient rien passer.
    private final Stock stock = new Stock();
    private final Journal journal = new Journal();

    public void commander(String produit, int quantite) {
        if (stock.quantite(produit) < quantite) {
            throw new IllegalArgumentException("Stock insuffisant pour " + produit);
        }
        stock.retirer(produit, quantite);
        journal.noter("commande de " + quantite + " " + produit);
    }
}
