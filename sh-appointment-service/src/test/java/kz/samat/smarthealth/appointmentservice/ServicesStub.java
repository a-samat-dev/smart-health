package kz.samat.smarthealth.appointmentservice;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Stands in for sh-doctor-service and sh-user-service: answers with the doctors and users a test adds, and can
 * be told to fail.
 */
public final class ServicesStub {

    private static final String DOCTORS_PATH = "/api/v1/doctors/";
    private static final String USERS_PATH = "/internal/v1/users/";

    private final HttpServer server;
    private final Map<String, String> resources = new ConcurrentHashMap<>();
    private volatile boolean failing;

    private ServicesStub(HttpServer server) {
        this.server = server;
    }

    public static ServicesStub start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            ServicesStub stub = new ServicesStub(server);
            server.createContext("/", stub::handle);
            server.start();
            return stub;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body = resources.get(exchange.getRequestURI().getPath());
        if (failing) {
            respond(exchange, 500, "{\"status\": 500}");
        } else if (body == null) {
            respond(exchange, 404, "{\"status\": 404}");
        } else {
            respond(exchange, 200, body);
        }
    }

    private static void respond(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    /** Adds a user with a new id and returns the id. */
    public UUID addUser(String firstName, String lastName) {
        UUID id = UUID.randomUUID();
        resources.put(USERS_PATH + id, """
                {"id": "%s", "email": "%s@example.com", "firstName": "%s", "lastName": "%s", "type": "CLIENT"}
                """.formatted(id, id, firstName, lastName));
        return id;
    }

    /**
     * Adds a user who is a doctor with one specialization, working 09:00 to 12:00 in Almaty from Monday to
     * Friday, and returns their id.
     */
    public UUID addDoctor(String firstName, String lastName, int specializationId, String specializationName) {
        UUID id = addUser(firstName, lastName);
        resources.put(DOCTORS_PATH + id, """
                {
                  "userId": "%s", "firstName": "%s", "lastName": "%s", "licenseNumber": "KZ-%s",
                  "specializations": [{"id": %d, "name": "%s"}],
                  "workingHours": {"start": "09:00", "end": "12:00"},
                  "daysOff": ["SATURDAY", "SUNDAY"],
                  "timeZone": "Asia/Almaty"
                }
                """.formatted(id, firstName, lastName, id, specializationId, specializationName));
        return id;
    }

    public void setFailing(boolean failing) {
        this.failing = failing;
    }

    public String uri() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    public void stop() {
        server.stop(0);
    }
}
