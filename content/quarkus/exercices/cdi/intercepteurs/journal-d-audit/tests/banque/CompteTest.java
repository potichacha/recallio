package banque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CompteTest {

    @Inject
    Compte compte;

    @Inject
    Journal journal;

    @BeforeEach
    void viderLeJournal() {
        journal.vider();
    }

    @Test
    void journaliseChaqueOperationReussie() {
        compte.deposer(50);

        assertEquals(List.of("deposer : ok"), journal.entrees());
    }

    @Test
    void journaliseLEchecPuisLaisseRemonterLException() {
        assertThrows(SoldeInsuffisantException.class, () -> compte.retirer(1_000_000));

        assertEquals(List.of("retirer : échec"), journal.entrees());
    }

    @Test
    void neJournalisePasLesMethodesNonAuditees() {
        compte.solde();

        assertTrue(journal.entrees().isEmpty());
    }

    @Test
    void renvoieLeResultatDeLaMethodeAuditee() {
        int avant = compte.solde();

        assertEquals(avant + 20, compte.deposer(20));
    }
}
