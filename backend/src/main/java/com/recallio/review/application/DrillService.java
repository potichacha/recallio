package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.CardType;
import com.recallio.content.SubjectDirectory;
import com.recallio.content.SubjectScope;
import com.recallio.review.domain.AnswerMismatchException;
import com.recallio.review.domain.CardNotReviewableException;
import com.recallio.review.domain.drill.DrillActivityRepository;
import com.recallio.review.domain.drill.DrillDraw;
import com.recallio.review.domain.drill.DrillDraw.Candidate;
import com.recallio.review.domain.drill.DrillLevel;
import com.recallio.review.domain.drill.DrillUsage;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Cas d'usage : la révision intensive (ADR 0078). Des questions de plusieurs sujets, sans fin, corrigées par le serveur
 * sur leurs choix (ADR 0051). C'est un entraînement libre (ADR 0038) : ni le journal de révision, ni FSRS, ni l'XP, ni
 * la série ne voient ces réponses, et aucun événement n'est publié.
 *
 * <p>Le tirage n'a pas d'état : la graine et la position viennent du client ({@link DrillDraw}). Seul le compteur du
 * jour est écrit, à chaque correction.
 */
@ApplicationScoped
public class DrillService {

    private final CardDirectory cards;
    private final SubjectDirectory subjects;
    private final DrillActivityRepository activity;
    private final Clock clock;

    public DrillService(CardDirectory cards, SubjectDirectory subjects, DrillActivityRepository activity, Clock clock) {
        this.cards = cards;
        this.subjects = subjects;
        this.activity = activity;
        this.clock = clock;
    }

    /**
     * Une page de questions : les cartes à choix de ces sujets dont la notion convient au poste visé, dans l'ordre que
     * fixe la graine, à partir de {@code offset}.
     *
     * @param scope ce que l'appelant peut voir : les brouillons, pour un admin
     * @throws com.recallio.platform.domain.ResourceNotFoundException si l'un des sujets n'existe pas ou ne lui est pas
     *     visible
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public DrillPage questions(
            Set<String> subjectSlugs, DrillLevel level, long seed, long offset, int limit, SubjectScope scope) {
        subjectSlugs.forEach(slug -> subjects.requireVisible(slug, scope));
        List<Candidate> candidates = cards.choiceCards(subjectSlugs, scope).stream()
                .filter(card -> level.accepts(card.level(), card.priority()))
                .map(card -> new Candidate(card.cardId(), card.notionId()))
                .toList();
        DrillDraw.Page page = DrillDraw.page(candidates, seed, offset, limit);
        return new DrillPage(cards.currentVersions(page.cardIds(), scope), page.total(), page.nextOffset());
    }

    /**
     * Corrige une réponse, et la compte dans l'usage du jour de l'utilisateur. Rien d'autre n'est enregistré.
     *
     * @throws CardNotReviewableException si la carte n'est pas une question de la révision intensive : inconnue,
     *     retirée, d'un sujet qui ne lui est pas visible, sans choix, ou un exercice de code
     * @throws AnswerMismatchException si la réponse n'est pas faite de choix cochés
     */
    @Transactional
    public DrillCheck check(UserRef user, GradedAnswer answer, SubjectScope scope) {
        CardSnapshot card = cards.currentVersions(List.of(answer.cardId()), scope).stream()
                .filter(DrillService::isDrillQuestion)
                .findFirst()
                .orElseThrow(() -> new CardNotReviewableException(answer.cardId()));
        AnswerCorrection correction = AnswerCorrection.grade(card, answer);
        DrillUsage usage = activity.record(user.id(), today(user), correction.correct());
        return new DrillCheck(card, correction, usage);
    }

    /** L'usage du jour de l'utilisateur, dans son fuseau. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public DrillUsage usage(UserRef user) {
        return activity.find(user.id(), today(user));
    }

    /** Le même critère que {@link CardDirectory#choiceCards} : des choix, une notion, et pas un exercice de code. */
    private static boolean isDrillQuestion(CardSnapshot card) {
        return card.isGradedByChoices() && card.type() != CardType.CODING_EXERCISE && card.notionSlug() != null;
    }

    private LocalDate today(UserRef user) {
        return LocalDate.now(clock.withZone(user.timezone()));
    }

    /**
     * @param questions les cartes de la page, dans l'ordre où les poser
     * @param total le nombre de cartes éligibles pour ces sujets et ce poste
     * @param nextOffset où reprendre pour la page suivante
     */
    public record DrillPage(List<CardSnapshot> questions, int total, long nextOffset) {

        public DrillPage {
            questions = List.copyOf(questions);
        }
    }

    /**
     * @param card la carte corrigée : son sujet et sa notion servent au bilan
     * @param usage l'usage du jour, cette réponse comprise
     */
    public record DrillCheck(CardSnapshot card, AnswerCorrection correction, DrillUsage usage) {}
}
