package com.recallio.content.infrastructure.folder;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.infrastructure.folder.CppSnippetRunner.Execution;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** L'exécuteur des « qu'affiche ce code ? » de C++ (ADR 0053) ; ignoré sur une machine sans GCC 14. */
class CppSnippetRunnerTest {

    private static CppSnippetRunner runner;

    @BeforeAll
    static void findGcc() {
        var found = CppSnippetRunner.find();
        Assumptions.assumeTrue(found.isPresent(), "g++ 14 n'est pas installé");
        runner = found.orElseThrow();
    }

    @Test
    void shouldRunAProgramInCpp23AndReturnWhatItPrinted() {
        Execution execution = runner.run("""
                #include <print>

                int main() {
                    std::println("{} {}", 7 / 2, 7 % 2);
                }
                """);

        assertThat(execution).isEqualTo(new Execution("3 1\n", null, null));
    }

    @Test
    void shouldReportACompilationError() {
        Execution execution = runner.run("int main() {\n    int x = 1\n}\n");

        // GCC cite le code entre guillemets typographiques dans une locale UTF-8, comme celle de la CI, et entre
        // apostrophes sinon : seul compte qu'il y ait une erreur, à sa place.
        assertThat(execution.compileError()).contains("main.cpp:3:1: error: expected");
    }

    @Test
    void shouldGiveTheTypeOfAnUncaughtExceptionAfterWhatWasPrintedBefore() {
        // Sans std::endl : la sortie part ligne par ligne, comme dans un terminal, et ne se perd pas dans le plantage.
        Execution execution = runner.run("""
                #include <iostream>
                #include <print>
                #include <vector>

                int main() {
                    std::vector<int> xs;
                    std::cout << "avant\\n";
                    std::println("encore");
                    std::cout << xs.at(3) << '\\n';
                }
                """);

        assertThat(execution.output()).isEqualTo("avant\nencore\n");
        assertThat(execution.stop()).isEqualTo("std::out_of_range");
    }

    @Test
    void shouldGiveTheExitCodeOfAProgramThatFails() {
        Execution execution = runner.run("int main() {\n    return 3;\n}\n");

        assertThat(execution.stop()).isEqualTo("code de sortie 3");
    }
}
