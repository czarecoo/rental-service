package com.czareg;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        carRentalService = new DefaultCarRentalService(cars, clients);
    }

    @Test
    void shouldRentCar() {
        Car car = carRentalService.rentCar(1, CLIENT_1_ID);

        assertTrue(carRentalService.isCarRented(1));
        assertTrue(carRentalService.getAllRentedCarsByClient(CLIENT_1_ID).contains(car));
    }

    @Test
    void shouldReturnCar() {
        carRentalService.rentCar(CAR_1_ID, CLIENT_1_ID);
        assertTrue(carRentalService.isCarRented(CAR_1_ID));

        assertNotNull(carRentalService.returnCar(CAR_1_ID, CLIENT_1_ID));

        assertFalse(carRentalService.isCarRented(CAR_1_ID));
        assertTrue(carRentalService.getAllRentedCarsByClient(CLIENT_1_ID).isEmpty());
    }

    @Test
    void cannotRentSameCarTwice() {
        carRentalService.rentCar(CAR_1_ID, CLIENT_1_ID);
        assertThrows(RuntimeException.class, () ->carRentalService.rentCar(CAR_1_ID, CLIENT_1_ID));
    }

    @Test
    void cannotRentOrReturnUnavailableCar() {
        long carId = 99L;
        long clientId = 100L;
        carRentalService = new DefaultCarRentalService(List.of(new Car(carId, CarStatus.UNAVAILABLE)), List.of(new Client(clientId)));
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
        assertThrows(RuntimeException.class, ()->carRentalService.rentCar(WRONG_CAR_ID, CLIENT_1_ID));
        assertThrows(RuntimeException.class, ()->carRentalService.returnCar(WRONG_CAR_ID, CLIENT_1_ID));
        assertThrows(RuntimeException.class, ()->carRentalService.isCarRented(WRONG_CAR_ID));
    }
}
