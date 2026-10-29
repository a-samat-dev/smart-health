package kz.samat.smarthealth.userservice.model.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {

    /** Stays the same when the tokens are refreshed; needed to refresh them and to sign out. */
    private UUID id;

    /** The signed-in user, whose profile is at /api/v1/users/{userId}. */
    private UUID userId;

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    /** Lifetime of the access token in seconds. */
    private long expiresIn;
}
