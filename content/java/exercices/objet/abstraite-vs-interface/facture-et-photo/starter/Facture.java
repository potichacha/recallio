public class Facture extends Document implements Exportable {

    public Facture(String numero, double montant) {
        super("À écrire");
    }

    @Override
    public int pages() {
        return 0; // À écrire.
    }

    @Override
    public String exporter() {
        return ""; // À écrire.
    }
}
