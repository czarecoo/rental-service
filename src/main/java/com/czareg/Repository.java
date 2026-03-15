package com.czareg;

import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Builder
@AllArgsConstructor
public class Repository {

    @Builder.Default
    private final Map<Long, Car> cars = new HashMap<>();
    @Builder.Default
    private final Map<Long, Client> clients = new HashMap<>();
    @Builder.Default
    private final Map<Long, Reservation> reservations = new HashMap<>();
    @Builder.Default
    private final Map<Long, Rental> rentals = new HashMap<>();
    @Builder.Default
    private final Map<Long, Unavailability> unavailabilities = new HashMap<>();

    public Repository(List<Car> cars, List<Client> clients) {
        this(toMap(cars, Car::carId), toMap(clients, Client::clientId), new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    private static <T> Map<Long, T> toMap(List<T> items, Function<T, Long> getItemId) {
        return items.stream()
                .collect(Collectors.toMap(
                        getItemId,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException("Duplicate item detected: " + getItemId.apply(a));
                        }));
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
        return rentals.values()
                .stream()
                .filter(rental -> Objects.equals(rental.carId(), carId)
                        && isNowBetweenOrEqualTo(rental.startTime(), rental.endTime()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Rental for %s doesn't exists".formatted(carId)));
    }

    public Reservation findReservationOrThrow(long carId) {
        return reservations.values()
                .stream()
                .filter(reservation -> Objects.equals(reservation.carId(), carId)
                        && isNowBetweenOrEqualTo(reservation.startTime(), reservation.endTime()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Reservation for %s doesn't exists".formatted(carId)));
    }

    public void put(Reservation reservation) {
        reservations.put(reservation.reservationId(), reservation);
    }

    public void put(Rental rental) {
        rentals.put(rental.rentalId(), rental);
    }

    public void put(Unavailability unavailability) {
        unavailabilities.put(unavailability.unavailabilityId(), unavailability);
    }

    public boolean isReserved(long carId) {
        for (Reservation reservation : reservations.values()) {
            if (Objects.equals(reservation.carId(), carId)
                    && Instant.now().isAfter(reservation.startTime())
                    && Instant.now().isBefore(reservation.endTime())) {
                return true;
            }
        }
        return false;
    }

    public boolean isRented(long carId) {
        for (Rental rental : rentals.values()) {
            if (Objects.equals(rental.carId(), carId) && isNowBetweenOrEqualTo(rental.startTime(), rental.endTime())) {
                return true;
            }
        }
        return false;
    }

    private boolean isNowBetweenOrEqualTo(Instant start, Instant end) {
        Instant now = Instant.now();
        return !now.isBefore(start) && !now.isAfter(end);
    }

    public boolean isUnavailable(long carId) {
        for (Unavailability unavailability : unavailabilities.values()) {
            if (Objects.equals(unavailability.carId(), carId)
                    && isNowBetweenOrEqualTo(unavailability.startTime(), unavailability.endTime())) {
                return true;
            }
        }
        return false;
    }

    public Optional<Reservation> getReservation(long reservationId) {
        return Optional.ofNullable(reservations.get(reservationId));
    }

    public Optional<Rental> getRental(long rentalId) {
        return Optional.ofNullable(rentals.get(rentalId));
    }

    public Optional<Unavailability> getUnavailability(long unavailabilityId) {
        return Optional.ofNullable(unavailabilities.get(unavailabilityId));
    }

    public List<Rental> getCurrentRentals(long clientId) {
        return rentals.values()
                .stream()
                .filter(rental -> Objects.equals(rental.clientId(), clientId)
                        && isNowBetweenOrEqualTo(rental.startTime(), rental.endTime()))
                .toList();
    }
}
