package commandes;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ExpeditionService {

    private final CommandeRepository commandes;

    public ExpeditionService(CommandeRepository commandes) {
        this.commandes = commandes;
    }

    public void expedier(Long id) {
    }
}
