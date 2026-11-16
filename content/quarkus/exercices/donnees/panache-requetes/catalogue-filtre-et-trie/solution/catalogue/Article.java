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
        return list("categorie", Sort.by("prix"), categorie);
    }

    public static List<Article> entre(int min, int max) {
        return list("prix >= :min and prix <= :max", Sort.by("prix").and("nom"),
                Parameters.with("min", min).and("max", max));
    }

    public static long nombreEnRupture() {
        return count("stock", 0);
    }

    public static Optional<Article> leMoinsCher(String categorie) {
        return find("categorie", Sort.by("prix"), categorie).firstResultOptional();
    }
}
