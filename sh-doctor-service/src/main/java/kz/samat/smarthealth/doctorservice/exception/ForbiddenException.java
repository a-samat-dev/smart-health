package kz.samat.smarthealth.doctorservice.exception;

import org.springframework.http.HttpStatus;

/** The caller is signed in but may not act on this resource, such as another doctor's profile. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
