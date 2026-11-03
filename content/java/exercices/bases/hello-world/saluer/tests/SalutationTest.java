import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SalutationTest {

    @Test
    void saluePersonnellementAvecLePrenom() {
        assertEquals("Bonjour, Ada !", Salutation.saluer("Ada"));
    }

    @Test
    void ignoreLesEspacesAutourDuPrenom() {
        assertEquals("Bonjour, Ada !", Salutation.saluer("  Ada "));
    }

    @Test
    void saluePoliementSansPrenom() {
        assertEquals("Bonjour !", Salutation.saluer(null));
    }

    @Test
    void saluePoliementAvecUnPrenomVide() {
        assertEquals("Bonjour !", Salutation.saluer("   "));
    }
}
