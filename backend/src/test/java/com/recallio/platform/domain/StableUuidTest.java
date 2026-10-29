package com.recallio.platform.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class StableUuidTest {

    @Test
    void shouldAlwaysDeriveTheSameIdentifierFromTheSameName() {
        assertThat(StableUuid.of("ns", "name")).isEqualTo(StableUuid.of("ns", "name"));
    }

    @Test
    void shouldDeriveDifferentIdentifiersForDifferentNamespaces() {
        assertThat(StableUuid.of("a", "name")).isNotEqualTo(StableUuid.of("b", "name"));
    }

    @Test
    void shouldProduceAVersion8RfcVariantUuid() {
        UUID id = StableUuid.of("ns", "name");
        assertThat(id.version()).isEqualTo(8);
        assertThat(id.variant()).isEqualTo(2);
    }
}
