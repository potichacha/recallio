package com.recallio.content.infrastructure.folder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.content.CardChoice;
import com.recallio.content.CardType;
import com.recallio.content.domain.CardContent;
import com.recallio.content.domain.NotionLevel;
import com.recallio.content.domain.Subject;
import com.recallio.content.domain.source.InvalidSourceException;
import com.recallio.content.domain.source.ParsedContent;
import com.recallio.content.domain.source.ParsedContent.CardKey;
import com.recallio.content.domain.source.ParsedContent.ParsedCard;
import com.recallio.content.domain.source.ParsedContent.SourceIssue;
import com.recallio.content.domain.source.SubjectFolder;
import java.util.Map;
import org.junit.jupiter.api.Test;

class YamlSubjectFolderParserTest {

    private static final String SUBJECT = """
            subject: java
            title: Java
            category: languages
            position: 10
            status: draft
            """;

    private static final String STRINGS = """
            domain: api-de-base
            title: API de base
            position: 30
            chapters:
              - id: chaines
                title: Chaînes de caractères
                notions:
                  - {id: string-bases, title: "La classe String", level: beginner, priority: 1}
                  - {id: string-pool, title: "Le pool de chaînes", level: intermediate, priority: 1, requires: [string-bases]}
            """;

    private static final String HISTORY = """
            domain: bases
            title: Les bases
            position: 10
            chapters:
              - id: histoire
                title: Histoire
                notions:
                  - {id: histoire-java, title: "L'histoire de Java", level: beginner, priority: 3, formats: [course, true-false]}
            """;

    private static final String POOL = """
            notion: string-pool
            lesson: |
              Le **pool** déduplique les littéraux.
            cards:
              - key: emplacement-du-pool
                format: course
                question: Où se trouve le pool de chaînes ?
                answer: Dans le tas depuis Java 7.
              - key: concatenation-constante
                format: code-output
                question: |
                  ```java
                  System.out.println("Ja" + "va" == "Java");
                  ```
                answer: "`true` : la concaténation est calculée à la compilation."
              - key: comparer
                format: multiple-choice
                question: Comment comparer le contenu de deux chaînes ?
                choices:
                  - {text: "`a.equals(b)`", correct: true}
                  - {text: "`a == b`", correct: false}
                answer: Avec equals.
            """;

    private final YamlSubjectFolderParser parser = new YamlSubjectFolderParser();

