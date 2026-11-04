package com.recallio.content.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.recallio.content.application.ContentImportService;
import com.recallio.content.domain.source.SubjectFolder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Le Tour d'un sujet (ADR 0037), de l'import à l'API. Le sujet de ces tests est un brouillon, visible des seuls admins :
 * il ne gêne pas les autres tests.
 */
@QuarkusTest
class TourResourceTest {

    private static final String TOUR = "/api/v1/subjects/lua/tour";

    private static final String SUBJECT = """
            subject: lua
            title: Lua
            category: languages
            position: 920
            status: draft
            """;

    private static final String REFERENTIAL = """
            domain: bases
            title: Les bases
            position: 10
            chapters:
              - id: premiers-pas
                title: Premiers pas
                notions:
                  - {id: histoire, title: "L'histoire de Lua", level: beginner, priority: 3}
                  - {id: bonjour, title: "Le premier programme", level: beginner, priority: 1}
              - id: fonctions
                title: Fonctions
                notions:
                  - {id: fonctions, title: "Déclarer une fonction", level: beginner, priority: 1, requires: [bonjour]}
            """;

    /** Le code des pages est en Go : le Tour n'accepte que les langages du bac à sable. */
    private static final String BONJOUR = """
            notion: bonjour
            tour:
              - title: Bonjour
                text: Le **premier** programme.
                example:
                  files:
                    main.go: |-
                      package main

                      func main() {}
                  output: rien
                task:
                  instructions: Fais afficher un salut.
                  starter:
                    main.go: |-
                      package main

                      func main() {}
                  solution:
                    main.go: |-
                      package main

                      import "fmt"

                      func main() { fmt.Println("Salut secret") }
                  output: Salut secret
              - text: Une seconde page, sans titre.
                example:
                  files:
                    main.go: |-
                      package main

                      func main() {}
                  output: rien
            cards:
              - {key: point-entree, format: course, question: "Où commence un programme ?", answer: Dans main.}
            """;

    private static final String FONCTIONS = """
            notion: fonctions
            tour:
              - text: Une fonction.
                example:
                  files:
                    main.go: |-
                      package main

                      func main() {}
                  output: rien
            cards:
              - {key: declarer, format: course, question: "Comment déclarer une fonction ?", answer: Avec func.}
            """;

    private static final String HISTOIRE = """
            notion: histoire
            cards:
              - {key: naissance, format: course, question: "Où est né Lua ?", answer: Au Brésil.}
            """;

    @Inject
    ContentImportService contentImport;

    @BeforeEach
    void importSubject() {
        contentImport.importFolder(new SubjectFolder(
                SUBJECT,
                Map.of("01-bases.yaml", REFERENTIAL),
                Map.of(
                        "bases/bonjour.yaml",
                        BONJOUR,
                        "bases/fonctions.yaml",
                        FONCTIONS,
                        "bases/histoire.yaml",
                        HISTOIRE)));
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void shouldListTheTourByDomainThenChapterWithoutTheNotionsOutsideIt() {
        given().get(TOUR)
                .then()
                .statusCode(200)
                .body("subjectSlug", equalTo("lua"))
                .body("subjectTitle", equalTo("Lua"))
                .body("domains.title", contains("Les bases"))
                .body("domains[0].chapters.title", contains("Premiers pas", "Fonctions"))
                .body("domains[0].chapters[0].notions.slug", contains("bonjour"))
                .body("domains[0].chapters[0].notions[0].title", equalTo("Bonjour"))
                .body("domains[0].chapters[0].notions[0].pageCount", is(2))
                .body("domains[0].chapters[1].notions[0].title", equalTo("Déclarer une fonction"));
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void shouldServeThePagesOfANotionWithoutWhatTheTaskExpects() {
        String body = given().get(TOUR + "/bonjour")
                .then()
                .statusCode(200)
                .body("title", equalTo("Le premier programme"))
                .body("chapterTitle", equalTo("Premiers pas"))
                .body("pages.title", contains("Bonjour", "Le premier programme"))
                .body("pages[0].text", equalTo("Le **premier** programme."))
                .body("pages[0].example.path", contains("main.go"))
                .body("pages[0].example[0].content", containsString("func main()"))
                .body("pages[0].task.instructions", equalTo("Fais afficher un salut."))
                .body("pages[0].task.starter.path", contains("main.go"))
                .body("pages[1].task", nullValue())
                .extract()
                .asString();

        assertThat(body)
                .as("la solution et la sortie attendue restent sur le serveur")
                .doesNotContain("Salut secret");
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void shouldNotFindANotionOutsideTheTour() {
        given().get(TOUR + "/histoire").then().statusCode(404).body("code", equalTo("content.tour-notion-not-found"));
        given().get(TOUR + "/inconnue").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "bob")
    void shouldHideTheTourOfADraftFromOtherUsers() {
        given().get(TOUR).then().statusCode(404).body("code", equalTo("content.subject-not-found"));
        given().get(TOUR + "/bonjour").then().statusCode(404);
    }
}
