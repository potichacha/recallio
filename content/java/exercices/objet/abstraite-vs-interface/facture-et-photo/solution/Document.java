public abstract class Document {

    private final String titre;

    protected Document(String titre) {
        if (titre == null || titre.isBlank()) {
            throw new IllegalArgumentException("titre obligatoire");
        }
        this.titre = titre;
    }

    public String titre() {
        return titre;
    }

    public abstract int pages();
}
