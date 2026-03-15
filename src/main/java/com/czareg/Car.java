package com.czareg;

public record Car(long carId) {

    public Car {
        if (carId < 0) {
            throw new IllegalArgumentException();
        }
    }
}
