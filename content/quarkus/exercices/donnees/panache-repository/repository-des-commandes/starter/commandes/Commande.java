package commandes;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Commande {

    @Id
    @GeneratedValue
    public Long id;

    public String client;
    public int montant;

    @Enumerated(EnumType.STRING)
    public Statut statut = Statut.EN_ATTENTE;

    public Commande() {}

    public Commande(String client, int montant) {
        this.client = client;
        this.montant = montant;
    }
}
