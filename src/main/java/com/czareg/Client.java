package com.czareg;

public record Client(long clientId) {

    public Client {
        if (clientId < 0) {
            throw new IllegalArgumentException();
        }
    }
}
