package com.recallio.content.infrastructure.folder;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.recallio.content.infrastructure.folder.FolderFiles.CardEntry;
import com.recallio.content.infrastructure.folder.FolderFiles.CheckEntry;
import com.recallio.content.infrastructure.folder.FolderFiles.NotionFile;
import com.recallio.content.infrastructure.folder.SnippetRunner.Execution;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Exécute chaque « qu'affiche ce code ? » des dossiers de sujet (ADR 0028) et compare le résultat à sa vérification
 * ({@code check}) : une réponse fausse, ou qui ne vaut plus pour le JDK du projet, se voit avant le merge.
 *
 * <p>Java s'exécute avec JShell, Go avec la chaîne d'outils {@code go} de la machine (ADR 0033). Le langage est celui du
 * premier bloc de code de la question ; un code de vérification qui commence par {@code package} est du Go. Sans
 * {@code go} installé, les cartes Go sont ignorées en local, mais échouent en CI.
 */
class CodeOutputTest {

    private static final Path CONTENT = Path.of("..", "content");
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final String CODE_OUTPUT = "code-output";
    /** Le premier bloc de code de la question : le code exécuté quand la vérification n'en donne pas. */
    private static final Pattern CODE_BLOCK = Pattern.compile("```(java|go)\\n(.*?)```", Pattern.DOTALL);

    private static final String GO = "go";

    /** Une seule JVM d'exécution pour toutes les cartes : en lancer une par carte coûterait une seconde chacune. */
    private static final SnippetRunner RUNNER = new SnippetRunner();

    /** La chaîne d'outils Go de la machine, si elle est installée. */
    private static final Optional<GoSnippetRunner> GO_RUNNER = GoSnippetRunner.find();

    @AfterAll
    static void stopJShell() {
        RUNNER.close();
    }

    static Stream<Arguments> cards() {
        return SubjectFolders.in(CONTENT).stream().flatMap(folder -> {
            Map<String, String> notions = SubjectFolders.read(folder).notions();
            return notions.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .flatMap(file -> read(file.getValue()).cards().stream()
                            .filter(card -> card.format().equals(CODE_OUTPUT))
                            .map(card ->
                                    Arguments.of(folder.getFileName() + "/" + file.getKey() + "#" + card.key(), card)));
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cards")
    void shouldBehaveAsItsAnswerSays(String name, CardEntry card) {
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
        if (snippet.go()) {
            verifyGo(name, check, snippet.code());
            return;
        }
        assertThat(check.panics())
                .as("%s : panics est réservé à Go, Java attend throws", name)
                .isNull();

        Execution execution = RUNNER.run(snippet.code());

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

    /** Le code à exécuter, et s'il est en Go. */
    private record Snippet(String code, boolean go) {}

    private static Snippet snippet(CheckEntry check, String question, String name) {
        Matcher block = CODE_BLOCK.matcher(question);
        boolean hasBlock = block.find();
        String code = check.code();
        if (code != null) {
            boolean go = code.strip().startsWith("package ")
                    || (hasBlock && block.group(1).equals(GO));
            return new Snippet(code, go);
        }
        assertThat(hasBlock)
                .as("%s : la question n'a pas de bloc de code, la vérification doit donner son code", name)
                .isTrue();
        return new Snippet(block.group(2), block.group(1).equals(GO));
    }

    /**
     * La sortie aux fins de ligne près : {@code \r\n} comme {@code \n}, sans les espaces invisibles en fin de ligne, sans
     * la dernière ligne vide.
     */
    private static String lines(@Nullable String output) {
        return output == null
                ? ""
                : output.replace("\r\n", "\n")
                        .lines()
                        .map(String::stripTrailing)
                        .collect(Collectors.joining("\n"))
                        .stripTrailing();
    }

    private static NotionFile read(String content) {
        try {
            return YAML.readValue(content, NotionFile.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
