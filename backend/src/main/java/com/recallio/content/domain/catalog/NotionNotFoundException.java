package com.recallio.content.domain.catalog;

import com.recallio.platform.domain.ResourceNotFoundException;

/** La notion demandée n'existe pas dans ce sujet, ou elle est retirée. */
public class NotionNotFoundException extends ResourceNotFoundException {

    public static final String CODE = "content.notion-not-found";

    public NotionNotFoundException(String subjectSlug, String notionSlug) {
        super(CODE, "La notion « %s » n'existe pas dans « %s ».".formatted(notionSlug, subjectSlug));
    }
}
