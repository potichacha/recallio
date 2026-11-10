package com.recallio.content.infrastructure.folder;

import static com.recallio.content.infrastructure.folder.CodeOutputCards.lines;
import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.infrastructure.folder.FolderFiles.CardEntry;
import com.recallio.content.infrastructure.folder.FolderFiles.CheckEntry;
import com.recallio.content.infrastructure.folder.SnippetRunner.Execution;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Exécute chaque « qu'affiche ce code ? » des dossiers de sujet (ADR 0028) et compare le résultat à sa vérification
 * ({@code check}) : une réponse fausse, ou qui ne vaut plus pour le JDK du projet, se voit avant le merge.
 *
 * <p>Java s'exécute avec JShell, Go avec la chaîne d'outils {@code go} de la machine (ADR 0033), C++ avec son
 * {@code g++} 14 (ADR 0053). Le langage est celui du premier bloc de code de la question ; un code de vérification qui
 * commence par {@code package} est du Go, un qui inclut un en-tête ({@code #include}) du C++. Sans {@code go} ni
 * {@code g++} 14, les cartes de ces langages sont ignorées en local, mais échouent en CI.
 */
class CodeOutputTest {

    /** Le premier bloc de code de la question : le code exécuté quand la vérification n'en donne pas. */
    private static final Pattern CODE_BLOCK = Pattern.compile("```(java|go|cpp)\\n(.*?)```", Pattern.DOTALL);

    /** Le langage d'une carte, et donc ce qui l'exécute. */
    private enum Language {
        JAVA,
        GO,
        CPP;

        /** Le langage d'un bloc de code Markdown ({@code java}, {@code go}, {@code cpp}). */
        static Language ofBlock(String language) {
            return valueOf(language.toUpperCase(Locale.ROOT));
        }
    }

    /** Une seule JVM d'exécution pour toutes les cartes : en lancer une par carte coûterait une seconde chacune. */
    private static final SnippetRunner RUNNER = new SnippetRunner();

    /** La chaîne d'outils Go de la machine, si elle est installée. */
    private static final Optional<GoSnippetRunner> GO_RUNNER = GoSnippetRunner.find();

    /** Le {@code g++} 14 de la machine, s'il est installé. */
    private static final Optional<CppSnippetRunner> CPP_RUNNER = CppSnippetRunner.find();

    @AfterAll
    static void stopJShell() {
        RUNNER.close();
    }

    /** Les cartes Java : une seule JVM JShell pour toutes, qui ne se partage pas entre deux threads. */
    static Stream<Arguments> javaCards() {
        return CodeOutputCards.matching(card -> language(card) == Language.JAVA);
    }

    /** Les cartes Go : chacune se compile dans son propre dossier, et le cache de {@code go} se partage sans risque. */
    static Stream<Arguments> goCards() {
        return CodeOutputCards.matching(card -> language(card) == Language.GO);
    }

    /** Les cartes C++ : chacune se compile dans son propre dossier, comme en Go. */
    static Stream<Arguments> cppCards() {
        return CodeOutputCards.matching(card -> language(card) == Language.CPP);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("javaCards")
    void shouldBehaveAsItsAnswerSays(String name, CardEntry card) {
        verify(name, card);
    }

    /** En parallèle (voir {@code junit-platform.properties}) : compiler chaque carte une à une prenait des minutes. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("goCards")
    // Le nom complet : SnippetRunner.Execution, le résultat d'une carte Java, porte déjà ce nom ici.
    @org.junit.jupiter.api.parallel.Execution(ExecutionMode.CONCURRENT)
    void shouldBehaveAsItsAnswerSaysInGo(String name, CardEntry card) {
        verify(name, card);
    }

    @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
    @MethodSource("cppCards")
    @org.junit.jupiter.api.parallel.Execution(ExecutionMode.CONCURRENT)
    void shouldBehaveAsItsAnswerSaysInCpp(String name, CardEntry card) {
        verify(name, card);
    }

    private static void verify(String name, CardEntry card) {
        String unverifiable = card.unverifiable();
        if (unverifiable != null) {
            assertThat(unverifiable).as("raison de ne pas vérifier %s", name).isNotBlank();
            return;
        }
        CheckEntry check = card.check();
        if (check == null) {
            throw new AssertionError(name + " : une vérification (check) ou la raison de son absence (unverifiable)");
        }

        Snippet snippet = snippet(check, card.question(), name);
        switch (snippet.language()) {
            case GO -> verifyGo(name, check, snippet.code());
            case CPP -> verifyCpp(name, check, snippet.code());
            case JAVA -> verifyJava(name, check, snippet.code());
        }
    }

    /** Un code Java, exécuté par JShell : sa sortie, puis l'exception qui l'arrête, nommée par {@code throws}. */
    private static void verifyJava(String name, CheckEntry check, String code) {
        assertThat(check.panics())
                .as("%s : panics est réservé à Go, Java attend throws", name)
                .isNull();

        Execution execution = RUNNER.run(code);

        if (Boolean.FALSE.equals(check.compiles())) {
            assertThat(execution.compileError())
                    .as("%s ne doit pas compiler", name)
                    .isNotNull();
            return;
        }
        assertThat(execution.compileError())
                .as("erreur de compilation de %s", name)
                .isNull();
        assertThat(lines(execution.output())).as("sortie de %s", name).isEqualTo(lines(check.output()));
        assertThat(execution.exception()).as("exception de %s", name).isEqualTo(check.throwsName());
    }

    /** Un programme Go complet : sa sortie, puis la panic qui l'arrête, dont {@code panics} donne un extrait. */
    private static void verifyGo(String name, CheckEntry check, String program) {
        assertThat(check.throwsName())
                .as("%s : throws est réservé à Java, Go attend panics", name)
                .isNull();
        if (GO_RUNNER.isEmpty()) {
            assertThat(System.getenv("CI"))
                    .as("%s : go doit être installé en CI (ADR 0033)", name)
                    .isNull();
            Assumptions.abort("go n'est pas installé : " + name + " n'est pas vérifiée");
        }
        GoSnippetRunner.Execution execution = GO_RUNNER.orElseThrow().run(program);

        if (Boolean.FALSE.equals(check.compiles())) {
            assertThat(execution.compileError())
                    .as("%s ne doit pas compiler", name)
                    .isNotNull();
            return;
        }
        assertThat(execution.compileError())
                .as("erreur de compilation de %s", name)
                .isNull();
        assertThat(lines(execution.output())).as("sortie de %s", name).isEqualTo(lines(check.output()));
        String panics = check.panics();
        if (panics == null) {
            assertThat(execution.panic()).as("arrêt de %s", name).isNull();
        } else {
            assertThat(execution.panic()).as("arrêt de %s", name).contains(panics);
        }
    }

    /**
     * Un programme C++ complet : sa sortie, puis le type de l'exception non rattrapée qui l'arrête, que {@code throws}
     * nomme ({@code std::out_of_range}).
     */
    private static void verifyCpp(String name, CheckEntry check, String program) {
        assertThat(check.panics())
                .as("%s : panics est réservé à Go, C++ attend throws", name)
                .isNull();
        if (CPP_RUNNER.isEmpty()) {
            assertThat(System.getenv("CI"))
                    .as("%s : g++ 14 doit être installé en CI (ADR 0053)", name)
                    .isNull();
            Assumptions.abort("g++ 14 n'est pas installé : " + name + " n'est pas vérifiée");
        }
        CppSnippetRunner.Execution execution = CPP_RUNNER.orElseThrow().run(program);

        if (Boolean.FALSE.equals(check.compiles())) {
            assertThat(execution.compileError())
                    .as("%s ne doit pas compiler", name)
                    .isNotNull();
            return;
        }
        assertThat(execution.compileError())
                .as("erreur de compilation de %s", name)
                .isNull();
        assertThat(lines(execution.output())).as("sortie de %s", name).isEqualTo(lines(check.output()));
        assertThat(execution.stop()).as("arrêt de %s", name).isEqualTo(check.throwsName());
    }

    /** Le code à exécuter, et son langage. */
    private record Snippet(String code, Language language) {}

    /**
     * Le langage d'une carte, comme le décide {@link #snippet} : celui de son code de vérification s'il se reconnaît
     * ({@code package} pour Go, {@code #include} pour C++), sinon celui du premier bloc de sa question. Une carte sans
     * code se range avec Java, où son test échoue et dit pourquoi.
     */
    private static Language language(CardEntry card) {
        CheckEntry check = card.check();
        String code = check == null ? null : check.code();
        if (code != null) {
            Optional<Language> declared = declaredLanguage(code);
            if (declared.isPresent()) {
                return declared.get();
            }
        }
        Matcher block = CODE_BLOCK.matcher(card.question());
        return block.find() ? Language.ofBlock(block.group(1)) : Language.JAVA;
    }

    /** Le langage qu'un code de vérification dit lui-même : un {@code package} Go, ou un {@code #include} C++. */
    private static Optional<Language> declaredLanguage(String code) {
        String start = code.strip();
        if (start.startsWith("package ")) {
            return Optional.of(Language.GO);
        }
        if (start.startsWith("#include")) {
            return Optional.of(Language.CPP);
        }
        return Optional.empty();
    }

    private static Snippet snippet(CheckEntry check, String question, String name) {
        Matcher block = CODE_BLOCK.matcher(question);
        boolean hasBlock = block.find();
        String code = check.code();
        if (code != null) {
            Language language =
                    declaredLanguage(code).orElse(hasBlock ? Language.ofBlock(block.group(1)) : Language.JAVA);
            return new Snippet(code, language);
        }
        assertThat(hasBlock)
                .as("%s : la question n'a pas de bloc de code, la vérification doit donner son code", name)
                .isTrue();
        return new Snippet(block.group(2), Language.ofBlock(block.group(1)));
    }
}
