package boutique;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Le stock, en mémoire. Il lève des exceptions métier, sans rien savoir de HTTP. */
@ApplicationScoped
public class Stock {

    private record Article(String nom, int quantite) {}

    private final Map<Long, Article> articles = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public long ajouter(String nom, int quantite) {
        long id = sequence.incrementAndGet();
        articles.put(id, new Article(nom, quantite));
        return id;
    }

    public synchronized void retirer(long produitId, int quantite) {
        Article article = articles.get(produitId);
        if (article == null) {
            throw new ProduitIntrouvableException(produitId);
        }
        if (article.quantite() < quantite) {
            throw new StockInsuffisantException(article.nom(), quantite, article.quantite());
        }
        articles.put(produitId, new Article(article.nom(), article.quantite() - quantite));
    }
}
