package com.recallio.live.api;

import com.recallio.live.api.LiveDtos.LiveGameDto;
import com.recallio.live.api.LiveDtos.OpenLiveGameRequest;
import com.recallio.live.application.LiveGameService;
import com.recallio.platform.api.ProblemDetail;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** La partie live d'un groupe : la trouver, en lancer une (ADR 0021). */
@Path("/api/v1/groups/{groupId}/live-games")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Live", description = "Parties synchronisées en temps réel")
@SecurityRequirement(name = "SecurityScheme")
public class GroupLiveGamesResource {

    private final LiveGameService games;
    private final CurrentUser currentUser;
    private final Clock clock;

    public GroupLiveGamesResource(LiveGameService games, CurrentUser currentUser, Clock clock) {
        this.games = games;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @GET
    @Operation(operationId = "listGroupLiveGames", summary = "La partie ouverte du groupe : une au plus, ou aucune")
    @APIResponse(responseCode = "200", description = "Zéro ou une partie")
    @APIResponse(
            responseCode = "404",
            description = "Groupe inconnu, ou dont l'utilisateur n'est pas membre",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public List<LiveGameDto> listGroupLiveGames(@PathParam("groupId") UUID groupId) {
        return games
                .openGameOf(currentUser.get(), groupId)
                .map(game -> LiveGameDto.from(game, clock.instant()))
                .stream()
                .toList();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "openLiveGame",
            summary = "Ouvre une partie live sur un sujet du groupe, dont l'utilisateur est l'hôte")
    @APIResponse(
            responseCode = "201",
            description = "La partie ouverte, en salle d'attente",
            content = @Content(schema = @Schema(implementation = LiveGameDto.class)))
    @APIResponse(
            responseCode = "400",
            description = "Requête invalide",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "404",
            description = "Groupe inconnu, ou dont l'utilisateur n'est pas membre",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "409",
            description = "Le groupe a déjà une partie ouverte",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "422",
            description = "Le sujet n'est pas l'un de ceux du groupe",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public Response openLiveGame(@PathParam("groupId") UUID groupId, @Valid @NotNull OpenLiveGameRequest request) {
        LiveGameDto game = LiveGameDto.from(games.open(currentUser.get(), groupId, request.subject()), clock.instant());
        return Response.created(URI.create("/api/v1/live-games/" + game.id()))
                .entity(game)
                .build();
    }
}
