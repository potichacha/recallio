package com.recallio.gamification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.recallio.gamification.domain.funnel.FunnelEventRepository;
import com.recallio.gamification.domain.funnel.FunnelReport;
import com.recallio.gamification.domain.funnel.FunnelReport.FunnelWeek;
import com.recallio.gamification.domain.funnel.FunnelReport.StepCount;
import com.recallio.gamification.domain.funnel.FunnelStep;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Le parcours mesuré (ADR 0064) : des semaines ISO en UTC, et le taux de passage d'une étape à la suivante. */
class FunnelServiceTest {

    /** Un mercredi : la semaine en cours va du lundi 16 au dimanche 22 novembre 2026. */
    private static final Instant NOW = Instant.parse("2026-11-18T10:00:00Z");

    private static final LocalDate THIS_MONDAY = LocalDate.of(2026, 11, 16);

    private final InMemoryEvents events = new InMemoryEvents();
    private final FunnelService service = new FunnelService(events, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void shouldMeasureTheEightLastIsoWeeksWithTheCurrentOneFirst() {
        List<FunnelWeek> weeks = service.report().weeks();

        assertThat(weeks).hasSize(FunnelReport.WEEKS);
        assertThat(weeks.getFirst().week().monday()).isEqualTo(THIS_MONDAY);
        assertThat(weeks.getFirst().week().sunday()).isEqualTo(LocalDate.of(2026, 11, 22));
        assertThat(weeks.getLast().week().monday()).isEqualTo(LocalDate.of(2026, 9, 28));
        assertThat(weeks.getFirst().steps())
                .extracting(StepCount::step)
                .containsExactly(FunnelStep.VISIT, FunnelStep.TRIAL, FunnelStep.ACCOUNT, FunnelStep.FIRST_REVIEW);
    }

    @Test
    void shouldCountEachEventInItsWeekOfTheUtcCalendar() {
        events.add(FunnelStep.VISIT, "2026-11-15T23:30:00Z"); // dimanche soir en UTC : la semaine précédente
        events.add(FunnelStep.VISIT, "2026-11-16T00:30:00Z"); // lundi en UTC, bien que dimanche à Montréal
        events.add(FunnelStep.VISIT, "2026-11-18T09:00:00Z");
        events.add(FunnelStep.VISIT, "2026-09-27T23:59:00Z"); // avant les huit semaines : jamais compté

        assertThat(service.report().weeks())
                .extracting(week -> count(week, FunnelStep.VISIT))
                .containsExactly(2L, 1L, 0L, 0L, 0L, 0L, 0L, 0L);
    }

    @Test
    void shouldGiveTheRateFromEachStepToTheNextRoundedDown() {
        events.add(FunnelStep.VISIT, 10);
        events.add(FunnelStep.TRIAL, 3);
        events.add(FunnelStep.ACCOUNT, 1);
        events.add(FunnelStep.FIRST_REVIEW, 1);

        assertThat(service.report().weeks().getFirst().steps())
                .extracting(StepCount::step, StepCount::count, StepCount::percent)
                .containsExactly(
                        tuple(FunnelStep.VISIT, 10L, null),
                        tuple(FunnelStep.TRIAL, 3L, 30),
                        tuple(FunnelStep.ACCOUNT, 1L, 33),
                        tuple(FunnelStep.FIRST_REVIEW, 1L, 100));
    }

    @Test
    void shouldGiveNoRateWhenNobodyReachedThePreviousStep() {
        // Deux essais sans visite comptée (un navigateur sans sessionStorage, ou une page ouverte la semaine d'avant).
        events.add(FunnelStep.TRIAL, 2);

        assertThat(service.report().weeks().getFirst().steps())
                .extracting(StepCount::count, StepCount::percent)
                .containsExactly(tuple(0L, null), tuple(2L, null), tuple(0L, 0), tuple(0L, null));
    }

    private static long count(FunnelWeek week, FunnelStep step) {
        return week.steps().stream()
                .filter(count -> count.step() == step)
                .findFirst()
                .orElseThrow()
                .count();
    }

    /** Faux dépôt en mémoire : il filtre les instants comme la requête de la base. */
    private static final class InMemoryEvents implements FunnelEventRepository {

        private final List<Map.Entry<FunnelStep, Instant>> events = new ArrayList<>();

        void add(FunnelStep step, String at) {
            record(UUID.randomUUID(), step, Instant.parse(at));
        }

        /** Autant d'événements cette semaine. */
        void add(FunnelStep step, int times) {
            for (int i = 0; i < times; i++) {
                record(UUID.randomUUID(), step, NOW);
            }
        }

        @Override
        public void record(UUID id, FunnelStep step, Instant occurredAt) {
            events.add(Map.entry(step, occurredAt));
        }

        @Override
        public Map<FunnelStep, Long> countsBetween(Instant from, Instant to) {
            Map<FunnelStep, Long> counts = new EnumMap<>(FunnelStep.class);
            for (Map.Entry<FunnelStep, Instant> event : events) {
                Instant at = event.getValue();
                if (!at.isBefore(from) && at.isBefore(to)) {
                    counts.merge(event.getKey(), 1L, Long::sum);
                }
            }
            return counts;
        }
    }
}
