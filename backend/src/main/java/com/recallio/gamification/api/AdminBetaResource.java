package com.recallio.gamification.api;

import com.recallio.content.CardType;
import com.recallio.gamification.application.BetaReportService;
import com.recallio.gamification.domain.beta.BetaReport;
import com.recallio.gamification.domain.beta.CardLabel;
import com.recallio.gamification.domain.beta.DayActivity;
import com.recallio.gamification.domain.beta.FailedCard;
import com.recallio.gamification.domain.beta.FeatureUsage;
import com.recallio.gamification.domain.beta.FeatureUsage.FeatureAdoption;
import com.recallio.gamification.domain.beta.ReportedCard;
import com.recallio.gamification.domain.beta.SubjectActivity;
import com.recallio.gamification.domain.beta.TourUsage;
import com.recallio.gamification.domain.beta.TourUsage.ChapterStops;
import com.recallio.gamification.domain.beta.TourUsage.SubjectStops;
import com.recallio.platform.domain.UsageFeature;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/**
 * La page « Bêta » des admins (ADR 0076), à côté de la rétention et du parcours : ce que font les testeurs sur les
 * trente derniers jours. Des comptes et des parts, jamais une personne : aucun identifiant d'utilisateur, aucun nom.
 */
@Path("/api/v1/admin/beta")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(AdminRetentionResource.ADMIN_ROLE)
@Tag(name = "Administration")
@SecurityRequirement(name = "SecurityScheme")
public class AdminBetaResource {

    private final BetaReportService beta;

    public AdminBetaResource(BetaReportService beta) {
        this.beta = beta;
    }

    @GET
    @Operation(
            operationId = "getBetaReport",
            summary = "L'activité, les sujets, les cartes ratées, le Tour et les fonctionnalités des trente derniers"
                    + " jours")
    @APIResponse(responseCode = "200", description = "Le rapport de la bêta")
    @APIResponse(responseCode = "403", description = "Réservé aux admins")
    public BetaReportDto getBetaReport() {
        return BetaReportDto.from(beta.report());
    }

    @Schema(name = "BetaReport")
    public record BetaReportDto(
            @Schema(description = "Le premier jour de la fenêtre, en UTC")
            LocalDate from,

            @Schema(description = "Le dernier jour de la fenêtre : aujourd'hui, en UTC")
            LocalDate to,

            @Schema(description = "L'activité de chaque jour de la fenêtre, du plus ancien au plus récent")
            List<BetaDayDto> days,

            @Schema(description = "Chaque sujet publié, et tout autre sujet suivi ou révisé, le plus révisé d'abord")
            List<BetaSubjectDto> subjects,

            @Schema(description = "Les cartes au plus fort taux d'échec sur la fenêtre, la plus ratée d'abord")
            List<BetaFailedCardDto> failedCards,

            @Schema(description = "Les cartes qui ont le plus de signalements ouverts, la plus signalée d'abord")
            List<BetaReportedCardDto> reportedCards,

            BetaTourDto tour,
            BetaFeaturesDto features) {

        static BetaReportDto from(BetaReport report) {
            return new BetaReportDto(
                    report.window().first(),
                    report.window().last(),
                    report.days().stream().map(BetaDayDto::from).toList(),
                    report.subjects().stream().map(BetaSubjectDto::from).toList(),
                    report.failedCards().stream().map(BetaFailedCardDto::from).toList(),
                    report.reportedCards().stream()
                            .map(BetaReportedCardDto::from)
                            .toList(),
                    BetaTourDto.from(report.tour()),
                    BetaFeaturesDto.from(report.features()));
        }
    }

    @Schema(name = "BetaDay")
    public record BetaDayDto(
            LocalDate day,

            @Schema(description = "Les comptes qui ont révisé au moins une carte, au jour de chacun dans son fuseau")
            long reviewers,

            @Schema(description = "Les cartes révisées, au jour de chacun dans son fuseau")
            long reviews,

            @Schema(description = "Les comptes qui ont atteint l'objectif du jour, au jour de chacun dans son fuseau")
            long goalsReached,

            @Schema(description = "Les comptes créés, au jour UTC ; un compte supprimé depuis n'est plus compté")
            long accountsCreated,

            @Schema(description = "Les notions validées dans le Tour, au jour UTC")
            long tourValidations) {

        static BetaDayDto from(DayActivity day) {
            return new BetaDayDto(
                    day.day(),
                    day.reviewers(),
                    day.reviews(),
                    day.goalsReached(),
                    day.accountsCreated(),
                    day.tourValidations());
        }
    }

    @Schema(name = "BetaSubject")
    public record BetaSubjectDto(
            @Schema(description = "Le slug du sujet") String subject,

            @Schema(description = "Son titre ; absent s'il n'est pas publié", nullable = true) @Nullable
            String title,

            @Schema(description = "Les comptes qui le suivent, à ce jour")
            long followers,

            @Schema(description = "Les comptes qui ont révisé au moins une de ses cartes sur la fenêtre")
            long reviewers,

            @Schema(description = "Les réponses données à ses cartes sur la fenêtre")
            long reviews,

            @Schema(
                    description = "La part des réponses justes, en pourcentage arrondi vers le bas ; absente sans"
                            + " réponse",
                    nullable = true)
            @Nullable
            Integer successPercent) {

        static BetaSubjectDto from(SubjectActivity subject) {
            return new BetaSubjectDto(
                    subject.slug(),
                    subject.title(),
                    subject.followers(),
                    subject.reviewers(),
                    subject.reviews(),
                    subject.successPercent());
        }
    }

