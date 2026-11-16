package boutique;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.net.URI;
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
        return catalogue.trouver(id).orElseThrow(NotFoundException::new);
    }

    @POST
    public Response creer(NouveauProduit nouveau) {
        Produit cree = catalogue.ajouter(nouveau);
        return Response.created(URI.create("/produits/" + cree.id())).entity(cree).build();
    }

    @DELETE
    @Path("/{id}")
    public void supprimer(@RestPath long id) {
        if (!catalogue.retirer(id)) {
            throw new NotFoundException();
        }
    }
}
