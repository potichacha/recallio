package boutique;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class Catalogue {

    private static final Map<String, Integer> PRIX = Map.of("stylo", 2, "cahier", 5, "cartable", 30);

    public int prix(String article) {
        Integer prix = PRIX.get(article);
        if (prix == null) {
            throw new IllegalArgumentException("Article inconnu : " + article);
        }
        return prix;
    }
}
