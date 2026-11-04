package com.recallio.review;

import com.recallio.content.CodeLanguage;
import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.review.domain.exercise.RunnerUnavailableException;
import com.recallio.review.domain.playground.ProgramRun;
import com.recallio.review.domain.playground.ProgramRunner;
import com.recallio.review.domain.playground.ProgramStatus;
import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/**
 * Le mode « programme » du runner dans les tests du backend (ADR 0037) : comme {@link FakeCodeRunner}, il remplace
 * l'appel HTTP, et chaque test choisit le résultat qu'il renvoie.
 */
@Mock
@ApplicationScoped
public class FakeProgramRunner implements ProgramRunner {

    public static final ProgramRun HELLO = new ProgramRun(ProgramStatus.COMPLETED, 0, "Bonjour\n", "", false, "");

    private volatile ProgramRun next = HELLO;
    private volatile boolean unavailable;
    private volatile CodeLanguage lastLanguage = CodeLanguage.GO;
    private volatile List<SourceFile> lastFiles = List.of();

    @Override
    public ProgramRun run(CodeLanguage language, List<SourceFile> files) {
        lastLanguage = language;
        lastFiles = List.copyOf(files);
        if (unavailable) {
            throw new RunnerUnavailableException();
        }
        return next;
    }

    public void willReturn(ProgramRun run) {
        next = run;
        unavailable = false;
    }

    public void willBeUnavailable() {
        unavailable = true;
    }

    public CodeLanguage lastLanguage() {
        return lastLanguage;
    }

    public List<SourceFile> lastFiles() {
        return lastFiles;
    }
}
