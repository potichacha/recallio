import java.util.HashMap;
import java.util.Map;

public final class Stock {

    private final Map<String, Integer> quantites = new HashMap<>();
    private long total;

    public synchronized void ajouter(String produit, int quantite) {
        if (quantite <= 0) {
            throw new IllegalArgumentException("quantité non positive : " + quantite);
        }
        quantites.merge(produit, quantite, Integer::sum);
        total += quantite;
    }

    public synchronized boolean retirer(String produit, int quantite) {
        int disponible = quantites.getOrDefault(produit, 0);
        if (quantite <= 0 || disponible < quantite) {
            return false;
        }
        quantites.put(produit, disponible - quantite);
        total -= quantite;
        return true;
    }

    public synchronized int quantite(String produit) {
        return quantites.getOrDefault(produit, 0);
    }

    public synchronized long total() {
        return total;
    }
}