    @Test
    void shouldReadTheSubjectItsNotionsAndTheCardsOfEachNotion() {
        ParsedContent parsed = parse(Map.of("api-de-base/string-pool.yaml", POOL));

        assertThat(parsed.subject())
                .isEqualTo(new Subject("java", "Java", Subject.Category.LANGUAGES, 10, Subject.Status.DRAFT));
        assertThat(parsed.notions())
                .extracting(notion -> notion.slug(), notion -> notion.position())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("histoire-java", 0),
                        org.assertj.core.groups.Tuple.tuple("string-bases", 1),
                        org.assertj.core.groups.Tuple.tuple("string-pool", 2));
        assertThat(parsed.notions().get(2)).satisfies(notion -> {
            assertThat(notion.level()).isEqualTo(NotionLevel.INTERMEDIATE);
            assertThat(notion.requires()).containsExactly("string-bases");
            assertThat(notion.chapterTitle()).isEqualTo("Chaînes de caractères");
            assertThat(notion.lesson()).isEqualTo("Le **pool** déduplique les littéraux.");
        });
        assertThat(parsed.issues()).isEmpty();
    }

    @Test
    void shouldMakeOneThemePerDomainThatHasCards() {
        ParsedContent parsed = parse(Map.of("api-de-base/string-pool.yaml", POOL));

        assertThat(parsed.themes()).singleElement().satisfies(theme -> {
            assertThat(theme.key()).isEqualTo("api-de-base");
            assertThat(theme.slug()).isEqualTo("java-api-de-base");
            assertThat(theme.title()).isEqualTo("API de base");
            assertThat(theme.position()).isEqualTo(30);
            assertThat(theme.cards())
                    .extracting(ParsedCard::key)
                    .containsExactly(
                            new CardKey("string-pool", "emplacement-du-pool"),
                            new CardKey("string-pool", "concatenation-constante"),
                            new CardKey("string-pool", "comparer"));
        });
    }

    @Test
    void shouldConvertEachFormatToItsCardContent() {
        var cards = parse(Map.of("api-de-base/string-pool.yaml", POOL))
                .themes()
                .getFirst()
                .cards();

        assertThat(cards.get(0).content())
                .isEqualTo(
                        new CardContent.Flashcard("Où se trouve le pool de chaînes ?", "Dans le tas depuis Java 7."));
        assertThat(cards.get(1).content().type()).isEqualTo(CardType.CODE_OUTPUT);
        assertThat(cards.get(1).content().prompt()).startsWith("```java").endsWith("```");
        assertThat(cards.get(2).content().choices())
                .containsExactly(new CardChoice("`a.equals(b)`", true), new CardChoice("`a == b`", false));
    }

    @Test
    void shouldReportInvalidCardsAndImportTheOthers() {
        String notion = """
                notion: string-bases
                cards:
                  - {key: valide, format: course, question: "Q ?", answer: "R."}
                  - {key: valide, format: course, question: "Autre ?", answer: "R."}
                  - {key: Majuscule, format: course, question: "Q ?", answer: "R."}
                  - {key: vrai-faux, format: true-false, question: "Q ?", answer: "R."}
                  - {key: inconnu, format: quiz, question: "Q ?", answer: "R."}
                  - {key: qcm-sans-choix, format: multiple-choice, question: "Q ?", answer: "R."}
                  - {key: qcm-un-choix, format: multiple-choice, question: "Q ?", answer: "R.", choices: [{text: a, correct: true}]}
                  - {key: vide, format: course, question: " ", answer: "R."}
                """;

        ParsedContent parsed = parse(Map.of("api-de-base/string-bases.yaml", notion));

        assertThat(parsed.cardCount()).isEqualTo(1);
        assertThat(parsed.issues())
                .extracting(SourceIssue::excerpt)
                .containsExactly(
                        "valide", "Majuscule", "vrai-faux", "inconnu", "qcm-sans-choix", "qcm-un-choix", "vide");
        assertThat(parsed.issues().get(2).reason()).contains("pas encore pris en charge");
    }

    @Test
    void shouldRefuseAFormatThatTheReferentialExcludesForTheNotion() {
        String notion = """
                notion: histoire-java
                cards:
                  - {key: code, format: code-output, question: "Q ?", answer: "R."}
                """;

        ParsedContent parsed = parse(Map.of("bases/histoire-java.yaml", notion));

        assertThat(parsed.issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue.reason()).contains("non retenu pour cette notion"));
    }

    @Test
    void shouldReportNotionFilesThatAreUnknownMisplacedOrUnreadable() {
        ParsedContent parsed = parse(Map.of(
                "api-de-base/inconnue.yaml", "notion: inconnue\ncards: []\n",
                "bases/string-pool.yaml", POOL,
                "api-de-base/string-bases.yaml", "notion: string-bases\ncartes: []\n"));

        assertThat(parsed.issues())
                .extracting(SourceIssue::reason)
                .anySatisfy(reason -> assertThat(reason).contains("absente du référentiel"))
                .anySatisfy(reason -> assertThat(reason).contains("notions/api-de-base/string-pool.yaml"))
                .anySatisfy(reason -> assertThat(reason).contains("illisible"));
        assertThat(parsed.themes()).isEmpty();
    }

    @Test
    void shouldRefuseAReferentialWithAnUnknownPrerequisite() {
        String broken = STRINGS.replace("requires: [string-bases]", "requires: [string-builder]");

        assertThatThrownBy(
                        () -> parser.parse(new SubjectFolder(SUBJECT, Map.of("03-api-de-base.yaml", broken), Map.of())))
                .isInstanceOf(InvalidSourceException.class)
                .hasMessageContaining("string-builder");
    }

    @Test
    void shouldRefuseAnInvalidSubjectFile() {
        assertThatThrownBy(() -> parser.parse(new SubjectFolder(
                        SUBJECT.replace("languages", "langages"), Map.of("01-bases.yaml", HISTORY), Map.of())))
                .isInstanceOf(InvalidSourceException.class)
                .hasMessageContaining("subject.yaml");
        assertThatThrownBy(() -> parser.parse(
                        new SubjectFolder(SUBJECT + "owner: alice\n", Map.of("01-bases.yaml", HISTORY), Map.of())))
                .isInstanceOf(InvalidSourceException.class)
                .hasMessageContaining("owner");
    }

    private ParsedContent parse(Map<String, String> notions) {
        return parser.parse(
                new SubjectFolder(SUBJECT, Map.of("01-bases.yaml", HISTORY, "03-api-de-base.yaml", STRINGS), notions));
    }
}
