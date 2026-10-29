package kz.samat.smarthealth.userservice.controller;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import kz.samat.smarthealth.userservice.model.dto.UpdateUserTypeRequest;
import kz.samat.smarthealth.userservice.model.dto.UserResponse;
import kz.samat.smarthealth.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * For the other services, such as sh-doctor-service and sh-appointment-service, which call it directly.
 * sh-api-gateway refuses these paths, so clients cannot reach them; that is also why they are left out
 * of the API docs.
 */
@Hidden
@RestController
@RequestMapping(InternalUserController.INTERNAL_USERS_PATH)
@RequiredArgsConstructor
public class InternalUserController {

    public static final String INTERNAL_USERS_PATH = "/internal/v1/users";

    private final UserService userService;

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable UUID id) {
        return userService.getAnyUser(id);
    }

    @PutMapping("/{id}/type")
    public UserResponse updateType(@PathVariable UUID id, @Valid @RequestBody UpdateUserTypeRequest request) {
        return userService.updateType(id, request.getType());
    }
}
