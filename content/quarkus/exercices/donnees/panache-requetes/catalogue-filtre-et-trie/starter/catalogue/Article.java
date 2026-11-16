package catalogue;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.Entity;
import java.util.List;
import java.util.Optional;

@Entity
public class Article extends PanacheEntity {

    public String nom;
    public String categorie;
    public int prix;
    public int stock;

    public static List<Article> deCategorie(String categorie) {
        return List.of();
    }

    public static List<Article> entre(int min, int max) {
        return List.of();
    }

    public static long nombreEnRupture() {
        return 0;
    }

    public static Optional<Article> leMoinsCher(String categorie) {
        return Optional.empty();
    }
}
