package com.recallio.runner.sandbox;

import com.recallio.runner.sandbox.Submission.SourceFile;
import java.util.List;

/**
 * Un programme à lancer, sans tests (ADR 0037) : l'exemple d'une page du Tour, ou ce qu'en a fait l'utilisateur. Java
 * demande un {@value #JAVA_ENTRY_POINT} à la racine, qui contient {@code main} ; Go, un {@code package main}, que vérifie
 * le compilateur.
 */
public record Program(Language language, List<SourceFile> files) {

    public static final String JAVA_ENTRY_POINT = "Main.java";

    public Program {
        files = List.copyOf(files);
        Submission.requireFiles(language, "programme", files, false);
        if (language == Language.JAVA
                && files.stream().noneMatch(file -> file.path().equals(JAVA_ENTRY_POINT))) {
            throw new IllegalArgumentException("Un programme Java commence par un fichier " + JAVA_ENTRY_POINT);
        }
    }
}
