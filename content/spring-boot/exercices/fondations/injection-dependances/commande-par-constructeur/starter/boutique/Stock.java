package boutique;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class Stock {

    private final Map<String, Integer> quantites = new HashMap<>(Map.of("cafe", 10, "the", 5, "chocolat", 3));

    public int quantite(String produit) {
        return quantites.getOrDefault(produit, 0);
    }

    public void retirer(String produit, int quantite) {
        quantites.merge(produit, -quantite, Integer::sum);
    }
}
