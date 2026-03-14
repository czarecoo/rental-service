package com.czareg;

import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Thread safe.
 */
@RequiredArgsConstructor
public class SynchronizingCarRentalService implements CarRentalService {

    private final CarRentalService carRentalService;

    @Override
    public synchronized Car rentCar(long carId, long clientId) {
        return carRentalService.rentCar(carId, clientId);
    }

    @Override
    public synchronized Car returnCar(long carId, long clientId) {
        return carRentalService.returnCar(carId, clientId);
    }

    @Override
    public List<Car> getAvailableCars() {
        return carRentalService.getAvailableCars();
    }

    @Override
    public List<Car> getAllRentedCarsByClient(long clientId) {
        return carRentalService.getAllRentedCarsByClient(clientId);
    }

    @Override
    public boolean isCarRented(long carId) {
        return carRentalService.isCarRented(carId);
    }

    @Override
    public List<Client> getAllClients() {
        return carRentalService.getAllClients();
    }
}
