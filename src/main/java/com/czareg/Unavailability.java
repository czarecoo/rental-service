package com.czareg;

import lombok.With;

import java.time.Instant;
import java.util.Objects;

@With
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

    public Unavailability(long carId, Instant endTime) {
        this(0, carId, Instant.now(), endTime);
    }

    @Override
    public CarStatus carStatus() {
        return CarStatus.UNAVAILABLE;
    }
}
