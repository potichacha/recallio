package com.recallio.live.api;

import com.recallio.live.api.LiveDtos.AnswerLiveQuestionRequest;
import com.recallio.live.api.LiveDtos.LiveGameDto;
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
import java.time.Clock;
import java.util.Set;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Une partie live : la lire, la démarrer (l'hôte), répondre aux questions. On la suit en direct sur le WebSocket {@code
 * /api/v1/live/{gameId}} ({@link LiveGameSocket}), qui diffuse chaque changement.
 */
@Path("/api/v1/live-games/{gameId}")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Live", description = "Parties synchronisées en temps réel")
@SecurityRequirement(name = "SecurityScheme")
public class LiveGamesResource {

    private final LiveGameService games;
    private final CurrentUser currentUser;
    private final Clock clock;

    public LiveGamesResource(LiveGameService games, CurrentUser currentUser, Clock clock) {
        this.games = games;
        this.currentUser = currentUser;
        this.clock = clock;
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
        return LiveGameDto.from(games.game(currentUser.get(), gameId), clock.instant());
    }

    @POST
    @Path("/start")
    @Operation(
            operationId = "startLiveGame",
            summary = "Démarre la partie : la première question est posée à tous (hôte seulement)")
    @APIResponse(responseCode = "200", description = "La partie démarrée")
    @APIResponse(
            responseCode = "403",
            description = "L'utilisateur n'est pas l'hôte",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "404",
            description = "Partie inconnue ou fermée, ou d'un groupe dont l'utilisateur n'est pas membre",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "409",
            description = "La partie a déjà démarré",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "422",
            description = "Pas assez de QCM dans le sujet",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public LiveGameDto startLiveGame(@PathParam("gameId") UUID gameId) {
        return LiveGameDto.from(games.start(currentUser.get(), gameId), clock.instant());
    }

    @POST
    @Path("/answers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "answerLiveQuestion",
            summary =
                    "Répond à la question en cours ; seule la première réponse compte, la correction vient à la fin du"
                            + " chrono")
    @APIResponse(responseCode = "200", description = "Réponse enregistrée")
    @APIResponse(
            responseCode = "400",
            description = "Requête invalide",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "404",
            description = "Partie inconnue ou fermée, ou d'un groupe dont l'utilisateur n'est pas membre",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "409",
            description = "La question n'attend plus de réponse, ou l'utilisateur n'est pas connecté à la partie",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public LiveGameDto answerLiveQuestion(
            @PathParam("gameId") UUID gameId, @Valid @NotNull AnswerLiveQuestionRequest request) {
        return LiveGameDto.from(
                games.answer(currentUser.get(), gameId, request.question(), Set.copyOf(request.selectedChoices())),
                clock.instant());
    }
}
