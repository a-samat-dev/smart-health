package kz.samat.smarthealth.apigateway.config;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param allowedOrigins browser origins allowed to call the API, such as the React app's
 */
@Validated
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(@NotEmpty List<String> allowedOrigins) {
}
