import java.util.ArrayList;
import java.util.List;

public class Panier {

    public List<String> articles = new ArrayList<>();

    public void ajouter(String article) {
        articles.add(article);
    }

    public boolean retirer(String article) {
        return articles.remove(article);
    }

    public List<String> articles() {
        return articles;
    }

    public int taille() {
        return articles.size();
    }
}
