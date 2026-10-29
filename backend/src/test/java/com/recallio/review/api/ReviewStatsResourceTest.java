package com.recallio.review.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.recallio.content.application.ContentImportService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Document d'exemple : 4 cartes dans « java-fondamentaux » ; quota de 3 nouvelles cartes par jour en test. */
@QuarkusTest
class ReviewStatsResourceTest {

    private static final String STATS = "/api/v1/me/review-stats";
    private static final String THEME = "java-fondamentaux";

    @Inject
    ContentImportService contentImport;

    @BeforeEach
    void importSample() throws IOException {
        try (InputStream input =
                Objects.requireNonNull(getClass().getResourceAsStream("/content/reference-sample.md"))) {
            contentImport.importContent(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void shouldRequireAuthentication() {
        given().get(STATS).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "stats-new-user"))
    void shouldShowAllCardsAsNewForANewUser() {
        given().get(STATS)
                .then()
                .statusCode(200)
                .body("overall.seen", is(0))
                .body("overall.dueNow", is(0))
                .body("newToday", is(3))
                .body("themes.find { it.slug == '%s' }.counts.total".formatted(THEME), is(4))
                .body("themes.find { it.slug == '%s' }.title".formatted(THEME), equalTo("JAVA : FONDAMENTAUX"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "stats-after-review"))
    void shouldCountReviewedCardsPerThemeAndAsDueToday() {
        String cardId = given().queryParam("theme", THEME)
                .get("/api/v1/review-queue")
                .then()
                .extract()
                .path("cards[0].cardId");
        given().contentType(ContentType.JSON)
                .body("{\"requestId\": \"%s\", \"cardId\": \"%s\", \"rating\": \"AGAIN\"}"
                        .formatted(UUID.randomUUID(), cardId))
                .post("/api/v1/reviews")
                .then()
                .statusCode(201);

        given().get(STATS)
                .then()
                .statusCode(200)
                .body("overall.seen", is(1))
                .body("overall.dueToday", is(1))
                .body("overall.mastered", is(0))
                .body("newToday", is(2))
                .body("themes.find { it.slug == '%s' }.counts.seen".formatted(THEME), is(1));
    }
}
