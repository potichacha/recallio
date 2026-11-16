package commandes;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class CommandeRepository implements PanacheRepository<Commande> {

    public List<Commande> duClient(String client) {
        return list("client", Sort.by("id"), client);
    }

    public long nombreEnAttente() {
        return count("statut", Statut.EN_ATTENTE);
    }
}
