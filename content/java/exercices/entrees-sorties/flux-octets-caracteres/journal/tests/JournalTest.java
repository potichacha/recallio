import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JournalTest {

    private static final String JOURNAL = """
            INFO démarrage
            WARN disque à 80 %
            ERROR connexion refusée

            INFO requête traitée
            ERROR délai dépassé
            """;

    @Test
    void compteLesLignesParNiveauTriees() throws IOException {
        Map<String, Long> compteurs = Journal.compterParNiveau(new StringReader(JOURNAL));

        assertEquals(Map.of("ERROR", 2L, "INFO", 2L, "WARN", 1L), compteurs);
        assertEquals(List.of("ERROR", "INFO", "WARN"), List.copyOf(compteurs.keySet()));
    }

    @Test
    void unJournalVideNeDonneAucunNiveau() throws IOException {
        assertEquals(Map.of(), Journal.compterParNiveau(new StringReader("")));
    }

    @Test
    void copieSeulementLesErreurs() throws IOException {
        StringWriter cible = new StringWriter();

        int copiees = Journal.copierErreurs(new StringReader(JOURNAL), cible);

        assertEquals(2, copiees);
        assertEquals(List.of("ERROR connexion refusée", "ERROR délai dépassé"), cible.toString().lines().toList());
    }

    @Test
    void neCopieRienSansErreur() throws IOException {
        StringWriter cible = new StringWriter();

        assertEquals(0, Journal.copierErreurs(new StringReader("INFO ok\n"), cible));
        assertEquals("", cible.toString());
    }
}
