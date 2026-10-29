package kz.samat.smarthealth.userservice.controller;

import java.util.Map;

import io.swagger.v3.oas.annotations.Hidden;
import kz.samat.smarthealth.userservice.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Publishes the public key that access tokens are signed with. sh-api-gateway calls this directly, not
 * through its own routes, which is why it is left out of the API docs.
 */
@Hidden
@RestController
@RequiredArgsConstructor
public class JwksController {

    public static final String JWKS_PATH = "/.well-known/jwks.json";

    private final JwtService jwtService;

    @GetMapping(JWKS_PATH)
    public Map<String, Object> publicKeys() {
        return jwtService.publicKeys();
    }
}
