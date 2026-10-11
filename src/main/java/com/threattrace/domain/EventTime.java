package com.threattrace.domain;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/** Converts JDBC and Java timestamp representations using UTC for zone-less database values. */
public final class EventTime {
    private EventTime() { }

    public static Instant toInstant(Object value) {
        if (value instanceof Instant instant) return instant;
        if (value instanceof Timestamp timestamp) return timestamp.toInstant();
        if (value instanceof LocalDateTime localDateTime) return localDateTime.toInstant(ZoneOffset.UTC);
        if (value instanceof OffsetDateTime offsetDateTime) return offsetDateTime.toInstant();
        if (value instanceof ZonedDateTime zonedDateTime) return zonedDateTime.toInstant();
        if (value instanceof java.util.Date date) return date.toInstant();
        throw new IllegalArgumentException("Unsupported event timestamp type: " +
                (value == null ? "null" : value.getClass().getName()));
    }

    public static String toUtcHour(Object value) {
        return toInstant(value).truncatedTo(ChronoUnit.HOURS).toString();
    }
}
