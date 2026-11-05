package com.recallio.review.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.recallio.content.application.ContentImportService;
import com.recallio.content.domain.source.SubjectFolder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * S'entraîner sur une notion (ADR 0038) : toutes ses cartes, formats mêlés, et une correction qui ne laisse aucune trace
 * dans la progression. Chaque test a son propre utilisateur.
 */
@QuarkusTest
class PracticeTest {

    private static final String PRACTICE = "/api/v1/me/practice/elixir/modules";

    private static final String SUBJECT = """
            subject: elixir
            title: Elixir
            category: languages
            position: 940
            status: published
            """;

    private static final String REFERENTIAL = """
            domain: bases
            title: Les bases
            position: 10
            chapters:
              - id: premiers-pas
                title: Premiers pas
                notions:
                  - {id: modules, title: "Les modules", level: beginner, priority: 1}
                  - {id: processus, title: "Les processus", level: beginner, priority: 1}
            """;

    /** Dans l'ordre du fichier : deux cours, deux vrai ou faux (« Vrai », puis « Faux »), un QCM, un code à trous. */
    private static final String MODULES = """
            notion: modules
            cards:
              - {key: cours-1, format: course, question: "Comment définir un module ?", answer: "Avec defmodule."}
              - {key: cours-2, format: course, question: "Comment appeler une fonction ?", answer: "Module.fonction()."}
              - {key: vrai-faux-1, format: true-false, question: "defmodule définit un module.", truth: true, answer: "Oui."}
              - {key: vrai-faux-2, format: true-false, question: "class définit un module.", truth: false, answer: "Non."}
              - key: choix
                format: multiple-choice
                question: "Quel mot-clé définit une fonction publique ?"
                choices:
                  - {text: "`def`", correct: true}
                  - {text: "`defp`", correct: false}
                answer: "def ; defp la rend privée."
              - key: trou
                format: fill-in
                question: "`___ Bonjour do`"
                blanks: [[defmodule]]
                answer: "defmodule ouvre un module."
            """;

    private static final String PROCESSUS = """
            notion: processus
            cards:
              - {key: vrai-faux, format: true-false, question: "spawn crée un processus.", truth: true, answer: "Oui."}
            """;

    @Inject
    ContentImportService contentImport;

    @BeforeEach
    void importSubject() {
        contentImport.importFolder(new SubjectFolder(
                SUBJECT,
                Map.of("01-bases.yaml", REFERENTIAL),
                Map.of("bases/modules.yaml", MODULES, "bases/processus.yaml", PROCESSUS)));
    }

    @Test
    void shouldRequireAuthentication() {
        given().get(PRACTICE).then().statusCode(401);
        check(choice(UUID.randomUUID().toString(), 0)).statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "practice-cards"))
    void shouldGiveEveryCardOfTheNotionWithTheFormatsMixed() {
        given().get(PRACTICE)
                .then()
                .statusCode(200)
                .body("notionTitle", equalTo("Les modules"))
                .body(
                        "cards.type",
                        contains("FLASHCARD", "TRUE_FALSE", "MULTIPLE_CHOICE", "FILL_IN", "FLASHCARD", "TRUE_FALSE"))
                .body("cards[0].answer", equalTo("Avec defmodule."))
                .body("cards[1].answer", nullValue())
                .body("cards[1].choices", contains("Vrai", "Faux"))
                .body("cards[2].multipleAnswers", is(false))
                .body("cards[3].blankCount", is(1));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "practice-check"))
    void shouldCorrectAnAnswerWithoutRecordingAnything() {
        List<String> ids = cardIds(PRACTICE);

        check(choice(ids.get(1), 1))
                .statusCode(200)
                .body("correct", is(false))
                .body("correctChoices", contains(0))
                .body("explanation", equalTo("Oui."));
        check(Map.of("cardId", ids.get(3), "filledBlanks", List.of("defmodule")))
                .statusCode(200)
                .body("correct", is(true))
                .body("blanks[0].expected", equalTo("defmodule"));

        given().get("/api/v1/me/progress")
                .then()
                .statusCode(200)
                .body("cardsToday", is(0))
                .body("totalXp", is(0));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "practice-refusals"))
    void shouldRefuseWhatItCannotCorrect() {
        String course = cardIds(PRACTICE).getFirst();
        String otherNotion = cardIds("/api/v1/me/practice/elixir/processus").getFirst();

        check(choice(course, 0)).statusCode(422).body("code", equalTo("review.answer-mismatch"));
        check(choice(otherNotion, 0)).statusCode(404).body("code", equalTo("review.card-not-found"));
        check(choice(UUID.randomUUID().toString(), 0)).statusCode(404);
        given().get("/api/v1/me/practice/elixir/inconnue")
                .then()
                .statusCode(404)
                .body("code", equalTo("review.notion-not-found"));
    }

    private static List<String> cardIds(String practice) {
        return given().get(practice).then().statusCode(200).extract().path("cards.cardId");
    }

    private static Map<String, Object> choice(String cardId, int index) {
        return Map.of("cardId", cardId, "selectedChoices", List.of(index));
    }

    private static ValidatableResponse check(Map<String, Object> answer) {
        return given().contentType(ContentType.JSON)
                .body(answer)
                .post(PRACTICE + "/checks")
                .then();
    }
}
