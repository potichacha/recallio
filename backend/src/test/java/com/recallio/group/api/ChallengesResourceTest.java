package com.recallio.group.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;

import com.recallio.content.application.ContentImportService;
import com.recallio.group.LeaderboardVisibility;
import com.recallio.group.application.ChallengeService;
import com.recallio.group.application.GroupSettingsService;
import com.recallio.group.application.StudyGroupService;
import com.recallio.group.domain.GroupSettings;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Les défis, sur le sujet « quiz » de test : quatre QCM dont on connaît les bonnes réponses. */
@QuarkusTest
class ChallengesResourceTest {

    private static final String GROUPS = "/api/v1/groups";
    private static final String CHALLENGES = "/api/v1/challenges";

    /** Les bonnes réponses des QCM du document de test, par énoncé. */
    private static final Map<String, String> RIGHT_ANSWERS = Map.of(
            "Combien font 2 + 2 ?", "[1]",
            "Quelle est la couleur du ciel par beau temps ?", "[0]",
            "Quels nombres sont pairs ?", "[0, 2]",
            "Combien de jours compte une semaine ?", "[1]");

    @Inject
    ContentImportService contentImport;

    @Inject
    StudyGroupService groupService;

    @Inject
    GroupSettingsService settingsService;

    @Inject
    ChallengeService challengeService;

    @Inject
    EntityManager entityManager;

