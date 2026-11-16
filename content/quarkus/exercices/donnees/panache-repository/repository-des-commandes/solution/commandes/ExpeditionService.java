package commandes;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ExpeditionService {

    private final CommandeRepository commandes;

    public ExpeditionService(CommandeRepository commandes) {
        this.commandes = commandes;
    }

    @Transactional
    public void expedier(Long id) {
        Commande commande = commandes.findByIdOptional(id).orElseThrow(() -> new CommandeIntrouvableException(id));
        commande.statut = Statut.EXPEDIEE;
    }
}
