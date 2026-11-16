package boutique;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class Produit extends PanacheEntity {
    public String nom;
    public int prix;
}
