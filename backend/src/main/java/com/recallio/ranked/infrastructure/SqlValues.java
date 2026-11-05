package com.recallio.ranked.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/** Les valeurs telles que les requêtes natives les rendent, selon le pilote. */
final class SqlValues {

    private SqlValues() {}

    static Instant instant(Object value) {
        return switch (value) {
            case Instant instant -> instant;
            case OffsetDateTime dateTime -> dateTime.toInstant();
            case Timestamp timestamp -> timestamp.toInstant();
            default -> throw new IllegalStateException("date inattendue : " + value.getClass());
        };
    }

    static @Nullable Instant nullableInstant(@Nullable Object value) {
        return value == null ? null : instant(value);
    }

    static double number(Object value) {
        return ((Number) value).doubleValue();
    }

    static int integer(Object value) {
        return ((Number) value).intValue();
    }
}
