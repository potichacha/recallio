package com.recallio.review.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.content.application.ContentImportService;
import com.recallio.content.domain.source.SubjectFolder;
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

/**
 * Juger un « À toi » du Tour (ADR 0037) : le programme passe par le runner, remplacé ici par {@link FakeProgramRunner},
 * et sa sortie est comparée à celle attendue, qui ne quitte pas le serveur.
 */
@QuarkusTest
class TourTaskTest {

    private static final String TASKS = "/api/v1/me/tour/zig/bonjour/tasks";

    private static final String SUBJECT = """
            subject: zig
            title: Zig
            category: languages
            position: 950
            status: published
            """;

    private static final String REFERENTIAL = """
            domain: bases
            title: Les bases
            position: 10
            chapters:
              - id: premiers-pas
                title: Premiers pas
                notions:
                  - {id: bonjour, title: "Le premier programme", level: beginner, priority: 1}
            """;

    /** Deux pages : la première a un « À toi », qui attend « Salut secret » ; la seconde n'en a pas. */
    private static final String BONJOUR = """
            notion: bonjour
            tour:
              - text: Le premier programme.
                example:
                  files:
                    main.go: |-
                      package main

                      func main() {}
                  output: rien
                task:
                  instructions: Fais afficher le salut secret.
                  starter:
                    main.go: |-
                      package main

                      func main() {}
                  solution:
                    main.go: |-
                      package main

                      import "fmt"

                      func main() { fmt.Println("Salut secret") }
                  output: Salut secret
              - text: Une page sans « À toi ».
                example:
                  files:
                    main.go: |-
                      package main

                      func main() {}
                  output: rien
            cards:
              - {key: vrai-faux-1, format: true-false, question: "main est le point d'entrée.", truth: true, answer: "Oui."}
              - {key: vrai-faux-2, format: true-false, question: "main prend des paramètres.", truth: false, answer: "Non."}
            """;

    private static final String PROGRAM =
            "package main\n\nimport \"fmt\"\n\nfunc main() { fmt.Println(\"Salut secret\") }";

    @Inject
    ContentImportService contentImport;

    @Inject
    FakeProgramRunner runner;

    @BeforeEach
    void importSubject() {
        contentImport.importFolder(new SubjectFolder(
                SUBJECT, Map.of("01-bases.yaml", REFERENTIAL), Map.of("bases/bonjour.yaml", BONJOUR)));
        runner.willReturn(FakeProgramRunner.HELLO);
    }

    @Test
    void shouldRequireAuthentication() {
        check(1, "main.go").statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "tour-task-passed"))
    void shouldPassWhenTheProgramWritesTheExpectedOutput() {
        runner.willReturn(new ProgramRun(ProgramStatus.COMPLETED, 0, "Salut secret\r\n\n", "", false, ""));

        check(1, "main.go")
                .statusCode(200)
                .body("passed", is(true))
                .body("run.status", equalTo("COMPLETED"))
                .body("run.stdout", equalTo("Salut secret\r\n\n"));

        assertThat(runner.lastFiles()).containsExactly(new SourceFile("main.go", PROGRAM));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "tour-task-failed"))
    void shouldFailWhenTheProgramWritesSomethingElseOrDoesNotFinish() {
        check(1, "main.go").statusCode(200).body("passed", is(false)).body("run.stdout", equalTo("Bonjour\n"));

        runner.willReturn(new ProgramRun(ProgramStatus.COMPLETED, 1, "Salut secret\n", "panic", false, ""));
        check(1, "main.go").statusCode(200).body("passed", is(false));

        runner.willReturn(new ProgramRun(ProgramStatus.COMPILATION_ERROR, 0, "", "", false, "undefined: x"));
        check(1, "main.go").statusCode(200).body("passed", is(false)).body("run.message", equalTo("undefined: x"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "tour-task-refused"))
    void shouldRefuseAPageWithoutTaskOrOtherFiles() {
        check(2, "main.go").statusCode(404).body("code", equalTo("review.tour-task-not-found"));
        check(9, "main.go").statusCode(404).body("code", equalTo("review.tour-task-not-found"));
        check(1, "autre.go").statusCode(422).body("code", equalTo("review.answer-mismatch"));
        check(0, "main.go").statusCode(400);
    }

    private static ValidatableResponse check(int page, String path) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("page", page, "files", List.of(Map.of("path", path, "content", PROGRAM))))
                .post(TASKS)
                .then();
    }
}
