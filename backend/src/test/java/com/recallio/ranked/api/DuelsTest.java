package com.recallio.ranked.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.CardType;
import com.recallio.content.application.ContentImportService;
import com.recallio.content.domain.source.SubjectFolder;
import com.recallio.ranked.application.DuelService;
import com.recallio.ranked.application.DuelService.Turn;
import com.recallio.ranked.domain.Duel;
import com.recallio.ranked.domain.Duel.Mode;
import com.recallio.ranked.domain.Duel.Status;
import com.recallio.ranked.domain.RankedRepositories.StandingRepository;
import com.recallio.ranked.domain.Standing;
import com.recallio.ranked.domain.glicko.Rating;
import com.recallio.review.CardReviewed;
import com.recallio.user.UserRef;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Les duels du classé (ADR 0046), de bout en bout : la recherche, les questions servies une fois, la correction, le
 * règlement et les cotes. Chaque test a ses propres joueurs ; le sujet de test a ses propres cartes.
 */
@QuarkusTest
class DuelsTest {

    private static final String SUBJECT = "lua";

    private static final String SUBJECT_HEADER = """
            subject: lua
            title: Lua
            category: languages
            position: 1000
            status: published
            """;

    private static final String REFERENTIAL = """
            domain: bases
            title: Les bases
            position: 10
            chapters:
              - id: tout
                title: Tout
                notions:
                  - {id: tables, title: "Les tables", level: beginner, priority: 1}
                  - {id: fonctions, title: "Les fonctions", level: beginner, priority: 1}
            """;

    private static final String TABLES = """
            notion: tables
            cards:
              - {key: vf1, format: true-false, question: "Les tables Lua commencent à 0.", truth: false, answer: "À 1."}
              - {key: vf2, format: true-false, question: "Une table peut servir de tableau.", truth: true, answer: "Oui."}
              - key: choix
                format: multiple-choice
                question: "Quelle fonction donne la longueur d'une table ?"
                choices:
                  - {text: "`#`", correct: true}
                  - {text: "`len`", correct: false}
                answer: "L'opérateur `#`."
            """;

    private static final String FONCTIONS = """
            notion: fonctions
            cards:
              - {key: trou, format: fill-in, question: "`___ carre(x) return x * x end`", blanks: [[function]], answer: "Le mot function."}
              - {key: vf, format: true-false, question: "Une fonction Lua peut rendre plusieurs valeurs.", truth: true, answer: "Oui."}
              - {key: vf3, format: true-false, question: "Lua n'a pas de fonctions anonymes.", truth: false, answer: "Il en a."}
            """;

    @Inject
    ContentImportService contentImport;

    @Inject
    DuelService duels;

    @Inject
    StandingRepository standings;

    @Inject
    CardDirectory cards;

    @Inject
    EntityManager entityManager;

    @Inject
    Event<CardReviewed> reviewed;

    @BeforeEach
    void importSubject() {
        contentImport.importFolder(new SubjectFolder(
                SUBJECT_HEADER,
                Map.of("01-bases.yaml", REFERENTIAL),
                Map.of("bases/tables.yaml", TABLES, "bases/fonctions.yaml", FONCTIONS)));
    }

    @Test
    void shouldMatchTwoPlayersOnTheSameQuestionsThenRateTheWinnerUp() {
        UserRef alice = player();
        UserRef bob = player();

        Duel opened = duels.findAsync(alice, SUBJECT);
        assertThat(opened.status()).isEqualTo(Status.OPEN);
        assertThat(opened.cardIds()).hasSize(Duel.QUESTIONS).doesNotHaveDuplicates();
        assertThat(duels.findAsync(alice, SUBJECT).id()).isEqualTo(opened.id());
        playAll(alice, opened.id(), true);

        Duel joined = duels.findAsync(bob, SUBJECT);
        assertThat(joined.id()).isEqualTo(opened.id());
        assertThat(joined.status()).isEqualTo(Status.FULL);
        playAll(bob, opened.id(), false);

        Duel settled = duels.view(alice, opened.id());
        assertThat(settled.status()).isEqualTo(Status.FINISHED);
        Standing winner = standings.find(alice.id(), SUBJECT).orElseThrow();
        Standing loser = standings.find(bob.id(), SUBJECT).orElseThrow();
        assertThat(winner.rating().rating()).isGreaterThan(1500);
        assertThat(loser.rating().rating()).isLessThan(1500);
        assertThat(winner.wins()).isEqualTo(1);
        assertThat(loser.losses()).isEqualTo(1);
        assertThat(winner.isPlacing()).isTrue();
        assertThat(ratedCards(opened.cardIds())).isEqualTo(Duel.QUESTIONS);
    }

