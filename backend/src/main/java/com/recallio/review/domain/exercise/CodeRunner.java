package com.recallio.review.domain.exercise;

import com.recallio.content.ExerciseFiles.SourceFile;
import java.util.List;

/**
 * Exécute le code d'un joueur contre les tests d'un exercice (ADR 0030) : un port, implémenté par l'appel au service
 * runner. Le backend n'exécute jamais de code lui-même.
 */
public interface CodeRunner {

    /** @throws RunnerUnavailableException si le runner ne répond pas, est saturé ou n'a pas pu exécuter le code */
    ExerciseRun run(List<SourceFile> code, List<SourceFile> tests);
}
