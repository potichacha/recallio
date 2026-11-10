package com.recallio.runner.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.runner.sandbox.RunResult.Status;
import com.recallio.runner.sandbox.RunResult.TestOutcome;
import com.recallio.runner.sandbox.Submission.SourceFile;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Le bac à sable C++ de bout en bout, avec Docker et l'image {@code sandbox/cpp} (ADR 0053) : les résultats d'une
 * exécution, et la même isolation que pour Java et Go. Chaque test lance un conteneur, en quelques secondes.
 */
class CppSandboxTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private static final String HEADER = "#pragma once\n\nint addition(int a, int b);\n";

    private static final String ADDITION = """
            #include "calcul.hpp"

            int addition(int a, int b) {
                return a + b;
            }
            """;

    @Test
    void shouldPassWhenEveryTestPasses() {
        RunResult result = run(ADDITION, test("CHECK(addition(2, 3) == 5);"));

        assertThat(result.status()).isEqualTo(Status.PASSED);
        assertThat(result.tests()).containsExactly(new TestOutcome("calcul_test.cpp", "addition", true, ""));
    }

    @Test
    void shouldReportTheFailingAssertionWithItsValues() {
        RunResult result = run(ADDITION.replace("a + b", "a - b"), test("CHECK(addition(2, 3) == 5);"));

        assertThat(result.status()).isEqualTo(Status.FAILED);
        assertThat(result.tests().getFirst().message())
                .startsWith("calcul_test.cpp:")
                .contains("CHECK( addition(2, 3) == 5 ) is NOT correct!")
                .contains("values: CHECK( -1 == 5 )");
    }

    @Test
    void shouldReportAnExceptionThrownByTheCode() {
        RunResult result = run(
                "#include \"calcul.hpp\"\n#include <stdexcept>\n\n"
                        + "int addition(int a, int b) {\n    throw std::out_of_range(\"hors limites\");\n}\n",
                test("CHECK(addition(2, 3) == 5);"));

        assertThat(result.status()).isEqualTo(Status.FAILED);
        assertThat(result.tests().getFirst().message()).contains("hors limites");
    }

    @Test
    void shouldReportCompilationErrorsWithoutTheSandboxPaths() {
        RunResult result = run(ADDITION.replace("return a + b", "return a + c"), test("addition(2, 3);"));

        assertThat(result.status()).isEqualTo(Status.COMPILATION_ERROR);
        assertThat(result.output())
                .contains("calcul.cpp:4:16: error: 'c' was not declared in this scope")
                .doesNotContain("/work");
    }

    @Test
    void shouldStopCodeThatNeverEnds() {
        DockerSandbox sandbox = SandboxImage.sandbox(Duration.ofSeconds(8));

        RunResult result = sandbox.run(submission(
                ADDITION.replace(
                        "return a + b;", "volatile int tours = 0;\n    while (true) {\n        ++tours;\n    }"),
                test("addition(2, 3);")));

        assertThat(result.status()).isEqualTo(Status.TIMEOUT);
    }

    @Test
    void shouldFailWhenTheProgramExitsBeforeTheReport() {
        RunResult result = run(
                "#include \"calcul.hpp\"\n#include <cstdlib>\n\nint addition(int a, int b) {\n    std::exit(0);\n}\n",
                test("addition(2, 3);"));

        assertThat(result.status()).isEqualTo(Status.FAILED);
        assertThat(result.output()).contains("exit, ou un plantage");
    }

    @Test
    void shouldNotReachTheNetworkNorWriteOutsideItsTemporaryFolders() {
        RunResult result = run(ADDITION, """
                #include "doctest.h"
                #include <arpa/inet.h>
                #include <fstream>
                #include <sys/socket.h>
                #include <unistd.h>

                TEST_CASE("isolation") {
                    int sock = socket(AF_INET, SOCK_STREAM, 0);
                    sockaddr_in to{};
                    to.sin_family = AF_INET;
                    to.sin_port = htons(443);
                    inet_pton(AF_INET, "1.1.1.1", &to.sin_addr);
                    CHECK_MESSAGE(connect(sock, reinterpret_cast<sockaddr*>(&to), sizeof to) != 0, "réseau joignable");
                    CHECK_MESSAGE(!std::ofstream("/opt/recallio/run.sh", std::ios::app), "image inscriptible");
                    CHECK_MESSAGE(!std::ofstream("/etc/evasion"), "/etc inscriptible");
                    CHECK(getuid() == 65534);
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
                Language.CPP,
                List.of(new SourceFile("calcul.hpp", HEADER), new SourceFile("calcul.cpp", code)),
                List.of(new SourceFile("calcul_test.cpp", test)));
    }

    /** Un fichier de tests d'un seul test, {@code addition}, avec ces instructions. */
    private static String test(String body) {
        return """
                #include "doctest.h"
                #include "calcul.hpp"

                TEST_CASE("addition") {
                    %s
                }
                """.formatted(body);
    }
}
