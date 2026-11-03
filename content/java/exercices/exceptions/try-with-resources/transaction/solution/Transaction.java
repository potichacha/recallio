public class Transaction implements AutoCloseable {

    private final Journal journal;
    private boolean validee;
    private boolean fermee;

    public Transaction(Journal journal) {
        this.journal = journal;
        journal.noter("ouverte");
    }

    public void valider() {
        validee = true;
        journal.noter("validée");
    }

    @Override
    public void close() {
        if (fermee) {
            return;
        }
        fermee = true;
        if (!validee) {
            journal.noter("annulée");
        }
    }
}
