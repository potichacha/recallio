package com.recallio.content;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Les fichiers d'un exercice de code ({@link CardType#CODING_EXERCISE}, ADR 0029) : le code de départ qu'on modifie, la
 * solution modèle, et les tests qui jugent l'un et l'autre.
 *
 * <p>La solution a exactement les fichiers du code de départ : le joueur rend les mêmes fichiers, et les tests
 * s'exécutent sur eux. Tous les fichiers sont d'un même langage ({@link #language()}, ADR 0033).
 */
public record ExerciseFiles(List<SourceFile> starter, List<SourceFile> solution, List<SourceFile> tests) {

    public static final int MAX_FILES_PER_PART = 10;

    public ExerciseFiles {
        starter = List.copyOf(starter);
        solution = List.copyOf(solution);
        tests = List.copyOf(tests);
        requireFiles("code de départ", starter);
        requireFiles("solution", solution);
        requireFiles("tests", tests);
        if (!paths(starter).equals(paths(solution))) {
            throw new IllegalArgumentException("La solution doit avoir exactement les fichiers du code de départ : "
                    + paths(starter) + " contre " + paths(solution));
        }
        Set<String> shared = new HashSet<>(paths(starter));
        shared.retainAll(paths(tests));
        if (!shared.isEmpty()) {
            throw new IllegalArgumentException(
                    "Un test ne peut pas remplacer un fichier du code de départ : " + shared);
        }
        requireOneLanguage(starter, tests);
    }

    /** Le langage de l'exercice : celui de tous ses fichiers. */
    public CodeLanguage language() {
        return language(tests.getFirst());
    }

    private static void requireOneLanguage(List<SourceFile> code, List<SourceFile> tests) {
        CodeLanguage language = language(tests.getFirst());
        for (SourceFile file : code) {
            requirePath(language, file, false);
        }
        for (SourceFile file : tests) {
            requirePath(language, file, true);
        }
    }

    private static void requirePath(CodeLanguage language, SourceFile file, boolean test) {
        if (language(file) != language) {
            throw new IllegalArgumentException("Un exercice est écrit dans un seul langage : %s n'est pas un fichier %s"
                    .formatted(file.path(), language));
        }
        language.pathProblem(file.path(), test).ifPresent(problem -> {
            throw new IllegalArgumentException("Exercice " + language + " : " + problem);
        });
    }

    /** Le chemin d'un fichier a déjà été vérifié ({@link SourceFile#PATH_PATTERN}) : son extension est connue. */
    private static CodeLanguage language(SourceFile file) {
        return CodeLanguage.of(file.path()).orElseThrow();
    }

    private static void requireFiles(String part, List<SourceFile> files) {
        if (files.isEmpty() || files.size() > MAX_FILES_PER_PART) {
            throw new IllegalArgumentException("Un exercice a de 1 à %d fichier(s) de %s, pas %d"
                    .formatted(MAX_FILES_PER_PART, part, files.size()));
        }
        if (paths(files).size() != files.size()) {
            throw new IllegalArgumentException("Deux fichiers de " + part + " ont le même chemin");
        }
    }

    private static Set<String> paths(List<SourceFile> files) {
        return files.stream().map(SourceFile::path).collect(Collectors.toSet());
    }

    /**
     * Un fichier source d'un exercice.
     *
     * @param path chemin relatif, avec des {@code /} : {@code fr/recallio/Panier.java}, {@code panier.go}
     */
    public record SourceFile(String path, String content) {

        public static final int MAX_LENGTH = 20_000;

        /**
         * Des segments de lettres, chiffres, {@code _} ou {@code -}, et un fichier {@code .java} ou {@code .go}. Chaque
         * langage restreint encore ({@link CodeLanguage}).
         */
        public static final String PATH_PATTERN = "([A-Za-z0-9_-]+/)*[A-Za-z0-9_-]+\\.(java|go)";

        private static final Pattern PATH = Pattern.compile(PATH_PATTERN);

        public SourceFile {
            if (!PATH.matcher(path).matches()) {
                throw new IllegalArgumentException(
                        "Chemin de fichier invalide (un fichier .java ou .go, sans « .. ») : " + path);
            }
            if (content.isBlank() || content.length() > MAX_LENGTH) {
                throw new IllegalArgumentException(
                        "Le fichier %s doit être non vide et faire au plus %d caractères".formatted(path, MAX_LENGTH));
            }
        }
    }
}
