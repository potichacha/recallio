package com.recallio.group.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.recallio.content.application.ContentImportService;
import com.recallio.group.application.GroupSettingsService;
import com.recallio.group.application.StudyGroupService;
import com.recallio.group.domain.GroupSettings;
import com.recallio.group.domain.LeaderboardVisibility;
import com.recallio.user.UserRef;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Les réglages d'un groupe, et la révision avec le groupe qui en découle. Java et Go sont publiés. */
@QuarkusTest
class GroupSettingsResourceTest {

    private static final String GROUPS = "/api/v1/groups";
    private static final String QUEUE = "/api/v1/review-queue";
    private static final String JAVA_BASICS = "java-fondamentaux";

    @Inject
    ContentImportService contentImport;

    @Inject
    StudyGroupService groupService;

    @Inject
    GroupSettingsService settingsService;

    @Inject
    EntityManager entityManager;

    @BeforeEach
    void importSubjects() throws IOException {
        for (String resource : List.of("/content/reference-sample.md", "/content/go-sample.md")) {
            try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(resource))) {
                contentImport.importContent(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "settings-owner"))
    void shouldStartWithDefaultsThenSaveTheSettingsOfTheOwner() {
        String groupId = given().contentType(ContentType.JSON)
                .body("{\"name\": \"Réglages\"}")
                .post(GROUPS)
                .then()
                .body("settings.subjects", empty())
                .body("settings.cardTypes", empty())
                .body("settings.dailyGoal", is(GroupSettings.DEFAULT_DAILY_GOAL))
                .body("settings.leaderboardVisibility", equalTo("NAMED"))
                .extract()
                .path("id");

        update(groupId, """
                        {"subjects": ["java", "go", "java"], "themes": ["%s"],
                         "cardTypes": ["OPEN_SCENARIO", "FLASHCARD"], "dailyGoal": 15,
                         "leaderboardVisibility": "ANONYMOUS"}
                        """.formatted(JAVA_BASICS))
                .statusCode(200)
                .body("settings.subjects", contains("java", "go"))
                .body("settings.themes", contains(JAVA_BASICS))
                .body("settings.cardTypes", contains("FLASHCARD", "OPEN_SCENARIO"));

        given().get(GROUPS + "/" + groupId)
                .then()
                .body("settings.dailyGoal", is(15))
                .body("settings.leaderboardVisibility", equalTo("ANONYMOUS"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "settings-invalid"))
    void shouldRejectUnknownSubjectsThemesOutsideTheGroupAndAnInvalidGoal() {
        String groupId = given().contentType(ContentType.JSON)
                .body("{\"name\": \"Invalide\"}")
                .post(GROUPS)
                .then()
                .extract()
                .path("id");

        update(groupId, settingsJson("[\"rust\"]", "[]", 10))
                .statusCode(422)
                .body("code", equalTo("group.unknown-subject"));
        update(groupId, settingsJson("[\"go\"]", "[\"" + JAVA_BASICS + "\"]", 10))
                .statusCode(422)
                .body("code", equalTo("group.unknown-theme"));
        update(groupId, settingsJson("[\"go\"]", "[]", 0)).statusCode(400).body("code", equalTo("validation.failed"));
    }

    @Test
    @TestSecurity(user = "bob")
    @OidcSecurity(claims = @Claim(key = "sub", value = "settings-member"))
    void shouldLetAMemberReviewWithTheGroupButNotChangeItsSettings() {
        var owner = new UserRef(UUID.randomUUID(), ZoneOffset.UTC);
        UUID groupId = groupService.create(owner, "Équipe Java").id();
        addMember(groupId, myId());
        settingsService.update(
                owner,
                groupId,
                new GroupSettings(
                        List.of("java", "go"), List.of(JAVA_BASICS), Set.of(), 10, LeaderboardVisibility.NAMED));

        // Java est restreint à un thème ; Go, sans thème sélectionné, les garde tous.
        given().get(QUEUE + "?group=" + groupId + "&subject=java")
                .then()
                .statusCode(200)
                .body("cards", not(empty()))
                .body("cards.themeSlug", everyItem(equalTo(JAVA_BASICS)));
        given().get(QUEUE + "?group=" + groupId + "&subject=go")
                .then()
                .body("cards", not(empty()))
                .body("cards.subjectSlug", everyItem(equalTo("go")));

        update(groupId.toString(), settingsJson("[\"go\"]", "[]", 10))
                .statusCode(403)
                .body("code", equalTo("group.owner-only"));
    }

    @Test
    @TestSecurity(user = "bob")
    @OidcSecurity(claims = @Claim(key = "sub", value = "settings-outsider"))
    void shouldHideTheCardsOfAGroupFromOutsiders() {
        UUID groupId = groupService
                .create(new UserRef(UUID.randomUUID(), ZoneOffset.UTC), "Pas le mien")
                .id();

        given().get(QUEUE + "?group=" + groupId).then().statusCode(404).body("code", equalTo("group.not-found"));
    }

    private static ValidatableResponse update(String groupId, String json) {
        return given().contentType(ContentType.JSON)
                .body(json)
                .put(GROUPS + "/" + groupId + "/settings")
                .then();
    }

    private static String settingsJson(String subjects, String themes, int dailyGoal) {
        return """
                {"subjects": %s, "themes": %s, "cardTypes": [], "dailyGoal": %d, "leaderboardVisibility": "NAMED"}
                """.formatted(subjects, themes, dailyGoal);
    }

    private static UUID myId() {
        return UUID.fromString(given().get("/api/v1/me").then().extract().path("id"));
    }

    private void addMember(UUID groupId, UUID userId) {
        QuarkusTransaction.requiringNew()
                .run(() -> entityManager
                        .createNativeQuery("""
                                insert into group_member (group_id, user_id, role, joined_at)
                                values (:groupId, :userId, 'MEMBER', :now)
                                """)
                        .setParameter("groupId", groupId)
                        .setParameter("userId", userId)
                        .setParameter("now", Instant.now())
                        .executeUpdate());
    }
}
