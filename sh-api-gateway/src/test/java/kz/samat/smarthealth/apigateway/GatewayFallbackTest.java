package kz.samat.smarthealth.apigateway;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Points the gateway at ports nobody listens on, as if the services were down. The public keys still come
 * from a stub, like keys the gateway fetched before the service went down.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class GatewayFallbackTest {

    /** A protected path: any user's account. */
    private static final String USER_PATH = "/user-service/api/v1/users/7d1f9c0e-8a4b-4f0e-9a51-3f2a6c1d2b10";

    private static final UpstreamStub keyServer = UpstreamStub.start();

    private final HttpClient client = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Value("${jwt.issuer}")
    private String issuer;

    @DynamicPropertySource
    static void upstreams(DynamicPropertyRegistry registry) {
        registry.add("USER_SERVICE_URL", () -> "http://localhost:" + unusedPort());
        registry.add("DOCTOR_SERVICE_URL", () -> "http://localhost:" + unusedPort());
        registry.add("APPOINTMENT_SERVICE_URL", () -> "http://localhost:" + unusedPort());
        registry.add("jwt.jwk-set-uri", () -> keyServer.uri() + UpstreamStub.JWKS_PATH);
    }

    @AfterAll
    static void stopStub() {
        keyServer.stop();
    }

    @Test
    void answersWithServiceUnavailableAfterRetryingAGet() throws Exception {
        String token = TestTokens.accessToken(issuer, UUID.randomUUID().toString(), Instant.now().plus(Duration.ofHours(1)));

        HttpResponse<String> response = send(request(USER_PATH).header("Authorization", "Bearer " + token).GET());

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.body()).contains("\"message\":\"User service is unavailable, please try again later\"");
    }

    @Test
    void answersWithServiceUnavailableToASignIn() throws Exception {
        HttpResponse<String> response = send(request("/user-service/api/v1/sessions")
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString("{}")));

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.body()).contains("\"status\":503");
    }

    @Test
    void namesTheServiceThatIsUnavailable() throws Exception {
        String token = TestTokens.accessToken(issuer, UUID.randomUUID().toString(), Instant.now().plus(Duration.ofHours(1)));

        assertThat(send(request("/doctor-service/api/v1/doctors").header("Authorization", "Bearer " + token).GET()).body())
                .contains("\"message\":\"Doctor service is unavailable, please try again later\"");
        assertThat(send(request("/appointment-service/api/v1/appointments").header("Authorization", "Bearer " + token).GET()).body())
                .contains("\"message\":\"Appointment service is unavailable, please try again later\"");
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        return client.send(request.build(), BodyHandlers.ofString());
    }

    private static int unusedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
