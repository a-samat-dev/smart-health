package kz.samat.smarthealth.appointmentservice.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.samat.smarthealth.appointmentservice.config.OpenApiConfig;
import kz.samat.smarthealth.appointmentservice.model.dto.SlotResponse;
import kz.samat.smarthealth.appointmentservice.service.SlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(SlotController.SLOTS_PATH)
@RequiredArgsConstructor
@Tag(name = "Slots", description = "Doctors' schedules: when appointments can be booked")
public class SlotController {

    public static final String SLOTS_PATH = "/api/v1/slots";

    private final SlotService slotService;

    @GetMapping
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "List a doctor's slots",
            description = "The doctor's upcoming slots between from and to, at most 31 days apart, and whether each is free.")
    public List<SlotResponse> getSlots(
            @Parameter(description = "The doctor's user id") @RequestParam UUID doctorId,
            @Parameter(example = "2026-11-02T00:00:00Z") @RequestParam Instant from,
            @Parameter(example = "2026-11-09T00:00:00Z") @RequestParam Instant to) {
        return slotService.getSlots(doctorId, from, to);
    }
}
