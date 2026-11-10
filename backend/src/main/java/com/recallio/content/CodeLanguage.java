package com.recallio.content;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Le langage des fichiers d'un exercice de code (ADR 0033, 0053), déduit de leur extension. Il choisit le bac à sable
 * qui les exécute, et impose ses règles de chemin ; le runner les vérifie de nouveau, sans faire confiance au backend.
 */
public enum CodeLanguage {
    /** Des fichiers {@code .java}, dans des packages si l'on veut ({@code fr/recallio/Panier.java}). */
    JAVA(List.of(".java"), "([A-Za-z0-9_-]+/)*[A-Za-z0-9_-]+\\.java", ""),

    /** Des fichiers {@code .go} à plat, un seul package ; les tests, et eux seuls, s'appellent {@code *_test.go}. */
    GO(List.of(".go"), "[A-Za-z0-9_-]+\\.go", "_test.go"),

    /**
     * Des fichiers {@code .cpp} et {@code .hpp} à plat ; les tests doctest, et eux seuls, s'appellent
     * {@code *_test.cpp} (ADR 0053).
     */
    CPP(List.of(".cpp", ".hpp"), "[A-Za-z0-9_-]+\\.(cpp|hpp)", "_test.cpp");

    private final List<String> extensions;
    private final Pattern path;
    /** Le suffixe qui distingue les fichiers de tests, ou rien quand le langage n'en impose pas. */
    private final String testSuffix;

    CodeLanguage(List<String> extensions, String path, String testSuffix) {
        this.extensions = extensions;
        this.path = Pattern.compile(path);
        this.testSuffix = testSuffix;
    }

    /** Le langage d'un fichier, d'après son extension. */
    public static Optional<CodeLanguage> of(String filePath) {
        for (CodeLanguage language : values()) {
            if (language.extensions.stream().anyMatch(filePath::endsWith)) {
                return Optional.of(language);
            }
        }
        return Optional.empty();
    }

    /**
     * Le problème d'un chemin de fichier de ce langage, s'il en a un.
     *
     * @param test vrai pour un fichier de tests
     */
    Optional<String> pathProblem(String filePath, boolean test) {
        if (!path.matcher(filePath).matches()) {
            return Optional.of("chemin invalide pour un fichier %s : %s".formatted(this, filePath));
        }
        if (!testSuffix.isEmpty() && filePath.endsWith(testSuffix) != test) {
            return Optional.of(
                    test
                            ? "un fichier de tests %s s'appelle *%s : %s".formatted(this, testSuffix, filePath)
                            : "seuls les fichiers de tests s'appellent *%s : %s".formatted(testSuffix, filePath));
        }
        return Optional.empty();
    }
}
