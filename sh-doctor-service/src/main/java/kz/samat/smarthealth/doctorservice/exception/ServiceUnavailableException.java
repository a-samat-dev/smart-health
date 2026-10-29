package kz.samat.smarthealth.doctorservice.exception;

import org.springframework.http.HttpStatus;

/** Something this service depends on, such as sh-user-service or MinIO, cannot be reached right now. */
public class ServiceUnavailableException extends ApiException {

    public ServiceUnavailableException(String dependency) {
        super(HttpStatus.SERVICE_UNAVAILABLE, dependency + " is unavailable, please try again later");
    }
}
