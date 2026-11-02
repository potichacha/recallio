package com.recallio.review.domain.exercise;

import java.util.List;

/**
 * Le résultat de l'exécution d'un exercice de code par ses tests (ADR 0030). Une panne du runner n'en est pas un : elle
 * lève {@link RunnerUnavailableException}, et la réponse du joueur n'est pas jugée.
 *
 * @param tests chaque test et son résultat ; aucun si le code n'a pas compilé ou n'a pas fini à temps
 * @param output les erreurs de compilation, ou ce qui a arrêté les tests ; vide sinon
 */
public record ExerciseRun(Status status, List<TestResult> tests, String output) {

    public ExerciseRun {
        tests = List.copyOf(tests);
    }

    /** L'exercice est résolu : tous ses tests passent. */
    public boolean solved() {
        return status == Status.PASSED;
    }

    public enum Status {
        PASSED,
        FAILED,
        COMPILATION_ERROR,
        TIMEOUT
    }

    /** @param message pourquoi le test échoue ; vide s'il passe */
    public record TestResult(String className, String name, boolean passed, String message) {}
}
