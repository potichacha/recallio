package com.recallio.review.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.ActiveCard;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardDirectory.CardSnapshot;
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
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Le test de niveau (ADR 0068). Fortran a deux domaines des bases : « bases » (variables, dans le Tour, puis types et
 * boucles, qui la requièrent, sur deux chapitres ; et la récursion, intermédiaire, qui requiert les boucles) et « outils »
 * (le compilateur, une seule notion débutante : une seule question). Chaque carte se répond « Oui », le premier choix.
 * Chaque test a son propre utilisateur.
 */
@QuarkusTest
class LevelChecksTest {

    private static final String CHECKS = "/api/v1/me/level-checks";
    private static final String FORTRAN = CHECKS + "/fortran";
    private static final String NOTIONS = "domains.chapters.notions.flatten()";

    private static final String YES_OR_NO = "choices: [{text: Oui, correct: true}, {text: Non, correct: false}]";

    private static final String BASES = """
            domain: bases
            title: Les bases
            position: 10
            chapters:
              - id: syntaxe
                title: Syntaxe
                notions:
                  - {id: variables, title: "Variables", level: beginner, priority: 1}
                  - {id: types, title: "Types", level: beginner, priority: 1, requires: [variables]}
              - id: controle
                title: Contrôle
                notions:
                  - {id: boucles, title: "Boucles", level: beginner, priority: 1, requires: [variables]}
                  - {id: recursion, title: "Récursion", level: intermediate, priority: 1, requires: [boucles]}
            """;

    private static final String OUTILS = """
            domain: outils
            title: Les outils
            position: 20
            chapters:
              - id: build
                title: Build
                notions:
                  - {id: compilateur, title: "Compilateur", level: beginner, priority: 1}
            """;

    /** Seulement des notions intermédiaires : pas de bases à tester. */
    private static final String AVANCE = """
            domain: avance
            title: L'avancé
            position: 10
            chapters:
              - id: tout
                title: Tout
                notions:
                  - {id: macros, title: "Macros", level: intermediate, priority: 1}
            """;

    private static final String TOUR_PAGE = """
            tour:
              - text: Une page.
                example:
                  files:
                    main.go: |-
                      package main

                      func main() {}
                  output: rien
            """;

    @Inject
    ContentImportService contentImport;

    @Inject
    CardDirectory cards;

    @Inject
    EntityManager entityManager;

    @BeforeEach
    void importSubjects() {
        contentImport.importFolder(folder("fortran", 1110, "published"));
        contentImport.importFolder(folder("cobol", 1120, "draft"));
        contentImport.importFolder(new SubjectFolder(
                header("lisp", 1130, "published"),
                Map.of("01-avance.yaml", AVANCE),
                Map.of("avance/macros.yaml", notion("macros", false))));
    }

