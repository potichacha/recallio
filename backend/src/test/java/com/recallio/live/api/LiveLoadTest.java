package com.recallio.live.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import com.recallio.content.application.ContentImportService;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/**
 * Un test de charge simple (CLAUDE.md, phase 3 ; ADR 0024) : cent connexions WebSocket suivent la même partie, et
 * chacune doit recevoir chaque étape jusqu'à la fin, vite. Les connexions sont celles d'un même joueur (plusieurs
 * onglets) : c'est la diffusion qui est mise à l'épreuve, pas la limite de joueurs.
 */
@QuarkusTest
class LiveLoadTest {

    private static final int CONNECTIONS = 100;
    /** Trois questions de 2 s et leurs corrections de 1 s, en profil de test : une dizaine de secondes. */
    private static final long GAME_TIMEOUT_SECONDS = 30;
    /** Le délai toléré entre le démarrage et la première question chez la plus lente des connexions. */
    private static final long MAX_FIRST_QUESTION_DELAY_MS = 2_000;

    @TestHTTPResource("/")
    URI baseUri;

    @Inject
    ContentImportService contentImport;

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = {@Claim(key = "sub", value = "live-load"), @Claim(key = "name", value = "Alice")})
    void shouldDeliverEveryStepOfAGameToAHundredConnections() throws Exception {
        importQuiz();
        String gameId = openGame();

        CountDownLatch inLobby = new CountDownLatch(CONNECTIONS);
        CountDownLatch finished = new CountDownLatch(CONNECTIONS);
        AtomicLong startedAt = new AtomicLong();
        var firstQuestionDelays = new ConcurrentHashMap<Integer, Long>();
        Set<Integer> inLobbyClients = ConcurrentHashMap.newKeySet();
        List<WebSocketClientConnection> connections = new ArrayList<>();
        for (int index = 0; index < CONNECTIONS; index++) {
            int client = index;
            connections.add(BasicWebSocketConnector.create()
                    .baseUri(baseUri)
                    .path("/api/v1/live/" + gameId)
                    .onTextMessage((connection, message) -> {
                        String status = JsonPath.from(message).getString("game.status");
                        switch (status) {
                            // Chaque arrivée est diffusée à tous : on ne compte que le premier message de chaque
                            // connexion.
                            case "LOBBY" -> {
                                if (inLobbyClients.add(client)) {
                                    inLobby.countDown();
                                }
                            }
                            case "QUESTION" ->
                                firstQuestionDelays.putIfAbsent(client, System.nanoTime() - startedAt.get());
                            case "FINISHED" -> finished.countDown();
                            default -> {}
                        }
                    })
                    .connectAndAwait());
        }
        assertThat(inLobby.await(GAME_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        given().get("/q/metrics").then().body(containsString("recallio_live_connections"));

        startedAt.set(System.nanoTime());
        given().post("/api/v1/live-games/" + gameId + "/start").then().statusCode(200);

        assertThat(finished.await(GAME_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("chaque connexion voit la fin de la partie")
                .isTrue();
        assertThat(firstQuestionDelays).hasSize(CONNECTIONS);
        long slowestMs = TimeUnit.NANOSECONDS.toMillis(firstQuestionDelays.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElseThrow());
        assertThat(slowestMs).isLessThan(MAX_FIRST_QUESTION_DELAY_MS);
        connections.forEach(WebSocketClientConnection::closeAndAwait);
    }

    private void importQuiz() throws IOException {
        try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream("/content/quiz-sample.md"))) {
            contentImport.importContent(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    /** Un groupe de l'utilisateur sur le sujet « quiz », et sa partie. */
    private static String openGame() {
        String groupId = given().contentType(ContentType.JSON)
                .body("{\"name\": \"Charge\"}")
                .post("/api/v1/groups")
                .then()
                .extract()
                .path("id");
        given().contentType(ContentType.JSON)
                .body("""
                        {"subjects": ["quiz"], "themes": [], "cardTypes": [], "dailyGoal": 10,
                         "leaderboardVisibility": "NAMED"}
                        """)
                .put("/api/v1/groups/" + groupId + "/settings")
                .then()
                .statusCode(200);
        return given().contentType(ContentType.JSON)
                .body("{\"subject\": \"quiz\"}")
                .post("/api/v1/groups/" + groupId + "/live-games")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }
}
