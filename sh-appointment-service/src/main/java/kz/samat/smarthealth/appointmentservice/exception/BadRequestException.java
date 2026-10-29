package kz.samat.smarthealth.appointmentservice.exception;

import org.springframework.http.HttpStatus;

/** Input that passes the field checks but cannot be used, such as a time the doctor does not work. */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
