package com.czareg;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Named.named;

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
        Repository repository = new Repository(List.of(new Car(carId)), List.of(new Client(clientId)));
        repository.add(new Unavailability(unavailabilityId, carId, Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS)));

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

    @ParameterizedTest
    @MethodSource("provideRepositories")
    void shouldBeAbleToRentCarAfterItBecomesAvailableAfter(Supplier<Repository> repositorySupplier) {
        carRentalService = new DefaultCarRentalService(repositorySupplier.get());

        await()
                .atLeast(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertDoesNotThrow(() -> carRentalService.rentCar(CAR_1_ID, CLIENT_1_ID)));
    }

    private static Stream<Arguments> provideRepositories() {
        return Stream.of(
                Arguments.of(named("Unavailability", createRepositoryWithCarClientAndUnavailability())),
                Arguments.of(named("Reservation",createRepositoryWithCarClientAndReservation())),
                Arguments.of(named("Rental",createRepositoryWithCarClientAndRental()))
        );
    }

    private static Supplier<Repository> createRepositoryWithCarClientAndUnavailability() {
        return () -> {
            Repository repository = createRepositoryWithCarAndClient();
            repository.add(new Unavailability(100L, CAR_1_ID, Instant.now(), Instant.now().plusSeconds(2)));
            return repository;
        };
    }

    private static Supplier<Repository> createRepositoryWithCarClientAndReservation() {
        return () -> {
            Repository repository = createRepositoryWithCarAndClient();
            repository.add(new Reservation(100L, CAR_1_ID, CLIENT_1_ID, Instant.now(), Instant.now().plusSeconds(3)));
            return repository;
        };
    }

    private static Supplier<Repository> createRepositoryWithCarClientAndRental() {
        return () -> {
            Repository repository = createRepositoryWithCarAndClient();
            repository.add(new Rental(100L, CAR_1_ID, CLIENT_1_ID, Instant.now(), Instant.now().plusSeconds(4)));
            return repository;
        };
    }

    private static Repository createRepositoryWithCarAndClient() {
        return new Repository(List.of(new Car(CAR_1_ID)), List.of(new Client(CLIENT_1_ID)));
    }
}
