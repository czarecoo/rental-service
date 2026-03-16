package com.czareg;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class CarRentalTask1Test {

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
    void shouldRentCar() {
        Rental rental = carRentalService.rentCar(CAR_1_ID, CLIENT_1_ID);

        assertEquals(CAR_1_ID, rental.carId());
        assertEquals(CLIENT_1_ID, rental.clientId());
        assertEquals(Instant.MAX, rental.endTime());
        assertEquals(CarStatus.RENTED, carRentalService.getCarStatus(CLIENT_1_ID));
        assertEquals(List.of(new Car(CAR_1_ID)), carRentalService.getAllRentedCarsByClient(CLIENT_1_ID));
        assertThat(carRentalService.getAvailableCars()).doesNotContain(new Car(CAR_1_ID));
    }

    @Test
    void shouldReturnCar() {
        carRentalService.rentCar(CAR_1_ID, CLIENT_1_ID);
        assertEquals(CarStatus.RENTED, carRentalService.getCarStatus(CAR_1_ID));

        carRentalService.returnCar(CAR_1_ID, CLIENT_1_ID);

        assertEquals(CarStatus.AVAILABLE, carRentalService.getCarStatus(CAR_1_ID));
        assertTrue(carRentalService.getAllRentedCarsByClient(CLIENT_1_ID).isEmpty());
    }

    @Test
    void cannotRentSameCarTwice() {
        carRentalService.rentCar(CAR_1_ID, CLIENT_1_ID);
        assertThrows(RuntimeException.class, () -> carRentalService.rentCar(CAR_1_ID, CLIENT_1_ID));
    }

    @Test
    void cannotRentOrReturnUnavailableCar() {
        long carId = 99L;
        long clientId = 100L;
        long unavailabilityId = 101L;
        Repository repository = Repository.builder()
                .cars(Map.of(carId, new Car(carId)))
                .clients(Map.of(clientId, new Client(clientId)))
                .unavailabilities(Map.of(unavailabilityId, new Unavailability(unavailabilityId, carId, Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS))))
                .build();
        carRentalService = new DefaultCarRentalService(repository);
        assertThrows(RuntimeException.class, () -> carRentalService.rentCar(carId, clientId));
        assertThrows(RuntimeException.class, () -> carRentalService.returnCar(carId, clientId));
    }

    @Test
    void cannotReturnCarNotRented() {
        assertThrows(RuntimeException.class, () -> carRentalService.returnCar(CAR_1_ID, CLIENT_1_ID));
    }

    @Test
    void shouldGetAvailableCars() {
        assertEquals(3, carRentalService.getAvailableCars().size());
        carRentalService.rentCar(CAR_1_ID, CLIENT_1_ID);
        assertEquals(2, carRentalService.getAvailableCars().size());
        carRentalService.rentCar(CAR_2_ID, CLIENT_2_ID);
        assertEquals(1, carRentalService.getAvailableCars().size());
        carRentalService.rentCar(CAR_3_ID, CLIENT_3_ID);
        assertEquals(0, carRentalService.getAvailableCars().size());
    }

    @Test
    void shouldGetAllRentedCarsByClient() {
        assertEquals(0, carRentalService.getAllRentedCarsByClient(CLIENT_1_ID).size());
        carRentalService.rentCar(CAR_1_ID, CLIENT_1_ID);
        assertEquals(1, carRentalService.getAllRentedCarsByClient(CLIENT_1_ID).size());
        carRentalService.rentCar(CAR_2_ID, CLIENT_1_ID);
        assertEquals(2, carRentalService.getAllRentedCarsByClient(CLIENT_1_ID).size());
        carRentalService.rentCar(CAR_3_ID, CLIENT_1_ID);
        assertEquals(3, carRentalService.getAllRentedCarsByClient(CLIENT_1_ID).size());
    }

    @Test
    void shouldHandleWrongCarId() {
        assertThrows(RuntimeException.class, () -> carRentalService.rentCar(WRONG_CAR_ID, CLIENT_1_ID));
        assertThrows(RuntimeException.class, () -> carRentalService.returnCar(WRONG_CAR_ID, CLIENT_1_ID));
        assertThrows(RuntimeException.class, () -> carRentalService.getCarStatus(WRONG_CAR_ID));
    }

    @Test
    void shouldBeAbleToRentAfterCarIsFinallyAvailable() {
        long rentalId = 100L;
        Instant unavailabilityEnd = Instant.now().plusSeconds(2);
        Instant reservationEnd = unavailabilityEnd.plusSeconds(2);
        Instant rentalEnd = reservationEnd.plusSeconds(2);
        Repository repository = Repository.builder()
                .cars(Map.of(CAR_1_ID, new Car(CAR_1_ID)))
                .clients(Map.of(CLIENT_1_ID, new Client(CLIENT_1_ID)))
                .unavailabilities(new HashMap<>(Map.of(rentalId, new Unavailability(rentalId, CAR_1_ID, Instant.now(), unavailabilityEnd))))
                .reservations(new HashMap<>(Map.of(rentalId, new Reservation(rentalId, CAR_1_ID, CLIENT_1_ID, unavailabilityEnd, reservationEnd))))
                .rentals(new HashMap<>(Map.of(rentalId, new Rental(rentalId, CAR_1_ID, CLIENT_1_ID, reservationEnd, rentalEnd))))
                .build();
        carRentalService = new DefaultCarRentalService(repository);

        await()
                .atLeast(Duration.ofSeconds(6))
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertDoesNotThrow(() -> carRentalService.rentCar(CAR_1_ID, CLIENT_1_ID)));
    }
}
