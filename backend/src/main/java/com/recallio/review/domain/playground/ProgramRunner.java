package com.recallio.review.domain.playground;

import com.recallio.content.CodeLanguage;
import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.review.domain.exercise.RunnerUnavailableException;
import java.util.List;

/**
 * Lance un programme dans le bac à sable (ADR 0037) : un port, implémenté par l'appel au mode « programme » du service
 * runner. Le backend n'exécute jamais de code lui-même.
 */
public interface ProgramRunner {

    /**
     * @param language le langage du programme, qui choisit le bac à sable
     * @throws RunnerUnavailableException si le runner ne répond pas, est saturé ou n'a pas pu lancer le programme
     */
    ProgramRun run(CodeLanguage language, List<SourceFile> files);
}
