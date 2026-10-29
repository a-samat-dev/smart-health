# sh-user-service

Owns user accounts: sign-up, sign-in, token refresh, and creating, updating and deleting users. Spring Boot,
PostgreSQL (schema `user_service`, migrated by Flyway). Runs on port 8081.

## API

All requests come through sh-api-gateway, under `/user-service`: clients call `/user-service/api/v1/...`, and the
gateway passes the request on as `/api/v1/...`. Swagger UI: http://localhost:8080/swagger-ui.html (through the
gateway) or http://localhost:8081/swagger-ui.html.

| Method | Path                           | Access token | What it does                                         | Success |
|--------|--------------------------------|--------------|------------------------------------------------------|---------|
| POST   | `/api/v1/users`                | no           | Signs up: creates the account; `Location` points to it | 201   |
| GET    | `/api/v1/users/{id}`           | yes          | The user's profile                                   | 200     |
| PUT    | `/api/v1/users/{id}`           | yes          | Replaces the editable profile fields; a field left out is cleared | 200 |
| DELETE | `/api/v1/users/{id}`           | yes          | Deletes the account and its sessions                 | 204     |
| POST   | `/api/v1/sessions`             | no           | Signs in with email and password; `Location` points to the new session | 201 |
| POST   | `/api/v1/sessions/{id}/tokens` | no           | Refreshes the session's tokens with its refresh token | 200    |
| DELETE | `/api/v1/sessions/{id}`        | yes          | Signs out of the session                             | 204     |

Users can only reach their own account and end their own sessions. Signing in returns the user's id
(`userId`) along with the session id.

Every user has a `type`: `CLIENT` when they sign up, `DOCTOR` once they register as a doctor with
sh-doctor-service. Users cannot change their type themselves.

### Internal endpoints

Other services call these directly. The gateway refuses `/user-service/internal/**`, so clients cannot
reach them, and they are left out of Swagger.

| Method | Path                           | Called by                     | What it does                          | Success |
|--------|--------------------------------|-------------------------------|---------------------------------------|---------|
| GET    | `/internal/v1/users/{id}`      | sh-doctor-service, sh-appointment-service | Any user's profile, for their name | 200 |
| PUT    | `/internal/v1/users/{id}/type` | sh-doctor-service             | Sets the type, as `{"type": "DOCTOR"}` | 200    |

Errors have one shape: `{"timestamp", "status", "error", "message", "path", "details"}`, where `details`
lists rejected fields. The statuses:

| Status | When                                                                          |
|--------|-------------------------------------------------------------------------------|
| 400    | Invalid input                                                                 |
| 401    | Wrong email or password, an invalid or expired refresh token, or no `X-User-Id` |
| 403    | Another user's account or session                                             |
| 404    | A user or session that does not exist                                         |
| 409    | An email that is already taken                                                |

## Sessions and tokens

Signing in creates a session, which holds two tokens:

- **Access token**: a JWT signed with RS256, valid for 1 hour. The subject is the user id, the issuer is
  `sh-user-service`, and the `kid` header names the key that signed it.
- **Refresh token**: a random string valid for 7 days. Refreshing replaces both tokens but keeps the
  session id, and the refresh token sent stops working. The database keeps only a SHA-256 hash of it.

The public key is published as a JSON Web Key Set at `GET /.well-known/jwks.json`. Anyone verifying access
tokens fetches it from there, so the private key never leaves this service. The gateway calls this endpoint
directly, not through its routes, and it is not listed in Swagger.

### Signing key

Generate a private key and point `JWT_PRIVATE_KEY_LOCATION` at it:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
```

```bash
JWT_PRIVATE_KEY_LOCATION=file:./jwt-private.pem mvn spring-boot:run
```

Without it, the service generates a temporary key at startup and logs a warning. That is fine for a single
local instance. After a restart, the old access tokens stop working once the gateway fetches the new key,
and clients get new ones with their refresh token. It is not fine for production, where several instances
must sign with the same key.

### What this service expects from the gateway

- For the endpoints that need an access token, the gateway passes the user's id in the `X-User-Id` header.
  This service trusts that header, so it must not be reachable except through the gateway.

## Running

You need Java 21, Maven, and PostgreSQL with a `smart_health` database:

```bash
psql -h localhost -p 5436 -U postgres -c "CREATE DATABASE smart_health"
```

```bash
mvn spring-boot:run
```

| Variable                                                   | Default                                  |
|------------------------------------------------------------|------------------------------------------|
| `DB_HOST`, `DB_PORT`, `DB_NAME`                            | `localhost`, `5436`, `smart_health`      |
| `DB_USERNAME`, `DB_PASSWORD`                               | `postgres`, empty                        |
| `JWT_PRIVATE_KEY_LOCATION`                                 | empty: a temporary key is generated (see above) |
| `GATEWAY_URL`                                              | `http://localhost:8080`, the server Swagger UI sends requests to |

## Tracing

Micrometer Tracing with Brave puts `[traceId-spanId]` on every log line and continues traces started by
the gateway. The conventions ask for Spring Cloud Sleuth, which does not work with Spring Boot 3 or later;
Micrometer Tracing is what replaced it.

## Tests

```bash
mvn test
```

The tests start PostgreSQL in Docker with Testcontainers, so Docker must be running.
