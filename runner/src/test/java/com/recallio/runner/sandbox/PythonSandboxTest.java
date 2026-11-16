package com.recallio.runner.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.runner.sandbox.RunResult.Status;
import com.recallio.runner.sandbox.RunResult.TestOutcome;
import com.recallio.runner.sandbox.Submission.SourceFile;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Le bac à sable Python de bout en bout, avec Docker et l'image {@code sandbox/python} (ADR 0062) : les résultats d'une
 * exécution par pytest, et la même isolation que pour les autres langages. Chaque test lance un conteneur, en une
 * seconde environ.
 */
class PythonSandboxTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private static final String CALCUL = """
            def addition(a, b):
                return a + b
            """;

    @Test
    void shouldPassWhenEveryTestPasses() {
        RunResult result = run(CALCUL, test("assert addition(2, 3) == 5"));

        assertThat(result.status()).isEqualTo(Status.PASSED);
        assertThat(result.tests()).containsExactly(new TestOutcome("calcul_test", "test_addition", true, ""));
    }

    @Test
    void shouldReportTheFailingAssertionWithItsValues() {
        RunResult result = run(CALCUL.replace("a + b", "a - b"), test("assert addition(2, 3) == 5"));

        assertThat(result.status()).isEqualTo(Status.FAILED);
        assertThat(result.tests().getFirst().message())
                .startsWith("calcul_test.py:5: in test_addition")
                .contains("E   assert -1 == 5")
                .doesNotContain("/work");
    }

    @Test
    void shouldReportAnExceptionRaisedByTheCode() {
        RunResult result = run(
                "def addition(a, b):\n    raise ValueError(\"hors limites\")\n", test("assert addition(2, 3) == 5"));

        assertThat(result.status()).isEqualTo(Status.FAILED);
        assertThat(result.tests().getFirst().message()).contains("ValueError: hors limites");
    }

    @Test
    void shouldReportASyntaxErrorLikeACompilationError() {
        RunResult result = run(CALCUL.replace("(a, b):", "(a, b)"), test("addition(2, 3)"));

        assertThat(result.status()).isEqualTo(Status.COMPILATION_ERROR);
        assertThat(result.output())
                .contains("File \"calcul.py\", line 1")
                .contains("SyntaxError: expected ':'")
                .doesNotContain("/work");
    }

    @Test
    void shouldReportCodeThatCannotBeImportedLikeACompilationError() {
        RunResult result = run(CALCUL.replace("def addition", "def somme"), test("assert addition(2, 3) == 5"));

        assertThat(result.status()).isEqualTo(Status.COMPILATION_ERROR);
        assertThat(result.output())
                .contains("ImportError: cannot import name 'addition' from 'calcul'")
                .doesNotContain("/usr/local/lib")
                .doesNotContain("/work");
    }

    @Test
    void shouldStopCodeThatNeverEnds() {
        DockerSandbox sandbox = SandboxImage.sandbox(Duration.ofSeconds(8));

        RunResult result =
                sandbox.run(submission("def addition(a, b):\n    while True:\n        pass\n", test("addition(2, 3)")));

        assertThat(result.status()).isEqualTo(Status.TIMEOUT);
    }

    @Test
    void shouldFailWhenTheProcessExitsBeforeTheReport() {
        RunResult result = run("import os\n\n\ndef addition(a, b):\n    os._exit(0)\n", test("addition(2, 3)"));

        assertThat(result.status()).isEqualTo(Status.FAILED);
        assertThat(result.output()).contains("os._exit");
    }

    @Test
    void shouldNotReachTheNetworkNorWriteOutsideItsTemporaryFolders() {
        RunResult result = run(CALCUL, """
                import os
                import socket

                import pytest


                def test_isolation():
                    with pytest.raises(OSError):
                        socket.create_connection(("1.1.1.1", 443), timeout=2)
                    with pytest.raises(OSError):
                        open("/opt/recallio/run.sh", "a")
                    with pytest.raises(OSError):
                        open("/etc/evasion", "w")
                    assert os.getuid() == 65534
                """);

        assertThat(result.tests())
                .singleElement()
                .satisfies(test -> assertThat(test.message()).isEmpty());
        assertThat(result.status()).isEqualTo(Status.PASSED);
    }

    private static RunResult run(String code, String test) {
        return SandboxImage.sandbox(TIMEOUT).run(submission(code, test));
    }

    private static Submission submission(String code, String test) {
        return new Submission(
                Language.PYTHON,
                List.of(new SourceFile("calcul.py", code)),
                List.of(new SourceFile("calcul_test.py", test)));
    }

    /** Un fichier de tests d'un seul test, {@code test_addition}, avec cette instruction. */
    private static String test(String body) {
        return """
                from calcul import addition


                def test_addition():
                    %s
                """.formatted(body);
    }
}
