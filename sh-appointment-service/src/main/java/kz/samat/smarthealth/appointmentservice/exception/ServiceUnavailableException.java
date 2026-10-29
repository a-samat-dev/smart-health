package kz.samat.smarthealth.appointmentservice.exception;

import org.springframework.http.HttpStatus;

/** Something this service depends on, such as sh-doctor-service, cannot be reached right now. */
public class ServiceUnavailableException extends ApiException {

    public ServiceUnavailableException(String dependency) {
        super(HttpStatus.SERVICE_UNAVAILABLE, dependency + " is unavailable, please try again later");
    }
}
