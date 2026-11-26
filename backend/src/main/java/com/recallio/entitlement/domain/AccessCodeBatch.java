package com.recallio.entitlement.domain;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Un lot de codes d'accès, créé par un admin (ADR 0079). Le lot porte ce que ses codes donnent : aujourd'hui, des jours
 * de Plus.
 *
 * @param label ce qui le fait reconnaître (« Testeurs de la bêta »)
 * @param plusDays les jours de Plus que donne chaque utilisation d'un de ses codes
 * @param maxRedemptionsPerCode le nombre de comptes qui peuvent utiliser un même code
 * @param expiresAt l'instant à partir duquel ses codes ne marchent plus ; {@code null} s'ils n'expirent pas
 * @param revokedAt quand un admin l'a révoqué ; {@code null} s'il ne l'est pas
 */
public record AccessCodeBatch(
        UUID id,
        String label,
        int plusDays,
        int maxRedemptionsPerCode,
        @Nullable Instant expiresAt,
        Instant createdAt,
        @Nullable Instant revokedAt) {

    public static final int MAX_LABEL_LENGTH = 100;

    /** Un lot se lit d'un coup, codes compris : il reste petit. */
    public static final int MAX_CODES = 200;

    /** Dix ans : au-delà, c'est une faute de frappe. */
    public static final int MAX_PLUS_DAYS = 3650;

    public static final int MAX_REDEMPTIONS_PER_CODE = 10_000;

    public AccessCodeBatch {
        if (label.isBlank() || label.length() > MAX_LABEL_LENGTH) {
            throw new IllegalArgumentException(
                    "Le libellé d'un lot fait de 1 à %d caractères".formatted(MAX_LABEL_LENGTH));
        }
        if (plusDays < 1 || plusDays > MAX_PLUS_DAYS) {
            throw new IllegalArgumentException("Un code donne de 1 à %d jours".formatted(MAX_PLUS_DAYS));
        }
        if (maxRedemptionsPerCode < 1 || maxRedemptionsPerCode > MAX_REDEMPTIONS_PER_CODE) {
            throw new IllegalArgumentException("Un code s'utilise de 1 à %d fois".formatted(MAX_REDEMPTIONS_PER_CODE));
        }
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpiredAt(Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }

    /** Vrai si ses codes marchent encore : ni révoqué, ni expiré. */
    public boolean isOpenAt(Instant now) {
        return !isRevoked() && !isExpiredAt(now);
    }
}
