package kz.samat.smarthealth.userservice.model.dto;

import jakarta.validation.constraints.NotNull;
import kz.samat.smarthealth.userservice.model.entity.UserType;
import lombok.Data;

@Data
public class UpdateUserTypeRequest {

    @NotNull
    private UserType type;
}
