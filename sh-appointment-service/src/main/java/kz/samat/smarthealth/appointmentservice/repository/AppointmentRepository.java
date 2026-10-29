package kz.samat.smarthealth.appointmentservice.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import kz.samat.smarthealth.appointmentservice.model.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID>, JpaSpecificationExecutor<Appointment> {

    /** Appointments that overlap the period and have any of the users as their doctor or patient. */
    @Query("""
            select a from Appointment a
            where (a.doctorId in :userIds or a.clientId in :userIds)
              and a.startTime < :end and a.endTime > :start
            order by a.startTime""")
    List<Appointment> findOverlapping(Collection<UUID> userIds, Instant start, Instant end);
}
