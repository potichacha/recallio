package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.CardType;
import com.recallio.content.NotionInfo;
import com.recallio.content.SubjectDirectory;
import com.recallio.group.GroupReviewScopes;
import com.recallio.group.GroupReviewScopes.GroupReviewScope;
import com.recallio.review.domain.LearningState;
import com.recallio.review.domain.Rating;
import com.recallio.review.domain.ReviewLogRepository;
import com.recallio.review.domain.ReviewState;
import com.recallio.review.domain.ReviewStateRepository;
import com.recallio.review.domain.fsrs.FsrsScheduler;
import com.recallio.review.domain.queue.ReviewQueuePlanner;
import com.recallio.review.domain.queue.ReviewQueuePlanner.Candidate;
import com.recallio.review.domain.queue.ReviewQueuePlanner.QueuePlan;
import com.recallio.review.domain.track.LearningPath;
import com.recallio.review.domain.track.NewCardOrder;
import com.recallio.user.FollowedSubjects;
import com.recallio.user.LearningTrack;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Cas d'usage : quelles cartes réviser maintenant (CLAUDE.md, section 6.1). Sans filtre, les cartes viennent des sujets
 * suivis ; un sujet ou un thème demandé explicitement est révisable même s'il n'est pas suivi. Avec un groupe, elles
 * viennent de ses réglages (ADR 0017).
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class ReviewQueueService {

    private final CardDirectory cards;
    private final SubjectDirectory subjects;
    private final FollowedSubjects followedSubjects;
    private final GroupReviewScopes groupScopes;
    private final ReviewStateRepository states;
    private final ReviewLogRepository log;
    private final FsrsScheduler scheduler;
    private final NewCardQuota quota;
    private final Clock clock;

    public ReviewQueueService(
            CardDirectory cards,
            SubjectDirectory subjects,
            FollowedSubjects followedSubjects,
            GroupReviewScopes groupScopes,
            ReviewStateRepository states,
            ReviewLogRepository log,
            FsrsScheduler scheduler,
            NewCardQuota quota,
            Clock clock) {
        this.cards = cards;
        this.subjects = subjects;
        this.followedSubjects = followedSubjects;
        this.groupScopes = groupScopes;
        this.states = states;
        this.log = log;
        this.scheduler = scheduler;
        this.quota = quota;
        this.clock = clock;
    }

    public ReviewQueue queue(UserRef user, QueueFilter filter, int size) {
        Instant now = clock.instant();
        Map<UUID, ReviewState> memory = states.findAllByUser(user.id());
        List<Candidate> eligible = arranged(
                user,
                filter,
                cards.activeCards(cardFilter(user, filter)).stream()
                        .map(card -> new Candidate(card.cardId(), card.subjectSlug(), card.notionId()))
                        .toList(),
                memory.keySet());
        QueuePlan plan = ReviewQueuePlanner.plan(
                eligible, memory, reviewedToday(user, now), now, quota.remainingToday(user, now), size);
        List<QueuedCard> queued = cards.currentVersions(plan.cardIds()).stream()
                .map(card -> queuedCard(card, memory.getOrDefault(card.cardId(), ReviewState.newCard(now)), now))
                .toList();
        return new ReviewQueue(queued, plan.dueCount(), plan.newCount(), plan.nextDueAt());
    }

    /**
     * Les cartes rangées selon le parcours de leur sujet (ADR 0032). Avec un groupe, chaque sujet suit le parcours par
     * défaut : le groupe choisit ses cartes, pas le parcours de chacun.
     */
    private List<Candidate> arranged(UserRef user, QueueFilter filter, List<Candidate> eligible, Set<UUID> known) {
        Map<String, LearningTrack> tracks = filter.group() == null ? followedSubjects.tracksOf(user) : Map.of();
        List<NotionInfo> notions =
                cards.notions(eligible.stream().map(Candidate::subjectSlug).collect(Collectors.toSet()));
        Set<UUID> locked = tracks.containsValue(LearningTrack.LEARN)
                ? learningPath(user, notions).locked()
                : Set.of();
        return NewCardOrder.arrange(
                eligible,
                known,
                tracks,
                notions.stream().collect(Collectors.toMap(NotionInfo::id, Function.identity())),
                locked);
    }

    /** Le chemin d'apprentissage de l'utilisateur : ses cartes réussies, comptées par notion. */
    private LearningPath learningPath(UserRef user, List<NotionInfo> notions) {
        Map<UUID, Integer> recalled = cards.notionsOf(log.cardsRecalled(user.id())).values().stream()
                .collect(Collectors.toMap(Function.identity(), _ -> 1, Integer::sum));
        return new LearningPath(notions, recalled);
    }

    /** Les cartes révisées depuis minuit, dans le fuseau de l'utilisateur, par notion : leurs sœurs attendent demain. */
    private Map<UUID, Set<UUID>> reviewedToday(UserRef user, Instant now) {
        Set<UUID> reviewed = log.cardsReviewedSince(user.id(), NewCardQuota.startOfDay(now, user.timezone()));
        return cards.notionsOf(reviewed).entrySet().stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toSet())));
    }

    private CardFilter cardFilter(UserRef user, QueueFilter filter) {
        UUID group = filter.group();
        if (group != null) {
            return groupFilter(groupScopes.of(user, group), filter);
        }
        Set<CardType> types = filter.type() == null ? Set.of() : Set.of(filter.type());
        String subject = filter.subject();
        if (subject != null) {
            subjects.requirePublished(subject);
            return new CardFilter(Set.of(subject), filter.theme(), Set.of(), types);
        }
        if (filter.theme() != null) {
            return new CardFilter(null, filter.theme(), Set.of(), types);
        }
        return new CardFilter(Set.copyOf(followedSubjects.of(user)), null, Set.of(), types);
    }

    /** Les cartes du groupe ; un sujet, un thème ou un format demandé en plus restreint encore la file. */
    private static CardFilter groupFilter(GroupReviewScope scope, QueueFilter filter) {
        Set<String> groupSubjects = scope.subjectSlugs();
        String subject = filter.subject();
        CardType type = filter.type();
        boolean typeOutsideGroup = type != null
                && !scope.cardTypes().isEmpty()
                && !scope.cardTypes().contains(type);
        if ((subject != null && !groupSubjects.contains(subject)) || typeOutsideGroup) {
            return new CardFilter(Set.of(), null, Set.of(), Set.of());
        }
        return new CardFilter(
                subject == null ? groupSubjects : Set.of(subject),
                filter.theme(),
                scope.themeSlugs(),
                type == null ? scope.cardTypes() : Set.of(type));
    }

    private QueuedCard queuedCard(CardSnapshot card, ReviewState state, Instant now) {
        List<RatingPreview> previews = Arrays.stream(Rating.values())
                .map(rating -> new RatingPreview(
                        rating, scheduler.review(state, rating, now).dueAt()))
                .toList();
        return new QueuedCard(card, state.state(), previews);
    }

    /**
     * @param group les cartes de ce groupe, dont l'utilisateur est membre, au lieu de ses sujets suivis
     * @param subject restreint à un sujet, suivi ou non
     * @param theme restreint à un thème, suivi ou non
     * @param type restreint à un format de carte
     */
    public record QueueFilter(
            @Nullable UUID group,
            @Nullable String subject,
            @Nullable String theme,
            @Nullable CardType type) {

        /** Aucun filtre : les cartes des sujets suivis. */
        public static QueueFilter followedSubjects() {
            return new QueueFilter(null, null, null, null);
        }
    }

    /**
     * @param dueCount cartes dues au total (la file peut en contenir moins)
     * @param newCount cartes nouvelles encore disponibles aujourd'hui
     * @param nextDueAt prochaine échéance, quand plus rien n'est dû
     */
    public record ReviewQueue(
            List<QueuedCard> cards,
            int dueCount,
            int newCount,
            @Nullable Instant nextDueAt) {}

    /** Une carte à réviser, avec l'échéance que donnerait chaque note (affichée sur les boutons). */
    public record QueuedCard(CardSnapshot card, LearningState state, List<RatingPreview> previews) {}

    public record RatingPreview(Rating rating, Instant dueAt) {}
}
