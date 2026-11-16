package boutique;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class FraisDePort {

    // À toi : lis ces valeurs dans la configuration.
    int seuilGratuit = 50;

    int forfait = 5;

    List<String> paysLivres = List.of("FR");

    public int calculer(int montantPanier) {
        return montantPanier >= seuilGratuit ? 0 : forfait;
    }

    public boolean livrable(String pays) {
        return paysLivres.contains(pays);
    }
}
