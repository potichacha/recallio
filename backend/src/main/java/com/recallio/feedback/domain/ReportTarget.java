package com.recallio.feedback.domain;

import java.util.UUID;

/** Ce qu'on signale (ADR 0042) : une carte, ou une page du Tour. */
public sealed interface ReportTarget {

    record CardTarget(UUID cardId) implements ReportTarget {}

    /** @param page le rang de la page dans la notion, à partir de 1 */
    record TourPageTarget(String subjectSlug, String notionSlug, int page) implements ReportTarget {

        public TourPageTarget {
            if (page < 1) {
                throw new IllegalArgumentException("Les pages d'une notion se comptent à partir de 1 : " + page);
            }
        }
    }
}
