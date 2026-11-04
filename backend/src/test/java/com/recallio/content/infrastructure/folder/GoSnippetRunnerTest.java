package com.recallio.content.infrastructure.folder;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.infrastructure.folder.GoSnippetRunner.Execution;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** L'exécuteur des « qu'affiche ce code ? » de Go (ADR 0033) ; ignoré sur une machine sans {@code go}. */
class GoSnippetRunnerTest {

    private static GoSnippetRunner runner;

    @BeforeAll
    static void findGo() {
        var found = GoSnippetRunner.find();
        Assumptions.assumeTrue(found.isPresent(), "go n'est pas installé");
        runner = found.orElseThrow();
    }

    @Test
    void shouldRunAProgramAndReturnWhatItPrinted() {
        Execution execution = runner.run("""
                package main

                import "fmt"

                func main() {
                	fmt.Println(7/2, 7%2)
                }
                """);

        assertThat(execution).isEqualTo(new Execution("3 1\n", null, null));
    }

    @Test
    void shouldReportACompilationError() {
        // Go refuse une variable déclarée et inutilisée.
        Execution execution = runner.run("package main\n\nfunc main() {\n\tx := 1\n}\n");

        assertThat(execution.compileError()).contains("declared and not used");
    }

    @Test
    void shouldGiveTheFirstLineOfAPanicAfterTheOutput() {
        Execution execution = runner.run("""
                package main

                import "fmt"

                func main() {
                	var s []int
                	fmt.Println("avant")
                	fmt.Println(s[3])
                }
                """);

        assertThat(execution.output()).isEqualTo("avant\n");
        assertThat(execution.panic()).startsWith("panic: runtime error: index out of range [3] with length 0");
    }

    @Test
    void shouldReportADeadlock() {
        Execution execution = runner.run("package main\n\nfunc main() {\n\tch := make(chan int)\n\tch <- 1\n}\n");

        assertThat(execution.panic()).isEqualTo("fatal error: all goroutines are asleep - deadlock!");
    }
}
