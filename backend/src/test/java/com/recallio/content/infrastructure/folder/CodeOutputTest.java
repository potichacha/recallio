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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Exécute chaque « qu'affiche ce code ? » des dossiers de sujet (ADR 0028) et compare le résultat à sa vérification
 * ({@code check}) : une réponse fausse, ou qui ne vaut plus pour le JDK du projet, se voit avant le merge.
 *
 * <p>Seul Java s'exécute pour l'instant : un sujet migré dans un autre langage ajoutera son exécuteur.
 */
class CodeOutputTest {

    private static final Path CONTENT = Path.of("..", "content");
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final String CODE_OUTPUT = "code-output";
    /** Le premier bloc {@code java} de la question : le code exécuté quand la vérification n'en donne pas. */
    private static final Pattern JAVA_BLOCK = Pattern.compile("```java\\n(.*?)```", Pattern.DOTALL);

    /** Une seule JVM d'exécution pour toutes les cartes : en lancer une par carte coûterait une seconde chacune. */
    private static final SnippetRunner RUNNER = new SnippetRunner();

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

        Execution execution = RUNNER.run(code(check, card.question(), name));

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

    private static String code(CheckEntry check, String question, String name) {
        String code = check.code();
        if (code != null) {
            return code;
        }
        Matcher block = JAVA_BLOCK.matcher(question);
        assertThat(block.find())
                .as("%s : la question n'a pas de bloc java, la vérification doit donner son code", name)
                .isTrue();
        return block.group(1);
    }

    /** La sortie aux fins de ligne près : {@code \r\n} comme {@code \n}, sans la dernière. */
    private static String lines(@Nullable String output) {
        return output == null ? "" : output.replace("\r\n", "\n").stripTrailing();
    }

    private static NotionFile read(String content) {
        try {
            return YAML.readValue(content, NotionFile.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
