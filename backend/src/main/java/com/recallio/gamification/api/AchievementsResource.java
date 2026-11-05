package com.recallio.gamification.api;

import com.recallio.gamification.application.AchievementService;
import com.recallio.gamification.application.AchievementService.AchievementView;
import com.recallio.gamification.domain.achievement.Achievement;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/** Les succès de l'utilisateur connecté (ADR 0040). */
@Path("/api/v1/me/achievements")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Progression")
@SecurityRequirement(name = "SecurityScheme")
public class AchievementsResource {

    private final AchievementService achievements;
    private final CurrentUser currentUser;

    public AchievementsResource(AchievementService achievements, CurrentUser currentUser) {
        this.achievements = achievements;
        this.currentUser = currentUser;
    }

    @GET
    @Operation(operationId = "getMyAchievements", summary = "Tous les succès, débloqués ou non")
    @APIResponse(responseCode = "200", description = "Les succès, dans l'ordre du catalogue")
    public List<AchievementStatusDto> getMyAchievements() {
        return achievements.all(currentUser.get()).stream()
                .map(AchievementStatusDto::from)
                .toList();
    }

    @Schema(name = "AchievementStatus")
    public record AchievementStatusDto(
            Achievement achievement,

            @Schema(description = "Absent tant que le succès n'est pas débloqué", nullable = true) @Nullable
            Instant unlockedAt) {

        static AchievementStatusDto from(AchievementView view) {
            return new AchievementStatusDto(view.achievement(), view.unlockedAt());
        }
    }
}
