package kz.samat.smarthealth.doctorservice.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Where the doctor sees patients. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class Address {

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String houseNumber;
}
