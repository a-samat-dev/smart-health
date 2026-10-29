package kz.samat.smarthealth.userservice.model.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Replaces every editable profile field: a field left out is cleared. The email cannot be changed here.
 */
@Data
public class UpdateUserRequest {

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @Pattern(regexp = "^\\+?[0-9 ()-]{7,30}$", message = "must be a phone number")
    private String phoneNumber;

    @Past
    private LocalDate dateOfBirth;
}
