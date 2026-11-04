import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CompteBancaireTest {

    @Test
    void unCompteOuvertAvecUnSoldeLeGarde() {
        CompteBancaire compte = new CompteBancaire("Ada", 100);

        assertEquals("Ada", compte.titulaire());
        assertEquals(100, compte.solde());
    }

    @Test
    void unCompteOuvertSansSoldeCommenceAZero() {
        assertEquals(0, new CompteBancaire("Ada").solde());
    }

    @Test
    void unSoldeInitialNegatifEstRefuse() {
        assertThrows(IllegalArgumentException.class, () -> new CompteBancaire("Ada", -1));
    }

    @Test
    void unTitulaireNullOuVideEstRefuseParLesDeuxConstructeurs() {
        assertThrows(IllegalArgumentException.class, () -> new CompteBancaire(null, 10));
        assertThrows(IllegalArgumentException.class, () -> new CompteBancaire("  ", 10));
        assertThrows(IllegalArgumentException.class, () -> new CompteBancaire(null));
        assertThrows(IllegalArgumentException.class, () -> new CompteBancaire(""));
    }

    @Test
    void unDepotAugmenteLeSolde() {
        CompteBancaire compte = new CompteBancaire("Ada", 10);

        compte.deposer(5.5);

        assertEquals(15.5, compte.solde());
    }

    @Test
    void unDepotNegatifOuNulEstRefuse() {
        CompteBancaire compte = new CompteBancaire("Ada");

        assertThrows(IllegalArgumentException.class, () -> compte.deposer(0));
        assertThrows(IllegalArgumentException.class, () -> compte.deposer(-3));
        assertEquals(0, compte.solde());
    }
}
