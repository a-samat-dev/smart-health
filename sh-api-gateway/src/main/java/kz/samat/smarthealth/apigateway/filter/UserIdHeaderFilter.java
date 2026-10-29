package kz.samat.smarthealth.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Passes the id of the authenticated user to downstream services in the X-User-Id header. Any X-User-Id
 * the client sent is dropped first, so services can trust the header.
 */
@Component
public class UserIdHeaderFilter implements GlobalFilter, Ordered {

    public static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange withoutUserId = exchange.mutate()
                .request(request -> request.headers(headers -> headers.remove(USER_ID_HEADER)))
                .build();

        return exchange.getPrincipal()
                .ofType(JwtAuthenticationToken.class)
                .map(authentication -> withoutUserId.mutate()
                        .request(request -> request.header(USER_ID_HEADER, authentication.getName()))
                        .build())
                .defaultIfEmpty(withoutUserId)
                .flatMap(chain::filter);
    }

    /** Runs before the request is sent to the service. */
    @Override
    public int getOrder() {
        return 0;
    }
}
