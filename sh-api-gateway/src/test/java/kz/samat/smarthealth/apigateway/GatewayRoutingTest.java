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

/** Runs the gateway in front of a stub for each service and checks where requests go. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    private static final UpstreamStub userService = UpstreamStub.start();
    private static final UpstreamStub doctorService = UpstreamStub.start();
    private static final UpstreamStub appointmentService = UpstreamStub.start();

    private final HttpClient client = HttpClient.newHttpClient();
    private final String userId = UUID.randomUUID().toString();

    @Value("${local.server.port}")
    private int port;

    @Value("${jwt.issuer}")
    private String issuer;

    @DynamicPropertySource
    static void upstreams(DynamicPropertyRegistry registry) {
        registry.add("USER_SERVICE_URL", userService::uri);
        registry.add("DOCTOR_SERVICE_URL", doctorService::uri);
        registry.add("APPOINTMENT_SERVICE_URL", appointmentService::uri);
    }

    @AfterAll
    static void stopStubs() {
        userService.stop();
        doctorService.stop();
        appointmentService.stop();
    }

    @BeforeEach
    void clearStubs() {
        userService.clear();
        doctorService.clear();
        appointmentService.clear();
    }

    @Test
    void sendsUserServicePathsToTheUserServiceWithoutThePrefix() throws Exception {
        String path = "/api/v1/users/" + userId;

        assertThat(send(authorized("/user-service" + path).GET()).statusCode()).isEqualTo(200);

        assertThat(userService.requests()).containsExactly(new ReceivedRequest("GET", path, userId));
        assertThat(doctorService.requests()).isEmpty();
    }

    @Test
    void sendsDoctorServicePathsToTheDoctorService() throws Exception {
        assertThat(send(authorized("/doctor-service/api/v1/doctors").POST(BodyPublishers.ofString("{}"))).statusCode()).isEqualTo(200);
        assertThat(send(authorized("/doctor-service/api/v1/specializations").GET()).statusCode()).isEqualTo(200);

        assertThat(doctorService.requests()).containsExactly(
                new ReceivedRequest("POST", "/api/v1/doctors", userId),
                new ReceivedRequest("GET", "/api/v1/specializations", userId));
        assertThat(userService.requests()).isEmpty();
    }

    @Test
    void sendsAppointmentServicePathsToTheAppointmentService() throws Exception {
        String path = "/api/v1/appointments/" + UUID.randomUUID();

        assertThat(send(authorized("/appointment-service" + path).DELETE()).statusCode()).isEqualTo(200);
        assertThat(send(authorized("/appointment-service/api/v1/slots").GET()).statusCode()).isEqualTo(200);

        assertThat(appointmentService.requests()).containsExactly(
                new ReceivedRequest("DELETE", path, userId),
                new ReceivedRequest("GET", "/api/v1/slots", userId));
    }

    @Test
    void requiresAnAccessTokenForTheNewServices() throws Exception {
        assertThat(send(request("/doctor-service/api/v1/doctors").GET()).statusCode()).isEqualTo(401);
        assertThat(send(request("/appointment-service/api/v1/appointments").GET()).statusCode()).isEqualTo(401);
        assertThat(doctorService.requests()).isEmpty();
        assertThat(appointmentService.requests()).isEmpty();
    }

    /** The services' internal endpoints trust their callers, so clients must not reach them. */
    @Test
    void refusesInternalEndpoints() throws Exception {
        HttpResponse<String> response = send(authorized("/user-service/internal/v1/users/" + userId + "/type")
                .PUT(BodyPublishers.ofString("{\"type\": \"DOCTOR\"}")));

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(send(authorized("/doctor-service/internal/anything").GET()).statusCode()).isEqualTo(403);
        assertThat(userService.requests()).isEmpty();
        assertThat(doctorService.requests()).isEmpty();
    }

    @Test
    void doesNotRoutePathsWithoutAServiceName() throws Exception {
        assertThat(send(authorized("/api/v1/doctors").GET()).statusCode()).isEqualTo(404);
        assertThat(doctorService.requests()).isEmpty();
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
    }

    private HttpRequest.Builder authorized(String path) {
        String token = TestTokens.accessToken(issuer, userId, Instant.now().plus(Duration.ofHours(1)));
        return request(path).header("Authorization", "Bearer " + token);
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        return client.send(request.build(), BodyHandlers.ofString());
    }
}
