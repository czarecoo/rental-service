package com.czareg;

import java.util.Objects;

public record Car(long carId, CarStatus status) {

    public Car {
        if (carId < 0) {
            throw new IllegalArgumentException();
        }
        Objects.requireNonNull(status);
    }

    public Car(long carId) {
        this(carId, CarStatus.AVAILABLE);
    }

    public Car withStatus(CarStatus carStatus) {
        return new Car(carId, carStatus);
    }
}
