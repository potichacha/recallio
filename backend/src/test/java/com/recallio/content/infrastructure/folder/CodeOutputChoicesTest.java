package com.recallio.content.infrastructure.folder;

import static com.recallio.content.infrastructure.folder.CodeOutputCards.lines;
import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.infrastructure.folder.FolderFiles.CardEntry;
import com.recallio.content.infrastructure.folder.FolderFiles.CheckEntry;
import com.recallio.content.infrastructure.folder.FolderFiles.ChoiceEntry;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Les choix d'un « qu'affiche ce code ? » (ADR 0051) : le bon est celui que donne sa vérification ({@code check}), que
 * {@link CodeOutputTest} confronte à l'exécution du code, et aucun autre choix n'affiche la même chose. Sans exécution,
 * ce test tourne partout.
 */
class CodeOutputChoicesTest {

    /** Un choix écrit comme du code : un bloc entier, ou un extrait entre accents graves. */
    private static final Pattern CODE = Pattern.compile("```[a-z]*\\n(.*?)\\n?```|`([^`]*)`", Pattern.DOTALL);

    static Stream<Arguments> cards() {
        return CodeOutputCards.matching(card -> card.choices() != null && card.check() != null);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cards")
    void shouldMarkAsCorrectTheChoiceThatItsCheckExpects(String name, CardEntry card) {
        CheckEntry check = Objects.requireNonNull(card.check());
        List<ChoiceEntry> choices = Objects.requireNonNull(card.choices());
        List<String> right = choices.stream()
                .filter(ChoiceEntry::correct)
                .map(ChoiceEntry::text)
                .toList();
        assertThat(right)
                .as("%s : un seul bon choix, ce que fait le code", name)
                .hasSize(1);
        String expected = right.getFirst();

        if (Boolean.FALSE.equals(check.compiles())) {
            assertThat(expected).as("%s : le code ne compile pas", name).containsIgnoringCase("compil");
            return;
        }
        String output = lines(check.output());
        // Une exception Java se nomme ; une panic Go se dit « panic ».
        String stop = check.panics() != null ? "panic" : check.throwsName();
        if (stop != null) {
            assertThat(expected).as("%s : le code s'arrête sur %s", name, stop).containsIgnoringCase(stop);
            output.lines()
                    .filter(line -> !line.isBlank())
                    .forEach(line -> assertThat(expected)
                            .as("%s : le code affiche « %s » avant de s'arrêter", name, line)
                            .contains(line.strip()));
            return;
        }
        if (output.isEmpty()) {
            assertThat(expected).as("%s : le code n'affiche rien", name).containsIgnoringCase("rien");
            return;
        }
        assertThat(shown(expected))
                .as("%s : le bon choix est la sortie du code", name)
                .isEqualTo(output);
        choices.stream()
                .filter(choice -> !choice.correct())
                .forEach(wrong -> assertThat(shown(wrong.text()))
                        .as("%s : « %s » affiche aussi la sortie du code", name, wrong.text())
                        .isNotEqualTo(output));
    }

    /** Ce qu'un choix dit que le code affiche : le contenu de son code, ou son texte s'il n'est pas écrit comme du code. */
    private static String shown(String choice) {
        String text = choice.strip();
        Matcher code = CODE.matcher(text);
        if (code.matches()) {
            return lines(firstNonNull(code.group(1), code.group(2)));
        }
        return lines(text);
    }

    private static String firstNonNull(@Nullable String first, @Nullable String second) {
        return first != null ? first : Objects.requireNonNull(second);
    }
}
