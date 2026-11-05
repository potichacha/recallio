package com.recallio.review.domain.tour;

import com.recallio.platform.domain.ResourceNotFoundException;

/** La page n'a pas d'« À toi » : elle n'existe pas, ou n'en propose pas (ADR 0037). */
public class TourTaskNotFoundException extends ResourceNotFoundException {

    public static final String CODE = "review.tour-task-not-found";

    public TourTaskNotFoundException(String subjectSlug, String notionSlug, int page) {
        super(
                CODE,
                "La page %d de la notion « %s » du Tour de « %s » n'a pas d'« À toi »."
                        .formatted(page, notionSlug, subjectSlug));
    }
}
