package com.recallio.ranked.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.ActiveCard;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.SubjectDirectory;
import com.recallio.platform.domain.IdGenerator;
import com.recallio.ranked.domain.Duel;
import com.recallio.ranked.domain.Duel.DuelAnswer;
import com.recallio.ranked.domain.Duel.DuelPlayer;
import com.recallio.ranked.domain.Duel.Mode;
import com.recallio.ranked.domain.Duel.Status;
import com.recallio.ranked.domain.DuelDraw;
import com.recallio.ranked.domain.DuelDraw.Candidate;
import com.recallio.ranked.domain.DuelGrading;
import com.recallio.ranked.domain.DuelGrading.Correction;
import com.recallio.ranked.domain.RankedExceptions.DuelNotFoundException;
import com.recallio.ranked.domain.RankedExceptions.DuelOverException;
import com.recallio.ranked.domain.RankedExceptions.NotEnoughDuelQuestionsException;
import com.recallio.ranked.domain.RankedExceptions.QuestionNotServedException;
import com.recallio.ranked.domain.RankedRepositories.CardRatingRepository;
import com.recallio.ranked.domain.RankedRepositories.DuelRepository;
import com.recallio.ranked.domain.RankedRepositories.LiveQueueRepository;
import com.recallio.ranked.domain.RankedRepositories.StandingRepository;
import com.recallio.ranked.domain.Season;
import com.recallio.ranked.domain.Standing;
import com.recallio.ranked.domain.glicko.Rating;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;

/**
 * Cas d'usage : chercher un duel du classé, en asynchrone ou en direct, puis le jouer (ADR 0046). Une question est
 * servie une seule fois, et son temps compte dès cet instant ; le serveur corrige chaque réponse.
 */
@ApplicationScoped
public class DuelService {

    /** Un joueur qui ne s'est pas signalé depuis ce temps a quitté la file du direct. */
    static final Duration LIVE_HEARTBEAT = Duration.ofSeconds(15);

    private final DuelRepository duels;
    private final StandingRepository standings;
    private final LiveQueueRepository queue;
    private final CardRatingRepository cardRatings;
    private final CardDifficulty difficulty;
    private final DuelResolution resolution;
    private final CardDirectory cards;
    private final SubjectDirectory subjects;
    private final IdGenerator ids;
    private final Clock clock;

    public DuelService(
            DuelRepository duels,
            StandingRepository standings,
            LiveQueueRepository queue,
            CardRatingRepository cardRatings,
            CardDifficulty difficulty,
            DuelResolution resolution,
            CardDirectory cards,
            SubjectDirectory subjects,
            IdGenerator ids,
            Clock clock) {
        this.duels = duels;
        this.standings = standings;
        this.queue = queue;
        this.cardRatings = cardRatings;
        this.difficulty = difficulty;
        this.resolution = resolution;
        this.cards = cards;
        this.subjects = subjects;
        this.ids = ids;
        this.clock = clock;
    }

    /** Un duel asynchrone : le sien s'il n'est pas fini, sinon le duel ouvert le plus proche en cote, sinon un nouveau. */
    @Transactional
    public Duel findAsync(UserRef user, String subjectSlug) {
        subjects.requirePublished(subjectSlug);
        Instant now = clock.instant();
        Optional<Duel> unfinished = duels.unfinished(user.id(), subjectSlug, Mode.ASYNC, now);
        if (unfinished.isPresent()) {
            return unfinished.get();
        }
        Rating rating = currentRating(user.id(), subjectSlug, now);
        Optional<Duel> open = duels.lockClosestOpen(subjectSlug, user.id(), rating.rating(), now);
        if (open.isPresent()) {
            duels.join(open.get().id(), user.id(), rating, now, now.plus(Duel.TIME_TO_PLAY));
            return duels.find(open.get().id()).orElseThrow();
        }
        Duel duel = draw(
                subjectSlug,
                Mode.ASYNC,
                Status.OPEN,
                now,
                now.plus(Duel.OPEN_FOR),
                List.of(new Seat(user.id(), rating)));
        duels.create(duel);
        return duel;
    }

