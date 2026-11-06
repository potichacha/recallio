package com.recallio.content.infrastructure.folder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.content.CardBlank;
import com.recallio.content.CardChoice;
import com.recallio.content.CardType;
import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.content.TourPage;
import com.recallio.content.domain.CardContent;
import com.recallio.content.domain.NotionLevel;
import com.recallio.content.domain.Subject;
import com.recallio.content.domain.source.InvalidSourceException;
import com.recallio.content.domain.source.ParsedContent;
import com.recallio.content.domain.source.ParsedContent.CardKey;
import com.recallio.content.domain.source.ParsedContent.ParsedCard;
import com.recallio.content.domain.source.ParsedContent.SourceIssue;
import com.recallio.content.domain.source.SubjectFolder;
import java.util.HashMap;
import java.util.List;
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

    private static final String POOL_WITH_TOUR = """
            notion: string-pool
            tour:
              - title: Le pool
                text: |-
                  Deux littéraux identiques sont **le même objet**.
                example:
                  files:
                    Main.java: |-
                      void main() {
                          IO.println("Ja" + "va" == "Java");
                      }
                  output: "true"
                task:
                  instructions: Compare avec une chaîne créée par `new String`.
                  starter:
                    Main.java: |-
                      void main() {
                          IO.println("?");
                      }
                  solution:
                    Main.java: |-
                      void main() {
                          IO.println(new String("Java") == "Java");
                      }
                  output: "false"
              - text: Une seconde page, sans titre ni « À toi ».
                example:
                  files:
                    Main.java: |-
                      void main() {
                          IO.println("Java".intern() == "Java");
                      }
                  output: "true"
            cards:
              - key: emplacement-du-pool
                format: course
                question: Où se trouve le pool de chaînes ?
                answer: Dans le tas depuis Java 7.
              - {key: vrai-faux, format: true-false, question: "Une affirmation vraie.", truth: true, answer: Oui.}
              - key: choix
                format: multiple-choice
                question: Quel choix est le bon ?
                choices:
                  - {text: Le bon, correct: true}
                  - {text: Le mauvais, correct: false}
                answer: Le bon.
            """;

    private final YamlSubjectFolderParser parser = new YamlSubjectFolderParser();

    @Test
    void shouldReadTheTourPagesOfANotionInTheOrderOfItsFile() {
        ParsedContent parsed = parse(Map.of("api-de-base/string-pool.yaml", POOL_WITH_TOUR));

        assertThat(parsed.issues()).isEmpty();
        List<TourPage> tour = parsed.notions().get(2).tour();
        assertThat(tour).extracting(TourPage::title).containsExactly("Le pool", null);
        assertThat(tour.getFirst().text()).isEqualTo("Deux littéraux identiques sont **le même objet**.");
        assertThat(tour.getFirst().example())
                .isEqualTo(new TourPage.Program(
                        List.of(new SourceFile(
                                "Main.java", "void main() {\n    IO.println(\"Ja\" + \"va\" == \"Java\");\n}\n")),
                        "true"));
        assertThat(tour.getFirst().task()).satisfies(task -> {
            assertThat(task).isNotNull();
            assertThat(task.output()).isEqualTo("false");
            assertThat(task.solution().getFirst().content()).contains("new String(\"Java\")");
        });
        assertThat(tour.get(1).task()).isNull();
        assertThat(parsed.notions().get(1).tour()).isEmpty();
    }

    @Test
    void shouldReportAnInvalidTourPageAndKeepTheOthers() {
        ParsedContent parsed = parse(Map.of("api-de-base/string-pool.yaml", """
                notion: string-pool
                tour:
                  - text: Une page valide.
                    example:
                      files:
                        Main.java: |-
                          void main() {
                              IO.println("ok");
                          }
                      output: ok
                  - text: Un programme Java sans Main.java.
                    example:
                      files:
                        Pool.java: |-
                          class Pool {}
                      output: rien
                cards:
                  - {key: vrai-faux, format: true-false, question: "Une affirmation vraie.", truth: true, answer: Oui.}
                  - key: choix
                    format: multiple-choice
                    question: Quel choix est le bon ?
                    choices:
                      - {text: Le bon, correct: true}
                      - {text: Le mauvais, correct: false}
                    answer: Le bon.
                """));

        assertThat(parsed.notions().get(2).tour()).hasSize(1);
        assertThat(parsed.issues())
                .extracting(SourceIssue::reason)
                .containsExactly("notions/api-de-base/string-pool.yaml : page 2 du Tour : "
                        + "L'exemple Java a un fichier Main.java, qui contient main");
    }

    @Test
    void shouldRefuseATourWhereANotionComesBeforeItsPrerequisite() {
        String referential = """
                domain: bases
                title: Les bases
                position: 10
                chapters:
                  - id: objets
                    title: Les objets
                    notions:
                      - {id: methodes, title: "Les méthodes", level: beginner, priority: 1, requires: [classes]}
                      - {id: classes, title: "Les classes", level: beginner, priority: 1}
                """;

        ParsedContent parsed = parser.parse(new SubjectFolder(
                SUBJECT,
                Map.of("01-bases.yaml", referential),
                Map.of("bases/methodes.yaml", javaPage("methodes"), "bases/classes.yaml", javaPage("classes"))));

        assertThat(parsed.issues())
                .extracting(SourceIssue::reason)
                .containsExactly(
                        "Tour : la notion « methodes » vient avant son prérequis « classes » dans le référentiel");
    }

    @Test
    void shouldRefuseATourInTwoLanguages() {
        ParsedContent parsed =
                parse(Map.of("api-de-base/string-bases.yaml", """
                notion: string-bases
                tour:
                  - text: Une page en Go, dans un sujet dont les autres pages sont en Java.
                    example:
                      files:
                        main.go: |-
                          package main

                          func main() {}
                      output: rien
                cards:
                  - {key: vrai-faux, format: true-false, question: "Une affirmation vraie.", truth: true, answer: Oui.}
                  - {key: faux, format: true-false, question: "Une affirmation fausse.", truth: false, answer: Non.}
                """, "api-de-base/string-pool.yaml", POOL_WITH_TOUR));

        assertThat(parsed.issues())
                .extracting(SourceIssue::reason)
                .containsExactly("Tour : les pages d'un sujet sont dans un seul langage, pas [JAVA, GO]");
    }

    @Test
    void shouldRequireTwoCardsGradedByTheServerInANotionOfTheTour() {
        ParsedContent parsed = parse(Map.of("api-de-base/string-pool.yaml", """
                notion: string-pool
                tour:
                  - text: Une page.
                    example:
                      files:
                        Main.java: |-
                          void main() {
                              IO.println("ok");
                          }
                      output: ok
                cards:
                  - {key: cours, format: course, question: "Une question ?", answer: Une réponse.}
                  - {key: vrai-faux, format: true-false, question: "Une affirmation vraie.", truth: true, answer: Oui.}
                """));

        assertThat(parsed.issues())
                .extracting(SourceIssue::reason)
                .containsExactly("Tour : la notion « string-pool » a 1 carte(s) corrigée(s) par le serveur (vrai ou "
                        + "faux, QCM, code à compléter) ; il en faut au moins 2 pour la valider");
    }

    /** Un fichier de notion avec une page du Tour en Java, et les deux cartes corrigées qui la valident. */
    private static String javaPage(String notion) {
        return """
                notion: %s
                tour:
                  - text: Une page.
                    example:
                      files:
                        Main.java: |-
                          void main() {
                              IO.println("ok");
                          }
                      output: ok
                cards:
                  - {key: vrai-faux, format: true-false, question: "Une affirmation vraie.", truth: true, answer: Oui.}
                  - key: choix
                    format: multiple-choice
                    question: Quel choix est le bon ?
                    choices:
                      - {text: Le bon, correct: true}
                      - {text: Le mauvais, correct: false}
                    answer: Le bon.
                """.formatted(notion);
    }

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
                  - {key: exercice, format: exercise, question: "Q ?", answer: "R."}
                  - {key: trous-sans-reponses, format: fill-in, question: "`___`", answer: "R."}
                  - {key: trous-en-trop, format: fill-in, question: "`___`", answer: "R.", blanks: [[a], [b]]}
                  - {key: trous-hors-format, format: course, question: "Q ?", answer: "R.", blanks: [[a]]}
                  - {key: vrai-faux-sans-valeur, format: true-false, question: "Q ?", answer: "R."}
                  - {key: valeur-hors-vrai-faux, format: course, question: "Q ?", answer: "R.", truth: true}
                  - {key: inconnu, format: quiz, question: "Q ?", answer: "R."}
                  - {key: qcm-sans-choix, format: multiple-choice, question: "Q ?", answer: "R."}
                  - {key: qcm-un-choix, format: multiple-choice, question: "Q ?", answer: "R.", choices: [{text: a, correct: true}]}
                  - {key: choix-sur-vrai-faux, format: true-false, question: "Q ?", answer: "R.", truth: true, choices: [{text: a, correct: true}, {text: b, correct: false}]}
                  - {key: choix-en-double, format: course, question: "Q ?", answer: "R.", choices: [{text: a, correct: true}, {text: a, correct: false}]}
                  - {key: vide, format: course, question: " ", answer: "R."}
                  - {key: verification-hors-format, format: course, question: "Q ?", answer: "R.", check: {output: "1"}}
                  - {key: verification-et-raison, format: code-output, question: "Q ?", answer: "R.", check: {output: "1"}, unverifiable: threads}
                  - {key: verification-sans-attendu, format: code-output, question: "Q ?", answer: "R.", check: {code: "f();"}}
                  - {key: compile-et-affiche, format: code-output, question: "Q ?", answer: "R.", check: {output: "1", compiles: false}}
                  - {key: verifiee, format: code-output, question: "Q ?", answer: "R.", check: {code: "f();", throws: NullPointerException}}
                """;

        ParsedContent parsed = parse(Map.of("api-de-base/string-bases.yaml", notion));

        assertThat(parsed.cardCount()).isEqualTo(2);
        assertThat(parsed.issues())
                .extracting(SourceIssue::excerpt)
                .containsExactly(
                        "valide",
                        "Majuscule",
                        "exercice",
                        "trous-sans-reponses",
                        "trous-en-trop",
                        "trous-hors-format",
                        "vrai-faux-sans-valeur",
                        "valeur-hors-vrai-faux",
                        "inconnu",
                        "qcm-sans-choix",
                        "qcm-un-choix",
                        "choix-sur-vrai-faux",
                        "choix-en-double",
                        "vide",
                        "verification-hors-format",
                        "verification-et-raison",
                        "verification-sans-attendu",
                        "compile-et-affiche");
        assertThat(parsed.issues().get(2).reason())
                .contains("son propre dossier : exercices/api-de-base/string-bases/");
    }

    @Test
    void shouldGradeACardOfAnotherFormatOnTheChoicesItReceives() {
        String notion = """
                notion: string-bases
                cards:
                  - key: cours
                    format: course
                    question: Java est-il compilé ou interprété ?
                    choices:
                      - {text: Compilé, correct: false}
                      - {text: Interprété, correct: false}
                      - {text: Les deux, correct: true}
                    answer: Compilé en bytecode par javac, puis interprété et compilé à la volée par la JVM.
                  - key: situation
                    format: scenario
                    question: Ton programme échoue sur le serveur avec `UnsupportedClassVersionError`. Pourquoi ?
                    choices:
                      - {text: Il vise une version de Java plus récente que celle du serveur., correct: true}
                      - {text: Une classe manque au classpath., correct: false}
                    answer: Le bytecode a été produit pour une JVM plus récente.
                  - {key: pas-encore, format: course, question: "Qu'est-ce qu'une chaîne ?", answer: "Une suite de caractères."}
                """;

        var cards = parse(Map.of("api-de-base/string-bases.yaml", notion))
                .themes()
                .getFirst()
                .cards();

        assertThat(cards.get(0).content()).isInstanceOf(CardContent.Flashcard.class);
        assertThat(cards.get(0).content().choices())
                .extracting(CardChoice::correct)
                .containsExactly(false, false, true);
        assertThat(cards.get(1).content().type()).isEqualTo(CardType.OPEN_SCENARIO);
        assertThat(cards.get(1).content().choices()).hasSize(2);
        // Une carte qui n'a pas encore reçu de choix s'auto-évalue, le temps de la migration (ADR 0051).
        assertThat(cards.get(2).content().choices()).isEmpty();
    }

    @Test
    void shouldReadATrueFalseCardAsTwoFixedChoicesAndABugFixAsSelfAssessed() {
        String notion = """
                notion: string-bases
                cards:
                  - {key: affirmation, format: true-false, question: "String est immuable.", truth: true, answer: "Oui."}
                  - key: bug
                    format: bug-fix
                    question: "`if (s == \\"oui\\")`"
                    answer: "`if (\\"oui\\".equals(s))`"
                """;

        var cards = parse(Map.of("api-de-base/string-bases.yaml", notion))
                .themes()
                .getFirst()
                .cards();

        assertThat(cards.get(0).content()).isEqualTo(new CardContent.TrueFalse("String est immuable.", "Oui.", true));
        assertThat(cards.get(0).content().choices())
                .containsExactly(new CardChoice("Vrai", true), new CardChoice("Faux", false));
        assertThat(cards.get(1).content().type()).isEqualTo(CardType.BUG_FIX);
        assertThat(cards.get(1).content().choices()).isEmpty();
    }

    @Test
    void shouldReadAFillInCardWithTheAcceptedAnswersOfEachBlank() {
        String notion = """
                notion: string-bases
                cards:
                  - key: comparer
                    format: fill-in
                    question: |-
                      ```java
                      boolean memeTexte = a.___(b);
                      int ordre = a.___(b);
                      ```
                    blanks: [[equals], [compareTo, compareToIgnoreCase]]
                    answer: equals compare le contenu, compareTo l'ordre.
                """;

        ParsedCard card = parse(Map.of("api-de-base/string-bases.yaml", notion))
                .themes()
                .getFirst()
                .cards()
                .getFirst();

        assertThat(card.content().type()).isEqualTo(CardType.FILL_IN);
        assertThat(card.content().blanks())
                .containsExactly(
                        new CardBlank(List.of("equals")), new CardBlank(List.of("compareTo", "compareToIgnoreCase")));
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

    @Test
    void shouldReadACodingExerciseFromItsFolderAfterTheCardsOfItsNotion() {
        ParsedContent parsed =
                parse(Map.of("api-de-base/string-pool.yaml", POOL), exercise("api-de-base/string-pool/inverser"));

        ParsedCard exercise = parsed.themes().getFirst().cards().getLast();
        assertThat(exercise.key()).isEqualTo(new CardKey("string-pool", "inverser"));
        assertThat(exercise.content()).isInstanceOfSatisfying(CardContent.CodingExercise.class, content -> {
            assertThat(content.prompt()).isEqualTo("Inverser une chaîne.");
            assertThat(content.answer()).isEqualTo("Avec un StringBuilder.");
            assertThat(content.files().starter())
                    .containsExactly(new SourceFile("fr/recallio/Chaines.java", STARTER_CODE));
            assertThat(content.files().solution())
                    .extracting(SourceFile::path)
                    .containsExactly("fr/recallio/Chaines.java");
            assertThat(content.files().tests()).extracting(SourceFile::path).containsExactly("ChainesTest.java");
        });
        assertThat(parsed.issues()).isEmpty();
    }

    @Test
    void shouldReadTheExercisesOfANotionThatHasNoCardFile() {
        ParsedContent parsed = parse(Map.of(), exercise("api-de-base/string-bases/inverser"));

        assertThat(parsed.cardCount()).isEqualTo(1);
        assertThat(parsed.issues()).isEmpty();
    }

    @Test
    void shouldReportInvalidExercisesAndImportTheOthers() {
        Map<String, String> files = new HashMap<>(exercise("api-de-base/string-bases/valide"));
        files.putAll(exercise("api-de-base/inconnue/exercice"));
        files.putAll(exercise("bases/string-bases/mauvais-domaine"));
        files.putAll(exercise("bases/histoire-java/format-exclu"));
        files.putAll(exercise("api-de-base/string-pool/emplacement-du-pool"));
        files.putAll(exercise("api-de-base/string-bases/sans-solution"));
        files.remove("api-de-base/string-bases/sans-solution/solution/fr/recallio/Chaines.java");
        files.putAll(exercise("api-de-base/string-bases/fichier-en-trop"));
        files.put("api-de-base/string-bases/fichier-en-trop/notes.md", "Des notes.");
        files.putAll(exercise("api-de-base/string-bases/sans-description"));
        files.remove("api-de-base/string-bases/sans-description/exercise.yaml");
        files.put("api-de-base/orphelin.java", "class Orphelin {}");

        ParsedContent parsed = parse(Map.of("api-de-base/string-pool.yaml", POOL), files);

        assertThat(parsed.themes().stream().flatMap(theme -> theme.cards().stream()))
                .filteredOn(card -> card.content().type() == CardType.CODING_EXERCISE)
                .extracting(ParsedCard::key)
                .containsExactly(new CardKey("string-bases", "valide"));
        assertThat(parsed.issues())
                .extracting(SourceIssue::excerpt)
                .containsExactlyInAnyOrder(
                        "api-de-base/orphelin.java",
                        "api-de-base/inconnue/exercice",
                        "bases/string-bases/mauvais-domaine",
                        "bases/histoire-java/format-exclu",
                        "emplacement-du-pool",
                        "api-de-base/string-bases/sans-solution",
                        "api-de-base/string-bases/fichier-en-trop",
                        "api-de-base/string-bases/sans-description");
    }

    private static final String STARTER_CODE = """
            package fr.recallio;

            public class Chaines {
                public static String inverser(String texte) {
                    return texte;
                }
            }
            """;

    /** Un exercice valide dans ce dossier : son énoncé, son code de départ, sa solution et son test. */
    private static Map<String, String> exercise(String directory) {
        return Map.of(
                directory + "/exercise.yaml",
                "statement: Inverser une chaîne.\nexplanation: Avec un StringBuilder.\n",
                directory + "/starter/fr/recallio/Chaines.java",
                STARTER_CODE,
                directory + "/solution/fr/recallio/Chaines.java",
                STARTER_CODE.replace("return texte;", "return new StringBuilder(texte).reverse().toString();"),
                directory + "/tests/ChainesTest.java",
                "class ChainesTest {}");
    }

    private ParsedContent parse(Map<String, String> notions) {
        return parse(notions, Map.of());
    }

    private ParsedContent parse(Map<String, String> notions, Map<String, String> exercises) {
        return parser.parse(new SubjectFolder(
                SUBJECT, Map.of("01-bases.yaml", HISTORY, "03-api-de-base.yaml", STRINGS), notions, exercises));
    }
}
