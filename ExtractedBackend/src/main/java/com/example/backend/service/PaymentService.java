package com.example.backend.service;

import com.example.backend.dto.PaymentInitiationResponse;
import com.example.backend.model.Booking;
import com.example.backend.model.BookingStatus;
import com.example.backend.model.PaymentGateway;
import com.example.backend.model.PaymentStatus;
import com.example.backend.model.PaymentTransaction;
import com.example.backend.model.User;
import com.example.backend.model.UserRole;
import com.example.backend.model.Vehicle;
import com.example.backend.repository.BookingRepository;
import com.example.backend.repository.PaymentTransactionRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.VehicleRepository;
import com.example.backend.service.gateway.PaymentGatewayFactory;
import com.example.backend.service.gateway.PaymentInitiationResult;
import com.example.backend.service.gateway.ReturnUrls;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final PayHereHashService payHereHashService;
    private final PaymentGatewayFactory paymentGatewayFactory;
    private final ObjectMapper objectMapper;

    @Value("${app.payment.currency:LKR}")
    private String defaultCurrency;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${app.backend.base-url:http://localhost:8080}")
    private String backendBaseUrl;

    @Transactional
    public PaymentInitiationResponse initiatePayHerePayment(String bookingId) {
        User user = getAuthenticatedUser();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Booking not found: " + bookingId));

        enforceOwnerOrAdmin(user, booking.getUserId());
        validateBookingPayable(booking);

        BigDecimal amount = resolveAmount(booking);
        String currency = normalizeCurrency(defaultCurrency);
        String orderId = generateOrderId(bookingId);

        PaymentGateway selectedGateway = paymentGatewayFactory.resolveGateway();

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setBookingId(bookingId);
        transaction.setGateway(selectedGateway);
        transaction.setOrderId(orderId);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setStatus(PaymentStatus.INITIATED);
        transaction.setCreatedAt(Instant.now());
        transaction.setUpdatedAt(Instant.now());
        transaction.setInitiatedBy(user);
        paymentTransactionRepository.save(transaction);

        booking.setPaymentStatus(PaymentStatus.INITIATED);
        bookingRepository.save(booking);

        ReturnUrls returnUrls = ReturnUrls.builder()
                .returnUrl(frontendBaseUrl + "/payment/success?bookingId=" + bookingId)
                .cancelUrl(frontendBaseUrl + "/payment/fail?bookingId=" + bookingId)
                .notifyUrl(backendBaseUrl + "/api/v1/payments/payhere/notify")
                .mockCheckoutUrl(backendBaseUrl + "/api/v1/payments/mock/checkout")
                .build();

        PaymentInitiationResult initiation = paymentGatewayFactory
                .gatewayBean(selectedGateway)
                .initiate(booking, transaction, returnUrls);

        log.info("PAYMENT_INITIATE - gateway={}, orderId={}, bookingId={}, amount={}, currency={}",
                initiation.getGateway(),
                orderId,
                bookingId,
                amount,
                currency);

        return PaymentInitiationResponse.builder()
                .bookingId(bookingId)
                .gateway(initiation.getGateway().name())
                .orderId(orderId)
                .payhereUrl(initiation.getPayhereUrl())
                .redirectUrl(initiation.getRedirectUrl())
                .fields(initiation.getFields())
                .build();
    }

    @Transactional
    public void handlePayHereNotify(Map<String, String> payload) {
        if (payHereHashService.hasCredentialsConfigured() && !payHereHashService.isValidNotifyHash(payload)) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid PayHere signature");
        }

        if (!payHereHashService.hasCredentialsConfigured()) {
            log.warn("PAYHERE_NOTIFY - Signature verification skipped because merchant credentials are not configured for environment {}",
                    payHereHashService.getEnvironment());
        }

        String orderId = normalize(payload.get("order_id"));
        if (orderId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "order_id is required");
        }

        String statusCode = payHereHashService.resolveStatusCode(payload);
        if (statusCode == null) {
            throw new ResponseStatusException(BAD_REQUEST, "status_code is required");
        }

        boolean success = "2".equals(statusCode);
        String gatewayPaymentId = firstNonBlank(payload.get("payment_id"), payload.get("payhere_reference"));
        completePayment(orderId, success, gatewayPaymentId, toJson(payload), "PAYHERE_NOTIFY", statusCode);
    }

    @Transactional
    public Map<String, String> completeMockPayment(String orderId, String status) {
        String normalizedOrderId = normalize(orderId);
        if (normalizedOrderId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "orderId is required");
        }

        String normalizedStatus = normalize(status);
        if (normalizedStatus == null) {
            throw new ResponseStatusException(BAD_REQUEST, "status is required");
        }

        String statusUpper = normalizedStatus.toUpperCase(Locale.ROOT);
        if (!"SUCCESS".equals(statusUpper) && !"FAILED".equals(statusUpper)) {
            throw new ResponseStatusException(BAD_REQUEST, "status must be SUCCESS or FAILED");
        }

        boolean success = "SUCCESS".equals(statusUpper);
        Booking booking = completePayment(
                normalizedOrderId,
                success,
                "MOCK-" + normalizedOrderId,
                toJson(Map.of("orderId", normalizedOrderId, "status", statusUpper)),
                "MOCK_COMPLETE",
                statusUpper);

        String redirectUrl = success
                ? frontendBaseUrl + "/payment/success?bookingId=" + booking.getId()
                : frontendBaseUrl + "/payment/fail?bookingId=" + booking.getId();

        return Map.of(
                "status", "ok",
                "orderId", normalizedOrderId,
                "gateway", PaymentGateway.MOCK.name(),
                "paymentStatus", success ? PaymentStatus.SUCCESS.name() : PaymentStatus.FAILED.name(),
                "redirectUrl", redirectUrl
        );
    }

    public Map<String, String> getMockCheckoutContext(String orderId) {
        String normalizedOrderId = normalize(orderId);
        if (normalizedOrderId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "orderId is required");
        }

        PaymentTransaction transaction = paymentTransactionRepository.findByOrderId(normalizedOrderId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Payment transaction not found for orderId: " + normalizedOrderId));

        return Map.of(
                "orderId", normalizedOrderId,
                "bookingId", transaction.getBookingId(),
                "gateway", PaymentGateway.MOCK.name(),
                "completeUrl", "/api/v1/payments/mock/complete"
        );
    }

    private Booking completePayment(
            String orderId,
            boolean success,
            String gatewayPaymentId,
            String rawPayload,
            String source,
            String externalStatus) {
        PaymentTransaction transaction = paymentTransactionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Payment transaction not found for orderId: " + orderId));

        if (transaction.getStatus() != PaymentStatus.SUCCESS) {
            transaction.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        }

        String resolvedGatewayPaymentId = normalize(gatewayPaymentId);
        if (resolvedGatewayPaymentId != null) {
            transaction.setGatewayPaymentId(resolvedGatewayPaymentId);
        }

        transaction.setRawNotifyPayload(rawPayload);
        transaction.setUpdatedAt(Instant.now());
        paymentTransactionRepository.save(transaction);

        Booking booking = bookingRepository.findById(transaction.getBookingId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Booking not found for payment order: " + orderId));

        if (success) {
            booking.setPaymentStatus(PaymentStatus.SUCCESS);
            booking.setPaymentDate(Instant.now());
            booking.setAdvancePaid(true);
            booking.setStatus(BookingStatus.CONFIRMED);
        } else if (booking.getPaymentStatus() != PaymentStatus.SUCCESS) {
            booking.setPaymentStatus(PaymentStatus.FAILED);
        }

        bookingRepository.save(booking);

        log.info("PAYMENT_COMPLETE - source={}, orderId={}, externalStatus={}, resolvedStatus={}",
                source,
                orderId,
                externalStatus,
                success ? "SUCCESS" : "FAILED");

        return booking;
    }

    private void validateBookingPayable(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.REJECTED
                || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ResponseStatusException(BAD_REQUEST, "Booking is not payable in its current state");
        }
        if (booking.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new ResponseStatusException(BAD_REQUEST, "Booking payment has already been completed");
        }
    }

    private BigDecimal resolveAmount(Booking booking) {
        if (booking.getAdvanceAmount() != null && booking.getAdvanceAmount().compareTo(BigDecimal.ZERO) > 0) {
            return booking.getAdvanceAmount().setScale(2, RoundingMode.HALF_UP);
        }

        Vehicle vehicle = vehicleRepository.findById(booking.getVehicleId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Vehicle not found: " + booking.getVehicleId()));

        BigDecimal ratePerDay = vehicle.getRentalPricePerDay() != null
                ? vehicle.getRentalPricePerDay()
                : vehicle.getRentalPrice();
        if (ratePerDay == null || ratePerDay.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Cannot determine payment amount for booking");
        }

        long durationMinutes = Math.max(0L, Duration.between(booking.getStartDate(), booking.getEndDate()).toMinutes());
        long chargeableDays = Math.max(1L, (long) Math.ceil(durationMinutes / (24d * 60d)));
        BigDecimal amount = ratePerDay.multiply(BigDecimal.valueOf(chargeableDays)).setScale(2, RoundingMode.HALF_UP);
        booking.setAdvanceAmount(amount);
        return amount;
    }

    private String generateOrderId(String bookingId) {
        String candidate = "ORD-" + bookingId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        while (paymentTransactionRepository.findByOrderId(candidate).isPresent()) {
            candidate = "ORD-" + bookingId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        }
        return candidate;
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(UNAUTHORIZED, "Authentication is required");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Authenticated user not found"));
    }

    private void enforceOwnerOrAdmin(User currentUser, String ownerId) {
        boolean isOwner = currentUser.getId() != null && currentUser.getId().equals(ownerId);
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(FORBIDDEN, "Access denied for this booking");
        }
    }

    private String firstNonBlank(String first, String second) {
        String f = normalize(first);
        if (f != null) {
            return f;
        }
        return normalize(second);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeCurrency(String value) {
        String currency = normalize(value);
        return currency == null ? "LKR" : currency.toUpperCase(Locale.ROOT);
    }

    private String toJson(Map<String, String> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return payload.toString();
        }
    }
}
