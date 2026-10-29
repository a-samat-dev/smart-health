package kz.samat.smarthealth.apigateway;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Stands in for sh-user-service: publishes the keys of {@link TestTokens#SIGNING_KEY}, answers 200 to every
 * other request, and records those requests.
 */
public final class UpstreamStub {

    public record ReceivedRequest(String method, String path, String userId) {
    }

    public static final String JWKS_PATH = "/.well-known/jwks.json";

    private final HttpServer server;
    private final List<ReceivedRequest> requests = new CopyOnWriteArrayList<>();

    private UpstreamStub(HttpServer server) {
        this.server = server;
    }

    public static UpstreamStub start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            UpstreamStub stub = new UpstreamStub(server);
            server.createContext(JWKS_PATH, exchange -> respond(exchange, TestTokens.publishedKeys()));
            server.createContext("/", exchange -> {
                stub.requests.add(new ReceivedRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                        exchange.getRequestHeaders().getFirst("X-User-Id")));
                respond(exchange, "{}");
            });
            server.start();
            return stub;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void respond(HttpExchange exchange, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    public String uri() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    /** Requests received, apart from fetches of the published keys. */
    public List<ReceivedRequest> requests() {
        return requests;
    }

    public void clear() {
        requests.clear();
    }

    public void stop() {
        server.stop(0);
    }
}
