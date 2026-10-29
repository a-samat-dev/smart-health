package kz.samat.smarthealth.doctorservice;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Stands in for sh-user-service's internal endpoints: knows the users a test adds, records the requests it
 * gets, and can be told to fail.
 */
public final class UserServiceStub {

    public record ReceivedRequest(String method, String path) {
    }

    private static final Pattern USER_PATH = Pattern.compile("/internal/v1/users/([^/]+)(/type)?");

    private final HttpServer server;
    private final Map<UUID, String> users = new ConcurrentHashMap<>();
    private final List<ReceivedRequest> requests = new CopyOnWriteArrayList<>();
    private volatile boolean failing;
    private volatile boolean failingTypeUpdates;

    private UserServiceStub(HttpServer server) {
        this.server = server;
    }

    public static UserServiceStub start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            UserServiceStub stub = new UserServiceStub(server);
            server.createContext("/", stub::handle);
            server.start();
            return stub;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        requests.add(new ReceivedRequest(method, path));

        Matcher matcher = USER_PATH.matcher(path);
        boolean typeUpdate = matcher.matches() && matcher.group(2) != null;
        if (failing || (typeUpdate && failingTypeUpdates)) {
            respond(exchange, 500, "{\"status\": 500}");
            return;
        }
        String user = matcher.matches() ? users.get(UUID.fromString(matcher.group(1))) : null;
        if (user == null) {
            respond(exchange, 404, "{\"status\": 404}");
        } else {
            respond(exchange, 200, user);
        }
    }

    private static void respond(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    /** Adds a client with a new id and returns the id. */
    public UUID addUser(String firstName, String lastName) {
        UUID id = UUID.randomUUID();
        users.put(id, """
                {"id": "%s", "email": "%s@example.com", "firstName": "%s", "lastName": "%s", "type": "CLIENT"}
                """.formatted(id, id, firstName, lastName));
        return id;
    }

    public void setFailing(boolean failing) {
        this.failing = failing;
    }

    public void setFailingTypeUpdates(boolean failingTypeUpdates) {
        this.failingTypeUpdates = failingTypeUpdates;
    }

    public List<ReceivedRequest> requestsFor(UUID userId) {
        return requests.stream().filter(request -> request.path().contains(userId.toString())).toList();
    }

    public String uri() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    public void stop() {
        server.stop(0);
    }
}
