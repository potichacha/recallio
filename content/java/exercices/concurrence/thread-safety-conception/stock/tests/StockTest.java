import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class StockTest {

    private static void enParallele(int taches, Runnable action) throws InterruptedException {
        CountDownLatch depart = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            for (int i = 0; i < taches; i++) {
                pool.submit(() -> {
                    depart.await();
                    action.run();
                    return null;
                });
            }
            depart.countDown();
        }
    }

    @Test
    void ajouteEtRetireSansConcurrence() {
        Stock stock = new Stock();
        stock.ajouter("pomme", 5);

        assertTrue(stock.retirer("pomme", 3));
        assertFalse(stock.retirer("pomme", 3));
        assertEquals(2, stock.quantite("pomme"));
        assertEquals(0, stock.quantite("kiwi"));
        assertEquals(2, stock.total());
    }

    @Test
    void nePerdAucunAjoutConcurrent() throws InterruptedException {
        Stock stock = new Stock();

        enParallele(10_000, () -> stock.ajouter("pomme", 1));

        assertEquals(10_000, stock.quantite("pomme"));
        assertEquals(10_000, stock.total());
    }

    @Test
    void neDescendJamaisSousZero() throws InterruptedException {
        Stock stock = new Stock();
        stock.ajouter("kiwi", 1_000);
        AtomicInteger reussis = new AtomicInteger();

        enParallele(5_000, () -> {
            if (stock.retirer("kiwi", 1)) {
                reussis.incrementAndGet();
            }
        });

        assertEquals(1_000, reussis.get());
        assertEquals(0, stock.quantite("kiwi"));
        assertEquals(0, stock.total());
    }

    @Test
    void gardeUnTotalCoherentAvecLesQuantites() throws InterruptedException {
        Stock stock = new Stock();

        enParallele(6_000, () -> {
            int n = (int) (Thread.currentThread().threadId() % 3);
            String produit = "p" + n;
            stock.ajouter(produit, 2);
            stock.retirer(produit, 1);
        });

        assertEquals(6_000, stock.total());
        assertEquals(stock.total(), stock.quantite("p0") + stock.quantite("p1") + stock.quantite("p2"));
    }
}
