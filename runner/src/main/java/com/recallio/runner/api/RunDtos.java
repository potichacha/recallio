package com.recallio.runner.api;

import com.recallio.runner.sandbox.Language;
import com.recallio.runner.sandbox.Program;
import com.recallio.runner.sandbox.ProgramResult;
import com.recallio.runner.sandbox.RunResult;
import com.recallio.runner.sandbox.RunResult.Status;
import com.recallio.runner.sandbox.Submission;
import com.recallio.runner.sandbox.Submission.SourceFile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Les messages de l'API du runner (ADR 0030, 0037). */
final class RunDtos {

    private RunDtos() {}

    /**
     * Une exécution demandée.
     *
     * @param language le langage du code et des tests (ADR 0033), qui choisit l'image du bac à sable
     * @param code les fichiers du joueur (ou d'une solution), placés dans {@code src/}
     * @param tests les tests qui les jugent, placés dans {@code tests/}
     */
    record RunRequest(
            @NotNull Language language,

            @NotNull @Size(min = 1, max = Submission.MAX_FILES_PER_PART)
            List<@NotNull @Valid SourceFileDto> code,

            @NotNull @Size(min = 1, max = Submission.MAX_FILES_PER_PART)
            List<@NotNull @Valid SourceFileDto> tests) {

        Submission toSubmission() {
            return new Submission(
                    language,
                    code.stream().map(SourceFileDto::toSourceFile).toList(),
                    tests.stream().map(SourceFileDto::toSourceFile).toList());
        }
    }

    /**
     * Un programme à lancer (ADR 0037).
     *
     * @param files ses fichiers, placés dans {@code src/} : en Java, un {@code Main.java} à la racine en fait partie
     */
    record ProgramRequest(
            @NotNull Language language,

            @NotNull @Size(min = 1, max = Submission.MAX_FILES_PER_PART)
            List<@NotNull @Valid SourceFileDto> files) {

        Program toProgram() {
            return new Program(
                    language, files.stream().map(SourceFileDto::toSourceFile).toList());
        }
    }

    record SourceFileDto(
            @NotNull @Pattern(regexp = SourceFile.PATH_PATTERN)
            String path,

            @NotNull @Size(max = SourceFile.MAX_LENGTH) String content) {

        SourceFile toSourceFile() {
            return new SourceFile(path, content);
        }
    }

    record RunResultDto(Status status, List<TestOutcomeDto> tests, String output) {

        static RunResultDto from(RunResult result) {
            return new RunResultDto(
                    result.status(),
                    result.tests().stream()
                            .map(test ->
                                    new TestOutcomeDto(test.className(), test.name(), test.passed(), test.message()))
                            .toList(),
                    result.output());
        }
    }

    record TestOutcomeDto(String className, String name, boolean passed, String message) {}

    record ProgramResultDto(
            ProgramResult.Status status,
            int exitCode,
            String stdout,
            String stderr,
            boolean truncated,
            String message) {

        static ProgramResultDto from(ProgramResult result) {
            return new ProgramResultDto(
                    result.status(),
                    result.exitCode(),
                    result.stdout(),
                    result.stderr(),
                    result.truncated(),
                    result.message());
        }
    }
}
