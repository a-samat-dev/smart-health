package kz.samat.smarthealth.userservice.controller;

import java.net.URI;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.samat.smarthealth.userservice.config.OpenApiConfig;
import kz.samat.smarthealth.userservice.model.dto.CreateUserRequest;
import kz.samat.smarthealth.userservice.model.dto.UpdateUserRequest;
import kz.samat.smarthealth.userservice.model.dto.UserResponse;
import kz.samat.smarthealth.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Signing up, and user accounts. Except for sign-up, sh-api-gateway checks the access token and passes the
 * caller's id in the X-User-Id header, so this service must only be reachable through the gateway.
 */
@RestController
@RequestMapping(UserController.USERS_PATH)
@RequiredArgsConstructor
@Tag(name = "Users", description = "Signing up, and user accounts")
public class UserController {

    public static final String USERS_PATH = "/api/v1/users";
    public static final String USER_ID_HEADER = "X-User-Id";

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Sign up", description = "Creates an account. Sign in afterwards with POST /api/v1/sessions.")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.created(URI.create(USERS_PATH + "/" + user.getId())).body(user);
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Get a user's profile", description = "Only your own; any other id gets 403.")
    public UserResponse getUser(@PathVariable UUID id,
                                @Parameter(hidden = true) @RequestHeader(USER_ID_HEADER) UUID currentUserId) {
        return userService.getUser(id, currentUserId);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Update a user's profile", description = "Only your own; any other id gets 403.")
    public UserResponse updateUser(@PathVariable UUID id,
                                   @Parameter(hidden = true) @RequestHeader(USER_ID_HEADER) UUID currentUserId,
                                   @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateUser(id, currentUserId, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Delete a user's account", description = "Only your own; any other id gets 403.")
    public void deleteUser(@PathVariable UUID id,
                           @Parameter(hidden = true) @RequestHeader(USER_ID_HEADER) UUID currentUserId) {
        userService.deleteUser(id, currentUserId);
    }
}
