package kz.samat.smarthealth.appointmentservice.model.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** The query parameters of GET /api/v1/appointments. */
@Data
public class AppointmentFilter {

    @Schema(description = "nextCursor from the previous page; leave out for the first page")
    private String cursor;

    @Min(1)
    @Max(100)
    @Schema(description = "How many appointments a page has", defaultValue = "20")
    private int size = 20;

    @Schema(description = "Only appointments whose specialization contains this text, ignoring case", example = "cardio")
    private String specialization;

    @Schema(description = "Only appointments that start at or after this moment", example = "2026-11-02T00:00:00Z")
    private Instant from;

    @Schema(description = "Only appointments that start before this moment", example = "2026-11-09T00:00:00Z")
    private Instant to;

    @Pattern(regexp = "(createdAt|startTime),(asc|desc)",
            message = "must be createdAt or startTime, then asc or desc, such as startTime,asc")
    @Schema(description = "createdAt (when it was booked) or startTime, then asc or desc", defaultValue = "createdAt,desc")
    private String sort = "createdAt,desc";
}
