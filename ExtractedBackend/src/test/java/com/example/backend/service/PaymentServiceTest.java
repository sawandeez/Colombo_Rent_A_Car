package com.example.backend.service;

import com.example.backend.dto.PaymentInitiationResponse;
import com.example.backend.model.Booking;
import com.example.backend.model.BookingStatus;
import com.example.backend.model.PaymentGateway;
import com.example.backend.model.PaymentStatus;
import com.example.backend.model.PaymentTransaction;
import com.example.backend.model.User;
import com.example.backend.model.UserRole;
import com.example.backend.repository.BookingRepository;
import com.example.backend.repository.PaymentTransactionRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.VehicleRepository;
import com.example.backend.service.gateway.PaymentGatewayFactory;
import com.example.backend.service.gateway.PaymentInitiationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private PayHereHashService payHereHashService;

    @Mock
    private PaymentGatewayFactory paymentGatewayFactory;

    @Mock
    private com.example.backend.service.gateway.PaymentGateway gatewayBean;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                bookingRepository,
                paymentTransactionRepository,
                userRepository,
                vehicleRepository,
                payHereHashService,
                paymentGatewayFactory,
                new ObjectMapper());

        User user = new User();
        user.setId("u-1");
        user.setRole(UserRole.CUSTOMER);
        user.setEmail("customer@example.com");
        user.setName("John Doe");
        user.setPhone("+94770000000");
        user.setAddress("No 1, Main Street");
        user.setCity("Colombo");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("customer@example.com", "n/a"));

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void initiateCreatesTransactionWithInitiatedStatusForPayHere() {
        Booking booking = buildPayableBooking();

        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));
        when(paymentTransactionRepository.findByOrderId(any())).thenReturn(Optional.empty());
        when(paymentGatewayFactory.resolveGateway()).thenReturn(PaymentGateway.PAYHERE);
        when(paymentGatewayFactory.gatewayBean(PaymentGateway.PAYHERE)).thenReturn(gatewayBean);
        when(gatewayBean.initiate(any(), any(), any())).thenReturn(PaymentInitiationResult.builder()
                .gateway(PaymentGateway.PAYHERE)
                .payhereUrl("https://sandbox.payhere.lk/pay/checkout")
                .fields(Map.of("merchant_id", "123"))
                .build());

        PaymentInitiationResponse response = paymentService.initiatePayHerePayment("b-1");

        ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(txCaptor.capture());

        PaymentTransaction saved = txCaptor.getValue();
        assertEquals("b-1", saved.getBookingId());
        assertEquals(PaymentStatus.INITIATED, saved.getStatus());
        assertEquals(new BigDecimal("15000.00"), saved.getAmount());
        assertNotNull(saved.getOrderId());

        assertEquals("b-1", response.getBookingId());
        assertEquals("PAYHERE", response.getGateway());
        assertEquals("https://sandbox.payhere.lk/pay/checkout", response.getPayhereUrl());
        assertEquals("123", response.getFields().get("merchant_id"));
        assertNotNull(response.getOrderId());
    }

    @Test
    void initiateReturnsMockRedirectWhenGatewayFallsBackToMock() {
        Booking booking = buildPayableBooking();

        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));
        when(paymentTransactionRepository.findByOrderId(any())).thenReturn(Optional.empty());
        when(paymentGatewayFactory.resolveGateway()).thenReturn(PaymentGateway.MOCK);
        when(paymentGatewayFactory.gatewayBean(PaymentGateway.MOCK)).thenReturn(gatewayBean);
        when(gatewayBean.initiate(any(), any(), any())).thenReturn(PaymentInitiationResult.builder()
                .gateway(PaymentGateway.MOCK)
                .redirectUrl("http://localhost:8080/api/v1/payments/mock/checkout?orderId=ORD-b-1-AAAA1111")
                .build());

        PaymentInitiationResponse response = paymentService.initiatePayHerePayment("b-1");

        assertEquals("MOCK", response.getGateway());
        assertNotNull(response.getRedirectUrl());
    }

    @Test
    void notifyUpdatesTransactionAndBookingToConfirmedOnSuccess() {
        Booking booking = new Booking();
        booking.setId("b-1");
        booking.setUserId("u-1");
        booking.setStatus(BookingStatus.PENDING);

        PaymentTransaction tx = new PaymentTransaction();
        tx.setOrderId("ORD-b-1-AAA11111");
        tx.setBookingId("b-1");
        tx.setStatus(PaymentStatus.INITIATED);

        when(payHereHashService.hasCredentialsConfigured()).thenReturn(true);
        when(payHereHashService.isValidNotifyHash(any())).thenReturn(true);
        when(payHereHashService.resolveStatusCode(any())).thenReturn("2");
        when(paymentTransactionRepository.findByOrderId("ORD-b-1-AAA11111")).thenReturn(Optional.of(tx));
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("order_id", "ORD-b-1-AAA11111");
        payload.put("payment_id", "PH-123");
        payload.put("status_code", "2");

        paymentService.handlePayHereNotify(payload);

        ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(txCaptor.capture());
        assertEquals(PaymentStatus.SUCCESS, txCaptor.getValue().getStatus());
        assertEquals("PH-123", txCaptor.getValue().getGatewayPaymentId());

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        assertEquals(BookingStatus.CONFIRMED, bookingCaptor.getValue().getStatus());
        assertEquals(PaymentStatus.SUCCESS, bookingCaptor.getValue().getPaymentStatus());
        assertNotNull(bookingCaptor.getValue().getPaymentDate());
    }

    @Test
    void completeMockPaymentMarksBookingFailedWithoutConfirmation() {
        Booking booking = new Booking();
        booking.setId("b-1");
        booking.setUserId("u-1");
        booking.setStatus(BookingStatus.PENDING);

        PaymentTransaction tx = new PaymentTransaction();
        tx.setOrderId("ORD-b-1-AAA11111");
        tx.setBookingId("b-1");
        tx.setStatus(PaymentStatus.INITIATED);

        when(paymentTransactionRepository.findByOrderId("ORD-b-1-AAA11111")).thenReturn(Optional.of(tx));
        when(bookingRepository.findById("b-1")).thenReturn(Optional.of(booking));

        Map<String, String> result = paymentService.completeMockPayment("ORD-b-1-AAA11111", "FAILED");

        assertEquals("FAILED", result.get("paymentStatus"));

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        assertEquals(BookingStatus.PENDING, bookingCaptor.getValue().getStatus());
        assertEquals(PaymentStatus.FAILED, bookingCaptor.getValue().getPaymentStatus());
    }

    private Booking buildPayableBooking() {
        Booking booking = new Booking();
        booking.setId("b-1");
        booking.setUserId("u-1");
        booking.setVehicleId("veh-1");
        booking.setStatus(BookingStatus.PENDING);
        booking.setAdvanceAmount(new BigDecimal("15000"));
        booking.setStartDate(LocalDateTime.of(2026, 4, 20, 9, 0));
        booking.setEndDate(LocalDateTime.of(2026, 4, 22, 18, 0));
        return booking;
    }
}
