package com.recallio.review.domain;

import com.recallio.platform.domain.ResourceNotFoundException;

/** Pas d'essai pour ce sujet (ADR 0050) : il est inconnu, non publié, ou n'a pas encore de notions essentielles. */
public class TrialUnavailableException extends ResourceNotFoundException {

    public static final String CODE = "review.trial-unavailable";

    public TrialUnavailableException(String subjectSlug) {
        super(CODE, "Le sujet « %s » n'a pas encore d'essai.".formatted(subjectSlug));
    }
}
