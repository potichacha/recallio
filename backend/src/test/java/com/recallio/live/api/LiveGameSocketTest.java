package com.recallio.live.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import com.recallio.content.application.ContentImportService;
import com.recallio.group.LeaderboardVisibility;
import com.recallio.group.application.GroupSettingsService;
import com.recallio.group.application.StudyGroupService;
import com.recallio.group.domain.GroupSettings;
import com.recallio.live.application.LiveGameService;
import com.recallio.user.UserRef;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * La salle d'attente d'une partie live : l'API REST pour l'ouvrir, le WebSocket pour la suivre. Les tests ouvrent de
 * vraies connexions WebSocket sur l'application démarrée.
 */
@QuarkusTest
class LiveGameSocketTest {

    private static final long TIMEOUT_SECONDS = 5;

    @TestHTTPResource("/")
    URI baseUri;

    @Inject
    ContentImportService contentImport;

    @Inject
    StudyGroupService groupService;

    @Inject
    GroupSettingsService settingsService;

    @Inject
    LiveGameService liveGames;

    /** Java (sans QCM) et « quiz » (quatre QCM dont on connaît les bonnes réponses). */
    @BeforeEach
    void importSubjects() throws IOException {
        for (String resource : List.of("/content/reference-sample.md", "/content/quiz-sample.md")) {
            try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(resource))) {
                contentImport.importContent(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    @Test
    void shouldRequireAuthenticationToOpenTheSocket() {
        given().get("/api/v1/live/" + UUID.randomUUID()).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = {@Claim(key = "sub", value = "live-host"), @Claim(key = "name", value = "Alice")})
    void shouldOpenAGameAndFollowItsLobbyLive() throws InterruptedException {
        String groupId = groupOn("java");
        open(groupId, "go").statusCode(422).body("code", equalTo("live.subject-not-in-group"));
        String gameId = open(groupId, "java")
                .statusCode(201)
                .body("status", equalTo("LOBBY"))
                .extract()
                .path("id");
        open(groupId, "java").statusCode(409).body("code", equalTo("live.game-already-open"));
        given().get("/api/v1/groups/" + groupId + "/live-games").then().body("id", equalTo(List.of(gameId)));

        Client first = connect(gameId);
        JsonPath joined = first.nextMessage();
        assertThat(joined.getString("type")).isEqualTo("game");
        assertThat(joined.getList("game.players.displayName")).containsExactly("Alice");
        assertThat(joined.getBoolean("game.players[0].host")).isTrue();

        // Un second onglet : le même joueur, une connexion de plus ; les deux sont tenus au courant.
        Client second = connect(gameId);
        assertThat(second.nextMessage().getList("game.players")).hasSize(1);
        assertThat(first.nextMessage().getList("game.players")).hasSize(1);

        // Java n'a pas de QCM : impossible de démarrer.
        given().post("/api/v1/live-games/" + gameId + "/start")
                .then()
                .statusCode(422)
                .body("code", equalTo("live.not-enough-questions"));

        first.connection().closeAndAwait();
        assertThat(second.nextMessage().getList("game.players")).hasSize(1);
        second.connection().closeAndAwait();

        // Le dernier joueur parti, la salle est fermée et le groupe peut en ouvrir une autre.
        awaitClosed(gameId);
        given().get("/api/v1/groups/" + groupId + "/live-games").then().body("$", empty());
    }

    @Test
    @TestSecurity(user = "bob")
    @OidcSecurity(claims = @Claim(key = "sub", value = "live-outsider"))
    void shouldRefuseTheGameOfAnotherGroup() throws InterruptedException {
        var host = new UserRef(UUID.randomUUID(), ZoneOffset.UTC);
        UUID groupId = groupService.create(host, "Pas le mien").id();
        settingsService.update(
                host,
                groupId,
                new GroupSettings(List.of("java"), List.of(), Set.of(), 10, LeaderboardVisibility.NAMED));
        UUID gameId = liveGames.open(host, groupId, "java").id();

        given().get("/api/v1/live-games/" + gameId).then().statusCode(404).body("code", equalTo("live.game-not-found"));
        given().get("/api/v1/groups/" + groupId + "/live-games").then().statusCode(404);

        BlockingQueue<Integer> closeCodes = new LinkedBlockingQueue<>();
        BasicWebSocketConnector.create()
                .baseUri(baseUri)
                .path("/api/v1/live/" + gameId)
                .onClose((connection, reason) -> closeCodes.add(reason.getCode()))
                .connectAndAwait();
        assertThat(closeCodes.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isEqualTo(LiveGameSocket.CLOSE_NOT_FOUND);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = {@Claim(key = "sub", value = "live-runner"), @Claim(key = "name", value = "Alice")})
    void shouldAskEveryQuestionInTurnUntilTheEnd() throws InterruptedException {
        String groupId = groupOn("quiz");
        String gameId = open(groupId, "quiz").statusCode(201).extract().path("id");
        Client player = connect(gameId);
        player.awaitStatus("LOBBY");

        JsonPath first = start(gameId);
        assertThat(first.getInt("round.index")).isZero();
        assertThat(first.getInt("round.total")).isEqualTo(3);
        assertThat(first.getLong("round.remainingMs")).isPositive();
        assertThat(first.getMap("round.reveal"))
                .as("la correction attend la fin de la question")
                .isNull();
        assertThat(player.awaitStatus("QUESTION").getInt("game.round.index")).isZero();

        // Seul joueur : sa réponse clôt la question, la correction est diffusée aussitôt.
        answer(gameId, 0, RIGHT_ANSWERS.get(first.getString("round.prompt")));
        JsonPath revealed = player.awaitStatus("REVEAL");
        assertThat(revealed.getInt("game.round.reveal.correctCount")).isEqualTo(1);
        assertThat(revealed.getList("game.players.answered")).containsExactly(true);

        // Après la correction, la question suivante ; sans réponse, le chrono la clôt.
        assertThat(player.awaitStatus("QUESTION").getInt("game.round.index")).isEqualTo(1);
        assertThat(player.awaitStatus("REVEAL").getInt("game.round.reveal.correctCount"))
                .isZero();

        String third = player.awaitStatus("QUESTION").getString("game.round.prompt");
        answer(gameId, 2, RIGHT_ANSWERS.get(third));
        player.awaitStatus("REVEAL");
        JsonPath finished = player.awaitStatus("FINISHED");

        // Deux bonnes réponses sur trois, données vite : entre 500 et 1 000 points chacune.
        assertThat(finished.getInt("game.standings[0].correctCount")).isEqualTo(2);
        assertThat(finished.getInt("game.standings[0].score")).isBetween(2 * 500, 2 * 1000);
        int score = finished.getInt("game.standings[0].score");

        // Le résultat est conservé, hors de la boucle d'événements : on attend qu'il soit écrit.
        String results = "/api/v1/groups/" + groupId + "/live-games/results";
        await().atMost(Duration.ofSeconds(TIMEOUT_SECONDS))
                .until(() -> given().get(results).jsonPath().getList("$").size() == 1);
        given().get(results)
                .then()
                .body("[0].gameId", equalTo(gameId))
                .body("[0].questionCount", equalTo(3))
                .body("[0].standings[0].displayName", equalTo("Alice"))
                .body("[0].standings[0].score", equalTo(score));

        given().contentType(ContentType.JSON)
                .body("{\"question\": 2, \"selectedChoices\": [0]}")
                .post("/api/v1/live-games/" + gameId + "/answers")
                .then()
                .statusCode(409)
                .body("code", equalTo("live.round-closed"));
        player.connection().closeAndAwait();
    }

    /** Les bonnes réponses des QCM du document « quiz », par énoncé. */
    private static final Map<String, String> RIGHT_ANSWERS = Map.of(
            "Combien font 2 + 2 ?", "[1]",
            "Quelle est la couleur du ciel par beau temps ?", "[0]",
            "Quels nombres sont pairs ?", "[0, 2]",
            "Combien de jours compte une semaine ?", "[1]");

    private static JsonPath start(String gameId) {
        return given().post("/api/v1/live-games/" + gameId + "/start")
                .then()
                .statusCode(200)
                .body("status", equalTo("QUESTION"))
                .extract()
                .jsonPath();
    }

    private static void answer(String gameId, int question, @Nullable String selectedChoices) {
        given().contentType(ContentType.JSON)
                .body("{\"question\": %d, \"selectedChoices\": %s}"
                        .formatted(question, Objects.requireNonNull(selectedChoices, "QCM inattendu")))
                .post("/api/v1/live-games/" + gameId + "/answers")
                .then()
                .statusCode(200);
    }

    /** Un groupe de l'utilisateur connecté, qui révise ce sujet. */
    private static String groupOn(String subject) {
        String groupId = given().contentType(ContentType.JSON)
                .body("{\"name\": \"Live\"}")
                .post("/api/v1/groups")
                .then()
                .extract()
                .path("id");
        given().contentType(ContentType.JSON)
                .body("""
                        {"subjects": ["%s"], "themes": [], "cardTypes": [], "dailyGoal": 10,
                         "leaderboardVisibility": "NAMED"}
                        """.formatted(subject))
                .put("/api/v1/groups/" + groupId + "/settings")
                .then()
                .statusCode(200);
        return groupId;
    }

    private static ValidatableResponse open(String groupId, String subject) {
        return given().contentType(ContentType.JSON)
                .body("{\"subject\": \"%s\"}".formatted(subject))
                .post("/api/v1/groups/" + groupId + "/live-games")
                .then();
    }

    private Client connect(String gameId) {
        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        WebSocketClientConnection connection = BasicWebSocketConnector.create()
                .baseUri(baseUri)
                .path("/api/v1/live/" + gameId)
                .onTextMessage((client, message) -> messages.add(message))
                .connectAndAwait();
        return new Client(connection, messages);
    }

    /** La fermeture se propage de façon asynchrone : on attend que la partie disparaisse. */
    private static void awaitClosed(String gameId) {
        await().atMost(Duration.ofSeconds(TIMEOUT_SECONDS))
                .until(() -> given().get("/api/v1/live-games/" + gameId).statusCode() == 404);
    }

    private record Client(WebSocketClientConnection connection, BlockingQueue<String> messages) {

        JsonPath nextMessage() throws InterruptedException {
            String message = messages.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(message).as("message du serveur").isNotNull();
            return JsonPath.from(Objects.requireNonNull(message));
        }

        /** Le prochain message où la partie a ce statut ; ceux d'avant (arrivées, réponses) sont passés. */
        JsonPath awaitStatus(String status) throws InterruptedException {
            while (true) {
                JsonPath message = nextMessage();
                if (status.equals(message.getString("game.status"))) {
                    return message;
                }
            }
        }
    }
}
