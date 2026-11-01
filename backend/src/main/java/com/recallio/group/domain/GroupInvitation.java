package com.recallio.group.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Le lien d'invitation d'un groupe : qui le reçoit peut rejoindre le groupe jusqu'à son expiration. */
public record GroupInvitation(UUID groupId, InvitationToken token, Instant createdAt, Instant expiresAt) {

    /** Assez pour qu'une équipe entière rejoigne le groupe, assez court pour qu'un lien égaré ne serve pas longtemps. */
    public static final Duration VALIDITY = Duration.ofDays(7);

    public static GroupInvitation create(UUID groupId, InvitationToken token, Instant now) {
        return new GroupInvitation(groupId, token, now, now.plus(VALIDITY));
    }

    public boolean isActiveAt(Instant now) {
        return now.isBefore(expiresAt);
    }
}
