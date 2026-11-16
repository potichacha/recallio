package boutique;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.util.List;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestQuery;

@Path("/produits")
public class ProduitResource {

    private final Catalogue catalogue;

    ProduitResource(Catalogue catalogue) {
        this.catalogue = catalogue;
    }

    @GET
    public List<Produit> lister(@RestQuery @DefaultValue("20") int taille) {
        return catalogue.premiers(taille);
    }

    @POST
    @ResponseStatus(201)
    public Produit creer(NouveauProduit nouveau) {
        return catalogue.ajouter(nouveau);
    }
}
