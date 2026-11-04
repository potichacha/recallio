package com.recallio.runner.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import com.recallio.runner.sandbox.SandboxImage;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProgramsResourceTest {

    private static final String TOKEN = "Bearer dev-token-du-runner-a-ne-jamais-deployer";
    private static final String PROGRAMS = "/api/v1/programs";

    private static final Map<String, Object> HELLO = Map.of(
            "language",
            "GO",
            "files",
            List.of(Map.of(
                    "path",
                    "main.go",
                    "content",
                    "package main\n\nimport \"fmt\"\n\nfunc main() {\n\tfmt.Println(\"Bonjour\")\n}\n")));

    @BeforeAll
    static void buildTheSandbox() {
        SandboxImage.ensureBuilt();
    }

    @Test
    void shouldRunTheProgramAndReturnWhatItWrote() {
        given().header("Authorization", TOKEN)
                .contentType(ContentType.JSON)
                .body(HELLO)
                .post(PROGRAMS)
                .then()
                .statusCode(200)
                .body("status", equalTo("COMPLETED"))
                .body("exitCode", equalTo(0))
                .body("stdout", equalTo("Bonjour\n"))
                .body("truncated", equalTo(false));
    }

    @Test
    void shouldRefuseARequestWithoutTheToken() {
        given().contentType(ContentType.JSON).body(HELLO).post(PROGRAMS).then().statusCode(401);
    }

    @Test
    void shouldRefuseAJavaProgramWithoutItsMainFile() {
        given().header("Authorization", TOKEN)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "language",
                        "JAVA",
                        "files",
                        List.of(Map.of("path", "Point.java", "content", "record Point(int x, int y) {}"))))
                .post(PROGRAMS)
                .then()
                .statusCode(400)
                .body(containsString("Main.java"));
    }
}
