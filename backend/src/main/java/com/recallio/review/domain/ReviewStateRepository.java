package com.recallio.review.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Mémoire des cartes par utilisateur. Une carte sans état est nouvelle pour cet utilisateur. */
public interface ReviewStateRepository {

    Map<UUID, ReviewState> findAllByUser(UUID userId);

    /** L'état de la carte, verrouillé jusqu'à la fin de la transaction : deux réponses simultanées sont sérialisées. */
    Optional<ReviewState> findForUpdate(UUID userId, UUID cardId);

    void save(UUID userId, UUID cardId, ReviewState state);

    /** Nombre de cartes révisées pour la première fois depuis {@code since} : base du quota quotidien de nouvelles. */
    long countIntroducedSince(UUID userId, Instant since);
}
