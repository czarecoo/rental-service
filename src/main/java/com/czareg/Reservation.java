package com.czareg;

import lombok.With;

import java.time.Instant;
import java.util.Objects;

@With
public record Reservation(long id, long carId, long clientId, Instant startTime, Instant endTime) implements TimeSlot {

    public Reservation {
        if (id < 0) {
            throw new IllegalArgumentException();
        }
        if (carId < 0) {
            throw new IllegalArgumentException();
        }
        if (clientId < 0) {
            throw new IllegalArgumentException();
        }
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(endTime);
    }

    @Override
    public CarStatus carStatus() {
        return CarStatus.RESERVED;
    }
}
