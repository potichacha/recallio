package bibliotheque;

import io.quarkus.panache.common.Sort;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@Path("/livres")
public class LivreResource {

    @GET
    public List<Livre> tous() {
        return Livre.listAll(Sort.by("titre"));
    }

    @GET
    @Path("/{id}")
    public Livre un(@PathParam("id") long id) {
        Livre livre = Livre.findById(id);
        if (livre == null) {
            throw new NotFoundException("Livre " + id + " introuvable");
        }
        return livre;
    }

    @POST
    @Transactional
    public Response creer(@Valid NouveauLivre nouveau) {
        Livre livre = new Livre();
        livre.titre = nouveau.titre();
        livre.auteur = nouveau.auteur();
        livre.annee = nouveau.annee();
        livre.persist();
        return Response.created(URI.create("/livres/" + livre.id)).entity(livre).build();
    }
}
