package com.schoolsaas.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.stripe")
public record StripeProperties(
        String secretKey,
        String webhookSecret,
        String checkoutSuccessUrl,
        String checkoutCancelUrl) {
}
