package com.recallio.live.application;

import com.recallio.content.CardChoice;
import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.ActiveCard;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.CardType;
import com.recallio.live.domain.LiveExceptions.NotEnoughQuestionsException;
import com.recallio.live.domain.LiveQuestion;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Tire les questions d'une partie : des QCM ou des vrai ou faux du sujet (corrigés par le serveur), dans les thèmes du
 * groupe, au hasard (ADR 0022, 0027).
 */
@ApplicationScoped
public class LiveQuestionDraw {

    private final CardDirectory cards;
    private final LiveConfig config;

    public LiveQuestionDraw(CardDirectory cards, LiveConfig config) {
        this.cards = cards;
        this.config = config;
    }

    /**
     * @param themeSlugs la sélection de thèmes du groupe (ADR 0017) ; vide : tous les thèmes du sujet
     * @throws NotEnoughQuestionsException si le sujet compte moins de QCM que le minimum d'une partie
     */
    public List<LiveQuestion> draw(String subjectSlug, Set<String> themeSlugs) {
        CardFilter filter = new CardFilter(Set.of(subjectSlug), null, themeSlugs, CardType.GRADED_BY_CHOICES);
        List<UUID> pool = new ArrayList<>(
                cards.activeCards(filter).stream().map(ActiveCard::cardId).toList());
        if (pool.size() < config.minQuestions()) {
            throw new NotEnoughQuestionsException(pool.size(), config.minQuestions());
        }
        Collections.shuffle(pool, ThreadLocalRandom.current());
        List<UUID> series = pool.subList(0, Math.min(config.questionsPerGame(), pool.size()));
        return cards.currentVersions(series).stream()
                .map(LiveQuestionDraw::question)
                .toList();
    }

    private static LiveQuestion question(CardSnapshot card) {
        return new LiveQuestion(
                card.cardId(),
                card.prompt(),
                card.choices().stream().map(CardChoice::text).toList(),
                card.correctChoices(),
                card.answer());
    }
}
