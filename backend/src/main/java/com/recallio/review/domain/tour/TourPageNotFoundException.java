package com.recallio.review.domain.tour;

import com.recallio.platform.domain.ResourceNotFoundException;

/** La page n'est pas dans le Tour : notion inconnue, retirée, sans pages, ou qui n'en a pas autant (ADR 0037). */
public class TourPageNotFoundException extends ResourceNotFoundException {

    public static final String CODE = "review.tour-page-not-found";

    public TourPageNotFoundException(String subjectSlug, String notionSlug, int page) {
        super(
                CODE,
                "La page %d de la notion « %s » n'est pas dans le Tour de « %s »."
                        .formatted(page, notionSlug, subjectSlug));
    }
}
