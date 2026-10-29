package kz.samat.smarthealth.doctorservice.repository;

import java.util.UUID;

import kz.samat.smarthealth.doctorservice.model.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    Page<Doctor> findBySpecializationsId(Integer specializationId, Pageable pageable);

    boolean existsByLicenseNumberAndUserIdNot(String licenseNumber, UUID userId);
}
