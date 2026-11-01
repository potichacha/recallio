package com.recallio.group.api;

import com.recallio.group.api.ChallengeDtos.ChallengeAnswerRequest;
import com.recallio.group.api.ChallengeDtos.ChallengeAnswerResultDto;
import com.recallio.group.api.ChallengeDtos.ChallengeDto;
import com.recallio.group.api.ChallengeDtos.ChallengeStepDto;
import com.recallio.group.application.ChallengePlayService;
import com.recallio.group.application.ChallengeService;
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
import java.util.Set;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Un défi : le consulter avec son classement, et y jouer carte par carte (ADR 0020). Un défi d'un groupe dont
 * l'utilisateur n'est pas membre répond 404.
 */
@Path("/api/v1/challenges/{challengeId}")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Groupes", description = "Groupes de révision et leurs membres")
@SecurityRequirement(name = "SecurityScheme")
public class ChallengesResource {

    private final ChallengeService challenges;
    private final ChallengePlayService play;
    private final CurrentUser currentUser;

    public ChallengesResource(ChallengeService challenges, ChallengePlayService play, CurrentUser currentUser) {
        this.challenges = challenges;
        this.play = play;
        this.currentUser = currentUser;
    }

    @GET
    @Operation(operationId = "getChallenge", summary = "Le défi, la partie de l'utilisateur et le classement")
    @APIResponse(responseCode = "200", description = "Le défi")
    @APIResponse(
            responseCode = "404",
            description = "Défi inconnu, ou d'un groupe dont l'utilisateur n'est pas membre",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public ChallengeDto getChallenge(@PathParam("challengeId") UUID challengeId) {
        return ChallengeDto.from(challenges.detail(currentUser.get(), challengeId));
    }

    @POST
    @Path("/next")
    @Operation(
            operationId = "nextChallengeCard",
            summary = "La carte à jouer : celle en cours si son délai court encore, sinon la suivante (qui démarre son"
                    + " chrono)")
    @APIResponse(responseCode = "200", description = "La carte, ou la fin de la partie")
    @APIResponse(
            responseCode = "404",
            description = "Défi inconnu, ou d'un groupe dont l'utilisateur n'est pas membre",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "409",
            description = "Le défi est terminé",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public ChallengeStepDto nextChallengeCard(@PathParam("challengeId") UUID challengeId) {
        return ChallengeStepDto.from(play.next(currentUser.get(), challengeId));
    }

    @POST
    @Path("/answers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "answerChallengeCard",
            summary = "Répond à la carte présentée ; renvoyer la même réponse rend la correction déjà enregistrée")
    @APIResponse(responseCode = "200", description = "La correction")
    @APIResponse(
            responseCode = "400",
            description = "Requête invalide",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "404",
            description = "Défi inconnu, ou d'un groupe dont l'utilisateur n'est pas membre",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "409",
            description = "Cette carte n'a pas été présentée à l'utilisateur",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public ChallengeAnswerResultDto answerChallengeCard(
            @PathParam("challengeId") UUID challengeId, @Valid @NotNull ChallengeAnswerRequest request) {
        return ChallengeAnswerResultDto.from(
                play.answer(currentUser.get(), challengeId, request.position(), Set.copyOf(request.selectedChoices())));
    }
}
