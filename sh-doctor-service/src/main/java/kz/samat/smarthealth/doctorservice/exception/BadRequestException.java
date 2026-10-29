package kz.samat.smarthealth.doctorservice.exception;

import org.springframework.http.HttpStatus;

/** Input that passes the field checks but cannot be used, such as an unknown time zone. */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
