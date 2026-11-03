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
        return LocalDate.now().isAfter(fin); // Ignore l'horloge : à corriger.
    }

    public long joursRestants() {
        return ChronoUnit.DAYS.between(LocalDate.now(), fin); // À corriger.
    }
}
