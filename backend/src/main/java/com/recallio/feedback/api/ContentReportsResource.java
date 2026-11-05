package com.recallio.feedback.api;

import com.recallio.feedback.application.ContentReportService;
import com.recallio.feedback.domain.ContentReport;
import com.recallio.feedback.domain.FeedbackExceptions.InvalidReportTargetException;
import com.recallio.feedback.domain.ReportReason;
import com.recallio.feedback.domain.ReportTarget;
import com.recallio.feedback.domain.ReportTarget.CardTarget;
import com.recallio.feedback.domain.ReportTarget.TourPageTarget;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/** Signaler une erreur dans une carte ou une page du Tour (ADR 0042). */
@Path("/api/v1/content-reports")
@Authenticated
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Signalements")
@SecurityRequirement(name = "SecurityScheme")
public class ContentReportsResource {

    static final int MAX_SLUG_LENGTH = 100;

    private final ContentReportService reports;
    private final CurrentUser currentUser;

    public ContentReportsResource(ContentReportService reports, CurrentUser currentUser) {
        this.reports = reports;
        this.currentUser = currentUser;
    }

    @POST
    @Operation(operationId = "reportContent", summary = "Signale une erreur dans une carte ou une page du Tour")
    @APIResponse(
            responseCode = "201",
            description = "Le signalement, en attente d'un admin",
            content = @Content(schema = @Schema(implementation = ContentReportDto.class)))
    @APIResponse(responseCode = "404", description = "Ce contenu n'existe pas, ou plus")
    @APIResponse(responseCode = "409", description = "Déjà signalé par cet utilisateur, et pas encore traité")
    @APIResponse(responseCode = "422", description = "Ni une carte, ni une page du Tour")
    @APIResponse(responseCode = "429", description = "Trop de signalements en attente")
    public Response reportContent(@Valid @NotNull NewContentReportDto request) {
        ContentReport report = reports.report(
                currentUser.get(),
                request.target(),
                request.reason(),
                request.comment() == null ? "" : request.comment());
        return Response.status(Response.Status.CREATED)
                .entity(ContentReportDto.from(report))
                .build();
    }

    @Schema(
            name = "NewContentReport",
            description = "Une carte (cardId), ou une page du Tour (subject, notion et page)",
            requiredProperties = {"reason"})
    public record NewContentReportDto(
            @Nullable UUID cardId,

            @Schema(description = "Le sujet de la page") @Nullable @Size(max = MAX_SLUG_LENGTH)
            String subject,

            @Schema(description = "La notion de la page") @Nullable @Size(max = MAX_SLUG_LENGTH)
            String notion,

            @Schema(description = "Le rang de la page dans la notion, à partir de 1") @Nullable @Min(1)
            Integer page,

            @NotNull ReportReason reason,

            @Schema(description = "Ce que l'utilisateur a remarqué")
            @Nullable
            @Size(max = ContentReport.MAX_COMMENT_LENGTH)
            String comment) {

        /** @throws InvalidReportTargetException sans carte ni page complète, ou avec les deux */
        ReportTarget target() {
            boolean anyPagePart = subject != null || notion != null || page != null;
            if (cardId != null && !anyPagePart) {
                return new CardTarget(cardId);
            }
            if (cardId == null && subject != null && notion != null && page != null) {
                return new TourPageTarget(subject, notion, page);
            }
            throw new InvalidReportTargetException();
        }
    }

    @Schema(name = "ContentReport")
    public record ContentReportDto(UUID id, ReportReason reason, Instant createdAt) {

        static ContentReportDto from(ContentReport report) {
            return new ContentReportDto(report.id(), report.reason(), report.createdAt());
        }
    }
}
