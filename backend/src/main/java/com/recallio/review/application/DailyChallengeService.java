package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardDirectory.CardFilter.Grading;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.NotionInfo;
import com.recallio.content.NotionInfo.Level;
import com.recallio.content.SubjectDirectory;
import com.recallio.platform.domain.IdGenerator;
import com.recallio.review.domain.RequestIdConflictException;
import com.recallio.review.domain.daily.DailyChallenge;
import com.recallio.review.domain.daily.DailyChallengeAnswer;
import com.recallio.review.domain.daily.DailyChallengeAttempt;
import com.recallio.review.domain.daily.DailyChallengeDraw;
import com.recallio.review.domain.daily.DailyChallengeDraw.Candidate;
import com.recallio.review.domain.daily.DailyChallengeExceptions.DailyChallengeAlreadyAnsweredException;
import com.recallio.review.domain.daily.DailyChallengeExceptions.DailyChallengeClosedException;
import com.recallio.review.domain.daily.DailyChallengeExceptions.DailyChallengeNotFoundException;
import com.recallio.review.domain.daily.DailyChallengeExceptions.DailyChallengeUnavailableException;
import com.recallio.review.domain.daily.DailyChallengeExceptions.NotAChallengeQuestionException;
import com.recallio.review.domain.daily.DailyChallengeRepository;
import com.recallio.review.domain.daily.DailyChallengeStats;
import com.recallio.user.FollowedSubjects;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Cas d'usage : le défi du jour (ADR 0067). Cinq questions par sujet publié et par jour, les mêmes pour tous, de
 * difficulté croissante ; une réponse par question, corrigée tout de suite ; le score, puis les chiffres du jour. Le jour
 * est celui de chaque joueur, dans son fuseau, et un défi commencé avant minuit se finit après.
 *
 * <p>Comme l'examen blanc (ADR 0044), le défi mesure et n'entraîne pas : ni le journal de révision, ni FSRS, ni l'XP,
 * ni la cote des cartes (ADR 0046) ne voient ses réponses. Aucun événement n'est publié.
 */
@ApplicationScoped
public class DailyChallengeService {

