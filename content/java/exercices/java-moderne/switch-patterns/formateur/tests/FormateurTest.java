import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class FormateurTest {

    @Test
    void formateNullParUnTiret() {
        assertEquals("-", Formateur.formater(null));
    }

    @Test
    void formateLesEntiers() {
        assertEquals("42", Formateur.formater(42));
        assertEquals("0", Formateur.formater(0));
        assertEquals("(12)", Formateur.formater(-12));
    }

    @Test
    void formateLesChaines() {
        assertEquals("ada", Formateur.formater("  ada "));
        assertEquals("-", Formateur.formater("   "));
        assertEquals("-", Formateur.formater(""));
    }

    @Test
    void formateLesBooleens() {
        assertEquals("oui", Formateur.formater(true));
        assertEquals("non", Formateur.formater(false));
    }

    @Test
    void formateLesListesRecursivement() {
        assertEquals("[1, (2), oui, -]", Formateur.formater(Arrays.asList(1, -2, true, null)));
        assertEquals("[[a], []]", Formateur.formater(List.of(List.of(" a"), List.of())));
    }

    @Test
    void formateLesAutresValeursParToString() {
        assertEquals("2026-01-15", Formateur.formater(LocalDate.of(2026, 1, 15)));
        assertEquals("2.5", Formateur.formater(2.5));
    }
}
