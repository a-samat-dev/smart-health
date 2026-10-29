package kz.samat.smarthealth.doctorservice.model.dto;

import java.util.UUID;

/** The parts of a user from sh-user-service that this service uses. */
public record UserDto(UUID id, String firstName, String lastName) {
}
