public class Transaction implements AutoCloseable {

    private final Journal journal;

    public Transaction(Journal journal) {
        this.journal = journal;
        // À écrire.
    }

    public void valider() {
        // À écrire.
    }

    @Override
    public void close() {
        // À écrire.
    }
}
