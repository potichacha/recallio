package com.recallio.review.api;

import com.recallio.platform.api.ProblemDetail;
import com.recallio.review.api.PracticeResource.PracticeAnswerDto;
import com.recallio.review.api.PracticeResource.PracticeCardDto;
import com.recallio.review.api.PracticeResource.PracticeCorrectionDto;
import com.recallio.review.application.TrialService;
import com.recallio.review.application.TrialService.TrialQuestion;
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
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * L'essai sans compte (ADR 0050) : public comme le catalogue, il ne sait rien de l'utilisateur et n'enregistre rien.
 */
@Path("/api/v1/subjects/{subject}/trial")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Essai", description = "Essayer sans compte : quelques questions corrigées, rien d'enregistré (ADR 0050)")
public class TrialResource {

    private final TrialService trials;

    public TrialResource(TrialService trials) {
        this.trials = trials;
    }

    @GET
    @Operation(operationId = "getTrial", summary = "Les questions de l'essai d'un sujet, sans compte")
    @APIResponse(responseCode = "200", description = "Les questions, une par notion essentielle")
    @APIResponse(
            responseCode = "404",
            description = "Sujet inconnu, non publié, ou sans notion essentielle",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public TrialDto getTrial(@PathParam("subject") String subject) {
        return new TrialDto(
                trials.questions(subject).stream().map(TrialQuestionDto::from).toList());
    }

    @POST
    @Path("/{notion}/checks")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "checkTrialAnswer", summary = "Corrige une réponse de l'essai, sans l'enregistrer")
    @APIResponse(responseCode = "200", description = "La correction")
    @APIResponse(
            responseCode = "404",
            description = "Notion inconnue, ou carte qui n'est pas une carte active de la notion",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "422",
            description = "La réponse ne convient pas au format de la carte",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public PracticeCorrectionDto checkTrialAnswer(
            @PathParam("subject") String subject,
            @PathParam("notion") String notion,
            @Valid @NotNull PracticeAnswerDto answer) {
        return PracticeCorrectionDto.from(trials.check(subject, notion, answer.toAnswer()));
    }

    @Schema(name = "Trial")
    public record TrialDto(
            @Schema(description = "Les questions, des notions les plus simples aux plus avancées")
            List<TrialQuestionDto> questions) {}

    @Schema(name = "TrialQuestion")
    public record TrialQuestionDto(
            @Schema(description = "Le slug de la notion, à rappeler pour corriger la réponse")
            String notion,

            PracticeCardDto card) {

        static TrialQuestionDto from(TrialQuestion question) {
            return new TrialQuestionDto(question.notionSlug(), PracticeCardDto.from(question.card()));
        }
    }
}
