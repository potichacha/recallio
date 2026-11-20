package com.recallio.review.domain.daily;

import java.time.Instant;
import java.util.UUID;

/**
 * La réponse d'un joueur à une question d'un défi du jour (ADR 0067) : une seule, définitive. Seul son verdict est gardé.
 *
 * @param requestId identifiant choisi par le client : renvoyer la même requête rend la même correction
 */
public record DailyChallengeAnswer(
        UUID challengeId, UUID userId, int position, boolean correct, Instant answeredAt, UUID requestId) {

    /** Vrai si cette réponse est celle de ce joueur à cette question de ce défi. */
    public boolean answers(UUID challengeId, UUID userId, int position) {
        return this.challengeId.equals(challengeId) && this.userId.equals(userId) && this.position == position;
    }
}
