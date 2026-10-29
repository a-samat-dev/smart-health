package kz.samat.smarthealth.doctorservice.model.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.SortedSet;
import java.util.UUID;

import lombok.Data;

@Data
public class DoctorResponse {

    /** The doctor's user id, which is also the id of the doctor profile. */
    private UUID userId;

    private String firstName;

    private String lastName;

    private String licenseNumber;

    /** Sorted by name. */
    private List<SpecializationResponse> specializations;

    private AddressDto address;

    private String contactEmail;

    private String contactPhone;

    private WorkingHoursDto workingHours;

    /** From Monday to Sunday. */
    private SortedSet<DayOfWeek> daysOff;

    private String timeZone;

    private Instant createdAt;

    private Instant updatedAt;
}