    private final DailyChallengeRepository challenges;
    private final CardDirectory cards;
    private final SubjectDirectory subjects;
    private final FollowedSubjects followedSubjects;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public DailyChallengeService(
            DailyChallengeRepository challenges,
            CardDirectory cards,
            SubjectDirectory subjects,
            FollowedSubjects followedSubjects,
            IdGenerator idGenerator,
            Clock clock) {
        this.challenges = challenges;
        this.cards = cards;
        this.subjects = subjects;
        this.followedSubjects = followedSubjects;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    /**
     * Le défi de chaque sujet publié qui a assez de questions, les sujets suivis d'abord : où en est le joueur, et son
     * score s'il l'a fini. Ne tire aucun défi : un sujet dont le défi n'est pas encore tiré n'est pas commencé.
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<ChallengeSummary> summaries(UserRef user) {
        LocalDate today = today(user);
        Map<String, String> titles = subjects.publishedTitles();
        Set<String> playable = playableSubjects(titles.keySet());
        List<DailyChallenge> recent = challenges.findByDays(List.of(today, today.minusDays(1)));
        Map<UUID, List<DailyChallengeAnswer>> answers =
                challenges
                        .answers(
                                user.id(),
                                recent.stream().map(DailyChallenge::id).toList())
                        .stream()
                        .collect(Collectors.groupingBy(DailyChallengeAnswer::challengeId));
        Set<UUID> activeCards = activeCards(recent.stream()
                .flatMap(challenge -> challenge.cardIds().stream())
                .toList());

        List<ChallengeSummary> summaries = new ArrayList<>();
        for (String slug : subjectsInOrder(user, titles.keySet())) {
            if (!playable.contains(slug)) {
                continue;
            }
            Optional<Attempted> started = find(recent, slug, today.minusDays(1))
                    .map(challenge -> attempted(challenge, activeCards, answers))
                    .filter(Attempted::toFinish);
            Optional<Attempted> current = started.or(
                    () -> find(recent, slug, today).map(challenge -> attempted(challenge, activeCards, answers)));
            summaries.add(new ChallengeSummary(
                    slug,
                    titles.getOrDefault(slug, slug),
                    current.map(attempted -> attempted.challenge().day()).orElse(today),
                    current.map(Attempted::attempt).orElse(null)));
        }
        return summaries;
    }

    /**
     * Le défi du jour du joueur dans ce sujet, tiré s'il ne l'est pas encore ; ou celui de la veille, s'il l'avait
     * commencé sans le finir.
     *
     * @throws com.recallio.platform.domain.ResourceNotFoundException si le sujet n'est pas publié
     * @throws DailyChallengeUnavailableException s'il n'a pas assez de questions pour un défi
     */
    @Transactional
    public ChallengeView challenge(UserRef user, String subjectSlug) {
        subjects.requirePublished(subjectSlug);
        LocalDate today = today(user);
        Optional<ChallengeView> yesterday = challenges
                .find(subjectSlug, today.minusDays(1))
                .map(challenge -> view(user, challenge))
                .filter(view -> view.attempt().isStarted() && !view.attempt().isDone());
        if (yesterday.isPresent()) {
            return yesterday.get();
        }
        DailyChallenge challenge = challenges.find(subjectSlug, today).orElseGet(() -> draw(subjectSlug, today));
        return view(user, challenge);
    }

    /**
     * Corrige la réponse du joueur à une question et la garde : une seule par question, définitive. La même requête
     * renvoyée rend la même correction.
     *
     * @throws DailyChallengeNotFoundException si le défi n'existe pas
     * @throws DailyChallengeClosedException si ce n'est ni le défi du jour du joueur, ni celui de la veille qu'il avait
     *     commencé
     * @throws NotAChallengeQuestionException si le défi n'a pas de question à cette position
     * @throws DailyChallengeAlreadyAnsweredException si la question a déjà sa réponse
     * @throws RequestIdConflictException si l'identifiant de requête a déjà servi pour une autre réponse
     * @throws com.recallio.review.domain.AnswerMismatchException si la réponse ne convient pas à la question
     */
    @Transactional
    public AnswerOutcome answer(UserRef user, UUID challengeId, ChallengeAnswer answer) {
        DailyChallenge challenge =
                challenges.find(challengeId).orElseThrow(() -> new DailyChallengeNotFoundException(challengeId));
        Optional<DailyChallengeAnswer> previous = challenges.findAnswer(answer.requestId());
        if (previous.isPresent()) {
            return replay(user, challenge, answer, previous.get());
        }
        ChallengeView view = view(user, challenge);
        requireOpen(user, view);
        CardSnapshot card = view.question(answer.position());
        if (view.attempt().isAnswered(answer.position())) {
            throw new DailyChallengeAlreadyAnsweredException(answer.position());
        }
        AnswerCorrection correction = AnswerCorrection.grade(card, answer.toGradedAnswer(card.cardId()));
        boolean added = challenges.addAnswerIfAbsent(new DailyChallengeAnswer(
                challengeId, user.id(), answer.position(), correction.correct(), clock.instant(), answer.requestId()));
        if (!added) {
            // Une requête jumelle est passée juste avant : la même requête rejouée, ou une autre réponse.
            Optional<DailyChallengeAnswer> twin = challenges.findAnswer(answer.requestId());
            if (twin.isPresent()) {
                return replay(user, challenge, answer, twin.get());
            }
            throw new DailyChallengeAlreadyAnsweredException(answer.position());
        }
        return outcome(view(user, challenge), answer.position(), correction);
    }

    /** La correction d'une réponse déjà gardée : son verdict fait foi, même si la carte a changé depuis. */
    private AnswerOutcome replay(
            UserRef user, DailyChallenge challenge, ChallengeAnswer answer, DailyChallengeAnswer previous) {
        if (!previous.answers(challenge.id(), user.id(), answer.position())) {
            throw new RequestIdConflictException(answer.requestId());
        }
        ChallengeView view = view(user, challenge);
        CardSnapshot card = view.question(answer.position());
        AnswerCorrection graded = AnswerCorrection.grade(card, answer.toGradedAnswer(card.cardId()));
        var correction = new AnswerCorrection(
                card.cardId(), previous.correct(), graded.correctChoices(), graded.blanks(), graded.explanation());
        return outcome(view, answer.position(), correction);
    }

    private AnswerOutcome outcome(ChallengeView view, int position, AnswerCorrection correction) {
        return new AnswerOutcome(position, correction, view.attempt(), view.stats());
    }

    /** Un défi se joue le jour du joueur ; celui de la veille, seulement s'il l'avait commencé. */
    private void requireOpen(UserRef user, ChallengeView view) {
        LocalDate today = today(user);
        LocalDate day = view.challenge().day();
        boolean open = day.equals(today)
                || (day.equals(today.minusDays(1)) && view.attempt().isStarted());
        if (!open) {
            throw new DailyChallengeClosedException();
        }
    }

    /** Tire le défi du jour d'un sujet, et le garde, à moins qu'une demande simultanée ne l'ait gardé avant. */
    private DailyChallenge draw(String subjectSlug, LocalDate day) {
        List<Candidate> candidates = candidates(subjectSlug);
        if (!DailyChallengeDraw.canDraw(candidates)) {
            throw new DailyChallengeUnavailableException(subjectSlug);
        }
        Set<UUID> recent = challenges.recentCards(subjectSlug, day, DailyChallenge.RECENT_CHALLENGES);
        UUID id = idGenerator.newId();
        long seed = id.getMostSignificantBits() ^ id.getLeastSignificantBits();
        return challenges.addIfAbsent(new DailyChallenge(
                id, subjectSlug, day, DailyChallengeDraw.draw(candidates, recent, seed), clock.instant()));
    }

    /** Les questions possibles d'un sujet : ses cartes que le serveur corrige sans exécuter de code, comme l'examen. */
    private List<Candidate> candidates(String subjectSlug) {
        Map<UUID, NotionInfo> notions = cards.notions(Set.of(subjectSlug)).stream()
                .collect(Collectors.toMap(NotionInfo::id, Function.identity()));
        return cards.activeCards(gradedWithoutRunningCode(Set.of(subjectSlug))).stream()
                .map(card -> {
                    UUID notionId = card.notionId();
                    NotionInfo notion = notionId == null ? null : notions.get(notionId);
                    return notion == null
                            ? new Candidate(card.cardId(), notionId, null, Level.INTERMEDIATE)
                            : new Candidate(card.cardId(), notionId, notion.domain(), notion.level());
                })
                .toList();
    }

    /**
     * Les sujets publiés qui ont assez de notions pour un défi. La liste est demandée à chaque accueil : on compte les
     * notions, sans lire les milliers de cartes des sujets. Depuis l'ADR 0063, toute carte d'un dossier de notions a ses
     * choix ; un sujet qui n'aurait pas assez de questions malgré ses notions est refusé au tirage
     * ({@link DailyChallengeUnavailableException}).
     */
    private Set<String> playableSubjects(Collection<String> published) {
        return cards.notions(published).stream()
                .collect(Collectors.groupingBy(NotionInfo::subjectSlug, Collectors.counting()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() >= DailyChallenge.QUESTIONS)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static CardFilter gradedWithoutRunningCode(Set<String> subjectSlugs) {
        return new CardFilter(subjectSlugs, null, Set.of(), Set.of(), Grading.CHOICES_OR_BLANKS);
    }

    /** Les sujets suivis d'abord, puis les autres, chacun dans l'ordre du catalogue. */
    private List<String> subjectsInOrder(UserRef user, Collection<String> catalogue) {
        Set<String> ordered = new LinkedHashSet<>(followedSubjects.of(user));
        ordered.retainAll(catalogue);
        ordered.addAll(catalogue);
        return List.copyOf(ordered);
    }

    private ChallengeView view(UserRef user, DailyChallenge challenge) {
        Map<UUID, CardSnapshot> active = cards.currentVersions(challenge.cardIds()).stream()
                .collect(Collectors.toMap(CardSnapshot::cardId, Function.identity()));
        SortedMap<Integer, CardSnapshot> questions = new TreeMap<>();
        for (int position = 0; position < challenge.cardIds().size(); position++) {
            CardSnapshot card = active.get(challenge.cardIds().get(position));
            if (card != null) {
                questions.put(position, card);
            }
        }
        var attempt = DailyChallengeAttempt.of(
                List.copyOf(questions.keySet()), challenges.answers(user.id(), List.of(challenge.id())));
        @Nullable
        DailyChallengeStats stats = attempt.isDone()
                ? DailyChallengeStats.of(challenges.playersByScore(challenge.id(), questions.keySet()), attempt.total())
                : null;
        String title = subjects.publishedTitles().getOrDefault(challenge.subjectSlug(), challenge.subjectSlug());
        return new ChallengeView(challenge, title, questions, attempt, stats);
    }

    private Set<UUID> activeCards(List<UUID> cardIds) {
        if (cardIds.isEmpty()) {
            return Set.of();
        }
        return cards.currentVersions(cardIds).stream()
                .map(CardSnapshot::cardId)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Attempted attempted(
            DailyChallenge challenge, Set<UUID> activeCards, Map<UUID, List<DailyChallengeAnswer>> answers) {
        List<Integer> questions = new ArrayList<>();
        for (int position = 0; position < challenge.cardIds().size(); position++) {
            if (activeCards.contains(challenge.cardIds().get(position))) {
                questions.add(position);
            }
        }
        return new Attempted(
                challenge, DailyChallengeAttempt.of(questions, answers.getOrDefault(challenge.id(), List.of())));
    }

    private static Optional<DailyChallenge> find(List<DailyChallenge> recent, String subjectSlug, LocalDate day) {
        return recent.stream()
                .filter(challenge -> challenge.subjectSlug().equals(subjectSlug)
                        && challenge.day().equals(day))
                .findFirst();
    }

    private LocalDate today(UserRef user) {
        return LocalDate.now(clock.withZone(user.timezone()));
    }

    private record Attempted(DailyChallenge challenge, DailyChallengeAttempt attempt) {

        /** Commencé sans être fini : un défi de la veille dans cet état se finit avant celui du jour. */
        boolean toFinish() {
            return attempt.isStarted() && !attempt.isDone();
        }
    }

    /**
     * Une réponse à une question du défi : les choix cochés d'une carte à choix, ou les trous d'un code à compléter.
     *
     * @param requestId identifiant choisi par le client, réutilisé tel quel si la requête est renvoyée
     * @param position la question, à partir de 0
     */
    public record ChallengeAnswer(
            UUID requestId,
            int position,
            @Nullable Set<Integer> selectedChoices,
            @Nullable List<String> filledBlanks) {

        GradedAnswer toGradedAnswer(UUID cardId) {
            return new GradedAnswer(cardId, selectedChoices, filledBlanks);
        }
    }

    /**
     * Le défi d'un sujet pour un joueur.
     *
     * @param questions les questions encore posées, par position, sans leurs réponses tant qu'elles n'ont pas la sienne
     * @param stats les chiffres du jour, une fois son défi fini ; {@code null} avant
     */
    public record ChallengeView(
            DailyChallenge challenge,
            String subjectTitle,
            SortedMap<Integer, CardSnapshot> questions,
            DailyChallengeAttempt attempt,
            @Nullable DailyChallengeStats stats) {

        public ChallengeView {
            questions = Collections.unmodifiableSortedMap(new TreeMap<>(questions));
        }

        /** @throws NotAChallengeQuestionException si le défi n'a pas de question à cette position */
        CardSnapshot question(int position) {
            CardSnapshot card = questions.get(position);
            if (card == null) {
                throw new NotAChallengeQuestionException(position);
            }
            return card;
        }
    }

    /**
     * Le défi d'un sujet dans la liste du joueur.
     *
     * @param day le jour du défi : aujourd'hui, ou la veille si son défi est à finir
     * @param attempt où il en est ; {@code null} si le défi du jour n'est pas encore tiré
     */
    public record ChallengeSummary(
            String subjectSlug,
            String subjectTitle,
            LocalDate day,
            @Nullable DailyChallengeAttempt attempt) {}

    /**
     * La correction d'une réponse, et où en est le défi.
     *
     * @param stats les chiffres du jour, si cette réponse finit le défi ; {@code null} sinon
     */
    public record AnswerOutcome(
            int position,
            AnswerCorrection correction,
            DailyChallengeAttempt attempt,
            @Nullable DailyChallengeStats stats) {}
}
