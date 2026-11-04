package com.recallio.review.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.recallio.content.CodeLanguage;
import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.review.FakeProgramRunner;
import com.recallio.review.domain.playground.ProgramRun;
import com.recallio.review.domain.playground.ProgramStatus;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Le mode « programme » du runner est remplacé par {@link FakeProgramRunner}, qui a ses propres tests avec Docker. */
@QuarkusTest
class PlaygroundRunsTest {

    private static final String RUNS = "/api/v1/playground-runs";
    private static final String HELLO_GO =
            "package main\n\nimport \"fmt\"\n\nfunc main() {\n\tfmt.Println(\"Bonjour\")\n}\n";

    @Inject
    FakeProgramRunner runner;

    @BeforeEach
    void runnerWorks() {
        runner.willReturn(FakeProgramRunner.HELLO);
    }

    @Test
    void shouldRequireAuthentication() {
        run(Map.of("main.go", HELLO_GO)).statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "playground-run"))
    void shouldRunTheProgramAndReturnWhatItWrote() {
        runner.willReturn(new ProgramRun(ProgramStatus.COMPLETED, 2, "avant\n", "panic: oups", false, ""));

        run(Map.of("main.go", HELLO_GO))
                .statusCode(200)
                .body("status", equalTo("COMPLETED"))
                .body("exitCode", equalTo(2))
                .body("stdout", equalTo("avant\n"))
                .body("stderr", equalTo("panic: oups"))
                .body("truncated", equalTo(false));
        assertThat(runner.lastLanguage()).isEqualTo(CodeLanguage.GO);
        assertThat(runner.lastFiles()).containsExactly(new SourceFile("main.go", HELLO_GO));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "playground-invalid"))
    void shouldRefuseFilesThatDoNotMakeAProgram() {
        run(Map.of("Point.java", "record Point(int x, int y) {}"))
                .statusCode(422)
                .body("code", equalTo("review.invalid-program"));
        run(Map.of("main.go", HELLO_GO, "Main.java", "void main() {}"))
                .statusCode(422)
                .body("code", equalTo("review.invalid-program"));
        run(Map.of("main_test.go", "package main\n")).statusCode(422);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "playground-empty"))
    void shouldRefuseAnEmptyFileOrNoFileAtAll() {
        run(Map.of("main.go", "")).statusCode(400);
        given().contentType(ContentType.JSON)
                .body(Map.of("files", List.of()))
                .post(RUNS)
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "playground-unavailable"))
    void shouldAnswerUnavailableWhenTheRunnerIsDown() {
        runner.willBeUnavailable();

        run(Map.of("main.go", HELLO_GO)).statusCode(503).body("code", equalTo("review.runner-unavailable"));
    }

    private static ValidatableResponse run(Map<String, String> files) {
        return given().contentType(ContentType.JSON)
                .body(Map.of(
                        "files",
                        files.entrySet().stream()
                                .map(file -> Map.of("path", file.getKey(), "content", file.getValue()))
                                .toList()))
                .post(RUNS)
                .then();
    }
}
