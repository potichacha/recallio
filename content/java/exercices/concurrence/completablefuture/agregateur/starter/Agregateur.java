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
        return CompletableFuture.completedFuture(null); // À écrire.
    }
}
