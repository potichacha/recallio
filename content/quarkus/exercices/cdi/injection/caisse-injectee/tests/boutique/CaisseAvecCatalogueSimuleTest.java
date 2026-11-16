package boutique;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
class CaisseAvecCatalogueSimuleTest {

    @InjectMock
    Catalogue catalogue;

    @Inject
    Caisse caisse;

    @Test
    void demandeLesPrixAuCatalogueInjecte() {
        Mockito.when(catalogue.prix("stylo")).thenReturn(10);

        assertEquals(20, caisse.total(List.of("stylo", "stylo"), false));
        Mockito.verify(catalogue, Mockito.times(2)).prix("stylo");
    }
}
