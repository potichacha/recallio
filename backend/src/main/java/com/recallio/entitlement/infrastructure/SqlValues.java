package com.recallio.entitlement.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/** Les valeurs d'une ligne lue par une requête native. */
final class SqlValues {

    private SqlValues() {}

    /** Selon le pilote, une colonne {@code timestamptz} arrive en {@link Instant}, {@link OffsetDateTime} ou {@link Timestamp}. */
    static Instant instant(Object value) {
        return switch (value) {
            case Instant instant -> instant;
            case OffsetDateTime dateTime -> dateTime.toInstant();
            case Timestamp timestamp -> timestamp.toInstant();
            default -> throw new IllegalStateException("Type de date inattendu : " + value.getClass());
        };
    }

    static @Nullable Instant nullableInstant(@Nullable Object value) {
        return value == null ? null : instant(value);
    }

    static int integer(Object value) {
        return ((Number) value).intValue();
    }
}
