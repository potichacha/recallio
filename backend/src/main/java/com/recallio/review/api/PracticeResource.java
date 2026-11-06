package com.recallio.review.api;

import com.recallio.content.CardChoice;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.CardType;
import com.recallio.platform.api.ProblemDetail;
import com.recallio.review.api.ReviewDtos.BlankCorrectionDto;
import com.recallio.review.api.ReviewDtos.ExerciseToSolveDto;
import com.recallio.review.application.AnswerCorrection;
import com.recallio.review.application.GradedAnswer;
import com.recallio.review.application.PracticeService;
import com.recallio.review.application.PracticeService.Practice;
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
import java.util.Set;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/**
 * S'entraîner sur une notion (ADR 0038) : ses cartes, et la correction de celles que le serveur sait corriger. Rien
 * n'est enregistré ; un exercice s'essaie avec {@code POST /api/v1/exercise-runs}, comme en révision.
 */
@Path("/api/v1/me/practice/{subject}/{notion}")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Entraînement", description = "S'entraîner sur une notion, sans rien enregistrer (ADR 0038)")
@SecurityRequirement(name = "SecurityScheme")
public class PracticeResource {

    private final PracticeService practice;

    public PracticeResource(PracticeService practice) {
        this.practice = practice;
    }

    @GET
    @Operation(operationId = "getPractice", summary = "Les cartes d'une notion, pour s'entraîner sans rien enregistrer")
    @APIResponse(responseCode = "200", description = "Les cartes, formats mêlés, les exercices de code à la fin")
    @APIResponse(
            responseCode = "404",
            description = "Notion inconnue, retirée, ou d'un sujet non publié",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public PracticeDto getPractice(@PathParam("subject") String subject, @PathParam("notion") String notion) {
        return PracticeDto.from(practice.practice(subject, notion));
    }

    @POST
    @Path("/checks")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "checkPracticeAnswer",
            summary = "Corrige la réponse à une carte de l'entraînement, sans l'enregistrer")
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
            description = "La carte s'auto-évalue, ou la réponse ne convient pas à son format",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public PracticeCorrectionDto checkPracticeAnswer(
            @PathParam("subject") String subject,
            @PathParam("notion") String notion,
            @Valid @NotNull PracticeAnswerDto answer) {
        return PracticeCorrectionDto.from(practice.check(subject, notion, answer.toAnswer()));
    }

    @Schema(name = "Practice")
    public record PracticeDto(
            String notionTitle,

            @Schema(description = "Les cartes, formats mêlés, les exercices de code à la fin")
            List<PracticeCardDto> cards) {

        static PracticeDto from(Practice practice) {
            return new PracticeDto(
                    practice.notionTitle(),
                    practice.cards().stream().map(PracticeCardDto::from).toList());
        }
    }

    @Schema(name = "PracticeCard")
    public record PracticeCardDto(
            UUID cardId,
            CardType type,

            @Schema(description = "Recto, en Markdown") String prompt,

            @Schema(
                    description = "Verso, en Markdown ; absent pour une carte corrigée par le serveur (une carte à"
                            + " choix, un code à trous, un exercice), dont l'explication arrive avec la correction",
                    nullable = true)
            @Nullable
            String answer,

            @Schema(
                    description = "Les choix de la carte, sans dire lesquels sont justes ; vide pour une carte qui"
                            + " s'auto-évalue, un code à trous ou un exercice (ADR 0051)")
            List<String> choices,

            @Schema(description = "Vrai si plusieurs choix sont justes")
            boolean multipleAnswers,

            @Schema(description = "Le nombre de trous d'un code à trous ; 0 sinon")
            int blankCount,

            @Schema(
                    description = "Les fichiers d'un exercice de code ; absent pour les autres formats",
                    nullable = true)
            @Nullable
            ExerciseToSolveDto exercise) {

        static PracticeCardDto from(CardSnapshot card) {
            return new PracticeCardDto(
                    card.cardId(),
                    card.type(),
                    card.prompt(),
                    card.isGradedByServer() ? null : card.answer(),
                    card.choices().stream().map(CardChoice::text).toList(),
                    card.correctChoices().size() > 1,
                    card.blanks().size(),
                    card.exercise() == null ? null : ExerciseToSolveDto.from(card.exercise()));
        }
    }

    @Schema(name = "PracticeAnswer")
    public record PracticeAnswerDto(
            @NotNull UUID cardId,

            @Schema(description = "Les positions cochées d'un QCM ou d'un vrai ou faux, à partir de 0", nullable = true)
            @Nullable
            List<@NotNull Integer> selectedChoices,

            @Schema(description = "Les réponses d'un code à trous, une par trou", nullable = true) @Nullable
            List<@NotNull String> filledBlanks) {

        GradedAnswer toAnswer() {
            List<Integer> selected = selectedChoices;
            return new GradedAnswer(cardId, selected == null ? null : Set.copyOf(selected), filledBlanks);
        }
    }

    @Schema(name = "PracticeCorrection")
    public record PracticeCorrectionDto(
            UUID cardId,
            boolean correct,

            @Schema(description = "Les bonnes réponses d'un QCM ou d'un vrai ou faux, à partir de 0")
            List<Integer> correctChoices,

            @Schema(description = "La correction de chaque trou d'un code à compléter")
            List<BlankCorrectionDto> blanks,

            @Schema(description = "L'explication, en Markdown")
            String explanation) {

        static PracticeCorrectionDto from(AnswerCorrection correction) {
            return new PracticeCorrectionDto(
                    correction.cardId(),
                    correction.correct(),
                    correction.correctChoices().stream().sorted().toList(),
                    correction.blanks().stream().map(BlankCorrectionDto::from).toList(),
                    correction.explanation());
        }
    }
}
