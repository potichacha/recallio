package boutique;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class Caisse {

    private final Catalogue catalogue = new Catalogue();
    private final Remises remises = new Remises();

    public int total(List<String> articles, boolean clientFidele) {
        int total = 0;
        for (String article : articles) {
            total += catalogue.prix(article);
        }
        return remises.appliquer(total, clientFidele);
    }
}
