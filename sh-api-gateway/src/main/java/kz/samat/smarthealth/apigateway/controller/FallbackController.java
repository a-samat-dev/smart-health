package kz.samat.smarthealth.apigateway.controller;

import java.util.Map;

import io.swagger.v3.oas.annotations.Hidden;
import kz.samat.smarthealth.apigateway.exception.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

/** Answers in place of a service whose circuit breaker is open or whose call failed. */
@Hidden
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /** Names used in the messages, by the service name the routes forward with. */
    private static final Map<String, String> SERVICE_NAMES = Map.of(
            "user-service", "User service",
            "doctor-service", "Doctor service",
            "appointment-service", "Appointment service");

    @RequestMapping("/{service}")
    public ResponseEntity<ApiError> serviceUnavailable(@PathVariable String service, ServerWebExchange exchange) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(),
                SERVICE_NAMES.getOrDefault(service, "The service") + " is unavailable, please try again later",
                exchange.getRequest().getPath().value());
        return ResponseEntity.status(status).body(body);
    }
}
