package com.czareg;

import java.util.List;

public interface CarRentalService
{
    /**
     * Rent a car for a client
     * @param carId - id of the car to rent
     * @param clientId - id of the client renting the car
     * @return the car that was rented
     */
    Car rentCar(long carId, long clientId);

    /**
     * Return a car that was rented by a client
     * @param carId - id of the car to return
     * @param clientId - id of the client returning the car
     * @return the car that was returned
     */
    Car returnCar(long carId, long clientId);

    /**
     * Get a list of all available cars
     * @return list of available cars
     */
    List<Car> getAvailableCars();

    /**
     * Get a list of all rented cars by a client
     * @return list of rented cars
     */
    List<Car> getAllRentedCarsByClient(long clientId);

    /**
     * Check if a car is rented
     * @param carId - id of the car to check
     * @return true if the car is rented, false otherwise
     */
    boolean isCarRented(long carId);

    /**
     * Get a list of all clients
     * @return list of clients
     */
    List<Client> getAllClients();
}
