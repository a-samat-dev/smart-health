package kz.samat.smarthealth.doctorservice.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class DoctorNotFoundException extends ApiException {

    public DoctorNotFoundException(UUID userId) {
        super(HttpStatus.NOT_FOUND, "Doctor " + userId + " not found");
    }
}
