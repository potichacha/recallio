import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class TableauxTest {

    @Test
    void inverseUnTableauDeLongueurImpaire() {
        int[] t = {1, 2, 3};
        Tableaux.inverser(t);
        assertArrayEquals(new int[] {3, 2, 1}, t);
    }

    @Test
    void inverseUnTableauDeLongueurPaire() {
        int[] t = {4, 8, 15, 16};
        Tableaux.inverser(t);
        assertArrayEquals(new int[] {16, 15, 8, 4}, t);
    }

    @Test
    void laisseUnTableauDUnSeulElement() {
        int[] t = {42};
        Tableaux.inverser(t);
        assertArrayEquals(new int[] {42}, t);
    }

    @Test
    void accepteUnTableauVide() {
        int[] t = {};
        Tableaux.inverser(t);
        assertArrayEquals(new int[] {}, t);
    }
}
