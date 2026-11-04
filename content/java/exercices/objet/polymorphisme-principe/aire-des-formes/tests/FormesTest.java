import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class FormesTest {

    private static final double PRECISION = 1e-9;

    @Test
    void chaqueFormeCalculeSonAire() {
        assertEquals(Math.PI * 4, new Cercle(2).aire(), PRECISION);
        assertEquals(12, new Rectangle(3, 4).aire(), PRECISION);
        assertEquals(10, new Triangle(5, 4).aire(), PRECISION);
    }

    @Test
    void laireTotaleAdditionneDesFormesDeTypesDifferents() {
        List<Forme> formes = List.of(new Rectangle(2, 3), new Triangle(4, 1), new Cercle(1));

        assertEquals(6 + 2 + Math.PI, Formes.aireTotale(formes), PRECISION);
    }

    @Test
    void laireTotaleAccepteUneFormeInconnueDeFormes() {
        Forme carreUnite = () -> 1;

        assertEquals(2, Formes.aireTotale(List.of(carreUnite, carreUnite)), PRECISION);
    }

    @Test
    void laireTotaleDUneListeVideVautZero() {
        assertEquals(0, Formes.aireTotale(List.of()), PRECISION);
    }
}
