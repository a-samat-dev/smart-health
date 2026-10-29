package kz.samat.smarthealth.appointmentservice.service;

import java.util.UUID;

import kz.samat.smarthealth.appointmentservice.config.ServicesProperties;
import kz.samat.smarthealth.appointmentservice.exception.ServiceUnavailableException;
import kz.samat.smarthealth.appointmentservice.exception.UserNotFoundException;
import kz.samat.smarthealth.appointmentservice.model.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Calls sh-user-service's internal endpoints directly, not through the gateway, with the same retries and
 * circuit breaker as {@link DoctorClient}.
 */
@Slf4j
@Component
public class UserClient {

    private static final String SERVICE_NAME = "user-service";

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final RetryTemplate retryTemplate;

    public UserClient(RestClient.Builder restClientBuilder, ServicesProperties services,
                      CircuitBreakerFactory<?, ?> circuitBreakerFactory, RetryTemplate retryTemplate) {
        this.restClient = restClientBuilder.baseUrl(services.userService()).build();
        this.circuitBreaker = circuitBreakerFactory.create(SERVICE_NAME);
        this.retryTemplate = retryTemplate;
    }

    public UserDto getUser(UUID id) {
        return circuitBreaker.run(
                () -> retryTemplate.invoke(() -> restClient.get()
                        .uri("/internal/v1/users/{id}", id)
                        .retrieve()
                        .body(UserDto.class)),
                failure -> {
                    if (failure instanceof HttpClientErrorException.NotFound) {
                        throw new UserNotFoundException(id);
                    }
                    log.warn("Call to {} failed: {}", SERVICE_NAME, failure.toString());
                    throw new ServiceUnavailableException("User service");
                });
    }
}
