package com.recallio.content.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.recallio.content.application.ContentImportService;
import com.recallio.content.domain.importing.ImportReport;
import com.recallio.content.domain.source.SubjectFolder;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Import d'un dossier de sujet (ADR 0026) et retrait des thèmes disparus, de bout en bout jusqu'à l'API du catalogue.
 * Les sujets de ces tests sont des brouillons, visibles des seuls admins : ils ne gênent pas les autres tests.
 */
@QuarkusTest
@TestSecurity(user = "admin", roles = "admin")
class SubjectFolderImportTest {

    private static final String SUBJECT = """
            subject: kotlin
            title: Kotlin
            category: languages
            position: 900
            status: draft
            """;

    private static final String REFERENTIAL = """
            domain: bases
            title: Les bases
            position: 10
            chapters:
              - id: variables
                title: Variables
                notions:
                  - {id: val-var, title: "val et var", level: beginner, priority: 1}
                  - {id: null-safety, title: "La sûreté du null", level: beginner, priority: 1, requires: [val-var]}
            """;

    private static final String VAL_VAR = """
            notion: val-var
            cards:
              - key: difference
                format: course
                question: Quelle différence entre val et var ?
                answer: val ne se réaffecte pas, var si.
            """;

    @Inject
    ContentImportService importService;

    @Inject
    EntityManager entityManager;

    @Test
    void shouldImportAFolderAsOneThemePerDomain() {
        importFolder(VAL_VAR);

        given().get("/api/v1/subjects/kotlin/themes")
                .then()
                .statusCode(200)
                .body("slug", contains("kotlin-bases"))
                .body("[0].title", equalTo("Les bases"))
                .body("[0].cardCount", is(1));
    }

    @Test
    void shouldKeepAKeyedCardWhenItsQuestionIsReworded() {
        importFolder(VAL_VAR);

        ImportReport reworded = importFolder(VAL_VAR.replace("Quelle différence", "Quelle est la différence"));

        assertThat(reworded.cardsAdded()).isZero();
        assertThat(reworded.cardsUpdated()).isEqualTo(1);
        given().get("/api/v1/themes/kotlin-bases/cards")
                .then()
                .body("totalItems", is(1))
                .body("items[0].prompt", equalTo("Quelle est la différence entre val et var ?"));
    }

    @Test
    void shouldBeIdempotent() {
        importFolder(VAL_VAR);

        ImportReport again = importFolder(VAL_VAR);

        assertThat(again.cardsAdded() + again.cardsUpdated() + again.cardsRetired())
                .isZero();
    }

    @Test
    void shouldImportACodingExerciseWithItsFilesAndServeItInTheCatalog() {
        Map<String, String> exercise = Map.of(
                "bases/null-safety/longueur/exercise.yaml",
                "statement: Renvoyer la longueur, 0 pour null.\nexplanation: Avec l'opérateur Elvis.\n",
                "bases/null-safety/longueur/starter/Longueur.java",
                "class Longueur { static int de(String s) { return s.length(); } }",
                "bases/null-safety/longueur/solution/Longueur.java",
                "class Longueur { static int de(String s) { return s == null ? 0 : s.length(); } }",
                "bases/null-safety/longueur/tests/LongueurTest.java",
                "class LongueurTest {}");
        importService.importFolder(new SubjectFolder(
                SUBJECT, Map.of("01-bases.yaml", REFERENTIAL), Map.of("bases/val-var.yaml", VAL_VAR), exercise));

        ImportReport again = importService.importFolder(new SubjectFolder(
                SUBJECT, Map.of("01-bases.yaml", REFERENTIAL), Map.of("bases/val-var.yaml", VAL_VAR), exercise));

        assertThat(again.cardsAdded() + again.cardsUpdated()).isZero();
        given().get("/api/v1/themes/kotlin-bases/cards")
                .then()
                .statusCode(200)
                .body("items[1].type", equalTo("CODING_EXERCISE"))
                .body("items[1].prompt", equalTo("Renvoyer la longueur, 0 pour null."))
                .body("items[1].exercise.starter.path", contains("Longueur.java"))
                .body("items[1].exercise.solution[0].content", containsString("s == null ? 0"))
                .body("items[1].exercise.tests.path", contains("LongueurTest.java"))
                .body("items[0].exercise", nullValue());
    }

