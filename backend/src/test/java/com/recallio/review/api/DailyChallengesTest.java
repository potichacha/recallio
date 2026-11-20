package com.recallio.review.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.CardType;
import com.recallio.content.application.ContentImportService;
import com.recallio.content.domain.source.SubjectFolder;
import com.recallio.review.application.DailyChallengeService;
import com.recallio.review.application.DailyChallengeService.ChallengeView;
import com.recallio.user.UserRef;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Le défi du jour (ADR 0067). Pascal a six notions sur deux domaines : trois débutantes, deux intermédiaires et une
 * experte, dont la carte est un code à compléter ; chaque autre carte se répond « Oui » (le premier choix). Le mot en tête
 * de chaque question dit son niveau. Les profils des tests sont en UTC, et l'horloge des tests est à midi du jour.
 */
@QuarkusTest
class DailyChallengesTest {

    private static final String DAILY = "/api/v1/me/daily-challenges";

    private static final List<String> INCREASING =
            List.of("Débutant", "Débutant", "Intermédiaire", "Intermédiaire", "Expert");

    /** Une notion d'un sujet de test : son domaine, son niveau, et les clés de ses cartes. */
    private record Notion(String id, String domain, String level, List<String> keys) {}

    private static final List<Notion> PASCAL = List.of(
            new Notion("b1", "bases", "beginner", List.of("a")),
            new Notion("b2", "bases", "beginner", List.of("a")),
            new Notion("i1", "bases", "intermediate", List.of("a")),
            new Notion("b3", "outils", "beginner", List.of("a")),
            new Notion("i2", "outils", "intermediate", List.of("a")),
            new Notion("e1", "outils", "expert", List.of("a")));

    /** Deux cartes par notion, juste de quoi tirer un défi sans les cartes d'un défi passé. */
    private static final List<Notion> MODULA = List.of(
            new Notion("mb1", "bases", "beginner", List.of("a", "b")),
            new Notion("mb2", "bases", "beginner", List.of("a", "b")),
            new Notion("mi1", "outils", "intermediate", List.of("a", "b")),
            new Notion("mi2", "outils", "intermediate", List.of("a", "b")),
            new Notion("me1", "outils", "expert", List.of("a", "b")));

    /** Quatre notions : trop peu pour un défi. */
    private static final List<Notion> LOGO = PASCAL.subList(0, 4);

    @Inject
    ContentImportService contentImport;

    @Inject
    DailyChallengeService service;

    @Inject
    CardDirectory cards;

    @Inject
    EntityManager entityManager;

    @BeforeEach
    void importSubjects() {
        contentImport.importFolder(folder("pascal", 1010, "published", PASCAL));
        contentImport.importFolder(folder("modula", 1020, "published", MODULA));
        contentImport.importFolder(folder("ada", 1030, "draft", PASCAL));
        contentImport.importFolder(folder("logo", 1040, "published", LOGO));
        contentImport.importFolder(folder("simula", 1050, "published", PASCAL));
    }

