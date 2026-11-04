package com.recallio.runner.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.recallio.runner.sandbox.ProgramResult.Status;
import com.recallio.runner.sandbox.Submission.SourceFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Le contrôle des pages du Tour (ADR 0037), dans le bac à sable où les utilisateurs les exécuteront :
 *
 * <ul>
 *   <li>chaque exemple s'exécute et donne la sortie annoncée ;
 *   <li>la solution de chaque « À toi » donne la sortie attendue ;
 *   <li>son code de départ s'exécute, mais ne la donne pas.
 * </ul>
 *
 * <p>Le reste d'une page (texte, titre, fichiers permis) est vérifié par le backend, dans le job {@code content}. Une
 * nouvelle page est vérifiée sans modifier ce test.
 */
class TourTest {

    private static final Path CONTENT = Path.of("..", "content");
    private static final DockerSandbox SANDBOX = SandboxImage.sandbox(Duration.ofSeconds(60));

    /** Ce test ne lit que les pages du Tour : le reste du fichier de notion est l'affaire du backend. */
    private static final ObjectMapper YAML =
            new ObjectMapper(new YAMLFactory()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    record NotionFile(@Nullable List<PageEntry> tour) {}

    record PageEntry(ProgramEntry example, @Nullable TaskEntry task) {}

    record ProgramEntry(Map<String, String> files, String output) {}

    record TaskEntry(Map<String, String> starter, Map<String, String> solution, String output) {}

    /** Une page, nommée par son fichier et son rang dans la notion : {@code go/notions/bases/hello-world.yaml, page 1}. */
    record Page(String name, PageEntry entry) {

        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Page> pages() throws IOException {
        try (Stream<Path> files = Files.walk(CONTENT)) {
            return files
                    .filter(file -> file.toString().replace('\\', '/').contains("/notions/"))
                    .filter(file -> file.getFileName().toString().endsWith(".yaml"))
                    .sorted()
                    .flatMap(TourTest::pagesOf)
                    .toList()
                    .stream();
        }
    }

    static Stream<Page> pagesWithATask() throws IOException {
        return pages().filter(page -> page.entry().task() != null);
    }

    @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
    @MethodSource("pages")
    void shouldPrintTheOutputOfItsExample(Page page) {
        ProgramEntry example = page.entry().example();

        assertCompletes(page + " (exemple)", example.files(), example.output());
    }

    @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
    @MethodSource("pagesWithATask")
    void shouldBeSolvedByTheSolutionOfItsTask(Page page) {
        TaskEntry task = task(page);

        assertCompletes(page + " (solution de l'« À toi »)", task.solution(), task.output());
    }

    @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
    @MethodSource("pagesWithATask")
    void shouldStartItsTaskFromCodeThatRunsButDoesNotSolveIt(Page page) {
        TaskEntry task = task(page);

        ProgramResult result = SANDBOX.run(program(task.starter()));

        assertThat(result.status())
                .as("code de départ de %s : %s", page, result)
                .isEqualTo(Status.COMPLETED);
        assertThat(normalized(result.stdout()))
                .as("le code de départ de %s donne déjà la sortie attendue", page)
                .isNotEqualTo(normalized(task.output()));
    }

    private static void assertCompletes(String what, Map<String, String> files, String output) {
        ProgramResult result = SANDBOX.run(program(files));

        assertThat(result.status()).as("%s : %s", what, result).isEqualTo(Status.COMPLETED);
        assertThat(result.exitCode())
                .as("code de sortie de %s : %s", what, result)
                .isZero();
        assertThat(normalized(result.stdout())).as("sortie de %s", what).isEqualTo(normalized(output));
    }

    private static TaskEntry task(Page page) {
        return Objects.requireNonNull(page.entry().task(), () -> "« À toi » de " + page);
    }

    /**
     * Les fichiers d'un programme, terminés par un retour à la ligne comme à l'import ; le langage est celui de leur
     * extension.
     */
    private static Program program(Map<String, String> files) {
        List<SourceFile> sources = files.entrySet().stream()
                .map(file -> new SourceFile(
                        file.getKey(), file.getValue().endsWith("\n") ? file.getValue() : file.getValue() + "\n"))
                .toList();
        Language language = sources.getFirst().path().endsWith(".go") ? Language.GO : Language.JAVA;
        return new Program(language, sources);
    }

    /** Aux fins de ligne près, comme les « qu'affiche ce code ? » : ce qui suit la dernière ligne ne compte pas. */
    private static String normalized(String output) {
        return output.replace("\r\n", "\n").stripTrailing();
    }

    private static Stream<Page> pagesOf(Path file) {
        List<PageEntry> tour = read(file).tour();
        if (tour == null) {
            return Stream.empty();
        }
        String name = CONTENT.relativize(file).toString().replace('\\', '/');
        return IntStream.range(0, tour.size())
                .mapToObj(index -> new Page(name + ", page " + (index + 1), tour.get(index)));
    }

    private static NotionFile read(Path file) {
        try {
            return YAML.readValue(file.toFile(), NotionFile.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Fichier de notion illisible : " + file, e);
        }
    }
}
