package kz.samat.smarthealth.doctorservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressDto {

    @NotBlank
    @Size(max = 100)
    private String country;

    @NotBlank
    @Size(max = 100)
    private String city;

    @NotBlank
    @Size(max = 200)
    private String street;

    @NotBlank
    @Size(max = 20)
    private String houseNumber;
}
