package com.recallio.feedback.api;

import com.recallio.feedback.application.ContentReportService;
import com.recallio.feedback.application.ContentReportService.ReportView;
import com.recallio.feedback.domain.ContentReport;
import com.recallio.feedback.domain.ReportReason;
import com.recallio.feedback.domain.ReportResolution;
import com.recallio.feedback.domain.ReportTarget.CardTarget;
import com.recallio.feedback.domain.ReportTarget.TourPageTarget;
import com.recallio.platform.domain.Page;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/**
 * Tâche d'administration : traiter les signalements (ADR 0042). L'auteur d'un signalement n'est pas montré : l'admin n'a
 * besoin que du contenu signalé et de ce qui ne va pas.
 */
@Path("/api/v1/admin/content-reports")
@RolesAllowed(AdminContentReportsResource.ADMIN_ROLE)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Signalements")
@SecurityRequirement(name = "SecurityScheme")
public class AdminContentReportsResource {

    static final String ADMIN_ROLE = "admin";
    static final int MAX_PAGE_SIZE = 100;

    private final ContentReportService reports;

    public AdminContentReportsResource(ContentReportService reports) {
        this.reports = reports;
    }

    @GET
    @Operation(operationId = "listContentReports", summary = "Les signalements ouverts, ou traités")
    @APIResponse(responseCode = "200", description = "Une page de signalements")
    @APIResponse(responseCode = "403", description = "Rôle admin requis")
    public ReportPageDto listContentReports(
            @QueryParam("status") @DefaultValue("OPEN") ReportStatus status,
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return ReportPageDto.from(reports.list(status == ReportStatus.OPEN, page, size));
    }

    @PUT
    @Path("/{id}/resolution")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "resolveContentReport", summary = "Traite un signalement : corrigé, ou rejeté")
    @APIResponse(responseCode = "200", description = "Le signalement traité")
    @APIResponse(responseCode = "403", description = "Rôle admin requis")
    @APIResponse(responseCode = "404", description = "Aucun signalement de cet identifiant")
    @APIResponse(responseCode = "409", description = "Déjà traité")
    public AdminReportDto resolveContentReport(@PathParam("id") UUID id, @Valid @NotNull ResolutionDto request) {
        return AdminReportDto.from(reports.resolve(id, request.resolution()));
    }

    public enum ReportStatus {
        OPEN,
        RESOLVED
    }

    @Schema(name = "ReportResolutionRequest")
    public record ResolutionDto(@NotNull ReportResolution resolution) {}

    @Schema(name = "AdminContentReportPage")
    public record ReportPageDto(List<AdminReportDto> items, int page, int size, long totalItems, int totalPages) {

        static ReportPageDto from(Page<ReportView> page) {
            return new ReportPageDto(
                    page.items().stream().map(AdminReportDto::from).toList(),
                    page.page(),
                    page.size(),
                    page.totalItems(),
                    page.totalPages());
        }
    }

    @Schema(name = "AdminContentReport")
    public record AdminReportDto(
            UUID id,
            String subject,

            @Schema(description = "La carte signalée ; absente pour une page du Tour") @Nullable
            UUID cardId,

            @Schema(description = "Le thème de la carte, pour l'ouvrir ; absent pour une page, ou une carte retirée")
            @Nullable
            String themeSlug,

            @Schema(description = "La notion de la page signalée ; absente pour une carte") @Nullable
            String notion,

            @Schema(description = "Le rang de la page, à partir de 1 ; absent pour une carte") @Nullable
            Integer page,

            @Schema(description = "Le titre de la notion de la carte ou de la page, s'il est connu") @Nullable
            String notionTitle,

            @Schema(description = "Le début de la question de la carte ; absent pour une page, ou une carte retirée")
            @Nullable
            String excerpt,

            ReportReason reason,

            @Schema(description = "Ce que l'utilisateur a remarqué ; vide s'il n'a rien précisé")
            String comment,

            Instant createdAt,

            @Schema(description = "Absente tant que le signalement est ouvert") @Nullable
            ReportResolution resolution,

            @Nullable Instant resolvedAt) {

        static AdminReportDto from(ReportView view) {
            ContentReport report = view.report();
            UUID cardId = null;
            String notion = null;
            Integer page = null;
            switch (report.target()) {
                case CardTarget(UUID id) -> cardId = id;
                case TourPageTarget(String _, String notionSlug, int rank) -> {
                    notion = notionSlug;
                    page = rank;
                }
            }
            return new AdminReportDto(
                    report.id(),
                    report.subjectSlug(),
                    cardId,
                    view.themeSlug(),
                    notion,
                    page,
                    view.notionTitle(),
                    view.excerpt(),
                    report.reason(),
                    report.comment(),
                    report.createdAt(),
                    report.resolution(),
                    report.resolvedAt());
        }
    }
}
