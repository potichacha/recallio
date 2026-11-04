import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class Abonnement {

    private final LocalDate fin;
    private final Clock horloge;

    public Abonnement(LocalDate fin, Clock horloge) {
        this.fin = fin;
        this.horloge = horloge;
    }

    public boolean estExpire() {
        return aujourdhui().isAfter(fin);
    }

    public long joursRestants() {
        return Math.max(0, ChronoUnit.DAYS.between(aujourdhui(), fin));
    }

    private LocalDate aujourdhui() {
        return LocalDate.now(horloge);
    }
}
