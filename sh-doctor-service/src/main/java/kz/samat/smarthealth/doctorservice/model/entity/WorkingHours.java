package kz.samat.smarthealth.doctorservice.model.entity;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** When the doctor sees patients on a working day, such as 09:00 to 17:00, in the doctor's time zone. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class WorkingHours {

    @Column(name = "work_start", nullable = false)
    private LocalTime start;

    @Column(name = "work_end", nullable = false)
    private LocalTime end;
}
