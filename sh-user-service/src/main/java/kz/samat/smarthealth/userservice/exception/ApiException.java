package kz.samat.smarthealth.userservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Base class for errors that are reported to the client with their own HTTP status. */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
