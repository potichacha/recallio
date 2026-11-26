package com.recallio.entitlement.domain;

import com.recallio.platform.domain.Page;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Les lots de codes d'accès, leurs codes et leurs utilisations (ADR 0079). */
public interface AccessCodeRepository {

    /** Enregistre un lot et ses codes : leur empreinte et leurs derniers caractères, jamais le code. */
    void add(AccessCodeBatch batch, List<NewCode> codes);

    /** Les lots, le plus récent d'abord. */
    Page<BatchSummary> list(int page, int size);

    Optional<BatchSummary> find(UUID batchId);

    /** Les codes d'un lot, dans l'ordre de leur création. */
    List<BatchSummary.Code> codesOf(UUID batchId);

    /**
     * Révoque un lot qui ne l'est pas encore.
     *
     * @return faux si le lot n'existe pas, ou s'il est déjà révoqué
     */
    boolean revoke(UUID batchId, Instant at);

    /** Le code de cette empreinte, sans le retenir. */
    Optional<RedeemableCode> find(String fingerprint);

    /**
     * Le code de cette empreinte, verrouillé jusqu'à la fin de la transaction : deux comptes qui l'utilisent en même
     * temps passent l'un après l'autre, et son nombre d'utilisations n'est jamais dépassé.
     */
    Optional<RedeemableCode> lock(String fingerprint);

    boolean hasRedeemed(UUID codeId, UUID userId);

    /** Compte une utilisation de plus du code, et note quel compte l'a utilisé. */
    void redeem(RedeemableCode code, UUID userId, Instant at);

    /**
     * Un code à enregistrer.
     *
     * @param fingerprint son empreinte ({@link AccessCode#fingerprint()})
     * @param suffix ses derniers caractères ({@link AccessCode#suffix()})
     */
    record NewCode(UUID id, String fingerprint, String suffix) {}
}
