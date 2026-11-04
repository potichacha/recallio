package com.recallio.runner.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.recallio.runner.sandbox.RunResult.TestOutcome;
import org.junit.jupiter.api.Test;

class GoTestReportTest {

    @Test
    void shouldReadEachTestAndWhatAFailingOneWrote() {
        String report = """
                {"Action":"start"}
                {"Action":"run","Test":"TestVide"}
                {"Action":"output","Test":"TestVide","Output":"=== RUN   TestVide\\n","OutputType":"frame"}
                {"Action":"pass","Test":"TestVide","Elapsed":0}
                {"Action":"run","Test":"TestTrois"}
                {"Action":"output","Test":"TestTrois","Output":"    somme_test.go:6: Somme = 0, attendu 6\\n","OutputType":"error"}
                {"Action":"output","Test":"TestTrois","Output":"--- FAIL: TestTrois (0.00s)\\n","OutputType":"frame"}
                {"Action":"fail","Test":"TestTrois","Elapsed":0}
                {"Action":"fail","Elapsed":0.004}
                """;

        assertThat(GoTestReport.parse(report))
                .containsExactly(
                        new TestOutcome("TestVide", "TestVide", true, ""),
                        new TestOutcome("TestTrois", "TestTrois", false, "somme_test.go:6: Somme = 0, attendu 6"));
    }

    @Test
    void shouldKeepSubtestsButNotTheirParent() {
        String report = """
                {"Action":"pass","Test":"TestSomme/vide"}
                {"Action":"fail","Test":"TestSomme/trois"}
                {"Action":"fail","Test":"TestSomme"}
                {"Action":"pass","Test":"TestSommeNegative"}
                {"Action":"fail"}
                """;

        assertThat(GoTestReport.parse(report))
                .extracting(TestOutcome::className, TestOutcome::name)
                .containsExactly(
                        tuple("TestSomme", "TestSomme/vide"),
                        tuple("TestSomme", "TestSomme/trois"),
                        tuple("TestSommeNegative", "TestSommeNegative"));
    }

    @Test
    void shouldStopAPanicMessageBeforeItsStack() {
        String report = """
                {"Action":"output","Test":"TestX","Output":"panic: runtime error: index out of range [5] with length 0\\n"}
                {"Action":"output","Test":"TestX","Output":"goroutine 7 [running]:\\n"}
                {"Action":"output","Test":"TestX","Output":"testing.tRunner.func1.2({0x6c9590, 0xc0000a8000})\\n"}
                {"Action":"fail","Test":"TestX"}
                {"Action":"fail"}
                """;

        assertThat(GoTestReport.parse(report))
                .singleElement()
                .extracting(TestOutcome::message)
                .isEqualTo("panic: runtime error: index out of range [5] with length 0");
    }

    @Test
    void shouldExplainAFailureThatWroteNothing() {
        String report = """
                {"Action":"fail","Test":"TestX"}
                {"Action":"fail"}
                """;

        assertThat(GoTestReport.parse(report))
                .singleElement()
                .extracting(TestOutcome::message)
                .isEqualTo("Le test a échoué.");
    }

    @Test
    void shouldGiveNoTestWhenTheProgramStoppedBeforeThePackageResult() {
        // Le code a appelé os.Exit(0) : test2json compte comme réussi le test en cours, et les suivants n'ont pas
        // tourné.
        String report = """
                {"Action":"run","Test":"TestVide"}
                {"Action":"pass","Test":"TestVide","Elapsed":0.002}
                """;

        assertThat(GoTestReport.parse(report)).isEmpty();
    }

    @Test
    void shouldRefuseAnUnreadableReport() {
        assertThatThrownBy(() -> GoTestReport.parse("pas du json")).isInstanceOf(IllegalArgumentException.class);
    }
}
