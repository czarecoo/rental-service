package com.czareg;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread safe.
 */
public class LockingCarRentalService implements CarRentalService {

    private final CarRentalService carRentalService;
    private final ReadWriteLock lock;

    public LockingCarRentalService(CarRentalService carRentalService) {
        this.carRentalService = carRentalService;
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public Car rentCar(long carId, long clientId) {
        lock.writeLock().lock();
        try {
            return carRentalService.rentCar(carId, clientId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Car returnCar(long carId, long clientId) {
        lock.writeLock().lock();
        try {
            return carRentalService.returnCar(carId, clientId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<Car> getAvailableCars() {
        lock.readLock().lock();
        try {
            return carRentalService.getAvailableCars();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Car> getAllRentedCarsByClient(long clientId) {
        lock.readLock().lock();
        try {
            return carRentalService.getAllRentedCarsByClient(clientId);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isCarRented(long carId) {
        lock.readLock().lock();
        try {
            return carRentalService.isCarRented(carId);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Client> getAllClients() {
        lock.readLock().lock();
        try {
            return carRentalService.getAllClients();
        } finally {
            lock.readLock().unlock();
        }
    }
}
