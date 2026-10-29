package kz.samat.smarthealth.appointmentservice.model.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** A patient's appointment with a doctor, in one of the doctor's slots. */
@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The doctor's user id. */
    @Column(nullable = false)
    private UUID doctorId;

    /** Copied from sh-doctor-service when the appointment is booked. */
    @Column(nullable = false)
    private String doctorName;

    /** The user id of the patient who booked the appointment. */
    @Column(nullable = false)
    private UUID clientId;

    /** Copied from sh-user-service when the appointment is booked. */
    @Column(nullable = false)
    private String clientName;

    /** One of the doctor's specializations, which the appointment is for. */
    @Column(nullable = false)
    private Integer specializationId;

    @Column(nullable = false)
    private String specializationName;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private Instant endTime;

    private String notes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    /** Whether the user is this appointment's doctor or patient. */
    public boolean hasParticipant(UUID userId) {
        return doctorId.equals(userId) || clientId.equals(userId);
    }
}
