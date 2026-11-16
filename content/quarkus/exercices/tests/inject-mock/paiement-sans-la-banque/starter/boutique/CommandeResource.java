package boutique;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("/commandes")
public class CommandeResource {

    public record NouvelleCommande(String carte, int montant) {}

    @Inject
    Banque banque;

    @POST
    @Transactional
    public Response creer(NouvelleCommande nouvelle) {
        Commande commande = new Commande();
        commande.carte = nouvelle.carte();
        commande.montant = nouvelle.montant();
        commande.persist();
        return Response.status(Response.Status.CREATED).entity(commande).build();
    }

    @POST
    @Path("/{id}/paiement")
    @Transactional
    public Response payer(@PathParam("id") long id) {
        Commande commande = Commande.findById(id);
        commande.transaction = banque.debiter(commande.carte, commande.montant);
        commande.statut = Commande.Statut.PAYEE;
        return Response.ok(commande).build();
    }
}
