package com.recallio.user.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import com.recallio.content.application.ContentImportService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Les sujets suivis (CLAUDE.md, 2.5) : choisis au premier login, modifiables dans le profil. */
@QuarkusTest
class FollowedSubjectsTest {

    private static final String ME = "/api/v1/me";
    private static final String FOLLOWED = "/api/v1/me/followed-subjects";

    @Inject
    ContentImportService contentImport;

    @BeforeEach
    void importJavaAndGo() throws IOException {
        for (String resource : new String[] {"/content/reference-sample.md", "/content/go-sample.md"}) {
            try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(resource))) {
                contentImport.importContent(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    @Test
    void shouldRequireAuthentication() {
        follow("[\"java\"]").statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "followed-new-user"))
    void shouldFollowNoSubjectUntilTheUserChooses() {
        given().get(ME).then().statusCode(200).body("followedSubjects", empty());
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "followed-choose"))
    void shouldListTheChosenSubjectsInCatalogueOrder() {
        follow("[\"go\", \"java\", \"go\"]").statusCode(200).body("followedSubjects", contains("java", "go"));

        given().get(ME).then().body("followedSubjects", contains("java", "go"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "followed-replace"))
    void shouldReplaceThePreviousChoice() {
        follow("[\"java\", \"go\"]").statusCode(200);

        follow("[\"go\"]").statusCode(200).body("followedSubjects", contains("go"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "followed-unknown"))
    void shouldRejectAnUnknownSubjectWithoutChangingTheChoice() {
        follow("[\"java\"]").statusCode(200);

        follow("[\"java\", \"cobol\"]")
                .statusCode(422)
                .contentType("application/problem+json")
                .body("code", equalTo("user.unknown-subject"));
        given().get(ME).then().body("followedSubjects", contains("java"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "followed-empty"))
    void shouldRequireAtLeastOneSubject() {
        follow("[]")
                .statusCode(400)
                .body("code", equalTo("validation.failed"))
                .body("invalidParams.name", hasItem("subjects"));
    }

    private static ValidatableResponse follow(String subjects) {
        return given().contentType(ContentType.JSON)
                .body("{\"subjects\": %s}".formatted(subjects))
                .put(FOLLOWED)
                .then();
    }
}
