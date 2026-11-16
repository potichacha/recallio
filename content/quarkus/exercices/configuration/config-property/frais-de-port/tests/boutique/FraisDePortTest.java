package boutique;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(FraisDePortTest.LivraisonEnBelgique.class)
class FraisDePortTest {

    public static class LivraisonEnBelgique implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("boutique.livraison.seuil-gratuit", "30", "boutique.livraison.pays", "FR,BE");
        }
    }

    @Inject
    FraisDePort fraisDePort;

    @Test
    void offreLaLivraisonDesLeSeuilConfigure() {
        assertEquals(0, fraisDePort.calculer(30));
        assertEquals(0, fraisDePort.calculer(45));
    }

    @Test
    void appliqueLeForfaitParDefautSousLeSeuil() {
        assertEquals(5, fraisDePort.calculer(29));
    }

    @Test
    void livreLesPaysConfigures() {
        assertTrue(fraisDePort.livrable("FR"));
        assertTrue(fraisDePort.livrable("BE"));
        assertFalse(fraisDePort.livrable("CH"));
    }
}
