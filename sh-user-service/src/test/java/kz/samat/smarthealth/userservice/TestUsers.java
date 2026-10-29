package kz.samat.smarthealth.userservice;

import java.security.PublicKey;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Signs users up and in through the API for tests. */
public final class TestUsers {

    public static final String PASSWORD = "correct-horse-battery";

    private TestUsers() {
    }

    public static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    public static String newUserJson(String email, String password) {
        return """
                {"email": "%s", "password": "%s", "firstName": "Aigerim", "lastName": "Nurlanova"}
                """.formatted(email, password);
    }

    public static String credentialsJson(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }

    /** Signs up a new user and returns the created user. */
    public static String createUser(MockMvc mvc, String email) throws Exception {
        return mvc.perform(post("/api/v1/users").contentType(APPLICATION_JSON).content(newUserJson(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    /** Signs the user in and returns the session with its tokens. */
    public static String createSession(MockMvc mvc, String email) throws Exception {
        return mvc.perform(post("/api/v1/sessions").contentType(APPLICATION_JSON).content(credentialsJson(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    /** Signs up a new user, signs them in, and returns the session. */
    public static String signedInUser(MockMvc mvc) throws Exception {
        String email = uniqueEmail();
        createUser(mvc, email);
        return createSession(mvc, email);
    }

    public static String field(String json, String name) {
        return JsonPath.read(json, "$." + name);
    }

    /** Verifies the access token with the public key and returns its claims. */
    public static Claims claims(String accessToken, PublicKey publicKey) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(accessToken)
                .getPayload();
    }
}
