package kz.samat.smarthealth.doctorservice.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.samat.smarthealth.doctorservice.config.OpenApiConfig;
import kz.samat.smarthealth.doctorservice.model.dto.SpecializationResponse;
import kz.samat.smarthealth.doctorservice.service.SpecializationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(SpecializationController.SPECIALIZATIONS_PATH)
@RequiredArgsConstructor
@Tag(name = "Specializations", description = "The specializations doctors choose from")
public class SpecializationController {

    public static final String SPECIALIZATIONS_PATH = "/api/v1/specializations";

    private final SpecializationService specializationService;

    @GetMapping
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "List the specializations", description = "Sorted by name.")
    public List<SpecializationResponse> getSpecializations() {
        return specializationService.getSpecializations();
    }
}
