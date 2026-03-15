package com.czareg;

import java.time.Instant;
import java.util.Objects;

public record Unavailability(long unavailabilityId, long carId, Instant startTime, Instant endTime) {

    public Unavailability {
        if (unavailabilityId < 0) {
            throw new IllegalArgumentException();
        }
        if (carId < 0) {
            throw new IllegalArgumentException();
        }
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(endTime);
    }
}
