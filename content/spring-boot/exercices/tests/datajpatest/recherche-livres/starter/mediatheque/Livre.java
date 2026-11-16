package mediatheque;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Livre {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String isbn;

    private String titre;

    private String auteur;

    private int annee;

    private boolean disponible;

    protected Livre() {
    }

    public Livre(String isbn, String titre, String auteur, int annee, boolean disponible) {
        this.isbn = isbn;
        this.titre = titre;
        this.auteur = auteur;
        this.annee = annee;
        this.disponible = disponible;
    }

    public Long getId() {
        return id;
    }

    public String getIsbn() {
        return isbn;
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

    public boolean isDisponible() {
        return disponible;
    }
}
