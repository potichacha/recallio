package com.recallio.platform.infrastructure;

import com.recallio.platform.domain.IdGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;

/**
 * UUID v7 (RFC 9562) : 48 bits d'horodatage en millisecondes, puis de l'aléa. Les identifiants sont croissants dans le
 * temps, ce qui garde les index B-tree compacts (ADR 0008).
 */
@ApplicationScoped
public class UuidV7Generator implements IdGenerator {

    private static final long VERSION_7 = 0x7000L;
    private static final long RANDOM_12_BITS = 0x0fffL;
    private static final long VARIANT_RFC_9562 = 0x8000_0000_0000_0000L;
    private static final long RANDOM_62_BITS = 0x3fff_ffff_ffff_ffffL;
    private static final int TIMESTAMP_SHIFT = 16;

    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public UuidV7Generator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public UUID newId() {
        long timestamp = clock.millis();
        long mostSignificant = (timestamp << TIMESTAMP_SHIFT) | VERSION_7 | (random.nextLong() & RANDOM_12_BITS);
        long leastSignificant = VARIANT_RFC_9562 | (random.nextLong() & RANDOM_62_BITS);
        return new UUID(mostSignificant, leastSignificant);
    }
}
