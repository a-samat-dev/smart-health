package kz.samat.smarthealth.appointmentservice.model.dto;

import java.time.Instant;

/**
 * One of a doctor's appointment slots.
 *
 * @param available false when the doctor already has an appointment at that time
 */
public record SlotResponse(Instant startTime, Instant endTime, boolean available) {
}
