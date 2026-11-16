package boutique;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/produits")
public class ProduitResource {

    public record NouveauProduit(@NotBlank String nom, @Min(0) int prix) {}

    @GET
    public List<Produit> tous() {
        return Produit.listAll();
    }

    @POST
    @Transactional
    public Response creer(@Valid NouveauProduit nouveau) {
        Produit produit = new Produit();
        produit.nom = nouveau.nom();
        produit.prix = nouveau.prix();
        produit.persist();
        return Response.status(Response.Status.CREATED).entity(produit).build();
    }
}
