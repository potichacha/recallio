package com.recallio.content.domain.catalog;

import com.recallio.platform.domain.ResourceNotFoundException;

public class SubjectNotFoundException extends ResourceNotFoundException {

    public static final String CODE = "content.subject-not-found";

    public SubjectNotFoundException(String slug) {
        super(CODE, "Aucun sujet ne correspond à « " + slug + " ».");
    }
}
