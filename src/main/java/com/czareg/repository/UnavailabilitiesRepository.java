package com.czareg.repository;

import com.czareg.Unavailability;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class UnavailabilitiesRepository {

    private final AtomicLong id = new AtomicLong();
    private final Map<Long, Unavailability> unavailabilities = new HashMap<>();

    public Unavailability save(Unavailability unavailability) {
        unavailability = unavailability.withId(id.getAndIncrement());
        unavailabilities.put(unavailability.id(), unavailability);
        return unavailability;
    }
}
