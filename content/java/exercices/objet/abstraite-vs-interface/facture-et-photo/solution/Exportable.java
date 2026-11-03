public interface Exportable {

    String exporter();

    default String nomFichier() {
        return "export.txt";
    }
}
