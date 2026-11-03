import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MoyenneTest {

    private static final double PRECISION = 1e-9;

    @Test
    void gardeLesDecimales() {
        assertEquals(12.5, Moyenne.de(new int[] {10, 15}), PRECISION);
    }

    @Test
    void calculeUneMoyenneQuiNeTombePasJuste() {
        assertEquals(11.0 / 3, Moyenne.de(new int[] {3, 4, 4}), PRECISION);
    }

    @Test
    void renvoieLaNoteQuandIlNyEnAQuUne() {
        assertEquals(12.0, Moyenne.de(new int[] {12}), PRECISION);
    }

    @Test
    void renvoieZeroSansAucuneNote() {
        assertEquals(0.0, Moyenne.de(new int[] {}), PRECISION);
    }
}
