package kz.samat.smarthealth.appointmentservice.model.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAppointmentRequest {

    /** The doctor's user id. */
    @NotNull
    private UUID doctorId;

    /** One of the doctor's specializations. */
    @NotNull
    private Integer specializationId;

    /** The start of one of the doctor's free slots, from GET /api/v1/slots. */
    @NotNull
    private Instant startTime;

    /** What the patient wants the doctor to know, such as their symptoms. */
    @Size(max = 1000)
    private String notes;
}
