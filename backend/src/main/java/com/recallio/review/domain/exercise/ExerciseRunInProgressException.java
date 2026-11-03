package com.recallio.review.domain.exercise;

import com.recallio.platform.domain.TooManyRequestsException;

/** L'utilisateur a déjà une exécution en cours : une à la fois, pour qu'un seul joueur ne monopolise pas le runner. */
public class ExerciseRunInProgressException extends TooManyRequestsException {

    public static final String CODE = "review.exercise-run-in-progress";

    public ExerciseRunInProgressException() {
        super(CODE, "Une exécution est déjà en cours : attends son résultat avant d'en lancer une autre.");
    }
}
