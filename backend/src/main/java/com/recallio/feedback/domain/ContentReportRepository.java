package com.recallio.feedback.domain;

import com.recallio.platform.domain.Page;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Les signalements (ADR 0042), dans la table {@code content_report}. */
public interface ContentReportRepository {

    void add(ContentReport report);

    /** Vrai si cet utilisateur a déjà signalé ce contenu, et que ce signalement est encore ouvert. */
    boolean hasOpenReport(UUID userId, ReportTarget target);

    /** Les signalements encore ouverts de cet utilisateur. */
    long openReportsBy(UUID userId);

    Optional<ContentReport> find(UUID id);

    /**
     * Les signalements ouverts, les plus anciens d'abord, ou traités, les derniers traités d'abord.
     *
     * @param page à partir de 0
     */
    Page<ContentReport> list(boolean open, int page, int size);

    /** @return faux si le signalement était déjà traité : sa résolution ne change pas */
    boolean resolve(UUID id, ReportResolution resolution, Instant resolvedAt);
}
