package com.recallio.review.application;

import com.recallio.content.CodeLanguage;
import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.content.TourPage;
import com.recallio.review.domain.exercise.RunnerUnavailableException;
import com.recallio.review.domain.playground.InvalidProgramException;
import com.recallio.review.domain.playground.ProgramRun;
import com.recallio.review.domain.playground.ProgramRunInProgressException;
import com.recallio.review.domain.playground.ProgramRunner;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Objects;

/**
 * Cas d'usage : lancer un programme du Tour (ADR 0037), l'exemple d'une page ou ce qu'en a fait l'utilisateur. Rien
 * n'est enregistré, et une seule exécution à la fois par utilisateur ({@link RunLock}).
 */
@ApplicationScoped
public class PlaygroundRunService {

    private final ProgramRunner runner;
    private final RunLock lock;

    public PlaygroundRunService(ProgramRunner runner, RunLock lock) {
        this.runner = runner;
        this.lock = lock;
    }

    /**
     * @throws InvalidProgramException si les fichiers ne forment pas un programme du Tour
     * @throws ProgramRunInProgressException si l'utilisateur a déjà une exécution en cours
     * @throws RunnerUnavailableException si le runner ne répond pas
     */
    public ProgramRun run(UserRef user, List<SourceFile> files) {
        CodeLanguage language;
        try {
            language = TourPage.programLanguage(files);
        } catch (IllegalArgumentException e) {
            throw new InvalidProgramException(Objects.requireNonNullElse(e.getMessage(), "Programme invalide"));
        }
        return lock.alone(user.id(), () -> runner.run(language, files), ProgramRunInProgressException::new);
    }
}
