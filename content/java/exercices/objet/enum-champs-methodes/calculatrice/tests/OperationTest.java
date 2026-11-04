import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OperationTest {

    @Test
    void chaqueOperationCalculeSonResultat() {
        assertEquals(7, Operation.PLUS.appliquer(4, 3));
        assertEquals(1, Operation.MOINS.appliquer(4, 3));
        assertEquals(12, Operation.FOIS.appliquer(4, 3));
        assertEquals(2, Operation.DIVISE.appliquer(7, 3));
    }

    @Test
    void onRetrouveChaqueOperationParSonSymbole() {
        for (Operation operation : Operation.values()) {
            assertEquals(operation, Operation.deSymbole(operation.symbole()));
        }
        assertEquals(Operation.FOIS, Operation.deSymbole("*"));
    }

    @Test
    void unSymboleInconnuEstRefuse() {
        assertThrows(IllegalArgumentException.class, () -> Operation.deSymbole("%"));
    }

    @Test
    void symboleEtCalculSEnchainent() {
        assertEquals(20, Operation.deSymbole("*").appliquer(Operation.deSymbole("+").appliquer(2, 3), 4));
    }
}
