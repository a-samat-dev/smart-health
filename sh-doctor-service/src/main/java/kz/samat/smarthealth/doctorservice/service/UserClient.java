package kz.samat.smarthealth.doctorservice.service;

import java.util.UUID;
import java.util.function.Supplier;

import kz.samat.smarthealth.doctorservice.config.ServicesProperties;
import kz.samat.smarthealth.doctorservice.exception.ServiceUnavailableException;
import kz.samat.smarthealth.doctorservice.exception.UserNotFoundException;
import kz.samat.smarthealth.doctorservice.model.dto.UserDto;
import kz.samat.smarthealth.doctorservice.model.dto.UserTypeRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Calls sh-user-service directly, not through the gateway. Connection errors and 5xx responses are retried
 * with backoff, and a circuit breaker stops calling while the service keeps failing. Either way the caller
 * gets a 503.
 */
@Slf4j
@Component
public class UserClient {

    private static final String SERVICE_NAME = "user-service";
    private static final String USERS_PATH = "/internal/v1/users/{id}";

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
        return call(id, () -> restClient.get().uri(USERS_PATH, id).retrieve().body(UserDto.class));
    }

    /** Safe to retry: setting the type a second time changes nothing. */
    public void makeDoctor(UUID id) {
        call(id, () -> restClient.put()
                .uri(USERS_PATH + "/type", id)
                .body(new UserTypeRequest("DOCTOR"))
                .retrieve()
                .toBodilessEntity());
    }

    private <T> T call(UUID userId, Supplier<T> request) {
        return circuitBreaker.run(() -> retryTemplate.invoke(request), failure -> {
            if (failure instanceof HttpClientErrorException.NotFound) {
                throw new UserNotFoundException(userId);
            }
            log.warn("Call to {} failed: {}", SERVICE_NAME, failure.toString());
            throw new ServiceUnavailableException("User service");
        });
    }
}
