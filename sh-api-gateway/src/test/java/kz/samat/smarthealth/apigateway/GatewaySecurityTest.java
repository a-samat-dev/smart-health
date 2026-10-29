package kz.samat.smarthealth.apigateway;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.nimbusds.jose.jwk.RSAKey;
import kz.samat.smarthealth.apigateway.UpstreamStub.ReceivedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs the gateway in front of a stub user service and checks which requests get through. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class GatewaySecurityTest {

    /** A protected path: any user's account, as sh-user-service sees it. */
    private static final String USER_PATH = "/api/v1/users/7d1f9c0e-8a4b-4f0e-9a51-3f2a6c1d2b10";

    /** The gateway reaches sh-user-service under this prefix and removes it before passing a request on. */
    private static final String PREFIX = "/user-service";

    private static final String WEB_APP_ORIGIN = "http://localhost:5173";

    /** Has the same key id as the real signing key, so only the signature check can reject its tokens. */
    private static final RSAKey OTHER_KEY = TestTokens.generateKey(TestTokens.SIGNING_KEY.getKeyID());

    private static final UpstreamStub userService = UpstreamStub.start();

    private final HttpClient client = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Value("${jwt.issuer}")
    private String issuer;

    @DynamicPropertySource
    static void upstreams(DynamicPropertyRegistry registry) {
        registry.add("USER_SERVICE_URL", userService::uri);
    }

    @AfterAll
    static void stopStub() {
        userService.stop();
    }

    @BeforeEach
    void clearStub() {
        userService.clear();
    }

    @Test
    void rejectsRequestsWithoutAnAccessToken() throws Exception {
        HttpResponse<String> response = send(get(USER_PATH));

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("WWW-Authenticate")).contains("Bearer");
        assertThat(response.body())
                .contains("\"status\":401")
                .contains("\"message\":\"Access token is missing, invalid or expired\"")
                .contains("\"path\":\"" + PREFIX + USER_PATH + "\"");
        assertThat(userService.requests()).isEmpty();
    }

    @Test
    void rejectsAnExpiredAccessToken() throws Exception {
        String token = TestTokens.accessToken(issuer, UUID.randomUUID().toString(), Instant.now().minus(Duration.ofMinutes(5)));

        HttpResponse<String> response = send(get(USER_PATH).header("Authorization", "Bearer " + token));

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("WWW-Authenticate")).contains("Bearer error=\"invalid_token\"");
        assertThat(userService.requests()).isEmpty();
    }

    /** Only POST is public on users and sessions; everything else there needs a token. */
    @Test
    void requiresAnAccessTokenForOtherMethodsOnPublicPaths() throws Exception {
        assertThat(send(get("/api/v1/users")).statusCode()).isEqualTo(401);
        assertThat(send(request("/api/v1/sessions/" + UUID.randomUUID()).DELETE()).statusCode()).isEqualTo(401);
        assertThat(userService.requests()).isEmpty();
    }

    @Test
    void rejectsAnAccessTokenSignedWithAnotherKey() throws Exception {
        String token = TestTokens.accessToken(OTHER_KEY, issuer, UUID.randomUUID().toString(), inOneHour());

        assertThat(send(get(USER_PATH).header("Authorization", "Bearer " + token)).statusCode()).isEqualTo(401);
    }

    @Test
    void rejectsAnAccessTokenFromAnotherIssuer() throws Exception {
        String token = TestTokens.accessToken("someone-else", UUID.randomUUID().toString(), inOneHour());

        assertThat(send(get(USER_PATH).header("Authorization", "Bearer " + token)).statusCode()).isEqualTo(401);
    }

    @Test
    void passesTheUserIdFromTheAccessTokenInsteadOfTheOneTheClientSent() throws Exception {
        String userId = UUID.randomUUID().toString();
        String token = TestTokens.accessToken(issuer, userId, inOneHour());

        HttpResponse<String> response = send(get(USER_PATH)
                .header("Authorization", "Bearer " + token)
                .header("X-User-Id", UUID.randomUUID().toString()));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(userService.requests()).containsExactly(new ReceivedRequest("GET", USER_PATH, userId));
    }

    @Test
    void letsSignUpAndSignInThroughWithoutAnAccessToken() throws Exception {
        assertThat(send(post("/api/v1/users")).statusCode()).isEqualTo(200);
        assertThat(send(post("/api/v1/sessions")).statusCode()).isEqualTo(200);

        assertThat(userService.requests()).containsExactly(
                new ReceivedRequest("POST", "/api/v1/users", null),
                new ReceivedRequest("POST", "/api/v1/sessions", null));
    }

    @Test
    void letsTokenRefreshesThroughWithoutCheckingTheAccessToken() throws Exception {
        String expired = TestTokens.accessToken(issuer, UUID.randomUUID().toString(), Instant.now().minus(Duration.ofDays(1)));
        String path = "/api/v1/sessions/" + UUID.randomUUID() + "/tokens";

        HttpResponse<String> response = send(post(path)
                .header("Authorization", "Bearer " + expired)
                .header("X-User-Id", UUID.randomUUID().toString()));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(userService.requests()).containsExactly(new ReceivedRequest("POST", path, null));
    }

    @Test
    void passesSignOutsThroughWithTheUserId() throws Exception {
        String userId = UUID.randomUUID().toString();
        String path = "/api/v1/sessions/" + UUID.randomUUID();

        HttpResponse<String> response = send(request(path)
                .header("Authorization", "Bearer " + TestTokens.accessToken(issuer, userId, inOneHour()))
                .DELETE());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(userService.requests()).containsExactly(new ReceivedRequest("DELETE", path, userId));
    }

    @Test
    void addsCorsHeadersToRejectedRequests() throws Exception {
        HttpResponse<String> response = send(get(USER_PATH).header("Origin", WEB_APP_ORIGIN));

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).contains(WEB_APP_ORIGIN);
    }

    @Test
    void answersCorsPreflightRequestsFromTheWebApp() throws Exception {
        HttpResponse<String> response = send(request(USER_PATH)
                .method("OPTIONS", BodyPublishers.noBody())
                .header("Origin", WEB_APP_ORIGIN)
                .header("Access-Control-Request-Method", "PUT")
                .header("Access-Control-Request-Headers", "authorization,content-type"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).contains(WEB_APP_ORIGIN);
        assertThat(userService.requests()).isEmpty();
    }

    @Test
    void refusesCorsRequestsFromOtherOrigins() throws Exception {
        HttpResponse<String> response = send(post("/api/v1/sessions").header("Origin", "https://evil.example"));

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(userService.requests()).isEmpty();
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + PREFIX + path));
    }

    private HttpRequest.Builder get(String path) {
        return request(path).GET();
    }

    private HttpRequest.Builder post(String path) {
        return request(path).header("Content-Type", "application/json").POST(BodyPublishers.ofString("{}"));
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        return client.send(request.build(), BodyHandlers.ofString());
    }

    private static Instant inOneHour() {
        return Instant.now().plus(Duration.ofHours(1));
    }
}
