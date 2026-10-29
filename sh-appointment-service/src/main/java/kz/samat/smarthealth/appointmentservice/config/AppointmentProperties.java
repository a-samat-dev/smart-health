package kz.samat.smarthealth.appointmentservice.config;

import java.time.Duration;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param slotDuration how long an appointment lasts; a doctor's working hours are cut into slots this long
 */
@Validated
@ConfigurationProperties(prefix = "appointments")
public record AppointmentProperties(@NotNull Duration slotDuration) {
}
