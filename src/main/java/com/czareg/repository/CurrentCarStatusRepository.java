package com.czareg.repository;

import com.czareg.TimeSlot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CurrentCarStatusRepository {

    private final Map<Long, TimeSlot> timeSlotByCarId = new HashMap<>();

    public Optional<TimeSlot> findActiveByCarId(long carId) {
        TimeSlot slot = timeSlotByCarId.get(carId);
        if (slot != null && slot.isActiveNow()) {
            return Optional.of(slot);
        }
        return Optional.empty();
    }

    public void save(TimeSlot slot) {
        timeSlotByCarId.put(slot.carId(), slot);
    }

    public void remove(long carId) {
        timeSlotByCarId.remove(carId);
    }

    public List<TimeSlot> findAllActive() {
        return timeSlotByCarId.values().stream()
                .filter(TimeSlot::isActiveNow)
                .toList();
    }
}
