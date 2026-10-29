package kz.samat.smarthealth.appointmentservice.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Base URLs of the services this one calls directly, rather than through the gateway.
 *
 * @param userService   sh-user-service, for the names of patients
 * @param doctorService sh-doctor-service, for doctors' working hours and specializations
 */
@Validated
@ConfigurationProperties(prefix = "services")
public record ServicesProperties(@NotBlank String userService, @NotBlank String doctorService) {
}
