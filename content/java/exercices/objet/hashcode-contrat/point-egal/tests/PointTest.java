import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PointTest {

    @Test
    void deuxPointsDeMemesCoordonneesSontEgaux() {
        assertEquals(new Point(1, 2), new Point(1, 2));
    }

    @Test
    void desCoordonneesDifferentesDonnentDesPointsDifferents() {
        assertNotEquals(new Point(1, 2), new Point(2, 1));
    }

    @Test
    void deuxPointsEgauxOntLeMemeHashCode() {
        assertEquals(new Point(3, 4).hashCode(), new Point(3, 4).hashCode());
    }

    @Test
    void unHashSetNeGardeQuUnPointParCoordonnees() {
        Set<Point> points = new HashSet<>();
        points.add(new Point(1, 2));
        points.add(new Point(1, 2));

        assertEquals(1, points.size());
    }

    @Test
    void unPointNEstEgalNiANullNiAUnObjetDUnAutreType() {
        Point point = new Point(1, 2);

        assertFalse(point.equals(null));
        assertFalse(point.equals("(1, 2)"));
    }
}
