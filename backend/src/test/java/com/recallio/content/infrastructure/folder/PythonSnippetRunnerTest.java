package com.recallio.content.infrastructure.folder;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.infrastructure.folder.PythonSnippetRunner.Execution;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** L'exécuteur des « qu'affiche ce code ? » de Python (ADR 0062) ; ignoré sur une machine sans Python 3.14. */
class PythonSnippetRunnerTest {

    private static PythonSnippetRunner runner;

    @BeforeAll
    static void findPython() {
        var found = PythonSnippetRunner.find();
        Assumptions.assumeTrue(found.isPresent(), "Python 3.14 n'est pas installé");
        runner = found.orElseThrow();
    }

    @Test
    void shouldRunAProgramAndReturnWhatItPrinted() {
        Execution execution = runner.run("""
                print(7 // 2, 7 % 2, "é")
                """);

        assertThat(execution).isEqualTo(new Execution("3 1 é\n", null, null));
    }

    @Test
    void shouldReportASyntaxErrorLikeACompilationError() {
        Execution execution = runner.run("def total(prix):\nreturn sum(prix)\n");

        assertThat(execution.compileError()).contains("IndentationError: expected an indented block");
        assertThat(execution.output()).isEmpty();
    }

    @Test
    void shouldGiveTheTypeOfAnUncaughtExceptionAfterWhatWasPrintedBefore() {
        Execution execution = runner.run("""
                prix = {"stylo": 2}
                print("avant")
                print(prix["gomme"])
                """);

        assertThat(execution.output()).isEqualTo("avant\n");
        assertThat(execution.stop()).isEqualTo("KeyError");
    }

    @Test
    void shouldGiveTheExitCodeOfAProgramThatExits() {
        Execution execution = runner.run("import sys\n\nsys.exit(3)\n");

        assertThat(execution.stop()).isEqualTo("code de sortie 3");
    }
}
