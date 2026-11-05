package com.recallio.gamification.api;

import com.recallio.gamification.application.RetentionService;
import com.recallio.gamification.domain.RetentionReport;
import com.recallio.gamification.domain.RetentionReport.DayRetention;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** La rétention des joueurs (ADR 0050), pour les admins : des chiffres agrégés, jamais un joueur en particulier. */
@Path("/api/v1/admin/retention")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(AdminRetentionResource.ADMIN_ROLE)
@Tag(name = "Administration")
@SecurityRequirement(name = "SecurityScheme")
public class AdminRetentionResource {

    static final String ADMIN_ROLE = "admin";

    private final RetentionService retention;

    public AdminRetentionResource(RetentionService retention) {
        this.retention = retention;
    }

    @GET
    @Operation(operationId = "getRetention", summary = "La part des joueurs revenus réviser 1, 7 et 30 jours plus tard")
    @APIResponse(responseCode = "200", description = "La rétention")
    @APIResponse(responseCode = "403", description = "Réservé aux admins")
    public RetentionDto getRetention() {
        return RetentionDto.from(retention.report());
    }

    @Schema(name = "Retention")
    public record RetentionDto(
            @Schema(description = "Les joueurs qui ont révisé au moins une fois")
            int players,

            @Schema(description = "La rétention à J1, J7 et J30")
            List<DayRetentionDto> days) {

        static RetentionDto from(RetentionReport report) {
            return new RetentionDto(
                    report.players(),
                    report.days().stream().map(DayRetentionDto::from).toList());
        }
    }

    @Schema(name = "DayRetention")
    public record DayRetentionDto(
            @Schema(description = "Le nombre de jours après le premier jour actif")
            int day,

            @Schema(description = "Les joueurs dont le premier jour actif remonte à au moins ce nombre de jours")
            int eligible,

            @Schema(description = "Ceux d'entre eux qui ont révisé ce jour-là")
            int retained,

            @Schema(description = "La part des joueurs revenus, en pourcentage")
            int percent) {

        static DayRetentionDto from(DayRetention day) {
            return new DayRetentionDto(day.day(), day.eligible(), day.retained(), day.percent());
        }
    }
}
