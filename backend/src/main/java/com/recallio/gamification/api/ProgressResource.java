package com.recallio.gamification.api;

import com.recallio.gamification.application.ProgressService;
import com.recallio.gamification.application.ProgressService.Progress;
import com.recallio.gamification.domain.ActivityRepository.DailyActivity;
import com.recallio.platform.api.ProblemDetail;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** Progression de l'utilisateur connecté : points, streak et objectif du jour (CLAUDE.md, section 6.3). */
@Path("/api/v1/me/progress")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Progression")
@SecurityRequirement(name = "SecurityScheme")
public class ProgressResource {

    static final int MAX_ACTIVITY_DAYS = 90;

    private final ProgressService progress;
    private final CurrentUser currentUser;

    public ProgressResource(ProgressService progress, CurrentUser currentUser) {
        this.progress = progress;
        this.currentUser = currentUser;
    }

    @GET
    @Operation(operationId = "getMyProgress", summary = "Points, streak et objectif du jour de l'utilisateur connecté")
    @APIResponse(responseCode = "200", description = "La progression")
    @APIResponse(responseCode = "401", description = "Non authentifié")
    public ProgressDto getMyProgress() {
        return ProgressDto.from(progress.progress(currentUser.get()));
    }

    @GET
    @Path("/activity")
    @Operation(
            operationId = "getMyRecentActivity",
            summary = "Cartes révisées chacun des derniers jours, aujourd'hui compris (fuseau de l'utilisateur)")
    @APIResponse(responseCode = "200", description = "Un élément par jour, du plus ancien au plus récent")
    @APIResponse(
            responseCode = "400",
            description = "Nombre de jours invalide",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public List<DayActivityDto> getMyRecentActivity(
            @QueryParam("days") @DefaultValue("7") @Min(1) @Max(MAX_ACTIVITY_DAYS) int days) {
        return progress.recentActivity(currentUser.get(), days).stream()
                .map(DayActivityDto::from)
                .toList();
    }

    @Schema(name = "DayActivity")
    public record DayActivityDto(LocalDate day, int cardsReviewed, boolean goalReached) {
        static DayActivityDto from(DailyActivity activity) {
            return new DayActivityDto(activity.day(), activity.cardsReviewed(), activity.goalReached());
        }
    }

    @Schema(name = "Progress")
    public record ProgressDto(
            @Schema(description = "Total des points gagnés") long totalPoints,

            @Schema(description = "Jours consécutifs avec l'objectif atteint")
            int streak,

            @Schema(description = "Cartes révisées aujourd'hui, dans le fuseau de l'utilisateur")
            int cardsToday,

            @Schema(description = "Cartes à réviser chaque jour pour entretenir le streak")
            int dailyGoal,

            boolean goalReachedToday) {

        static ProgressDto from(Progress progress) {
            return new ProgressDto(
                    progress.totalPoints(),
                    progress.streak(),
                    progress.cardsToday(),
                    progress.dailyGoal(),
                    progress.goalReachedToday());
        }
    }
}