    @BeforeEach
    void importQuiz() throws IOException {
        try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream("/content/quiz-sample.md"))) {
            contentImport.importContent(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void shouldRequireAuthentication() {
        given().get(CHALLENGES + "/" + UUID.randomUUID()).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = {@Claim(key = "sub", value = "challenge-owner"), @Claim(key = "name", value = "Alice")})
    void shouldPlayAWholeChallengeAndRankTheFinishedRuns() {
        String groupId = quizGroup();
        createChallenge(groupId, 5).statusCode(422).body("code", equalTo("group.not-enough-cards"));
        String challengeId = createChallenge(groupId, 3)
                .statusCode(201)
                .body("myStatus", equalTo("NOT_STARTED"))
                .body("open", is(true))
                .extract()
                .path("id");

        String first = next(challengeId)
                .body("card.position", is(0))
                .body("card.remainingMs", greaterThan(0))
                .body("card.remainingMs", lessThanOrEqualTo(30_000))
                .extract()
                .path("card.prompt");
        // Redemander la carte ne relance pas le chrono : c'est la même.
        next(challengeId).body("card.position", is(0)).body("card.prompt", equalTo(first));

        answer(challengeId, 0, rightAnswer(first)).statusCode(200).body("correct", is(true));
        answer(challengeId, 0, "[]").statusCode(200).body("correct", is(true)); // rejouée : même correction
        answer(challengeId, 2, "[0]").statusCode(409).body("code", equalTo("group.challenge-no-pending-card"));

        next(challengeId).body("card.position", is(1));
        answer(challengeId, 1, "[]").body("correct", is(false)).body("finished", is(false));
        String third = next(challengeId).extract().path("card.prompt");
        answer(challengeId, 2, rightAnswer(third)).body("finished", is(true));
        next(challengeId).body("finished", is(true)).body("card", nullValue()).body("score", is(2));

        given().get(CHALLENGES + "/" + challengeId)
                .then()
                .statusCode(200)
                .body("summary.myStatus", equalTo("FINISHED"))
                .body("summary.myScore", is(2))
                .body("standings", hasSize(1))
                .body("standings[0].displayName", equalTo("Alice"))
                .body("standings[0].me", is(true));
        given().get(GROUPS + "/" + groupId + "/challenges").then().body("id", contains(challengeId));
    }

    @Test
    @TestSecurity(user = "bob")
    @OidcSecurity(claims = @Claim(key = "sub", value = "challenge-late"))
    void shouldCountALateAnswerAsWrongAndLetOnlyTheOwnerLaunchAChallenge() {
        var owner = new UserRef(UUID.randomUUID(), ZoneOffset.UTC);
        UUID groupId = groupService.create(owner, "Défi du soir").id();
        addMember(groupId, myId());
        settingsService.update(
                owner,
                groupId,
                new GroupSettings(List.of("quiz"), List.of(), Set.of(), 10, LeaderboardVisibility.NAMED));
        UUID challengeId = challengeService
                .create(owner, groupId, "Défi", 3, 10)
                .challenge()
                .id();

        String prompt = next(challengeId.toString()).extract().path("card.prompt");
        presentedLongAgo(challengeId);
        answer(challengeId.toString(), 0, rightAnswer(prompt))
                .body("correct", is(false))
                .body("timedOut", is(true));

        createChallenge(groupId.toString(), 3).statusCode(403).body("code", equalTo("group.owner-only"));
    }

    @Test
    @TestSecurity(user = "bob")
    @OidcSecurity(claims = @Claim(key = "sub", value = "challenge-outsider"))
    void shouldHideTheChallengesOfOtherGroups() {
        var owner = new UserRef(UUID.randomUUID(), ZoneOffset.UTC);
        UUID groupId = groupService.create(owner, "Pas le mien").id();
        settingsService.update(
                owner,
                groupId,
                new GroupSettings(List.of("quiz"), List.of(), Set.of(), 10, LeaderboardVisibility.NAMED));
        UUID challengeId = challengeService
                .create(owner, groupId, "Secret", 3, 10)
                .challenge()
                .id();

        given().get(CHALLENGES + "/" + challengeId)
                .then()
                .statusCode(404)
                .body("code", equalTo("group.challenge-not-found"));
        given().post(CHALLENGES + "/" + challengeId + "/next").then().statusCode(404);
        given().get(GROUPS + "/" + groupId + "/challenges").then().statusCode(404);
    }

    /** Un groupe de l'utilisateur, qui révise le sujet « quiz ». */
    private static String quizGroup() {
        String groupId = given().contentType(ContentType.JSON)
                .body("{\"name\": \"Quiz\"}")
                .post(GROUPS)
                .then()
                .extract()
                .path("id");
        given().contentType(ContentType.JSON)
                .body("""
                        {"subjects": ["quiz"], "themes": [], "cardTypes": [], "dailyGoal": 10,
                         "leaderboardVisibility": "NAMED"}
                        """)
                .put(GROUPS + "/" + groupId + "/settings")
                .then()
                .statusCode(200);
        return groupId;
    }

    private static String rightAnswer(String prompt) {
        return Objects.requireNonNull(RIGHT_ANSWERS.get(prompt), () -> "QCM inattendu : " + prompt);
    }

    private static ValidatableResponse createChallenge(String groupId, int cardCount) {
        return given().contentType(ContentType.JSON)
                .body("{\"title\": \"Défi du lundi\", \"cardCount\": %d, \"secondsPerCard\": 30}".formatted(cardCount))
                .post(GROUPS + "/" + groupId + "/challenges")
                .then();
    }

    private static ValidatableResponse next(String challengeId) {
        return given().post(CHALLENGES + "/" + challengeId + "/next").then().statusCode(200);
    }

    private static ValidatableResponse answer(String challengeId, int position, String selectedChoices) {
        return given().contentType(ContentType.JSON)
                .body("{\"position\": %d, \"selectedChoices\": %s}".formatted(position, selectedChoices))
                .post(CHALLENGES + "/" + challengeId + "/answers")
                .then();
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

    /** Recule l'heure de présentation des cartes : le délai est dépassé, tolérance réseau comprise. */
    private void presentedLongAgo(UUID challengeId) {
        QuarkusTransaction.requiringNew()
                .run(() -> entityManager
                        .createNativeQuery("update challenge_answer set presented_at = :past where challenge_id = :id")
                        .setParameter("past", Instant.now().minusSeconds(60))
                        .setParameter("id", challengeId)
                        .executeUpdate());
    }
}