    @Test
    void shouldRequireAuthentication() {
        given().get(DAILY).then().statusCode(401);
        given().get(DAILY + "/pascal").then().statusCode(401);
        answer(UUID.randomUUID().toString(), UUID.randomUUID(), 0, choices(0)).statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "daily-same"))
    void shouldGiveEveryoneTheSameQuestionsOfIncreasingDifficultyWithoutTheirAnswers() {
        JsonPath challenge = given().get(DAILY + "/pascal")
                .then()
                .statusCode(200)
                .body("subjectSlug", is("pascal"))
                .body("subjectTitle", is("Pascal"))
                .body("day", is(today().toString()))
                .body("state", is("NOT_STARTED"))
                .body("score", nullValue())
                .body("questionCount", is(5))
                .body("questions.position", contains(0, 1, 2, 3, 4))
                .body("answers", empty())
                .body("stats", nullValue())
                .extract()
                .jsonPath();
        List<String> prompts = challenge.getList("questions.prompt");
        assertThat(prompts).extracting(prompt -> prompt.split(" ")[0]).containsExactlyElementsOf(INCREASING);
        assertThat(prompts).extracting(prompt -> prompt.split(" ")[1]).doesNotHaveDuplicates();
        // Ni les bonnes réponses, ni les explications avant d'avoir répondu.
        assertThat(challenge.prettify()).doesNotContain("correct", "Oui.", "Un int.");

        // Un autre joueur, le même jour : le même défi, les mêmes questions dans le même ordre.
        ChallengeView other = service.challenge(new UserRef(UUID.randomUUID(), ZoneOffset.UTC), "pascal");
        assertThat(other.challenge().id()).hasToString(challenge.getString("id"));
        assertThat(other.questions().values())
                .extracting(card -> card.cardId().toString())
                .containsExactlyElementsOf(challenge.getList("questions.cardId"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "daily-player"))
    void shouldAnswerEachQuestionOnceThenShowTheScoreAndTheFiguresOfTheDay() {
        String id =
                given().get(DAILY + "/pascal").then().statusCode(200).extract().path("id");
        UUID first = UUID.randomUUID();

        answer(id, first, 0, choices(0))
                .statusCode(200)
                .body("correction.position", is(0))
                .body("correction.correct", is(true))
                .body("correction.correctChoices", contains(0))
                .body("correction.explanation", is("Oui."))
                .body("state", is("IN_PROGRESS"))
                .body("score", nullValue())
                .body("stats", nullValue());
        // La même requête renvoyée rend la même correction ; une autre réponse à la même question est refusée.
        answer(id, first, 0, choices(0)).statusCode(200).body("correction.correct", is(true));
        answer(id, UUID.randomUUID(), 0, choices(1))
                .statusCode(409)
                .body("code", is("daily-challenge.already-answered"));
        answer(id, first, 1, choices(0)).statusCode(409).body("code", is("review.request-id-conflict"));
        answer(id, UUID.randomUUID(), 1, blanks("x")).statusCode(422).body("code", is("review.answer-mismatch"));
        // Les chiffres du jour attendent la fin du défi, pour ne pas l'influencer.
        given().get(DAILY + "/pascal")
                .then()
                .body("state", is("IN_PROGRESS"))
                .body("answers.position", contains(0))
                .body("answers[0].correct", is(true))
                .body("answers[0].explanation", is("Oui."))
                .body("stats", nullValue());

        answer(id, UUID.randomUUID(), 1, choices(1)).statusCode(200).body("correction.correct", is(false));
        answer(id, UUID.randomUUID(), 2, choices(0)).statusCode(200);
        answer(id, UUID.randomUUID(), 3, choices(0)).statusCode(200).body("state", is("IN_PROGRESS"));
        answer(id, UUID.randomUUID(), 4, blanks("int"))
                .statusCode(200)
                .body("correction.correct", is(true))
                .body("correction.blanks[0].expected", is("int"))
                .body("state", is("DONE"))
                .body("score", is(4))
                .body("questionCount", is(5))
                .body("stats.players", is(1))
                .body("stats.averageScore", is(4.0f))
                .body("stats.distribution.score", contains(0, 1, 2, 3, 4, 5))
                .body("stats.distribution.players", contains(0, 0, 0, 0, 1, 0));

        // Revenir après la fin montre le résultat, jamais un second essai.
        given().get(DAILY + "/pascal")
                .then()
                .body("id", is(id))
                .body("state", is("DONE"))
                .body("score", is(4))
                .body("answers.correct", contains(true, false, true, true, true))
                .body("answers[4].expectedBlanks", contains("int"))
                .body("stats.players", is(1));
        given().get(DAILY)
                .then()
                .statusCode(200)
                .body("find { it.subjectSlug == 'pascal' }.state", is("DONE"))
                .body("find { it.subjectSlug == 'pascal' }.score", is(4));
        // Le défi n'est pas une révision : rien dans le journal, ni dans l'XP.
        UUID me = profileId();
        assertThat(count("select count(*) from review_log where user_id = :value", me))
                .isZero();
        assertThat(count("select count(*) from points_ledger where user_id = :value", me))
                .isZero();
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "daily-yesterday"))
    void shouldFinishAfterMidnightAChallengeStartedTheDayBefore() {
        UUID yesterday = yesterdaysChallenge();
        UUID me = profileId();
        // La veille, avant minuit, une première réponse.
        QuarkusTransaction.requiringNew()
                .run(() -> entityManager
                        .createNativeQuery("""
                                insert into daily_challenge_answer
                                    (challenge_id, user_id, position, correct, answered_at, request_id)
                                values (:challenge, :user, 0, true, now(), gen_random_uuid())
                                """)
                        .setParameter("challenge", yesterday)
                        .setParameter("user", me)
                        .executeUpdate());

        given().get(DAILY + "/pascal")
                .then()
                .statusCode(200)
                .body("id", is(yesterday.toString()))
                .body("day", is(today().minusDays(1).toString()))
                .body("state", is("IN_PROGRESS"))
                .body("answers.position", contains(0));
        given().get(DAILY)
                .then()
                .body(
                        "find { it.subjectSlug == 'pascal' }.day",
                        is(today().minusDays(1).toString()))
                .body("find { it.subjectSlug == 'pascal' }.state", is("IN_PROGRESS"));
        for (int position = 1; position < 4; position++) {
            answer(yesterday.toString(), UUID.randomUUID(), position, choices(0))
                    .statusCode(200);
        }
        answer(yesterday.toString(), UUID.randomUUID(), 4, blanks("int"))
                .statusCode(200)
                .body("state", is("DONE"))
                .body("score", is(5));

        // Fini, le défi de la veille laisse la place à celui du jour.
        given().get(DAILY + "/pascal")
                .then()
                .statusCode(200)
                .body("id", not(is(yesterday.toString())))
                .body("day", is(today().toString()));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "daily-closed"))
    void shouldRefuseTheChallengeOfTheDayBeforeWhenItWasNotStarted() {
        UUID yesterday = yesterdaysChallenge();

        answer(yesterday.toString(), UUID.randomUUID(), 0, choices(0))
                .statusCode(409)
                .body("code", is("daily-challenge.closed"));
        given().get(DAILY + "/pascal").then().statusCode(200).body("day", is(today().toString()));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "daily-recent"))
    void shouldNotAskAgainTheCardsOfTheLastChallenges() {
        // Un défi passé a posé la première carte de chaque notion : celui du jour pose la seconde.
        List<UUID> asked = cardsOf("modula").stream()
                .filter(card -> card.prompt().contains("(a)"))
                .map(CardSnapshot::cardId)
                .toList();
        assertThat(asked).hasSize(5);
        insertChallenge("modula", today().minusDays(3), asked);

        List<String> prompts = given().get(DAILY + "/modula")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("questions.prompt");
        assertThat(prompts).hasSize(5).allMatch(prompt -> prompt.contains("(b)"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "daily-refusals"))
    void shouldRefuseADraftAnUnknownSubjectAndOneWithTooFewQuestions() {
        given().get(DAILY + "/ada").then().statusCode(404);
        given().get(DAILY + "/inconnu").then().statusCode(404);
        given().get(DAILY + "/logo").then().statusCode(422).body("code", is("daily-challenge.unavailable"));

        answer(UUID.randomUUID().toString(), UUID.randomUUID(), 0, choices(0))
                .statusCode(404)
                .body("code", is("daily-challenge.not-found"));
        String id =
                given().get(DAILY + "/pascal").then().statusCode(200).extract().path("id");
        answer(id, UUID.randomUUID(), 5, choices(0)).statusCode(400).body("code", is("validation.failed"));
        given().contentType(ContentType.JSON)
                .body("{\"requestId\": \"%s\", \"position\": 0}".formatted(UUID.randomUUID()))
                .post(DAILY + "/" + id + "/answers")
                .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "daily-list"))
    void shouldListThePlayableSubjectsFollowedFirstWithoutDrawingAChallenge() {
        given().contentType(ContentType.JSON)
                .body("{\"subjects\": [\"simula\"]}")
                .put("/api/v1/me/followed-subjects")
                .then()
                .statusCode(200);

        given().get(DAILY)
                .then()
                .statusCode(200)
                .body("[0].subjectSlug", is("simula"))
                .body("[0].subjectTitle", is("Simula"))
                .body("[0].day", is(today().toString()))
                .body("[0].state", is("NOT_STARTED"))
                .body("[0].score", nullValue())
                .body("[0].questionCount", is(5))
                .body("subjectSlug", hasItems("simula", "pascal", "modula"))
                .body("subjectSlug", not(hasItems("ada")))
                .body("subjectSlug", not(hasItems("logo")));
        assertThat(count("select count(*) from daily_challenge where subject_slug = :value", "simula"))
                .isZero();
    }

    /** Le défi de la veille de Pascal : quatre questions à choix, puis son code à compléter. */
    private UUID yesterdaysChallenge() {
        List<CardSnapshot> pascal = cardsOf("pascal");
        Stream<UUID> withChoices = pascal.stream()
                .filter(card -> card.type() != CardType.FILL_IN)
                .map(CardSnapshot::cardId)
                .limit(4);
        Stream<UUID> fillIn =
                pascal.stream().filter(card -> card.type() == CardType.FILL_IN).map(CardSnapshot::cardId);
        return insertChallenge(
                "pascal",
                today().minusDays(1),
                Stream.concat(withChoices, fillIn).toList());
    }

    /** Garde un défi pour ce sujet et ce jour s'il n'y en a pas encore ; rend celui qui est gardé. */
    private UUID insertChallenge(String subject, LocalDate day, List<UUID> questions) {
        return QuarkusTransaction.requiringNew().call(() -> {
            UUID id = UUID.randomUUID();
            int inserted = entityManager
                    .createNativeQuery("""
                            insert into daily_challenge (id, subject_slug, day, created_at)
                            values (:id, :subject, :day, now())
                            on conflict do nothing
                            """)
                    .setParameter("id", id)
                    .setParameter("subject", subject)
                    .setParameter("day", day)
                    .executeUpdate();
            if (inserted == 1) {
                for (int position = 0; position < questions.size(); position++) {
                    entityManager
                            .createNativeQuery("""
                                    insert into daily_challenge_question (challenge_id, position, card_id)
                                    values (:id, :position, :card)
                                    """)
                            .setParameter("id", id)
                            .setParameter("position", position)
                            .setParameter("card", questions.get(position))
                            .executeUpdate();
                }
            }
            return (UUID) entityManager
                    .createNativeQuery(
                            "select id from daily_challenge where subject_slug = :subject and day = :day", UUID.class)
                    .setParameter("subject", subject)
                    .setParameter("day", day)
                    .getSingleResult();
        });
    }

    private List<CardSnapshot> cardsOf(String subject) {
        List<UUID> ids = cards.activeCards(new CardFilter(Set.of(subject), null, Set.of(), Set.of())).stream()
                .map(CardDirectory.ActiveCard::cardId)
                .toList();
        return cards.currentVersions(ids);
    }

    private static UUID profileId() {
        return UUID.fromString(
                given().get("/api/v1/me").then().statusCode(200).extract().path("id"));
    }

    /** Le résultat d'un {@code count(*)} dont le seul paramètre est {@code :value}. */
    private long count(String sql, Object value) {
        return QuarkusTransaction.requiringNew()
                .call(() -> ((Number) entityManager
                                .createNativeQuery(sql)
                                .setParameter("value", value)
                                .getSingleResult())
                        .longValue());
    }

    private static ValidatableResponse answer(String challengeId, UUID requestId, int position, String response) {
        return given().contentType(ContentType.JSON)
                .body("{\"requestId\": \"%s\", \"position\": %d, \"answer\": %s}"
                        .formatted(requestId, position, response))
                .post(DAILY + "/" + challengeId + "/answers")
                .then();
    }

    private static String choices(int... selected) {
        return "{\"selectedChoices\": %s}".formatted(Arrays.toString(selected));
    }

    private static String blanks(String... filled) {
        return Arrays.stream(filled)
                .map(blank -> "\"" + blank + "\"")
                .collect(Collectors.joining(", ", "{\"filledBlanks\": [", "]}"));
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    /**
     * Un sujet en dossier : un fichier de référentiel par domaine, une notion par fichier. Chaque question commence par
     * le niveau de sa notion ; celle d'une notion experte est un code à compléter, les autres se répondent « Oui ».
     */
    private static SubjectFolder folder(String slug, int position, String status, List<Notion> notions) {
        Map<String, List<Notion>> byDomain = notions.stream()
                .collect(Collectors.groupingBy(Notion::domain, LinkedHashMap::new, Collectors.toList()));
        Map<String, String> referential = new LinkedHashMap<>();
        int rank = 1;
        for (var domain : byDomain.entrySet()) {
            String list = domain.getValue().stream()
                    .map(notion -> "      - {id: %s, title: \"%s\", level: %s, priority: 1}"
                            .formatted(notion.id(), notion.id(), notion.level()))
                    .collect(Collectors.joining("\n"));
            referential.put("0%d-%s.yaml".formatted(rank, domain.getKey()), """
                    domain: %s
                    title: %s
                    position: %d
                    chapters:
                      - id: %s-tout
                        title: Tout
                        notions:
                    %s
                    """.formatted(
                            domain.getKey(), domain.getKey(), rank * 10, domain.getKey(), list));
            rank++;
        }
        Map<String, String> files = notions.stream()
                .collect(Collectors.toMap(
                        notion -> "%s/%s.yaml".formatted(notion.domain(), notion.id()), DailyChallengesTest::cards));
        return new SubjectFolder(
                """
                subject: %s
                title: %s
                category: languages
                position: %d
                status: %s
                """.formatted(slug, slug.substring(0, 1).toUpperCase() + slug.substring(1), position, status),
                referential,
                files);
    }

    private static String cards(Notion notion) {
        String word = switch (notion.level()) {
            case "beginner" -> "Débutant";
            case "intermediate" -> "Intermédiaire";
            default -> "Expert";
        };
        String list = notion.keys().stream()
                .map(key -> {
                    String prompt = "%s %s (%s)".formatted(word, notion.id(), key);
                    return notion.level().equals("expert")
                            ? FILL_IN.formatted(key, prompt + " : `___ x = 1;`")
                            : COURSE.formatted(key, prompt);
                })
                .collect(Collectors.joining("\n"));
        return "notion: %s\ncards:\n%s\n".formatted(notion.id(), list);
    }

    private static final String FILL_IN =
            "  - {key: %s, format: fill-in, question: \"%s\", blanks: [[int]], answer: \"Un int.\"}";
    private static final String COURSE = "  - {key: %s, format: course, question: \"%s\", answer: \"Oui.\","
            + " choices: [{text: Oui, correct: true}, {text: Non, correct: false}]}";
}
