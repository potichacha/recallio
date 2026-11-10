package com.recallio.runner.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.runner.sandbox.ProgramResult.Status;
import com.recallio.runner.sandbox.Submission.SourceFile;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Les programmes du Tour de bout en bout, avec Docker et les images {@code sandbox/java} et {@code sandbox/go}
 * (ADR 0037) : ce qu'ils écrivent, leur code de sortie, leurs erreurs, et les limites qui les bornent. Chaque test lance
 * un conteneur, en une à trois secondes.
 */
class ProgramSandboxTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Test
    void shouldRunACompactJavaSourceFile() {
        ProgramResult result = java("""
                void main() {
                    IO.println("Bonjour, Java !");
                }
                """);

        assertThat(result).isEqualTo(new ProgramResult(Status.COMPLETED, 0, "Bonjour, Java !\n", "", false, ""));
    }

    @Test
    void shouldRunAJavaProgramOfSeveralFiles() {
        ProgramResult result = run(
                Language.JAVA,
                new SourceFile("Main.java", "void main() {\n    IO.println(new Point(1, 2));\n}\n"),
                new SourceFile("Point.java", "record Point(int x, int y) {}\n"));

        assertThat(result.status()).isEqualTo(Status.COMPLETED);
        assertThat(result.stdout()).isEqualTo("Point[x=1, y=2]\n");
    }

    @Test
    void shouldReportJavaCompilationErrorsByFileName() {
        ProgramResult result = java("void main() {\n    IO.println(\"Bonjour\")\n}\n");

        assertThat(result.status()).isEqualTo(Status.COMPILATION_ERROR);
        assertThat(result.message()).startsWith("Main.java:2: error: ';' expected");
    }

    @Test
    void shouldReturnTheExitCodeAndTheErrorOutputOfAFailingJavaProgram() {
        ProgramResult result = java("""
                void main() {
                    IO.println("avant");
                    throw new IllegalStateException("panne");
                }
                """);

        assertThat(result.status()).isEqualTo(Status.COMPLETED);
        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).isEqualTo("avant\n");
        assertThat(result.stderr()).contains("java.lang.IllegalStateException: panne");
    }

    @Test
    void shouldStopAProgramThatNeverEndsAndKeepWhatItWrote() {
        DockerSandbox sandbox = SandboxImage.sandbox(TIMEOUT, Duration.ofSeconds(2));

        ProgramResult result = sandbox.run(program(
                Language.JAVA,
                new SourceFile("Main.java", "void main() {\n    IO.println(\"début\");\n    while (true) {}\n}\n")));

        assertThat(result.status()).isEqualTo(Status.TIMEOUT);
        assertThat(result.stdout()).isEqualTo("début\n");
        assertThat(result.message()).isEqualTo("Le programme a dépassé 2 secondes.");
    }

    @Test
    void shouldTruncateAnOutputThatIsTooLong() {
        ProgramResult result = java("""
                void main() {
                    var line = "x".repeat(99);
                    for (int i = 0; i < 2_000; i++) {
                        IO.println(line);
                    }
                }
                """);

        assertThat(result.truncated()).isTrue();
        assertThat(result.stdout()).hasSize(ProgramResult.MAX_OUTPUT_BYTES);
    }

    @Test
    void shouldGiveNoStandardInputToTheProgram() {
        ProgramResult result = java("""
                void main() throws Exception {
                    IO.println(System.in.read());
                }
                """);

        assertThat(result.stdout()).isEqualTo("-1\n");
    }

    @Test
    void shouldRunAGoProgram() {
        ProgramResult result = go("""
                package main

                import "fmt"

                func main() {
                	fmt.Println("Bonjour, Go !")
                }
                """);

        assertThat(result).isEqualTo(new ProgramResult(Status.COMPLETED, 0, "Bonjour, Go !\n", "", false, ""));
    }

    @Test
    void shouldCompileAStandardPackageThatIsNotPrecompiled() {
        ProgramResult result = go("""
                package main

                import (
                	"fmt"
                	"math/cmplx"
                )

                func main() {
                	fmt.Println(cmplx.Sqrt(-1))
                }
                """);

        assertThat(result.stdout()).isEqualTo("(0+1i)\n");
    }

    @Test
    void shouldReportGoCompilationErrorsByFileName() {
        ProgramResult result = go("package main\n\nfunc main() {\n\tx := 1\n}\n");

        assertThat(result.status()).isEqualTo(Status.COMPILATION_ERROR);
        assertThat(result.message()).startsWith("main.go:4:2: declared and not used: x");
    }

    @Test
    void shouldReturnThePanicOfAGoProgram() {
        ProgramResult result = go("""
                package main

                func main() {
                	var xs []int
                	_ = xs[3]
                }
                """);

        assertThat(result.status()).isEqualTo(Status.COMPLETED);
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stderr()).startsWith("panic: runtime error: index out of range [3] with length 0");
    }

    @Test
    void shouldNotReachTheNetworkFromAProgram() {
        ProgramResult result = go("""
                package main

                import (
                	"fmt"
                	"net/http"
                	"time"
                )

                func main() {
                	client := http.Client{Timeout: 2 * time.Second}
                	_, err := client.Get("https://example.com")
                	fmt.Println(err != nil)
                }
                """);

        assertThat(result.stdout()).isEqualTo("true\n");
    }

    @Test
    void shouldRunACppProgramOfSeveralFiles() {
        ProgramResult result = run(
                Language.CPP,
                new SourceFile(
                        "main.cpp",
                        "#include <print>\n#include \"panier.hpp\"\n\n"
                                + "int main() {\n    std::println(\"Total : {}\", total({12, 30}));\n}\n"),
                new SourceFile(
                        "panier.hpp", "#pragma once\n#include <vector>\n\nint total(const std::vector<int>& prix);\n"),
                new SourceFile(
                        "panier.cpp",
                        "#include \"panier.hpp\"\n#include <numeric>\n\n"
                                + "int total(const std::vector<int>& prix) {\n"
                                + "    return std::accumulate(prix.begin(), prix.end(), 0);\n}\n"));

        assertThat(result).isEqualTo(new ProgramResult(Status.COMPLETED, 0, "Total : 42\n", "", false, ""));
    }

    @Test
    void shouldReportCppCompilationErrorsByFileName() {
        ProgramResult result = cpp("int main() {\n    int x = 1\n}\n");

        assertThat(result.status()).isEqualTo(Status.COMPILATION_ERROR);
        // GCC situe l'erreur sur l'accolade qui suit l'instruction inachevée, sans le ./ de la compilation.
        assertThat(result.message())
                .contains("main.cpp:3:1: error: expected ',' or ';' before '}' token")
                .doesNotContain("./main.cpp");
    }

    @Test
    void shouldReturnTheUncaughtExceptionOfACppProgram() {
        ProgramResult result = cpp("""
                #include <print>
                #include <vector>

                int main() {
                    std::vector<int> xs;
                    std::println("avant");
                    return xs.at(3);
                }
                """);

        assertThat(result.status()).isEqualTo(Status.COMPLETED);
        assertThat(result.exitCode()).isEqualTo(134);
        // La sortie part ligne par ligne, comme dans un terminal : le plantage ne la perd pas.
        assertThat(result.stdout()).isEqualTo("avant\n");
        assertThat(result.stderr())
                .startsWith("terminate called after throwing an instance of 'std::out_of_range'")
                .endsWith("\nAborted\n")
                .doesNotContain("timeout");
    }

    @Test
    void shouldNameTheSignalThatStoppedACppProgramLikeATerminal() {
        ProgramResult result = cpp("""
                #include <print>

                int main() {
                    std::println("avant");
                    int* p = nullptr;
                    return *p;
                }
                """);

        assertThat(result.exitCode()).isEqualTo(139);
        assertThat(result.stdout()).isEqualTo("avant\n");
        assertThat(result.stderr()).isEqualTo("Segmentation fault\n");
    }

    @Test
    void shouldStopACppProgramThatNeverEndsAndKeepWhatItWrote() {
        DockerSandbox sandbox = SandboxImage.sandbox(TIMEOUT, Duration.ofSeconds(2));

        ProgramResult result = sandbox.run(program(
                Language.CPP,
                new SourceFile(
                        "main.cpp",
                        "#include <print>\n\nint main() {\n    std::println(\"début\");\n"
                                + "    volatile bool encore = true;\n    while (encore) {}\n}\n")));

        assertThat(result.status()).isEqualTo(Status.TIMEOUT);
        assertThat(result.stdout()).isEqualTo("début\n");
        assertThat(result.stderr()).isEmpty();
    }

    private static ProgramResult cpp(String main) {
        return run(Language.CPP, new SourceFile("main.cpp", main));
    }

    private static ProgramResult java(String main) {
        return run(Language.JAVA, new SourceFile(Program.JAVA_ENTRY_POINT, main));
    }

    private static ProgramResult go(String main) {
        return run(Language.GO, new SourceFile("main.go", main));
    }

    private static ProgramResult run(Language language, SourceFile... files) {
        return SandboxImage.sandbox(TIMEOUT).run(program(language, files));
    }

    private static Program program(Language language, SourceFile... files) {
        return new Program(language, List.of(files));
    }
}
