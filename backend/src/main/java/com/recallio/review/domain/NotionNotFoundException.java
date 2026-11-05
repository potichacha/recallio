package com.recallio.review.domain;

import com.recallio.platform.domain.ResourceNotFoundException;

/** Aucune notion active de ce slug dans ce sujet publié. */
public class NotionNotFoundException extends ResourceNotFoundException {

    public static final String CODE = "review.notion-not-found";

    public NotionNotFoundException(String subjectSlug, String notionSlug) {
        super(CODE, "Le sujet « %s » n'a pas de notion « %s ».".formatted(subjectSlug, notionSlug));
    }
}
