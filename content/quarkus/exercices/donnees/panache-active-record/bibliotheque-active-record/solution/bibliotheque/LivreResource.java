package bibliotheque;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("/livres")
public class LivreResource {

    public record NouveauLivre(String titre, String auteur) {}

    @POST
    @Transactional
    public Response ajouter(NouveauLivre nouveau) {
        Livre livre = new Livre();
        livre.titre = nouveau.titre();
        livre.auteur = nouveau.auteur();
        livre.persist();
        return Response.status(Response.Status.CREATED).entity(livre).build();
    }

    @GET
    @Path("/{id}")
    public Livre lire(@PathParam("id") Long id) {
        return Livre.<Livre>findByIdOptional(id).orElseThrow(NotFoundException::new);
    }

    @POST
    @Path("/{id}/emprunt")
    @Transactional
    public Response emprunter(@PathParam("id") Long id) {
        Livre livre = Livre.<Livre>findByIdOptional(id).orElseThrow(NotFoundException::new);
        try {
            livre.emprunter();
        } catch (LivreIndisponibleException e) {
            return Response.status(Response.Status.CONFLICT).build();
        }
        return Response.noContent().build();
    }
}
