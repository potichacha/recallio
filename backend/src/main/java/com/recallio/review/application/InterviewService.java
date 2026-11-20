package com.recallio.review.application;

import com.recallio.content.NotionInfo;
import com.recallio.content.SubjectDirectory;
import com.recallio.review.domain.ReviewStateRepository;
import com.recallio.review.domain.interview.InterviewCountdown;
import com.recallio.review.domain.interview.InterviewExceptions.InterviewSubjectNotFollowedException;
import com.recallio.review.domain.interview.InterviewExceptions.NoEssentialNotionsException;
import com.recallio.review.domain.interview.InterviewPlan;
import com.recallio.review.domain.interview.InterviewPlanRepository;
import com.recallio.review.domain.level.KnownNotionRepository;
import com.recallio.review.domain.track.NotionMastery;
import com.recallio.review.domain.track.Readiness;
import com.recallio.user.FollowedSubjects;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cas d'usage : le compte à rebours avant un entretien (ADR 0066), sur les sujets suivis. L'utilisateur ne donne que la
 * date ; le plan se recalcule à chaque lecture, d'après la maîtrise des notions, comme le tableau de bord. Un sujet qu'on
 * ne suit plus garde sa date, sans la montrer : elle revient si on le suit de nouveau.
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class InterviewService {

    private final InterviewPlanRepository plans;
    private final FollowedSubjects followedSubjects;
    private final SubjectDirectory subjects;
    private final ReviewStateRepository states;
    private final NotionMasteries masteries;
    private final KnownNotionRepository known;
    private final Clock clock;

    public InterviewService(
            InterviewPlanRepository plans,
            FollowedSubjects followedSubjects,
            SubjectDirectory subjects,
            ReviewStateRepository states,
            NotionMasteries masteries,
            KnownNotionRepository known,
            Clock clock) {
        this.plans = plans;
        this.followedSubjects = followedSubjects;
        this.subjects = subjects;
        this.states = states;
        this.masteries = masteries;
        this.known = known;
        this.clock = clock;
    }

    /** Les comptes à rebours des sujets suivis qui ont une date, dans l'ordre du catalogue. */
    public List<SubjectCountdown> countdowns(UserRef user) {
        Map<String, InterviewPlan> planned = plans.findAll(user.id());
        List<InterviewPlan> withDate = new ArrayList<>();
        for (String slug : followedSubjects.of(user)) {
            InterviewPlan plan = planned.get(slug);
            if (plan != null) {
                withDate.add(plan);
            }
        }
        if (withDate.isEmpty()) {
            return List.of();
        }
        List<String> slugs = withDate.stream().map(InterviewPlan::subjectSlug).toList();
        Map<String, Map<NotionInfo, NotionMastery>> notions = notionsBySubject(user, slugs);
        Set<UUID> knownNotions = known.known(user.id(), slugs);
        LocalDate today = today(user);
        Map<String, String> titles = subjects.publishedTitles();
        return withDate.stream()
                .map(plan -> countdown(
                        plan, today, notions.getOrDefault(plan.subjectSlug(), Map.of()), knownNotions, titles))
                .toList();
    }

    /**
     * Fixe ou change la date de l'entretien d'un sujet suivi : le plan repart d'aujourd'hui, avec les notions
     * essentielles qui restent à acquérir.
     *
     * @throws com.recallio.review.domain.interview.InterviewExceptions.InvalidInterviewDateException si la date n'est
     *     pas après aujourd'hui, ou trop loin
     * @throws InterviewSubjectNotFollowedException si l'utilisateur ne suit pas ce sujet
     * @throws NoEssentialNotionsException si le sujet n'a aucune notion essentielle
     */
    @Transactional
    public SubjectCountdown plan(UserRef user, String subjectSlug, LocalDate interviewOn) {
        LocalDate today = today(user);
        InterviewPlan.requireValidDate(interviewOn, today);
        if (!followedSubjects.of(user).contains(subjectSlug)) {
            throw new InterviewSubjectNotFollowedException(subjectSlug);
        }
        Map<NotionInfo, NotionMastery> notions =
                notionsBySubject(user, List.of(subjectSlug)).getOrDefault(subjectSlug, Map.of());
        if (essentials(notions).isEmpty()) {
            throw new NoEssentialNotionsException(subjectSlug);
        }
        Set<UUID> knownNotions = known.known(user.id(), Set.of(subjectSlug));
        var plan = new InterviewPlan(
                subjectSlug,
                interviewOn,
                today,
                InterviewCountdown.toAcquire(notions, knownNotions).size());
        plans.save(user.id(), plan);
        return countdown(plan, today, notions, knownNotions, subjects.publishedTitles());
    }

    /** Retire la date de l'entretien d'un sujet, s'il en avait une. */
    @Transactional
    public void cancel(UserRef user, String subjectSlug) {
        plans.delete(user.id(), subjectSlug);
    }

    /** @param knownNotions les notions connues au test de niveau (ADR 0068) : le plan les met après les autres */
    private static SubjectCountdown countdown(
            InterviewPlan plan,
            LocalDate today,
            Map<NotionInfo, NotionMastery> notions,
            Set<UUID> knownNotions,
            Map<String, String> titles) {
        Readiness readiness = Readiness.of(essentials(notions));
        return new SubjectCountdown(
                titles.getOrDefault(plan.subjectSlug(), plan.subjectSlug()),
                InterviewCountdown.of(plan, today, InterviewCountdown.toAcquire(notions, knownNotions), readiness));
    }

    /** La maîtrise des notions de ces sujets, sujet par sujet : les mêmes lectures que le tableau de bord. */
    private Map<String, Map<NotionInfo, NotionMastery>> notionsBySubject(UserRef user, Collection<String> slugs) {
        return masteries.of(user.id(), Set.copyOf(slugs), states.findAllByUser(user.id())).entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getKey().subjectSlug(),
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    private static List<NotionMastery> essentials(Map<NotionInfo, NotionMastery> notions) {
        return notions.entrySet().stream()
                .filter(entry -> entry.getKey().priority() == Readiness.ESSENTIAL_PRIORITY)
                .map(Map.Entry::getValue)
                .toList();
    }

    private LocalDate today(UserRef user) {
        return LocalDate.now(clock.withZone(user.timezone()));
    }

    /** Le compte à rebours d'un sujet, avec son titre. */
    public record SubjectCountdown(String subjectTitle, InterviewCountdown countdown) {}
}
