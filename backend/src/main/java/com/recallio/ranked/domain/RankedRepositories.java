package com.recallio.ranked.domain;

import com.recallio.ranked.domain.Duel.Mode;
import com.recallio.ranked.domain.Rank.Tier;
import com.recallio.ranked.domain.glicko.Rating;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Les ports de persistance du classé (ADR 0046). */
public final class RankedRepositories {

    private RankedRepositories() {}

    /** Les cotes de difficulté des cartes, dans la table {@code card_rating}. */
    public interface CardRatingRepository {

        /** Les cotes de ces cartes ; une carte à laquelle personne n'a répondu est absente. */
        Map<UUID, Rating> ratings(Collection<UUID> cardIds);

        /**
         * La cote de la carte, verrouillée jusqu'à la fin de la transaction : deux réponses simultanées ne perdent
         * pas de mise à jour. Une carte sans cote part de {@link Rating#START}.
         */
        Rating lock(UUID cardId, String subjectSlug, Instant now);

        void save(UUID cardId, Rating rating, Instant now);
    }

    /** Les places des joueurs et leurs insignes de saison, dans {@code ranked_standing} et {@code ranked_badge}. */
    public interface StandingRepository {

        Optional<Standing> find(UUID userId, String subjectSlug);

        /** Comme {@link #find}, en verrouillant la place jusqu'à la fin de la transaction. */
        Optional<Standing> lock(UUID userId, String subjectSlug);

        List<Standing> of(UUID userId);

        void save(Standing standing, Instant now);

        /** Garde le meilleur rang d'une saison finie ; une seconde fois ne change rien. */
        void award(UUID userId, String subjectSlug, Season season, Tier tier, Instant now);

        List<Badge> badges(UUID userId);
    }

    /** Un insigne de saison : le meilleur rang atteint dans un sujet. */
    public record Badge(String subjectSlug, Season season, Tier tier) {}

    /** Les duels, dans {@code duel}, {@code duel_question}, {@code duel_player} et {@code duel_answer}. */
    public interface DuelRepository {

        void create(Duel duel);

        Optional<Duel> find(UUID id);

        /** Comme {@link #find}, en verrouillant le duel jusqu'à la fin de la transaction. */
        Optional<Duel> lock(UUID id);

        /** Le duel de ce sujet que l'utilisateur n'a pas fini : il le reprend plutôt que d'en chercher un autre. */
        Optional<Duel> unfinished(UUID userId, String subjectSlug, Mode mode, Instant now);

        /**
         * Le duel asynchrone ouvert le plus proche de cette cote, ouvert par un autre, verrouillé ; un duel déjà
         * verrouillé par une autre recherche est sauté.
         */
        Optional<Duel> lockClosestOpen(String subjectSlug, UUID userId, double rating, Instant now);

        /** Fait entrer l'adversaire : le duel devient complet, avec sa nouvelle échéance. */
        void join(UUID duelId, UUID userId, Rating rating, Instant now, Instant expiresAt);

        void serve(UUID duelId, UUID userId, int position, Instant now);

        void answer(UUID duelId, UUID userId, int position, Instant now, boolean correct);

        void finish(UUID duelId, UUID userId, Instant now);

        /** Règle le duel : son statut final, et la cote d'après de chaque joueur (vide pour un duel expiré). */
        void settle(UUID duelId, Duel.Status status, Map<UUID, Double> ratingsAfter, Instant now);

        /** Les derniers duels de l'utilisateur, les plus récents d'abord. */
        List<Duel> recent(UUID userId, int limit);
    }

    /** La file du classé en direct, dans {@code ranked_queue}. */
    public interface LiveQueueRepository {

        /** Le duel trouvé pour cet utilisateur pendant qu'il attendait, s'il y en a un ; sa place dans la file est rendue. */
        Optional<UUID> takeMatch(UUID userId);

        /** Un autre joueur qui attend dans ce sujet et s'est signalé depuis {@code since}, verrouillé. */
        Optional<UUID> lockWaiting(String subjectSlug, UUID userId, Instant since);

        /** Le joueur qui attendait a un duel : il le trouvera à son prochain signal. */
        void match(UUID waitingUserId, UUID duelId);

        /** Entre dans la file, ou s'y signale de nouveau. */
        void enqueue(UUID userId, String subjectSlug, Instant now);

        void leave(UUID userId);
    }
}
