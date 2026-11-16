package banque;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Compte {

    @Id
    @GeneratedValue
    private Long id;

    private String titulaire;
    private int solde;

    protected Compte() {}

    public Compte(String titulaire, int solde) {
        this.titulaire = titulaire;
        this.solde = solde;
    }

    public Long getId() {
        return id;
    }

    public int getSolde() {
        return solde;
    }

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
