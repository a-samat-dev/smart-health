package kz.samat.smarthealth.appointmentservice.exception;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/** Body of every error response. */
@Getter
@Builder
public class ApiError {

    @Builder.Default
    private final Instant timestamp = Instant.now();

    private final int status;

    private final String error;

    private final String message;

    private final String path;

    /** One entry per rejected field, as "field: reason". */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<String> details;
}
