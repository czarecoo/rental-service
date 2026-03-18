package com.czareg;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.function.Executable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class CarRentalTask2Test {

    private static final int REPETITIONS = 100;
    private static final int NUMBER_OF_THREADS = 10;
    private static final long CAR_1_ID = 1;

    @RepeatedTest(REPETITIONS)
    void allClientsTryToRentOneCarAtTheSameTime() {
        // given
        List<Car> cars = List.of(new Car(CAR_1_ID));
        List<Client> clients = LongStream.rangeClosed(1, NUMBER_OF_THREADS).mapToObj(Client::new).toList();
        Repository repository = new Repository(cars, clients);
        CarRentalService carRentalService = new LockingCarRentalService(new DefaultCarRentalService(repository));
        List<Long> clientIds = clients.stream().map(Client::id).toList();

        // when
        try (ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS)) {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            clientIds.forEach(clientId -> {
                Runnable task = () -> {
                    try {
                        countDownLatch.await(2, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    tryToRentACar(carRentalService, CAR_1_ID, clientId);
                };
                CompletableFuture.runAsync(task, executorService);
            });
            countDownLatch.countDown(); // all tasks are started at the same time
        }

        // then
        assertThat(carRentalService.getCarStatus(CAR_1_ID)).isEqualTo(CarStatus.RENTED);
        assertThat(carRentalService.getAvailableCars()).isEmpty();
        Map<Client, List<Car>> carsByClient = carRentalService.getAllClients()
                .stream()
                .collect(Collectors.toMap(Function.identity(), client -> carRentalService.getAllRentedCarsByClient(client.id())));
        Map<Integer, List<Client>> clientsByNumberOfCars = carsByClient.keySet()
                .stream()
                .collect(Collectors.groupingBy(client -> carsByClient.get(client).size(),
                        toList()));

        assertThat(clientsByNumberOfCars).hasEntrySatisfying(0, clientsWithoutCars -> assertThat(clientsWithoutCars).hasSize(9));
        assertThat(clientsByNumberOfCars).hasEntrySatisfying(1, clientsWithCars -> assertThat(clientsWithCars).hasSize(1));
        Client client = clientsByNumberOfCars.get(1).getFirst();
        List<Car> rentedCars = carsByClient.get(client);
        assertThat(rentedCars).hasSize(1);
        List<Car> allRentedCarsByClient = carRentalService.getAllRentedCarsByClient(client.id());
        assertThat(allRentedCarsByClient).hasSize(1).containsExactly(new Car(CAR_1_ID));
    }

    @RepeatedTest(REPETITIONS)
    void allClientsTryToRentAllAvailableCars() {
        // given
        List<Car> cars = LongStream.rangeClosed(1, NUMBER_OF_THREADS).mapToObj(Car::new).toList();
        List<Client> clients = LongStream.rangeClosed(1, NUMBER_OF_THREADS).mapToObj(Client::new).toList();
        Repository repository = new Repository(cars, clients);
        CarRentalService carRentalService = new LockingCarRentalService(new DefaultCarRentalService(repository));
        List<Long> clientIds = clients.stream().map(Client::id).toList();

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
            assertEquals(CarStatus.RENTED, carRentalService.getCarStatus(car.id()));
        }
        List<Client> actualClients = carRentalService.getAllClients();
        assertThat(actualClients).isNotEmpty().hasSizeLessThanOrEqualTo(NUMBER_OF_THREADS);
        List<Car> allRentedCars = new ArrayList<>();
        for (Client client : actualClients) {
            List<Car> rentedCars = carRentalService.getAllRentedCarsByClient(client.id());
            assertThat(rentedCars).hasSizeGreaterThanOrEqualTo(0).hasSizeLessThanOrEqualTo(NUMBER_OF_THREADS);
            allRentedCars.addAll(rentedCars);
        }

        assertThat(allRentedCars).containsExactlyInAnyOrderElementsOf(cars.stream().toList());
    }

    @RepeatedTest(5)
    void someClientsRentAndReturnCarsAndOthersRead() throws InterruptedException {
        // given
        List<Car> cars = LongStream.rangeClosed(1, 5).mapToObj(Car::new).toList();
        List<Client> clients = LongStream.rangeClosed(1, 5).mapToObj(Client::new).toList();
        Repository repository = new Repository(cars, clients);
        CarRentalService carRentalService = new LockingCarRentalService(new DefaultCarRentalService(repository));
        List<Long> carIds = cars.stream().map(Car::id).toList();
        List<Long> clientIds = clients.stream().map(Client::id).toList();

        try (ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS)) {
            List<Callable<Void>> callables = new ArrayList<>();
            // writers
            for (long clientId : clientIds) {
                callables.add(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        for (long carId : carIds) {
                            tryToRentACar(carRentalService, carId, clientId);
                            tryToReturnACar(carRentalService, carId, clientId);
                        }
                    }
                    return null;
                });
            }

            // readers
            for (long clientId : clientIds) {
                callables.add(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        List<Car> allRentedCarsByClient = carRentalService.getAllRentedCarsByClient(clientId);
                        for (Car car : allRentedCarsByClient) {
                            assertThat(car).isNotNull();
                        }
                    }
                    return null;
                });
            }

            List<Future<Void>> futures = executorService.invokeAll(callables, 1, TimeUnit.SECONDS);

            assertAll(futures.stream()
                    .map(future -> () -> {
                        try {
                            future.get();
                        } catch (CancellationException ignored) {
                            // expected due to timeout
                        } catch (ExecutionException e) {
                            throw e.getCause(); // unwrap real exception
                        }
                    }));
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
            tryToRentACar(carRentalService, car.id(), clientId);
        }
    }
}
