package com.czareg.repository;

import com.czareg.Reservation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationRepository {

    private final AtomicLong id = new AtomicLong();
    private final Map<Long, Reservation> reservations = new HashMap<>();

    public Reservation save(Reservation reservation) {
        reservation = reservation.withId(id.getAndIncrement());
        reservations.put(reservation.id(), reservation);
        return reservation;
    }
}
