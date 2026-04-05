package com.example.backend.service;

import com.example.backend.dto.BookingResponse;
import com.example.backend.model.Booking;
import com.example.backend.model.BookingStatus;
import com.example.backend.repository.BookingRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetAdvanceAmountServiceTest {

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
    }

    @Test
    void setAdvanceAmountOnApprovedBookingPersistsAmountAndCurrency() {
        Booking booking = new Booking();
        booking.setId("b-1");
        booking.setUserId("u-1");
        booking.setStatus(BookingStatus.APPROVED);

        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        BookingResponse response = bookingService.setAdvanceAmount("b-1", new BigDecimal("5000"), "LKR");

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());

        assertEquals(new BigDecimal("5000"), captor.getValue().getAdvanceAmount());
        assertEquals("LKR", captor.getValue().getAdvanceCurrency());
        assertEquals(new BigDecimal("5000"), response.getAdvanceAmount());
        assertEquals("LKR", response.getAdvanceCurrency());
    }

    @Test
    void setAdvanceAmountDefaultsCurrencyToLKRWhenNullProvided() {
        Booking booking = new Booking();
        booking.setId("b-2");
        booking.setUserId("u-1");
        booking.setStatus(BookingStatus.APPROVED);

        when(bookingRepository.findById("b-2")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        bookingService.setAdvanceAmount("b-2", new BigDecimal("8000"), null);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertEquals("LKR", captor.getValue().getAdvanceCurrency());
    }

    @Test
    void setAdvanceAmountOnPendingBookingThrows400() {
        Booking booking = new Booking();
        booking.setId("b-3");
        booking.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findById("b-3")).thenReturn(Optional.of(booking));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.setAdvanceAmount("b-3", new BigDecimal("5000"), "LKR"));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void setAdvanceAmountOnRejectedBookingThrows400() {
        Booking booking = new Booking();
        booking.setId("b-4");
        booking.setStatus(BookingStatus.REJECTED);

        when(bookingRepository.findById("b-4")).thenReturn(Optional.of(booking));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.setAdvanceAmount("b-4", new BigDecimal("5000"), "LKR"));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void setAdvanceAmountOnNonExistentBookingThrows404() {
        when(bookingRepository.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookingService.setAdvanceAmount("missing", new BigDecimal("5000"), "LKR"));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void getBookingResponseIncludesAdvanceAmountAfterSet() {
        Booking booking = new Booking();
        booking.setId("b-5");
        booking.setUserId("u-1");
        booking.setStatus(BookingStatus.APPROVED);
        booking.setAdvanceAmount(new BigDecimal("12000"));
        booking.setAdvanceCurrency("LKR");

        when(bookingRepository.findById("b-5")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        BookingResponse response = bookingService.setAdvanceAmount("b-5", new BigDecimal("12000"), "LKR");

        assertEquals(new BigDecimal("12000"), response.getAdvanceAmount());
        assertEquals("LKR", response.getAdvanceCurrency());
    }
}

