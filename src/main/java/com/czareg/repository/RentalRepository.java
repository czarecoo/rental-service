package com.czareg.repository;

import com.czareg.Rental;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class RentalRepository {

    private final AtomicLong id = new AtomicLong();
    private final Map<Long, Rental> rentals = new HashMap<>();

    public Rental save(Rental rental) {
        rental = rental.withId(id.getAndIncrement());
        rentals.put(rental.id(), rental);
        return rental;
    }

    public Rental update(Rental rental) {
        rentals.put(rental.id(), rental);
        return rental;
    }
}
