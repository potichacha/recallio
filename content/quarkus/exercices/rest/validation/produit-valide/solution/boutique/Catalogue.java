package boutique;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Le catalogue, en mémoire. */
@ApplicationScoped
public class Catalogue {

    private final Map<Long, Produit> produits = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public List<Produit> premiers(int taille) {
        return produits.values().stream().sorted(Comparator.comparingLong(Produit::id)).limit(taille).toList();
    }

    public Produit ajouter(NouveauProduit nouveau) {
        Produit produit = new Produit(sequence.incrementAndGet(), nouveau.nom(), nouveau.prix(), nouveau.reference());
        produits.put(produit.id(), produit);
        return produit;
    }
}
