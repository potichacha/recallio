package com.recallio.review.api;

import com.recallio.platform.api.ProblemDetail;
import com.recallio.review.api.PracticeResource.PracticeAnswerDto;
import com.recallio.review.api.PracticeResource.PracticeCardDto;
import com.recallio.review.api.PracticeResource.PracticeCorrectionDto;
import com.recallio.review.application.MistakesService;
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
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Le carnet d'erreurs (ADR 0057) : les cartes d'un sujet ratées cette semaine, à refaire sans rien enregistrer. Les
 * cartes et leur correction ont la forme de celles de l'entraînement (ADR 0038).
 */
@Path("/api/v1/me/mistakes/{subject}")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Entraînement")
@SecurityRequirement(name = "SecurityScheme")
public class MistakesResource {

    private final MistakesService mistakes;
    private final CurrentUser currentUser;

    public MistakesResource(MistakesService mistakes, CurrentUser currentUser) {
        this.mistakes = mistakes;
        this.currentUser = currentUser;
    }

    @GET
    @Operation(
            operationId = "getMistakes",
            summary = "Les cartes du sujet ratées ces sept derniers jours et pas réussies depuis")
    @APIResponse(responseCode = "200", description = "Les cartes, la plus récemment ratée d'abord, 20 au plus")
    public MistakesDto getMistakes(@PathParam("subject") String subject) {
        return new MistakesDto(mistakes.mistakes(currentUser.get(), subject).stream()
                .map(PracticeCardDto::from)
                .toList());
    }

    @POST
    @Path("/checks")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "checkMistakeAnswer",
            summary = "Corrige la réponse à une carte du carnet d'erreurs, sans l'enregistrer")
    @APIResponse(responseCode = "200", description = "La correction")
    @APIResponse(
            responseCode = "404",
            description = "La carte n'est pas dans le carnet de l'utilisateur pour ce sujet",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "422",
            description = "La carte s'auto-évalue, ou la réponse ne convient pas à son format",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public PracticeCorrectionDto checkMistakeAnswer(
            @PathParam("subject") String subject, @Valid @NotNull PracticeAnswerDto answer) {
        return PracticeCorrectionDto.from(mistakes.check(currentUser.get(), subject, answer.toAnswer()));
    }

    @Schema(name = "Mistakes")
    public record MistakesDto(
            @Schema(description = "Les cartes ratées, la plus récemment ratée d'abord")
            List<PracticeCardDto> cards) {}
}
