package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.NotionInfo;
import com.recallio.review.application.ReviewRecordingService.BlankCorrection;
import com.recallio.review.domain.AnswerMismatchException;
import com.recallio.review.domain.tour.TourNotionNotFoundException;
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
 * Cas d'usage : valider une notion du Tour en répondant juste à ses questions, corrigées par le serveur (ADR 0037). Ces
 * réponses ne sont pas des révisions : ni le journal, ni FSRS, ni l'XP ne les voient. Une notion validée le reste.
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class TourValidationService {

    private final CardDirectory cards;
    private final TourProgressRepository progress;
    private final Clock clock;

    public TourValidationService(CardDirectory cards, TourProgressRepository progress, Clock clock) {
        this.cards = cards;
        this.progress = progress;
        this.clock = clock;
    }

    /** Les notions d'un sujet que l'utilisateur a validées, dans l'ordre de son référentiel. */
    public List<String> validatedNotions(UserRef user, String subjectSlug) {
        List<NotionInfo> notions = cards.notions(Set.of(subjectSlug));
        Set<UUID> validated = progress.validated(
                user.id(), notions.stream().map(NotionInfo::id).toList());
        return notions.stream()
                .filter(notion -> validated.contains(notion.id()))
                .map(NotionInfo::slug)
                .toList();
    }

    /**
     * Les questions d'une notion, et si l'utilisateur l'a déjà validée.
     *
     * @throws TourNotionNotFoundException si la notion n'a pas de questions
     */
    public Checkpoint checkpoint(UserRef user, String subjectSlug, String notionSlug) {
        NotionInfo notion = notion(subjectSlug, notionSlug);
        return new Checkpoint(isValidated(user, notion), questions(subjectSlug, notionSlug));
    }

    /**
     * Corrige une réponse à chaque question de la notion. Toutes justes, la notion est validée ; on peut réessayer
     * autant de fois qu'on veut.
     *
     * @throws TourNotionNotFoundException si la notion n'a pas de questions
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
                .filter(notion -> notion.slug().equals(notionSlug))
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
