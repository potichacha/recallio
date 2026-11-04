public class Photo implements Exportable {

    private final String nom;

    public Photo(String nom) {
        this.nom = nom;
    }

    @Override
    public String exporter() {
        return "photo:" + nom;
    }
}
