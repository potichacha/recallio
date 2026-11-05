package com.recallio.ranked.application;

import com.recallio.content.CardType;
import com.recallio.ranked.domain.RankedRepositories.CardRatingRepository;
import com.recallio.ranked.domain.RankedRepositories.StandingRepository;
import com.recallio.ranked.domain.Standing;
import com.recallio.ranked.domain.glicko.Glicko2;
import com.recallio.ranked.domain.glicko.Glicko2.Game;
import com.recallio.ranked.domain.glicko.Rating;
import com.recallio.review.CardReviewed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * La cote de difficulté des cartes (ADR 0046) : chaque réponse corrigée par le serveur est une partie entre la carte et
 * celui qui répond, avec sa cote dans le sujet. Une bonne réponse fait perdre la carte.
 */
@ApplicationScoped
public class CardDifficulty {

    /** Les formats des duels, les seuls à avoir une cote de difficulté. */
    public static final Set<CardType> RATED_FORMATS =
            Set.of(CardType.MULTIPLE_CHOICE, CardType.TRUE_FALSE, CardType.FILL_IN);

    private final CardRatingRepository ratings;
    private final StandingRepository standings;
    private final Clock clock;

    public CardDifficulty(CardRatingRepository ratings, StandingRepository standings, Clock clock) {
        this.ratings = ratings;
        this.standings = standings;
        this.clock = clock;
    }

    /** Une révision : observateur synchrone, dans la transaction de la réponse. */
    @Transactional(Transactional.TxType.MANDATORY)
    void onCardReviewed(@Observes CardReviewed event) {
        if (!RATED_FORMATS.contains(event.cardType())) {
            return;
        }
        Rating player = standings
                .find(event.user().id(), event.subjectSlug())
                .map(Standing::rating)
                .orElse(Rating.START);
        record(event.cardId(), event.subjectSlug(), player, event.recalled());
    }

    /** Une réponse à la carte, par un joueur de cette cote. */
    @Transactional(Transactional.TxType.MANDATORY)
    public void record(UUID cardId, String subjectSlug, Rating player, boolean correct) {
        Instant now = clock.instant();
        Rating card = ratings.lock(cardId, subjectSlug, now);
        ratings.save(cardId, Glicko2.update(card, List.of(new Game(player, correct ? Game.LOSS : Game.WIN))), now);
    }
}
