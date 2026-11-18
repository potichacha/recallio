package com.recallio.gamification.api;

import com.recallio.gamification.application.FunnelService;
import com.recallio.gamification.domain.funnel.FunnelReport;
import com.recallio.gamification.domain.funnel.FunnelReport.FunnelWeek;
import com.recallio.gamification.domain.funnel.FunnelReport.StepCount;
import com.recallio.gamification.domain.funnel.FunnelStep;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/**
 * Le parcours mesuré (ADR 0064), pour les admins, à côté de la rétention : visite, essai, compte créé, première
 * révision, semaine par semaine. Des comptes, jamais une personne.
 */
@Path("/api/v1/admin/funnel")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(AdminRetentionResource.ADMIN_ROLE)
@Tag(name = "Administration")
@SecurityRequirement(name = "SecurityScheme")
public class AdminFunnelResource {

    private final FunnelService funnel;

    public AdminFunnelResource(FunnelService funnel) {
        this.funnel = funnel;
    }

    @GET
    @Operation(
            operationId = "getFunnel",
            summary = "Visites, essais, comptes créés et premières révisions des huit dernières semaines")
    @APIResponse(responseCode = "200", description = "Le parcours")
    @APIResponse(responseCode = "403", description = "Réservé aux admins")
    public FunnelDto getFunnel() {
        return FunnelDto.from(funnel.report());
    }

    @Schema(name = "Funnel")
    public record FunnelDto(
            @Schema(description = "Les semaines ISO, du lundi au dimanche en UTC, la semaine en cours d'abord")
            List<FunnelWeekDto> weeks) {

        static FunnelDto from(FunnelReport report) {
            return new FunnelDto(
                    report.weeks().stream().map(FunnelWeekDto::from).toList());
        }
    }

    @Schema(name = "FunnelWeek")
    public record FunnelWeekDto(
            @Schema(description = "Le lundi de la semaine") LocalDate weekStart,

            @Schema(description = "Le dimanche de la semaine")
            LocalDate weekEnd,

            @Schema(description = "Chaque étape du parcours, dans l'ordre")
            List<FunnelStepCountDto> steps) {

        static FunnelWeekDto from(FunnelWeek week) {
            return new FunnelWeekDto(
                    week.week().monday(),
                    week.week().sunday(),
                    week.steps().stream().map(FunnelStepCountDto::from).toList());
        }
    }

    @Schema(name = "FunnelStepCount")
    public record FunnelStepCountDto(
            FunnelStep step,

            @Schema(description = "Les fois où l'étape a été franchie cette semaine-là")
            long count,

            @Schema(
                    description = "Le taux de passage depuis l'étape précédente, en pourcentage arrondi vers le"
                            + " bas ; absent pour la première étape, ou si personne n'a franchi la précédente",
                    nullable = true)
            @Nullable
            Integer percent) {

        static FunnelStepCountDto from(StepCount step) {
            return new FunnelStepCountDto(step.step(), step.count(), step.percent());
        }
    }
}
