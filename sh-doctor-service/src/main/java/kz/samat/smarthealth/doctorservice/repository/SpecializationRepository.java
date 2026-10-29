package kz.samat.smarthealth.doctorservice.repository;

import kz.samat.smarthealth.doctorservice.model.entity.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecializationRepository extends JpaRepository<Specialization, Integer> {
}
