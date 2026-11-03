public class Facture extends Document implements Exportable {

    private final String numero;
    private final double montant;

    public Facture(String numero, double montant) {
        super("Facture " + numero);
        this.numero = numero;
        this.montant = montant;
    }

    @Override
    public int pages() {
        return 1;
    }

    @Override
    public String exporter() {
        return numero + ";" + montant;
    }

    @Override
    public String nomFichier() {
        return "facture-" + numero + ".csv";
    }
}
