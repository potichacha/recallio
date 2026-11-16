package commandes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ExpeditionServiceTest {

    @InjectMock
    CommandeRepository commandes;

    @Inject
    ExpeditionService expedition;

    @Test
    void expedierPasseLaCommandeAuStatutExpediee() {
        Commande commande = new Commande("Alice", 30);
        when(commandes.findByIdOptional(7L)).thenReturn(Optional.of(commande));

        expedition.expedier(7L);

        assertEquals(Statut.EXPEDIEE, commande.statut);
    }

    @Test
    void expedierUneCommandeInconnueLeveCommandeIntrouvable() {
        when(commandes.findByIdOptional(404L)).thenReturn(Optional.empty());

        assertThrows(CommandeIntrouvableException.class, () -> expedition.expedier(404L));
    }
}
