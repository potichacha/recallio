package banque;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class Compte extends PanacheEntity {

    public String titulaire;
    public int solde;

    public void crediter(int montant) {
        solde += montant;
    }

    public void debiter(int montant) {
        if (montant > solde) {
            throw new SoldeInsuffisantException(titulaire, solde, montant);
        }
        solde -= montant;
    }
}
