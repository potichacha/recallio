package com.recallio.review.api;

import com.recallio.review.application.ReviewStatsService;
import com.recallio.review.application.ReviewStatsService.ReviewStats;
import com.recallio.review.application.ReviewStatsService.SubjectStats;
import com.recallio.review.application.ReviewStatsService.ThemeStats;
import com.recallio.review.domain.stats.CardCounts;
import com.recallio.review.domain.track.Readiness;
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
            summary = "Cartes dues et progression par sujet, puis par thème, sur les sujets suivis")
    @APIResponse(responseCode = "200", description = "Les statistiques")
    @APIResponse(responseCode = "401", description = "Non authentifié")
    public ReviewStatsDto getMyReviewStats() {
        return ReviewStatsDto.from(stats.stats(currentUser.get()));
    }

    @Schema(name = "ReviewStats")
    public record ReviewStatsDto(
            CardCountsDto overall,

            @Schema(description = "Nouvelles cartes encore proposées aujourd'hui, tous sujets suivis confondus")
            int newToday,

            @Schema(description = "Les sujets suivis, dans l'ordre du catalogue")
            List<SubjectStatsDto> subjects) {

        static ReviewStatsDto from(ReviewStats stats) {
            return new ReviewStatsDto(
                    CardCountsDto.from(stats.overall()),
                    stats.newToday(),
                    stats.subjects().stream().map(SubjectStatsDto::from).toList());
        }
    }

    @Schema(name = "SubjectStats")
    public record SubjectStatsDto(
            String slug,
            String title,
            CardCountsDto counts,

            @Schema(description = "Nouvelles cartes encore proposées aujourd'hui dans ce sujet (son quota, ADR 0048)")
            int newToday,

            @Schema(
                    description = "Les notions du sujet par niveau de maîtrise (ADR 0034) ; aucune pour un sujet sans"
                            + " référentiel")
            MasteryCountsDto notions,

            @Schema(description = "L'indice de préparation du sujet (ADR 0041)")
            ReadinessDto readiness,

            @Schema(description = "Les thèmes du sujet, dans l'ordre du document")
            List<ThemeStatsDto> themes) {
        static SubjectStatsDto from(SubjectStats subject) {
            return new SubjectStatsDto(
                    subject.slug(),
                    subject.title(),
                    CardCountsDto.from(subject.counts()),
                    subject.newToday(),
                    MasteryCountsDto.from(subject.notions()),
                    ReadinessDto.from(subject.readiness()),
                    subject.themes().stream().map(ThemeStatsDto::from).toList());
        }
    }

    @Schema(
            name = "Readiness",
            description = "L'indice de préparation d'un sujet : une notion essentielle acquise compte pour moitié, une"
                    + " notion maîtrisée en entier")
    public record ReadinessDto(
            @Schema(description = "Les notions essentielles (priorité 1) du sujet ; aucune sans référentiel")
            int essentials,

            @Schema(description = "Les notions essentielles au moins acquises")
            int acquired,

            @Schema(description = "Les notions essentielles au moins maîtrisées")
            int mastered,

            @Schema(description = "L'indice, de 0 à 100") int percent,

            @Schema(description = "Vrai à partir de 80 : le sujet est prêt pour un entretien")
            boolean ready) {

        static ReadinessDto from(Readiness readiness) {
            return new ReadinessDto(
                    readiness.essentials(),
                    readiness.acquired(),
                    readiness.mastered(),
                    readiness.percent(),
                    readiness.ready());
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
