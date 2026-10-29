package kz.samat.smarthealth.apigateway.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param jwkSetUri where sh-user-service publishes the public keys that verify its access tokens
 * @param issuer    the only accepted value of the "iss" claim
 */
@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(@NotBlank String jwkSetUri, @NotBlank String issuer) {
}
