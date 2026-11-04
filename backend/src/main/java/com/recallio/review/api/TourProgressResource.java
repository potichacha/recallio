package com.recallio.review.api;

import com.recallio.content.CardChoice;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.CardType;
import com.recallio.platform.api.ProblemDetail;
import com.recallio.review.api.ReviewDtos.BlankCorrectionDto;
import com.recallio.review.application.TourValidationService;
import com.recallio.review.application.TourValidationService.CheckOutcome;
import com.recallio.review.application.TourValidationService.Checkpoint;
import com.recallio.review.application.TourValidationService.QuestionResult;
import com.recallio.review.application.TourValidationService.TourAnswer;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
 * La progression de l'utilisateur connecté dans le Tour d'un sujet (ADR 0037) : les notions qu'il a validées, et les
 * questions qui valident une notion. Ces réponses ne sont pas des révisions.
 */
@Path("/api/v1/me/tour/{subject}")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Tour")
@SecurityRequirement(name = "SecurityScheme")
public class TourProgressResource {

    /** Bien au-delà des questions d'une notion : une borne contre les requêtes démesurées, pas une règle du Tour. */
    static final int MAX_ANSWERS = 10;

    private final TourValidationService validation;
    private final CurrentUser currentUser;

    public TourProgressResource(TourValidationService validation, CurrentUser currentUser) {
        this.validation = validation;
        this.currentUser = currentUser;
    }

    @GET
    @Operation(
            operationId = "getMyTourProgress",
            summary = "Les notions du Tour d'un sujet que l'utilisateur a validées")
    @APIResponse(responseCode = "200", description = "La progression ; aucune notion si le sujet est inconnu")
    public TourProgressDto getMyTourProgress(@PathParam("subject") String subject) {
        return new TourProgressDto(validation.validatedNotions(currentUser.get(), subject));
    }

    @GET
    @Path("/{notion}")
    @Operation(
            operationId = "getTourCheckpoint",
            summary = "Les questions qui valident une notion du Tour, et si l'utilisateur l'a déjà validée")
    @APIResponse(responseCode = "200", description = "Les questions, sans leurs réponses")
    @APIResponse(
            responseCode = "404",
            description = "Notion sans questions : inconnue, hors du Tour, ou d'un sujet non publié",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public TourCheckpointDto getTourCheckpoint(
            @PathParam("subject") String subject, @PathParam("notion") String notion) {
        return TourCheckpointDto.from(validation.checkpoint(currentUser.get(), subject, notion));
    }

    @POST
    @Path("/{notion}/checks")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "checkTourNotion",
            summary =
                    "Corrige une réponse à chaque question d'une notion du Tour ; toutes justes, la notion est validée")
    @APIResponse(responseCode = "200", description = "La correction de chaque question")
    @APIResponse(
            responseCode = "404",
            description = "Notion sans questions",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "422",
            description = "Une réponse manque, est en trop, ou ne convient pas au format de sa question",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public TourCheckDto checkTourNotion(
            @PathParam("subject") String subject,
            @PathParam("notion") String notion,
            @Valid @NotNull TourCheckRequest request) {
        List<TourAnswer> answers =
                request.answers().stream().map(TourAnswerDto::toAnswer).toList();
        return TourCheckDto.from(validation.check(currentUser.get(), subject, notion, answers));
    }

    @Schema(name = "TourProgress")
    public record TourProgressDto(
            @Schema(description = "Les notions validées, dans l'ordre du référentiel")
            List<String> validatedNotions) {}

    @Schema(name = "TourCheckpoint")
    public record TourCheckpointDto(
            @Schema(description = "Vrai si l'utilisateur a déjà validé la notion")
            boolean validated,

            List<TourQuestionDto> questions) {

        static TourCheckpointDto from(Checkpoint checkpoint) {
            return new TourCheckpointDto(
                    checkpoint.validated(),
                    checkpoint.questions().stream().map(TourQuestionDto::from).toList());
        }
    }

    @Schema(name = "TourQuestion")
    public record TourQuestionDto(
            UUID cardId,
            CardType type,

            @Schema(description = "En Markdown ; les trous d'un code à compléter sont des ___")
            String prompt,

            @Schema(description = "Les choix d'un QCM ou d'un vrai ou faux, sans dire lesquels sont justes")
            List<String> choices,

            @Schema(description = "Vrai si plusieurs choix sont justes")
            boolean multipleAnswers,

            @Schema(description = "Le nombre de trous d'un code à compléter")
            int blankCount) {

        static TourQuestionDto from(CardSnapshot card) {
            return new TourQuestionDto(
                    card.cardId(),
                    card.type(),
                    card.prompt(),
                    card.choices().stream().map(CardChoice::text).toList(),
                    card.correctChoices().size() > 1,
                    card.blanks().size());
        }
    }

    @Schema(name = "TourCheckRequest")
    public record TourCheckRequest(
            @Schema(description = "Une réponse par question de la notion") @NotNull @Size(min = 1, max = MAX_ANSWERS)
            List<@NotNull @Valid TourAnswerDto> answers) {}

    @Schema(name = "TourAnswer")
    public record TourAnswerDto(
            @NotNull UUID cardId,

            @Schema(description = "Les positions cochées d'un QCM ou d'un vrai ou faux, à partir de 0", nullable = true)
            @Nullable
            List<Integer> selectedChoices,

            @Schema(description = "Les réponses d'un code à trous, une par trou", nullable = true) @Nullable
            List<String> filledBlanks) {

        TourAnswer toAnswer() {
            List<Integer> selected = selectedChoices;
            return new TourAnswer(cardId, selected == null ? null : Set.copyOf(selected), filledBlanks);
        }
    }

    @Schema(name = "TourCheck")
    public record TourCheckDto(
            @Schema(description = "Vrai si la notion est validée, par cette réponse ou par une précédente")
            boolean validated,

            List<TourQuestionResultDto> results) {

        static TourCheckDto from(CheckOutcome outcome) {
            return new TourCheckDto(
                    outcome.validated(),
                    outcome.results().stream().map(TourQuestionResultDto::from).toList());
        }
    }

    @Schema(name = "TourQuestionResult")
    public record TourQuestionResultDto(
            UUID cardId,
            boolean correct,

            @Schema(description = "Les bonnes réponses d'un QCM ou d'un vrai ou faux, à partir de 0")
            List<Integer> correctChoices,

            @Schema(description = "La correction de chaque trou d'un code à compléter")
            List<BlankCorrectionDto> blanks,

            @Schema(description = "L'explication, en Markdown")
            String explanation) {

        static TourQuestionResultDto from(QuestionResult result) {
            return new TourQuestionResultDto(
                    result.cardId(),
                    result.correct(),
                    result.correctChoices().stream().sorted().toList(),
                    result.blanks().stream().map(BlankCorrectionDto::from).toList(),
                    result.explanation());
        }
    }
}
