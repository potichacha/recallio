import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

class ExportTest {

    @Test
    void uneFactureEstUnDocumentDUnePage() {
        Facture facture = new Facture("F42", 120.5);

        assertEquals("Facture F42", facture.titre());
        assertEquals(1, facture.pages());
    }

    @Test
    void uneFactureSExporteEnCsv() {
        Facture facture = new Facture("F42", 120.5);

        assertEquals("F42;120.5", facture.exporter());
        assertEquals("facture-F42.csv", facture.nomFichier());
    }

    @Test
    void unePhotoSExporteAvecLeNomDeFichierParDefaut() {
        Photo photo = new Photo("plage.jpg");

        assertEquals("photo:plage.jpg", photo.exporter());
        assertEquals("export.txt", photo.nomFichier());
    }

    @Test
    void unePhotoNEstPasUnDocument() {
        Object photo = new Photo("plage.jpg");

        assertFalse(photo instanceof Document);
    }

    @Test
    void factureEtPhotoSExportentDeLaMemeFacon() {
        List<Exportable> exports = List.of(new Facture("F1", 10.0), new Photo("chat.png"));

        assertEquals(List.of("F1;10.0", "photo:chat.png"), exports.stream().map(Exportable::exporter).toList());
    }
}
