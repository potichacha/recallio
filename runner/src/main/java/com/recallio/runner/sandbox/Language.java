package com.recallio.runner.sandbox;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Les langages des exercices (ADR 0030, 0033, 0053) : chacun a son image de bac à sable, ses règles de chemin et le
 * format de son rapport de tests.
 */
public enum Language {
    /** Des fichiers {@code .java}, dans des packages si l'on veut ({@code fr/recallio/Panier.java}). */
    JAVA("([A-Za-z0-9_-]+/)*[A-Za-z0-9_-]+\\.java", ""),

    /** Des fichiers {@code .go} à plat, un seul package ; les tests, et eux seuls, s'appellent {@code *_test.go}. */
    GO("[A-Za-z0-9_-]+\\.go", "_test.go"),

    /**
     * Des fichiers {@code .cpp} et {@code .hpp} à plat ; les tests doctest, et eux seuls, s'appellent
     * {@code *_test.cpp} (ADR 0053).
     */
    CPP("[A-Za-z0-9_-]+\\.(cpp|hpp)", "_test.cpp");

    private final Pattern path;
    /** Le suffixe qui distingue les fichiers de tests, ou rien quand le langage n'en impose pas. */
    private final String testSuffix;

    Language(String path, String testSuffix) {
        this.path = Pattern.compile(path);
        this.testSuffix = testSuffix;
    }

    /**
     * Le problème d'un chemin de fichier de ce langage, s'il en a un.
     *
     * @param test vrai pour un fichier de tests
     */
    Optional<String> pathProblem(String filePath, boolean test) {
        if (!path.matcher(filePath).matches()) {
            return Optional.of("Chemin de fichier invalide pour %s : %s".formatted(this, filePath));
        }
        if (!testSuffix.isEmpty() && filePath.endsWith(testSuffix) != test) {
            return Optional.of(
                    test
                            ? "Un fichier de tests %s s'appelle *%s : %s".formatted(this, testSuffix, filePath)
                            : "Seuls les fichiers de tests s'appellent *%s : %s".formatted(testSuffix, filePath));
        }
        return Optional.empty();
    }
}
