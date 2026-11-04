package com.recallio.runner.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.recallio.runner.sandbox.RunResult.Status;
import com.recallio.runner.sandbox.RunResult.TestOutcome;
import com.recallio.runner.sandbox.Submission.SourceFile;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Le bac à sable Go de bout en bout, avec Docker et l'image {@code sandbox/go} (ADR 0033) : les résultats d'une
 * exécution, et la même isolation que pour Java. Chaque test lance un conteneur, en une à deux secondes.
 */
class GoSandboxTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private static final String ADDITION = """
            package calcul

            // Addition renvoie la somme de a et b.
            func Addition(a, b int) int {
            	return a + b
            }
            """;

    @Test
    void shouldPassWhenEveryTestPasses() {
        RunResult result =
                run(ADDITION, test("if got := Addition(2, 3); got != 5 { t.Errorf(\"Addition = %d\", got) }"));

        assertThat(result.status()).isEqualTo(Status.PASSED);
        assertThat(result.tests()).containsExactly(new TestOutcome("TestAddition", "TestAddition", true, ""));
    }

    @Test
    void shouldReportWhatTheFailingTestWrote() {
        RunResult result = run(
                ADDITION.replace("a + b", "a - b"),
                test("if got := Addition(2, 3); got != 5 { t.Errorf(\"Addition(2, 3) = %d, attendu 5\", got) }"));

        assertThat(result.status()).isEqualTo(Status.FAILED);
        assertThat(result.tests().getFirst().message()).isEqualTo("calcul_test.go:6: Addition(2, 3) = -1, attendu 5");
    }

    @Test
    void shouldCountEachSubtestOnce() {
        RunResult result = run(ADDITION.replace("a + b", "a * b"), """
                package calcul

                import "testing"

                func TestAddition(t *testing.T) {
                	t.Run("zero", func(t *testing.T) {
                		if Addition(0, 0) != 0 {
                			t.Error("0 + 0")
                		}
                	})
                	t.Run("positifs", func(t *testing.T) {
                		if Addition(2, 3) != 5 {
                			t.Error("2 + 3")
                		}
                	})
                }
                """);

        assertThat(result.status()).isEqualTo(Status.FAILED);
        assertThat(result.tests())
                .extracting(TestOutcome::name, TestOutcome::passed)
                .containsExactly(tuple("TestAddition/zero", true), tuple("TestAddition/positifs", false));
    }

    @Test
    void shouldReportCompilationErrorsWithoutTheSandboxPaths() {
        RunResult result = run(ADDITION.replace("return a + b", "return a + c"), test("Addition(2, 3)"));

        assertThat(result.status()).isEqualTo(Status.COMPILATION_ERROR);
        assertThat(result.output()).startsWith("calcul.go:5:13: undefined: c");
    }

    @Test
    void shouldReportAPanicWithoutItsStack() {
        RunResult result =
                run(ADDITION.replace("return a + b", "var xs []int\n\treturn xs[a+b]"), test("Addition(2, 3)"));

        assertThat(result.status()).isEqualTo(Status.FAILED);
        assertThat(result.tests().getFirst().message())
                .startsWith("panic: runtime error: index out of range [5] with length 0")
                .doesNotContain("goroutine");
    }

    @Test
    void shouldStopCodeThatNeverEnds() {
        DockerSandbox sandbox = SandboxImage.sandbox(Duration.ofSeconds(8));

        RunResult result =
                sandbox.run(submission(ADDITION.replace("return a + b", "for {\n\t}"), test("Addition(2, 3)")));

        assertThat(result.status()).isEqualTo(Status.TIMEOUT);
    }

    @Test
    void shouldFailWhenTheProgramExitsBeforeTheReport() {
        RunResult result = run(
                "package calcul\n\nimport \"os\"\n\nfunc Addition(a, b int) int {\n\tos.Exit(0)\n\treturn 0\n}\n",
                test("Addition(2, 3)"));

        assertThat(result.status()).isEqualTo(Status.FAILED);
    }

    @Test
    void shouldNotReachTheNetworkNorWriteOutsideItsTemporaryFolders() {
        RunResult result = run(ADDITION, """
                package calcul

                import (
                	"net"
                	"os"
                	"testing"
                	"time"
                )

                func TestIsolation(t *testing.T) {
                	if _, err := net.DialTimeout("tcp", "1.1.1.1:443", 2*time.Second); err == nil {
                		t.Error("le réseau est joignable")
                	}
                	if err := os.WriteFile("/opt/recallio/run.sh", []byte("x"), 0o644); err == nil {
                		t.Error("l'image est inscriptible")
                	}
                	if err := os.WriteFile("/etc/evasion", []byte("x"), 0o644); err == nil {
                		t.Error("/etc est inscriptible")
                	}
                	if os.Getuid() != 65534 {
                		t.Errorf("utilisateur %d", os.Getuid())
                	}
                }
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
                Language.GO,
                List.of(new SourceFile("calcul.go", code)),
                List.of(new SourceFile("calcul_test.go", test)));
    }

    /** Un fichier de tests d'un seul test, {@code TestAddition}, avec ces instructions. */
    private static String test(String body) {
        return """
                package calcul

                import "testing"

                func TestAddition(t *testing.T) {
                	%s
                }
                """.formatted(body);
    }
}
