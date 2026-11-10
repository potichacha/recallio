package com.recallio.runner.sandbox;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Ce qu'on exécute : le code du joueur et les tests qui le jugent, dans un langage (ADR 0030, 0033). Les limites sont
 * celles d'un exercice (ADR 0029), vérifiées ici aussi : le runner ne fait confiance à personne, pas même au backend.
 */
public record Submission(Language language, List<SourceFile> code, List<SourceFile> tests) {

    public static final int MAX_FILES_PER_PART = 10;

    public Submission {
        code = List.copyOf(code);
        tests = List.copyOf(tests);
        requireFiles(language, "code", code, false);
        requireFiles(language, "tests", tests, true);
        Set<String> shared = new HashSet<>(paths(code));
        shared.retainAll(paths(tests));
        if (!shared.isEmpty()) {
            // Go réunit le code et les tests dans un même dossier : un test y remplacerait un fichier du joueur.
            throw new IllegalArgumentException("Le code et les tests ont des fichiers de même chemin : " + shared);
        }
    }

    /**
     * Les fichiers d'une partie ({@code part}) : de 1 à {@value #MAX_FILES_PER_PART}, aux chemins distincts et valides
     * pour le langage. Les programmes du Tour s'en servent aussi ({@link Program}).
     */
    static void requireFiles(Language language, String part, List<SourceFile> files, boolean tests) {
        if (files.isEmpty() || files.size() > MAX_FILES_PER_PART) {
            throw new IllegalArgumentException(
                    "De 1 à %d fichier(s) de %s, pas %d".formatted(MAX_FILES_PER_PART, part, files.size()));
        }
        if (paths(files).size() != files.size()) {
            throw new IllegalArgumentException("Deux fichiers de " + part + " ont le même chemin");
        }
        for (SourceFile file : files) {
            language.pathProblem(file.path(), tests).ifPresent(problem -> {
                throw new IllegalArgumentException(problem);
            });
        }
    }

    private static Set<String> paths(List<SourceFile> files) {
        Set<String> paths = new HashSet<>();
        files.forEach(file -> paths.add(file.path()));
        return paths;
    }

    /**
     * Un fichier source.
     *
     * @param path chemin relatif, avec des {@code /} : {@code fr/recallio/Panier.java}, {@code panier.go}
     */
    public record SourceFile(String path, String content) {

        public static final int MAX_LENGTH = 20_000;

        /**
         * Des segments de lettres, chiffres, {@code _} ou {@code -}, et un fichier {@code .java}, {@code .go},
         * {@code .cpp} ou {@code .hpp} : jamais de « .. ». Chaque langage restreint encore ({@link Language}).
         */
        public static final String PATH_PATTERN = "([A-Za-z0-9_-]+/)*[A-Za-z0-9_-]+\\.(java|go|cpp|hpp)";

        private static final Pattern PATH = Pattern.compile(PATH_PATTERN);

        public SourceFile {
            if (!PATH.matcher(path).matches()) {
                throw new IllegalArgumentException("Chemin de fichier invalide : " + path);
            }
            if (content.length() > MAX_LENGTH) {
                throw new IllegalArgumentException("Le fichier %s dépasse %d caractères".formatted(path, MAX_LENGTH));
            }
        }
    }
}
