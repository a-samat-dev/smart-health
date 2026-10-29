package kz.samat.smarthealth.userservice.controller;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import io.jsonwebtoken.Claims;
import kz.samat.smarthealth.userservice.TestcontainersConfig;
import kz.samat.smarthealth.userservice.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static kz.samat.smarthealth.userservice.TestUsers.PASSWORD;
import static kz.samat.smarthealth.userservice.TestUsers.claims;
import static kz.samat.smarthealth.userservice.TestUsers.createUser;
import static kz.samat.smarthealth.userservice.TestUsers.credentialsJson;
import static kz.samat.smarthealth.userservice.TestUsers.field;
import static kz.samat.smarthealth.userservice.TestUsers.signedInUser;
import static kz.samat.smarthealth.userservice.TestUsers.uniqueEmail;
import static kz.samat.smarthealth.userservice.controller.UserController.USER_ID_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class SessionControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private KeyPair jwtKeyPair;

    @Autowired
    private SessionRepository sessionRepository;

    @Test
    void signInCreatesASession() throws Exception {
        String email = uniqueEmail();
        String userId = field(createUser(mvc, email), "id");

        String session = signIn(email, PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn().getResponse().getContentAsString();

        mvc.perform(delete("/api/v1/sessions/{id}", field(session, "id")).header(USER_ID_HEADER, userIdOf(session)))
                .andExpect(status().isNoContent());
    }

    @Test
    void signInPointsToTheNewSession() throws Exception {
        String email = uniqueEmail();
        createUser(mvc, email);

        ResultActions result = signIn(email, PASSWORD);
        String id = field(result.andReturn().getResponse().getContentAsString(), "id");

        result.andExpect(header().string("Location", "/api/v1/sessions/" + id));
    }

    @Test
    void accessTokenExpiresAfterOneHour() throws Exception {
        Claims claims = claims(field(signedInUser(mvc), "accessToken"), jwtKeyPair.getPublic());

        assertThat(claims.getIssuer()).isEqualTo("sh-user-service");
        assertThat(Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant()))
                .isEqualTo(Duration.ofHours(1));
    }

    @Test
    void refreshTokenExpiresAfterSevenDays() throws Exception {
        UUID id = UUID.fromString(field(signedInUser(mvc), "id"));

        assertThat(sessionRepository.findById(id).orElseThrow().getExpiresAt())
                .isCloseTo(Instant.now().plus(Duration.ofDays(7)), within(Duration.ofMinutes(1)));
    }

    @Test
    void signInAcceptsTheEmailInAnyCase() throws Exception {
        String email = uniqueEmail();
        createUser(mvc, email);

        signIn(email.toUpperCase(), PASSWORD).andExpect(status().isCreated());
    }

    @Test
    void signInRejectsAWrongPassword() throws Exception {
        String email = uniqueEmail();
        createUser(mvc, email);

        signIn(email, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void refreshTokenCanBeUsedOnlyOnce() throws Exception {
        String session = signedInUser(mvc);
        String id = field(session, "id");
        String refreshToken = field(session, "refreshToken");

        String refreshed = refresh(id, refreshToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        assertThat(field(refreshed, "refreshToken")).isNotEqualTo(refreshToken);

        refresh(id, refreshToken)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token is invalid or expired"));
        refresh(id, field(refreshed, "refreshToken")).andExpect(status().isOk());
    }

    @Test
    void refreshRejectsATokenOfAnotherSession() throws Exception {
        String session = signedInUser(mvc);
        String otherSession = signedInUser(mvc);

        refresh(field(session, "id"), field(otherSession, "refreshToken")).andExpect(status().isUnauthorized());
    }

    @Test
    void signOutEndsTheSession() throws Exception {
        String session = signedInUser(mvc);
        String id = field(session, "id");

        mvc.perform(delete("/api/v1/sessions/{id}", id).header(USER_ID_HEADER, userIdOf(session)))
                .andExpect(status().isNoContent());

        refresh(id, field(session, "refreshToken")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/sessions/{id}", id).header(USER_ID_HEADER, userIdOf(session)))
                .andExpect(status().isNotFound());
    }

    @Test
    void signOutCannotEndAnotherUsersSession() throws Exception {
        String session = signedInUser(mvc);
        String otherSession = signedInUser(mvc);

        mvc.perform(delete("/api/v1/sessions/{id}", field(session, "id")).header(USER_ID_HEADER, userIdOf(otherSession)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only end your own sessions"));
        refresh(field(session, "id"), field(session, "refreshToken")).andExpect(status().isOk());
    }

    private ResultActions signIn(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/sessions").contentType(APPLICATION_JSON).content(credentialsJson(email, password)));
    }

    private ResultActions refresh(String sessionId, String refreshToken) throws Exception {
        return mvc.perform(post("/api/v1/sessions/{id}/tokens", sessionId).contentType(APPLICATION_JSON).content("""
                {"refreshToken": "%s"}
                """.formatted(refreshToken)));
    }

    private String userIdOf(String session) {
        return claims(field(session, "accessToken"), jwtKeyPair.getPublic()).getSubject();
    }
}
