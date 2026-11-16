package com.recallio.runner.sandbox;

import com.recallio.runner.sandbox.Submission.SourceFile;
import java.util.List;

/**
 * Un programme à lancer, sans tests (ADR 0037) : l'exemple d'une page du Tour, ou ce qu'en a fait l'utilisateur. Java
 * demande un {@value #JAVA_ENTRY_POINT} à la racine, qui contient {@code main} ; Python, un {@value #PYTHON_ENTRY_POINT},
 * le script lancé (ADR 0062) ; Go et C++, une fonction {@code main}, que vérifie le compilateur.
 */
public record Program(Language language, List<SourceFile> files) {

    public static final String JAVA_ENTRY_POINT = "Main.java";

    public static final String PYTHON_ENTRY_POINT = "main.py";

    public Program {
        files = List.copyOf(files);
        Submission.requireFiles(language, "programme", files, false);
        requireEntryPoint(language, files, Language.JAVA, JAVA_ENTRY_POINT);
        requireEntryPoint(language, files, Language.PYTHON, PYTHON_ENTRY_POINT);
    }

    private static void requireEntryPoint(
            Language language, List<SourceFile> files, Language concerned, String entryPoint) {
        if (language == concerned
                && files.stream().noneMatch(file -> file.path().equals(entryPoint))) {
            throw new IllegalArgumentException(
                    "Un programme %s commence par un fichier %s".formatted(concerned, entryPoint));
        }
    }
}
