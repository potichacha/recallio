package banque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class VirementServiceTest {

    @Inject
    VirementService virements;

    Long alice;
    Long bob;
    Long chloe;

    @BeforeEach
    void ouvrirLesComptes() {
        alice = virements.ouvrir("Alice", 100);
        bob = virements.ouvrir("Bob", 50);
        chloe = virements.ouvrir("Chloé", 0);
    }

    int soldeEnBase(Long id) {
        return QuarkusTransaction.requiringNew().call(() -> Compte.<Compte>findById(id).solde);
    }

    @Test
    void unVirementDeplaceLArgent() {
        virements.virer(alice, bob, 30);

        assertEquals(70, soldeEnBase(alice));
        assertEquals(80, soldeEnBase(bob));
    }

    @Test
    void unVirementSansProvisionNeChangeAucunSolde() {
        assertThrows(SoldeInsuffisantException.class, () -> virements.virer(bob, alice, 80));

        assertEquals(50, soldeEnBase(bob));
        assertEquals(100, soldeEnBase(alice));
    }

    @Test
    void unLotReussiAppliqueTousSesVirements() {
        virements.virerLot(List.of(new Virement(alice, chloe, 40), new Virement(bob, chloe, 20)));

        assertEquals(60, soldeEnBase(alice));
        assertEquals(30, soldeEnBase(bob));
        assertEquals(60, soldeEnBase(chloe));
    }

    @Test
    void unLotQuiEchoueNAppliqueAucunDeSesVirements() {
        List<Virement> lot = List.of(new Virement(alice, chloe, 40), new Virement(bob, chloe, 20),
                new Virement(chloe, alice, 500));

        assertThrows(SoldeInsuffisantException.class, () -> virements.virerLot(lot));

        assertEquals(100, soldeEnBase(alice));
        assertEquals(50, soldeEnBase(bob));
        assertEquals(0, soldeEnBase(chloe));
    }
}
