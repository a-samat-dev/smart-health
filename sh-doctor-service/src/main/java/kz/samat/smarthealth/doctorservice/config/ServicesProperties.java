package kz.samat.smarthealth.doctorservice.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param userService base URL of sh-user-service, called directly rather than through the gateway
 */
@Validated
@ConfigurationProperties(prefix = "services")
public record ServicesProperties(@NotBlank String userService) {
}
