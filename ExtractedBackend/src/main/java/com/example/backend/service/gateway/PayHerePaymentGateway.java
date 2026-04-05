package com.example.backend.service.gateway;

import com.example.backend.model.Booking;
import com.example.backend.model.PaymentTransaction;
import com.example.backend.model.User;
import com.example.backend.service.PayHereHashService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PayHerePaymentGateway implements PaymentGateway {

    private final PayHereHashService payHereHashService;

    @Override
    public PaymentInitiationResult initiate(Booking booking, PaymentTransaction transaction, ReturnUrls returnUrls) {
        User user = transaction.getInitiatedBy();
        String hash = payHereHashService.generateCheckoutHash(transaction.getOrderId(), transaction.getAmount(), transaction.getCurrency());

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("merchant_id", payHereHashService.getMerchantId());
        fields.put("return_url", returnUrls.getReturnUrl());
        fields.put("cancel_url", returnUrls.getCancelUrl());
        fields.put("notify_url", returnUrls.getNotifyUrl());
        fields.put("order_id", transaction.getOrderId());
        fields.put("items", "Advance payment for booking " + booking.getId());
        fields.put("amount", payHereHashService.formatAmount(transaction.getAmount()));
        fields.put("currency", transaction.getCurrency());
        fields.put("first_name", firstName(user != null ? user.getName() : null));
        fields.put("last_name", lastName(user != null ? user.getName() : null));
        fields.put("email", normalize(user != null ? user.getEmail() : null));
        fields.put("phone", normalize(user != null ? user.getPhone() : null));
        fields.put("address", normalize(user != null ? user.getAddress() : null));
        fields.put("city", normalize(user != null ? user.getCity() : null));
        fields.put("country", "Sri Lanka");
        fields.put("hash", hash);

        return PaymentInitiationResult.builder()
                .gateway(com.example.backend.model.PaymentGateway.PAYHERE)
                .payhereUrl(payHereHashService.getCheckoutUrl())
                .fields(fields)
                .build();
    }

    private String firstName(String fullName) {
        String normalized = normalize(fullName);
        if (normalized == null) {
            return "Customer";
        }
        int idx = normalized.indexOf(' ');
        return idx > 0 ? normalized.substring(0, idx) : normalized;
    }

    private String lastName(String fullName) {
        String normalized = normalize(fullName);
        if (normalized == null) {
            return "User";
        }
        int idx = normalized.indexOf(' ');
        return idx > 0 ? normalized.substring(idx + 1) : "User";
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

