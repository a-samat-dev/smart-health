package kz.samat.smarthealth.apigateway.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/** Writes an {@link ApiError} for responses produced outside controllers, such as security rejections. */
@Component
@RequiredArgsConstructor
public class ApiErrorWriter {

    private final JsonMapper jsonMapper;

    public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String message) {
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(), message,
                exchange.getRequest().getPath().value());

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(Mono.fromSupplier(
                () -> response.bufferFactory().wrap(jsonMapper.writeValueAsBytes(body))));
    }
}
