package kz.samat.smarthealth.appointmentservice.service;

import java.util.UUID;

import kz.samat.smarthealth.appointmentservice.config.ServicesProperties;
import kz.samat.smarthealth.appointmentservice.exception.DoctorNotFoundException;
import kz.samat.smarthealth.appointmentservice.exception.ServiceUnavailableException;
import kz.samat.smarthealth.appointmentservice.model.dto.DoctorDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Calls sh-doctor-service directly, not through the gateway. Connection errors and 5xx responses are retried
 * with backoff, and a circuit breaker stops calling while the service keeps failing. Either way the caller
 * gets a 503.
 */
@Slf4j
@Component
public class DoctorClient {

    private static final String SERVICE_NAME = "doctor-service";

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final RetryTemplate retryTemplate;

    public DoctorClient(RestClient.Builder restClientBuilder, ServicesProperties services,
                        CircuitBreakerFactory<?, ?> circuitBreakerFactory, RetryTemplate retryTemplate) {
        this.restClient = restClientBuilder.baseUrl(services.doctorService()).build();
        this.circuitBreaker = circuitBreakerFactory.create(SERVICE_NAME);
        this.retryTemplate = retryTemplate;
    }

    /** The doctor's working hours, days off and specializations. */
    public DoctorDto getDoctor(UUID userId) {
        return circuitBreaker.run(
                () -> retryTemplate.invoke(() -> restClient.get()
                        .uri("/api/v1/doctors/{userId}", userId)
                        .retrieve()
                        .body(DoctorDto.class)),
                failure -> {
                    if (failure instanceof HttpClientErrorException.NotFound) {
                        throw new DoctorNotFoundException(userId);
                    }
                    log.warn("Call to {} failed: {}", SERVICE_NAME, failure.toString());
                    throw new ServiceUnavailableException("Doctor service");
                });
    }
}
