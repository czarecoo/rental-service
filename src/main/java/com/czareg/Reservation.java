package com.czareg;

import lombok.With;

import java.time.Instant;
import java.util.Objects;

@With
public record Reservation(long reservationId, long carId, long clientId, Instant startTime, Instant endTime) {

    public Reservation {
        if (reservationId < 0) {
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
}
