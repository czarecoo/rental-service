package com.czareg;

import java.util.List;

public interface CarRentalService {

    Rental rentCar(long carId, long clientId, int days);

    default Rental rentCar(long carId, long clientId) {
        return rentCar(carId, clientId, Integer.MAX_VALUE);
    }

    void returnCar(long carId, long clientId);

    List<Car> getAvailableCars();

    List<Car> getAllRentedCarsByClient(long clientId);

    CarStatus getCarStatus(long carId);

    List<Client> getAllClients();

    Reservation reserveCar(long carId, long clientId, int days);

    default Reservation reserveCar(long carId, long clientId) {
        return reserveCar(carId, clientId, Integer.MAX_VALUE);
    }

    void cancelCarReservation(long carId, long clientId);
}
