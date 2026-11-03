package com.recallio.review.domain.exercise;

import com.recallio.platform.domain.UnavailableException;

/** Le runner n'a pas pu exécuter le code (injoignable, saturé, en panne) : la réponse n'est pas jugée, on réessaiera. */
public class RunnerUnavailableException extends UnavailableException {

    public static final String CODE = "review.runner-unavailable";

    public RunnerUnavailableException() {
        super(CODE, "L'exécution du code est indisponible pour l'instant : réessaie dans un moment.");
    }
}
