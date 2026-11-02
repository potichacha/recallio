package com.recallio.live.domain;

import com.recallio.live.domain.LiveGame.Standing;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Le résultat d'une partie finie, conservé pour le groupe (ADR 0023). */
public record LiveGameResult(
        UUID gameId,
        UUID groupId,
        String subjectSlug,
        UUID hostId,
        Instant startedAt,
        Instant finishedAt,
        int questionCount,
        List<Standing> standings) {

    public LiveGameResult {
        standings = List.copyOf(standings);
    }
}