    /** La page publique affiche ces nombres (ADR 0058) : ils viennent du catalogue, pas d'un texte écrit à la main. */
    @Test
    void shouldCountTheNotionsAndTheCodingExercisesOfASubject() {
        Map<String, String> exercise = Map.of(
                "bases/null-safety/longueur/exercise.yaml",
                "statement: Renvoyer la longueur, 0 pour null.\nexplanation: Avec l'opérateur Elvis.\n",
                "bases/null-safety/longueur/starter/Longueur.java",
                "class Longueur {}",
                "bases/null-safety/longueur/solution/Longueur.java",
                "class Longueur {}",
                "bases/null-safety/longueur/tests/LongueurTest.java",
                "class LongueurTest {}");
        importService.importFolder(new SubjectFolder(
                SUBJECT, Map.of("01-bases.yaml", REFERENTIAL), Map.of("bases/val-var.yaml", VAL_VAR), exercise));

        given().get("/api/v1/subjects/kotlin")
                .then()
                .statusCode(200)
                .body("notionCount", equalTo(2))
                .body("exerciseCount", equalTo(1));
    }

    @Test
    void shouldStoreTheTourPagesOfANotionAndLeaveThemAloneWhenUnchanged() {
        String withTour = VAL_VAR.replace("cards:", """
                tour:
                  - title: Une valeur
                    text: Une valeur ne change plus.
                    example:
                      files:
                        Main.java: |-
                          void main() {
                              IO.println("val");
                          }
                      output: val
                cards:""");
        importFolder(withTour);
        Instant firstImport = notionUpdatedAt();

        importFolder(withTour);

        assertThat(notionUpdatedAt())
                .as("une notion inchangée n'est pas réécrite")
                .isEqualTo(firstImport);
        assertThat(QuarkusTransaction.requiringNew().call(() -> (String) entityManager
                        .createNativeQuery("select cast(tour as text) from notion where slug = 'val-var'")
                        .getSingleResult()))
                .contains("Une valeur", "Main.java", "IO.println");
    }

    @Test
    void shouldStopListingAThemeThatDisappearedFromItsDocument() {
        importService.importContent(rustDocument("# 1. LES BASES", "# 2. LA POSSESSION"));
        importService.importContent(rustDocument("# 2. LA POSSESSION"));

        given().get("/api/v1/subjects/rust/themes").then().statusCode(200).body("slug", contains("rust-la-possession"));
        given().get("/api/v1/themes/rust-les-bases/cards").then().statusCode(404);

        importService.importContent(rustDocument("# 1. LES BASES", "# 2. LA POSSESSION"));

        given().get("/api/v1/subjects/rust/themes")
                .then()
                .body("slug", contains("rust-les-bases", "rust-la-possession"))
                .body("[0].cardCount", is(1));
    }

    private Instant notionUpdatedAt() {
        return QuarkusTransaction.requiringNew().call(() -> (Instant) entityManager
                .createNativeQuery("select updated_at from notion where slug = 'val-var'", Instant.class)
                .getSingleResult());
    }

    private ImportReport importFolder(String valVar) {
        return importService.importFolder(
                new SubjectFolder(SUBJECT, Map.of("01-bases.yaml", REFERENTIAL), Map.of("bases/val-var.yaml", valVar)));
    }

    private static String rustDocument(String... chapters) {
        var document = new StringBuilder("""
                ---
                subject: rust
                title: Rust
                category: languages
                position: 910
                status: draft
                ---
                """);
        for (String chapter : chapters) {
            document.append(chapter)
                    .append("\n### Questions techniques\n**Q : Question de ")
                    .append(chapter.substring(2))
                    .append(" ?** R : Réponse.\n");
        }
        return document.toString();
    }
}
