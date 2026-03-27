package com.example.backend.service;

import com.example.backend.dto.BookingCreateRequest;
import com.example.backend.dto.BookingResponse;
import com.example.backend.dto.VehicleSummaryDto;
import com.example.backend.exception.ApiFieldError;
import com.example.backend.exception.RequestValidationException;
import com.example.backend.model.Booking;
import com.example.backend.model.BookingStatus;
import com.example.backend.model.DocumentCategory;
import com.example.backend.model.User;
import com.example.backend.model.UserRole;
import com.example.backend.model.Vehicle;
import com.example.backend.model.VehicleAvailabilityStatus;
import com.example.backend.repository.BookingRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final UserDocumentService userDocumentService;

    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request) {
        User user = getAuthenticatedUser();

        validateDateRange(request.getStartDate(), request.getEndDate());
        LocalDateTime bookingStart = toStartOfDay(request.getStartDate());
        LocalDateTime bookingEnd = toEndOfDay(request.getEndDate());

        getBookableVehicle(request.getVehicleId());
        validateBookingOverlap(request.getVehicleId(), bookingStart, bookingEnd);

        String nicFrontDocumentId = userDocumentService
                .requireMandatoryDocument(user.getId(), DocumentCategory.NIC_FRONT)
                .getId();
        String drivingLicenseDocumentId = userDocumentService
                .requireMandatoryDocument(user.getId(), DocumentCategory.DRIVING_LICENSE)
                .getId();

        Booking booking = new Booking();
        booking.setVehicleId(request.getVehicleId());
        booking.setStartDate(bookingStart);
        booking.setEndDate(bookingEnd);
        booking.setUserId(user.getId());
        booking.setBookingTime(LocalDateTime.now());
        booking.setStatus(BookingStatus.PENDING);
        booking.setNicFrontDocumentId(nicFrontDocumentId);
        booking.setDrivingLicenseDocumentId(drivingLicenseDocumentId);

        return toFullDto(bookingRepository.save(booking));
    }

    public List<BookingResponse> getMyBookings() {
        User user = getAuthenticatedUser();
        return bookingRepository.findByUserId(user.getId()).stream()
                .map(this::toFullDto)
                .toList();
    }

    public BookingResponse getBooking(String id) {
        User user = getAuthenticatedUser();
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Booking not found: " + id));
        enforceOwnerOrAdmin(user, booking.getUserId());
        return toFullDto(booking);
    }

    @Transactional
    public void cancelBooking(String bookingId) {
        User user = getAuthenticatedUser();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Booking not found: " + bookingId));
        enforceOwnerOrAdmin(user, booking.getUserId());

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ResponseStatusException(BAD_REQUEST, "Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ResponseStatusException(BAD_REQUEST, "Completed bookings cannot be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    public List<String> getBookedDatesForVehicle(String vehicleId) {
        List<Booking> activeBookings = bookingRepository.findActiveBookingsForVehicle(vehicleId);
        List<String> bookedDates = new ArrayList<>();

        for (Booking booking : activeBookings) {
            LocalDate start = booking.getStartDate().toLocalDate();
            LocalDate end = booking.getEndDate().toLocalTime().equals(java.time.LocalTime.MIDNIGHT)
                    ? booking.getEndDate().toLocalDate().minusDays(1)
                    : booking.getEndDate().toLocalDate();
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                bookedDates.add(date.toString());
            }
        }
        return bookedDates;
    }

    public Page<BookingResponse> getAllBookings(BookingStatus status, String search, int page, int size) {
        if (page < 0 || size <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "page must be >= 0 and size must be > 0");
        }

        List<Booking> source = status == null
                ? bookingRepository.findAll()
                : bookingRepository.findByStatus(status);

        List<Booking> filtered = source.stream()
                .filter(booking -> matchesSearch(booking, search))
                .sorted(Comparator.comparing(Booking::getBookingTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int start = Math.min(page * size, filtered.size());
        int end = Math.min(start + size, filtered.size());

        List<BookingResponse> content = filtered.subList(start, end).stream()
                .map(this::toFullDto)
                .toList();

        return new PageImpl<>(content, PageRequest.of(page, size), filtered.size());
    }

    @Transactional
    public void approveBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(BAD_REQUEST, "Only PENDING bookings can be approved");
        }

        List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                booking.getVehicleId(), booking.getStartDate(), booking.getEndDate());
        boolean hasRealOverlap = overlaps.stream().anyMatch(existing -> !existing.getId().equals(bookingId));
        if (hasRealOverlap) {
            throw new ResponseStatusException(BAD_REQUEST, "Cannot approve due to an overlapping booking");
        }

        booking.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(booking);
    }

    @Transactional
    public void rejectBooking(String bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(BAD_REQUEST, "Only PENDING bookings can be rejected");
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking.setRejectionReason(reason);
        bookingRepository.save(booking);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(BAD_REQUEST, "startDate and endDate are required");
        }

        LocalDate today = LocalDate.now();
        if (startDate.isBefore(today)) {
            throw new RequestValidationException(
                    "Validation failed",
                    new ApiFieldError("startDate", startDate, "startDate must not be in the past"));
        }
        if (endDate.isBefore(today)) {
            throw new RequestValidationException(
                    "Validation failed",
                    new ApiFieldError("endDate", endDate, "endDate must not be in the past"));
        }
        if (!endDate.isAfter(startDate)) {
            throw new RequestValidationException(
                    "Validation failed",
                    new ApiFieldError("endDate", endDate, "endDate must be after startDate"));
        }
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime toEndOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay();
    }

    private void getBookableVehicle(String vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Vehicle not found: " + vehicleId));

        if (!vehicle.isAvailable() || vehicle.isUnderMaintenance() || vehicle.isAdminHeld()) {
            throw new ResponseStatusException(BAD_REQUEST, "Vehicle is not available for booking");
        }
    }

    private void validateBookingOverlap(String vehicleId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Booking> overlaps = bookingRepository.findOverlappingBookings(vehicleId, startDate, endDate);
        if (!overlaps.isEmpty()) {
            throw new RequestValidationException(
                    "Validation failed",
                    new ApiFieldError("startDate", startDate.toLocalDate(), "Vehicle is already booked for the requested period"));
        }
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication is required");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Authenticated user not found"));
    }

    private void enforceOwnerOrAdmin(User currentUser, String ownerId) {
        boolean isOwner = currentUser.getId() != null && currentUser.getId().equals(ownerId);
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(FORBIDDEN, "Access denied for this booking");
        }
    }

    private boolean matchesSearch(Booking booking, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }

        String q = search.toLowerCase(Locale.ROOT);
        return contains(booking.getId(), q)
                || contains(booking.getVehicleId(), q)
                || contains(booking.getUserId(), q)
                || contains(booking.getRejectionReason(), q);
    }

    private boolean contains(String text, String q) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(q);
    }

    private BookingResponse toFullDto(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setUserId(booking.getUserId());
        response.setVehicleId(booking.getVehicleId());
        response.setBookingTime(booking.getBookingTime());
        response.setStartDate(booking.getStartDate());
        response.setEndDate(booking.getEndDate());
        response.setStatus(booking.getStatus());
        response.setAdvanceAmount(booking.getAdvanceAmount());
        response.setAdvancePaid(booking.isAdvancePaid());
        response.setRejectionReason(booking.getRejectionReason());
        response.setNicFrontDocumentId(booking.getNicFrontDocumentId());
        response.setDrivingLicenseDocumentId(booking.getDrivingLicenseDocumentId());
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
