package com.czareg;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Not thread safe.
 */
public class DefaultCarRentalService implements CarRentalService {

    private final Map<Long, Car> cars;
    private final Map<Long, Client> clients;

    public DefaultCarRentalService(List<Car> cars, List<Client> clients) {
        this.cars = cars.stream()
                .collect(Collectors.toMap(
                        Car::carId,
                        Function.identity(),
                        (carA, carB) -> {
                            throw new IllegalStateException("Duplicate car detected: " + carA.carId());
                        }));
        this.clients = clients.stream()
                .collect(Collectors.toMap(
                        Client::clientId,
                        Function.identity(),
                        (clientA, clientB) -> {
                            throw new IllegalStateException("Duplicate client detected: " + clientA.clientId());
                        }));
    }

    @Override
    public Car rentCar(long carId, long clientId) {
        Car car = findCarOrThrow(carId);
        CarStatus carStatus = car.status();
        if (carStatus != CarStatus.AVAILABLE) {
            throw new IllegalStateException("Car %s has status %s and cannot be rented by client %s".formatted(carId, carStatus, clientId));
        }
        // what if car is reserved?

        Client client = findClientOrThrow(clientId);
        clients.put(clientId, client.withCar(carId));
        Car modifiedCar = car.withStatus(CarStatus.RENTED);
        cars.put(carId, modifiedCar);
        return modifiedCar;
    }

    @Override
    public Car returnCar(long carId, long clientId) {
        Car car = findCarOrThrow(carId);
        CarStatus carStatus = car.status();
        if (carStatus != CarStatus.RENTED) {
            throw new IllegalStateException("Car %s has status %s and cannot be returned by client %s".formatted(carId, carStatus, clientId));
        }
        Client client = findClientOrThrow(clientId);
        List<Long> rentedCarIds = client.getRentedCarIds();
        if (!rentedCarIds.contains(carId)) {
            throw new IllegalArgumentException("Client %s doesn't rent car %s".formatted(carId, carStatus));
        }

        clients.put(clientId, client.withoutCar(carId));
        Car modifiedCar = car.withStatus(CarStatus.AVAILABLE);
        cars.put(carId, modifiedCar);
        return modifiedCar;
    }

    @Override
    public List<Car> getAvailableCars() {
        return cars.values()
                .stream()
                .filter(car -> car.status() == CarStatus.AVAILABLE)
                .toList();
    }

    @Override
    public List<Car> getAllRentedCarsByClient(long clientId) {
        Client client = findClientOrThrow(clientId);

        return client.getRentedCarIds()
                .stream()
                .map(cars::get)
                .toList();
    }

    @Override
    public boolean isCarRented(long carId) {
        Car car = findCarOrThrow(carId);
        return car.status() == CarStatus.RENTED;
    }

    @Override
    public List<Client> getAllClients() {
        return clients.values()
                .stream()
                .toList();
    }

    private Car findCarOrThrow(long carId) {
        return Optional.ofNullable(cars.get(carId))
                .orElseThrow(() -> new IllegalArgumentException("Car %s doesn't exists".formatted(carId)));
    }

    private Client findClientOrThrow(long clientId) {
        return Optional.ofNullable(clients.get(clientId))
                .orElseThrow(() -> new IllegalArgumentException("Client %s doesn't exists".formatted(clientId)));
    }
}
