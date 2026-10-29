# sh-appointment-service

Owns appointments: doctors' free slots, and booking, listing, rescheduling and cancelling appointments.
Spring Boot, PostgreSQL (schema `appointment_service`, migrated by Flyway). Runs on port 8083.

## API

All requests come through sh-api-gateway, under `/appointment-service`: clients call
`/appointment-service/api/v1/...`, and the gateway passes the request on as `/api/v1/...`. It checks the access
token. Swagger UI:
http://localhost:8080/swagger-ui.html (through the gateway) or http://localhost:8083/swagger-ui.html.

| Method | Path                                      | What it does                                                 | Success |
|--------|-------------------------------------------|--------------------------------------------------------------|---------|
| GET    | `/api/v1/slots?doctorId=&from=&to=`       | A doctor's upcoming slots in a period, and whether each is free | 200  |
| POST   | `/api/v1/appointments`                    | Books a free slot; `Location` points to the appointment      | 201     |
| GET    | `/api/v1/appointments`                    | One page of your appointments (see below)                    | 200     |
| GET    | `/api/v1/appointments/{id}`               | One of your appointments                                     | 200     |
| PUT    | `/api/v1/appointments/{id}`               | Moves it to another free slot of the same doctor, and replaces the notes | 200 |
| DELETE | `/api/v1/appointments/{id}`               | Cancels it, which frees the slot                             | 204     |

An appointment belongs to its patient, who booked it, and its doctor: both can see, reschedule and cancel
it. Nobody else can.

### Slots

A doctor's working hours from sh-doctor-service, such as 09:00 to 17:00 in Asia/Almaty, are cut into
30-minute slots (`APPOINTMENT_SLOT_DURATION`). Days off have none. `GET /api/v1/slots` takes `from` and
`to` as instants, such as `2026-11-02T00:00:00Z`, at most 31 days apart, and returns the slots between them
that have not started:

```json
[{"startTime": "2026-11-02T04:00:00Z", "endTime": "2026-11-02T04:30:00Z", "available": true}]
```

A slot is unavailable when the doctor has an appointment then, including one they booked with another
doctor as a patient.

### Booking

```json
{"doctorId": "…", "specializationId": 3, "startTime": "2026-11-02T04:00:00Z", "notes": "Headache for a week"}
```

The start must be one of the doctor's slots, in the future, and the specialization one of theirs. Neither
the doctor nor the patient may have another appointment then. A unique index on the doctor and the start
time stops two bookings of the same slot that race each other. The names of the doctor, the patient and the
specialization are copied onto the appointment, so listing appointments calls no other service.

### Listing

`GET /api/v1/appointments` lists the appointments you booked and, if you are a doctor, those booked with
you. The query parameters:

| Parameter        | What it does                                                                     |
|------------------|----------------------------------------------------------------------------------|
| `size`           | Appointments per page, 1 to 100; 20 by default                                   |
| `cursor`         | `nextCursor` of the previous page; left out for the first page                   |
| `sort`           | `createdAt` (when it was booked) or `startTime`, then `asc` or `desc`; `createdAt,desc` by default |
| `specialization` | Searches: only appointments whose specialization contains the text, ignoring case, such as `cardio` |
| `from`, `to`     | Filters by date: only appointments that start at or after `from` and before `to` (instants) |

```json
{"items": [{"id": "…", "doctorName": "…", "clientName": "…", "specializationName": "Cardiologist",
            "startTime": "…", "endTime": "…", "notes": "…", "createdAt": "…"}],
 "nextCursor": "Y3JlYXRlZEF0LGRlc2N8…"}
```

Pages are cut with a cursor rather than an offset. The cursor holds the last item's value of the sort
property (its creation time, `created_at`, by default) and its id, which breaks ties; the next page starts
after that item. Appointments booked in the meantime therefore do not shift pages or repeat items. A cursor
only works with the sort it was made for. `nextCursor` is `null` on the last page.

### Errors

Errors have one shape: `{"timestamp", "status", "error", "message", "path", "details"}`, where `details`
lists rejected fields.

| Status | When                                                                                      |
|--------|-------------------------------------------------------------------------------------------|
| 400    | Invalid input, a time that is not a future slot of the doctor, a specialization the doctor does not offer, an appointment with yourself, or an invalid cursor |
| 401    | No `X-User-Id` header                                                                     |
| 403    | Another user's appointment                                                                |
| 404    | An appointment, doctor or user that does not exist                                       |
| 409    | A slot that is taken, or a patient who has another appointment then                      |
| 503    | sh-doctor-service or sh-user-service cannot be reached                                    |

## Calls to other services

The service calls these directly, not through the gateway:

- sh-doctor-service, `GET /api/v1/doctors/{userId}`, for a doctor's working hours, days off, time zone and
  specializations,
- sh-user-service, `GET /internal/v1/users/{id}`, for the patient's name when they book.

Connection errors and 5xx responses are retried three times, after 100 ms, 200 ms and 400 ms (Spring
`RetryTemplate`). Each service has its own Resilience4j circuit breaker (Spring Cloud Circuit Breaker) that
opens when half of the last 20 calls failed; 4xx answers do not count as failures. When the calls fail or a
breaker is open, the client gets `503`. The settings are in `ResilienceConfig`.

### What this service expects from the gateway

- The gateway passes the user's id in the `X-User-Id` header. This service trusts that header, so it must
  not be reachable except through the gateway.

## Running

You need Java 21, Maven, PostgreSQL with a `smart_health` database (see sh-user-service), and
sh-user-service and sh-doctor-service running.

```bash
mvn spring-boot:run
```

| Variable                        | Default                                                     |
|---------------------------------|-------------------------------------------------------------|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | `localhost`, `5436`, `smart_health`                         |
| `DB_USERNAME`, `DB_PASSWORD`    | `postgres`, empty                                           |
| `USER_SERVICE_URL`              | `http://localhost:8081`                                     |
| `DOCTOR_SERVICE_URL`            | `http://localhost:8082`                                     |
| `APPOINTMENT_SLOT_DURATION`     | `30m`                                                       |
| `GATEWAY_URL`                   | `http://localhost:8080`, the server Swagger UI sends requests to |

## Tracing

Micrometer Tracing with Brave puts `[traceId-spanId]` on every log line, continues traces started by the
gateway and passes them on to the services it calls. The conventions ask for Spring Cloud Sleuth, which
does not work with Spring Boot 3 or later; Micrometer Tracing is what replaced it.

## Tests

```bash
mvn test
```

The tests start PostgreSQL in Docker with Testcontainers, so Docker must be running. sh-doctor-service and
sh-user-service are replaced by a stub.
