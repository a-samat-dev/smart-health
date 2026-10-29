package kz.samat.smarthealth.doctorservice.service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import kz.samat.smarthealth.doctorservice.exception.BadRequestException;
import kz.samat.smarthealth.doctorservice.exception.DoctorAlreadyExistsException;
import kz.samat.smarthealth.doctorservice.exception.DoctorNotFoundException;
import kz.samat.smarthealth.doctorservice.exception.ForbiddenException;
import kz.samat.smarthealth.doctorservice.exception.LicenseNumberTakenException;
import kz.samat.smarthealth.doctorservice.model.dto.DoctorRequest;
import kz.samat.smarthealth.doctorservice.model.dto.DoctorResponse;
import kz.samat.smarthealth.doctorservice.model.dto.PageResponse;
import kz.samat.smarthealth.doctorservice.model.dto.SpecializationResponse;
import kz.samat.smarthealth.doctorservice.model.dto.UserDto;
import kz.samat.smarthealth.doctorservice.model.entity.Doctor;
import kz.samat.smarthealth.doctorservice.model.entity.Specialization;
import kz.samat.smarthealth.doctorservice.repository.DoctorRepository;
import kz.samat.smarthealth.doctorservice.repository.SpecializationRepository;
import kz.samat.smarthealth.doctorservice.util.PdfUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private static final Sort BY_NAME = Sort.by("lastName", "firstName", "userId");

    private final DoctorRepository doctorRepository;
    private final SpecializationRepository specializationRepository;
    private final LicenseStorageService licenseStorageService;
    private final UserClient userClient;
    private final ModelMapper modelMapper;

    /**
     * Registers the user as a doctor. The user's type in sh-user-service changes to DOCTOR last, once the profile
     * is saved: if that call fails, the profile is rolled back and the user stays a client.
     */
    @Transactional
    public DoctorResponse createDoctor(UUID userId, DoctorRequest request, MultipartFile licenseFile) {
        if (doctorRepository.existsById(userId)) {
            throw new DoctorAlreadyExistsException();
        }
        checkLicenseFile(licenseFile);
        Doctor doctor = new Doctor();
        doctor.setUserId(userId);
        applyRequest(request, doctor);

        UserDto user = userClient.getUser(userId);
        doctor.setFirstName(user.firstName());
        doctor.setLastName(user.lastName());
        doctor.setLicenseFileUrl(licenseStorageService.store(userId, licenseFile));
        Doctor saved = doctorRepository.saveAndFlush(doctor);

        userClient.makeDoctor(userId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DoctorResponse getDoctor(UUID userId) {
        return toResponse(findDoctor(userId));
    }

    /** Doctors sorted by name, all of them or those with the specialization. */
    @Transactional(readOnly = true)
    public PageResponse<DoctorResponse> getDoctors(Integer specializationId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, BY_NAME);
        Page<Doctor> doctors = specializationId == null
                ? doctorRepository.findAll(pageRequest)
                : doctorRepository.findBySpecializationsId(specializationId, pageRequest);
        return PageResponse.of(doctors.map(this::toResponse));
    }

    @Transactional
    public DoctorResponse updateDoctor(UUID userId, UUID currentUserId, DoctorRequest request) {
        Doctor doctor = findOwnDoctor(userId, currentUserId);
        applyRequest(request, doctor);
        // Flushing sets updatedAt before the response is built.
        return toResponse(doctorRepository.saveAndFlush(doctor));
    }

    @Transactional
    public void updateLicenseFile(UUID userId, UUID currentUserId, MultipartFile licenseFile) {
        Doctor doctor = findOwnDoctor(userId, currentUserId);
        checkLicenseFile(licenseFile);
        doctor.setLicenseFileUrl(licenseStorageService.store(userId, licenseFile));
        // The link stays the same, so this is what marks the profile as changed.
        doctor.setUpdatedAt(Instant.now());
        doctorRepository.save(doctor);
    }

    @Transactional(readOnly = true)
    public byte[] getLicenseFile(UUID userId, UUID currentUserId) {
        return licenseStorageService.read(findOwnDoctor(userId, currentUserId).getLicenseFileUrl());
    }

    /** Copies the request onto the doctor, after the checks the field annotations cannot do. */
    private void applyRequest(DoctorRequest request, Doctor doctor) {
        if (!ZoneId.getAvailableZoneIds().contains(request.getTimeZone())) {
            throw new BadRequestException("Unknown time zone " + request.getTimeZone());
        }
        if (!request.getWorkingHours().getEnd().isAfter(request.getWorkingHours().getStart())) {
            throw new BadRequestException("Working hours must end after they start");
        }
        if (doctorRepository.existsByLicenseNumberAndUserIdNot(request.getLicenseNumber(), doctor.getUserId())) {
            throw new LicenseNumberTakenException(request.getLicenseNumber());
        }

        modelMapper.map(request, doctor);
        doctor.getDaysOff().clear();
        doctor.getDaysOff().addAll(request.getDaysOff());
        doctor.getSpecializations().clear();
        doctor.getSpecializations().addAll(findSpecializations(request.getSpecializationIds()));
    }

    private List<Specialization> findSpecializations(Set<Integer> ids) {
        List<Specialization> found = specializationRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            List<Integer> unknown = ids.stream()
                    .filter(id -> found.stream().noneMatch(specialization -> specialization.getId().equals(id)))
                    .sorted()
                    .toList();
            throw new BadRequestException("Unknown specialization ids: " + unknown);
        }
        return found;
    }

    private static void checkLicenseFile(MultipartFile licenseFile) {
        if (!PdfUtils.isPdf(licenseFile)) {
            throw new BadRequestException("The license file must be a PDF");
        }
    }

    private Doctor findDoctor(UUID userId) {
        return doctorRepository.findById(userId).orElseThrow(() -> new DoctorNotFoundException(userId));
    }

    /** Doctors can only change, and see the license of, their own profile. */
    private Doctor findOwnDoctor(UUID userId, UUID currentUserId) {
        if (!userId.equals(currentUserId)) {
            throw new ForbiddenException("You can only access your own doctor profile");
        }
        return findDoctor(userId);
    }

    private DoctorResponse toResponse(Doctor doctor) {
        DoctorResponse response = modelMapper.map(doctor, DoctorResponse.class);
        // A profile that was just built has its specializations in no particular order.
        response.getSpecializations().sort(Comparator.comparing(SpecializationResponse::getName));
        return response;
    }
}
