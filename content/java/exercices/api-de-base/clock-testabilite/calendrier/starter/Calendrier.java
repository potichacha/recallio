import java.time.Clock;
import java.time.LocalDate;

public class Calendrier {

    private final Clock clock;

    public Calendrier(Clock clock) {
        this.clock = clock;
    }

    public int age(LocalDate naissance) {
        return LocalDate.now().getYear() - naissance.getYear();
    }

    public long joursAvant(LocalDate date) {
        return 0; // À écrire.
    }

    public boolean estWeekEnd() {
        return false; // À écrire.
    }
}
