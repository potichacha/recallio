package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.NotionInfo;
import com.recallio.review.TourNotionValidated;
import com.recallio.review.TourPageOpened;
import com.recallio.review.domain.AnswerMismatchException;
import com.recallio.review.domain.tour.TourLastPage;
import com.recallio.review.domain.tour.TourNotionNotFoundException;
import com.recallio.review.domain.tour.TourPageNotFoundException;
import com.recallio.review.domain.tour.TourProgressRepository;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Cas d'usage : la progression d'un utilisateur dans le Tour (ADR 0037). Il valide une notion en répondant juste à ses
 * questions, corrigées par le serveur. Ces réponses ne sont pas des révisions : ni le journal, ni FSRS, ni l'XP ne les
 * voient, et une notion validée le reste. La dernière page qu'il a ouverte lui permet de reprendre où il s'était arrêté.
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class TourProgressService {

    private final CardDirectory cards;
    private final TourProgressRepository progress;
    private final Clock clock;
    private final Event<TourPageOpened> pageOpened;
    private final Event<TourNotionValidated> notionValidated;

    public TourProgressService(
            CardDirectory cards,
            TourProgressRepository progress,
            Clock clock,
            Event<TourPageOpened> pageOpened,
            Event<TourNotionValidated> notionValidated) {
        this.cards = cards;
        this.progress = progress;
        this.clock = clock;
        this.pageOpened = pageOpened;
        this.notionValidated = notionValidated;
    }

    /** Les notions validées, dans l'ordre du référentiel, et la dernière page ouverte, si elle est encore dans le Tour. */
    public Progress progress(UserRef user, String subjectSlug) {
        List<NotionInfo> notions = cards.notions(Set.of(subjectSlug));
        Set<UUID> validated = progress.validated(
                user.id(), notions.stream().map(NotionInfo::id).toList());
        List<String> validatedNotions = notions.stream()
                .filter(notion -> validated.contains(notion.id()))
                .map(NotionInfo::slug)
                .toList();
        @Nullable
        Step lastPage = progress.lastPage(user.id(), subjectSlug)
                .flatMap(last -> notions.stream()
                        .filter(notion -> notion.id().equals(last.notionId()) && last.page() <= notion.tourPages())
                        .findFirst()
                        .map(notion -> new Step(notion.slug(), last.page())))
                .orElse(null);
        return new Progress(validatedNotions, lastPage);
    }

    /**
     * Note la page qu'un utilisateur vient d'ouvrir : le sommaire lui proposera d'y reprendre.
     *
     * @param page le rang de la page dans sa notion, à partir de 1
     * @throws TourPageNotFoundException si la notion n'est pas dans le Tour, ou n'a pas cette page
     */
    @Transactional
    public void openPage(UserRef user, String subjectSlug, String notionSlug, int page) {
        NotionInfo notion = cards.notion(subjectSlug, notionSlug)
                .filter(found -> page >= 1 && page <= found.tourPages())
                .orElseThrow(() -> new TourPageNotFoundException(subjectSlug, notionSlug, page));
        Instant now = clock.instant();
        progress.saveLastPage(user.id(), subjectSlug, new TourLastPage(notion.id(), page), now);
        pageOpened.fire(new TourPageOpened(user, subjectSlug, notionSlug, page, now));
    }

    /**
     * Les questions d'une notion, et si l'utilisateur l'a déjà validée.
     *
     * @throws TourNotionNotFoundException si la notion n'est pas dans le Tour, ou n'a pas de questions
     */
    public Checkpoint checkpoint(UserRef user, String subjectSlug, String notionSlug) {
        NotionInfo notion = notion(subjectSlug, notionSlug);
        return new Checkpoint(isValidated(user, notion), questions(subjectSlug, notionSlug));
    }

    /**
     * Corrige une réponse à chaque question de la notion. Toutes justes, la notion est validée ; on peut réessayer
     * autant de fois qu'on veut.
     *
     * @throws TourNotionNotFoundException si la notion n'est pas dans le Tour, ou n'a pas de questions
     * @throws AnswerMismatchException s'il manque une réponse, s'il y en a en trop, ou si l'une ne convient pas au format
     */
    @Transactional
    public CheckOutcome check(UserRef user, String subjectSlug, String notionSlug, List<GradedAnswer> answers) {
        NotionInfo notion = notion(subjectSlug, notionSlug);
        List<CardSnapshot> questions = questions(subjectSlug, notionSlug);
        Map<UUID, GradedAnswer> byCard = new HashMap<>();
        for (GradedAnswer answer : answers) {
            if (byCard.put(answer.cardId(), answer) != null) {
                throw AnswerMismatchException.tourAnswersExpected();
            }
        }
        if (!byCard.keySet().equals(questions.stream().map(CardSnapshot::cardId).collect(Collectors.toSet()))) {
            throw AnswerMismatchException.tourAnswersExpected();
        }
        List<AnswerCorrection> results = questions.stream()
                .map(question ->
                        AnswerCorrection.grade(question, Objects.requireNonNull(byCard.get(question.cardId()))))
                .toList();
        if (results.stream().allMatch(AnswerCorrection::correct)) {
            Instant now = clock.instant();
            if (progress.markValidated(user.id(), notion.id(), now)) {
                notionValidated.fire(new TourNotionValidated(
                        user, subjectSlug, notionSlug, progress.validatedCount(user.id()), now));
            }
            return new CheckOutcome(true, results);
        }
        return new CheckOutcome(isValidated(user, notion), results);
    }

    private NotionInfo notion(String subjectSlug, String notionSlug) {
        return cards.notion(subjectSlug, notionSlug)
                .filter(NotionInfo::isInTour)
                .orElseThrow(() -> new TourNotionNotFoundException(subjectSlug, notionSlug));
    }

    private List<CardSnapshot> questions(String subjectSlug, String notionSlug) {
        List<CardSnapshot> questions = cards.tourQuestions(subjectSlug, notionSlug);
        if (questions.isEmpty()) {
            throw new TourNotionNotFoundException(subjectSlug, notionSlug);
        }
        return questions;
    }

    private boolean isValidated(UserRef user, NotionInfo notion) {
        return progress.validated(user.id(), List.of(notion.id())).contains(notion.id());
    }

    /**
     * @param validatedNotions dans l'ordre du référentiel
     * @param lastPage la dernière page ouverte ; {@code null} si aucune, ou si elle n'est plus dans le Tour
     */
    public record Progress(
            List<String> validatedNotions, @Nullable Step lastPage) {

        public Progress {
            validatedNotions = List.copyOf(validatedNotions);
        }
    }

    /**
     * Une page du Tour.
     *
     * @param notion le slug de sa notion
     * @param page son rang dans la notion, à partir de 1
     */
    public record Step(String notion, int page) {}

    /** @param questions dans l'ordre du fichier de la notion */
    public record Checkpoint(boolean validated, List<CardSnapshot> questions) {

        public Checkpoint {
            questions = List.copyOf(questions);
        }
    }

    /** @param validated si la notion est validée, par cette réponse ou par une précédente */
    public record CheckOutcome(boolean validated, List<AnswerCorrection> results) {

        public CheckOutcome {
            results = List.copyOf(results);
        }
    }
}
