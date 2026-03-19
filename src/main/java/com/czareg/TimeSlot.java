package com.czareg;

import java.time.Instant;

public interface TimeSlot {

    long carId();
    Instant endTime();
    CarStatus carStatus();

    default boolean isActiveNow() {
        return isActiveAt(Instant.now());
    }

    default boolean isActiveAt(Instant time) {
        return endTime().isAfter(time);
    }
}
