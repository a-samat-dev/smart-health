package kz.samat.smarthealth.apigateway.exception;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

/** Body of every error response the gateway writes itself; sh-user-service uses the same shape. */
@Getter
@Builder
public class ApiError {

    @Builder.Default
    private final Instant timestamp = Instant.now();

    private final int status;

    private final String error;

    private final String message;

    private final String path;

    public static ApiError of(int status, String error, String message, String path) {
        return ApiError.builder().status(status).error(error).message(message).path(path).build();
    }
}
