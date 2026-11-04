package com.recallio.gamification.api;

import com.recallio.gamification.application.GroupLeaderboardService;
import com.recallio.gamification.application.GroupLeaderboardService.Entry;
import com.recallio.gamification.application.GroupLeaderboardService.Leaderboard;
import com.recallio.group.LeaderboardVisibility;
import com.recallio.platform.api.ProblemDetail;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/**
 * Le classement de la semaine d'un groupe (CLAUDE.md, section 6.4 ; ADR 0018). Il vit dans ce module, qui détient l'XP
 * et l'activité ; son adresse le range sous le groupe.
 */
@Path("/api/v1/groups/{groupId}/leaderboard")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Groupes", description = "Groupes de révision et leurs membres")
@SecurityRequirement(name = "SecurityScheme")
public class LeaderboardResource {

    private final GroupLeaderboardService leaderboards;
    private final CurrentUser currentUser;

    public LeaderboardResource(GroupLeaderboardService leaderboards, CurrentUser currentUser) {
        this.leaderboards = leaderboards;
        this.currentUser = currentUser;
    }

    @GET
    @Operation(
            operationId = "getGroupLeaderboard",
            summary = "Le classement de la semaine en cours : la régularité d'abord, puis l'XP")
    @APIResponse(responseCode = "200", description = "Le classement ; aucune ligne s'il est désactivé")
    @APIResponse(
            responseCode = "404",
            description = "Groupe inconnu, ou dont l'utilisateur n'est pas membre",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public LeaderboardDto getGroupLeaderboard(@PathParam("groupId") UUID groupId) {
        return LeaderboardDto.from(leaderboards.currentWeek(currentUser.get(), groupId));
    }

    @Schema(name = "Leaderboard")
    public record LeaderboardDto(
            @Schema(description = "Lundi de la semaine, dans le fuseau de l'utilisateur")
            LocalDate weekStart,

            @Schema(description = "Dimanche de la semaine") LocalDate weekEnd,

            @Schema(description = "Cartes à réviser dans la journée pour qu'elle compte comme régulière")
            int dailyGoal,

            LeaderboardVisibility visibility,

            @Schema(description = "Du premier au dernier ; les ex æquo partagent leur rang")
            List<LeaderboardEntryDto> entries) {

        static LeaderboardDto from(Leaderboard leaderboard) {
            return new LeaderboardDto(
                    leaderboard.week().monday(),
                    leaderboard.week().sunday(),
                    leaderboard.dailyGoal(),
                    leaderboard.visibility(),
                    leaderboard.entries().stream()
                            .map(LeaderboardEntryDto::from)
                            .toList());
        }
    }

    @Schema(name = "LeaderboardEntry")
    public record LeaderboardEntryDto(
            int rank,

            @Schema(description = "Absent dans un classement anonymisé, sauf pour l'utilisateur connecté") @Nullable
            String displayName,

            @Schema(description = "Vrai pour l'utilisateur connecté")
            boolean me,

            @Schema(description = "Jours de la semaine où l'objectif du groupe est atteint")
            int regularDays,

            @Schema(description = "XP gagnée dans la semaine (ADR 0035)")
            long xp,

            int cardsReviewed) {

        static LeaderboardEntryDto from(Entry entry) {
            return new LeaderboardEntryDto(
                    entry.rank(),
                    entry.displayName(),
                    entry.me(),
                    entry.regularDays(),
                    entry.xp(),
                    entry.cardsReviewed());
        }
    }
}
