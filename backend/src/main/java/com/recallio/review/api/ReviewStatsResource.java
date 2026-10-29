package com.recallio.review.api;

import com.recallio.review.application.ReviewStatsService;
import com.recallio.review.application.ReviewStatsService.ReviewStats;
import com.recallio.review.application.ReviewStatsService.ThemeStats;
import com.recallio.review.domain.stats.CardCounts;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
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

/** Tableau de bord de révision de l'utilisateur connecté. */
@Path("/api/v1/me/review-stats")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Révision")
@SecurityRequirement(name = "SecurityScheme")
public class ReviewStatsResource {

    private final ReviewStatsService stats;
    private final CurrentUser currentUser;

    public ReviewStatsResource(ReviewStatsService stats, CurrentUser currentUser) {
        this.stats = stats;
        this.currentUser = currentUser;
    }

    @GET
    @Operation(
            operationId = "getMyReviewStats",
            summary = "Cartes dues et progression par thème de l'utilisateur connecté")
    @APIResponse(responseCode = "200", description = "Les statistiques")
    @APIResponse(responseCode = "401", description = "Non authentifié")
    public ReviewStatsDto getMyReviewStats() {
        return ReviewStatsDto.from(stats.stats(currentUser.get()));
    }

    @Schema(name = "ReviewStats")
    public record ReviewStatsDto(
            CardCountsDto overall,

            @Schema(description = "Nouvelles cartes encore proposées aujourd'hui")
            int newToday,

            List<ThemeStatsDto> themes) {

        static ReviewStatsDto from(ReviewStats stats) {
            return new ReviewStatsDto(
                    CardCountsDto.from(stats.overall()),
                    stats.newToday(),
                    stats.themes().stream().map(ThemeStatsDto::from).toList());
        }
    }

    @Schema(name = "ThemeStats")
    public record ThemeStatsDto(String slug, String title, CardCountsDto counts) {
        static ThemeStatsDto from(ThemeStats theme) {
            return new ThemeStatsDto(theme.slug(), theme.title(), CardCountsDto.from(theme.counts()));
        }
    }

    @Schema(name = "CardCounts")
    public record CardCountsDto(
            int total,

            @Schema(description = "Cartes révisées au moins une fois")
            int seen,

            @Schema(description = "Cartes mémorisées pour trois semaines ou plus")
            int mastered,

            @Schema(description = "Cartes à revoir maintenant")
            int dueNow,

            @Schema(description = "Cartes à revoir d'ici minuit, dont celles dues maintenant")
            int dueToday) {

        static CardCountsDto from(CardCounts counts) {
            return new CardCountsDto(
                    counts.total(), counts.seen(), counts.mastered(), counts.dueNow(), counts.dueToday());
        }
    }
}
