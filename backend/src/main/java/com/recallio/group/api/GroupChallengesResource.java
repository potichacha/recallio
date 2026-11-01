package com.recallio.group.api;

import com.recallio.group.api.ChallengeDtos.ChallengeSummaryDto;
import com.recallio.group.api.ChallengeDtos.CreateChallengeRequest;
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
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** Les défis d'un groupe : les voir, en lancer un (ADR 0020). */
@Path("/api/v1/groups/{groupId}/challenges")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Groupes", description = "Groupes de révision et leurs membres")
@SecurityRequirement(name = "SecurityScheme")
public class GroupChallengesResource {

    private final ChallengeService challenges;
    private final CurrentUser currentUser;

    public GroupChallengesResource(ChallengeService challenges, CurrentUser currentUser) {
        this.challenges = challenges;
        this.currentUser = currentUser;
    }

    @GET
    @Operation(operationId = "listGroupChallenges", summary = "Les défis récents du groupe, le plus récent d'abord")
    @APIResponse(responseCode = "200", description = "Les défis")
    @APIResponse(
            responseCode = "404",
            description = "Groupe inconnu, ou dont l'utilisateur n'est pas membre",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public List<ChallengeSummaryDto> listGroupChallenges(@PathParam("groupId") UUID groupId) {
        return challenges.ofGroup(currentUser.get(), groupId).stream()
                .map(ChallengeSummaryDto::from)
                .toList();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "createChallenge",
            summary = "Lance un défi : une série de QCM tirée au hasard dans les sujets du groupe (créateur seulement)")
    @APIResponse(
            responseCode = "201",
            description = "Le défi lancé",
            content = @Content(schema = @Schema(implementation = ChallengeSummaryDto.class)))
    @APIResponse(
            responseCode = "400",
            description = "Requête invalide",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "403",
            description = "L'utilisateur n'est pas le créateur",
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
            responseCode = "422",
            description = "Pas assez de QCM dans les sujets du groupe",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public Response createChallenge(
            @PathParam("groupId") UUID groupId, @Valid @NotNull CreateChallengeRequest request) {
        ChallengeSummaryDto created = ChallengeSummaryDto.from(challenges.create(
                currentUser.get(), groupId, request.title(), request.cardCount(), request.secondsPerCard()));
        return Response.created(URI.create("/api/v1/challenges/" + created.id()))
                .entity(created)
                .build();
    }
}
