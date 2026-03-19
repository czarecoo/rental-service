package com.czareg;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CarRentalTask3Test {

    private static final long CLIENT_1_ID = 1;
    private static final long CLIENT_2_ID = 2;
    private static final long CLIENT_3_ID = 3;
    private static final long CAR_1_ID = 1;
    private static final long CAR_2_ID = 2;
    private static final long CAR_3_ID = 3;
    private static final int WRONG_CAR_ID = 100;

    CarRentalService carRentalService;

    @BeforeEach
    void setUp() {
        List<Car> cars = List.of(
                new Car(CLIENT_1_ID),
                new Car(CLIENT_2_ID),
                new Car(CLIENT_3_ID)
        );
        List<Client> clients = List.of(
                new Client(CLIENT_1_ID),
                new Client(CLIENT_2_ID),
                new Client(CLIENT_3_ID)
        );
        Repository repository = new Repository(cars, clients);
        carRentalService = new DefaultCarRentalService(repository);
    }

    @Test
    void shouldReserveCar() {
        Reservation reservation = carRentalService.reserveCar(CAR_1_ID, CLIENT_1_ID);

        assertEquals(CAR_1_ID, reservation.carId());
        assertEquals(CLIENT_1_ID, reservation.clientId());
        assertEquals(Instant.MAX, reservation.endTime());
        assertEquals(CarStatus.RESERVED, carRentalService.getCarStatus(CLIENT_1_ID));
        assertThat(carRentalService.getAvailableCars()).doesNotContain(new Car(CAR_1_ID));
    }

    @Test
    void shouldCancerCarReservation() {
        carRentalService.reserveCar(CAR_1_ID, CLIENT_1_ID);
        assertEquals(CarStatus.RESERVED, carRentalService.getCarStatus(CAR_1_ID));

        carRentalService.cancelCarReservation(CAR_1_ID, CLIENT_1_ID);

        assertEquals(CarStatus.AVAILABLE, carRentalService.getCarStatus(CAR_1_ID));
    }

    @Test
    void cannotReserveSameCarTwice() {
        carRentalService.reserveCar(CAR_1_ID, CLIENT_1_ID);
        assertThrows(RuntimeException.class, () -> carRentalService.reserveCar(CAR_1_ID, CLIENT_1_ID));
    }

    @Test
    void cannotReserveOrCancelReservationForUnavailableCar() {
        long carId = 99L;
        long clientId = 100L;
        long unavailabilityId = 101L;
        Repository repository = new Repository(List.of(new Car(carId)), List.of(new Client(clientId)));
        repository.add(new Unavailability(unavailabilityId, carId, Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS)));

        carRentalService = new DefaultCarRentalService(repository);
        assertThrows(RuntimeException.class, () -> carRentalService.reserveCar(carId, clientId));
        assertThrows(RuntimeException.class, () -> carRentalService.cancelCarReservation(carId, clientId));
    }

    @Test
    void cannotCancelCarReservationNotReserved() {
        assertThrows(RuntimeException.class, () -> carRentalService.cancelCarReservation(CAR_1_ID, CLIENT_1_ID));
    }

    @Test
    void shouldGetAvailableCars() {
        assertEquals(3, carRentalService.getAvailableCars().size());
        carRentalService.reserveCar(CAR_1_ID, CLIENT_1_ID);
        assertEquals(2, carRentalService.getAvailableCars().size());
        carRentalService.reserveCar(CAR_2_ID, CLIENT_2_ID);
        assertEquals(1, carRentalService.getAvailableCars().size());
        carRentalService.reserveCar(CAR_3_ID, CLIENT_3_ID);
        assertEquals(0, carRentalService.getAvailableCars().size());
    }

    @Test
    void shouldHandleWrongCarId() {
        assertThrows(RuntimeException.class, () -> carRentalService.reserveCar(WRONG_CAR_ID, CLIENT_1_ID));
        assertThrows(RuntimeException.class, () -> carRentalService.cancelCarReservation(WRONG_CAR_ID, CLIENT_1_ID));
        assertThrows(RuntimeException.class, () -> carRentalService.getCarStatus(WRONG_CAR_ID));
    }
}
