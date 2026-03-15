package com.czareg;

import lombok.With;

import java.time.Instant;
import java.util.Objects;

@With
public record Rental(long rentalId, long carId, long clientId, Instant startTime, Instant endTime) {

    public Rental {
        if (rentalId < 0) {
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
