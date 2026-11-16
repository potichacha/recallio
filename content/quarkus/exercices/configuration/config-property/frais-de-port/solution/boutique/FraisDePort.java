package boutique;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class FraisDePort {

    @ConfigProperty(name = "boutique.livraison.seuil-gratuit", defaultValue = "50")
    int seuilGratuit;

    @ConfigProperty(name = "boutique.livraison.forfait", defaultValue = "5")
    int forfait;

    @ConfigProperty(name = "boutique.livraison.pays")
    List<String> paysLivres;

    public int calculer(int montantPanier) {
        return montantPanier >= seuilGratuit ? 0 : forfait;
    }

    public boolean livrable(String pays) {
        return paysLivres.contains(pays);
    }
}
