package boutique;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.util.List;
import org.jboss.resteasy.reactive.RestPath;

@Path("/produits")
public class ProduitResource {

    private final Catalogue catalogue;

    ProduitResource(Catalogue catalogue) {
        this.catalogue = catalogue;
    }

    @GET
    public List<Produit> lister() {
        return catalogue.tous();
    }

    @GET
    @Path("/{id}")
    public Produit trouver(@RestPath long id) {
        return catalogue.trouver(id).orElse(null);
    }

    @POST
    public Produit creer(NouveauProduit nouveau) {
        return catalogue.ajouter(nouveau);
    }

    // À toi : DELETE /produits/{id}
}
