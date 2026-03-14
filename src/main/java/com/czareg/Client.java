package com.czareg;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record Client(long clientId, List<Long> rentedCarIds) {

    public Client {
        if (clientId < 0) {
            throw new IllegalArgumentException();
        }
        Objects.requireNonNull(rentedCarIds);
    }

    public Client(long clientId) {
        this(clientId, new ArrayList<>());
    }

    public Client withCar(long carId) {
        List<Long> newCarIds = new ArrayList<>(rentedCarIds);
        newCarIds.add(carId);
        return new Client(clientId, newCarIds);
    }

    public Client withoutCar(long carId) {
        List<Long> newCarIds = new ArrayList<>(rentedCarIds);
        newCarIds.remove(carId);
        return new Client(clientId, newCarIds);
    }

    public List<Long> getRentedCarIds() {
        return List.copyOf(rentedCarIds);
    }
}
