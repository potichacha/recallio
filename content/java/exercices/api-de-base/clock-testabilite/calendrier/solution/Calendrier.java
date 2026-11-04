import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

public class Calendrier {

    private final Clock clock;

    public Calendrier(Clock clock) {
        this.clock = clock;
    }

    public int age(LocalDate naissance) {
        return Period.between(naissance, aujourdhui()).getYears();
    }

    public long joursAvant(LocalDate date) {
        return ChronoUnit.DAYS.between(aujourdhui(), date);
    }

    public boolean estWeekEnd() {
        DayOfWeek jour = aujourdhui().getDayOfWeek();
        return jour == DayOfWeek.SATURDAY || jour == DayOfWeek.SUNDAY;
    }

    private LocalDate aujourdhui() {
        return LocalDate.now(clock);
    }
}
