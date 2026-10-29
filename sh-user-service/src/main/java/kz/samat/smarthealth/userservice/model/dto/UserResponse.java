package kz.samat.smarthealth.userservice.model.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import kz.samat.smarthealth.userservice.model.entity.UserType;
import lombok.Data;

@Data
public class UserResponse {

    private UUID id;

    private String email;

    private String firstName;

    private String lastName;

    private String phoneNumber;

    private LocalDate dateOfBirth;

    private UserType type;

    private Instant createdAt;

    private Instant updatedAt;
}
