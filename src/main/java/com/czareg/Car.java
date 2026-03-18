package com.czareg;

public record Car(long id) {

    public Car {
        if (id < 0) {
            throw new IllegalArgumentException();
        }
    }
}
