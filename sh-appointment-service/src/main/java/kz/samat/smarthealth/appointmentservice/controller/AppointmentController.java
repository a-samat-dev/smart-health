package kz.samat.smarthealth.appointmentservice.controller;

import java.net.URI;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.samat.smarthealth.appointmentservice.config.OpenApiConfig;
import kz.samat.smarthealth.appointmentservice.model.dto.AppointmentFilter;
import kz.samat.smarthealth.appointmentservice.model.dto.AppointmentPageResponse;
import kz.samat.smarthealth.appointmentservice.model.dto.AppointmentResponse;
import kz.samat.smarthealth.appointmentservice.model.dto.CreateAppointmentRequest;
import kz.samat.smarthealth.appointmentservice.model.dto.UpdateAppointmentRequest;
import kz.samat.smarthealth.appointmentservice.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The caller's appointments, as patient and as doctor. sh-api-gateway checks the access token and passes the
 * caller's id in the X-User-Id header, so this service must only be reachable through the gateway.
 */
@RestController
@RequestMapping(AppointmentController.APPOINTMENTS_PATH)
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Appointments", description = "Booking, listing, rescheduling and cancelling appointments")
public class AppointmentController {

    public static final String APPOINTMENTS_PATH = "/api/v1/appointments";
    public static final String USER_ID_HEADER = "X-User-Id";

    private final AppointmentService appointmentService;

    @PostMapping
    @Operation(summary = "Book an appointment", description = "Books one of the doctor's free slots, from GET /api/v1/slots.")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Parameter(hidden = true) @RequestHeader(USER_ID_HEADER) UUID currentUserId,
            @Valid @RequestBody CreateAppointmentRequest request) {
        AppointmentResponse appointment = appointmentService.createAppointment(currentUserId, request);
        return ResponseEntity.created(URI.create(APPOINTMENTS_PATH + "/" + appointment.getId())).body(appointment);
    }

    @GetMapping
    @Operation(summary = "List your appointments",
            description = "Appointments you booked and, if you are a doctor, those booked with you. One page at a "
                    + "time: pass the response's nextCursor as cursor to get the next one.")
    public AppointmentPageResponse getAppointments(
            @Parameter(hidden = true) @RequestHeader(USER_ID_HEADER) UUID currentUserId,
            @Valid @ParameterObject AppointmentFilter filter) {
        return appointmentService.getAppointments(currentUserId, filter);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an appointment", description = "Only your own; any other id gets 403.")
    public AppointmentResponse getAppointment(@PathVariable UUID id,
                                              @Parameter(hidden = true) @RequestHeader(USER_ID_HEADER) UUID currentUserId) {
        return appointmentService.getAppointment(id, currentUserId);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Reschedule an appointment",
            description = "Moves it to another free slot of the same doctor and replaces the notes. Only your own.")
    public AppointmentResponse updateAppointment(@PathVariable UUID id,
                                                 @Parameter(hidden = true) @RequestHeader(USER_ID_HEADER) UUID currentUserId,
                                                 @Valid @RequestBody UpdateAppointmentRequest request) {
        return appointmentService.updateAppointment(id, currentUserId, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel an appointment", description = "Frees its slot. Only your own; any other id gets 403.")
    public void deleteAppointment(@PathVariable UUID id,
                                  @Parameter(hidden = true) @RequestHeader(USER_ID_HEADER) UUID currentUserId) {
        appointmentService.deleteAppointment(id, currentUserId);
    }
}
