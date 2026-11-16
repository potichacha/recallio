package boutique;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class Remises {

    @ConfigProperty(name = "boutique.remise-fidelite", defaultValue = "10")
    int pourcentage;

    public int appliquer(int total, boolean clientFidele) {
        return clientFidele ? total - total * pourcentage / 100 : total;
    }
}
