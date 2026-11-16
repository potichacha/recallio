package boutique;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.jboss.resteasy.reactive.ResponseStatus;

@Path("/commandes")
public class CommandeResource {

    public record NouvelleCommande(long produitId, int quantite) {}

    private final Stock stock;

    CommandeResource(Stock stock) {
        this.stock = stock;
    }

    @POST
    @ResponseStatus(201)
    public NouvelleCommande passer(NouvelleCommande commande) {
        stock.retirer(commande.produitId(), commande.quantite());
        return commande;
    }
}
