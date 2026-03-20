package com.example.backend.service;

import com.example.backend.dto.BookingRequest;
import com.example.backend.dto.BookingResponse;
import com.example.backend.dto.VehicleSummaryDto;
import com.example.backend.mapper.BookingMapper;
import com.example.backend.model.Booking;
import com.example.backend.model.BookingStatus;
import com.example.backend.model.Vehicle;
import com.example.backend.model.VehicleAvailabilityStatus;
import com.example.backend.repository.BookingRepository;
import com.example.backend.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final String DEFAULT_USER_ID = "guest-user";

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingMapper bookingMapper;

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        if (!request.getStartDate().isBefore(request.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + request.getVehicleId()));

        if (!vehicle.isAvailable() || vehicle.isUnderMaintenance() || vehicle.isAdminHeld()) {
            throw new IllegalStateException("Vehicle is not available for booking");
        }

        List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                request.getVehicleId(), request.getStartDate(), request.getEndDate());
        if (!overlaps.isEmpty()) {
            throw new IllegalStateException("Vehicle is already booked for the requested period");
        }

        Booking booking = bookingMapper.toEntity(request);
        booking.setUserId(DEFAULT_USER_ID);
        booking.setBookingTime(LocalDateTime.now());
        booking.setStatus(BookingStatus.PENDING);

        Booking saved = bookingRepository.save(booking);
        return toFullDto(saved);
    }

    public List<BookingResponse> getMyBookings() {
        return bookingRepository.findAll().stream()
                .map(this::toFullDto)
                .toList();
    }

    public BookingResponse getBooking(String id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));
        return toFullDto(booking);
    }

    @Transactional
    public void cancelBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Completed bookings cannot be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    public List<String> getBookedDatesForVehicle(String vehicleId) {
        List<Booking> activeBookings = bookingRepository.findActiveBookingsForVehicle(vehicleId);
        List<String> bookedDates = new ArrayList<>();

        for (Booking booking : activeBookings) {
            LocalDate start = booking.getStartDate().toLocalDate();
            LocalDate end = booking.getEndDate().toLocalDate();
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                bookedDates.add(date.toString());
            }
        }
        return bookedDates;
    }

    public List<BookingResponse> getAllBookingRequests() {
        return bookingRepository.findAll().stream()
                .map(this::toFullDto)
                .toList();
    }

    @Transactional
    public void approveBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING bookings can be approved");
        }

        List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                booking.getVehicleId(), booking.getStartDate(), booking.getEndDate());
        boolean hasRealOverlap = overlaps.stream().anyMatch(existing -> !existing.getId().equals(bookingId));
        if (hasRealOverlap) {
            throw new IllegalStateException("Cannot approve due to an overlapping booking");
        }

        booking.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(booking);
    }

    @Transactional
    public void rejectBooking(String bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING bookings can be rejected");
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking.setRejectionReason(reason);
        bookingRepository.save(booking);
    }

    @Transactional
    public void setAdvanceAmount(String bookingId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Advance amount must be greater than zero");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new IllegalStateException("Advance amount can only be set for APPROVED bookings");
        }

        booking.setAdvanceAmount(amount);
        bookingRepository.save(booking);
    }

    private BookingResponse toFullDto(Booking booking) {
        BookingResponse response = bookingMapper.toDto(booking);
        vehicleRepository.findById(booking.getVehicleId())
                .ifPresent(vehicle -> response.setVehicle(toVehicleSummaryDto(vehicle)));
        return response;
    }

    private VehicleSummaryDto toVehicleSummaryDto(Vehicle vehicle) {
        VehicleSummaryDto dto = new VehicleSummaryDto();
        dto.setId(vehicle.getId());
        dto.setName(vehicle.getName());
        dto.setThumbnailUrl(vehicle.getThumbnailUrl());
        dto.setVehicleTypeId(vehicle.getVehicleTypeId());
        dto.setRentalPrice(vehicle.getRentalPrice());
        VehicleAvailabilityStatus resolvedStatus = vehicle.getAvailabilityStatus() != null
                ? vehicle.getAvailabilityStatus()
                : VehicleAvailabilityStatus.fromFlags(vehicle.isAvailable(), vehicle.isUnderMaintenance(), vehicle.isAdminHeld());
        dto.setAvailabilityStatus(resolvedStatus.name());
        dto.setMake(vehicle.getMake());
        dto.setModel(vehicle.getModel());
        dto.setYear(vehicle.getYear());
        dto.setLicensePlate(vehicle.getLicensePlate());
        dto.setType(vehicle.getType());
        dto.setDescription(vehicle.getDescription());
        dto.setRentalPricePerDay(vehicle.getRentalPricePerDay());
        dto.setImageUrls(vehicle.getImageUrls());
        dto.setAvailable(vehicle.isAvailable());
        dto.setUnderMaintenance(vehicle.isUnderMaintenance());
        dto.setAdminHeld(vehicle.isAdminHeld());
        return dto;
    }
}
