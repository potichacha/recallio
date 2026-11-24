package com.recallio.content.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import com.recallio.content.application.ContentImportService;
import com.recallio.content.domain.source.SubjectFolder;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Le plan du site (ADR 0065), sans compte : les pages publiques des sujets publiés, à l'origine publique du site. Les
 * tests Quarkus partagent leur base : d'autres sujets publiés peuvent y figurer, jamais un brouillon.
 */
@QuarkusTest
class SitemapResourceTest {

    private static final String SITEMAP = "/api/v1/sitemap.xml";

    /** L'origine de {@code %test}, sans la barre finale que lui donne application.properties. */
    private static final String ORIGIN = "https://recallio.test";

    private static final String REFERENTIAL = """
            domain: bases
            title: Les bases
            position: 10
            chapters:
              - id: premiers-pas
                title: Premiers pas
                notions:
                  - {id: bonjour, title: "Le premier programme", level: beginner, priority: 1}
                  - {id: fonctions, title: "Les fonctions", level: beginner, priority: 1}
                  - {id: histoire, title: "L'histoire du langage", level: beginner, priority: 3}
            """;

    /** Une leçon et une page du Tour, avec les deux questions qui valident la notion (ADR 0037). */
    private static final String BONJOUR = """
            notion: bonjour
            lesson: Un programme commence par **main**.
            tour:
              - text: Le premier programme.
                example:
                  files:
                    main.go: |-
                      package main

                      func main() {}
                  output: rien
            cards:
              - {key: vrai-faux, format: true-false, question: "main prend des paramètres.", truth: false, answer: "Non."}
              - key: cours
                format: course
                question: "Où commence un programme ?"
                choices:
                  - {text: "Dans la fonction `main`", correct: true}
                  - {text: "À la première ligne du fichier", correct: false}
                answer: Dans main.
            """;

    /** Une leçon, hors du Tour. */
    private static final String FONCTIONS = """
            notion: fonctions
            lesson: Une fonction **associe** une valeur à chaque argument.
            cards:
              - {key: vrai-faux, format: true-false, question: "Une fonction est une valeur.", truth: true, answer: "Oui."}
            """;

    /** Ni leçon ni page : la notion n'a rien à montrer à un moteur. */
    private static final String HISTOIRE = """
            notion: histoire
            cards:
              - {key: vrai-faux, format: true-false, question: "Le langage est né en 1957.", truth: true, answer: "Oui."}
            """;

    @Inject
    ContentImportService contentImport;

    @BeforeEach
    void importSubjects() {
        contentImport.importFolder(subject("fortran", "published", 980));
        contentImport.importFolder(subject("cobol", "draft", 990));
    }

    private static SubjectFolder subject(String slug, String status, int position) {
        return new SubjectFolder(
                """
                subject: %s
                title: Un sujet
                category: languages
                position: %d
                status: %s
                """.formatted(slug, position, status),
                Map.of("01-bases.yaml", REFERENTIAL),
                Map.of(
                        "bases/bonjour.yaml",
                        BONJOUR,
                        "bases/fonctions.yaml",
                        FONCTIONS,
                        "bases/histoire.yaml",
                        HISTOIRE));
    }

    @Test
    void shouldListThePublicPagesOfThePublishedSubjectsWithoutAnAccount() throws Exception {
        String xml = given().get(SITEMAP)
                .then()
                .statusCode(200)
                .contentType(startsWith("application/xml"))
                .header("Cache-Control", equalTo(SitemapResource.CACHE_CONTROL))
                .extract()
                .asString();

        List<String> locations = locations(xml);

        assertThat(locations)
                .as("des adresses absolues, à l'origine publique sans sa barre finale")
                .allMatch(location -> location.startsWith(ORIGIN + "/") && !location.startsWith(ORIGIN + "//"))
                .doesNotHaveDuplicates()
                .contains(
                        ORIGIN + "/",
                        ORIGIN + "/learn",
                        ORIGIN + "/tours",
                        ORIGIN + "/subjects",
                        ORIGIN + "/mentions-legales",
                        ORIGIN + "/confidentialite",
                        ORIGIN + "/subjects/fortran",
                        ORIGIN + "/subjects/fortran/course/bonjour",
                        ORIGIN + "/subjects/fortran/course/fonctions",
                        ORIGIN + "/subjects/fortran/tour",
                        ORIGIN + "/subjects/fortran/tour/bonjour")
                .doesNotContain(
                        ORIGIN + "/subjects/fortran/course/histoire", ORIGIN + "/subjects/fortran/tour/fonctions")
                .noneMatch(location -> location.startsWith(ORIGIN + "/subjects/cobol"));
    }

    /** Les adresses du plan, lues par un parseur XML : le document est bien formé, dans l'espace de noms des plans. */
    private static List<String> locations(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        assertThat(document.getDocumentElement().getLocalName()).isEqualTo("urlset");
        assertThat(document.getDocumentElement().getNamespaceURI()).isEqualTo(SitemapResource.NAMESPACE);
        NodeList nodes = document.getElementsByTagNameNS(SitemapResource.NAMESPACE, "loc");
        List<String> locations = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            locations.add(nodes.item(i).getTextContent());
        }
        return locations;
    }
}
