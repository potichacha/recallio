package com.recallio.feedback.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.NotionInfo;
import com.recallio.feedback.domain.ContentReport;
import com.recallio.feedback.domain.ContentReportRepository;
import com.recallio.feedback.domain.FeedbackExceptions.ReportAlreadyOpenException;
import com.recallio.feedback.domain.FeedbackExceptions.ReportAlreadyResolvedException;
import com.recallio.feedback.domain.FeedbackExceptions.ReportNotFoundException;
import com.recallio.feedback.domain.FeedbackExceptions.ReportTargetNotFoundException;
import com.recallio.feedback.domain.FeedbackExceptions.TooManyOpenReportsException;
import com.recallio.feedback.domain.ReportReason;
import com.recallio.feedback.domain.ReportResolution;
import com.recallio.feedback.domain.ReportTarget;
import com.recallio.feedback.domain.ReportTarget.CardTarget;
import com.recallio.feedback.domain.ReportTarget.TourPageTarget;
import com.recallio.platform.domain.IdGenerator;
import com.recallio.platform.domain.Page;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;
import org.jspecify.annotations.Nullable;

/**
 * Cas d'usage : signaler une erreur dans une carte ou une page du Tour, puis la traiter, pour un admin (ADR 0042). Seul
 * le contenu publié se signale : c'est celui que voient les utilisateurs.
 */
@ApplicationScoped
public class ContentReportService {

    /** Au-delà, un utilisateur attend que ses signalements soient traités : de quoi décourager le spam. */
    static final int MAX_OPEN_REPORTS = 20;

    /** La longueur de l'extrait d'une carte montré aux admins. */
    static final int EXCERPT_LENGTH = 200;

    private static final Logger LOG = Logger.getLogger(ContentReportService.class);

    private final ContentReportRepository reports;
    private final CardDirectory cards;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public ContentReportService(
            ContentReportRepository reports, CardDirectory cards, IdGenerator idGenerator, Clock clock) {
        this.reports = reports;
        this.cards = cards;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    /**
     * @param comment ce que l'utilisateur a remarqué ; vide s'il n'a rien précisé
     * @throws ReportTargetNotFoundException si le contenu n'existe pas, est retiré, ou appartient à un brouillon
     * @throws ReportAlreadyOpenException si l'utilisateur l'a déjà signalé et que ce signalement est ouvert
     * @throws TooManyOpenReportsException s'il a déjà {@value #MAX_OPEN_REPORTS} signalements ouverts
     */
    @Transactional
    public ContentReport report(UserRef user, ReportTarget target, ReportReason reason, String comment) {
        String subjectSlug = subjectOf(target);
        if (reports.hasOpenReport(user.id(), target)) {
            throw new ReportAlreadyOpenException();
        }
        if (reports.openReportsBy(user.id()) >= MAX_OPEN_REPORTS) {
            throw new TooManyOpenReportsException(MAX_OPEN_REPORTS);
        }
        var report = new ContentReport(
                idGenerator.newId(),
                user.id(),
                subjectSlug,
                target,
                reason,
                comment.strip(),
                clock.instant(),
                null,
                null);
        reports.add(report);
        LOG.infof("Signalement %s (%s) sur le sujet %s", report.id(), reason, subjectSlug);
        return report;
    }

    /** Les signalements ouverts, les plus anciens d'abord, ou traités, les derniers traités d'abord. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<ReportView> list(boolean open, int page, int size) {
        Page<ContentReport> found = reports.list(open, page, size);
        var cardIds = found.items().stream()
                .map(ContentReport::target)
                .filter(CardTarget.class::isInstance)
                .map(target -> ((CardTarget) target).cardId())
                .toList();
        Map<UUID, CardSnapshot> snapshots = cards.currentVersions(cardIds).stream()
                .collect(Collectors.toMap(CardSnapshot::cardId, Function.identity()));
        return found.map(report -> view(report, snapshots));
    }

    /**
     * @throws ReportNotFoundException si le signalement n'existe pas
     * @throws ReportAlreadyResolvedException s'il a déjà été traité
     */
    @Transactional
    public ReportView resolve(UUID id, ReportResolution resolution) {
        // Une seule instruction, qui ne touche qu'un signalement ouvert : deux admins ne le traitent pas deux fois.
        if (!reports.resolve(id, resolution, clock.instant())) {
            if (reports.find(id).isEmpty()) {
                throw new ReportNotFoundException(id);
            }
            throw new ReportAlreadyResolvedException(id);
        }
        ContentReport resolved = reports.find(id).orElseThrow(() -> new ReportNotFoundException(id));
        LOG.infof("Signalement %s traité : %s", id, resolution);
        var snapshots = resolved.target() instanceof CardTarget(UUID cardId)
                ? cards.activeCard(cardId).map(card -> Map.of(cardId, card)).orElse(Map.of())
                : Map.<UUID, CardSnapshot>of();
        return view(resolved, snapshots);
    }

    private String subjectOf(ReportTarget target) {
        return switch (target) {
            case CardTarget(UUID cardId) ->
                cards.activeCard(cardId).map(CardSnapshot::subjectSlug).orElseThrow(ReportTargetNotFoundException::new);
            case TourPageTarget(String subjectSlug, String notionSlug, int page) ->
                cards.notion(subjectSlug, notionSlug)
                        .filter(notion -> page <= notion.tourPages())
                        .map(NotionInfo::subjectSlug)
                        .orElseThrow(ReportTargetNotFoundException::new);
        };
    }

    private ReportView view(ContentReport report, Map<UUID, CardSnapshot> snapshots) {
        return switch (report.target()) {
            case CardTarget(UUID cardId) -> {
                CardSnapshot card = snapshots.get(cardId);
                yield card == null
                        ? new ReportView(report, null, null, null)
                        : new ReportView(report, card.themeSlug(), card.notionTitle(), excerpt(card.prompt()));
            }
            case TourPageTarget(String subjectSlug, String notionSlug, int _) ->
                new ReportView(
                        report,
                        null,
                        cards.notion(subjectSlug, notionSlug)
                                .map(NotionInfo::title)
                                .orElse(null),
                        null);
        };
    }

    private static String excerpt(String prompt) {
        String flat = prompt.strip().replaceAll("\\s+", " ");
        return flat.length() <= EXCERPT_LENGTH ? flat : flat.substring(0, EXCERPT_LENGTH - 1) + "…";
    }

    /**
     * Un signalement, avec de quoi retrouver le contenu signalé.
     *
     * @param themeSlug le thème de la carte, pour l'ouvrir dans le catalogue ; {@code null} pour une page, ou une carte
     *     retirée depuis
     * @param notionTitle le titre de la notion de la carte ou de la page ; {@code null} si elle n'en a pas, ou plus
     * @param excerpt le début de la question de la carte ; {@code null} pour une page, ou une carte retirée depuis
     */
    public record ReportView(
            ContentReport report,
            @Nullable String themeSlug,
            @Nullable String notionTitle,
            @Nullable String excerpt) {}
}
