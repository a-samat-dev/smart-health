package kz.samat.smarthealth.appointmentservice.model.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** The parts of a doctor profile from sh-doctor-service that this service uses. */
public record DoctorDto(UUID userId, String firstName, String lastName, List<Specialization> specializations,
                        WorkingHours workingHours, Set<DayOfWeek> daysOff, String timeZone) {

    public record Specialization(Integer id, String name) {
    }

    /** In the doctor's time zone. */
    public record WorkingHours(LocalTime start, LocalTime end) {
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    public Optional<Specialization> specialization(Integer id) {
        return specializations.stream().filter(specialization -> specialization.id().equals(id)).findFirst();
    }
}
