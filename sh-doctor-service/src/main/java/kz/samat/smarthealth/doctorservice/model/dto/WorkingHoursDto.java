package kz.samat.smarthealth.doctorservice.model.dto;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** In the doctor's time zone, as HH:mm; the end must be after the start, on the same day. */
@Data
public class WorkingHoursDto {

    @NotNull
    @JsonFormat(pattern = "HH:mm")
    @Schema(type = "string", example = "09:00")
    private LocalTime start;

    @NotNull
    @JsonFormat(pattern = "HH:mm")
    @Schema(type = "string", example = "17:00")
    private LocalTime end;
}
