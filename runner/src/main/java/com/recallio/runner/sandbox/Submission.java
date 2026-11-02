package com.recallio.runner.sandbox;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Ce qu'on exécute : le code du joueur et les tests qui le jugent (ADR 0030). Les limites sont celles d'un exercice
 * (ADR 0029), vérifiées ici aussi : le runner ne fait confiance à personne, pas même au backend.
 */
public record Submission(List<SourceFile> code, List<SourceFile> tests) {

    public static final int MAX_FILES_PER_PART = 10;

    public Submission {
        code = List.copyOf(code);
        tests = List.copyOf(tests);
        requireFiles("code", code);
        requireFiles("tests", tests);
    }

    private static void requireFiles(String part, List<SourceFile> files) {
        if (files.isEmpty() || files.size() > MAX_FILES_PER_PART) {
            throw new IllegalArgumentException(
                    "De 1 à %d fichier(s) de %s, pas %d".formatted(MAX_FILES_PER_PART, part, files.size()));
        }
        Set<String> paths = new HashSet<>();
        for (SourceFile file : files) {
            if (!paths.add(file.path())) {
                throw new IllegalArgumentException("Deux fichiers de " + part + " ont le chemin " + file.path());
            }
        }
    }

    /**
     * Un fichier source.
     *
     * @param path chemin relatif, avec des {@code /} : {@code fr/recallio/Panier.java}
     */
    public record SourceFile(String path, String content) {

        public static final int MAX_LENGTH = 20_000;

        /** Des segments de lettres, chiffres, {@code _} ou {@code -}, et un fichier {@code .java} : jamais de « .. ». */
        public static final String PATH_PATTERN = "([A-Za-z0-9_-]+/)*[A-Za-z0-9_-]+\\.java";

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
