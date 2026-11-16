package boutique;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CaisseTest {

    @Inject
    Caisse caisse;

    @Test
    void additionneLesPrixDuCatalogue() {
        assertEquals(7, caisse.total(List.of("stylo", "cahier"), false));
    }

    @Test
    void appliqueLaRemiseDeFideliteConfiguree() {
        assertEquals(36, caisse.total(List.of("cartable", "cahier", "cahier"), true));
    }
}
