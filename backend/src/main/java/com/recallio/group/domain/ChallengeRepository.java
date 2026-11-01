package com.recallio.group.domain;

import com.recallio.group.domain.ChallengeRun.Attempt;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ChallengeRepository {

    /** Enregistre le défi et sa série de cartes. */
    void create(Challenge challenge);

    Optional<Challenge> findById(UUID challengeId);

    /** Les défis du groupe, du plus récent au plus ancien, au plus {@code limit}. */
    List<Challenge> findByGroup(UUID groupId, int limit);

    /** Les cartes présentées à un membre, et ses réponses. */
    List<Attempt> attempts(UUID challengeId, UUID userId);

    /** Les parties de tous les membres qui ont commencé le défi. */
    Map<UUID, List<Attempt>> attemptsByUser(UUID challengeId);

    /** Présente une carte ; sans effet si elle l'est déjà (deux onglets ouverts). */
    void present(UUID challengeId, UUID userId, int position, Instant presentedAt);

    /**
     * Enregistre la réponse à une carte présentée ; sans effet si elle en a déjà une.
     *
     * @return vrai si cette réponse-ci a été enregistrée
     */
    boolean answer(UUID challengeId, UUID userId, int position, Instant answeredAt, boolean correct, boolean timedOut);
}
