-- One row per user who registered as a doctor, keyed by their user id in sh-user-service. That id is not a
-- database foreign key because the users table belongs to another service's schema; sh-doctor-service checks
-- it with sh-user-service instead.
CREATE TABLE doctors
(
    user_id          UUID PRIMARY KEY,
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    license_number   VARCHAR(50)  NOT NULL UNIQUE,
    license_file_url VARCHAR(500) NOT NULL,
    country          VARCHAR(100) NOT NULL,
    city             VARCHAR(100) NOT NULL,
    street           VARCHAR(200) NOT NULL,
    house_number     VARCHAR(20)  NOT NULL,
    contact_email    VARCHAR(255) NOT NULL,
    contact_phone    VARCHAR(30)  NOT NULL,
    work_start       TIME         NOT NULL,
    work_end         TIME         NOT NULL,
    time_zone        VARCHAR(50)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CHECK (work_start < work_end)
);

CREATE TABLE doctor_specializations
(
    doctor_id         UUID    NOT NULL REFERENCES doctors (user_id) ON DELETE CASCADE,
    specialization_id INTEGER NOT NULL REFERENCES specializations (id),
    PRIMARY KEY (doctor_id, specialization_id)
);

CREATE INDEX idx_doctor_specializations_specialization_id ON doctor_specializations (specialization_id);

-- Days of the week without appointments, such as SATURDAY and SUNDAY.
CREATE TABLE doctor_days_off
(
    doctor_id   UUID       NOT NULL REFERENCES doctors (user_id) ON DELETE CASCADE,
    day_of_week VARCHAR(9) NOT NULL CHECK (day_of_week IN
                                           ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    PRIMARY KEY (doctor_id, day_of_week)
);
