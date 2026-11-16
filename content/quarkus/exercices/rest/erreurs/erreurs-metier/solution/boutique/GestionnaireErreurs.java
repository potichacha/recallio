package boutique;

import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/** Traduit les exceptions métier en réponses HTTP, pour toute l'application. */
public class GestionnaireErreurs {

    @ServerExceptionMapper
    public RestResponse<Erreur> produitIntrouvable(ProduitIntrouvableException e) {
        return RestResponse.status(Response.Status.NOT_FOUND, new Erreur("PRODUIT_INTROUVABLE", e.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<Erreur> stockInsuffisant(StockInsuffisantException e) {
        return RestResponse.status(Response.Status.CONFLICT, new Erreur("STOCK_INSUFFISANT", e.getMessage()));
    }
}
