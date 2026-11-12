package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.ActiveCard;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.review.domain.AnswerMismatchException;
import com.recallio.review.domain.CardNotReviewableException;
import com.recallio.review.domain.ReviewLogRepository;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cas d'usage : le carnet d'erreurs (ADR 0057). Les cartes d'un sujet que l'utilisateur a ratées ces sept derniers
 * jours et pas réussies depuis, à refaire tout de suite. Comme l'entraînement d'une notion (ADR 0038), rien n'est
 * enregistré : FSRS ne voit que les révisions espacées, et une carte ratée y revient de toute façon à son heure.
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class MistakesService {

    /** Le carnet remonte à une semaine : au-delà, la révision espacée a déjà reproposé la carte. */
    static final Duration WINDOW = Duration.ofDays(7);

    /** Une séance courte : les plus récentes erreurs d'abord, le reste à la séance suivante. */
    static final int MAX_CARDS = 20;

    private final ReviewLogRepository reviewLog;
    private final CardDirectory cards;
    private final Clock clock;

    public MistakesService(ReviewLogRepository reviewLog, CardDirectory cards, Clock clock) {
        this.reviewLog = reviewLog;
        this.cards = cards;
        this.clock = clock;
    }

    /** Les cartes du carnet pour ce sujet, la plus récemment ratée d'abord ; aucune pour un sujet inconnu. */
    public List<CardSnapshot> mistakes(UserRef user, String subjectSlug) {
        return cards.currentVersions(mistakenCards(user, subjectSlug));
    }

    /**
     * Corrige la réponse à une carte du carnet, sans l'enregistrer.
     *
     * @throws CardNotReviewableException si la carte n'est pas dans le carnet de l'utilisateur pour ce sujet : cette
     *     route ne corrige pas n'importe quelle carte
     * @throws AnswerMismatchException si la carte s'auto-évalue, ou si la réponse ne convient pas à son format
     */
    public AnswerCorrection check(UserRef user, String subjectSlug, GradedAnswer answer) {
        CardSnapshot card = mistakenCards(user, subjectSlug).contains(answer.cardId())
                ? cards.activeCard(answer.cardId()).orElse(null)
                : null;
        if (card == null) {
            throw new CardNotReviewableException(answer.cardId());
        }
        return AnswerCorrection.grade(card, answer);
    }

    /** Les cartes ratées qui sont encore des cartes actives du sujet : le contenu a pu changer depuis. */
    private List<UUID> mistakenCards(UserRef user, String subjectSlug) {
        List<UUID> failed =
                reviewLog.cardsStillFailedSince(user.id(), clock.instant().minus(WINDOW));
        if (failed.isEmpty()) {
            return List.of();
        }
        Set<UUID> ofSubject = cards.activeCards(new CardFilter(Set.of(subjectSlug), null, Set.of(), Set.of())).stream()
                .map(ActiveCard::cardId)
                .collect(Collectors.toSet());
        return failed.stream().filter(ofSubject::contains).limit(MAX_CARDS).toList();
    }
}
