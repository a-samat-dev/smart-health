package kz.samat.smarthealth.userservice.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class SessionNotFoundException extends ApiException {

    public SessionNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Session " + id + " not found");
    }
}
