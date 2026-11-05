package com.recallio.feedback.domain;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Un signalement d'erreur dans le contenu (ADR 0042) : une carte, ou une page du Tour. Il reste ouvert jusqu'à ce qu'un
 * admin le traite.
 *
 * @param subjectSlug le sujet du contenu signalé
 * @param comment ce que l'utilisateur a remarqué ; vide s'il n'a rien précisé
 * @param resolution comment un admin l'a traité ; {@code null} tant qu'il est ouvert
 * @param resolvedAt quand ; {@code null} tant qu'il est ouvert
 */
public record ContentReport(
        UUID id,
        UUID userId,
        String subjectSlug,
        ReportTarget target,
        ReportReason reason,
        String comment,
        Instant createdAt,
        @Nullable ReportResolution resolution,
        @Nullable Instant resolvedAt) {

    public static final int MAX_COMMENT_LENGTH = 1000;

    public ContentReport {
        if (comment.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException(
                    "Un commentaire fait au plus %d caractères".formatted(MAX_COMMENT_LENGTH));
        }
        if ((resolution == null) != (resolvedAt == null)) {
            throw new IllegalArgumentException("Un signalement traité a une résolution et une date, l'autre aucune");
        }
    }

    public boolean isOpen() {
        return resolution == null;
    }
}
