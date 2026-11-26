package com.recallio.entitlement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AccessCodeTest {

    private static final String SYMBOLS = "K7QW2M9XDF4TLPA8RBNCH3VYE";
    private static final String FORMATTED = "K7QW2-M9XDF-4TLPA-8RBNC-H3VYE";

    private final SecureRandom random = new SecureRandom();

    @Test
    void shouldGenerateTwentyFiveSymbolsOfAnAlphabetWithoutAmbiguousCharacters() {
        for (int draw = 0; draw < 200; draw++) {
            AccessCode code = AccessCode.generate(random);

            assertThat(code.symbols()).hasSize(25).matches("[2-9A-HJ-NP-Z]{25}").doesNotContain("0", "O", "1", "I");
        }
    }

    /** 32 caractères, soit 5 bits chacun : 25 caractères portent 125 bits, plus que les 100 demandés. */
    @Test
    void shouldCarryMoreThanAHundredBitsOfEntropy() {
        assertThat(AccessCode.ALPHABET).hasSize(32);
        assertThat(AccessCode.ALPHABET.chars().distinct().count()).isEqualTo(32);
        assertThat(AccessCode.LENGTH * 5).isEqualTo(125).isGreaterThanOrEqualTo(100);
    }

    @Test
    void shouldNotDrawTheSameCodeTwice() {
        Set<String> seen = new HashSet<>();
        for (int draw = 0; draw < 1000; draw++) {
            assertThat(seen.add(AccessCode.generate(random).symbols())).isTrue();
        }
    }

    @Test
    void shouldBeWrittenInFiveGroupsSeparatedByDashes() {
        assertThat(new AccessCode(SYMBOLS).formatted()).isEqualTo(FORMATTED);
        assertThat(AccessCode.generate(random).formatted()).matches("([2-9A-HJ-NP-Z]{5}-){4}[2-9A-HJ-NP-Z]{5}");
    }

    @Test
    void shouldReadACodeWhateverItsCaseSpacesAndDashes() {
        AccessCode expected = new AccessCode(SYMBOLS);

        assertThat(AccessCode.parse(FORMATTED)).contains(expected);
        assertThat(AccessCode.parse(SYMBOLS)).contains(expected);
        assertThat(AccessCode.parse("  k7qw2-m9xdf-4tlpa-8rbnc-h3vye\n")).contains(expected);
        assertThat(AccessCode.parse("k7qw2 m9xdf 4tlpa 8rbnc h3vye")).contains(expected);
        // Un tiret long, collé depuis un traitement de texte.
        assertThat(AccessCode.parse("K7QW2–M9XDF—4TLPA-8RBNC-H3VYE")).contains(expected);
    }

    @Test
    void shouldReadNothingFromWhatIsNotACode() {
        assertThat(AccessCode.parse("")).isEmpty();
        assertThat(AccessCode.parse("K7QW2-M9XDF-4TLPA-8RBNC")).isEmpty();
        assertThat(AccessCode.parse(FORMATTED + "A")).isEmpty();
        // Un zéro et un O : aucun des deux n'est dans l'alphabet.
        assertThat(AccessCode.parse("07QW2-M9XDF-4TLPA-8RBNC-H3VYE")).isEmpty();
        assertThat(AccessCode.parse("O7QW2-M9XDF-4TLPA-8RBNC-H3VYE")).isEmpty();
        assertThat(AccessCode.parse(" ".repeat(AccessCode.MAX_INPUT_LENGTH) + FORMATTED))
                .isEmpty();
        assertThatThrownBy(() -> new AccessCode("k7qw2m9xdf4tlpa8rbnch3vye"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldKeepOnlyAFingerprintAndTheLastCharacters() {
        AccessCode code = new AccessCode(SYMBOLS);

        assertThat(code.fingerprint())
                .matches("[0-9a-f]{64}")
                .isEqualTo(
                        AccessCode.parse(FORMATTED.toLowerCase()).orElseThrow().fingerprint())
                .isNotEqualTo(new AccessCode("K7QW2M9XDF4TLPA8RBNCH3VYF").fingerprint())
                .doesNotContainIgnoringCase(SYMBOLS);
        assertThat(code.suffix()).isEqualTo("3VYE").hasSize(AccessCode.SUFFIX_LENGTH);
    }

    @Test
    void shouldNeverShowTheCodeInAMessage() {
        AccessCode code = new AccessCode(SYMBOLS);

        assertThat(code.toString()).doesNotContain(SYMBOLS).doesNotContain("3VYE");
        assertThat("Code : " + code).isEqualTo("Code : AccessCode[***]");
    }
}
