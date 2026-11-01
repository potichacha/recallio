package com.recallio.review.api;

import com.recallio.content.CardType;
import com.recallio.platform.api.ProblemDetail;
import com.recallio.review.api.ReviewDtos.RecordReviewRequest;
import com.recallio.review.api.ReviewDtos.ReviewQueueDto;
import com.recallio.review.api.ReviewDtos.ReviewResultDto;
import com.recallio.review.application.ReviewQueueService;
import com.recallio.review.application.ReviewQueueService.QueueFilter;
import com.recallio.review.application.ReviewRecordingService;
import com.recallio.review.application.ReviewRecordingService.RecordReview;
import com.recallio.review.application.ReviewRecordingService.ReviewOutcome;
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
    private final CurrentUser currentUser;

    public ReviewsResource(ReviewQueueService queues, ReviewRecordingService recordings, CurrentUser currentUser) {
        this.queues = queues;
        this.recordings = recordings;
        this.currentUser = currentUser;
    }

    @GET
    @Path("/review-queue")
    @Operation(
            operationId = "getReviewQueue",
            summary = "Cartes à réviser maintenant : les cartes dues, puis des nouvelles dans la limite du jour."
                    + " Sans filtre, elles viennent des sujets suivis.")
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
            description = "Sujet ou thème inconnu",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public ReviewQueueDto getReviewQueue(
            @Parameter(description = "Slug du sujet, pour ne réviser que ce sujet (suivi ou non)")
                    @QueryParam("subject")
                    @Nullable
                    String subject,
            @Parameter(description = "Slug du thème, pour ne réviser que ce thème") @QueryParam("theme") @Nullable
                    String theme,
            @Parameter(description = "Format de carte, pour ne réviser que ce format") @QueryParam("type") @Nullable
                    CardType type,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(MAX_QUEUE_SIZE) int size) {
        return ReviewQueueDto.from(queues.queue(currentUser.get(), new QueueFilter(subject, theme, type), size));
    }

    @POST
    @Path("/reviews")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "recordReview",
            summary = "Enregistre la note donnée à une carte (idempotent grâce à requestId)")
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
    public Response recordReview(@Valid @NotNull RecordReviewRequest request) {
        ReviewOutcome outcome = recordings.record(
                currentUser.get(),
                new RecordReview(request.requestId(), request.cardId(), request.rating(), request.elapsedMs()));
        Response.Status status = outcome.replayed() ? Response.Status.OK : Response.Status.CREATED;
        return Response.status(status).entity(ReviewResultDto.from(outcome)).build();
    }
}
