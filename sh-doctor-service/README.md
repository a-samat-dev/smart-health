# sh-doctor-service

Owns doctor profiles: registering as a doctor with a license, specializations, the address and contacts of
the practice, working hours and days off. Spring Boot, PostgreSQL (schema `doctor_service`, migrated by
Flyway), license files in MinIO. Runs on port 8082.

## API

All requests come through sh-api-gateway, under `/doctor-service`: clients call
`/doctor-service/api/v1/...`, and the gateway passes the request on as `/api/v1/...`. It checks the access
token. Swagger UI:
http://localhost:8080/swagger-ui.html (through the gateway) or http://localhost:8082/swagger-ui.html.

| Method | Path                                    | What it does                                                    | Success |
|--------|-----------------------------------------|-----------------------------------------------------------------|---------|
| GET    | `/api/v1/specializations`               | The specializations a doctor can choose from, by name           | 200     |
| POST   | `/api/v1/doctors`                       | Registers the caller as a doctor; `Location` points to the profile | 201  |
| GET    | `/api/v1/doctors?specializationId=&page=&size=` | Doctors by name, optionally with one specialization     | 200     |
| GET    | `/api/v1/doctors/{userId}`              | A doctor's profile                                              | 200     |
| PUT    | `/api/v1/doctors/{userId}`              | Replaces your profile                                           | 200     |
| GET    | `/api/v1/doctors/{userId}/license-file` | Downloads your license PDF                                      | 200     |
| PUT    | `/api/v1/doctors/{userId}/license-file` | Replaces your license PDF                                       | 204     |

A doctor profile is identified by the doctor's user id: a user has at most one. Anyone signed in can read
profiles, but only the doctor can change theirs or download its license file.

### Registering as a doctor

`POST /api/v1/doctors` takes `multipart/form-data` with two parts: `doctor`, the profile as JSON, and
`licenseFile`, the license as a PDF of at most 10 MB.

```bash
curl -X POST http://localhost:8080/api/v1/doctors \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -F 'licenseFile=@license.pdf;type=application/pdf' \
  -F 'doctor={
        "licenseNumber": "KZ-0042",
        "specializationIds": [3, 5],
        "address": {"country": "Kazakhstan", "city": "Almaty", "street": "Abay Avenue", "houseNumber": "52A"},
        "contactEmail": "clinic@example.com",
        "contactPhone": "+7 727 123 45 67",
        "workingHours": {"start": "09:00", "end": "17:00"},
        "daysOff": ["SATURDAY", "SUNDAY"],
        "timeZone": "Asia/Almaty"
      };type=application/json'
```

- `specializationIds`: at least one id from `GET /api/v1/specializations`.
- `workingHours`: `HH:mm` in the doctor's `timeZone`, ending after they start on the same day. sh-appointment-service
  cuts them into appointment slots.
- `daysOff`: days of the week without appointments, at most six.

The service copies the user's first and last name from sh-user-service, stores the license file, saves the
profile, and then changes the user's type to `DOCTOR` in sh-user-service. That change comes last: if it
fails, the profile is rolled back and the user stays a client. Renaming the account later does not rename
the doctor profile.

### Errors

Errors have one shape: `{"timestamp", "status", "error", "message", "path", "details"}`, where `details`
lists rejected fields.

| Status | When                                                                                    |
|--------|-----------------------------------------------------------------------------------------|
| 400    | Invalid input, an unknown specialization or time zone, or a license file that is not a PDF |
| 401    | No `X-User-Id` header on a request that needs one                                       |
| 403    | Another doctor's profile or license file                                                |
| 404    | A doctor, or a registering user, that does not exist                                    |
| 409    | Registering twice, or a license number another doctor has                               |
| 413    | A license file over 10 MB                                                               |
| 503    | sh-user-service or MinIO cannot be reached                                              |

## Data

- `specializations` is an enum table: `id` and `name`, filled by the `V1` migration. New specializations
  come with a new migration.
- `doctors` is keyed by `user_id`, the user's id in sh-user-service. It is not a database foreign key: the
  users table belongs to another service's schema, so the service checks the id with sh-user-service when
  the doctor registers.
- `doctor_specializations` and `doctor_days_off` hold a doctor's specializations and days off.

## License files

License files go to the MinIO bucket `doctor-licenses` (created on the first upload) as
`{userId}/license.pdf`, so a new file replaces the old one. The database keeps the link to the file in
`doctors.license_file_url`. The bucket is private: clients download the file through
`GET /api/v1/doctors/{userId}/license-file`.

## Calls to sh-user-service

The service calls sh-user-service directly, not through the gateway:

- `GET /internal/v1/users/{id}` for the name of a user who registers as a doctor,
- `PUT /internal/v1/users/{id}/type` to make them a `DOCTOR`.

Connection errors and 5xx responses are retried three times, after 100 ms, 200 ms and 400 ms (Spring
`RetryTemplate`). The calls go through a Resilience4j circuit breaker (Spring Cloud Circuit Breaker) that
opens when half of the last 20 calls failed; 4xx answers do not count as failures. When the calls fail or the
breaker is open, the client gets `503`. The settings are in `ResilienceConfig`.

### What this service expects from the gateway

- For the endpoints that need an access token, the gateway passes the user's id in the `X-User-Id` header.
  This service trusts that header, so it must not be reachable except through the gateway.

## Running

You need Java 21, Maven, PostgreSQL with a `smart_health` database (see sh-user-service), MinIO, and
sh-user-service running.

MinIO runs from the compose file in `infra`, from the repository root:

```bash
docker compose -f infra/docker-compose.yml up -d
```

```bash
mvn spring-boot:run
```

| Variable                                  | Default                                   |
|-------------------------------------------|-------------------------------------------|
| `DB_HOST`, `DB_PORT`, `DB_NAME`           | `localhost`, `5436`, `smart_health`       |
| `DB_USERNAME`, `DB_PASSWORD`              | `postgres`, empty                         |
| `USER_SERVICE_URL`                        | `http://localhost:8081`                   |
| `MINIO_URL`                               | `http://localhost:9000`                   |
| `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`    | `minioadmin`, `minioadmin`: MinIO's defaults, for a local MinIO only |
| `MINIO_BUCKET`                            | `doctor-licenses`                         |
| `GATEWAY_URL`                             | `http://localhost:8080`, the server Swagger UI sends requests to |

## Tracing

Micrometer Tracing with Brave puts `[traceId-spanId]` on every log line, continues traces started by the
gateway and passes them on to sh-user-service. The conventions ask for Spring Cloud Sleuth, which does not
work with Spring Boot 3 or later; Micrometer Tracing is what replaced it.

## Tests

```bash
mvn test
```

The tests start PostgreSQL and MinIO in Docker with Testcontainers, so Docker must be running.
sh-user-service is replaced by a stub.
