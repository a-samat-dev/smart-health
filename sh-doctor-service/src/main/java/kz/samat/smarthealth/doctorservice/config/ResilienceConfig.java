package kz.samat.smarthealth.doctorservice.config;

import java.time.Duration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/** Retries and circuit breakers for calls to other services. */
@Configuration
public class ResilienceConfig {

    /**
     * Retries connection errors and 5xx responses three times, after 100 ms, 200 ms and 400 ms. Other errors,
     * such as a 404, are not retried.
     */
    @Bean
    public RetryTemplate retryTemplate() {
        return new RetryTemplate(RetryPolicy.builder()
                .maxRetries(3)
                .delay(Duration.ofMillis(100))
                .multiplier(2)
                .maxDelay(Duration.ofSeconds(1))
                .includes(ResourceAccessException.class, HttpServerErrorException.class)
                .build());
    }

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> circuitBreakerDefaults() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                // Opens once half of the last 20 calls failed (counted after 10), and lets
                // a few calls through again after 10 seconds to check whether the service is back.
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(20)
                        .minimumNumberOfCalls(10)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        // A 4xx answer, such as 404 for an unknown user, means the service is working.
                        .ignoreExceptions(HttpClientErrorException.class)
                        .build())
                .build());
    }
}
