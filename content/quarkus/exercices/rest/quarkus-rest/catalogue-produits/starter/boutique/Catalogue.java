package boutique;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Le catalogue, en mémoire. */
@ApplicationScoped
public class Catalogue {

    private final Map<Long, Produit> produits = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public List<Produit> tous() {
        return produits.values().stream().sorted(Comparator.comparingLong(Produit::id)).toList();
    }

    public Optional<Produit> trouver(long id) {
        return Optional.ofNullable(produits.get(id));
    }

    public Produit ajouter(NouveauProduit nouveau) {
        Produit produit = new Produit(sequence.incrementAndGet(), nouveau.nom(), nouveau.prix());
        produits.put(produit.id(), produit);
        return produit;
    }

    /** Retire le produit ; renvoie {@code false} s'il n'existait pas. */
    public boolean retirer(long id) {
        return produits.remove(id) != null;
    }
}
