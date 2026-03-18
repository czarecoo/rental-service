package com.czareg;

import java.time.Instant;

public interface TimeSlot {

    long carId();
    Instant startTime();
    Instant endTime();
    CarStatus carStatus();

    default boolean isActiveNow() {
        return isActiveAt(Instant.now());
    }

    default boolean isActiveAt(Instant time) {
        return !startTime().isAfter(time) && endTime().isAfter(time);
    }
}
