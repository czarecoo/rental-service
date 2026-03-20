package com.czareg;

import com.czareg.repository.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class Repository {

    private final CarRepository carRepository;
    private final ClientRepository clientRepository;
    private final RentalRepository rentalRepository;
    private final ReservationRepository reservationRepository;
    private final UnavailabilitiesRepository unavailabilitiesRepository;
    private final CurrentCarStatusRepository currentCarStatusRepository;

    public Repository(List<Car> cars, List<Client> clients) {
        this(new CarRepository(cars), new ClientRepository(clients), new RentalRepository(), new ReservationRepository(), new UnavailabilitiesRepository(), new CurrentCarStatusRepository());
    }

    public List<Car> getCars() {
        return carRepository.getCars();
    }

    public List<Client> getClients() {
        return clientRepository.getAll();
    }

    public Car findCarOrThrow(long carId) {
        return carRepository.findCar(carId)
                .orElseThrow(() -> new IllegalArgumentException("Car %s doesn't exists".formatted(carId)));
    }

    public Client findClientOrThrow(long clientId) {
        return clientRepository.find(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client %s doesn't exists".formatted(clientId)));
    }

    public Rental findRentalOrThrow(long carId) {
        return currentCarStatusRepository.findActiveByCarId(carId)
                .filter(slot -> slot instanceof Rental rental
                        && rental.isActiveNow())
                .map(slot -> (Rental) slot)
                .orElseThrow(() -> new IllegalArgumentException("Rental for %s doesn't exists".formatted(carId)));
    }

    public Reservation findReservationOrThrow(long carId) {
        return currentCarStatusRepository.findActiveByCarId(carId)
                .filter(slot -> slot instanceof Reservation reservation
                        && reservation.isActiveNow())
                .map(slot -> (Reservation) slot)
                .orElseThrow(() -> new IllegalArgumentException("Reservation for %s doesn't exists".formatted(carId)));
    }

    public Reservation add(Reservation reservation) {
        reservation = reservationRepository.save(reservation);
        currentCarStatusRepository.save(reservation);
        return reservation;
    }

    public Rental add(Rental rental) {
        rental = rentalRepository.save(rental);
        currentCarStatusRepository.save(rental);
        return rental;
    }

    public Unavailability add(Unavailability unavailability) {
        unavailability = unavailabilitiesRepository.save(unavailability);
        currentCarStatusRepository.save(unavailability);
        return unavailability;
    }

    public Reservation end(Reservation reservation) {
        reservation = reservationRepository.save(reservation);
        currentCarStatusRepository.remove(reservation.carId());
        return reservation;
    }

    public Rental end(Rental rental) {
        rental = rentalRepository.save(rental);
        currentCarStatusRepository.remove(rental.carId());
        return rental;
    }

    public Unavailability end(Unavailability unavailability) {
        unavailability = unavailabilitiesRepository.save(unavailability);
        currentCarStatusRepository.remove(unavailability.carId());
        return unavailability;
    }

    public List<Rental> getCurrentRentals(long clientId) {
        return currentCarStatusRepository.findAllActive()
                .stream()
                .filter(slot -> slot instanceof Rental rental
                        && rental.clientId() == clientId
                        && rental.isActiveNow())
                .map(slot -> (Rental) slot)
                .toList();
    }

    public Optional<TimeSlot> findActiveSlot(long carId) {
        return currentCarStatusRepository.findActiveByCarId(carId);
    }
}
