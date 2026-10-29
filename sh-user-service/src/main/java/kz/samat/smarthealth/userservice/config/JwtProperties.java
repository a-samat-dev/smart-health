package kz.samat.smarthealth.userservice.config;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param privateKeyLocation PEM file (PKCS#8) with the RSA private key that signs access tokens, such as
 *                           {@code file:/run/secrets/jwt-private.pem}; when empty, a temporary key is generated
 * @param issuer             value of the "iss" claim; the gateway rejects tokens with any other issuer
 * @param accessTokenTtl     how long an access token is valid
 * @param refreshTokenTtl    how long a refresh token is valid
 */
@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String privateKeyLocation,
        @NotBlank String issuer,
        @NotNull Duration accessTokenTtl,
        @NotNull Duration refreshTokenTtl) {
}
