package boutique;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
    public List<Produit> lister(@RestQuery @DefaultValue("20") @Min(1) @Max(100) int taille) {
        return catalogue.premiers(taille);
    }

    @POST
    @ResponseStatus(201)
    public Produit creer(@NotNull @Valid NouveauProduit nouveau) {
        return catalogue.ajouter(nouveau);
    }
}
