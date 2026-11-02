package com.recallio.runner.sandbox;

import java.util.List;

/**
 * Le résultat d'une exécution (ADR 0030).
 *
 * @param tests chaque test et son résultat ; aucun si le code n'a pas compilé ou n'a pas fini à temps
 * @param output les erreurs de compilation, ou l'explication d'un échec du runner ; vide sinon
 */
public record RunResult(Status status, List<TestOutcome> tests, String output) {

    public RunResult {
        tests = List.copyOf(tests);
    }

    public enum Status {
        /** Tous les tests passent. */
        PASSED,
        /** Au moins un test échoue, ou les tests ne sont pas allés au bout. */
        FAILED,
        /** Le code ne compile pas : {@code output} contient les erreurs de javac. */
        COMPILATION_ERROR,
        /** L'exécution a dépassé le délai et a été arrêtée. */
        TIMEOUT,
        /** Le runner n'a pas pu exécuter le code : une panne, jamais la faute du joueur. */
        ERROR
    }

    /**
     * @param message pourquoi le test échoue (le message de l'assertion) ; vide s'il passe
     */
    public record TestOutcome(String className, String name, boolean passed, String message) {}

    static RunResult of(List<TestOutcome> tests) {
        boolean passed = !tests.isEmpty() && tests.stream().allMatch(TestOutcome::passed);
        return new RunResult(passed ? Status.PASSED : Status.FAILED, tests, "");
    }

    static RunResult failure(Status status, String output) {
        return new RunResult(status, List.of(), output);
    }
}
