package com.recallio.content.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * La PWA ne colore que les langages de {@code highlightLanguages.ts} (audit de novembre 2026, élément 13) : un bloc de
 * code d'un autre langage s'afficherait en texte brut, sans que personne le remarque. Ce test lit donc les deux côtés :
 * les langages annoncés par les blocs de code de {@code content/}, et ceux que le front sait colorer.
 */
class CodeLanguagesTest {

    private static final Path CONTENT = Path.of("..", "content");
    private static final Path HIGHLIGHT_LANGUAGES =
            Path.of("..", "frontend", "src", "components", "highlightLanguages.ts");

    /** L'ouverture d'un bloc de code et son langage, en début de ligne ou dans une chaîne YAML. */
    private static final Pattern FENCE = Pattern.compile("```([A-Za-z][A-Za-z0-9+#-]*)");

    private static final Pattern IMPORT = Pattern.compile("from 'highlight\\.js/lib/languages/([a-z0-9-]+)'");

    @Test
    void shouldOnlyAnnounceCodeLanguagesThatTheFrontendHighlights() throws IOException {
        Set<String> highlighted = matches(IMPORT, Files.readString(HIGHLIGHT_LANGUAGES, StandardCharsets.UTF_8));
        assertThat(highlighted).as("les langages de %s", HIGHLIGHT_LANGUAGES).isNotEmpty();

        Set<String> announced = new TreeSet<>();
        try (Stream<Path> files = Files.walk(CONTENT)) {
            files.filter(CodeLanguagesTest::isText).forEach(file -> announced.addAll(matches(FENCE, read(file))));
        }

        assertThat(announced)
                .as(
                        "Un bloc de code annonce un langage que la PWA ne colore pas : écris son nom comme"
                                + " highlight.js (bash, cpp, yaml...), ou ajoute-le à %s",
                        HIGHLIGHT_LANGUAGES.getFileName())
                .isSubsetOf(highlighted);
    }

    private static boolean isText(Path file) {
        String name = file.getFileName().toString();
        return Files.isRegularFile(file) && (name.endsWith(".yaml") || name.endsWith(".md"));
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Set<String> matches(Pattern pattern, String text) {
        Set<String> found = new TreeSet<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }
}
