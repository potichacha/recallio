package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.CardType;
import com.recallio.content.NotionInfo;
import com.recallio.content.SubjectDirectory;
import com.recallio.platform.domain.IdGenerator;
import com.recallio.review.domain.exam.MockExam;
import com.recallio.review.domain.exam.MockExam.Submission;
import com.recallio.review.domain.exam.MockExamDraw;
import com.recallio.review.domain.exam.MockExamDraw.Candidate;
import com.recallio.review.domain.exam.MockExamExceptions.MockExamAlreadySubmittedException;
import com.recallio.review.domain.exam.MockExamExceptions.MockExamNotFoundException;
import com.recallio.review.domain.exam.MockExamExceptions.NotEnoughExamQuestionsException;
import com.recallio.review.domain.exam.MockExamRepository;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cas d'usage : l'examen blanc d'un sujet (ADR 0044). Des questions corrigées par le serveur, tirées dans les notions
 * essentielles d'abord, un temps limité, puis la note et le corrigé. Ni le journal de révision, ni FSRS, ni l'XP ne
 * voient ces réponses : un examen mesure, il n'entraîne pas la mémoire.
 */
@ApplicationScoped
public class MockExamService {

    /** Les formats d'un examen : ceux que le serveur corrige, sauf les exercices de code, trop longs pour une épreuve. */
    static final Set<CardType> EXAM_FORMATS = Set.of(CardType.MULTIPLE_CHOICE, CardType.TRUE_FALSE, CardType.FILL_IN);

    /** La priorité d'une carte sans notion (document Markdown) : courante. */
    static final int UNKNOWN_PRIORITY = 2;

    static final int HISTORY_SIZE = 10;

    private final MockExamRepository exams;
    private final CardDirectory cards;
    private final SubjectDirectory subjects;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public MockExamService(
            MockExamRepository exams,
            CardDirectory cards,
            SubjectDirectory subjects,
            IdGenerator idGenerator,
            Clock clock) {
        this.exams = exams;
        this.cards = cards;
        this.subjects = subjects;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    /**
     * @throws com.recallio.platform.domain.ResourceNotFoundException si le sujet n'est pas publié
     * @throws NotEnoughExamQuestionsException s'il a moins de {@value MockExam#MIN_QUESTIONS} questions possibles
     */
    @Transactional
    public ExamView start(UserRef user, String subjectSlug) {
        subjects.requirePublished(subjectSlug);
        Map<UUID, Integer> priorities = cards.notions(Set.of(subjectSlug)).stream()
                .collect(Collectors.toMap(NotionInfo::id, NotionInfo::priority));
        List<Candidate> candidates =
                cards.activeCards(new CardFilter(Set.of(subjectSlug), null, Set.of(), EXAM_FORMATS)).stream()
                        .map(card -> {
                            UUID notionId = card.notionId();
                            int priority = notionId == null
                                    ? UNKNOWN_PRIORITY
                                    : priorities.getOrDefault(notionId, UNKNOWN_PRIORITY);
                            return new Candidate(card.cardId(), notionId, priority);
                        })
                        .toList();
        if (candidates.size() < MockExam.MIN_QUESTIONS) {
            throw new NotEnoughExamQuestionsException(subjectSlug, MockExam.MIN_QUESTIONS);
        }
        UUID id = idGenerator.newId();
        long seed = id.getMostSignificantBits() ^ id.getLeastSignificantBits();
        var exam = MockExam.start(
                id, user.id(), subjectSlug, MockExamDraw.draw(candidates, MockExam.QUESTIONS, seed), clock.instant());
        exams.add(exam);
        return view(exam);
    }

    /** Un examen de l'utilisateur, pour le reprendre après un rechargement de la page. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public ExamView get(UserRef user, UUID examId) {
        return view(owned(user, examId));
    }

    /**
     * Note la copie : une question sans réponse est fausse. Une copie rendue après le temps imparti est notée, mais
     * marquée hors délai.
     *
     * @param answers les réponses, au plus une par question ; une réponse à une carte hors de l'examen est ignorée
     * @throws MockExamAlreadySubmittedException si la copie est déjà rendue
     */
    @Transactional
    public ExamResult submit(UserRef user, UUID examId, List<GradedAnswer> answers) {
        MockExam exam = owned(user, examId);
        if (exam.isSubmitted()) {
            throw new MockExamAlreadySubmittedException(examId);
        }
        Map<UUID, GradedAnswer> byCard = answers.stream()
                .collect(Collectors.toMap(GradedAnswer::cardId, Function.identity(), (first, _) -> first));
        List<AnswerCorrection> corrections = questions(exam).stream()
                .map(card -> AnswerCorrection.grade(card, byCard.getOrDefault(card.cardId(), unanswered(card))))
                .toList();
        Instant now = clock.instant();
        int correct =
                (int) corrections.stream().filter(AnswerCorrection::correct).count();
        var submission = new Submission(now, correct, corrections.size());
        if (!exams.submit(examId, submission)) {
            throw new MockExamAlreadySubmittedException(examId);
        }
        return new ExamResult(submission, exam.inTime(now), corrections);
    }

    /** Les {@value #HISTORY_SIZE} dernières copies rendues dans ce sujet, la plus récente d'abord. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<MockExam> history(UserRef user, String subjectSlug) {
        return exams.submitted(user.id(), subjectSlug, HISTORY_SIZE);
    }

    private MockExam owned(UserRef user, UUID examId) {
        return exams.find(examId)
                .filter(exam -> exam.userId().equals(user.id()))
                .orElseThrow(() -> new MockExamNotFoundException(examId));
    }

    private ExamView view(MockExam exam) {
        return new ExamView(exam, questions(exam), exam.remaining(clock.instant()));
    }

    /** Les questions de l'examen dans leur ordre ; une carte retirée depuis le tirage est sautée. */
    private List<CardSnapshot> questions(MockExam exam) {
        Map<UUID, CardSnapshot> byId = cards.currentVersions(exam.cardIds()).stream()
                .collect(Collectors.toMap(CardSnapshot::cardId, Function.identity()));
        List<CardSnapshot> questions = new ArrayList<>();
        for (UUID cardId : exam.cardIds()) {
            CardSnapshot card = byId.get(cardId);
            if (card != null) {
                questions.add(card);
            }
        }
        return questions;
    }

    /** Une réponse vide, de la forme qu'attend la carte : sa correction donne la bonne réponse. */
    private static GradedAnswer unanswered(CardSnapshot card) {
        return card.type() == CardType.FILL_IN
                ? new GradedAnswer(
                        card.cardId(), null, Collections.nCopies(card.blanks().size(), ""))
                : new GradedAnswer(card.cardId(), Set.of(), null);
    }

    /**
     * @param questions les questions, sans leurs réponses tant que la copie n'est pas rendue
     * @param remaining le temps qui reste, mesuré par le serveur
     */
    public record ExamView(MockExam exam, List<CardSnapshot> questions, Duration remaining) {}

    /**
     * @param inTime faux si la copie est arrivée après le temps imparti, marge comprise
     * @param corrections la correction de chaque question, dans l'ordre de l'examen
     */
    public record ExamResult(Submission submission, boolean inTime, List<AnswerCorrection> corrections) {}
}
