import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class TransactionTest {

    @Test
    void uneTransactionValideeNEstPasAnnulee() {
        Journal journal = new Journal();

        try (Transaction t = new Transaction(journal)) {
            t.valider();
        }

        assertEquals(List.of("ouverte", "validée"), journal.evenements());
    }

    @Test
    void uneTransactionNonValideeEstAnnuleeALaFermeture() {
        Journal journal = new Journal();

        try (Transaction t = new Transaction(journal)) {
            // aucun valider()
        }

        assertEquals(List.of("ouverte", "annulée"), journal.evenements());
    }

    @Test
    void uneExceptionAnnuleLaTransactionEtRemonte() {
        Journal journal = new Journal();

        assertThrows(IllegalStateException.class, () -> {
            try (Transaction t = new Transaction(journal)) {
                throw new IllegalStateException("virement refusé");
            }
        });

        assertEquals(List.of("ouverte", "annulée"), journal.evenements());
    }

    @Test
    void fermerDeuxFoisNAnnuleQuUneFois() {
        Journal journal = new Journal();
        Transaction t = new Transaction(journal);

        t.close();
        t.close();

        assertEquals(List.of("ouverte", "annulée"), journal.evenements());
    }
}
