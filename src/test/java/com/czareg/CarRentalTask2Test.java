package com.czareg;

import org.junit.jupiter.api.RepeatedTest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class CarRentalTask2Test {

    private static final int REPETITIONS = 100;
    private static final int NUMBER_OF_THREADS = 10;
    private static final long CAR_1_ID = 1;

    @RepeatedTest(REPETITIONS)
    void allClientsTryToRentOneCarAtTheSameTime() {
        // given
        List<Car> cars = List.of(new Car(CAR_1_ID));
        List<Client> clients = LongStream.rangeClosed(1, NUMBER_OF_THREADS).mapToObj(Client::new).toList();
        CarRentalService carRentalService = new LockingCarRentalService(new DefaultCarRentalService(cars, clients));
        List<Long> clientIds = clients.stream().map(Client::clientId).toList();

        // when
        try (ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS)) {
            clientIds.forEach(clientId -> {
                Runnable task = () -> tryToRentACar(carRentalService, CAR_1_ID, clientId);
                CompletableFuture.runAsync(task, executorService);
            });
        }

        // then
        assertThat(carRentalService.isCarRented(CAR_1_ID)).isTrue();
        assertThat(carRentalService.getAvailableCars()).isEmpty();
        Map<Integer, List<Client>> clientsByNumberOfCars = carRentalService.getAllClients()
                .stream()
                .collect(Collectors.groupingBy(client -> client.getRentedCarIds().size(),
                        toList()));
        assertThat(clientsByNumberOfCars).hasEntrySatisfying(0, clientsWithoutCars -> assertThat(clientsWithoutCars).hasSize(9));
        assertThat(clientsByNumberOfCars).hasEntrySatisfying(1, clientsWithCars -> assertThat(clientsWithCars).hasSize(1));
        Client client = clientsByNumberOfCars.get(1).getFirst();
        List<Long> rentedCarIds = client.getRentedCarIds();
        assertThat(rentedCarIds).hasSize(1);
        List<Car> allRentedCarsByClient = carRentalService.getAllRentedCarsByClient(client.clientId());
        assertThat(allRentedCarsByClient).hasSize(1).containsExactly(new Car(CAR_1_ID, CarStatus.RENTED));
    }

    @RepeatedTest(REPETITIONS)
    void allClientsTryToRentAllAvailableCars() {
        // given
        List<Car> cars = LongStream.rangeClosed(1, NUMBER_OF_THREADS).mapToObj(Car::new).toList();
        List<Client> clients = LongStream.rangeClosed(1, NUMBER_OF_THREADS).mapToObj(Client::new).toList();
        CarRentalService carRentalService = new LockingCarRentalService(new DefaultCarRentalService(cars, clients));
        List<Long> clientIds = clients.stream().map(Client::clientId).toList();

        // when
        try (ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS)) {
            clientIds.forEach(clientId -> {
                Runnable task = () -> tryToRentAllAvailableCars(carRentalService, clientId);
                CompletableFuture.runAsync(task, executorService);
            });
        }

        // then
        assertThat(carRentalService.getAvailableCars()).isEmpty();
        for (Car car : cars) {
            assertThat(carRentalService.isCarRented(car.carId())).isTrue();
        }
        List<Client> actualClients = carRentalService.getAllClients();
        assertThat(actualClients).isNotEmpty().hasSizeLessThanOrEqualTo(NUMBER_OF_THREADS);
        for (Client client : actualClients) {
            List<Long> rentedCarIds = client.getRentedCarIds();
            assertThat(rentedCarIds).hasSizeGreaterThanOrEqualTo(0).hasSizeLessThanOrEqualTo(NUMBER_OF_THREADS);
            List<Car> allRentedCarsByClient = carRentalService.getAllRentedCarsByClient(client.clientId());
            assertThat(allRentedCarsByClient).hasSizeGreaterThanOrEqualTo(0).hasSizeLessThanOrEqualTo(NUMBER_OF_THREADS);
        }

        List<Long> allCarIds = actualClients.stream().map(Client::getRentedCarIds).flatMap(List::stream).toList();
        assertThat(allCarIds).containsExactlyInAnyOrderElementsOf(cars.stream().map(Car::carId).toList());
    }

    @RepeatedTest(REPETITIONS)
    void someClientsRentAndReturnCarsAndOthersRead() {
        // given
        List<Car> cars = LongStream.rangeClosed(1, 5).mapToObj(Car::new).toList();
        List<Client> clients = LongStream.rangeClosed(1, 5).mapToObj(Client::new).toList();
        CarRentalService carRentalService = new LockingCarRentalService(new DefaultCarRentalService(cars, clients));
        List<Long> carIds = cars.stream().map(Car::carId).toList();
        List<Long> clientIds = clients.stream().map(Client::clientId).toList();

        try (ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS)) {
            // writers
            for (long clientId : clientIds) {
                CompletableFuture.runAsync(() -> {
                    for (int i = 0; i < 1000; i++) {
                        for (long carId : carIds) {
                            tryToRentACar(carRentalService, carId, clientId);
                            tryToReturnACar(carRentalService, carId, clientId);
                        }
                    }
                }, executorService);
            }

            // readers
            for (int i = 0; i < 5; i++) {
                CompletableFuture.runAsync(() -> {
                    for (int j = 0; j < 50_000; j++) {
                        // reading without synchronization
                        List<Client> allClients = carRentalService.getAllClients();

                        for (Client client : allClients) {
                            List<Long> rented = client.getRentedCarIds();

                            // iterate while other thread modifies
                            for (Long id : rented) {
                                assertThat(id).isNotNull();
                            }
                        }
                    }
                }, executorService);
            }
        }
    }

    private static void tryToRentACar(CarRentalService carRentalService, long carId, long clientId) {
        try {
            carRentalService.rentCar(carId, clientId);
        } catch (RuntimeException e) {
            // ignored
        }
    }

    private static void tryToReturnACar(CarRentalService carRentalService, long carId, long clientId) {
        try {
            carRentalService.returnCar(carId, clientId);
        } catch (RuntimeException e) {
            // ignored
        }
    }

    private static void tryToRentAllAvailableCars(CarRentalService carRentalService, long clientId) {
        while (true) {
            List<Car> cars = carRentalService.getAvailableCars();
            if (cars.isEmpty()) {
                break;
            }
            Car car = cars.getFirst();
            tryToRentACar(carRentalService, car.carId(), clientId);
        }
    }
}
