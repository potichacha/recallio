package com.recallio.group.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.group.domain.Challenge;
import com.recallio.group.domain.ChallengeRepository;
import com.recallio.group.domain.ChallengeRun;
import com.recallio.group.domain.ChallengeRun.Attempt;
import com.recallio.group.domain.GroupExceptions.ChallengeClosedException;
import com.recallio.group.domain.GroupExceptions.ChallengeNotFoundException;
import com.recallio.group.domain.GroupExceptions.NoPendingCardException;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Jouer un défi (ADR 0020), carte après carte. Le serveur garde l'heure de présentation de chaque carte : recharger la
 * page ne relance pas le chrono, et une réponse hors délai compte comme fausse. Les défis ne touchent pas à la
 * révision personnelle : ni note FSRS, ni points.
 */
@ApplicationScoped
public class ChallengePlayService {

    private final ChallengeRepository challenges;
    private final ChallengeService challengeService;
    private final CardDirectory cards;
    private final Clock clock;

    public ChallengePlayService(
            ChallengeRepository challenges, ChallengeService challengeService, CardDirectory cards, Clock clock) {
        this.challenges = challenges;
        this.challengeService = challengeService;
        this.cards = cards;
        this.clock = clock;
    }

    /**
     * La carte à jouer : celle déjà présentée si son délai court encore, sinon la suivante. Une carte présentée dont le
     * délai est passé sans réponse compte comme fausse.
     *
     * @throws ChallengeClosedException si le défi est terminé et que la partie ne l'est pas
     */
    @Transactional
    public Step next(UserRef user, UUID challengeId) {
        Challenge challenge = visibleChallenge(user, challengeId);
        Instant now = clock.instant();
        ChallengeRun run = run(challenge, user);
        Attempt pending = run.pending().orElse(null);
        if (pending != null) {
            if (challenge.answeredInTime(pending.presentedAt(), now)) {
                return present(challenge, user, run, pending, now);
            }
            challenges.answer(
                    challenge.id(),
                    user.id(),
                    pending.position(),
                    challenge.deadline(pending.presentedAt()),
                    false,
                    true);
            run = run(challenge, user);
        }
        if (run.isFinished()) {
            return new Step(null, challenge.cardCount(), run.score(), true);
        }
        if (!challenge.isOpenAt(now)) {
            throw new ChallengeClosedException();
        }
        challenges.present(challenge.id(), user.id(), run.nextPosition(), now);
        run = run(challenge, user);
        return present(challenge, user, run, run.pending().orElseThrow(), now);
    }

    /**
     * Corrige la réponse à la carte présentée. La renvoyer (nouvelle tentative réseau) rend la correction déjà
     * enregistrée.
     *
     * @throws NoPendingCardException si cette carte n'a pas été présentée à l'utilisateur
     */
    @Transactional
    public AnswerResult answer(UserRef user, UUID challengeId, int position, Set<Integer> selected) {
        Challenge challenge = visibleChallenge(user, challengeId);
        Attempt attempt = attempt(challenge, user, position);
        CardSnapshot card = cards.activeCard(challenge.cardIds().get(position))
                .orElseThrow(() -> new NoPendingCardException(position));
        if (!attempt.isAnswered()) {
            Instant now = clock.instant();
            boolean inTime = challenge.answeredInTime(attempt.presentedAt(), now);
            challenges.answer(
                    challenge.id(), user.id(), position, now, inTime && card.isCorrectSelection(selected), !inTime);
            attempt = attempt(challenge, user, position);
        }
        return new AnswerResult(
                attempt.isCorrect(),
                attempt.timedOut(),
                card.correctChoices(),
                card.answer(),
                run(challenge, user).isFinished());
    }

    /** Présente la carte ; une carte retirée du catalogue depuis le lancement du défi compte comme fausse. */
    private Step present(Challenge challenge, UserRef user, ChallengeRun run, Attempt attempt, Instant now) {
        var card = cards.activeCard(challenge.cardIds().get(attempt.position()));
        if (card.isEmpty()) {
            challenges.answer(challenge.id(), user.id(), attempt.position(), now, false, false);
            return next(user, challenge.id());
        }
        Duration remaining = Duration.between(now, challenge.deadline(attempt.presentedAt()));
        return new Step(
                new PresentedCard(attempt.position(), card.get(), remaining.isNegative() ? Duration.ZERO : remaining),
                challenge.cardCount(),
                run.score(),
                false);
    }

    private Challenge visibleChallenge(UserRef user, UUID challengeId) {
        Challenge challenge =
                challenges.findById(challengeId).orElseThrow(() -> new ChallengeNotFoundException(challengeId));
        challengeService.visibleGroupOf(user, challenge);
        return challenge;
    }

    private ChallengeRun run(Challenge challenge, UserRef user) {
        return new ChallengeRun(challenge, challenges.attempts(challenge.id(), user.id()));
    }

    private Attempt attempt(Challenge challenge, UserRef user, int position) {
        return run(challenge, user).attempts().stream()
                .filter(attempt -> attempt.position() == position)
                .findFirst()
                .orElseThrow(() -> new NoPendingCardException(position));
    }

    /**
     * @param card la carte à jouer ; absente une fois la partie terminée
     * @param score les bonnes réponses jusqu'ici
     */
    public record Step(@Nullable PresentedCard card, int cardCount, int score, boolean finished) {}

    /** @param remaining le temps qui reste pour répondre, mesuré par le serveur */
    public record PresentedCard(int position, CardSnapshot card, Duration remaining) {}

    /** @param finished vrai si c'était la dernière carte du défi */
    public record AnswerResult(
            boolean correct, boolean timedOut, Set<Integer> correctChoices, String explanation, boolean finished) {}
}
