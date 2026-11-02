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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
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

    @BeforeEach
    void importJava() throws IOException {
        try (InputStream input =
                Objects.requireNonNull(getClass().getResourceAsStream("/content/reference-sample.md"))) {
            contentImport.importContent(new String(input.readAllBytes(), StandardCharsets.UTF_8));
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
        String groupId = javaGroup();
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

    /** Un groupe de l'utilisateur connecté, qui révise Java. */
    private static String javaGroup() {
        String groupId = given().contentType(ContentType.JSON)
                .body("{\"name\": \"Live\"}")
                .post("/api/v1/groups")
                .then()
                .extract()
                .path("id");
        given().contentType(ContentType.JSON)
                .body("""
                        {"subjects": ["java"], "themes": [], "cardTypes": [], "dailyGoal": 10,
                         "leaderboardVisibility": "NAMED"}
                        """)
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
    }
}
