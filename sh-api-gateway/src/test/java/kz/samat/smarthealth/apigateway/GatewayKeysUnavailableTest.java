package kz.samat.smarthealth.apigateway;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** The gateway has not fetched sh-user-service's public keys yet, and the service is down. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class GatewayKeysUnavailableTest {

    /** A protected path: any user's account, as sh-user-service sees it. */
    private static final String USER_PATH = "/api/v1/users/7d1f9c0e-8a4b-4f0e-9a51-3f2a6c1d2b10";

    /** The gateway reaches sh-user-service under this prefix and removes it before passing a request on. */
    private static final String PREFIX = "/user-service";

    private final HttpClient client = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Value("${jwt.issuer}")
    private String issuer;

    @DynamicPropertySource
    static void upstreams(DynamicPropertyRegistry registry) {
        registry.add("USER_SERVICE_URL", () -> "http://localhost:" + unusedPort());
    }

    /** 503 rather than 401, so the client keeps its tokens instead of treating them as rejected. */
    @Test
    void answersWithServiceUnavailableWhenTheTokenCannotBeChecked() throws Exception {
        String token = TestTokens.accessToken(issuer, UUID.randomUUID().toString(), Instant.now().plus(Duration.ofHours(1)));

        HttpResponse<String> response = send(token);

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.body()).contains("\"message\":\"Access tokens cannot be checked right now, please try again later\"");
    }

    @Test
    void stillRejectsRequestsWithoutAToken() throws Exception {
        assertThat(send(null).statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> send(String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + PREFIX + USER_PATH));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
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
