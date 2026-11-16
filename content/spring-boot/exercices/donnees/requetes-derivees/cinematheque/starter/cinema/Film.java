package cinema;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Film {

    @Id
    @GeneratedValue
    private Long id;

    private String titre;
    private String realisateur;
    private int annee;
    private int dureeMinutes;
    private String genre;

    protected Film() {}

    public Film(String titre, String realisateur, int annee, int dureeMinutes, String genre) {
        this.titre = titre;
        this.realisateur = realisateur;
        this.annee = annee;
        this.dureeMinutes = dureeMinutes;
        this.genre = genre;
    }

    public Long getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public String getRealisateur() {
        return realisateur;
    }

    public int getAnnee() {
        return annee;
    }

    public int getDureeMinutes() {
        return dureeMinutes;
    }

    public String getGenre() {
        return genre;
    }
}
