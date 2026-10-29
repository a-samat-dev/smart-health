package kz.samat.smarthealth.doctorservice.controller;

import java.net.URI;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kz.samat.smarthealth.doctorservice.config.OpenApiConfig;
import kz.samat.smarthealth.doctorservice.model.dto.DoctorRequest;
import kz.samat.smarthealth.doctorservice.model.dto.DoctorResponse;
import kz.samat.smarthealth.doctorservice.model.dto.PageResponse;
import kz.samat.smarthealth.doctorservice.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Doctor profiles, identified by the doctor's user id. sh-api-gateway checks the access token and passes the
 * caller's id in the X-User-Id header, so this service must only be reachable through the gateway.
 */
@RestController
@RequestMapping(DoctorController.DOCTORS_PATH)
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Tag(name = "Doctors", description = "Registering as a doctor, and doctor profiles")
public class DoctorController {

    public static final String DOCTORS_PATH = "/api/v1/doctors";
    public static final String USER_ID_HEADER = "X-User-Id";

    private final DoctorService doctorService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Register as a doctor",
            description = "Multipart: the profile as JSON in the \"doctor\" part and the license PDF, at most 10 MB, "
                    + "in the \"licenseFile\" part. Makes the caller's account a DOCTOR.")
    public ResponseEntity<DoctorResponse> createDoctor(
            @Parameter(hidden = true) @RequestHeader(USER_ID_HEADER) UUID currentUserId,
            @Valid @RequestPart("doctor") DoctorRequest request,
            @RequestPart("licenseFile") MultipartFile licenseFile) {
        DoctorResponse doctor = doctorService.createDoctor(currentUserId, request, licenseFile);
        return ResponseEntity.created(URI.create(DOCTORS_PATH + "/" + doctor.getUserId())).body(doctor);
    }

    @GetMapping
    @Operation(summary = "List doctors", description = "Sorted by name, optionally only those with a specialization.")
    public PageResponse<DoctorResponse> getDoctors(
            @RequestParam(required = false) Integer specializationId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return doctorService.getDoctors(specializationId, page, size);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get a doctor's profile")
    public DoctorResponse getDoctor(@PathVariable UUID userId) {
        return doctorService.getDoctor(userId);
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update your doctor profile",
            description = "Replaces every field of the profile; only your own, any other id gets 403.")
    public DoctorResponse updateDoctor(@PathVariable UUID userId,
                                       @Parameter(hidden = true) @RequestHeader(USER_ID_HEADER) UUID currentUserId,
                                       @Valid @RequestBody DoctorRequest request) {
        return doctorService.updateDoctor(userId, currentUserId, request);
    }

    @GetMapping(value = "/{userId}/license-file", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Download your license file", description = "Only your own; any other id gets 403.")
    public ResponseEntity<byte[]> getLicenseFile(@PathVariable UUID userId,
                                                 @Parameter(hidden = true) @RequestHeader(USER_ID_HEADER) UUID currentUserId) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("license.pdf").build().toString())
                .body(doctorService.getLicenseFile(userId, currentUserId));
    }

    @PutMapping(value = "/{userId}/license-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Replace your license file",
            description = "Multipart with the PDF, at most 10 MB, in the \"licenseFile\" part. Only your own.")
    public void updateLicenseFile(@PathVariable UUID userId,
                                  @Parameter(hidden = true) @RequestHeader(USER_ID_HEADER) UUID currentUserId,
                                  @RequestPart("licenseFile") MultipartFile licenseFile) {
        doctorService.updateLicenseFile(userId, currentUserId, licenseFile);
    }
}
