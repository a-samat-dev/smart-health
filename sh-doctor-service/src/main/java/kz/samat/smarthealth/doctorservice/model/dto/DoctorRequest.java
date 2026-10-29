package kz.samat.smarthealth.doctorservice.model.dto;

import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** The doctor profile, for registering as a doctor and for replacing the profile later. */
@Data
public class DoctorRequest {

    @NotBlank
    @Size(max = 50)
    private String licenseNumber;

    /** Ids from GET /api/v1/specializations; at least one. */
    @NotEmpty
    private Set<@NotNull Integer> specializationIds = new HashSet<>();

    @NotNull
    @Valid
    private AddressDto address;

    @NotBlank
    @Email
    @Size(max = 255)
    private String contactEmail;

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9 ()-]{7,30}$", message = "must be a phone number")
    private String contactPhone;

    @NotNull
    @Valid
    private WorkingHoursDto workingHours;

    /** Days of the week without appointments, such as SATURDAY and SUNDAY. */
    @NotNull
    @Size(max = 6, message = "must leave at least one working day")
    private Set<@NotNull DayOfWeek> daysOff = new HashSet<>();

    @NotBlank
    @Schema(example = "Asia/Almaty")
    private String timeZone;
}
