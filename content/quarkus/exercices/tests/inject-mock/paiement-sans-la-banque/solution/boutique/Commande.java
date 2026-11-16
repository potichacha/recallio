package boutique;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
public class Commande extends PanacheEntity {

    public enum Statut { A_PAYER, PAYEE, REFUSEE }

    public String carte;
    public int montant;

    @Enumerated(EnumType.STRING)
    public Statut statut = Statut.A_PAYER;

    public String transaction;
}
