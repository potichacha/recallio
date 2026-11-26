package com.recallio.review.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardDirectory.LeveledCard;
import com.recallio.content.CardType;
import com.recallio.content.SubjectScope;
import com.recallio.content.application.ContentImportService;
import com.recallio.content.domain.source.SubjectFolder;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * La révision intensive (ADR 0078). Eiffel et Oberon sont publiés, Rexx est un brouillon. Chaque carte à choix se répond
 * « Oui » (le premier choix). Les profils des tests sont en UTC sauf mention, et l'horloge des tests est à midi du jour.
 */
@QuarkusTest
class DrillTest {

    private static final String QUESTIONS = "/api/v1/me/drill/questions";
    private static final String CHECKS = "/api/v1/me/drill/checks";
    private static final String USAGE = "/api/v1/me/drill/usage";

    private static final String SEED = "7";

    /** Une notion d'un sujet de test : son niveau, sa priorité, et le nombre de ses cartes à choix. */
    private record Notion(String id, String level, int priority, int cards) {}

    /**
     * Un junior y trouve j1 (deux cartes) et j2 ; un confirmé j2, c1 et e1 ; un senior j2, c1, e1 et s1. Personne n'y
     * trouve b3, débutante mais avancée. La notion j1 a aussi un code à trous, qui n'a pas de choix.
     */
    private static final List<Notion> EIFFEL = List.of(
            new Notion("j1", "beginner", 1, 2),
            new Notion("j2", "intermediate", 2, 1),
            new Notion("c1", "intermediate", 3, 1),
            new Notion("e1", "expert", 1, 1),
            new Notion("s1", "expert", 4, 1),
            new Notion("b3", "beginner", 3, 1));

    private static final List<Notion> OBERON = List.of(new Notion("o1", "beginner", 1, 1));

    private static final List<Notion> REXX = List.of(new Notion("r1", "beginner", 1, 1));

    private static final String CHOICE_CARD = """
              - {key: %s, format: course, question: "%s", answer: "Parce que.", choices: [{text: Oui, correct: true}, {text: Non, correct: false}]}
            """;

    private static final String FILL_IN_CARD = """
              - {key: trou, format: fill-in, question: "`___ x = 1;`", blanks: [[int]], answer: "Un int."}
            """;

    @Inject
    ContentImportService contentImport;

    @Inject
    CardDirectory cards;

    @Inject
    EntityManager entityManager;

    @BeforeEach
    void importSubjects() {
        contentImport.importFolder(folder("eiffel", 1110, "published", EIFFEL, "j1"));
        contentImport.importFolder(folder("oberon", 1120, "published", OBERON, ""));
        contentImport.importFolder(folder("rexx", 1130, "draft", REXX, ""));
    }