    @Test
    void shouldServeAQuestionOnceAndCountALateAnswerWrong() {
        UserRef alice = player();
        Duel duel = duels.findAsync(alice, SUBJECT);
        Turn first = duels.next(alice, duel.id()).orElseThrow();
        assertThat(duels.next(alice, duel.id()).orElseThrow().position()).isEqualTo(first.position());

        QuarkusTransaction.requiringNew()
                .run(() -> entityManager
                        .createNativeQuery(
                                "update duel_answer set served_at = served_at - interval '1 minute' where duel_id = :id")
                        .setParameter("id", duel.id())
                        .executeUpdate());

        var answered = answerRight(alice, duel.id(), first);
        assertThat(answered.correction().correct()).isTrue();
        assertThat(answered.late()).isTrue();
        assertThat(answered.counted()).isFalse();
        assertThat(duels.next(alice, duel.id()).orElseThrow().position()).isEqualTo(1);
    }

    @Test
    void shouldExpireAnOpenDuelNobodyJoinedWithoutChangingAnyRating() {
        UserRef alice = player();
        Duel duel = duels.findAsync(alice, SUBJECT);
        QuarkusTransaction.requiringNew()
                .run(() -> entityManager
                        .createNativeQuery("update duel set expires_at = created_at - interval '1 hour' where id = :id")
                        .setParameter("id", duel.id())
                        .executeUpdate());

        assertThat(duels.view(alice, duel.id()).status()).isEqualTo(Status.EXPIRED);
        assertThat(standings.find(alice.id(), SUBJECT)).isEmpty();
        assertThat(duels.findAsync(alice, SUBJECT).id()).isNotEqualTo(duel.id());
    }

    @Test
    void shouldPairTwoPlayersWaitingForALiveDuel() {
        UserRef alice = player();
        UserRef bob = player();

        assertThat(duels.queueLive(alice, SUBJECT)).isEmpty();
        Optional<UUID> found = duels.queueLive(bob, SUBJECT);
        assertThat(found).isPresent();
        assertThat(duels.queueLive(alice, SUBJECT)).isEqualTo(found);

        Duel live = duels.view(alice, found.orElseThrow());
        assertThat(live.mode()).isEqualTo(Mode.LIVE);
        assertThat(live.status()).isEqualTo(Status.FULL);
        assertThat(live.players()).extracting(Duel.DuelPlayer::userId).containsExactly(alice.id(), bob.id());
    }

    @Test
    void shouldRateACardFromItsServerGradedReviewsOnly() {
        UUID trueFalse = cardOfType(CardType.TRUE_FALSE);
        double before = cardRating(trueFalse).orElse(Rating.START_RATING);
        long answersBefore = cardAnswers(trueFalse);

        review(trueFalse, CardType.TRUE_FALSE, true);
        review(trueFalse, CardType.FLASHCARD, true);

        // Une bonne réponse fait perdre la carte ; un format auto-évalué ne compte pas.
        assertThat(cardRating(trueFalse).orElseThrow()).isLessThan(before);
        assertThat(cardAnswers(trueFalse)).isEqualTo(answersBefore + 1);
    }

