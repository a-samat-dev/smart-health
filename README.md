# Smart Health

Each folder here is a separate system with its own build, configuration and README:

| System                                           | Port | What it does                                                            |
|--------------------------------------------------|------|-------------------------------------------------------------------------|
| [sh-api-gateway](sh-api-gateway)                 | 8080 | Checks access tokens and routes requests to the services                |
| [sh-user-service](sh-user-service)               | 8081 | Sign-up, sign-in, token refresh and user accounts, clients and doctors  |
| [sh-doctor-service](sh-doctor-service)           | 8082 | Doctor profiles: license, specializations, address, working hours       |
| [sh-appointment-service](sh-appointment-service) | 8083 | Doctors' slots, and booking and listing appointments                    |
| [sh-web-app](sh-web-app)                         | 5173 | React app: accounts, doctor registration, booking, appointment calendar |

```
browser ──► sh-api-gateway ──┬──► sh-user-service ────────► PostgreSQL (schema user_service)
                             ├──► sh-doctor-service ──────► PostgreSQL (schema doctor_service), MinIO
                             │        └─► sh-user-service
                             └──► sh-appointment-service ─► PostgreSQL (schema appointment_service)
                                      ├─► sh-doctor-service
                                      └─► sh-user-service
```

Each service owns its schema; no service reads another's tables. The services call each other directly over
HTTP, with retries and circuit breakers. sh-user-service's `/internal/**` endpoints are only for them: the
gateway refuses those paths.

Clients reach each service through the gateway under the service's name: `/user-service/**`,
`/doctor-service/**` and `/appointment-service/**`. The gateway removes the name, so the services' own paths,
which their READMEs list, start with `/api/v1`.

The services share no secrets. sh-user-service signs access tokens with its RSA private key and
publishes the public key at `/.well-known/jwks.json`. sh-api-gateway verifies tokens with the keys it
fetches from there.

## Running everything locally

You need PostgreSQL with a `smart_health` database on port 5436, and MinIO on port 9000, which
`infra/docker-compose.yml` starts:

```bash
docker compose -f infra/docker-compose.yml up -d
```

The MinIO console is at http://localhost:9001 (`minioadmin` / `minioadmin`). Then start sh-user-service,
sh-doctor-service, sh-appointment-service, then sh-api-gateway, then sh-web-app, each as its README
describes, and open http://localhost:5173.
