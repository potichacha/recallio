package com.recallio.review.domain.playground;

import com.recallio.platform.domain.TooManyRequestsException;

/**
 * L'utilisateur a déjà une exécution en cours, d'un exercice ou d'un programme : une à la fois, pour qu'il n'occupe
 * qu'une place du runner (ADR 0037).
 */
public class ProgramRunInProgressException extends TooManyRequestsException {

    public static final String CODE = "review.program-run-in-progress";

    public ProgramRunInProgressException() {
        super(CODE, "Une exécution est déjà en cours : attends son résultat avant d'en lancer une autre.");
    }
}
