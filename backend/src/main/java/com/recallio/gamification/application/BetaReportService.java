package com.recallio.gamification.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.SubjectDirectory;
import com.recallio.gamification.domain.beta.BetaReport;
import com.recallio.gamification.domain.beta.BetaReport.Window;
import com.recallio.gamification.domain.beta.CardLabel;
import com.recallio.gamification.domain.beta.DayActivity;
import com.recallio.gamification.domain.beta.FailedCard;
import com.recallio.gamification.domain.beta.FeatureUsage;
import com.recallio.gamification.domain.beta.ReportedCard;
import com.recallio.gamification.domain.beta.SubjectActivity;
import com.recallio.gamification.domain.beta.TourUsage;
import com.recallio.platform.domain.UsageFeature;
import com.recallio.platform.domain.UsageStatistics;
import com.recallio.platform.domain.UsageStatistics.CardAnswers;
import com.recallio.platform.domain.UsageStatistics.CardReports;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cas d'usage : le rapport de la page « Bêta » des admins (ADR 0076). Chaque module répond de ses tables par son
 * {@link UsageStatistics} ; ce service rassemble les réponses et nomme le contenu par les façades du module content.
 * Il ne connaît aucune table.
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class BetaReportService {

    private final Instance<UsageStatistics> statistics;
    private final CardDirectory cards;
    private final SubjectDirectory subjects;
    private final Clock clock;

    public BetaReportService(
            Instance<UsageStatistics> statistics, CardDirectory cards, SubjectDirectory subjects, Clock clock) {
        this.statistics = statistics;
        this.cards = cards;
        this.subjects = subjects;
        this.clock = clock;
    }

    /** Les {@link BetaReport#DAYS} derniers jours, aujourd'hui compris. */
    public BetaReport report() {
        Window window = Window.endingAt(clock.instant());
        Instant from = window.from();
        Instant to = window.to();
        Map<String, String> titles = subjects.publishedTitles();
        Map<UsageFeature, Set<UUID>> featureUsers = featureUsers(from, to);
        return new BetaReport(
                window,
                DayActivity.series(window, summed(module -> module.dailyCounts(from, to))),
                SubjectActivity.ranked(titles, summed(module -> module.subjectCounts(from, to))),
                failedCards(from, to),
                reportedCards(),
                tour(featureUsers, titles),
                FeatureUsage.of(featureUsers));
    }

    /** Un même compte peut venir de deux modules : les ensembles se réunissent, ils ne s'additionnent pas. */
    private Map<UsageFeature, Set<UUID>> featureUsers(Instant from, Instant to) {
        Map<UsageFeature, Set<UUID>> users = new EnumMap<>(UsageFeature.class);
        for (UsageStatistics module : statistics) {
            module.featureUsers(from, to)
                    .forEach((feature, accounts) ->
                            users.computeIfAbsent(feature, _ -> new HashSet<>()).addAll(accounts));
        }
        return users;
    }

    /** Les comptes de tous les modules ; si deux modules répondent à la même question, leurs comptes s'ajoutent. */
    private <Q, K> Map<Q, Map<K, Long>> summed(Function<UsageStatistics, Map<Q, Map<K, Long>>> question) {
        Map<Q, Map<K, Long>> counts = new HashMap<>();
        for (UsageStatistics module : statistics) {
            question.apply(module)
                    .forEach((which, byKey) -> byKey.forEach((key, count) ->
                            counts.computeIfAbsent(which, _ -> new HashMap<>()).merge(key, count, Long::sum)));
        }
        return counts;
    }

    private List<FailedCard> failedCards(Instant from, Instant to) {
        List<CardAnswers> failed = new ArrayList<>();
        for (UsageStatistics module : statistics) {
            failed.addAll(module.mostFailedCards(from, to, BetaReport.MIN_ANSWERS, BetaReport.FAILED_CARDS));
        }
        List<UUID> cardIds = failed.stream().map(CardAnswers::cardId).toList();
        Function<UUID, CardLabel> label = labeller(cardIds);
        Map<UUID, Long> openReports = new HashMap<>();
        for (UsageStatistics module : statistics) {
            module.openReports(cardIds).forEach((cardId, count) -> openReports.merge(cardId, count, Long::sum));
        }
        return failed.stream()
                .map(card -> new FailedCard(
                        label.apply(card.cardId()),
                        card.answers(),
                        card.failures(),
                        openReports.getOrDefault(card.cardId(), 0L)))
                .toList();
    }

    private List<ReportedCard> reportedCards() {
        List<CardReports> reported = new ArrayList<>();
        for (UsageStatistics module : statistics) {
            reported.addAll(module.mostReportedCards(BetaReport.REPORTED_CARDS));
        }
        Function<UUID, CardLabel> label =
                labeller(reported.stream().map(CardReports::cardId).toList());
        return reported.stream()
                .map(card -> new ReportedCard(label.apply(card.cardId()), card.openReports()))
                .toList();
    }

    /** De quoi reconnaître chaque carte : sa version courante si elle est active, son sujet dans tous les cas. */
    private Function<UUID, CardLabel> labeller(Collection<UUID> cardIds) {
        Map<UUID, CardSnapshot> snapshots = cards.currentVersions(cardIds).stream()
                .collect(Collectors.toMap(CardSnapshot::cardId, Function.identity()));
        Map<UUID, String> subjectSlugs = cards.subjectsOf(cardIds);
        return cardId -> CardLabel.of(cardId, snapshots.get(cardId), subjectSlugs.get(cardId));
    }

    private TourUsage tour(Map<UsageFeature, Set<UUID>> featureUsers, Map<String, String> titles) {
        Map<UUID, Long> stops = new HashMap<>();
        for (UsageStatistics module : statistics) {
            module.tourStops().forEach((notionId, accounts) -> stops.merge(notionId, accounts, Long::sum));
        }
        int learners = featureUsers.getOrDefault(UsageFeature.TOUR, Set.of()).size();
        return TourUsage.of(learners, cards.notions(titles.keySet()), stops, titles);
    }
}
