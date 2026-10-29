package kz.samat.smarthealth.userservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSessionRequest {

    @NotBlank
    private String email;

    @NotBlank
    private String password;
}
