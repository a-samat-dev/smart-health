package kz.samat.smarthealth.appointmentservice.exception;

import org.springframework.http.HttpStatus;

/** The doctor or the patient already has an appointment at that time. */
public class TimeConflictException extends ApiException {

    public TimeConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
