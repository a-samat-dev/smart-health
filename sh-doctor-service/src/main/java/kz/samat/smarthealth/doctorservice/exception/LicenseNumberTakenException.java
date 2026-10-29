package kz.samat.smarthealth.doctorservice.exception;

import org.springframework.http.HttpStatus;

public class LicenseNumberTakenException extends ApiException {

    public LicenseNumberTakenException(String licenseNumber) {
        super(HttpStatus.CONFLICT, "Another doctor is registered with license number " + licenseNumber);
    }
}
