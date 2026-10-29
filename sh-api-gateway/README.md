# sh-api-gateway

The single entry point for clients. It checks access tokens and routes requests to the services. Spring
Cloud Gateway (WebFlux). Runs on port 8080.

## What it does

- **Authorization**: every request needs a valid access token (`Authorization: Bearer …`) except sign-up
  (`POST /user-service/api/v1/users`), sign-in (`POST /user-service/api/v1/sessions`), token refresh
  (`POST /user-service/api/v1/sessions/{id}/tokens`), health checks and the API docs. The gateway checks the token's
  RS256 signature, expiry and issuer (`sh-user-service`). When any check fails, it answers `401` with a
  `WWW-Authenticate` header and an error body. The public requests are passed on without looking at the
  token, so an expired one cannot block a refresh.
- **Public keys**: the gateway holds no key of its own. It fetches sh-user-service's public keys from
  `/.well-known/jwks.json` on the first token, and again when a token names a key it does not know, so a
  new signing key is picked up without a restart. If the keys cannot be fetched, the gateway answers `503`
  rather than `401`, so clients keep their tokens.
- **User id**: for authenticated requests the gateway sends the token's subject downstream as `X-User-Id`.
  Any `X-User-Id` the client sends is removed first.
- **Routing**: each service is reached under its name, and the gateway removes the name before passing the
  request on. `GET /doctor-service/api/v1/doctors/{id}`, for example, reaches sh-doctor-service as
  `GET /api/v1/doctors/{id}`.

  | Paths                     | Service                |
  |---------------------------|------------------------|
  | `/user-service/**`        | sh-user-service        |
  | `/doctor-service/**`      | sh-doctor-service      |
  | `/appointment-service/**` | sh-appointment-service |

  The services' internal endpoints, `/{service}/internal/**`, are refused with `403`: they trust their
  callers, so only the services may call them, directly.
- **Circuit breaker**: calls to each service go through its own Resilience4j circuit breaker (Spring Cloud
  Circuit Breaker). When a service is down or its breaker is open, the gateway answers `503` and names the
  service. The settings are in `ResilienceConfig`.
- **Retry with backoff**: `GET` requests that fail with a connection error or a 5xx are retried three
  times, after 100 ms, 200 ms and 400 ms. Other methods are not retried.
- **CORS**: browser origins in `CORS_ALLOWED_ORIGINS` may call the API. 401 responses carry CORS headers
  too, so a browser client can see them and refresh its token.
- **API docs**: http://localhost:8080/swagger-ui.html shows the OpenAPI documents of all three services,
  each served at `/{service}/v3/api-docs`.

## Running

You need Java 21 and Maven.

```bash
mvn spring-boot:run
```

| Variable               | Default                                                           |
|------------------------|-------------------------------------------------------------------|
| `USER_SERVICE_URL`     | `http://localhost:8081`; also where the public keys are fetched   |
| `DOCTOR_SERVICE_URL`   | `http://localhost:8082`                                           |
| `APPOINTMENT_SERVICE_URL` | `http://localhost:8083`                                        |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173`, comma-separated                          |

## Tracing

Micrometer Tracing with Brave puts `[traceId-spanId]` on every log line and passes the trace on to the
services, so one request carries the same trace id in every service's logs. The conventions ask for Spring
Cloud Sleuth, which does not work with Spring Boot 3 or later; Micrometer Tracing is what replaced it.

## Tests

```bash
mvn test
```

The tests run the gateway in front of stub services, so nothing else has to be running.
