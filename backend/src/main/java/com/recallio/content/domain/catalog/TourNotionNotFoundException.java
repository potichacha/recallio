package com.recallio.content.domain.catalog;

import com.recallio.platform.domain.ResourceNotFoundException;

/** La notion demandée n'a pas de page dans le Tour du sujet (ADR 0037), ou n'existe pas. */
public class TourNotionNotFoundException extends ResourceNotFoundException {

    public static final String CODE = "content.tour-notion-not-found";

    public TourNotionNotFoundException(String subjectSlug, String notionSlug) {
        super(CODE, "La notion « %s » n'est pas dans le Tour de « %s ».".formatted(notionSlug, subjectSlug));
    }
}