    @Test
    void shouldRequireAuthentication() {
        given().get(QUESTIONS + "?subjects=eiffel&level=JUNIOR&seed=1").then().statusCode(401);
        check(choice(UUID.randomUUID().toString(), 0)).statusCode(401);
        given().get(USAGE).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "drill-questions"))
    void shouldDrawQuestionsOfSeveralSubjectsWithoutTheirAnswers() {
        JsonPath page = questions("JUNIOR", 0, "eiffel", "oberon")
                .statusCode(200)
                .body("total", is(4))
                .body("questions.type", everyItem(equalTo("FLASHCARD")))
                .body("questions.choices", everyItem(contains("Oui", "Non")))
                .body("questions.multipleAnswers", everyItem(is(false)))
                .body("questions.blankCount", everyItem(is(0)))
                .body("questions.exercise", everyItem(nullValue()))
                .extract()
                .jsonPath();

        // Les deux cartes de j1 ne tiennent pas sur la même page : trois questions, de trois notions.
        assertThat(page.getList("questions.notionSlug", String.class)).containsExactlyInAnyOrder("j1", "j2", "o1");
        assertThat(page.getLong("nextOffset")).isEqualTo(3);
        assertThat(page.getString("questions.find { it.notionSlug == 'o1' }.subjectSlug"))
                .isEqualTo("oberon");
        assertThat(page.getString("questions.find { it.notionSlug == 'o1' }.subjectTitle"))
                .isEqualTo("Oberon");
        assertThat(page.getString("questions.find { it.notionSlug == 'j2' }.notionTitle"))
                .isEqualTo("Notion j2");
        // Ni les bonnes réponses, ni les explications avant d'avoir répondu.
        assertThat(page.prettify()).doesNotContain("correct", "Parce que.");

        // La même graine redonne la même page ; la page suivante pose la carte qui restait.
        assertThat(questions("JUNIOR", 0, "eiffel", "oberon")
                        .extract()
                        .jsonPath()
                        .getList("questions.cardId"))
                .isEqualTo(page.getList("questions.cardId"));
        JsonPath next = questions("JUNIOR", 3, "eiffel", "oberon")
                .statusCode(200)
                .body("questions.notionSlug", contains("j1"))
                .body("nextOffset", is(4))
                .extract()
                .jsonPath();
        assertThat(page.getList("questions.cardId")).doesNotContainAnyElementsOf(next.getList("questions.cardId"));

        // Le tour fini, un autre commence : la révision n'a pas de fin.
        questions("JUNIOR", 4, "eiffel", "oberon")
                .statusCode(200)
                .body("questions", hasSize(3))
                .body("nextOffset", is(7));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "drill-levels"))
    void shouldChooseTheNotionsFromTheTargetedLevel() {
        assertThat(notions("JUNIOR")).containsExactlyInAnyOrder("j1", "j1", "j2");
        assertThat(notions("CONFIRMED")).containsExactlyInAnyOrder("j2", "c1", "e1");
        assertThat(notions("SENIOR")).containsExactlyInAnyOrder("j2", "c1", "e1", "s1");
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "drill-bounds"))
    void shouldRefuseParametersOutOfBounds() {
        String valid = QUESTIONS + "?subjects=eiffel&level=JUNIOR&seed=1";

        invalid("?level=JUNIOR&seed=1", "subjects");
        invalid("?subjects=a&subjects=b&subjects=c&subjects=d&subjects=e&subjects=f&level=JUNIOR&seed=1", "subjects");
        invalid("?subjects=eiffel&seed=1", "level");
        invalid("?subjects=eiffel&level=JUNIOR", "seed");
        invalid(valid + "&offset=-1", "offset");
        invalid(valid + "&offset=" + (DrillResource.MAX_OFFSET + 1), "offset");
        invalid(valid + "&size=0", "size");
        invalid(valid + "&size=" + (DrillResource.MAX_PAGE_SIZE + 1), "size");

        given().get(valid + "&size=" + DrillResource.MAX_PAGE_SIZE).then().statusCode(200);
        given().get(QUESTIONS + "?subjects=eiffel&subjects=eiffel&level=JUNIOR&seed=1")
                .then()
                .statusCode(200)
                .body("total", is(3));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "drill-hidden"))
    void shouldHideADraftSubjectFromSomeoneWhoIsNotAnAdmin() {
        questions("JUNIOR", 0, "eiffel", "rexx").statusCode(404).body("code", equalTo("content.subject-not-found"));
        questions("JUNIOR", 0, "inconnu").statusCode(404).body("code", equalTo("content.subject-not-found"));

        check(choice(draftCard().toString(), 0)).statusCode(404).body("code", equalTo("review.card-not-found"));
        given().get(USAGE).then().statusCode(200).body("answers", is(0));
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    @OidcSecurity(claims = @Claim(key = "sub", value = "drill-admin"))
    void shouldLetAnAdminDrillOnADraftSubject() {
        String cardId = questions("JUNIOR", 0, "rexx")
                .statusCode(200)
                .body("total", is(1))
                .body("questions.subjectSlug", contains("rexx"))
                .extract()
                .path("questions[0].cardId");

        check(choice(cardId, 0)).statusCode(200).body("correct", is(true)).body("subjectSlug", equalTo("rexx"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "drill-check"))
    void shouldCorrectAnAnswerAndCountItWithoutRecordingAnythingElse() {
        UUID alice = profileId();
        JsonPath page = questions("SENIOR", 0, "eiffel").extract().jsonPath();
        String first = page.getString("questions[0].cardId");
        String second = page.getString("questions[1].cardId");

        given().get(USAGE)
                .then()
                .statusCode(200)
                .body("day", equalTo(LocalDate.now(ZoneOffset.UTC).toString()))
                .body("answers", is(0))
                .body("correct", is(0));

        check(choice(first, 0))
                .statusCode(200)
                .body("cardId", equalTo(first))
                .body("correct", is(true))
                .body("correctChoices", contains(0))
                .body("blanks", empty())
                .body("explanation", equalTo("Parce que."))
                .body("subjectSlug", equalTo("eiffel"))
                .body("subjectTitle", equalTo("Eiffel"))
                .body("notionSlug", equalTo(page.getString("questions[0].notionSlug")))
                .body("notionTitle", equalTo(page.getString("questions[0].notionTitle")))
                .body("answersToday", is(1));
        check(choice(second, 1))
                .statusCode(200)
                .body("correct", is(false))
                .body("correctChoices", contains(0))
                .body("answersToday", is(2));

        given().get(USAGE).then().statusCode(200).body("answers", is(2)).body("correct", is(1));

        // Ni le journal, ni FSRS, ni l'XP, ni la série : seul le compteur du jour a bougé.
        for (String table : List.of("review_log", "review_state", "points_ledger", "daily_activity")) {
            assertThat(count(table, alice)).as(table).isZero();
        }
        assertThat(count("drill_activity", alice)).isEqualTo(1);
        given().get("/api/v1/me/progress")
                .then()
                .statusCode(200)
                .body("cardsToday", is(0))
                .body("totalXp", is(0));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "drill-refusals"))
    void shouldRefuseWhatIsNotADrillQuestionWithoutCountingIt() {
        String question = questions("JUNIOR", 0, "eiffel").extract().path("questions[0].cardId");

        // Un code à trous n'a pas de choix : la révision intensive ne le pose pas, et ne le corrige pas.
        check(Map.of("cardId", fillInCard().toString(), "filledBlanks", List.of("int")))
                .statusCode(404)
                .body("code", equalTo("review.card-not-found"));
        check(choice(UUID.randomUUID().toString(), 0)).statusCode(404);
        check(Map.of("cardId", question, "filledBlanks", List.of("int")))
                .statusCode(422)
                .body("code", equalTo("review.answer-mismatch"));
        check(Map.of("selectedChoices", List.of(0))).statusCode(400).body("code", equalTo("validation.failed"));

        given().get(USAGE).then().statusCode(200).body("answers", is(0));
    }

    /** À midi UTC, il est déjà demain à Kiritimati (UTC+14) : le jour compté est celui de l'utilisatrice. */
    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "drill-timezone"))
    void shouldCountTheAnswerOnTheDayOfTheUserInTheirTimezone() {
        given().header("X-Timezone", "Pacific/Kiritimati")
                .get("/api/v1/me")
                .then()
                .statusCode(200)
                .body("timezone", equalTo("Pacific/Kiritimati"));
        String question = questions("JUNIOR", 0, "eiffel").extract().path("questions[0].cardId");
        String tomorrow = LocalDate.now(ZoneOffset.UTC).plusDays(1).toString();

        check(choice(question, 0)).statusCode(200);

        given().get(USAGE).then().statusCode(200).body("day", equalTo(tomorrow)).body("answers", is(1));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "drill-account"))
    void shouldExportTheCounterAndEraseItWithTheAccount() {
        UUID alice = profileId();
        String question = questions("JUNIOR", 0, "eiffel").extract().path("questions[0].cardId");
        check(choice(question, 0)).statusCode(200);

        given().get("/api/v1/me/account/export")
                .then()
                .statusCode(200)
                .body("sections.review.drill_activity", hasSize(1))
                .body("sections.review.drill_activity[0].answers", is(1))
                .body("sections.review.drill_activity[0].correct", is(1));

        given().delete("/api/v1/me/account").then().statusCode(204);

        assertThat(count("drill_activity", alice)).isZero();
    }

    private static ValidatableResponse questions(String level, long offset, String... subjects) {
        RequestSpecification request =
                given().queryParam("level", level).queryParam("seed", SEED).queryParam("offset", offset);
        return request.queryParam("subjects", (Object[]) subjects)
                .get(QUESTIONS)
                .then();
    }

    /** Les notions de toutes les cartes d'Eiffel posées à ce poste, sur un tour entier. */
    private static List<String> notions(String level) {
        List<String> notions = new ArrayList<>();
        long offset = 0;
        int total;
        do {
            JsonPath page =
                    questions(level, offset, "eiffel").statusCode(200).extract().jsonPath();
            notions.addAll(page.getList("questions.notionSlug", String.class));
            offset = page.getLong("nextOffset");
            total = page.getInt("total");
        } while (offset < total);
        return notions;
    }

    private static void invalid(String query, String parameter) {
        String url = query.startsWith(QUESTIONS) ? query : QUESTIONS + query;
        given().get(url)
                .then()
                .statusCode(400)
                .body("code", equalTo("validation.failed"))
                .body("invalidParams.name", contains(parameter));
    }

    private static Map<String, Object> choice(String cardId, int index) {
        return Map.of("cardId", cardId, "selectedChoices", List.of(index));
    }

    private static ValidatableResponse check(Map<String, Object> answer) {
        return given().contentType(ContentType.JSON).body(answer).post(CHECKS).then();
    }

    private static UUID profileId() {
        return UUID.fromString(
                given().get("/api/v1/me").then().statusCode(200).extract().path("id"));
    }

    private UUID draftCard() {
        return cards.choiceCards(Set.of("rexx"), SubjectScope.WITH_DRAFTS).stream()
                .map(LeveledCard::cardId)
                .findFirst()
                .orElseThrow();
    }

    private UUID fillInCard() {
        return cards.activeCards(new CardFilter(Set.of("eiffel"), null, Set.of(), Set.of(CardType.FILL_IN))).stream()
                .map(CardDirectory.ActiveCard::cardId)
                .findFirst()
                .orElseThrow();
    }

    private long count(String table, UUID userId) {
        return QuarkusTransaction.requiringNew()
                .call(() -> ((Number) entityManager
                                .createNativeQuery("select count(*) from " + table + " where user_id = :id")
                                .setParameter("id", userId)
                                .getSingleResult())
                        .longValue());
    }

    /** @param withFillIn la notion qui reçoit en plus un code à trous ; aucune si vide */
    private static SubjectFolder folder(
            String slug, int position, String status, List<Notion> notions, String withFillIn) {
        String list = notions.stream()
                .map(notion -> "      - {id: %s, title: \"Notion %s\", level: %s, priority: %d}"
                        .formatted(notion.id(), notion.id(), notion.level(), notion.priority()))
                .collect(Collectors.joining("\n"));
        String referential = """
                domain: bases
                title: Les bases
                position: 10
                chapters:
                  - id: tout
                    title: Tout
                    notions:
                %s
                """.formatted(list);
        Map<String, String> files = notions.stream()
                .collect(Collectors.toMap(
                        notion -> "bases/%s.yaml".formatted(notion.id()),
                        notion -> cards(notion, notion.id().equals(withFillIn))));
        return new SubjectFolder(
                """
                subject: %s
                title: %s
                category: languages
                position: %d
                status: %s
                """.formatted(slug, slug.substring(0, 1).toUpperCase() + slug.substring(1), position, status),
                Map.of("01-bases.yaml", referential),
                files);
    }

    private static String cards(Notion notion, boolean withFillIn) {
        StringBuilder list = new StringBuilder();
        for (int number = 1; number <= notion.cards(); number++) {
            list.append(CHOICE_CARD.formatted("c" + number, "Question %d de %s ?".formatted(number, notion.id())));
        }
        if (withFillIn) {
            list.append(FILL_IN_CARD);
        }
        return "notion: %s\ncards:\n%s".formatted(notion.id(), list);
    }
}
