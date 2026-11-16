package bibliotheque;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class Livre extends PanacheEntity {
    public String titre;
    public String auteur;
    public int annee;
}
