package com.czareg;

public record Client(long id) {

    public Client {
        if (id < 0) {
            throw new IllegalArgumentException();
        }
    }
}
