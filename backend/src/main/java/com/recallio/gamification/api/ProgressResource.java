package com.recallio.gamification.api;

import com.recallio.gamification.application.ProgressService;
import com.recallio.gamification.application.ProgressService.Progress;
import com.recallio.gamification.application.ProgressService.RecentDay;
import com.recallio.gamification.application.ProgressService.SubjectXp;
import com.recallio.gamification.domain.Streak;
import com.recallio.gamification.domain.SubjectLevel;
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
import java.util.OptionalInt;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/**
 * Progression de l'utilisateur connecté : XP, streak et ses gels, objectif du jour (CLAUDE.md, section 6.3 ; ADR 0035,
 * 0036).
 */
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
    @Operation(
            operationId = "getMyProgress",
            summary = "XP et niveau de chaque sujet suivi, streak et gels, objectif du jour de l'utilisateur connecté")
    @APIResponse(responseCode = "200", description = "La progression")
    @APIResponse(responseCode = "401", description = "Non authentifié")
    public ProgressDto getMyProgress() {
        return ProgressDto.from(progress.progress(currentUser.get()));
    }

    @GET
    @Path("/activity")
    @Operation(
            operationId = "getMyRecentActivity",
            summary =
                    "Cartes révisées et jours couverts par un gel, chacun des derniers jours, aujourd'hui compris (fuseau de l'utilisateur)")
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
    public record DayActivityDto(
            LocalDate day,
            int cardsReviewed,
            boolean goalReached,

            @Schema(description = "Jour manqué couvert par un gel de streak (ADR 0036)")
            boolean frozen) {

        static DayActivityDto from(RecentDay day) {
            return new DayActivityDto(day.day(), day.cardsReviewed(), day.goalReached(), day.frozen());
        }
    }

    @Schema(name = "Progress")
    public record ProgressDto(
            @Schema(description = "Toute l'XP gagnée, bonus de l'objectif du jour compris")
            long totalXp,

            @Schema(description = "Jours consécutifs avec l'objectif atteint, sans compter ceux couverts par un gel")
            int streak,

            @Schema(description = "Gels de streak en réserve : chacun couvre un jour manqué (ADR 0036)")
            int streakFreezes,

            @Schema(
                    description =
                            "Jours où atteindre l'objectif avant le prochain gel ; absent quand la réserve est pleine",
                    nullable = true)
            @Nullable
            Integer daysToNextFreeze,

            @Schema(description = "Vrai si l'objectif atteint aujourd'hui vient de rapporter un gel")
            boolean freezeEarnedToday,

            @Schema(description = "Cartes révisées aujourd'hui, dans le fuseau de l'utilisateur")
            int cardsToday,

            @Schema(description = "Cartes à réviser chaque jour pour entretenir le streak")
            int dailyGoal,

            boolean goalReachedToday,

            @Schema(description = "L'XP et le niveau de chaque sujet suivi, dans l'ordre du catalogue (ADR 0035)")
            List<SubjectXpDto> subjects) {

        static ProgressDto from(Progress progress) {
            Streak streak = progress.streak();
            OptionalInt toNextFreeze = streak.daysToNextFreeze();
            return new ProgressDto(
                    progress.totalXp(),
                    streak.days(),
                    streak.freezes(),
                    toNextFreeze.isPresent() ? toNextFreeze.getAsInt() : null,
                    streak.freezeEarnedToday(),
                    progress.cardsToday(),
                    progress.dailyGoal(),
                    progress.goalReachedToday(),
                    progress.subjects().stream().map(SubjectXpDto::from).toList());
        }
    }

    @Schema(name = "SubjectXp")
    public record SubjectXpDto(
            String slug,
            long xp,

            @Schema(description = "Le niveau du sujet, à partir de 1")
            int level,

            @Schema(description = "L'XP à laquelle ce niveau commence")
            long levelXp,

            @Schema(description = "L'XP à laquelle commence le niveau suivant")
            long nextLevelXp) {

        static SubjectXpDto from(SubjectXp subject) {
            SubjectLevel level = subject.level();
            return new SubjectXpDto(
                    subject.subjectSlug(), level.xp(), level.level(), level.levelXp(), level.nextLevelXp());
        }
    }
}
