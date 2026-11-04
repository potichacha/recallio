package com.recallio.runner.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import com.recallio.runner.sandbox.SandboxImage;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RunsResourceTest {

    private static final String TOKEN = "Bearer dev-token-du-runner-a-ne-jamais-deployer";

    private static final Map<String, Object> PASSING = Map.of(
            "language",
            "JAVA",
            "code",
            List.of(Map.of(
                    "path",
                    "Bonjour.java",
                    "content",
                    "public class Bonjour { static String dire() { return \"bonjour\"; } }")),
            "tests",
            List.of(Map.of("path", "BonjourTest.java", "content", """
                    import static org.junit.jupiter.api.Assertions.assertEquals;
                    import org.junit.jupiter.api.Test;
                    class BonjourTest {
                        @Test
                        void ditBonjour() {
                            assertEquals("bonjour", Bonjour.dire());
                        }
                    }
                    """)));

    @BeforeAll
    static void buildTheSandbox() {
        SandboxImage.ensureBuilt();
    }

    @Test
    void shouldRunTheTestsAndReturnEachResult() {
        given().header("Authorization", TOKEN)
                .contentType(ContentType.JSON)
                .body(PASSING)
                .post("/api/v1/runs")
                .then()
                .statusCode(200)
                .body("status", equalTo("PASSED"))
                .body("tests.name", contains("ditBonjour"))
                .body("tests[0].passed", equalTo(true));
    }

    @Test
    void shouldRefuseARequestWithoutTheToken() {
        given().contentType(ContentType.JSON)
                .body(PASSING)
                .post("/api/v1/runs")
                .then()
                .statusCode(401);
        given().header("Authorization", "Bearer un-autre-jeton-de-plus-de-trente-deux-caracteres")
                .contentType(ContentType.JSON)
                .body(PASSING)
                .post("/api/v1/runs")
                .then()
                .statusCode(401);
    }

    @Test
    void shouldRefuseAPathThatLeavesTheWorkingFolder() {
        given().header("Authorization", TOKEN)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "language",
                        "JAVA",
                        "code",
                        List.of(Map.of("path", "../../etc/Evasion.java", "content", "class Evasion {}")),
                        "tests",
                        List.of(Map.of("path", "T.java", "content", "class T {}"))))
                .post("/api/v1/runs")
                .then()
                .statusCode(400);
    }

    @Test
    void shouldRefuseFilesThatAreNotOfTheAnnouncedLanguage() {
        given().header("Authorization", TOKEN)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "language",
                        "JAVA",
                        "code",
                        List.of(Map.of("path", "somme.go", "content", "package somme")),
                        "tests",
                        List.of(Map.of("path", "somme_test.go", "content", "package somme"))))
                .post("/api/v1/runs")
                .then()
                .statusCode(400);
    }

    @Test
    void shouldRefuseARequestWithoutALanguage() {
        given().header("Authorization", TOKEN)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "code",
                        List.of(Map.of("path", "A.java", "content", "class A {}")),
                        "tests",
                        List.of(Map.of("path", "T.java", "content", "class T {}"))))
                .post("/api/v1/runs")
                .then()
                .statusCode(400);
    }

    @Test
    void shouldKeepTheHealthCheckOpen() {
        given().get("/q/health").then().statusCode(200).body("status", equalTo("UP"));
    }
}
