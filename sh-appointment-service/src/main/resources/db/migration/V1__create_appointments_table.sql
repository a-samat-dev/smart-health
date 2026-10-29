-- doctor_id and client_id are user ids from sh-user-service; a doctor's profile in sh-doctor-service has the
-- same id. They are not database foreign keys, because those tables belong to other services' schemas. The
-- names are copied when the appointment is booked, so listing appointments needs no other service.
CREATE TABLE appointments
(
    id                  UUID PRIMARY KEY,
    doctor_id           UUID          NOT NULL,
    doctor_name         VARCHAR(201)  NOT NULL,
    client_id           UUID          NOT NULL,
    client_name         VARCHAR(201)  NOT NULL,
    specialization_id   INTEGER       NOT NULL,
    specialization_name VARCHAR(100)  NOT NULL,
    start_time          TIMESTAMPTZ   NOT NULL,
    end_time            TIMESTAMPTZ   NOT NULL,
    notes               VARCHAR(1000),
    created_at          TIMESTAMPTZ   NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL,
    CHECK (start_time < end_time),
    -- Two bookings of the same slot that race each other cannot both succeed.
    UNIQUE (doctor_id, start_time)
);

CREATE INDEX idx_appointments_client_id_start_time ON appointments (client_id, start_time);
