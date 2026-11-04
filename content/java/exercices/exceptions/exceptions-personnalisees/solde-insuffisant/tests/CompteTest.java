import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CompteTest {

    @Test
    void unRetraitCouvertDiminueLeSolde() {
        Compte compte = new Compte(100);

        compte.retirer(30);

        assertEquals(70, compte.solde());
    }

    @Test
    void unRetraitTropGrosLeveSoldeInsuffisantAvecSesDonnees() {
        Compte compte = new Compte(20);

        SoldeInsuffisant e = assertThrows(SoldeInsuffisant.class, () -> compte.retirer(50));

        assertEquals(20, e.solde());
        assertEquals(50, e.montant());
        assertEquals(30, e.manque());
    }

    @Test
    void leMessageDitCeQuiNeVaPas() {
        SoldeInsuffisant e = assertThrows(SoldeInsuffisant.class, () -> new Compte(20).retirer(50));

        assertEquals("solde 20.0 insuffisant pour un retrait de 50.0", e.getMessage());
    }

    @Test
    void unRetraitRefuseNeChangePasLeSolde() {
        Compte compte = new Compte(20);

        assertThrows(SoldeInsuffisant.class, () -> compte.retirer(50));

        assertEquals(20, compte.solde());
    }

    @Test
    void lExceptionEstNonVerifiee() {
        assertInstanceOf(RuntimeException.class, new SoldeInsuffisant(1, 2));
    }
}