    @Schema(
            name = "BetaCard",
            description = "De quoi reconnaître une carte ; une carte retirée, ou d'un sujet qui n'est plus publié,"
                    + " n'a plus que son identifiant et son sujet")
    public record BetaCardDto(
            UUID cardId,

            @Schema(description = "Le slug de son sujet ; absent si la carte est inconnue", nullable = true) @Nullable
            String subject,

            @Schema(nullable = true) @Nullable String subjectTitle,

            @Schema(description = "Son thème, pour l'ouvrir dans le catalogue", nullable = true) @Nullable
            String themeSlug,

            @Schema(description = "Le titre de sa notion", nullable = true) @Nullable
            String notionTitle,

            @Schema(description = "Le début de sa question", nullable = true) @Nullable
            String excerpt,

            @Schema(nullable = true) @Nullable CardType type) {

        static BetaCardDto from(CardLabel card) {
            return new BetaCardDto(
                    card.cardId(),
                    card.subjectSlug(),
                    card.subjectTitle(),
                    card.themeSlug(),
                    card.notionTitle(),
                    card.excerpt(),
                    card.type());
        }
    }

    @Schema(name = "BetaFailedCard")
    public record BetaFailedCardDto(
            BetaCardDto card,

            @Schema(description = "Les réponses données à la carte sur la fenêtre : au moins cinq")
            long answers,

            @Schema(description = "Celles qui sont fausses") long failures,

            @Schema(description = "La part des réponses fausses, en pourcentage arrondi vers le bas")
            int failurePercent,

            @Schema(description = "Ses signalements qui ne sont pas encore traités")
            long openReports) {

        static BetaFailedCardDto from(FailedCard card) {
            return new BetaFailedCardDto(
                    BetaCardDto.from(card.card()),
                    card.answers(),
                    card.failures(),
                    card.failurePercent(),
                    card.openReports());
        }
    }

    @Schema(name = "BetaReportedCard")
    public record BetaReportedCardDto(
            BetaCardDto card,

            @Schema(description = "Ses signalements qui ne sont pas encore traités")
            long openReports) {

        static BetaReportedCardDto from(ReportedCard card) {
            return new BetaReportedCardDto(BetaCardDto.from(card.card()), card.openReports());
        }
    }

    @Schema(name = "BetaTour")
    public record BetaTourDto(
            @Schema(description = "Les comptes qui ont validé au moins une notion du Tour sur la fenêtre")
            int learners,

            @Schema(
                    description = "Où chacun s'est arrêté, à ce jour : les sujets publiés où au moins un compte a"
                            + " ouvert une page. L'ouverture d'une page n'a pas d'historique : seule la dernière de"
                            + " chacun par sujet est gardée")
            List<BetaTourSubjectDto> stops) {

        static BetaTourDto from(TourUsage tour) {
            return new BetaTourDto(
                    tour.learners(),
                    tour.stops().stream().map(BetaTourSubjectDto::from).toList());
        }
    }

    @Schema(name = "BetaTourSubject")
    public record BetaTourSubjectDto(
            @Schema(description = "Le slug du sujet") String subject,

            String title,

            @Schema(description = "Tous les chapitres de son Tour, dans l'ordre du référentiel")
            List<BetaTourChapterDto> chapters) {

        static BetaTourSubjectDto from(SubjectStops stops) {
            return new BetaTourSubjectDto(
                    stops.subjectSlug(),
                    stops.subjectTitle(),
                    stops.chapters().stream().map(BetaTourChapterDto::from).toList());
        }
    }

    @Schema(name = "BetaTourChapter")
    public record BetaTourChapterDto(
            String domainTitle,

            String chapterTitle,

            @Schema(description = "Les comptes dont la dernière page ouverte du sujet est dans ce chapitre")
            long accounts) {

        static BetaTourChapterDto from(ChapterStops chapter) {
            return new BetaTourChapterDto(chapter.domainTitle(), chapter.chapterTitle(), chapter.accounts());
        }
    }

    @Schema(name = "BetaFeatures")
    public record BetaFeaturesDto(
            @Schema(
                    description = "Les comptes actifs sur la fenêtre : ceux qui ont utilisé au moins une"
                            + " fonctionnalité datée (réviser, valider une notion du Tour, jouer un duel...)")
            int activeAccounts,

            @Schema(description = "Chaque fonctionnalité, toujours dans le même ordre")
            List<BetaFeatureDto> features) {

        static BetaFeaturesDto from(FeatureUsage usage) {
            return new BetaFeaturesDto(
                    usage.activeAccounts(),
                    usage.features().stream().map(BetaFeatureDto::from).toList());
        }
    }

    @Schema(name = "BetaFeature")
    public record BetaFeatureDto(
            UsageFeature feature,

            @Schema(
                    description = "Les comptes actifs qui l'ont utilisée au moins une fois sur la fenêtre ; pour une"
                            + " fonctionnalité qui n'est pas datée, ceux qui l'ont à ce jour")
            int accounts,

            @Schema(
                    description = "Leur part des comptes actifs, en pourcentage arrondi vers le bas ; absente quand"
                            + " aucun compte n'est actif",
                    nullable = true)
            @Nullable
            Integer percent,

            @Schema(
                    description = "Vrai si l'usage est daté et borné à la fenêtre ; faux s'il est compté à ce jour,"
                            + " faute de date : le compte à rebours, les rappels, le profil public et les groupes")
            boolean dated) {

        static BetaFeatureDto from(FeatureAdoption adoption) {
            return new BetaFeatureDto(
                    adoption.feature(),
                    adoption.accounts(),
                    adoption.percent(),
                    adoption.feature().isDated());
        }
    }
}
