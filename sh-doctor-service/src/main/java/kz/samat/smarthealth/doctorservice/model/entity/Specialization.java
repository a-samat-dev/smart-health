package kz.samat.smarthealth.doctorservice.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** One of the specializations a doctor can choose from, such as Cardiologist. They are added by migrations. */
@Entity
@Immutable
@Table(name = "specializations")
@Getter
@NoArgsConstructor
public class Specialization {

    @Id
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;
}
