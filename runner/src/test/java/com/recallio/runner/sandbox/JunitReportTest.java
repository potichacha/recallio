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

        assertThat(JunitReport.parse(report, JunitReport.Message.ATTRIBUTE))
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

        assertThat(JunitReport.parse(report, JunitReport.Message.ATTRIBUTE)
                        .getFirst()
                        .message())
                .hasSize(JunitReport.MAX_MESSAGE_LENGTH + 1);
    }

    /**
     * Le rapport JUnit de doctest (ADR 0053) : son texte dit l'assertion et ses valeurs ; une exception y met son
     * message.
     */
    @Test
    void shouldReadTheTextOfADoctestReport() {
        String report = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuites>
                  <testsuite name="exercice" errors="1" failures="1" tests="3">
                    <testcase classname="panier_test.cpp" name="additionne les prix" status="run"/>
                    <testcase classname="panier_test.cpp" name="panier vide" status="run">
                      <failure message="0 == 1" type="CHECK">panier_test.cpp:4:
                CHECK( total({}) == 1 ) is NOT correct!
                  values: CHECK( 0 == 1 )
                </failure>
                    </testcase>
                    <testcase classname="panier_test.cpp" name="prix hors limites" status="run">
                      <error message="exception">vector::_M_range_check</error>
                    </testcase>
                  </testsuite>
                </testsuites>
                """;

        assertThat(JunitReport.parse(report, JunitReport.Message.TEXT))
                .containsExactly(
                        new TestOutcome("panier_test.cpp", "additionne les prix", true, ""),
                        new TestOutcome(
                                "panier_test.cpp",
                                "panier vide",
                                false,
                                "panier_test.cpp:4:\nCHECK( total({}) == 1 ) is NOT correct!\n  values: CHECK( 0 == 1 )"),
                        new TestOutcome("panier_test.cpp", "prix hors limites", false, "vector::_M_range_check"));
    }

    /** Le rapport sort d'un conteneur où tourne du code non fiable : pas d'entité externe (XXE). */
    @Test
    void shouldRefuseADocumentTypeDeclaration() {
        String report = """
                <?xml version="1.0"?>
                <!DOCTYPE testsuite [<!ENTITY secret SYSTEM "file:///etc/passwd">]>
                <testsuite><testcase name="t()" classname="&secret;"/></testsuite>
                """;

        assertThatThrownBy(() -> JunitReport.parse(report, JunitReport.Message.ATTRIBUTE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