    /**
     * Le direct : le duel trouvé pendant l'attente, sinon un joueur qui attend dans ce sujet, sinon l'attente. La PWA
     * rappelle toutes les quelques secondes, ce qui signale que le joueur est toujours là.
     *
     * @return le duel trouvé ; vide tant que le joueur attend
     */
    @Transactional
    public Optional<UUID> queueLive(UserRef user, String subjectSlug) {
        subjects.requirePublished(subjectSlug);
        Instant now = clock.instant();
        Optional<UUID> matched = queue.takeMatch(user.id())
                .or(() ->
                        duels.unfinished(user.id(), subjectSlug, Mode.LIVE, now).map(Duel::id));
        if (matched.isPresent()) {
            queue.leave(user.id());
            return matched;
        }
        Optional<UUID> waiting = queue.lockWaiting(subjectSlug, user.id(), now.minus(LIVE_HEARTBEAT));
        if (waiting.isEmpty()) {
            queue.enqueue(user.id(), subjectSlug, now);
            return Optional.empty();
        }
        UUID opponent = waiting.get();
        // Le joueur qui attendait ouvre le duel : il a la place 0.
        List<Seat> seats = List.of(
                new Seat(opponent, currentRating(opponent, subjectSlug, now)),
                new Seat(user.id(), currentRating(user.id(), subjectSlug, now)));
        Duel duel = draw(subjectSlug, Mode.LIVE, Status.FULL, now, now.plus(Duel.LIVE_FOR), seats);
        duels.create(duel);
        queue.match(opponent, duel.id());
        queue.leave(user.id());
        return Optional.of(duel.id());
    }

    @Transactional
    public void leaveLive(UserRef user) {
        queue.leave(user.id());
    }

    /** Le duel, réglé s'il doit l'être. */
    @Transactional
    public Duel view(UserRef user, UUID duelId) {
        return lockedAndSettled(user, duelId, clock.instant());
    }

    /**
     * La question suivante du joueur : la même tant que son temps court, puis la suivante. Une question dont le temps
     * est passé reste sans réponse, et compte fausse.
     *
     * @return vide quand le joueur a joué toutes ses questions
     * @throws DuelOverException si le duel n'attend plus de réponse de sa part
     */
    @Transactional
    public Optional<Turn> next(UserRef user, UUID duelId) {
        Instant now = clock.instant();
        Duel duel = lockedAndSettled(user, duelId, now);
        DuelPlayer me = playing(duel, user);
        Optional<DuelAnswer> pending = me.pending();
        if (pending.isPresent() && !pending.get().isLate(now)) {
            DuelAnswer served = pending.get();
            Duration remaining = Duration.between(now, served.servedAt().plus(Duel.TIME_PER_QUESTION));
            return Optional.of(turn(duel, served.position(), remaining.isNegative() ? Duration.ZERO : remaining));
        }
        int position = me.answers().size();
        if (position >= duel.cardIds().size()) {
            duels.finish(duelId, user.id(), now);
            resolution.settleIfDue(duels.find(duelId).orElseThrow(), now);
            return Optional.empty();
        }
        duels.serve(duelId, user.id(), position, now);
        return Optional.of(turn(duel, position, Duel.TIME_PER_QUESTION));
    }

    /**
     * Corrige la réponse à la question servie. Une réponse juste arrivée après le temps imparti compte fausse.
     *
     * @throws QuestionNotServedException si cette question n'attend pas de réponse
     */
    @Transactional
    public Answered answer(
            UserRef user,
            UUID duelId,
            int position,
            @Nullable Set<Integer> selectedChoices,
            @Nullable List<String> filledBlanks) {
        Instant now = clock.instant();
        Duel duel = lockedAndSettled(user, duelId, now);
        DuelPlayer me = playing(duel, user);
        DuelAnswer served = me.pending()
                .filter(answer -> answer.position() == position)
                .orElseThrow(() -> new QuestionNotServedException(position));
        CardSnapshot card = question(duel, position);
        Correction correction = DuelGrading.grade(card, selectedChoices, filledBlanks);
        boolean late = served.isLate(now);
        boolean counted = correction.correct() && !late;
        duels.answer(duelId, user.id(), position, now, counted);
        difficulty.record(card.cardId(), duel.subjectSlug(), me.ratingBefore(), counted);
        boolean finished = position == duel.cardIds().size() - 1;
        if (finished) {
            duels.finish(duelId, user.id(), now);
            resolution.settleIfDue(duels.find(duelId).orElseThrow(), now);
        }
        return new Answered(correction, counted, late, finished);
    }

