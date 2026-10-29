package kz.samat.smarthealth.userservice.exception;

import org.springframework.http.HttpStatus;

/** The caller is signed in but may not act on this resource, such as another user's account. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
