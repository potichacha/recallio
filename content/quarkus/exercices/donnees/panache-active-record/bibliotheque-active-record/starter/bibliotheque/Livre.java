package bibliotheque;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.Entity;
import java.util.List;
import java.util.Optional;

@Entity
public class Livre extends PanacheEntity {

    public String titre;
    public String auteur;
    public boolean disponible = true;

    public static List<Livre> parAuteur(String auteur) {
        return List.of();
    }

    public static Optional<Livre> parTitre(String titre) {
        return Optional.empty();
    }

    public static long nombreDisponibles() {
        return 0;
    }

    public void emprunter() {
    }
}
