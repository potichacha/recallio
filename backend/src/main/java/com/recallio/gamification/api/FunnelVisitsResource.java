package com.recallio.gamification.api;

import com.recallio.gamification.application.FunnelRecorder;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * La première étape du parcours mesuré (ADR 0064) : la page publique annonce une visite, une fois par session de
 * navigateur. Publique comme le catalogue, sans corps : rien du visiteur n'est reçu ni gardé.
 */
@Path("/api/v1/funnel/visits")
@Tag(name = "Parcours", description = "Le parcours des visiteurs, mesuré sans donnée personnelle (ADR 0064)")
public class FunnelVisitsResource {

    private final FunnelRecorder funnel;

    public FunnelVisitsResource(FunnelRecorder funnel) {
        this.funnel = funnel;
    }

    @POST
    @Operation(operationId = "recordVisit", summary = "Compte une visite de la page publique, sans compte")
    @APIResponse(responseCode = "204", description = "Visite comptée")
    public void recordVisit() {
        funnel.recordVisit();
    }
}
