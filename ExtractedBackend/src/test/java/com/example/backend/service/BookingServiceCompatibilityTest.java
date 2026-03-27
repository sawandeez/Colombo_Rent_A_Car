package com.example.backend.service;

import com.example.backend.dto.BookingCreateRequest;
import com.example.backend.dto.BookingResponse;
import com.example.backend.exception.RequestValidationException;
import com.example.backend.model.Booking;
import com.example.backend.model.DocumentCategory;
import com.example.backend.model.User;
import com.example.backend.model.UserDocument;
import com.example.backend.model.UserRole;
import com.example.backend.model.Vehicle;
import com.example.backend.repository.BookingRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.VehicleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceCompatibilityTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDocumentService userDocumentService;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(bookingRepository, vehicleRepository, userRepository, userDocumentService);

        User user = new User();
        user.setId("user-1");
        user.setEmail("customer@example.com");
        user.setRole(UserRole.CUSTOMER);

        Vehicle vehicle = new Vehicle();
        vehicle.setId("veh-1");
        vehicle.setName("Toyota Prius");
        vehicle.setRentalPricePerDay(new BigDecimal("15000"));
        vehicle.setAvailable(true);
        vehicle.setUnderMaintenance(false);
        vehicle.setAdminHeld(false);

        UserDocument nicFront = new UserDocument();
        nicFront.setId("doc-nic");
        UserDocument license = new UserDocument();
        license.setId("doc-license");

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(vehicleRepository.findById("veh-1")).thenReturn(Optional.of(vehicle));
        when(bookingRepository.findOverlappingBookings(eq("veh-1"), any(), any())).thenReturn(List.of());
        when(userDocumentService.requireMandatoryDocument("user-1", DocumentCategory.NIC_FRONT)).thenReturn(nicFront);
        when(userDocumentService.requireMandatoryDocument("user-1", DocumentCategory.DRIVING_LICENSE)).thenReturn(license);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId("booking-1");
            return booking;
        });

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("customer@example.com", "n/a"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createBookingAcceptsExistingIsoDateTimePayload() {
        BookingCreateRequest request = new BookingCreateRequest();
        request.setVehicleId("veh-1");
        request.setStartDate(LocalDateTime.of(2026, 4, 10, 9, 0));
        request.setEndDate(LocalDateTime.of(2026, 4, 12, 18, 0));

        BookingResponse response = bookingService.createBooking(request);

        assertEquals(LocalDateTime.of(2026, 4, 10, 9, 0), response.getStartDate());
        assertEquals(LocalDateTime.of(2026, 4, 12, 18, 0), response.getEndDate());
        assertEquals("booking-1", response.getId());
    }

    @Test
    void createBookingAcceptsDateOnlyPayloadAndUsesExclusiveEndBoundary() {
        BookingCreateRequest request = new BookingCreateRequest();
        request.setVehicleId("veh-1");
        request.setStartDate(LocalDate.of(2026, 4, 10));
        request.setEndDate(LocalDate.of(2026, 4, 12));
        request.setPickupDateTime("2026-04-10T00:00:00");
        request.setReturnDateTime("2026-04-12T00:00:00");

        bookingService.createBooking(request);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        Booking savedBooking = bookingCaptor.getValue();

        assertEquals(LocalDateTime.of(2026, 4, 10, 0, 0), savedBooking.getStartDate());
        assertEquals(LocalDateTime.of(2026, 4, 13, 0, 0), savedBooking.getEndDate());
    }

    @Test
    void createBookingRejectsInvalidDateFormatClearly() {
        BookingCreateRequest request = new BookingCreateRequest();
        request.setVehicleId("veh-1");
        request.setStartDate("2026/04/10");
        request.setEndDate("2026-04-12");

        RequestValidationException exception = assertThrows(RequestValidationException.class,
                () -> bookingService.createBooking(request));

        assertEquals("Validation failed", exception.getMessage());
        assertEquals("startDate", exception.getErrors().getFirst().field());
    }
}

