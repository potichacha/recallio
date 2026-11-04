import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class InscriptionTest {

    @Test
    void uneInscriptionValideNeLevePasDException() {
        assertDoesNotThrow(() -> Inscription.verifier("ada@exemple.fr", 36));
        assertDoesNotThrow(() -> Inscription.verifier("ada@exemple.fr", 13));
    }

    @Test
    void unEmailSansArobaseEstRefuseAvecSaValeur() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Inscription.verifier("ada.exemple.fr", 36));

        assertEquals("email invalide : ada.exemple.fr", e.getMessage());
    }

    @Test
    void unEmailNullEstRefuse() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Inscription.verifier(null, 36));

        assertEquals("email invalide : null", e.getMessage());
    }

    @Test
    void unAgeTropJeuneEstRefuse() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Inscription.verifier("ada@exemple.fr", 12));

        assertEquals("âge minimum 13 ans : 12", e.getMessage());
    }

    @Test
    void unAgeInvraisemblableEstRefuse() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Inscription.verifier("ada@exemple.fr", 150));

        assertEquals("âge invraisemblable : 150", e.getMessage());
    }

    @Test
    void lEmailEstVerifieAvantLAge() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Inscription.verifier("invalide", 5));

        assertEquals("email invalide : invalide", e.getMessage());
    }
}
