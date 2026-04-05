package com.example.backend.service;

import com.example.backend.config.PayHereProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;

@Service
public class PayHereHashService {

    private final PayHereProperties payHereProperties;

    public PayHereHashService(PayHereProperties payHereProperties) {
        this.payHereProperties = payHereProperties;
    }

    public String getMerchantId() {
        return normalize(resolveActiveConfig().getMerchantId());
    }

    public String getCheckoutUrl() {
        return normalize(resolveActiveConfig().getCheckoutUrl());
    }

    public String getEnvironment() {
        String environment = payHereProperties.getEnvironment();
        return environment == null || environment.isBlank() ? "sandbox" : environment.trim().toLowerCase(Locale.ROOT);
    }

    public boolean hasCredentialsConfigured() {
        PayHereProperties.GatewayConfig activeConfig = resolveActiveConfig();
        return normalize(activeConfig.getMerchantId()) != null && normalize(activeConfig.getMerchantSecret()) != null;
    }

    public String generateCheckoutHash(String orderId, BigDecimal amount, String currency) {
        PayHereProperties.GatewayConfig activeConfig = resolveActiveConfig();
        String merchantId = normalize(activeConfig.getMerchantId());
        String merchantSecret = normalize(activeConfig.getMerchantSecret());

        String formattedAmount = formatAmount(amount);
        String localSecret = md5Upper(merchantSecret == null ? "" : merchantSecret);
        return md5Upper((merchantId == null ? "" : merchantId) + orderId + formattedAmount + currency + localSecret);
    }

    public boolean isValidNotifyHash(Map<String, String> payload) {
        PayHereProperties.GatewayConfig activeConfig = resolveActiveConfig();
        String merchantId = normalize(activeConfig.getMerchantId());
        String merchantSecret = normalize(activeConfig.getMerchantSecret());

        String md5Sig = normalize(payload.get("md5sig"));
        if (md5Sig == null) {
            return false;
        }

        String orderId = normalize(payload.get("order_id"));
        String payhereAmount = normalize(payload.get("payhere_amount"));
        String payhereCurrency = normalize(payload.get("payhere_currency"));
        String statusCode = resolveStatusCode(payload);

        if (orderId == null || payhereAmount == null || payhereCurrency == null || statusCode == null) {
            return false;
        }

        String localSecret = md5Upper(merchantSecret == null ? "" : merchantSecret);
        String localSig = md5Upper((merchantId == null ? "" : merchantId)
                + orderId
                + payhereAmount
                + payhereCurrency
                + statusCode
                + localSecret);

        return localSig.equalsIgnoreCase(md5Sig);
    }

    public String resolveStatusCode(Map<String, String> payload) {
        String statusCode = normalize(payload.get("status_code"));
        return statusCode != null ? statusCode : normalize(payload.get("payment_status"));
    }

    public String formatAmount(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        return safeAmount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String md5Upper(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString().toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("MD5 algorithm is not available", ex);
        }
    }

    private PayHereProperties.GatewayConfig resolveActiveConfig() {
        return payHereProperties.resolveActiveConfig();
    }
}
