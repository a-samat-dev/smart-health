package kz.samat.smarthealth.userservice.controller;

import java.net.URI;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.samat.smarthealth.userservice.config.OpenApiConfig;
import kz.samat.smarthealth.userservice.model.dto.CreateSessionRequest;
import kz.samat.smarthealth.userservice.model.dto.RefreshTokenRequest;
import kz.samat.smarthealth.userservice.model.dto.SessionResponse;
import kz.samat.smarthealth.userservice.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static kz.samat.smarthealth.userservice.controller.UserController.USER_ID_HEADER;

@RestController
@RequestMapping(SessionController.SESSIONS_PATH)
@RequiredArgsConstructor
@Tag(name = "Sessions", description = "Signing in and out, and refreshing tokens")
public class SessionController {

    public static final String SESSIONS_PATH = "/api/v1/sessions";

    private final SessionService sessionService;

    @PostMapping
    @Operation(summary = "Sign in", description = "Creates a session with an access token and a refresh token.")
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        SessionResponse session = sessionService.createSession(request);
        return ResponseEntity.created(URI.create(SESSIONS_PATH + "/" + session.getId())).body(session);
    }

    @PostMapping("/{id}/tokens")
    @Operation(summary = "Refresh the session's tokens",
            description = "Needs no access token. The refresh token sent here stops working; use the one in the response next time.")
    public SessionResponse refreshTokens(@PathVariable UUID id, @Valid @RequestBody RefreshTokenRequest request) {
        return sessionService.refreshTokens(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Sign out", description = "Ends one of your sessions; another user's session gets 403.")
    public void deleteSession(@PathVariable UUID id,
                              @Parameter(hidden = true) @RequestHeader(USER_ID_HEADER) UUID userId) {
        sessionService.deleteSession(id, userId);
    }
}
