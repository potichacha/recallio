package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.ActiveCard;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.NotionInfo;
import com.recallio.review.domain.AnswerMismatchException;
import com.recallio.review.domain.CardNotReviewableException;
import com.recallio.review.domain.NotionNotFoundException;
import com.recallio.review.domain.practice.PracticeOrder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Cas d'usage : s'entraîner sur une notion (ADR 0038). Toutes ses cartes actives, les formats mêlés, et la correction
 * par le serveur de celles qui s'y prêtent (vrai ou faux, QCM, code à compléter ; un exercice s'essaie contre ses tests).
 * Rien n'est enregistré : ni le journal, ni FSRS, ni l'XP, ni la série ne voient ces réponses. Les cartes reviennent
 * ensuite dans les révisions espacées comme les autres, une par notion et par jour (ADR 0031).
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class PracticeService {

    private final CardDirectory cards;

    public PracticeService(CardDirectory cards) {
        this.cards = cards;
    }

    /**
     * Les cartes de la notion, dans l'ordre de l'entraînement.
     *
     * @throws NotionNotFoundException si la notion est inconnue, retirée, ou d'un sujet non publié
     */
    public Practice practice(String subjectSlug, String notionSlug) {
        NotionInfo notion = notion(subjectSlug, notionSlug);
        List<ActiveCard> notionCards =
                cards.activeCards(new CardFilter(Set.of(subjectSlug), null, Set.of(), Set.of())).stream()
                        .filter(card -> notion.id().equals(card.notionId()))
                        .toList();
        List<UUID> ordered = PracticeOrder.arrange(notionCards, ActiveCard::type).stream()
                .map(ActiveCard::cardId)
                .toList();
        return new Practice(notion.title(), cards.currentVersions(ordered));
    }

    /**
     * Corrige la réponse à une carte de la notion, sans l'enregistrer.
     *
     * @throws NotionNotFoundException si la notion est inconnue, retirée, ou d'un sujet non publié
     * @throws CardNotReviewableException si la carte n'est pas une carte active de cette notion
     * @throws AnswerMismatchException si la carte s'auto-évalue, ou si la réponse ne convient pas à son format
     */
    public AnswerCorrection check(String subjectSlug, String notionSlug, GradedAnswer answer) {
        NotionInfo notion = notion(subjectSlug, notionSlug);
        CardSnapshot card = cards.activeCard(answer.cardId())
                .filter(found -> notion.id()
                        .equals(cards.notionsOf(List.of(found.cardId())).get(found.cardId())))
                .orElseThrow(() -> new CardNotReviewableException(answer.cardId()));
        return AnswerCorrection.grade(card, answer);
    }

    private NotionInfo notion(String subjectSlug, String notionSlug) {
        return cards.notion(subjectSlug, notionSlug)
                .orElseThrow(() -> new NotionNotFoundException(subjectSlug, notionSlug));
    }

    /** @param cards dans l'ordre de l'entraînement */
    public record Practice(String notionTitle, List<CardSnapshot> cards) {

        public Practice {
            cards = List.copyOf(cards);
        }
    }
}
