package kz.samat.smarthealth.userservice.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ApiException {

    public UserNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "User " + id + " not found");
    }
}
