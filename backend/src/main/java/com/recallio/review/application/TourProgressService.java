package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.NotionInfo;
import com.recallio.review.application.ReviewRecordingService.BlankCorrection;
import com.recallio.review.domain.AnswerMismatchException;
import com.recallio.review.domain.tour.TourLastPage;
import com.recallio.review.domain.tour.TourNotionNotFoundException;
import com.recallio.review.domain.tour.TourPageNotFoundException;
import com.recallio.review.domain.tour.TourProgressRepository;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

    public TourProgressService(CardDirectory cards, TourProgressRepository progress, Clock clock) {
        this.cards = cards;
        this.progress = progress;
        this.clock = clock;
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
        NotionInfo notion = cards.notions(Set.of(subjectSlug)).stream()
                .filter(candidate -> candidate.slug().equals(notionSlug))
                .filter(candidate -> page >= 1 && page <= candidate.tourPages())
                .findFirst()
                .orElseThrow(() -> new TourPageNotFoundException(subjectSlug, notionSlug, page));
        progress.saveLastPage(user.id(), subjectSlug, new TourLastPage(notion.id(), page), clock.instant());
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
    public CheckOutcome check(UserRef user, String subjectSlug, String notionSlug, List<TourAnswer> answers) {
        NotionInfo notion = notion(subjectSlug, notionSlug);
        List<CardSnapshot> questions = questions(subjectSlug, notionSlug);
        Map<UUID, TourAnswer> byCard = new HashMap<>();
        for (TourAnswer answer : answers) {
            if (byCard.put(answer.cardId(), answer) != null) {
                throw AnswerMismatchException.tourAnswersExpected();
            }
        }
        if (!byCard.keySet().equals(questions.stream().map(CardSnapshot::cardId).collect(Collectors.toSet()))) {
            throw AnswerMismatchException.tourAnswersExpected();
        }
        List<QuestionResult> results = questions.stream()
                .map(question -> grade(question, Objects.requireNonNull(byCard.get(question.cardId()))))
                .toList();
        if (results.stream().allMatch(QuestionResult::correct)) {
            progress.markValidated(user.id(), notion.id(), clock.instant());
            return new CheckOutcome(true, results);
        }
        return new CheckOutcome(isValidated(user, notion), results);
    }

    private NotionInfo notion(String subjectSlug, String notionSlug) {
        return cards.notions(Set.of(subjectSlug)).stream()
                .filter(notion -> notion.slug().equals(notionSlug) && notion.isInTour())
                .findFirst()
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

    private static QuestionResult grade(CardSnapshot question, TourAnswer answer) {
        if (question.type().isGradedByChoices()) {
            Set<Integer> selected = answer.selectedChoices();
            if (selected == null || answer.filledBlanks() != null) {
                throw AnswerMismatchException.choicesExpected();
            }
            return new QuestionResult(
                    question.cardId(),
                    question.isCorrectSelection(selected),
                    question.correctChoices(),
                    List.of(),
                    question.answer());
        }
        List<String> filled = answer.filledBlanks();
        if (filled == null || filled.size() != question.blanks().size() || answer.selectedChoices() != null) {
            throw AnswerMismatchException.blanksExpected(question.blanks().size());
        }
        List<Boolean> checks = question.checkBlanks(filled);
        List<BlankCorrection> blanks = IntStream.range(0, checks.size())
                .mapToObj(index ->
                        new BlankCorrection(question.blanks().get(index).expected(), checks.get(index)))
                .toList();
        return new QuestionResult(question.cardId(), !checks.contains(false), Set.of(), blanks, question.answer());
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

    /**
     * @param selectedChoices les positions cochées d'un QCM ou d'un vrai ou faux, à partir de 0 ; absentes sinon
     * @param filledBlanks les réponses d'un code à trous, une par trou ; absentes sinon
     */
    public record TourAnswer(
            UUID cardId,
            @Nullable Set<Integer> selectedChoices,
            @Nullable List<String> filledBlanks) {

        public TourAnswer {
            selectedChoices = selectedChoices == null ? null : Set.copyOf(selectedChoices);
            filledBlanks = filledBlanks == null ? null : List.copyOf(filledBlanks);
        }
    }

    /** @param questions dans l'ordre du fichier de la notion */
    public record Checkpoint(boolean validated, List<CardSnapshot> questions) {

        public Checkpoint {
            questions = List.copyOf(questions);
        }
    }

    /** @param validated si la notion est validée, par cette réponse ou par une précédente */
    public record CheckOutcome(boolean validated, List<QuestionResult> results) {

        public CheckOutcome {
            results = List.copyOf(results);
        }
    }

    /**
     * @param correctChoices les bonnes réponses d'un QCM ou d'un vrai ou faux ; aucune pour un code à trous
     * @param blanks la correction de chaque trou d'un code à trous ; aucune pour les autres formats
     */
    public record QuestionResult(
            UUID cardId,
            boolean correct,
            Set<Integer> correctChoices,
            List<BlankCorrection> blanks,
            String explanation) {

        public QuestionResult {
            correctChoices = Set.copyOf(correctChoices);
            blanks = List.copyOf(blanks);
        }
    }
}
