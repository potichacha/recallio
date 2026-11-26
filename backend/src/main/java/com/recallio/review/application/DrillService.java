package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.CardType;
import com.recallio.content.SubjectDirectory;
import com.recallio.content.SubjectScope;
import com.recallio.entitlement.PlusAccess;
import com.recallio.review.domain.AnswerMismatchException;
import com.recallio.review.domain.CardNotReviewableException;
import com.recallio.review.domain.drill.DrillActivityRepository;
import com.recallio.review.domain.drill.DrillCheckId;
import com.recallio.review.domain.drill.DrillDraw;
import com.recallio.review.domain.drill.DrillDraw.Candidate;
import com.recallio.review.domain.drill.DrillLevel;
import com.recallio.review.domain.drill.DrillLimitReachedException;
import com.recallio.review.domain.drill.DrillQuota;
import com.recallio.review.domain.drill.DrillUsage;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Cas d'usage : la révision intensive (ADR 0078). Des questions de plusieurs sujets, sans fin, corrigées par le serveur
 * sur leurs choix (ADR 0051). C'est un entraînement libre (ADR 0038) : ni le journal de révision, ni FSRS, ni l'XP, ni
 * la série ne voient ces réponses, et aucun événement n'est publié.
 *
 * <p>Le tirage n'a pas d'état : la graine et la position viennent du client ({@link DrillDraw}). Seul le compteur du
 * jour est écrit, à chaque correction. Il porte la limite quotidienne d'un compte sans Plus (ADR 0079) : le tirage, lui,
 * reste ouvert, puisqu'une question sans sa correction ne donne rien.
 */
@ApplicationScoped
public class DrillService {

    private final CardDirectory cards;
    private final SubjectDirectory subjects;
    private final DrillActivityRepository activity;
    private final PlusAccess plus;
    private final ReviewConfig config;
    private final Clock clock;

    public DrillService(
            CardDirectory cards,
            SubjectDirectory subjects,
            DrillActivityRepository activity,
            PlusAccess plus,
            ReviewConfig config,
            Clock clock) {
        this.cards = cards;
        this.subjects = subjects;
        this.activity = activity;
        this.plus = plus;
        this.config = config;
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
     * <p>Idempotent : la même requête rejouée ({@code requestId}, carte et choix identiques) rend la même correction sans
     * être comptée deux fois. Sans Plus, la réponse de trop est refusée avant que sa correction soit rendue.
     *
     * @param requestId l'identifiant que le client a tiré pour cette réponse, et qu'il renvoie tel quel s'il la rejoue
     * @throws CardNotReviewableException si la carte n'est pas une question de la révision intensive : inconnue,
     *     retirée, d'un sujet qui ne lui est pas visible, sans choix, ou un exercice de code
     * @throws AnswerMismatchException si la réponse n'est pas faite de choix cochés
     * @throws DrillLimitReachedException si le compte, sans Plus, a déjà fait corriger ses réponses du jour
     */
    @Transactional
    public DrillCheck check(UserRef user, UUID requestId, GradedAnswer answer, SubjectScope scope) {
        CardSnapshot card = cards.currentVersions(List.of(answer.cardId()), scope).stream()
                .filter(DrillService::isDrillQuestion)
                .findFirst()
                .orElseThrow(() -> new CardNotReviewableException(answer.cardId()));
        AnswerCorrection correction = AnswerCorrection.grade(card, answer);
        Set<Integer> selected = answer.selectedChoices();
        UUID checkId = DrillCheckId.of(requestId, card.cardId(), selected == null ? Set.of() : selected);
        Integer dailyLimit = dailyLimit(user);
        DrillUsage usage = activity.record(
                        user.id(), today(user), correction.correct(), checkId, maxAnswers(dailyLimit))
                .orElseThrow(() -> new DrillLimitReachedException(config.drillFreeAnswersPerDay()));
        return new DrillCheck(card, correction, new DrillQuota(usage, dailyLimit));
    }

    /** L'usage du jour de l'utilisateur, dans son fuseau, et ce qu'il lui reste sans Plus. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public DrillQuota quota(UserRef user) {
        return new DrillQuota(activity.find(user.id(), today(user)), dailyLimit(user));
    }

    /** La limite du jour de ce compte ; {@code null} avec Plus, qui n'en a pas. */
    private @Nullable Integer dailyLimit(UserRef user) {
        return plus.hasPlus(user.id()) ? null : config.drillFreeAnswersPerDay();
    }

    private static int maxAnswers(@Nullable Integer dailyLimit) {
        return dailyLimit == null ? Integer.MAX_VALUE : dailyLimit;
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
     * @param quota l'usage du jour, cette réponse comprise, et ce qu'il reste
     */
    public record DrillCheck(CardSnapshot card, AnswerCorrection correction, DrillQuota quota) {}
}
