package com.recallio.runner.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.runner.sandbox.RunResult.Status;
import com.recallio.runner.sandbox.Submission.SourceFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Le contrôle des exercices de code du contenu (ADR 0029, 0030), dans le bac à sable où ils seront joués : la solution
 * passe tous ses tests, et le code de départ compile mais en échoue au moins un. Un exercice impossible ou déjà résolu
 * se voit avant le merge. Un nouvel exercice est vérifié sans modifier ce test.
 */
class ExercisesTest {

    private static final Path CONTENT = Path.of("..", "content");
    private static final String DESCRIPTION = "exercise.yaml";
    private static final DockerSandbox SANDBOX = SandboxImage.sandbox(Duration.ofSeconds(60));

    static Stream<Path> exercises() throws IOException {
        try (Stream<Path> files = Files.walk(CONTENT)) {
            List<Path> found = files.filter(
                            file -> file.getFileName().toString().equals(DESCRIPTION))
                    .filter(file -> file.toString().replace('\\', '/').contains("/exercices/"))
                    .map(Path::getParent)
                    .sorted()
                    .toList();
            assertThat(found).as("exercices de code du contenu").isNotEmpty();
            return found.stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("exercises")
    void shouldBeSolvedByItsSolution(Path exercise) {
        RunResult result = SANDBOX.run(submission(exercise, "solution"));

        assertThat(result.status()).as("solution de %s : %s", exercise, result).isEqualTo(Status.PASSED);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("exercises")
    void shouldStartFromCodeThatCompilesButFails(Path exercise) {
        RunResult result = SANDBOX.run(submission(exercise, "starter"));

        assertThat(result.status())
                .as("code de départ de %s : %s", exercise, result)
                .isEqualTo(Status.FAILED);
        assertThat(result.tests()).as("tests de %s", exercise).isNotEmpty();
    }

    /** Une partie de l'exercice et ses tests, dans le langage de ses fichiers (ADR 0033). */
    private static Submission submission(Path exercise, String part) {
        List<SourceFile> tests = files(exercise, "tests");
        Language language = tests.getFirst().path().endsWith(".go") ? Language.GO : Language.JAVA;
        return new Submission(language, files(exercise, part), tests);
    }

    /** Les fichiers d'une partie de l'exercice, par chemin relatif à elle. */
    private static List<SourceFile> files(Path exercise, String part) {
        Path root = exercise.resolve(part);
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                    .sorted()
                    .map(file -> new SourceFile(root.relativize(file).toString().replace('\\', '/'), read(file)))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
