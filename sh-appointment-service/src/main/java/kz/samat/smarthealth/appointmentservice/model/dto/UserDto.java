package kz.samat.smarthealth.appointmentservice.model.dto;

import java.util.UUID;

/** The parts of a user from sh-user-service that this service uses. */
public record UserDto(UUID id, String firstName, String lastName) {

    public String fullName() {
        return firstName + " " + lastName;
    }
}
