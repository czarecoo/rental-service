package com.czareg;

import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Not thread safe.
 */
@RequiredArgsConstructor
public class DefaultCarRentalService implements CarRentalService {

    private final Repository repository;
    private final AtomicLong id = new AtomicLong();

    @Override
    public Rental rentCar(long carId, long clientId, int days) {
        repository.findCarOrThrow(carId);
        CarStatus carStatus = getCarStatus(carId);
        if (carStatus != CarStatus.AVAILABLE) {
            throw new IllegalStateException("Car %s has status %s and cannot be rented by client %s".formatted(carId, carStatus, clientId));
        }
        repository.findClientOrThrow(clientId);

        Instant endTime = days == Integer.MAX_VALUE ? Instant.MAX : Instant.now().plus(days, ChronoUnit.DAYS);
        Rental rental = new Rental(id.getAndIncrement(), carId, clientId, Instant.now(), endTime);
        repository.add(rental);
        return rental;
    }

    @Override
    public void returnCar(long carId, long clientId) {
        repository.findCarOrThrow(carId);
        CarStatus carStatus = getCarStatus(carId);
        if (carStatus != CarStatus.RENTED) {
            throw new IllegalStateException("Car %s has status %s and cannot be returned by client %s".formatted(carId, carStatus, clientId));
        }
        repository.findClientOrThrow(clientId);
        Rental rental = repository.findRentalOrThrow(carId);
        if (!Objects.equals(rental.clientId(), clientId)) {
            throw new IllegalArgumentException("Client %s doesn't rent car %s".formatted(clientId, carId));
        }
        Rental modifiedRental = rental.withEndTime(Instant.now());
        repository.end(modifiedRental);
    }

    @Override
    public List<Car> getAvailableCars() {
        return repository.getCars()
                .stream()
                .filter(car -> getCarStatus(car.id()) == CarStatus.AVAILABLE)
                .toList();
    }

    @Override
    public List<Car> getAllRentedCarsByClient(long clientId) {
        repository.findClientOrThrow(clientId);
        List<Rental> rentals = repository.getCurrentRentals(clientId);
        return rentals.stream()
                .map(Rental::carId)
                .map(repository::findCarOrThrow)
                .toList();
    }

    @Override
    public CarStatus getCarStatus(long carId) {
        repository.findCarOrThrow(carId);

        return repository.findActiveSlot(carId)
                .map(TimeSlot::carStatus)
                .orElse(CarStatus.AVAILABLE);
    }

    @Override
    public List<Client> getAllClients() {
        return repository.getClients();
    }

    @Override
    public Reservation reserveCar(long carId, long clientId, int days) {
        repository.findCarOrThrow(carId);
        CarStatus carStatus = getCarStatus(carId);
        if (carStatus != CarStatus.AVAILABLE) {
            throw new IllegalStateException("Car %s has status %s and cannot be reserved by client %s".formatted(carId, carStatus, clientId));
        }
        repository.findClientOrThrow(clientId);

        Instant endTime = days == Integer.MAX_VALUE ? Instant.MAX : Instant.now().plus(days, ChronoUnit.DAYS);
        Reservation reservation = new Reservation(id.getAndIncrement(), carId, clientId, Instant.now(), endTime);
        repository.add(reservation);
        return reservation;
    }

    @Override
    public void cancelCarReservation(long carId, long clientId) {
        repository.findCarOrThrow(carId);
        CarStatus carStatus = getCarStatus(carId);
        if (carStatus != CarStatus.RESERVED) {
            throw new IllegalStateException("Car %s has status %s and cannot have its reservation cancelled by client %ss".formatted(carId, carStatus, clientId));
        }
        repository.findClientOrThrow(clientId);
        Reservation reservation = repository.findReservationOrThrow(carId);
        if (Objects.equals(reservation.clientId(), clientId)) {
            throw new IllegalArgumentException("Client %s doesn't have a reservation for car %s".formatted(carId, carStatus));
        }

        Reservation modifiedReservation = reservation.withEndTime(Instant.now());
        repository.end(modifiedReservation);
    }
}
