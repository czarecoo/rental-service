package com.czareg.repository;

import com.czareg.Car;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CarRepository {

    private final Map<Long, Car> cars = new HashMap<>();

    public CarRepository(List<Car> cars) {
        cars.forEach(car -> this.cars.put(car.id(), car));
    }

    public List<Car> getCars() {
        return List.copyOf(cars.values());
    }

    public Optional<Car> findCar(long carId) {
        return Optional.ofNullable(cars.get(carId));
    }
}
