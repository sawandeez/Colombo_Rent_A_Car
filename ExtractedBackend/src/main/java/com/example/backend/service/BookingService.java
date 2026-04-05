package com.example.backend.service;

import com.example.backend.dto.AdminUserResponse;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
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

    private static final ZoneId COLOMBO_ZONE = ZoneId.of("Asia/Colombo");

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final UserDocumentService userDocumentService;

    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request) {
        User user = getAuthenticatedUser();

        ResolvedBookingWindow resolvedWindow = resolveRequestedWindow(request);

        validateDateRange(
                resolvedWindow.startDateTime(),
                resolvedWindow.endDateTime(),
                request.getStartDate(),
                request.getEndDate());

        getBookableVehicle(request.getVehicleId());
        validateBookingOverlap(
                request.getVehicleId(),
                resolvedWindow.startDateTime(),
                resolvedWindow.endDateTime(),
                request.getStartDate());

        String nicFrontDocumentId = userDocumentService
                .requireMandatoryDocument(user.getId(), DocumentCategory.NIC_FRONT)
                .getId();
        String drivingLicenseDocumentId = userDocumentService
                .requireMandatoryDocument(user.getId(), DocumentCategory.DRIVING_LICENSE)
                .getId();

        Booking booking = new Booking();
        booking.setVehicleId(request.getVehicleId());
        booking.setStartDate(resolvedWindow.startDateTime());
        booking.setEndDate(resolvedWindow.endDateTime());
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

    public Page<BookingResponse> getAllBookings(BookingStatus status, String search, String fromDate, String toDate, int page, int size) {
        if (page < 0 || size <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "page must be >= 0 and size must be > 0");
        }

        DateFilter dateFilter = resolveDateFilter(fromDate, toDate);

        List<Booking> source = status == null
                ? bookingRepository.findAll()
                : bookingRepository.findByStatus(status);

        List<Booking> filtered = source.stream()
                .filter(booking -> matchesSearch(booking, search))
                .filter(booking -> matchesDateFilter(booking, dateFilter))
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

    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate, String rawStartDate, String rawEndDate) {
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(BAD_REQUEST, "startDate and endDate are required");
        }

        LocalDate today = LocalDate.now(COLOMBO_ZONE);
        if (startDate.toLocalDate().isBefore(today)) {
            throw new RequestValidationException(
                    "Validation failed",
                    new ApiFieldError("startDate", rawStartDate, "startDate must not be in the past"));
        }
        if (endDate.toLocalDate().isBefore(today)) {
            throw new RequestValidationException(
                    "Validation failed",
                    new ApiFieldError("endDate", rawEndDate, "endDate must not be in the past"));
        }
        if (endDate.isBefore(startDate)) {
            throw new RequestValidationException(
                    "Validation failed",
                    new ApiFieldError("endDate", rawEndDate, "endDate must be on or after startDate"));
        }
    }

    private void getBookableVehicle(String vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Vehicle not found: " + vehicleId));

        if (!vehicle.isAvailable() || vehicle.isUnderMaintenance() || vehicle.isAdminHeld()) {
            throw new ResponseStatusException(BAD_REQUEST, "Vehicle is not available for booking");
        }
    }

    private void validateBookingOverlap(String vehicleId, LocalDateTime startDate, LocalDateTime endDate, String rejectedValue) {
        List<Booking> overlaps = bookingRepository.findOverlappingBookings(vehicleId, startDate, endDate);
        if (!overlaps.isEmpty()) {
            throw new RequestValidationException(
                    "Validation failed",
                    new ApiFieldError("startDate", rejectedValue, "Vehicle is already booked for the requested period"));
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

    private DateFilter resolveDateFilter(String fromDate, String toDate) {
        LocalDate from = parseFlexibleDateParam("fromDate", fromDate);
        LocalDate to = parseFlexibleDateParam("toDate", toDate);

        if (from != null && to != null && to.isBefore(from)) {
            throw new RequestValidationException(
                    "Validation failed",
                    new ApiFieldError("toDate", toDate, "toDate must be on or after fromDate"));
        }

        return new DateFilter(from, to);
    }

    private boolean matchesDateFilter(Booking booking, DateFilter dateFilter) {
        if (dateFilter.from() == null && dateFilter.to() == null) {
            return true;
        }

        LocalDate bookingStart = booking.getStartDate().toLocalDate();
        LocalDate bookingEnd = toInclusiveBookingEndDate(booking.getEndDate());

        if (dateFilter.from() != null && bookingEnd.isBefore(dateFilter.from())) {
            return false;
        }
        return dateFilter.to() == null || !bookingStart.isAfter(dateFilter.to());
    }

    private LocalDate parseFlexibleDateParam(String fieldName, String rawValue) {
        String value = normalizeText(rawValue);
        if (value == null) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(value).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(COLOMBO_ZONE).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Instant.parse(value).atZone(COLOMBO_ZONE).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }

        throw new RequestValidationException(
                "Validation failed",
                new ApiFieldError(fieldName, rawValue, "Invalid date format. Use yyyy-MM-dd or ISO-8601 datetime."));
    }

    private ResolvedBookingWindow resolveRequestedWindow(BookingCreateRequest request) {
        List<ApiFieldError> errors = new ArrayList<>();

        if (normalizeText(request.getStartDate()) == null) {
            errors.add(new ApiFieldError("startDate", request.getStartDate(), "startDate is required"));
        }
        if (normalizeText(request.getEndDate()) == null) {
            errors.add(new ApiFieldError("endDate", request.getEndDate(), "endDate is required"));
        }

        TemporalInput startInput = parseFlexibleTemporal("startDate", request.getStartDate(), errors);
        TemporalInput endInput = parseFlexibleTemporal("endDate", request.getEndDate(), errors);
        TemporalInput pickupInput = parseFlexibleTemporal("pickupDateTime", request.getPickupDateTime(), errors);
        TemporalInput returnInput = parseFlexibleTemporal("returnDateTime", request.getReturnDateTime(), errors);

        if (!errors.isEmpty()) {
            throw new RequestValidationException("Validation failed", errors);
        }

        LocalDateTime start = startInput != null && startInput.isDateOnly() && pickupInput != null
                ? pickupInput.dateTime()
                : startInput == null ? null : startInput.dateTime();

        LocalDateTime end = endInput != null && endInput.isDateOnly() && returnInput != null && !returnInput.dateTime().toLocalTime().equals(LocalTime.MIDNIGHT)
                ? returnInput.dateTime()
                : endInput == null ? null : endInput.dateTime();

        return new ResolvedBookingWindow(start, end);
    }

    private TemporalInput parseFlexibleTemporal(String fieldName, String rawValue, List<ApiFieldError> errors) {
        String value = normalizeText(rawValue);
        if (value == null) {
            return null;
        }

        try {
            LocalDate date = LocalDate.parse(value);
            boolean endField = "endDate".equals(fieldName);
            return new TemporalInput(endField ? date.plusDays(1).atStartOfDay() : date.atStartOfDay(), true);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return new TemporalInput(LocalDateTime.parse(value), false);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return new TemporalInput(OffsetDateTime.parse(value).atZoneSameInstant(COLOMBO_ZONE).toLocalDateTime(), false);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return new TemporalInput(Instant.parse(value).atZone(COLOMBO_ZONE).toLocalDateTime(), false);
        } catch (DateTimeParseException ignored) {
        }

        errors.add(new ApiFieldError(fieldName, rawValue, "Invalid date format. Use yyyy-MM-dd or ISO-8601 datetime."));
        return null;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
        response.setPaymentStatus(booking.getPaymentStatus());
        response.setPaymentDate(booking.getPaymentDate());
        response.setRejectionReason(booking.getRejectionReason());
        response.setNicFrontDocumentId(booking.getNicFrontDocumentId());
        response.setDrivingLicenseDocumentId(booking.getDrivingLicenseDocumentId());

        userRepository.findById(booking.getUserId())
                .ifPresent(user -> response.setUser(AdminUserResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .username(user.getEmail())
                        .build()));

        vehicleRepository.findById(booking.getVehicleId())
                .ifPresent(vehicle -> {
                    VehicleSummaryDto vehicleDto = toVehicleSummaryDto(vehicle);
                    response.setVehicle(vehicleDto);
                    response.setVehicleName(vehicleDto.getName());
                    response.setTotalPrice(calculateTotalPrice(booking, vehicleDto));
                });

        if (response.getTotalPrice() == null) {
            response.setTotalPrice(booking.getAdvanceAmount());
        }

        return response;
    }

    private BigDecimal calculateTotalPrice(Booking booking, VehicleSummaryDto vehicle) {
        BigDecimal ratePerDay = vehicle.getRentalPricePerDay() != null
                ? vehicle.getRentalPricePerDay()
                : vehicle.getRentalPrice();
        if (ratePerDay == null || booking.getStartDate() == null || booking.getEndDate() == null) {
            return booking.getAdvanceAmount();
        }

        long durationMinutes = Math.max(0L, Duration.between(booking.getStartDate(), booking.getEndDate()).toMinutes());
        long chargeableDays = Math.max(1L, (long) Math.ceil(durationMinutes / (24d * 60d)));
        return ratePerDay.multiply(BigDecimal.valueOf(chargeableDays));
    }

    private LocalDate toInclusiveBookingEndDate(LocalDateTime bookingEnd) {
        return bookingEnd.toLocalTime().equals(LocalTime.MIDNIGHT)
                ? bookingEnd.toLocalDate().minusDays(1)
                : bookingEnd.toLocalDate();
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

    private record TemporalInput(LocalDateTime dateTime, boolean isDateOnly) {
    }

    private record ResolvedBookingWindow(LocalDateTime startDateTime, LocalDateTime endDateTime) {
    }

    private record DateFilter(LocalDate from, LocalDate to) {
    }
}
