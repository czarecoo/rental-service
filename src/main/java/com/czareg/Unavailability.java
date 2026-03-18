package com.czareg;

import java.time.Instant;
import java.util.Objects;

public record Unavailability(long id, long carId, Instant startTime, Instant endTime) implements TimeSlot {

    public Unavailability {
        if (id < 0) {
            throw new IllegalArgumentException();
        }
        if (carId < 0) {
            throw new IllegalArgumentException();
        }
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(endTime);
    }

    @Override
    public CarStatus carStatus() {
        return CarStatus.UNAVAILABLE;
    }
}
