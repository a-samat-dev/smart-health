package kz.samat.smarthealth.appointmentservice.model.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Moves the appointment to another of the doctor's free slots, and replaces the notes. */
@Data
public class UpdateAppointmentRequest {

    @NotNull
    private Instant startTime;

    @Size(max = 1000)
    private String notes;
}