    @Test
    void shouldRequireAuthentication() {
        given().get(FORTRAN).then().statusCode(401);
        check("bases", List.of(answer(UUID.randomUUID(), 0))).statusCode(401);
        given().delete(FORTRAN).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "level-check-questions"))
    void shouldGiveTwoQuestionsOfDifferentNotionsPerDomainOfTheBasicsWithoutTheirAnswers() {
        JsonPath test = given().get(FORTRAN)
                .then()
                .statusCode(200)
                .body("subjectSlug", is("fortran"))
                .body("subjectTitle", is("Fortran"))
                .body("knownNotions", is(0))
                .body("domains.id", contains("bases", "outils"))
                .body("domains.title", contains("Les bases", "Les outils"))
                .body("domains.beginnerNotions", contains(3, 1))
                .body("domains.known", contains(false, false))
                .body("domains[0].questions", hasSize(2))
                .body("domains[1].questions", hasSize(1))
                .body("domains[1].questions[0].notionSlug", is("compilateur"))
                .body("domains[0].questions[0].choices", contains("Oui", "Non"))
                .extract()
                .jsonPath();

        List<String> notions = test.getList("domains[0].questions.notionSlug");
        assertThat(notions).doesNotHaveDuplicates().isSubsetOf("variables", "types", "boucles");
        // Ni les bonnes réponses, ni les explications.
        assertThat(test.prettify()).doesNotContain("correct", "Oui.");
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "level-check-passed"))
    void shouldMakeTheBeginnerNotionsOfAPassedDomainKnownAndNotThoseOfAFailedOne() {
        check("bases", answers("bases", 0))
                .statusCode(200)
                .body("domain", is("bases"))
                .body("passed", is(true))
                .body("corrections.correct", contains(true, true))
                .body("corrections[0].correctChoices", contains(0))
                .body("corrections[0].explanation", is("Oui."))
                .body("knownNotions", is(3));
        check("outils", answers("outils", 1))
                .statusCode(200)
                .body("passed", is(false))
                .body("corrections.correct", contains(false))
                .body("knownNotions", is(3));

        given().get(FORTRAN).then().body("knownNotions", is(3)).body("domains.known", contains(true, false));
        // Le sommaire du Tour marque les notions connues, sans les valider.
        given().get("/api/v1/me/tour/fortran")
                .then()
                .statusCode(200)
                .body("knownNotions", contains("variables", "types", "boucles"))
                .body("validatedNotions", empty());
        // Le test n'est pas une révision : rien dans le journal, ni dans l'XP.
        UUID me = profileId();
        assertThat(count("select count(*) from review_log where user_id = :id", me))
                .isZero();
        assertThat(count("select count(*) from points_ledger where user_id = :id", me))
                .isZero();
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "level-check-again"))
    void shouldKeepTheKnownNotionsWhenTheTestIsTakenAgain() {
        check("bases", answers("bases", 0)).statusCode(200).body("knownNotions", is(3));

        check("bases", answers("bases", 1))
                .statusCode(200)
                .body("passed", is(false))
                .body("knownNotions", is(3));
        check("bases", answers("bases", 0))
                .statusCode(200)
                .body("passed", is(true))
                .body("knownNotions", is(3));
        given().get(FORTRAN).then().body("domains[0].known", is(true));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "level-check-cancel"))
    void shouldForgetEveryKnownNotionOfTheSubjectWhenTheTestIsCancelled() {
        check("bases", answers("bases", 0)).statusCode(200);
        check("outils", answers("outils", 0)).statusCode(200).body("knownNotions", is(4));

        given().delete(FORTRAN).then().statusCode(204);

        given().get(FORTRAN).then().body("knownNotions", is(0)).body("domains.known", contains(false, false));
        given().get("/api/v1/me/tour/fortran").then().body("knownNotions", empty());
        // Annuler un test jamais passé ne change rien.
        given().delete(CHECKS + "/lisp").then().statusCode(204);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "level-check-refusals"))
    void shouldRefuseCardsThatAreNotADrawOfTheDomain() {
        Map<String, List<UUID>> byNotion = cardsByNotion();
        List<UUID> variables = cardsOf(byNotion, "variables");
        UUID variable = variables.getFirst();
        UUID compiler = cardsOf(byNotion, "compilateur").getFirst();

        // Une carte d'un autre domaine, deux cartes d'une même notion, une réponse qui manque.
        invalid("bases", List.of(answer(variable, 0), answer(compiler, 0)));
        invalid("bases", List.of(answer(variable, 0), answer(variables.getLast(), 0)));
        invalid("bases", List.of(answer(variable, 0)));
        // Une notion intermédiaire n'est pas une question du test.
        invalid(
                "bases",
                List.of(
                        answer(variable, 0),
                        answer(cardsOf(byNotion, "recursion").getFirst(), 0)));

        check("inconnu", List.of(answer(variable, 0))).statusCode(404).body("code", is("level-check.domain-not-found"));
        given().contentType(ContentType.JSON)
                .body(Map.of(
                        "answers", List.of(Map.of("cardId", compiler, "answer", Map.of("filledBlanks", List.of("x"))))))
                .post(FORTRAN + "/domains/outils")
                .then()
                .statusCode(422)
                .body("code", is("review.answer-mismatch"));
        check("outils", List.of()).statusCode(400).body("code", is("validation.failed"));
        given().get(FORTRAN).then().body("knownNotions", is(0));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "level-check-unavailable"))
    void shouldRefuseADraftAnUnknownSubjectAndOneWithoutBasics() {
        given().get(CHECKS + "/cobol").then().statusCode(404);
        given().get(CHECKS + "/inconnu").then().statusCode(404);
        given().get(CHECKS + "/lisp").then().statusCode(422).body("code", is("level-check.unavailable"));
    }

    /**
     * En parcours « apprendre », une notion connue n'attend plus le Tour et ouvre celles qui la requièrent (ADR 0059,
     * 0032) ; ses cartes passent après les autres. Elle n'est pas acquise : l'indice de préparation ne bouge pas.
     */
    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "level-check-learning"))
    void shouldOpenTheKnownNotionsToLearnAndServeTheirCardsAfterTheOthers() {
        followAndLearn();
        // Les variables attendent le Tour, et tout ce qui les requiert attend les variables.
        queue().body("cards.notionSlug", everyItem(equalTo("compilateur")));
        given().get("/api/v1/learning-paths/fortran")
                .then()
                .statusCode(200)
                .body(NOTIONS + ".status", contains("TOUR_FIRST", "LOCKED", "LOCKED", "LOCKED", "AVAILABLE"));

        check("bases", answers("bases", 0)).statusCode(200).body("passed", is(true));

        List<String> served = queue().extract().path("cards.notionSlug");
        // Trois nouvelles cartes par jour en test : les deux notions inconnues d'abord, puis une connue.
        assertThat(served).hasSize(3);
        assertThat(served.subList(0, 2)).containsExactly("compilateur", "recursion");
        assertThat(served.get(2)).isIn("variables", "types", "boucles");
        given().get("/api/v1/learning-paths/fortran")
                .then()
                .body(NOTIONS + ".status", contains("AVAILABLE", "AVAILABLE", "AVAILABLE", "AVAILABLE", "AVAILABLE"))
                .body(NOTIONS + ".mastery", everyItem(equalTo("NEW")));
        given().get("/api/v1/me/review-stats")
                .then()
                .statusCode(200)
                .body("subjects.find { it.slug == 'fortran' }.notions.acquired", is(0))
                .body("subjects.find { it.slug == 'fortran' }.readiness.acquired", is(0))
                .body("subjects.find { it.slug == 'fortran' }.readiness.percent", is(0));
    }

    /** Qui prépare un entretien a toutes les notions ouvertes : les connues passent après celles de même priorité. */
    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "level-check-prepare"))
    void shouldServeTheKnownNotionsAfterTheOthersOfTheSamePriorityToPrepareAnInterview() {
        follow();
        check("bases", answers("bases", 0)).statusCode(200);

        List<String> served = queue().extract().path("cards.notionSlug");

        // Toutes essentielles : les deux notions inconnues d'abord, puis une connue.
        assertThat(served).hasSize(3);
        assertThat(served.subList(0, 2)).containsExactlyInAnyOrder("recursion", "compilateur");
        assertThat(served.get(2)).isIn("variables", "types", "boucles");
    }

    private static ValidatableResponse queue() {
        return given().queryParam("subject", "fortran")
                .get("/api/v1/review-queue")
                .then()
                .statusCode(200);
    }

    private static void follow() {
        given().contentType(ContentType.JSON)
                .body(Map.of("subjects", List.of("fortran")))
                .put("/api/v1/me/followed-subjects")
                .then()
                .statusCode(200);
    }

    private static void followAndLearn() {
        follow();
        given().contentType(ContentType.JSON)
                .body(Map.of("track", "LEARN"))
                .put("/api/v1/me/followed-subjects/fortran/track")
                .then()
                .statusCode(200);
    }

    /** Une réponse à chaque question d'un domaine, tirée par le serveur : le même choix pour toutes. */
    private static List<Map<String, Object>> answers(String domain, int choice) {
        List<String> ids = given().get(FORTRAN)
                .then()
                .statusCode(200)
                .extract()
                .path("domains.find { it.id == '%s' }.questions.cardId".formatted(domain));
        return ids.stream().map(id -> answer(UUID.fromString(id), choice)).toList();
    }

    private static Map<String, Object> answer(UUID cardId, int choice) {
        return Map.of("cardId", cardId, "answer", Map.of("selectedChoices", List.of(choice)));
    }

    private static ValidatableResponse check(String domain, List<Map<String, Object>> answers) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("answers", answers))
                .post(FORTRAN + "/domains/" + domain)
                .then();
    }

    private static void invalid(String domain, List<Map<String, Object>> answers) {
        check(domain, answers).statusCode(422).body("code", is("level-check.invalid-answers"));
    }

    /** Les cartes de Fortran, par notion, dans l'ordre du programme. */
    private Map<String, List<UUID>> cardsByNotion() {
        List<UUID> ids = cards.activeCards(new CardFilter(Set.of("fortran"), null, Set.of(), Set.of())).stream()
                .map(ActiveCard::cardId)
                .toList();
        return cards.currentVersions(ids).stream()
                .collect(Collectors.groupingBy(
                        card -> Objects.requireNonNull(card.notionSlug()),
                        LinkedHashMap::new,
                        Collectors.mapping(CardSnapshot::cardId, Collectors.toList())));
    }

    private static List<UUID> cardsOf(Map<String, List<UUID>> byNotion, String notion) {
        return Objects.requireNonNull(byNotion.get(notion), notion);
    }

    private static UUID profileId() {
        return UUID.fromString(
                given().get("/api/v1/me").then().statusCode(200).extract().path("id"));
    }

    private long count(String sql, UUID id) {
        return QuarkusTransaction.requiringNew()
                .call(() -> ((Number) entityManager
                                .createNativeQuery(sql)
                                .setParameter("id", id)
                                .getSingleResult())
                        .longValue());
    }

    private static SubjectFolder folder(String slug, int position, String status) {
        return new SubjectFolder(
                header(slug, position, status),
                Map.of("01-bases.yaml", BASES, "02-outils.yaml", OUTILS),
                Map.of(
                        "bases/variables.yaml", notion("variables", true),
                        "bases/types.yaml", notion("types", false),
                        "bases/boucles.yaml", notion("boucles", false),
                        "bases/recursion.yaml", notion("recursion", false),
                        "outils/compilateur.yaml", notion("compilateur", false)));
    }

    private static String header(String slug, int position, String status) {
        return """
                subject: %s
                title: %s
                category: languages
                position: %d
                status: %s
                """.formatted(slug, slug.substring(0, 1).toUpperCase() + slug.substring(1), position, status);
    }

    /** Deux cartes à choix par notion, qui se répondent « Oui » ; une page du Tour si la notion y est. */
    private static String notion(String id, boolean inTour) {
        String cardList = List.of("a", "b").stream()
                .map(key -> "  - {key: %s, format: course, question: \"%s (%s) ?\", answer: \"Oui.\", %s}"
                        .formatted(key, id, key, YES_OR_NO))
                .collect(Collectors.joining("\n"));
        return "notion: %s\n%scards:\n%s\n".formatted(id, inTour ? TOUR_PAGE : "", cardList);
    }
}
