package com.recallio.review.api;

import com.recallio.content.CardType;
import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.platform.api.ProblemDetail;
import com.recallio.review.api.ReviewDtos.ExerciseFileDto;
import com.recallio.review.api.ReviewDtos.ExerciseRunDto;
import com.recallio.review.api.ReviewDtos.ExerciseRunRequest;
import com.recallio.review.api.ReviewDtos.RecordReviewRequest;
import com.recallio.review.api.ReviewDtos.ReviewQueueDto;
import com.recallio.review.api.ReviewDtos.ReviewResultDto;
import com.recallio.review.application.ExerciseRunService;
import com.recallio.review.application.ExerciseSubmissionService;
import com.recallio.review.application.ReviewQueueService;
import com.recallio.review.application.ReviewQueueService.QueueFilter;
import com.recallio.review.application.ReviewRecordingService;
import com.recallio.review.application.ReviewRecordingService.RecordReview;
import com.recallio.review.application.ReviewRecordingService.ReviewOutcome;
import com.recallio.review.domain.AnswerMismatchException;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/** Révision de l'utilisateur connecté : chacun ne lit et ne modifie que sa propre progression (CLAUDE.md, 4.5). */
@Path("/api/v1")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Révision", description = "File de révision et réponses, planifiées par FSRS")
@SecurityRequirement(name = "SecurityScheme")
public class ReviewsResource {

    static final int MAX_QUEUE_SIZE = 50;

    private final ReviewQueueService queues;
    private final ReviewRecordingService recordings;
    private final ExerciseRunService exerciseRuns;
    private final ExerciseSubmissionService exerciseSubmissions;
    private final CurrentUser currentUser;

    public ReviewsResource(
            ReviewQueueService queues,
            ReviewRecordingService recordings,
            ExerciseRunService exerciseRuns,
            ExerciseSubmissionService exerciseSubmissions,
            CurrentUser currentUser) {
        this.queues = queues;
        this.recordings = recordings;
        this.exerciseRuns = exerciseRuns;
        this.exerciseSubmissions = exerciseSubmissions;
        this.currentUser = currentUser;
    }

    @GET
    @Path("/review-queue")
    @Operation(
            operationId = "getReviewQueue",
            summary = "Cartes à réviser maintenant : les cartes dues, puis des nouvelles dans la limite du jour."
                    + " Sans filtre, elles viennent des sujets suivis ; avec un groupe, de ses réglages.")
    @APIResponse(responseCode = "200", description = "La file de révision")
    @APIResponse(
            responseCode = "400",
            description = "Paramètre invalide",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "404",
            description = "Sujet ou thème inconnu, ou groupe dont l'utilisateur n'est pas membre",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public ReviewQueueDto getReviewQueue(
            @Parameter(description = "Identifiant d'un groupe dont l'utilisateur est membre, pour réviser ses cartes")
                    @QueryParam("group")
                    @Nullable
                    UUID group,
            @Parameter(description = "Slug du sujet, pour ne réviser que ce sujet (suivi ou non)")
                    @QueryParam("subject")
                    @Nullable
                    String subject,
            @Parameter(description = "Slug du thème, pour ne réviser que ce thème") @QueryParam("theme") @Nullable
                    String theme,
            @Parameter(
                            description = "Formats de carte, pour ne réviser qu'eux (répétable : le mode « questions"
                                    + " corrigées » de la PWA en demande quatre, ADR 0048) ; aucun : tous les formats")
                    @QueryParam("type")
                    @Nullable
                    List<CardType> types,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(MAX_QUEUE_SIZE) int size) {
        var filter = new QueueFilter(group, subject, theme, types == null ? Set.of() : Set.copyOf(types));
        return ReviewQueueDto.from(queues.queue(currentUser.get(), filter, size));
    }

    @POST
    @Path("/reviews")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "recordReview",
            summary =
                    "Enregistre la note donnée à une carte, corrige un QCM ou un code à trous, ou juge un exercice de code"
                            + " par ses tests (idempotent grâce à requestId)")
    @APIResponse(
            responseCode = "201",
            description = "Révision enregistrée",
            content = @Content(schema = @Schema(implementation = ReviewResultDto.class)))
    @APIResponse(
            responseCode = "200",
            description = "Requête déjà reçue : le résultat enregistré la première fois",
            content = @Content(schema = @Schema(implementation = ReviewResultDto.class)))
    @APIResponse(
            responseCode = "400",
            description = "Requête invalide",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "404",
            description = "Carte inconnue ou retirée",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "409",
            description = "requestId déjà utilisé pour une autre révision",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "422",
            description = "Une réponse qui ne correspond pas au format : une note pour un QCM ou un code à trous, des"
                    + " choix ou des trous pour une carte en auto-évaluation, un nombre de trous inexact",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "429",
            description = "Un exercice de code de cet utilisateur est déjà en cours d'exécution",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "503",
            description =
                    "L'exécution des exercices de code est indisponible : rien n'est enregistré, réessayer plus tard",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public Response recordReview(@Valid @NotNull RecordReviewRequest request) {
        List<ExerciseFileDto> submitted = request.submittedFiles();
        ReviewOutcome outcome = submitted == null ? record(request) : submit(request, submitted);
        Response.Status status = outcome.replayed() ? Response.Status.OK : Response.Status.CREATED;
        return Response.status(status).entity(ReviewResultDto.from(outcome)).build();
    }

    @POST
    @Path("/exercise-runs")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "runExercise",
            summary = "Exécute le code d'un exercice contre ses tests, pour l'essayer : rien n'est enregistré")
    @APIResponse(responseCode = "200", description = "Le résultat de chaque test")
    @APIResponse(
            responseCode = "400",
            description = "Requête invalide",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "404",
            description = "Carte inconnue ou retirée",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "422",
            description = "La carte n'est pas un exercice, ou les fichiers ne sont pas ceux de son code de départ",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "429",
            description = "Une exécution de cet utilisateur est déjà en cours",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "503",
            description = "L'exécution des exercices de code est indisponible",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public ExerciseRunDto runExercise(@Valid @NotNull ExerciseRunRequest request) {
        return ExerciseRunDto.from(exerciseRuns.run(currentUser.get(), request.cardId(), sourceFiles(request.files())));
    }

    private ReviewOutcome record(RecordReviewRequest request) {
        List<Integer> selected = request.selectedChoices();
        return recordings.record(
                currentUser.get(),
                new RecordReview(
                        request.requestId(),
                        request.cardId(),
                        request.rating(),
                        selected == null ? null : Set.copyOf(selected),
                        request.filledBlanks(),
                        null,
                        request.elapsedMs()));
    }

    /** Un exercice rendu : ses fichiers seuls, sans note ni autre réponse, que ses tests jugent. */
    private ReviewOutcome submit(RecordReviewRequest request, List<ExerciseFileDto> submitted) {
        if (request.rating() != null || request.selectedChoices() != null || request.filledBlanks() != null) {
            throw AnswerMismatchException.filesExpected();
        }
        return exerciseSubmissions.submit(
                currentUser.get(), request.requestId(), request.cardId(), sourceFiles(submitted), request.elapsedMs());
    }

    private static List<SourceFile> sourceFiles(List<ExerciseFileDto> files) {
        return files.stream().map(ExerciseFileDto::toSourceFile).toList();
    }
}
