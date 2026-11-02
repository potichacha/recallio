package com.recallio.live.api;

import com.recallio.live.api.LiveDtos.LiveGameDto;
import com.recallio.live.application.LiveGameService;
import com.recallio.platform.api.ProblemDetail;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Une partie live, en lecture. On la suit en direct sur le WebSocket {@code /api/v1/live/{gameId}} ({@link
 * LiveGameSocket}).
 */
@Path("/api/v1/live-games/{gameId}")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Live", description = "Parties synchronisées en temps réel")
@SecurityRequirement(name = "SecurityScheme")
public class LiveGamesResource {

    private final LiveGameService games;
    private final CurrentUser currentUser;

    public LiveGamesResource(LiveGameService games, CurrentUser currentUser) {
        this.games = games;
        this.currentUser = currentUser;
    }

    @GET
    @Operation(operationId = "getLiveGame", summary = "L'état de la partie à cet instant")
    @APIResponse(responseCode = "200", description = "La partie")
    @APIResponse(
            responseCode = "404",
            description = "Partie inconnue ou fermée, ou d'un groupe dont l'utilisateur n'est pas membre",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public LiveGameDto getLiveGame(@PathParam("gameId") UUID gameId) {
        return LiveGameDto.from(games.game(currentUser.get(), gameId));
    }
}
