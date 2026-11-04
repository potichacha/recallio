public class CompteBancaire {

    private final String titulaire;
    private double solde;

    public CompteBancaire(String titulaire, double soldeInitial) {
        // À écrire : valider, puis affecter les champs.
        this.titulaire = titulaire;
    }

    public CompteBancaire(String titulaire) {
        // À écrire : ouvrir le compte à 0 sans dupliquer la validation.
        this.titulaire = titulaire;
    }

    public void deposer(double montant) {
        // À écrire.
    }

    public String titulaire() {
        return titulaire;
    }

    public double solde() {
        return solde;
    }
}
