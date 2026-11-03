public class CompteBancaire {

    private final String titulaire;
    private double solde;

    public CompteBancaire(String titulaire, double soldeInitial) {
        if (titulaire == null || titulaire.isBlank()) {
            throw new IllegalArgumentException("titulaire obligatoire");
        }
        if (soldeInitial < 0) {
            throw new IllegalArgumentException("solde initial négatif : " + soldeInitial);
        }
        this.titulaire = titulaire;
        this.solde = soldeInitial;
    }

    public CompteBancaire(String titulaire) {
        this(titulaire, 0);
    }

    public void deposer(double montant) {
        if (montant <= 0) {
            throw new IllegalArgumentException("montant non positif : " + montant);
        }
        solde += montant;
    }

    public String titulaire() {
        return titulaire;
    }

    public double solde() {
        return solde;
    }
}
