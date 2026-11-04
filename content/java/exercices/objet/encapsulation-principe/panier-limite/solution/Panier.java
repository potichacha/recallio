import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Panier {

    private static final int TAILLE_MAX = 10;

    private final List<String> articles = new ArrayList<>();

    public void ajouter(String article) {
        if (article == null || article.isBlank()) {
            throw new IllegalArgumentException("article vide");
        }
        if (articles.size() == TAILLE_MAX) {
            throw new IllegalStateException("panier plein : " + TAILLE_MAX + " articles au plus");
        }
        articles.add(article);
    }

    public boolean retirer(String article) {
        return articles.remove(article);
    }

    public List<String> articles() {
        return Collections.unmodifiableList(articles);
    }

    public int taille() {
        return articles.size();
    }
}
