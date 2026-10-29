package kz.samat.smarthealth.doctorservice.exception;

import org.springframework.http.HttpStatus;

public class DoctorAlreadyExistsException extends ApiException {

    public DoctorAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "You are already registered as a doctor");
    }
}
