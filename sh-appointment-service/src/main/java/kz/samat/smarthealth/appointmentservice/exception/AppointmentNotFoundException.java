package kz.samat.smarthealth.appointmentservice.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class AppointmentNotFoundException extends ApiException {

    public AppointmentNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Appointment " + id + " not found");
    }
}
