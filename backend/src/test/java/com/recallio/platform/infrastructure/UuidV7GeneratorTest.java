package com.recallio.platform.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7GeneratorTest {

    private static final Instant NOW = Instant.parse("2026-10-29T10:00:00Z");
    private static final int TIMESTAMP_SHIFT = 16;

    @Test
    void shouldProduceAVersion7RfcVariantUuid() {
        UUID id = new UuidV7Generator(Clock.fixed(NOW, ZoneOffset.UTC)).newId();
        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    void shouldEncodeTheCurrentTimeInTheFirst48Bits() {
        UUID id = new UuidV7Generator(Clock.fixed(NOW, ZoneOffset.UTC)).newId();
        assertThat(id.getMostSignificantBits() >>> TIMESTAMP_SHIFT).isEqualTo(NOW.toEpochMilli());
    }

    @Test
    void shouldSortIdentifiersByCreationTime() {
        UUID earlier = new UuidV7Generator(Clock.fixed(NOW, ZoneOffset.UTC)).newId();
        UUID later = new UuidV7Generator(Clock.fixed(NOW.plusMillis(1), ZoneOffset.UTC)).newId();
        assertThat(earlier.toString()).isLessThan(later.toString());
    }

    @Test
    void shouldNotRepeatWithinTheSameMillisecond() {
        var generator = new UuidV7Generator(Clock.fixed(NOW, ZoneOffset.UTC));
        assertThat(generator.newId()).isNotEqualTo(generator.newId());
    }
}
