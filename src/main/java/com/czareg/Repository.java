package com.czareg;

import lombok.AllArgsConstructor;

import java.util.*;

@AllArgsConstructor
public class Repository {

    private final Map<Long, Car> cars = new HashMap<>();
    private final Map<Long, Client> clients = new HashMap<>();
    private final Map<Long, Reservation> reservations = new HashMap<>();
    private final Map<Long, Rental> rentals = new HashMap<>();
    private final Map<Long, Unavailability> unavailabilities = new HashMap<>();

    private final Map<Long, TimeSlot> timeSlots = new HashMap<>(); //index for holding ONLY active timeslots for carId

    public Repository(List<Car> cars, List<Client> clients) {
        cars.forEach(car -> this.cars.put(car.id(),car));
        clients.forEach(client -> this.clients.put(client.id(),client));
    }

    public List<Car> getCars(){
        return List.copyOf(cars.values());
    }

    public List<Client> getClients(){
        return List.copyOf(clients.values());
    }

    public Car findCarOrThrow(long carId) {
        return Optional.ofNullable(cars.get(carId))
                .orElseThrow(() -> new IllegalArgumentException("Car %s doesn't exists".formatted(carId)));
    }

    public Client findClientOrThrow(long clientId) {
        return Optional.ofNullable(clients.get(clientId))
                .orElseThrow(() -> new IllegalArgumentException("Client %s doesn't exists".formatted(clientId)));
    }

    public Rental findRentalOrThrow(long carId) {
        return Optional.ofNullable(timeSlots.get(carId))
                .filter(slot -> slot instanceof Rental rental
                        && rental.isActiveNow())
                .map(slot -> (Rental) slot)
                .orElseThrow(() -> new IllegalArgumentException("Rental for %s doesn't exists".formatted(carId)));
    }

    public Reservation findReservationOrThrow(long carId) {
        return Optional.ofNullable(timeSlots.get(carId))
                .filter(slot -> slot instanceof Reservation reservation
                        && reservation.isActiveNow())
                .map(slot -> (Reservation) slot)
                .orElseThrow(() -> new IllegalArgumentException("Reservation for %s doesn't exists".formatted(carId)));
    }

    public void add(Reservation reservation) {
        reservations.put(reservation.id(), reservation);
        timeSlots.put(reservation.carId(),reservation);
    }

    public void add(Rental rental) {
        rentals.put(rental.id(), rental);
        timeSlots.put(rental.carId(),rental);
    }

    public void add(Unavailability unavailability) {
        unavailabilities.put(unavailability.id(), unavailability);
        timeSlots.put(unavailability.carId(),unavailability);
    }

    public void end(Reservation reservation) {
        reservations.put(reservation.id(), reservation);
        timeSlots.remove(reservation.carId());
    }

    public void end(Rental rental) {
        rentals.put(rental.id(), rental);
        timeSlots.remove(rental.carId());
    }

    public void end(Unavailability unavailability) {
        unavailabilities.put(unavailability.id(), unavailability);
        timeSlots.remove(unavailability.carId());
    }

    public List<Rental> getCurrentRentals(long clientId) {
        return timeSlots.values().stream()
                .filter(slot -> slot instanceof Rental rental
                        && rental.clientId() == clientId
                        && rental.isActiveNow())
                .map(slot -> (Rental) slot)
                .toList();
    }

    public Optional<TimeSlot> findActiveSlot(long carId) {
        TimeSlot timeSlot = timeSlots.get(carId);

        if (timeSlot != null && timeSlot.isActiveNow()) {
            return Optional.of(timeSlot);
        }
        return Optional.empty();
    }
}
