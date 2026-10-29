package kz.samat.smarthealth.appointmentservice.model.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class AppointmentResponse {

    private UUID id;

    private UUID doctorId;

    private String doctorName;

    private UUID clientId;

    private String clientName;

    private Integer specializationId;

    private String specializationName;

    private Instant startTime;

    private Instant endTime;

    private String notes;

    private Instant createdAt;

    private Instant updatedAt;
}
