package com.example.backend.repository;

import com.example.backend.model.Booking;
import com.example.backend.model.BookingStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends MongoRepository<Booking, String> {

    List<Booking> findByUserId(String userId);

    // Find overlapping bookings for a vehicle that are NOT cancelled or rejected
    @Query("{ 'vehicleId': ?0, 'status': { $nin: ['CANCELLED', 'REJECTED'] }, 'startDate': { $lt: ?2 }, 'endDate': { $gt: ?1 } }")
    List<Booking> findOverlappingBookings(String vehicleId, LocalDateTime start, LocalDateTime end);

    List<Booking> findByStatus(BookingStatus status);

    @Query("{ 'vehicleId': ?0, 'status': { $nin: ['CANCELLED', 'REJECTED'] } }")
    List<Booking> findActiveBookingsForVehicle(String vehicleId);
}
