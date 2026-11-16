package bibliotheque;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Livre {

    @Id
    @GeneratedValue
    private Long id;

    private String titre;
    private String auteur;
    private int annee;

    protected Livre() {}

    public Livre(String titre, String auteur, int annee) {
        this.titre = titre;
        this.auteur = auteur;
        this.annee = annee;
    }

    public Long getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public String getAuteur() {
        return auteur;
    }

    public int getAnnee() {
        return annee;
    }
}
