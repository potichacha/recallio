package bibliotheque;

import io.quarkus.panache.common.Sort;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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
        return Livre.findById(id);
    }

    @POST
    @Transactional
    public Livre creer(NouveauLivre nouveau) {
        Livre livre = new Livre();
        livre.titre = nouveau.titre();
        livre.auteur = nouveau.auteur();
        livre.annee = nouveau.annee();
        livre.persist();
        return livre;
    }
}
