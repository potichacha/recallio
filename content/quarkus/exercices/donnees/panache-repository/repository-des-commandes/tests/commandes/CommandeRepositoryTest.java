package commandes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestTransaction
class CommandeRepositoryTest {

    @Inject
    CommandeRepository commandes;

    @Test
    void duClientRenvoieSesCommandesDansLOrdreDesIdentifiants() {
        Commande premiere = new Commande("Alice", 30);
        Commande autre = new Commande("Bob", 12);
        Commande seconde = new Commande("Alice", 45);
        commandes.persist(premiere);
        commandes.persist(autre);
        commandes.persist(seconde);

        List<Long> identifiants = commandes.duClient("Alice").stream().map(commande -> commande.id).toList();

        assertEquals(List.of(premiere.id, seconde.id), identifiants);
    }

    @Test
    void nombreEnAttenteIgnoreLesCommandesExpediees() {
        long avant = commandes.nombreEnAttente();
        Commande expediee = new Commande("Chloé", 20);
        expediee.statut = Statut.EXPEDIEE;
        commandes.persist(new Commande("Chloé", 10));
        commandes.persist(new Commande("David", 15));
        commandes.persist(expediee);

        assertEquals(avant + 2, commandes.nombreEnAttente());
    }
}
