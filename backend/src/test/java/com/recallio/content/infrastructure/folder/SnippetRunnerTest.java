package com.recallio.content.infrastructure.folder;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.infrastructure.folder.SnippetRunner.Execution;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class SnippetRunnerTest {

    private static final SnippetRunner RUNNER = new SnippetRunner(Duration.ofSeconds(2));

    @AfterAll
    static void stopJShell() {
        RUNNER.close();
    }

    @Test
    void shouldRunStatementsWithTheDefaultImportsOfJShell() {
        Execution execution = RUNNER.run("List<Integer> l = new ArrayList<>(List.of(1, 2));\nSystem.out.println(l);");

        assertThat(execution).isEqualTo(new Execution("[1, 2]" + System.lineSeparator(), null, null));
    }

    @Test
    void shouldReportTheExceptionAfterWhatWasPrinted() {
        Execution execution =
                RUNNER.run("System.out.print(\"avant\");\nList.of(1).add(2);\nSystem.out.print(\"après\");");

        assertThat(execution).isEqualTo(new Execution("avant", null, "UnsupportedOperationException"));
    }

    @Test
    void shouldReportACompilationError() {
        Execution execution = RUNNER.run("short s = 1; s = s + 1;");

        assertThat(execution.compileError()).contains("incompatible types");
    }

    @Test
    void shouldReportAnUnresolvedReferenceAsACompilationError() {
        assertThat(RUNNER.run("System.out.println(inconnue);").compileError()).isNotNull();
    }

    @Test
    void shouldForgetTheDeclarationsOfTheEarlierSnippets() {
        RUNNER.run("class Parent { String name() { return \"premier\"; } }\nint compteur = 1;");

        assertThat(RUNNER.run("System.out.println(compteur);").compileError()).isNotNull();
        assertThat(RUNNER.run("class Parent {}\nSystem.out.print(new Parent().getClass().getSimpleName());"))
                .isEqualTo(new Execution("Parent", null, null));
    }

    @Test
    void shouldStopAnEndlessLoop() {
        Execution execution = RUNNER.run("while (true) {}");

        assertThat(execution.exception()).isEqualTo(SnippetRunner.TIMED_OUT);
        assertThat(RUNNER.run("System.out.print(1);").output()).isEqualTo("1");
    }
}
