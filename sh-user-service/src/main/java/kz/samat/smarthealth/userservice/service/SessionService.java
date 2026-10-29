package kz.samat.smarthealth.userservice.service;

import java.time.Instant;
import java.util.UUID;

import kz.samat.smarthealth.userservice.config.JwtProperties;
import kz.samat.smarthealth.userservice.exception.ForbiddenException;
import kz.samat.smarthealth.userservice.exception.InvalidCredentialsException;
import kz.samat.smarthealth.userservice.exception.InvalidRefreshTokenException;
import kz.samat.smarthealth.userservice.exception.SessionNotFoundException;
import kz.samat.smarthealth.userservice.model.dto.CreateSessionRequest;
import kz.samat.smarthealth.userservice.model.dto.RefreshTokenRequest;
import kz.samat.smarthealth.userservice.model.dto.SessionResponse;
import kz.samat.smarthealth.userservice.model.entity.Session;
import kz.samat.smarthealth.userservice.model.entity.User;
import kz.samat.smarthealth.userservice.repository.SessionRepository;
import kz.samat.smarthealth.userservice.repository.UserRepository;
import kz.samat.smarthealth.userservice.util.EmailUtils;
import kz.samat.smarthealth.userservice.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Signing in creates a session with an access token and a refresh token. Refresh tokens are random strings
 * rather than JWTs, so they can be revoked, and each works once: refreshing replaces it with a new one.
 */
@Service
@RequiredArgsConstructor
public class SessionService {

    private static final String TOKEN_TYPE = "Bearer";

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    /** Signs the user in. */
    @Transactional
    public SessionResponse createSession(CreateSessionRequest request) {
        User user = userRepository.findByEmail(EmailUtils.normalize(request.getEmail()))
                .filter(found -> passwordEncoder.matches(request.getPassword(), found.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        sessionRepository.deleteExpiredByUserId(user.getId(), Instant.now());

        Session session = new Session();
        session.setUser(user);
        return issueTokens(session);
    }

    /** Replaces the session's tokens. The refresh token sent here stops working. */
    @Transactional
    public SessionResponse refreshTokens(UUID sessionId, RefreshTokenRequest request) {
        Session session = sessionRepository.findLockedById(sessionId)
                .filter(found -> found.getRefreshTokenHash().equals(TokenUtils.sha256(request.getRefreshToken())))
                .filter(found -> found.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(InvalidRefreshTokenException::new);
        return issueTokens(session);
    }

    /** Signs out of the session, which must belong to the user. */
    @Transactional
    public void deleteSession(UUID sessionId, UUID userId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        if (!session.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only end your own sessions");
        }
        sessionRepository.delete(session);
    }

    private SessionResponse issueTokens(Session session) {
        String refreshToken = TokenUtils.generateToken();
        session.setRefreshTokenHash(TokenUtils.sha256(refreshToken));
        session.setExpiresAt(Instant.now().plus(jwtProperties.refreshTokenTtl()));
        sessionRepository.save(session);

        return SessionResponse.builder()
                .id(session.getId())
                .userId(session.getUser().getId())
                .accessToken(jwtService.generateAccessToken(session.getUser()))
                .refreshToken(refreshToken)
                .tokenType(TOKEN_TYPE)
                .expiresIn(jwtProperties.accessTokenTtl().toSeconds())
                .build();
    }
}