    private Duel lockedAndSettled(UserRef user, UUID duelId, Instant now) {
        Duel duel = duels.lock(duelId)
                .filter(found -> found.player(user.id()).isPresent())
                .orElseThrow(() -> new DuelNotFoundException(duelId));
        return resolution.settleIfDue(duel, now);
    }

    private static DuelPlayer playing(Duel duel, UserRef user) {
        DuelPlayer me = duel.player(user.id()).orElseThrow();
        if (!duel.isPlayable() || me.hasFinished()) {
            throw new DuelOverException(duel.id());
        }
        return me;
    }

    private Turn turn(Duel duel, int position, Duration remaining) {
        return new Turn(position, duel.cardIds().size(), question(duel, position), remaining);
    }

    /** Les questions sont des cartes actives au tirage : une carte retirée pendant le duel est un cas rare, une erreur. */
    private CardSnapshot question(Duel duel, int position) {
        UUID cardId = duel.cardIds().get(position);
        return cards.activeCard(cardId)
                .orElseThrow(() -> new IllegalStateException("Question de duel retirée : " + cardId));
    }

    private Rating currentRating(UUID userId, String subjectSlug, Instant now) {
        Season season = Season.of(now);
        return standings
                .find(userId, subjectSlug)
                .map(standing -> standing.inSeason(season))
                .orElseGet(() -> Standing.start(userId, subjectSlug, season))
                .rating();
    }

    /** Un nouveau duel, ses questions tirées près de la cote moyenne de ses joueurs, dans l'ordre de leurs places. */
    private Duel draw(String subjectSlug, Mode mode, Status status, Instant now, Instant expiresAt, List<Seat> seats) {
        List<ActiveCard> active =
                cards.activeCards(new CardFilter(Set.of(subjectSlug), null, Set.of(), CardDifficulty.RATED_FORMATS));
        if (active.size() < Duel.QUESTIONS) {
            throw new NotEnoughDuelQuestionsException(subjectSlug);
        }
        Map<UUID, Rating> known =
                cardRatings.ratings(active.stream().map(ActiveCard::cardId).toList());
        List<Candidate> candidates = active.stream()
                .map(card -> new Candidate(
                        card.cardId(),
                        card.notionId(),
                        known.getOrDefault(card.cardId(), Rating.START).rating()))
                .toList();
        double level = seats.stream()
                .mapToDouble(seat -> seat.rating().rating())
                .average()
                .orElse(Rating.START_RATING);
        UUID id = ids.newId();
        long seed = id.getMostSignificantBits() ^ id.getLeastSignificantBits();
        List<DuelPlayer> seated = IntStream.range(0, seats.size())
                .mapToObj(index -> new DuelPlayer(
                        seats.get(index).userId(), index, seats.get(index).rating(), now, List.of(), null, null))
                .toList();
        return new Duel(
                id,
                subjectSlug,
                mode,
                status,
                Season.of(now),
                DuelDraw.draw(candidates, level, Duel.QUESTIONS, seed),
                now,
                expiresAt,
                null,
                seated);
    }

    /** Un joueur et sa cote, au moment où il entre dans le duel. */
    private record Seat(UUID userId, Rating rating) {}

    /**
     * La question servie au joueur.
     *
     * @param position son rang dans le duel, à partir de 0
     * @param remaining le temps qui lui reste, mesuré par le serveur
     */
    public record Turn(int position, int total, CardSnapshot card, Duration remaining) {}

    /**
     * @param counted vrai si la réponse compte juste : juste, et à temps
     * @param finished vrai si c'était la dernière question du joueur
     */
    public record Answered(Correction correction, boolean counted, boolean late, boolean finished) {}
}
