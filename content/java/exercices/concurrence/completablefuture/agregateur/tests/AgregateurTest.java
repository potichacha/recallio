import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AgregateurTest {

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @AfterEach
    void arreter() {
        executor.shutdownNow();
    }

    /** Chaque service attend que les trois soient appelés en même temps : un appel séquentiel échoue. */
    private static final class ServicesSimultanes implements Services {
        private final CyclicBarrier barriere = new CyclicBarrier(3);
        private final boolean nomEnPanne;
        private final boolean commandesEnPanne;
        private final boolean pointsEnPanne;

        ServicesSimultanes(boolean nomEnPanne, boolean commandesEnPanne, boolean pointsEnPanne) {
            this.nomEnPanne = nomEnPanne;
            this.commandesEnPanne = commandesEnPanne;
            this.pointsEnPanne = pointsEnPanne;
        }

        private void attendreLesAutres() {
            try {
                barriere.await(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException("les trois appels ne sont pas simultanés", e);
            }
        }

        @Override public String nom(String id) {
            attendreLesAutres();
            if (nomEnPanne) throw new IllegalStateException("service de nom en panne");
            return "Ada";
        }

        @Override public List<String> commandes(String id) {
            attendreLesAutres();
            if (commandesEnPanne) throw new IllegalStateException("service de commandes en panne");
            return List.of("c1", "c2");
        }

        @Override public int points(String id) {
            attendreLesAutres();
            if (pointsEnPanne) throw new IllegalStateException("service de points en panne");
            return 120;
        }
    }

    @Test
    void agregeLesTroisServicesEnParallele() {
        Agregateur agregateur = new Agregateur(new ServicesSimultanes(false, false, false), executor);

        assertEquals(new Profil("Ada", List.of("c1", "c2"), 120), agregateur.profil("42").join());
    }

    @Test
    void remplaceLesCommandesEnPanneParUneListeVide() {
        Agregateur agregateur = new Agregateur(new ServicesSimultanes(false, true, false), executor);

        assertEquals(new Profil("Ada", List.of(), 120), agregateur.profil("42").join());
    }

    @Test
    void remplaceLesPointsEnPanneParZero() {
        Agregateur agregateur = new Agregateur(new ServicesSimultanes(false, false, true), executor);

        assertEquals(new Profil("Ada", List.of("c1", "c2"), 0), agregateur.profil("42").join());
    }

    @Test
    void echoueSiLeNomEstIndisponible() {
        Agregateur agregateur = new Agregateur(new ServicesSimultanes(true, false, false), executor);

        CompletionException erreur = assertThrows(CompletionException.class, () -> agregateur.profil("42").join());
        assertInstanceOf(IllegalStateException.class, erreur.getCause());
    }
}
