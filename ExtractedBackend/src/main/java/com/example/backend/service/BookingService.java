package com.example.backend.service;

import com.example.backend.dto.BookingRequest;
import com.example.backend.dto.BookingCreateRequest;
import com.example.backend.dto.BookingResponse;
import com.example.backend.dto.VehicleSummaryDto;
import com.example.backend.mapper.BookingMapper;
import com.example.backend.model.Booking;
import com.example.backend.model.BookingStatus;
import com.example.backend.model.User;
import com.example.backend.model.UserRole;
import com.example.backend.model.Vehicle;
import com.example.backend.model.VehicleAvailabilityStatus;
import com.example.backend.repository.BookingRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE
    );
    private static final Map<String, String> CONTENT_TYPE_EXTENSION = Map.of(
            MediaType.IMAGE_JPEG_VALUE, ".jpg",
            MediaType.IMAGE_PNG_VALUE, ".png"
    );

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;

    @Value("${app.upload.base-dir:uploads}")
    private String uploadBaseDir;

    @Value("${app.upload.max-file-size-bytes:5242880}")
    private long maxFileSizeBytes;

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        BookingCreateRequest mappedRequest = new BookingCreateRequest();
        mappedRequest.setVehicleId(request.getVehicleId());
        mappedRequest.setPickupDate(request.getStartDate());
        mappedRequest.setReturnDate(request.getEndDate());
        return createBookingWithDocuments(mappedRequest, null, null);
    }

    @Transactional
    public BookingResponse createBookingWithDocuments(
            BookingCreateRequest request,
            MultipartFile nicFront,
            MultipartFile drivingLicense) {
        User user = getAuthenticatedUser();

        validateDateRange(request.getPickupDate(), request.getReturnDate());
        getBookableVehicle(request.getVehicleId());
        validateBookingOverlap(request.getVehicleId(), request.getPickupDate(), request.getReturnDate());

        boolean hasNicFront = isPresent(user.getNicFrontPath());
        boolean hasDrivingLicense = isPresent(user.getDrivingLicensePath());

        if (!hasNicFront && (nicFront == null || nicFront.isEmpty())) {
            throw new IllegalArgumentException("nicFront is required because no NIC front document is stored for this user");
        }
        if (!hasDrivingLicense && (drivingLicense == null || drivingLicense.isEmpty())) {
            throw new IllegalArgumentException("drivingLicense is required because no driving license document is stored for this user");
        }

        if (nicFront != null && !nicFront.isEmpty()) {
            String previousPath = user.getNicFrontPath();
            String storedPath = storeUserDocument(user.getId(), nicFront, "nic-front");
            user.setNicFrontPath(storedPath);
            deleteIfExists(previousPath);
        }

        if (drivingLicense != null && !drivingLicense.isEmpty()) {
            String previousPath = user.getDrivingLicensePath();
            String storedPath = storeUserDocument(user.getId(), drivingLicense, "driving-license");
            user.setDrivingLicensePath(storedPath);
            deleteIfExists(previousPath);
        }

        user.setDocumentsUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        Booking booking = bookingMapper.toEntity(request);
        booking.setUserId(user.getId());
        booking.setBookingTime(LocalDateTime.now());
        booking.setStatus(BookingStatus.PENDING);
        booking.setNicFrontPath(user.getNicFrontPath());
        booking.setDrivingLicensePath(user.getDrivingLicensePath());

        Booking savedBooking = bookingRepository.save(booking);
        return toFullDto(savedBooking);
    }

    public List<BookingResponse> getMyBookings() {
        User user = getAuthenticatedUser();
        return bookingRepository.findByUserId(user.getId()).stream()
                .map(this::toFullDto)
                .toList();
    }

    public BookingResponse getBooking(String id) {
        User user = getAuthenticatedUser();
        String bookingId = requireNonBlank(id, "id");
        Booking booking = bookingRepository.findById(Objects.requireNonNull(bookingId, "id"))
            .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        enforceOwnerOrAdmin(user, booking.getUserId());
        return toFullDto(booking);
    }

    @Transactional
    public void cancelBooking(String bookingId) {
        User user = getAuthenticatedUser();
        String requiredBookingId = requireNonBlank(bookingId, "bookingId");
        Booking booking = bookingRepository.findById(Objects.requireNonNull(requiredBookingId, "bookingId"))
            .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + requiredBookingId));
        enforceOwnerOrAdmin(user, booking.getUserId());

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
        String requiredBookingId = requireNonBlank(bookingId, "bookingId");
        Booking booking = bookingRepository.findById(Objects.requireNonNull(requiredBookingId, "bookingId"))
            .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + requiredBookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING bookings can be approved");
        }

        List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                booking.getVehicleId(), booking.getStartDate(), booking.getEndDate());
        boolean hasRealOverlap = overlaps.stream().anyMatch(existing -> !existing.getId().equals(requiredBookingId));
        if (hasRealOverlap) {
            throw new IllegalStateException("Cannot approve due to an overlapping booking");
        }

        booking.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(booking);
    }

    @Transactional
    public void rejectBooking(String bookingId, String reason) {
        String requiredBookingId = requireNonBlank(bookingId, "bookingId");
        Booking booking = bookingRepository.findById(Objects.requireNonNull(requiredBookingId, "bookingId"))
            .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + requiredBookingId));

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

        String requiredBookingId = requireNonBlank(bookingId, "bookingId");
        Booking booking = bookingRepository.findById(Objects.requireNonNull(requiredBookingId, "bookingId"))
            .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + requiredBookingId));

        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new IllegalStateException("Advance amount can only be set for APPROVED bookings");
        }

        booking.setAdvanceAmount(amount);
        bookingRepository.save(booking);
    }

    public DocumentContent loadMyNicFrontDocument() {
        User user = getAuthenticatedUser();
        return loadUserDocument(user, true);
    }

    public DocumentContent loadMyDrivingLicenseDocument() {
        User user = getAuthenticatedUser();
        return loadUserDocument(user, false);
    }

    public DocumentContent loadUserNicFrontDocumentForAdmin(String userId) {
        User currentUser = getAuthenticatedUser();
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(FORBIDDEN, "Admin role required");
        }
        String requiredUserId = requireNonBlank(userId, "userId");
        User targetUser = userRepository.findById(Objects.requireNonNull(requiredUserId, "userId"))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found: " + requiredUserId));
        return loadUserDocument(targetUser, true);
    }

    public DocumentContent loadUserDrivingLicenseDocumentForAdmin(String userId) {
        User currentUser = getAuthenticatedUser();
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(FORBIDDEN, "Admin role required");
        }
        String requiredUserId = requireNonBlank(userId, "userId");
        User targetUser = userRepository.findById(Objects.requireNonNull(requiredUserId, "userId"))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found: " + requiredUserId));
        return loadUserDocument(targetUser, false);
    }

    private void validateDateRange(LocalDateTime pickupDate, LocalDateTime returnDate) {
        if (pickupDate == null || returnDate == null) {
            throw new IllegalArgumentException("pickupDate and returnDate are required");
        }
        if (!returnDate.isAfter(pickupDate)) {
            throw new IllegalArgumentException("returnDate must be after pickupDate");
        }
    }

    private void getBookableVehicle(String vehicleId) {
        String requiredVehicleId = requireNonBlank(vehicleId, "vehicleId");
        Vehicle vehicle = vehicleRepository.findById(Objects.requireNonNull(requiredVehicleId, "vehicleId"))
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + requiredVehicleId));

        if (!vehicle.isAvailable() || vehicle.isUnderMaintenance() || vehicle.isAdminHeld()) {
            throw new IllegalStateException("Vehicle is not available for booking");
        }
    }

    private void validateBookingOverlap(String vehicleId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Booking> overlaps = bookingRepository.findOverlappingBookings(vehicleId, startDate, endDate);
        if (!overlaps.isEmpty()) {
            throw new IllegalStateException("Vehicle is already booked for the requested period");
        }
    }

    private String storeUserDocument(String userId, MultipartFile file, String filePrefix) {
        validateFile(file, filePrefix);

        String extension = CONTENT_TYPE_EXTENSION.get(file.getContentType());
        String generatedName = filePrefix + "-" + UUID.randomUUID() + extension;

        Path userDirectory = resolveUserDirectory(userId);
        Path targetPath = userDirectory.resolve(generatedName).normalize();

        if (!targetPath.startsWith(userDirectory)) {
            throw new IllegalArgumentException("Invalid storage path");
        }

        try {
            Files.createDirectories(userDirectory);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store " + filePrefix + " file", ex);
        }

        return "user-docs/" + userId + "/" + generatedName;
    }

    private Path resolveUserDirectory(String userId) {
        return Paths.get(uploadBaseDir).toAbsolutePath().normalize()
                .resolve("user-docs")
                .resolve(userId)
                .normalize();
    }

    private void validateFile(MultipartFile file, String fieldName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(fieldName + " must be image/jpeg or image/png");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException(fieldName + " exceeds the 5MB size limit");
        }
    }

    private void deleteIfExists(String storedPath) {
        if (!isPresent(storedPath)) {
            return;
        }
        try {
            Path filePath = resolveStoredPath(storedPath);
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to replace existing document", ex);
        }
    }

    private DocumentContent loadUserDocument(User user, boolean nicFront) {
        String storedPath = nicFront ? user.getNicFrontPath() : user.getDrivingLicensePath();
        if (!isPresent(storedPath)) {
            throw new ResponseStatusException(NOT_FOUND, "Requested document is not uploaded");
        }

        Path filePath = resolveStoredPath(storedPath);
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new ResponseStatusException(NOT_FOUND, "Stored document file not found");
        }

        try {
            Resource resource = new UrlResource(Objects.requireNonNull(filePath.toUri(), "file URI"));
            if (!resource.exists()) {
                throw new ResponseStatusException(NOT_FOUND, "Stored document file not found");
            }
            String contentType = Files.probeContentType(filePath);
            if (!isPresent(contentType)) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            return new DocumentContent(resource, contentType, filePath.getFileName().toString());
        } catch (MalformedURLException ex) {
            throw new ResponseStatusException(NOT_FOUND, "Stored document file not found");
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read stored document", ex);
        }
    }

    private Path resolveStoredPath(String storedPath) {
        Path basePath = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
        Path resolved = basePath.resolve(storedPath).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new IllegalArgumentException("Invalid stored path");
        }
        return resolved;
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

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    public record DocumentContent(Resource resource, String contentType, String fileName) {
    }

    private BookingResponse toFullDto(Booking booking) {
        BookingResponse response = bookingMapper.toDto(booking);
        String vehicleId = booking.getVehicleId();
        if (isPresent(vehicleId)) {
            vehicleRepository.findById(Objects.requireNonNull(vehicleId, "vehicleId"))
                    .ifPresent(vehicle -> response.setVehicle(toVehicleSummaryDto(vehicle)));
        }
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
