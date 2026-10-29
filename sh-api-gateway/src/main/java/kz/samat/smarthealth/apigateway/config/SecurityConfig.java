package kz.samat.smarthealth.apigateway.config;

import java.time.Duration;
import java.util.List;

import kz.samat.smarthealth.apigateway.exception.ApiErrorWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Every request needs a valid access token except the {@link #PUBLIC_REQUESTS}.
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Served without looking at the Authorization header at all, so an expired access token sent along
     * with a token refresh cannot get that request rejected.
     */
    private static final ServerWebExchangeMatcher PUBLIC_REQUESTS = new OrServerWebExchangeMatcher(
            // Sign-up, sign-in and token refresh.
            ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "/user-service/api/v1/users",
                    "/user-service/api/v1/sessions", "/user-service/api/v1/sessions/*/tokens"),
            ServerWebExchangeMatchers.pathMatchers(
                    "/actuator/health/**", "/actuator/info",
                    "/swagger-ui.html", "/swagger-ui/**", "/webjars/**", "/v3/api-docs/**",
                    "/user-service/v3/api-docs", "/doctor-service/v3/api-docs", "/appointment-service/v3/api-docs"));

    /** Endpoints the services offer only to each other, such as /user-service/internal/v1/users/{id}. */
    private static final String INTERNAL_PATHS = "/*/internal/**";

    private static final String INVALID_TOKEN_MESSAGE = "Access token is missing, invalid or expired";

    @Bean
    @Order(1)
    public SecurityWebFilterChain publicSecurityWebFilterChain(ServerHttpSecurity http) {
        return statelessApi(http)
                .securityMatcher(PUBLIC_REQUESTS)
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .build();
    }

    @Bean
    @Order(2)
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ApiErrorWriter apiErrorWriter) {
        return statelessApi(http)
                .authorizeExchange(exchanges -> exchanges
                        // The services' internal endpoints trust their callers, so clients must not reach them.
                        .pathMatchers(INTERNAL_PATHS).denyAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationFailureHandler((filterExchange, ex) ->
                                onInvalidToken(filterExchange.getExchange(), ex, apiErrorWriter))
                        .authenticationEntryPoint((exchange, ex) ->
                                unauthorized(exchange, "Bearer", apiErrorWriter))
                        .accessDeniedHandler((exchange, ex) -> apiErrorWriter.write(
                                exchange, HttpStatus.FORBIDDEN, "Access denied")))
                .build();
    }

    /**
     * Verifies the signature, expiry and issuer of access tokens issued by sh-user-service, using the public
     * keys it publishes. Only RS256 is accepted.
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder(JwtProperties jwtProperties) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwtProperties.jwkSetUri())
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(jwtProperties.issuer()));
        // A failed key fetch surfaces as an IllegalStateException, which would become a 500. As a JwtException
        // it reaches onInvalidToken as an AuthenticationServiceException instead.
        return token -> decoder.decode(token).onErrorMap(
                ex -> !(ex instanceof JwtException),
                ex -> new JwtException(ex.getMessage(), ex));
    }

    /**
     * A bad token gets 401, so the client refreshes it. When the token could not be checked at all, because
     * the public keys could not be fetched, the client gets 503 instead and keeps its tokens.
     */
    private static Mono<Void> onInvalidToken(ServerWebExchange exchange, AuthenticationException ex,
                                             ApiErrorWriter apiErrorWriter) {
        if (ex instanceof AuthenticationServiceException) {
            log.warn("Could not check an access token: {}", ex.getMessage());
            return apiErrorWriter.write(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                    "Access tokens cannot be checked right now, please try again later");
        }
        return unauthorized(exchange, "Bearer error=\"invalid_token\"", apiErrorWriter);
    }

    /** A 401 must say how to authenticate (RFC 9110); the error code follows RFC 6750. */
    private static Mono<Void> unauthorized(ServerWebExchange exchange, String wwwAuthenticate,
                                           ApiErrorWriter apiErrorWriter) {
        exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, wwwAuthenticate);
        return apiErrorWriter.write(exchange, HttpStatus.UNAUTHORIZED, INVALID_TOKEN_MESSAGE);
    }

    /**
     * Applied by the security filters, so it also covers the 401 responses they write. Without CORS headers
     * on a 401 the browser hides its status, and the client could not tell that it should refresh its token.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        configuration.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private ServerHttpSecurity statelessApi(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .cors(Customizer.withDefaults());
    }
}
