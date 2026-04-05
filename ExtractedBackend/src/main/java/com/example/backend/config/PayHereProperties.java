package com.example.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payhere")
public class PayHereProperties {

    private String environment = "sandbox";
    private final GatewayConfig sandbox = new GatewayConfig();
    private final GatewayConfig live = new GatewayConfig();

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public GatewayConfig getSandbox() {
        return sandbox;
    }

    public GatewayConfig getLive() {
        return live;
    }

    public GatewayConfig resolveActiveConfig() {
        if ("live".equalsIgnoreCase(environment)) {
            return live;
        }
        return sandbox;
    }

    public static class GatewayConfig {
        private String merchantId;
        private String merchantSecret;
        private String checkoutUrl;

        public String getMerchantId() {
            return merchantId;
        }

        public void setMerchantId(String merchantId) {
            this.merchantId = merchantId;
        }

        public String getMerchantSecret() {
            return merchantSecret;
        }

        public void setMerchantSecret(String merchantSecret) {
            this.merchantSecret = merchantSecret;
        }

        public String getCheckoutUrl() {
            return checkoutUrl;
        }

        public void setCheckoutUrl(String checkoutUrl) {
            this.checkoutUrl = checkoutUrl;
        }
    }
}

