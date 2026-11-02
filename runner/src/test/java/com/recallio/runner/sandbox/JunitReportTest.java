package com.recallio.runner.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.runner.sandbox.RunResult.TestOutcome;
import org.junit.jupiter.api.Test;

class JunitReportTest {

    @Test
    void shouldReadEachTestAndWhyItFailed() {
        String report = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="JUnit Jupiter" tests="3">
                  <testcase name="additionne()" classname="CaisseTest" time="0.01"/>
                  <testcase name="ignoreLesImpayees()" classname="CaisseTest" time="0.01">
                    <failure message="expected: &lt;1250&gt; but was: &lt;0&gt;" type="AssertionFailedError">trace</failure>
                  </testcase>
                  <testcase name="plante()" classname="CaisseTest" time="0.01">
                    <error type="java.lang.NullPointerException">trace</error>
                  </testcase>
                </testsuite>
                """;

        assertThat(JunitReport.parse(report))
                .containsExactly(
                        new TestOutcome("CaisseTest", "additionne", true, ""),
                        new TestOutcome("CaisseTest", "ignoreLesImpayees", false, "expected: <1250> but was: <0>"),
                        new TestOutcome("CaisseTest", "plante", false, "java.lang.NullPointerException"));
    }

    @Test
    void shouldTruncateAVeryLongMessage() {
        String report =
                "<testsuite><testcase name=\"t()\" classname=\"T\"><failure message=\"%s\"/></testcase></testsuite>"
                        .formatted("x".repeat(JunitReport.MAX_MESSAGE_LENGTH * 2));

        assertThat(JunitReport.parse(report).getFirst().message()).hasSize(JunitReport.MAX_MESSAGE_LENGTH + 1);
    }

    /** Le rapport sort d'un conteneur où tourne du code non fiable : pas d'entité externe (XXE). */
    @Test
    void shouldRefuseADocumentTypeDeclaration() {
        String report = """
                <?xml version="1.0"?>
                <!DOCTYPE testsuite [<!ENTITY secret SYSTEM "file:///etc/passwd">]>
                <testsuite><testcase name="t()" classname="&secret;"/></testsuite>
                """;

        assertThatThrownBy(() -> JunitReport.parse(report)).isInstanceOf(IllegalArgumentException.class);
    }
}
