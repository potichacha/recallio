package boutique;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class Caisse {

    private final Catalogue catalogue;
    private final Remises remises;

    Caisse(Catalogue catalogue, Remises remises) {
        this.catalogue = catalogue;
        this.remises = remises;
    }

    public int total(List<String> articles, boolean clientFidele) {
        int total = 0;
        for (String article : articles) {
            total += catalogue.prix(article);
        }
        return remises.appliquer(total, clientFidele);
    }
}
