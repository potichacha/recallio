package com.recallio.runner.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.runner.sandbox.RunResult.Status;
import com.recallio.runner.sandbox.RunResult.TestOutcome;
import com.recallio.runner.sandbox.Submission.SourceFile;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Le bac à sable de bout en bout, avec Docker et l'image {@code sandbox/java} : les résultats d'une exécution, et
 * l'isolation qu'il promet (ADR 0030). Chaque test lance un conteneur, en deux secondes environ. Go a les siens
 * ({@link GoSandboxTest}).
 */
class DockerSandboxTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final Duration PROGRAM = Duration.ofSeconds(5);

    private static final String ADDITION = """
            public final class Addition {
                public static int de(int a, int b) {
                    return a + b;
                }
            }
            """;

    @Test
    void shouldPassWhenEveryTestPasses() {
        RunResult result = run(ADDITION, test("assertEquals(5, Addition.de(2, 3));"));

        assertThat(result.status()).isEqualTo(Status.PASSED);
        assertThat(result.tests()).containsExactly(new TestOutcome("AdditionTest", "verifie", true, ""));
    }

    @Test
    void shouldReportTheFailedAssertion() {
        RunResult result = run(ADDITION.replace("a + b", "a - b"), test("assertEquals(5, Addition.de(2, 3));"));

        assertThat(result.status()).isEqualTo(Status.FAILED);
        assertThat(result.tests().getFirst().message()).isEqualTo("expected: <5> but was: <-1>");
    }

    @Test
    void shouldReportCompilationErrorsWithoutTheSandboxPaths() {
        RunResult result =
                run(ADDITION.replace("return a + b;", "return a + b"), test("assertEquals(5, Addition.de(2, 3));"));

        assertThat(result.status()).isEqualTo(Status.COMPILATION_ERROR);
        assertThat(result.output()).startsWith("src/Addition.java:3: error: ';' expected");
    }

    @Test
    void shouldStopCodeThatNeverEnds() {
        DockerSandbox sandbox = SandboxImage.sandbox(Duration.ofSeconds(8));

        RunResult result = sandbox.run(
                submission(ADDITION.replace("return a + b;", "while (true) {}"), test("Addition.de(2, 3);")));

        assertThat(result.status()).isEqualTo(Status.TIMEOUT);
    }

    @Test
    void shouldFailWhenTheProgramExitsBeforeTheReport() {
        RunResult result =
                run(ADDITION.replace("return a + b;", "System.exit(0); return 0;"), test("Addition.de(2, 3);"));

        assertThat(result.status()).isEqualTo(Status.FAILED);
        assertThat(result.output()).contains("System.exit");
    }

    @Test
    void shouldNotReachTheNetwork() {
        RunResult result = run(ADDITION, test("""
                        assertThrows(java.io.IOException.class, () -> {
                            try (var socket = new java.net.Socket()) {
                                socket.connect(new java.net.InetSocketAddress("1.1.1.1", 443), 2_000);
                            }
                        });
                        """));

        assertThat(result.status()).isEqualTo(Status.PASSED);
    }

    @Test
    void shouldNotWriteOutsideItsTemporaryFolders() {
        RunResult result = run(ADDITION, test("""
                        assertThrows(java.io.IOException.class,
                                () -> java.nio.file.Files.writeString(java.nio.file.Path.of("/opt/recallio/run.sh"), "x"));
                        assertThrows(java.io.IOException.class,
                                () -> java.nio.file.Files.writeString(java.nio.file.Path.of("/etc/evasion"), "x"));
                        assertEquals("65534", new String(new ProcessBuilder("id", "-u").start().getInputStream().readAllBytes()).strip());
                        """));

        assertThat(result.tests())
                .singleElement()
                .satisfies(test -> assertThat(test.message()).isEmpty());
        assertThat(result.status()).isEqualTo(Status.PASSED);
    }

    @Test
    void shouldContainCodeThatExhaustsItsMemory() {
        RunResult result = run(ADDITION, test("""
                        var hoard = new java.util.ArrayList<long[]>();
                        while (true) { hoard.add(new long[1_000_000]); }
                        """));

        assertThat(result.status()).isEqualTo(Status.FAILED);
    }

    @Test
    void shouldInterpretTheOutputOfTheContainer() {
        assertThat(DockerSandbox.interpret(Language.JAVA, "@@recallio:compilation-error\nA.java:1: error\n"))
                .isEqualTo(RunResult.failure(Status.COMPILATION_ERROR, "A.java:1: error"));
        assertThat(DockerSandbox.interpret(Language.JAVA, "@@recallio:report\n").status())
                .isEqualTo(Status.FAILED);
        assertThat(DockerSandbox.interpret(Language.JAVA, "@@recallio:invalid\n")
                        .status())
                .isEqualTo(Status.ERROR);
        assertThat(DockerSandbox.interpret(Language.JAVA, "").status()).isEqualTo(Status.ERROR);
        assertThat(DockerSandbox.interpret(Language.JAVA, "@@recallio:report\n<pas du xml")
                        .status())
                .isEqualTo(Status.ERROR);
    }

    @Test
    void shouldInterpretTheOutputOfAProgram() {
        String output = "@@recallio:program 3 0\n%s\n%s\n".formatted(base64("Bonjour\n"), base64("oups"));

        assertThat(DockerSandbox.interpretProgram(output, PROGRAM))
                .isEqualTo(new ProgramResult(ProgramResult.Status.COMPLETED, 3, "Bonjour\n", "oups", false, ""));
        assertThat(DockerSandbox.interpretProgram("@@recallio:program 0 1\n\n\n", PROGRAM)
                        .truncated())
                .isTrue();
        assertThat(DockerSandbox.interpretProgram("@@recallio:compilation-error\nMain.java:1: error\n", PROGRAM))
                .isEqualTo(ProgramResult.failure(ProgramResult.Status.COMPILATION_ERROR, "Main.java:1: error"));
    }

    @Test
    void shouldReportATimeoutWithWhatTheProgramWroteUntilThen() {
        String output = "@@recallio:program 124 0\n%s\n\n".formatted(base64("début\n"));

        assertThat(DockerSandbox.interpretProgram(output, PROGRAM))
                .isEqualTo(new ProgramResult(
                        ProgramResult.Status.TIMEOUT, 124, "début\n", "", false, "Le programme a dépassé 5 secondes."));
    }

    @Test
    void shouldNotTrustAnUnexpectedProgramOutput() {
        assertThat(DockerSandbox.interpretProgram("", PROGRAM).status()).isEqualTo(ProgramResult.Status.ERROR);
        assertThat(DockerSandbox.interpretProgram("@@recallio:invalid\n", PROGRAM)
                        .status())
                .isEqualTo(ProgramResult.Status.ERROR);
        assertThat(DockerSandbox.interpretProgram("@@recallio:program zéro 0\n\n\n", PROGRAM)
                        .status())
                .isEqualTo(ProgramResult.Status.ERROR);
        assertThat(DockerSandbox.interpretProgram("@@recallio:program 0 0\npas du base64 !\n\n", PROGRAM)
                        .status())
                .isEqualTo(ProgramResult.Status.ERROR);
        assertThat(DockerSandbox.interpretProgram("@@recallio:program 0 0\n", PROGRAM)
                        .status())
                .isEqualTo(ProgramResult.Status.ERROR);
    }

    @Test
    void shouldRefuseAProgramTimeoutUnderOneSecond() {
        assertThatThrownBy(() -> new DockerSandbox(Map.of(), TIMEOUT, Duration.ofMillis(500), Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAddTheIsolatingRuntimeWhenOneIsConfigured() {
        var sandbox = new DockerSandbox(
                Map.of(Language.JAVA, "image"), TIMEOUT, SandboxImage.PROGRAM_TIMEOUT, Optional.of("runsc"));

        assertThat(sandbox.command("run", "image", List.of("tests")))
                .containsSequence("--network", "none")
                .containsSequence("--cap-drop", "ALL")
                .containsSequence("--user", "65534:65534")
                .containsSequence("--runtime", "runsc", "image", "tests")
                .contains("--read-only");
    }

    /** Une sortie du programme, encodée comme le fait l'image. */
    private static String base64(String output) {
        return Base64.getEncoder().encodeToString(output.getBytes(StandardCharsets.UTF_8));
    }

    private static RunResult run(String code, String test) {
        return SandboxImage.sandbox(TIMEOUT).run(submission(code, test));
    }

    private static Submission submission(String code, String test) {
        return new Submission(
                Language.JAVA,
                List.of(new SourceFile("Addition.java", code)),
                List.of(new SourceFile("AdditionTest.java", test)));
    }

    /** Une classe de test JUnit d'un seul test, {@code verifie}, avec ces instructions. */
    private static String test(String body) {
        return """
                import static org.junit.jupiter.api.Assertions.*;

                import org.junit.jupiter.api.Test;

                class AdditionTest {
                    @Test
                    void verifie() throws Exception {
                %s
                    }
                }
                """.formatted(body.indent(8));
    }
}
