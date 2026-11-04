package com.recallio.review.domain.tour;

import com.recallio.platform.domain.ResourceNotFoundException;

/** La notion n'a pas de questions à valider : inconnue, retirée, hors du Tour, ou d'un sujet non publié (ADR 0037). */
public class TourNotionNotFoundException extends ResourceNotFoundException {

    public static final String CODE = "review.tour-notion-not-found";

    public TourNotionNotFoundException(String subjectSlug, String notionSlug) {
        super(CODE, "La notion « %s » n'a pas de questions dans le Tour de « %s ».".formatted(notionSlug, subjectSlug));
    }
}
