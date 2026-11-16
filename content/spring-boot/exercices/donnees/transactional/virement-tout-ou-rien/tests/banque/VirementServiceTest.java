package banque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VirementServiceTest {

    @Autowired
    VirementService virements;

    @Autowired
    CompteRepository comptes;

    Long alice;
    Long bob;
    Long chloe;

    @BeforeEach
    void ouvrirLesComptes() {
        comptes.deleteAll();
        alice = comptes.save(new Compte("Alice", 100)).getId();
        bob = comptes.save(new Compte("Bob", 50)).getId();
        chloe = comptes.save(new Compte("Chloé", 0)).getId();
    }

    int solde(Long id) {
        return comptes.findById(id).orElseThrow().getSolde();
    }

    @Test
    void unVirementDeplaceLArgent() {
        virements.virer(alice, bob, 30);

        assertThat(solde(alice)).isEqualTo(70);
        assertThat(solde(bob)).isEqualTo(80);
    }

    @Test
    void unVirementSansProvisionNeChangeAucunSolde() {
        assertThatThrownBy(() -> virements.virer(bob, alice, 80)).isInstanceOf(SoldeInsuffisantException.class);

        assertThat(solde(bob)).isEqualTo(50);
        assertThat(solde(alice)).isEqualTo(100);
    }

    @Test
    void unLotReussiAppliqueTousSesVirements() {
        virements.virerLot(List.of(new Virement(alice, chloe, 40), new Virement(bob, chloe, 20)));

        assertThat(solde(alice)).isEqualTo(60);
        assertThat(solde(bob)).isEqualTo(30);
        assertThat(solde(chloe)).isEqualTo(60);
    }

    @Test
    void unLotQuiEchoueNAppliqueAucunDeSesVirements() {
        List<Virement> lot = List.of(new Virement(alice, chloe, 40), new Virement(bob, chloe, 20),
                new Virement(chloe, alice, 500));

        assertThatThrownBy(() -> virements.virerLot(lot)).isInstanceOf(SoldeInsuffisantException.class);

        assertThat(solde(alice)).isEqualTo(100);
        assertThat(solde(bob)).isEqualTo(50);
        assertThat(solde(chloe)).isEqualTo(0);
    }
}