    @Test
    void shouldRequireAuthentication() {
        given().contentType(ContentType.JSON)
                .body("{\"subject\": \"lua\"}")
                .post("/api/v1/ranked/duels")
                .then()
                .statusCode(401);
        given().get("/api/v1/me/ranked").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "ranked-api"))
    void shouldPlayADuelThroughTheApiAndShowRanksNotRatings() {
        String id = given().contentType(ContentType.JSON)
                .body("{\"subject\": \"lua\"}")
                .post("/api/v1/ranked/duels")
                .then()
                .statusCode(200)
                .body("status", is("OPEN"))
                .body("questionCount", is(Duel.QUESTIONS))
                .body("opponent", org.hamcrest.Matchers.nullValue())
                .extract()
                .path("id");

        ValidatableResponse turn = given().post("/api/v1/ranked/duels/" + id + "/next")
                .then()
                .statusCode(200)
                .body("finished", is(false))
                .body("position", is(0))
                .body("remainingMs", greaterThan(25_000))
                .body("question.prompt", notNullValue());
        String type = turn.extract().path("question.type");
        answer(id, 1, "[0]").statusCode(409).body("code", is("ranked.question-not-served"));
        answer(id, 0, type.equals("FILL_IN") ? "[0]" : null).statusCode(422).body("code", is("ranked.answer-mismatch"));

        // Le classé montre les sujets suivis, même sans duel réglé.
        given().contentType(ContentType.JSON)
                .body("{\"subjects\": [\"lua\"]}")
                .put("/api/v1/me/followed-subjects")
                .then()
                .statusCode(200);
        String body = given().get("/api/v1/me/ranked")
                .then()
                .statusCode(200)
                .body("season", notNullValue())
                .body("subjects.find { it.subject == 'lua' }.placing", is(true))
                .body("subjects.find { it.subject == 'lua' }.placementDuels", is(5))
                .extract()
                .asString();
        assertThat(body).doesNotContain("rating", "deviation");
        given().get("/api/v1/me/ranked/duels").then().statusCode(200).body("id", hasItem(id));
    }

    private static UserRef player() {
        return new UserRef(UUID.randomUUID(), ZoneOffset.UTC);
    }

    private void playAll(UserRef user, UUID duelId, boolean right) {
        for (Optional<Turn> turn = duels.next(user, duelId); turn.isPresent(); turn = duels.next(user, duelId)) {
            if (right) {
                answerRight(user, duelId, turn.get());
            } else {
                answerWrong(user, duelId, turn.get());
            }
            if (turn.get().position() == turn.get().total() - 1) {
                return;
            }
        }
    }

    private DuelService.Answered answerRight(UserRef user, UUID duelId, Turn turn) {
        CardSnapshot card = turn.card();
        return card.blanks().isEmpty()
                ? duels.answer(user, duelId, turn.position(), card.correctChoices(), null)
                : duels.answer(
                        user,
                        duelId,
                        turn.position(),
                        null,
                        card.blanks().stream().map(blank -> blank.expected()).toList());
    }

    private void answerWrong(UserRef user, UUID duelId, Turn turn) {
        CardSnapshot card = turn.card();
        if (card.blanks().isEmpty()) {
            Set<Integer> wrong = card.correctChoices().contains(0) ? Set.of(1) : Set.of(0);
            duels.answer(user, duelId, turn.position(), wrong, null);
        } else {
            duels.answer(
                    user,
                    duelId,
                    turn.position(),
                    null,
                    Collections.nCopies(card.blanks().size(), "faux"));
        }
    }

    private static ValidatableResponse answer(String duelId, int position, @Nullable String selectedChoices) {
        String choices = selectedChoices == null ? "" : ", \"selectedChoices\": " + selectedChoices;
        return given().contentType(ContentType.JSON)
                .body("{\"position\": %d%s}".formatted(position, choices))
                .post("/api/v1/ranked/duels/" + duelId + "/answers")
                .then();
    }

    private UUID cardOfType(CardType type) {
        return cards.activeCards(new CardFilter(Set.of(SUBJECT), null, Set.of(), Set.of(type)))
                .getFirst()
                .cardId();
    }

    private void review(UUID cardId, CardType type, boolean recalled) {
        QuarkusTransaction.requiringNew()
                .run(() -> reviewed.fire(
                        new CardReviewed(player(), UUID.randomUUID(), cardId, SUBJECT, type, recalled, Instant.now())));
    }

    private Optional<Double> cardRating(UUID cardId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            List<?> rows = entityManager
                    .createNativeQuery("select rating from card_rating where card_id = :id")
                    .setParameter("id", cardId)
                    .getResultList();
            return rows.stream().findFirst().map(value -> ((Number) value).doubleValue());
        });
    }

    private long cardAnswers(UUID cardId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            List<?> rows = entityManager
                    .createNativeQuery("select answers from card_rating where card_id = :id")
                    .setParameter("id", cardId)
                    .getResultList();
            return rows.stream()
                    .findFirst()
                    .map(value -> ((Number) value).longValue())
                    .orElse(0L);
        });
    }

    private long ratedCards(List<UUID> cardIds) {
        return QuarkusTransaction.requiringNew()
                .call(() -> ((Number) entityManager
                                .createNativeQuery(
                                        "select count(*) from card_rating where card_id = any (cast(:ids as uuid[]))")
                                .setParameter("ids", cardIds.toArray(UUID[]::new))
                                .getSingleResult())
                        .longValue());
    }
}
