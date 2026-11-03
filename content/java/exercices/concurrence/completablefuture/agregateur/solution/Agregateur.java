import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class Agregateur {

    private final Services services;
    private final Executor executor;

    public Agregateur(Services services, Executor executor) {
        this.services = services;
        this.executor = executor;
    }

    public CompletableFuture<Profil> profil(String id) {
        CompletableFuture<String> nom = CompletableFuture.supplyAsync(() -> services.nom(id), executor);
        CompletableFuture<List<String>> commandes = CompletableFuture
                .supplyAsync(() -> services.commandes(id), executor)
                .exceptionally(e -> List.of());
        CompletableFuture<Integer> points = CompletableFuture
                .supplyAsync(() -> services.points(id), executor)
                .exceptionally(e -> 0);

        return nom.thenCombine(commandes, (n, c) -> new Profil(n, c, 0))
                .thenCombine(points, (p, pts) -> new Profil(p.nom(), p.commandes(), pts));
    }
}
